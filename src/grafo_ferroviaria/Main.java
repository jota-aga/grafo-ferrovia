package grafo_ferroviaria;

import grafo_ferroviaria.enums.Metrica;
import grafo_ferroviaria.models.*;
import grafo_ferroviaria.view.View;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Grafo grafo = new Grafo();
        View view = new View(grafo);

        try (Scanner scan = new Scanner(new File("ferrovia.txt"), StandardCharsets.UTF_8)) {

            int numVertices = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numVertices; i++) {
                String nome = scan.nextLine().trim();
                grafo.adicionarVertice(nome);
            }

            int numArestas = Integer.parseInt(scan.nextLine().trim());
            int ok = 0, nok = 0;
            for (int i = 0; i < numArestas; i++) {
                String[] p = scan.nextLine().split(",");
                if (p.length != 5) {
                    System.out.println("Aresta ignorada (formato inválido): " + String.join(",", p));
                    nok++;
                    continue;
                }
                String origem    = p[0].trim();
                String destino   = p[1].trim();
                double distancia = Double.parseDouble(p[2].trim());
                double preco     = Double.parseDouble(p[3].trim());
                int tempo        = Integer.parseInt(p[4].trim());

                boolean added = grafo.adicionarAresta(origem, destino, distancia, preco, tempo);
                if (!added) {
                    System.out.printf("Aresta ignorada: '%s' -> '%s' (vértice inexistente)%n", origem, destino);
                    nok++;
                } else {
                    ok++;
                    
                }
            }
            System.out.printf("Arestas adicionadas: %d | ignoradas: %d%n", ok, nok);

        } catch (Exception e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
            return;
        }

        System.out.println("\n=== Grafo carregado ===");
        grafo.exibirGrafo();

        
        view.principal();
    }
}
