#!/bin/bash

RAILWAY_FILE=$1

if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed!"
    echo "Install Maven with: sudo apt install maven"
    exit 1
fi

mvn clean compile

if [ $? -ne 0 ]; then
    echo "Error: Compilation failed!"
    exit 1
fi

mvn exec:java -Dexec.mainClass="grafo_ferroviaria.Main" -Dexec.args="$RAILWAY_FILE"