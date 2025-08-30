package grafo_ferroviaria;

import grafo_ferroviaria.models.*;
import grafo_ferroviaria.view.View;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        HashMap<String, TrainStation> stations = new HashMap<>();
        GenericGraph<TrainStation, Rail> graph = new GenericGraph<>(false);

        try (Scanner scan = new Scanner(new File("ferrovia.txt"), StandardCharsets.UTF_8)) {

            int numVertices = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numVertices; i++) {
                String nome = scan.nextLine().trim();
                stations.put(nome, new TrainStation(nome, 0, 0));
                graph.addVertex(stations.get(nome));
            }

            int numEdges = Integer.parseInt(scan.nextLine().trim());
            int ok = 0, nok = 0;
            for (int i = 0; i < numEdges; i++) {
                String[] p = scan.nextLine().split(",");
                if (p.length != 5) {
                    System.out.println("Aresta ignorada (formato inválido): " + String.join(",", p));
                    nok++;
                    continue;
                }

                TrainStation from = stations.get(p[0].trim());
                TrainStation to = stations.get(p[1].trim());
                double distance = Double.parseDouble(p[2].trim());
                double price = Double.parseDouble(p[3].trim());
                int time = Integer.parseInt(p[4].trim());

                graph.addEdge(from, to, new Rail(price, time, distance, false));
                ok++;
            }

            System.out.printf("Arestas adicionadas: %d | ignoradas: %d%n", ok, nok);

        } catch (Exception e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
            return;
        }

        System.out.println("\n=== grafo carregado ===");
        graph.vertices().forEach(System.out::println);

        View view = new View(graph, stations);

        view.principal();
    }
}
