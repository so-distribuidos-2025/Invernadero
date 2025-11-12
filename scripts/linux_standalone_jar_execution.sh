#!/bin/bash
cd "$(dirname "$0")"
cd ..

echo "Iniciando Servidor Mutex..."
xfce4-terminal -T "Mutex" -e "java -jar components/server_mutex/serverMutex.jar"

echo "Esperando 3 segundos al Mutex..."
sleep 3

echo "Iniciando Sistema de Fertirrigacion..."
xfce4-terminal -T "Ferti" -e "java -jar components/sistema_fertirrigacion/sistemaFertirrigacion.jar"

echo "Iniciando 7 Electrovalvulas..."
for ((i=0; i<=6; i++)); do
    xfce4-terminal -T "Electrovalvula" -e "java -jar components/electrovalvula/electrovalvula.jar "$i""
done

echo "Esperando 5 segundos a las valvulas..."
sleep 5

echo "Iniciando Controlador..."
xfce4-terminal -T "Controlador" -e "java -jar components/controlador/controlador.jar"

echo "Esperando 5 segundos al controlador..."
sleep 5

echo "Iniciando sensores globales..."
xfce4-terminal -T "SensorI" -e "java -jar components/sensores/iluminacion/sensorIluminacion.jar"
xfce4-terminal -T "SensorL" -e "java -jar components/sensores/lluvia/sensorLluvia.jar"
xfce4-terminal -T "SensorT" -e "java -jar components/sensores/temperatura/sensorTemperatura.jar"

echo "Iniciando 5 sensores de Humedad..."
for ((i=0; i<=4; i++)); do
    xfce4-terminal -T "SensorH" -e "java -jar components/sensores/humedad/sensorHumedad.jar "$i""
done

echo "Iniciando 5 Temporizadores..."
for ((i=0; i<=4; i++)); do
    xfce4-terminal -T "Timer" -e "java -jar components/temporizador/temporizador.jar "$i""
done

echo
echo "Sistema de Fertirrigacion iniciado correctamente."
