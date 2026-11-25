# GPS-tracker

USB GPS 수신기(u-blox 등 NMEA 출력 장치)의 위치를 브라우저에서 Google 지도에 실시간으로 표시합니다.

## 실행
1. Node.js 설치 후 `start.bat` 실행 (또는 `node server.js` → http://localhost:8790)
2. Chrome/Edge에서 **GPS 연결** → GPS의 COM 포트 선택 (기본 9600bps)

## 기능
- 위도·경도·고도·속도·사용 위성·HDOP·시각 표시, 위성별 신호(SNR) 막대
- Google Maps API 키 입력 시: 실시간 마커, 이동 경로, 정확도 원
- 키가 없으면 Google 지도 임베드로 현재 위치 표시
- 이동 경로 GPX 저장

## 파일
- `index.html` — 화면, Web Serial 읽기, 지도 표시
- `nmea.js` — NMEA 0183 파서 (RMC/GGA/GSA/GSV, 체크섬 검사)
- `server.js` — localhost 정적 서버
- `start.bat` — 서버 실행 + Edge 열기

## Google Maps API 키
Google Cloud 콘솔에서 Maps JavaScript API를 사용 설정하고 API 키를 만든 뒤,
웹사이트 제한을 `http://localhost:8790/*`, API 제한을 Maps JavaScript API로 설정하세요.
키는 브라우저 localStorage에만 저장되며 저장소에는 포함되지 않습니다.
