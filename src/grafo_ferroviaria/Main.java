package grafo_ferroviaria;

import grafo_ferroviaria.models.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        HashMap<String, TrainStation> stations = new HashMap<>();
        GenericGraph<TrainStation, Rail> grafo = new GenericGraph<>(false);

        try (Scanner scan = new Scanner(new File("ferrovia.txt"), StandardCharsets.UTF_8)) {

            int numVertices = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numVertices; i++) {
                String nome = scan.nextLine().trim();
                stations.put(nome, new TrainStation(nome, 0, 0));
                grafo.addVertex(stations.get(nome));
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

                TrainStation origem = stations.get(p[0].trim());
                TrainStation destino = stations.get(p[1].trim());
                double distancia = Double.parseDouble(p[2].trim());
                double preco = Double.parseDouble(p[3].trim());
                int tempo = Integer.parseInt(p[4].trim());

                grafo.addEdge(origem, destino, new Rail(preco, tempo, distancia, false));
                ok++;
            }

            System.out.printf("Arestas adicionadas: %d | ignoradas: %d%n", ok, nok);

        } catch (Exception e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
            return;
        }

        System.out.println("\n=== Grafo carregado ===");
        grafo.vertices().forEach(System.out::println);

        try {
            var r = grafo.shortestPath(stations.get("Jabaquara"), stations.get("BarraFunda"),
                    Rail::time, null);
            if (Double.isInfinite(r.cost)) {
                System.out.println("\nNão existe caminho entre Jabaquara e BarraFunda.");
            } else {
                System.out.println("\n=== Menor caminho (TEMPO) ===");
                System.out.println(String.join(" -> ", r.path.stream().map(TrainStation::name).toList()));
                System.out.printf("Tempo total: %.2f min%n", r.cost);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\nErro no cálculo de rota: " + e.getMessage());
        }
    }
}
