@echo off
rem atalho: roda o atualizar.ps1 sem mudar a politica de execucao do Windows
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0atualizar.ps1" %*
if "%~1"=="" pause
