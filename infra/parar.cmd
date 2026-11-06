@echo off
rem atalho: roda o parar.ps1 sem mudar a politica de execucao do Windows
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0parar.ps1" %*
if "%~1"=="" pause
