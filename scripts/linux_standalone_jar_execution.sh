#!/bin/bash
cd "$(dirname "$0")"
cd ..

echo "Iniciando Servidor Mutex..."
java -jar components/server_mutex/serverMutex.jar &

echo "Esperando 3 segundos al Mutex..."
sleep 3

echo "Iniciando Sistema de Fertirrigacion..."
java -jar components/sistema_fertirrigacion/sistemaFertirrigacion.jar &

echo "Iniciando 7 Electrovalvulas..."
for ((i=0; i<=6; i++)); do
    java -jar components/electrovalvula/electrovalvula.jar "$i" &
done

echo "Esperando 5 segundos a las valvulas..."
sleep 5

echo "Iniciando Controlador..."
java -jar components/controlador/controlador.jar &

echo "Esperando 5 segundos al controlador..."
sleep 5

echo "Iniciando sensores globales..."
java -jar components/sensores/iluminacion/sensorIluminacion.jar &
java -jar components/sensores/lluvia/sensorLluvia.jar &
java -jar components/sensores/temperatura/sensorTemperatura.jar &

echo "Iniciando 5 sensores de Humedad..."
for ((i=0; i<=4; i++)); do
    java -jar components/sensores/humedad/sensorHumedad.jar "$i" &
done

echo "Iniciando 5 Temporizadores..."
for ((i=0; i<=4; i++)); do
    java -jar components/temporizador/temporizador.jar "$i" &
done

echo
echo "Sistema de Fertirrigacion iniciado correctamente."
