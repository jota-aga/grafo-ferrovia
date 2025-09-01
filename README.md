# Introdução
Projeto desenvolvido durante a disciplina de Algoritmos e estruturas de dados II ministrados pelo professor Igor Medeiros Vanderlei na Universidade Federal do Agreste de Pernambuco.

Alunos:
* José Uilton Ferreira de Siqueira
* João Henrique Araújo de Souza
* Pedro Henrique Matos Oliveira

# Requisitos
Para executar o projeto é necessário ter instalado em seu dispositivo os seguintes softwares:
* Java 21+
* Maven 3.6+

# Como Executar

## Linux
Certifique-se de ter instalado em sua máquina o Java 21+ e o maven 3.6+, caso não você pode instalar com os comandos abaixo:

```bash
sudo apt-get update
sudo apt-get install openjdk-21 -y
sudo apt-get install maven -y
```

### Executar no Linux:
```bash
# Dar permissão de execução ao script
chmod +x run.sh

# Executar com o arquivo de ferrovia
./run.sh ferrovia.txt
```

## Windows

### 1. Instalar Java 21
1. Baixe o OpenJDK 21 do site oficial: https://www.oracle.com/br/java/technologies/downloads/#java21
2. Execute o instalador e siga as instruções
3. Verifique a instalação abrindo o Prompt de Comando:
```cmd
java --version
```

### 2. Instalar Maven
1. Baixe o Maven do site oficial: https://maven.apache.org/download.cgi
2. Extraia o arquivo para `C:\Program Files\Apache\maven`
3. Configure as variáveis de ambiente:
   - Abra "Variáveis de Ambiente do Sistema"
   - Em "Variáveis do Sistema", clique em "Novo"
   - Nome: `MAVEN_HOME`
   - Valor: `C:\Program Files\Apache\maven`
   - Adicione `%MAVEN_HOME%\bin` ao PATH
4. Verifique a instalação:
```cmd
mvn --version
```

### 3. Executar o Projeto

#### Usando o arquivo run.bat (Recomendado)
```cmd
# Navegar até a pasta do projeto
cd C:\caminho\para\grafo-ferrovia

# Executar com o arquivo de ferrovia
run.bat ferrovia.txt
```

### 4. Solução de Problemas no Windows

#### Erro: "mvn não é reconhecido"
- Verifique se o Maven está no PATH
- Reinicie o Prompt de Comando após configurar as variáveis

#### Erro: "java não é reconhecido"
- Verifique se o Java está instalado corretamente
- Configure a variável JAVA_HOME se necessário

#### Erro de permissão
- Execute o Prompt de Comando como Administrador

#### Interface gráfica não aparece
- Verifique se o Java está configurado para usar GUI

### 5. Usando IDEs no Windows e Linux

#### IntelliJ IDEA
1. Abra o projeto (File > Open > selecione a pasta)
2. Configure o JDK 21 (File > Project Structure > Project SDK)
3. Execute a classe `SimpleGraphGUI` com argumento `ferrovia.txt`

#### Eclipse
1. Importe o projeto Maven (File > Import > Maven > Existing Maven Projects)
2. Configure o JDK 21 (Window > Preferences > Java > Installed JREs)
3. Execute a classe `SimpleGraphGUI` com argumento `ferrovia.txt`

#### VS Code
1. Instale as extensões: "Extension Pack for Java" e "Maven for Java"
2. Abra a pasta do projeto
3. Execute via Maven Explorer ou terminal integrado
