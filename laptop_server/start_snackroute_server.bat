@echo off
title SnackRoute Laptop Local Server
set "TARGET_DIR=D:\IDEAS AND PLANS\1 BANANA CHIPS\LOCAL SERVER"
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"
echo ============================================================
echo   SnackRoute Laptop Local Server
echo   Data Folder: %TARGET_DIR%
echo ============================================================
where python >nul 2>nul
if %errorlevel%==0 (
  python "%~dp0server.py" --data-dir "%TARGET_DIR%"
  goto :done
)
where py >nul 2>nul
if %errorlevel%==0 (
  py -3 "%~dp0server.py" --data-dir "%TARGET_DIR%"
  goto :done
)
echo Python 3 was not found. Please install Python 3 from python.org (check "Add Python to PATH").
:done
pause
