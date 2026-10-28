@echo off
setlocal
cd /d "%~dp0"

where node >nul 2>nul
if errorlevel 1 (
  echo Node.js no esta instalado. Instala Node ^>= 24.15 y vuelve a intentar.
  exit /b 1
)

node scripts\start-local.mjs %*
