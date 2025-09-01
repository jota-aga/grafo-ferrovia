@echo off

set RAILWAY_FILE=%~1

mvn clean compile

mvn exec:java -Dexec.mainClass="main.java.grafo_ferroviaria.Main" -Dexec.args="%RAILWAY_FILE%"
pause 