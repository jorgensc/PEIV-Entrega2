@echo off
rem atalho: roda o instalar.ps1 sem mudar a politica de execucao do Windows
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0instalar.ps1" %*
if "%~1"=="" pause
