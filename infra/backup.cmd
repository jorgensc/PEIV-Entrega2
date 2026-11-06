@echo off
rem atalho: roda o backup.ps1 sem mudar a politica de execucao do Windows
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0backup.ps1" %*
if "%~1"=="" pause
