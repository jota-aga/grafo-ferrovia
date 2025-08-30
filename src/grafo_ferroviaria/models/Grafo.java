package grafo_ferroviaria.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Grafo {
    public enum Metrica { TEMPO, DISTANCIA, PRECO }

    private final Map<String, List<Aresta>> porNome;

    public Grafo() {
        porNome = new HashMap<>();
    }

    public void adicionarVertice(String nome) {
        porNome.put(nome, new ArrayList<>());
    }

    
    public boolean adicionarAresta(String origem, String destino, double distancia, double preco, int tempo) {
        List<Aresta> arestasOrigem = getAresta(origem);
        List<Aresta> arestasDestino = getAresta(destino);

        if (arestasOrigem == null || arestasDestino == null) return false;
        arestasOrigem.add(new Aresta(destino, distancia, preco, tempo));
        return true;
    }

    public void exibirGrafo() {
        for (String nome : porNome.keySet()) {
            System.out.println("Origem: " + nome);
            for (Aresta a : porNome.get(nome)) {
                System.out.printf(
                    "Destino: %s | Preço: %.1f | Distância: %.1fkm | Tempo: %dmin%n",
                    a.getDestino(), a.getPreco(), a.getDistancia(), a.getTempo()
                );
            }
        }
    }

    public ResultadoCaminho menorCaminho(String origem, String destino, Metrica metrica) {
    	// verifica se não existe origem ou destino
        if (!contemVertice(origem) || !contemVertice(destino)) {
            throw new IllegalArgumentException("origem ou destino não existe");
        }

        // quarda a distancia acumulada de cada vertice ate a origem
        Map<String, Double> distancia = new HashMap<>();
        // guarda de qual vertice veio ate chegar nele, para reconstruir o caminho depois
        Map<String, String> prev = new HashMap<>();
        // quem ja foi processado
        Set<String> visitado = new HashSet<>();

        for (String nome : porNome.keySet()) distancia.put(nome, Double.POSITIVE_INFINITY);
        distancia.put(origem, 0.0);

        PriorityQueue<String> filaPrioridade =
            new PriorityQueue<>(Comparator.comparingDouble(distancia::get));
        filaPrioridade.add(origem);

        while (!filaPrioridade.isEmpty()) {
        	// pega o vertice com meno custo total
            String vMenorCusto = filaPrioridade.poll();
            // se ja visitou continua
            if (!visitado.add(vMenorCusto)) continue;
            // se chegou ao destino, para
            if (vMenorCusto.equals(destino)) break;

            List<Aresta> arestas = getAresta(vMenorCusto);
            for (Aresta aresta : arestas) {
                String vizinho = aresta.getDestino();
                if (!porNome.containsKey(vizinho)) continue;

                double peso;
                switch (metrica) {
                    case TEMPO:      peso = aresta.getTempo();      break;
                    case DISTANCIA:  peso = aresta.getDistancia();  break;
                    case PRECO:      peso = aresta.getPreco();      break;
                    default: throw new IllegalArgumentException("Métrica desconhecida: " + metrica);
                }

                double alt = distancia.get(vMenorCusto) + peso;
                if (alt < distancia.get(vizinho)) {
                    distancia.put(vizinho, alt);
                    prev.put(vizinho, vMenorCusto);
                   
                    filaPrioridade.remove(vizinho);
                    filaPrioridade.add(vizinho);
                }
            }
        }

       
        if (Double.isInfinite(distancia.get(destino))) {
            return new ResultadoCaminho(Collections.emptyList(), Double.POSITIVE_INFINITY);
        }

        // percorre o prev de trás pra frente e constroi a lista de vertices
        LinkedList<String> caminho = new LinkedList<>();
        String cur = destino;
        while (cur != null) {
            caminho.addFirst(cur);
            cur = prev.get(cur);
        }

        return new ResultadoCaminho(caminho, distancia.get(destino));
    }

    // entrega o caminho e o custo toal segunda a metrica
    public static class ResultadoCaminho {
        public final List<String> caminho;
        public final double custoTotal;
        public ResultadoCaminho(List<String> caminho, double custoTotal) {
            this.caminho = caminho;
            this.custoTotal = custoTotal;
        }
    }

    
    public List<Aresta> getAresta(String nome) { return porNome.get(nome); }
    public boolean contemVertice(String nome) { return porNome.containsKey(nome); }
}
