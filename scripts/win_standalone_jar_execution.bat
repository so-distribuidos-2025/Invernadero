@echo off
cd /d "%~dp0"
cd ..

echo Iniciando Servidor Mutex...
start "Servidor Mutex" java -jar components\server_mutex\serverMutex.jar

echo Esperando 3 segundos al Mutex...
timeout /t 3 /nobreak > nul

echo Iniciando Sistema de Fertirrigacion...
start "Sistema Fertirrigacion" java -jar components\sistema_fertirrigacion\sistemaFertirrigacion.jar

echo Iniciando 7 Electrovalvulas...
for /L %%i in (0, 1, 6) do (
    start "Electrovalvula %%i" java -jar components\electrovalvula\electrovalvula.jar %%i
)

echo Esperando 5 segundos a las valvulas...
timeout /t 5 /nobreak > nul

echo Iniciando Controlador...
start "Controlador" java -jar components\controlador\controlador.jar

echo Esperando 5 segundos al controlador...
timeout /t 5 /nobreak > nul

echo Iniciando sensores globales...
start "Sensor Iluminacion" java -jar components\sensores\iluminacion\sensorIluminacion.jar
start "Sensor Lluvia" java -jar components\sensores\lluvia\sensorLluvia.jar
start "Sensor Temperatura" java -jar components\sensores\temperatura\sensorTemperatura.jar

echo Iniciando 5 sensores de Humedad...
for /L %%i in (0, 1, 4) do (
    start "Sensor Humedad %%i" java -jar components\sensores\humedad\sensorHumedad.jar %%i
)

echo Iniciando 5 Temporizadores...
for /L %%i in (0, 1, 4) do (
    start "Temporizador %%i" java -jar components\temporizador\temporizador.jar %%i
)

echo.
echo Sistema de Fertirrigacion iniciado correctamente.
pause
