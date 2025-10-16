# Invernadero

Requisitos:

	Maven 3.6
	JDK 24
	
Pasos para ejecutar el proyecto:

Windows:
1. Abra una consola en su sistema operativo, y situese en la carpeta raiz del proyecto (../Invernadero/)
2. Ejecutar mvn clean package para generar los .jar
3. Ejecute con el comando .\scripts\win_standalone_jar_execution.bat
4. Para terminar, ejecute Ctrl + z

Linux
1. Abra una consola y situese en la carpeta raiz del proyecto
2. Ejecutar mvn clean package para generar los .jar
3. Ejecutar sudo chmod +x ./linux_standalone_jar_execution.sh
4. Ejecutar ./linux_standalone_jar_execution
5. Para terminar, ejecute sudo pkill -f '.\jar'
