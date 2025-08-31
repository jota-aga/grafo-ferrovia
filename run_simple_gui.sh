#!/bin/bash

echo "=== SIMULAÇÃO FERROVIÁRIA COM INTERFACE GRÁFICA SIMPLES ==="
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

# Compila o projeto (sem módulos)
echo "Compilando projeto..."
javac -d bin -cp src \
    --add-modules java.desktop \
    src/grafo_ferroviaria/*.java \
    src/grafo_ferroviaria/managers/*.java \
    src/grafo_ferroviaria/models/*.java \
    src/grafo_ferroviaria/enums/*.java \
    src/grafo_ferroviaria/view/*.java

if [ $? -ne 0 ]; then
    echo "Erro na compilação!"
    exit 1
fi

echo "Executando interface gráfica simples..."
echo "Pressione Ctrl+C para sair"
echo ""

# Executa a interface gráfica
java -cp bin grafo_ferroviaria.SimpleGUI "$RAILWAY_FILE" 