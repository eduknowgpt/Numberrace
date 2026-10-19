@echo off
rem Exportador dos dados de interação do Number Race (evolution/pt/exportacao-dados.md).
rem Coloque este arquivo na pasta do jogo, ao lado da pasta lib (com os .jar do jogo e o sqlite-jdbc).
rem Dois cliques: exporta todas as sessões deste computador e gera também um .zip.
rem Opções (filtros, outras pastas, verificação): exportar.bat --ajuda
chcp 1252 >nul
cd /d "%~dp0"
title Number Race - exportacao dos dados
java -cp "lib\*" org.unicog.numberrace.analytics.export.ExportTool --zip %*
echo.
if "%~1"=="" if exist "%USERPROFILE%\NumberRace\v3\exportacoes" start "" "%USERPROFILE%\NumberRace\v3\exportacoes"
pause
