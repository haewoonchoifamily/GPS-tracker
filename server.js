// GPS 지도 페이지를 http://localhost:8790 로 제공하는 작은 정적 서버
// (Web Serial과 Google Maps API 키의 도메인 제한이 localhost에서 잘 동작하도록)
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8790;
const ROOT = __dirname;
const TYPES = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css' };

http.createServer((req, res) => {
  const url = decodeURIComponent(req.url.split('?')[0]);
  const file = path.join(ROOT, url === '/' ? 'index.html' : url);
  if (!file.startsWith(ROOT)) { res.writeHead(403); return res.end(); }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404); return res.end('Not found'); }
    res.writeHead(200, { 'Content-Type': TYPES[path.extname(file)] || 'application/octet-stream' });
    res.end(data);
  });
}).listen(PORT, '127.0.0.1', () => console.log(`GPS 지도: http://localhost:${PORT}  (종료: Ctrl+C)`));
