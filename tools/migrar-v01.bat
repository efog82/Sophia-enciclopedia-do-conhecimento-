@echo off
py -3 "%~dp0migrate_v01.py" --apk "%~1" --reinstall
pause
