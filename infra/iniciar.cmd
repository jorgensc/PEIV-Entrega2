@echo off
rem atalho: roda o iniciar.ps1 sem mudar a politica de execucao do Windows
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar.ps1" %*
if "%~1"=="" pause
