package com.example.gpstracker

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.driver.UsbSerialPort
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

/**
 * USB(OTG)로 연결된 GPS 수신기에서 NMEA 문장을 읽어,
 * WebView에 로드된 기존 지도 페이지(assets/web/index.html)로 전달합니다.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private val usbManager by lazy { getSystemService(Context.USB_SERVICE) as UsbManager }
    private val io = Executors.newSingleThreadExecutor()
    private var port: UsbSerialPort? = null
    @Volatile private var reading = false

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val device = intent.getUsbDeviceExtra()
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                        openDevice(device)
                    } else {
                        postStatus("bad", "USB 권한이 거부되었습니다. GPS를 다시 연결해 주세요.")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> tryConnect()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    closePort()
                    postStatus("warn", "USB GPS 연결이 끊어졌습니다.")
                }
            }
        }
    }

    private fun Intent.getUsbDeviceExtra(): UsbDevice? =
        if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        else @Suppress("DEPRECATION") getParcelableExtra(UsbManager.EXTRA_DEVICE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(permissionReceiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(permissionReceiver, filter)

        // 페이지는 https://appassets.androidplatform.net/assets/web/index.html 로 제공합니다.
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).also { setContentView(it) }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // 구글 지도 API 키 등을 localStorage에 저장
        }
        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: android.webkit.WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)
        }
        webView.addJavascriptInterface(GpsBridge(), "AndroidGps")
        webView.loadUrl("https://appassets.androidplatform.net/assets/web/index.html")

        tryConnect()
    }

    override fun onDestroy() {
        reading = false
        closePort()
        io.shutdownNow()
        unregisterReceiver(permissionReceiver)
        super.onDestroy()
    }

    /** 연결된 USB 시리얼 장치를 찾아 권한을 요청하거나 바로 엽니다. */
    private fun tryConnect() {
        if (port != null) return
        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).firstOrNull()
        if (driver == null) {
            postStatus("warn", "USB GPS를 OTG 케이블로 연결해 주세요.")
            return
        }
        val device = driver.device
        if (usbManager.hasPermission(device)) {
            openDevice(device)
        } else {
            val flags = PendingIntent.FLAG_MUTABLE
            val pi = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION).setPackage(packageName), flags
            )
            usbManager.requestPermission(device, pi)
        }
    }

    private fun openDevice(device: UsbDevice) {
        if (port != null) return
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            postStatus("bad", "지원하지 않는 USB 장치입니다.")
            return
        }
        val connection = usbManager.openDevice(device)
        if (connection == null) {
            postStatus("bad", "USB 장치를 열 수 없습니다.")
            return
        }
        try {
            val p = driver.ports[0]
            p.open(connection)
            p.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port = p
            startReading(p)
            postStatus("warn", "연결됨 · 위성 찾는 중…")
        } catch (e: IOException) {
            postStatus("bad", "포트 열기 실패: ${e.message}")
        }
    }

    private fun startReading(p: UsbSerialPort) {
        reading = true
        io.execute {
            val buf = ByteArray(1024)
            val line = StringBuilder()
            try {
                while (reading) {
                    val n = p.read(buf, READ_TIMEOUT_MS)
                    for (i in 0 until n) {
                        val c = buf[i].toInt().toChar()
                        when {
                            c == '\n' -> {
                                emitLine(line.toString())
                                line.setLength(0)
                            }
                            c != '\r' && line.length < MAX_LINE_LENGTH -> line.append(c)
                        }
                    }
                }
            } catch (e: IOException) {
                if (reading) postStatus("bad", "읽기 오류: ${e.message}")
            }
        }
    }

    private fun emitLine(nmea: String) {
        if (!nmea.startsWith("$")) return
        val js = "window.feedNmea && window.feedNmea(${JSONObject.quote(nmea)});"
        runOnUiThread { webView.evaluateJavascript(js, null) }
    }

    private fun postStatus(kind: String, text: String) {
        runOnUiThread {
            webView.evaluateJavascript(
                "window.onNativeStatus && window.onNativeStatus(${JSONObject.quote(kind)}, ${JSONObject.quote(text)});",
                null
            )
        }
    }

    private fun closePort() {
        reading = false
        try { port?.close() } catch (_: IOException) {}
        port = null
    }

    /** window.AndroidGps 로 노출되는 객체. 웹 페이지가 앱 안에서 실행 중인지 판별하는 데 쓰입니다. */
    inner class GpsBridge {
        @JavascriptInterface
        fun ready(): Boolean = true

        @JavascriptInterface
        fun toast(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.gpstracker.USB_PERMISSION"
        private const val BAUD_RATE = 9600
        private const val READ_TIMEOUT_MS = 200
        private const val MAX_LINE_LENGTH = 512
    }
}
