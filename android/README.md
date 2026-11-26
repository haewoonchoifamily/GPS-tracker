# GPS Tracker (Android, USB OTG)

휴대폰에 OTG 케이블로 USB GPS 수신기를 연결하면, 앱이 NMEA 문장을 읽어 상위 폴더의 지도 페이지(`index.html`)에 표시합니다.

## 구조

- `app/src/main/java/com/example/gpstracker/MainActivity.kt`: USB 시리얼 읽기, WebView 로드, NMEA를 웹페이지로 전달
- `app/src/main/assets/web/`: 웹 지도 페이지 (`index.html`, `nmea.js`). **상위 폴더 파일을 복사한 것**이므로, 지도 화면을 고치면 다시 복사해야 합니다.
- 웹페이지는 앱 안에서 `window.AndroidGps`가 있으면 Web Serial 대신 `window.feedNmea(line)`으로 들어오는 데이터를 씁니다.

## 빌드 및 설치 (Android Studio)

1. Android Studio를 설치합니다.
2. `File → Open`으로 `android/` 폴더를 엽니다. 처음 열 때 Gradle Wrapper와 SDK가 자동으로 설정됩니다.
3. 휴대폰을 USB 디버깅 모드로 연결하고 `Run`을 누릅니다. 또는 `Build → Build APK(s)`로 APK를 만들어 설치합니다.

## 사용 방법

1. 휴대폰의 OTG 케이블 또는 젠더에 GPS 수신기를 꽂습니다.
2. 앱을 실행하면 USB 권한 요청이 뜹니다. 허용합니다.
3. 상태가 "위성 찾는 중…" → "위치 고정"으로 바뀌면 지도에 표시됩니다.
4. 앱이 실행 중일 때 GPS를 꽂아야 자동으로 인식됩니다.

기본 통신 속도는 9600bps입니다. 다른 속도의 GPS라면 `MainActivity.kt`의 `BAUD_RATE`를 바꾸세요.

## 확인이 필요한 항목

- **라이브러리 버전**: `usb-serial-for-android:3.8.1`은 작성 시점의 값입니다. Gradle 동기화에서 찾지 못하면 최신 버전으로 바꾸세요.
- **구글 지도 API 키**: 웹 지도 키를 쓰는 경우, 키의 HTTP 리퍼러 제한에 `https://appassets.androidplatform.net/*`를 추가해야 지도가 뜹니다.
- **지원 칩셋**: 기본 프로버가 지원하는 칩(CDC-ACM, FTDI, CP210x, CH34x, PL2303)이 아니면 인식되지 않을 수 있습니다.
- 이 프로젝트는 빌드와 실행을 직접 검증하지 않은 상태로 작성되었습니다. 빌드 오류가 나면 메시지를 알려주세요.
