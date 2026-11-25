@echo off
chcp 65001 >nul
cd /d "%~dp0"
start "" msedge "http://localhost:8790"
node server.js
