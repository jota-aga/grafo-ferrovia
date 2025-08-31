#!/bin/bash

echo "=== SIMULAÇÃO FERROVIÁRIA COM SPRITES DE TRENS ==="
echo ""

# Verifica se o arquivo de ferrovia foi fornecido
if [ $# -eq 0 ]; then
    echo "Uso: $0 <arquivo_ferrovia>"
    echo "Exemplo: $0 ferrovia.txt"
    exit 1
fi

RAILWAY_FILE=$1

# Verifica se o arquivo existe
if [ ! -f "$RAILWAY_FILE" ]; then
    echo "Erro: Arquivo '$RAILWAY_FILE' não encontrado!"
    exit 1
fi

echo "Arquivo de ferrovia: $RAILWAY_FILE"
echo ""

# Verifica se o Maven está instalado
if ! command -v mvn &> /dev/null; then
    echo "Erro: Maven não está instalado!"
    echo "Instale o Maven com: sudo apt install maven"
    exit 1
fi

echo "Compilando projeto com Maven..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "Erro na compilação!"
    exit 1
fi

echo "Executando interface com sprites de trens..."
echo "Pressione Ctrl+C para sair"
echo ""

# Executa a interface gráfica
mvn exec:java -Dexec.mainClass="main.java.grafo_ferroviaria.SimpleGraphGUI" -Dexec.args="$RAILWAY_FILE" 