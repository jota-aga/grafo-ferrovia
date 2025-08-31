package grafo_ferroviaria.models;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class GenericGraph<V, E> {
    private final boolean directed;

    private final Map<V, Map<V, E>> adj = new HashMap<>();

    public GenericGraph(boolean directed) {
        this.directed = directed;
    }

    public void addVertex(V v) {
        adj.putIfAbsent(v, new HashMap<>());
    }

    public void addEdge(V from, V to, E data) {
        addVertex(from);
        addVertex(to);
        adj.get(from).put(to, data);
        if (!directed)
            adj.get(to).put(from, data);
    }

    public boolean hasVertex(V v) {
        return adj.containsKey(v);
    }

    public Map<V, E> neighbors(V v) {
        return adj.getOrDefault(v, Map.of());
    }

    public Set<V> vertices() {
        return Collections.unmodifiableSet(adj.keySet());
    }

    /* ==================== Dijkstra genérico ==================== */
    /**
     * @param cost    Função que transforma E (dados da aresta) em custo (>= 0).
     * @param allowed (Opcional) Filtro para permitir/bloquear arestas (ex.:
     *                “somente trem rápido”).
     * @return distâncias mínimas a partir de source.
     */
    public Map<V, Double> dijkstraDistances(
            V source,
            ToDoubleFunction<? super E> cost,
            Predicate<? super E> allowed) {
        requireVertex(source);

        Map<V, Double> dist = new HashMap<>();
        for (V v : adj.keySet())
            dist.put(v, Double.POSITIVE_INFINITY);
        dist.put(source, 0.0);

        PriorityQueue<V> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        pq.add(source);

        while (!pq.isEmpty()) {
            V u = pq.poll();
            double du = dist.get(u);
            for (Map.Entry<V, E> e : neighbors(u).entrySet()) {
                V v = e.getKey();
                E data = e.getValue();
                if (allowed != null && !allowed.test(data))
                    continue;

                double w = cost.applyAsDouble(data);
                if (w < 0)
                    throw new IllegalArgumentException("Dijkstra exige custos não negativos.");

                double alt = du + w;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    // atualização simples de prioridade (remove/insere)
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }
        return dist;
    }

    public PathResult<V> shortestPath(
            V source, V target,
            ToDoubleFunction<? super E> cost,
            Predicate<? super E> allowed) {
        requireVertex(source);
        requireVertex(target);

        Map<V, Double> dist = new HashMap<>();
        Map<V, V> prev = new HashMap<>();
        for (V v : adj.keySet())
            dist.put(v, Double.POSITIVE_INFINITY);
        dist.put(source, 0.0);

        PriorityQueue<V> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        pq.add(source);

        while (!pq.isEmpty()) {
            V u = pq.poll();
            if (u.equals(target))
                break;

            double du = dist.get(u);
            for (Map.Entry<V, E> e : neighbors(u).entrySet()) {
                V v = e.getKey();
                E data = e.getValue();
                if (allowed != null && !allowed.test(data))
                    continue;

                double w = cost.applyAsDouble(data);
                if (w < 0)
                    throw new IllegalArgumentException("Dijkstra exige custos não negativos.");

                double alt = du + w;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        double d = dist.get(target);
        if (Double.isInfinite(d))
            return new PathResult<>(List.of(), Double.POSITIVE_INFINITY);

        List<V> path = new ArrayList<>();
        for (V at = target; at != null; at = prev.get(at)) {
            path.add(at);
            if (at.equals(source))
                break;
        }
        Collections.reverse(path);
        return new PathResult<>(path, d);
    }

    /**
     * Encontra o caminho mais curto excluindo uma aresta específica
     */
    public PathResult<V> shortestPathExcludingEdge(
            V source, V target,
            ToDoubleFunction<? super E> cost,
            V excludedFrom, V excludedTo) {
        requireVertex(source);
        requireVertex(target);

        Map<V, Double> dist = new HashMap<>();
        Map<V, V> prev = new HashMap<>();
        for (V v : adj.keySet())
            dist.put(v, Double.POSITIVE_INFINITY);
        dist.put(source, 0.0);

        PriorityQueue<V> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        pq.add(source);

        while (!pq.isEmpty()) {
            V u = pq.poll();
            if (u.equals(target))
                break;

            double du = dist.get(u);
            for (Map.Entry<V, E> e : neighbors(u).entrySet()) {
                V v = e.getKey();
                E data = e.getValue();

                // Exclui a aresta específica
                if (u.equals(excludedFrom) && v.equals(excludedTo))
                    continue;

                double w = cost.applyAsDouble(data);
                if (w < 0)
                    throw new IllegalArgumentException("Dijkstra exige custos não negativos.");

                double alt = du + w;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        double d = dist.get(target);
        if (Double.isInfinite(d))
            return new PathResult<>(List.of(), Double.POSITIVE_INFINITY);

        List<V> path = new ArrayList<>();
        for (V at = target; at != null; at = prev.get(at)) {
            path.add(at);
            if (at.equals(source))
                break;
        }
        Collections.reverse(path);
        return new PathResult<>(path, d);
    }

    private void requireVertex(V v) {
        if (!adj.containsKey(v))
            throw new IllegalArgumentException("Vértice inexistente: " + v);
    }

    public static final class PathResult<V> {
        public final List<V> path;
        public final double cost;

        public PathResult(List<V> path, double cost) {
            this.path = path;
            this.cost = cost;
        }

        @Override
        public String toString() {
            return "cost=" + cost + ", path=" + path;
        }
    }

    /* ==================== Helpers p/ custos genéricos ==================== */
    /** Extrator numérico de atributo (ex.: Rail::price), útil p/ composições. */
    public interface Feature<E> extends ToDoubleFunction<E> {
        static <E> Feature<E> of(ToDoubleFunction<E> f) {
            return f::applyAsDouble;
        }

        /** Soma ponderada de features (w1*f1 + w2*f2 + ...) */
        static <E> ToDoubleFunction<E> weighted(Map<Feature<E>, Double> weights) {
            return e -> {
                double s = 0.0;
                for (var entry : weights.entrySet()) {
                    s += entry.getValue() * entry.getKey().applyAsDouble(e);
                }
                return s;
            };
        }

        /**
         * Custo lexicográfico (min f1; empate -> f2; depois f3...).
         * Dijkstra puro não é lexicográfico, mas você pode usar isso para comparar
         * caminhos na pós-análise
         * ou adaptar para algoritmos multi-critério. Mantido aqui como utilitário.
         */
    }
}
