javac -d bin --module-path src src/module-info.java src/grafo_ferroviaria/Main.java src/grafo_ferroviaria/models/*.java

java --module-path bin --module grafo_ferroviaria/grafo_ferroviaria.Main