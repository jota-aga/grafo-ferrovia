#!/bin/bash

echo "=== SIMULAÇÃO FERROVIÁRIA COM INTERFACE GRÁFICA (MANUAL) ==="
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

# Verifica se as dependências do GraphStream estão disponíveis
if [ ! -d "lib" ]; then
    echo "Criando diretório de dependências..."
    mkdir -p lib
fi

# Verifica se as JARs do GraphStream estão presentes
if [ ! -f "lib/gs-core-2.0.jar" ] || [ ! -f "lib/gs-ui-swing-2.0.jar" ] || [ ! -f "lib/gs-algo-2.0.jar" ]; then
    echo "⚠️  Dependências do GraphStream não encontradas!"
    echo ""
    echo "Para usar a interface gráfica avançada, você precisa:"
    echo "1. Instalar o Maven: sudo apt install maven"
    echo "2. Executar: ./run_gui.sh ferrovia.txt"
    echo ""
    echo "Ou usar a interface simples: ./run_simple_gui.sh ferrovia.txt"
    echo ""
    echo "Executando interface simples como alternativa..."
    ./run_simple_gui.sh "$RAILWAY_FILE"
    exit 0
fi

echo "Compilando projeto com dependências manuais..."
javac -d bin -cp "src:lib/*" \
    src/grafo_ferroviaria/*.java \
    src/grafo_ferroviaria/managers/*.java \
    src/grafo_ferroviaria/models/*.java \
    src/grafo_ferroviaria/enums/*.java \
    src/grafo_ferroviaria/view/*.java

if [ $? -ne 0 ]; then
    echo "Erro na compilação!"
    exit 1
fi

echo "Executando interface gráfica avançada..."
echo "Pressione Ctrl+C para sair"
echo ""

# Executa a interface gráfica
java -cp "bin:lib/*" grafo_ferroviaria.MainGUI "$RAILWAY_FILE" 