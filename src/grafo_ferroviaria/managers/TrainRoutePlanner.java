package grafo_ferroviaria.managers;

import grafo_ferroviaria.models.*;
import java.util.*;

public class TrainRoutePlanner {
    private final RailwayManager railwayManager;

    public TrainRoutePlanner(RailwayManager railwayManager) {
        this.railwayManager = railwayManager;
    }

    public List<TrainStation> planShortestRouteByDistance(String fromStation, String toStation) {
        TrainStation from = railwayManager.stations().get(fromStation);
        TrainStation to = railwayManager.stations().get(toStation);

        if (from == null || to == null) {
            throw new IllegalArgumentException("Estação não encontrada");
        }

        GenericGraph.PathResult<TrainStation> result = railwayManager.graph()
                .shortestPath(from, to, Rail::distance, null);

        if (result.cost == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException(
                    "Não existe caminho entre as estações " + from.name() + " e " + to.name());
        }

        return result.path;
    }

    /**
     * Planeja uma rota entre duas estações usando o caminho mais barato
     */
    public List<TrainStation> planCheapestRoute(String fromStation, String toStation) {
        TrainStation from = railwayManager.stations().get(fromStation);
        TrainStation to = railwayManager.stations().get(toStation);

        if (from == null || to == null) {
            throw new IllegalArgumentException("Estação não encontrada");
        }

        GenericGraph.PathResult<TrainStation> result = railwayManager.graph()
                .shortestPath(from, to, Rail::price, null);

        if (result.cost == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException(
                    "Não existe caminho entre as estações " + from.name() + " e " + to.name());
        }

        return result.path;
    }

    /**
     * Planeja uma rota entre duas estações usando o caminho mais barato
     */
    public List<TrainStation> planMultiStopRoute(List<String> stationNames) {
        List<TrainStation> route = new ArrayList<>();

        for (String stationName : stationNames) {
            TrainStation station = railwayManager.stations().get(stationName);
            if (station == null) {
                throw new IllegalArgumentException("Estação não encontrada: " + stationName);
            }
            route.add(station);
        }

        return route;
    }

    /**
     * Calcula estatísticas de uma rota
     */
    public RouteStatistics calculateRouteStatistics(List<TrainStation> route) {
        if (route.size() < 2) {
            throw new IllegalArgumentException("Rota deve ter pelo menos 2 estações");
        }

        double totalDistance = 0.0;
        double totalPrice = 0.0;
        int totalTime = 0;
        int numStops = route.size() - 1;

        for (int i = 0; i < route.size() - 1; i++) {
            TrainStation from = route.get(i);
            TrainStation to = route.get(i + 1);

            Rail rail = railwayManager.graph().neighbors(from).get(to);
            if (rail == null) {
                throw new IllegalArgumentException("Não existe conexão entre " + from.name() + " e " + to.name());
            }

            totalDistance += rail.distance();
            totalPrice += rail.price();
            totalTime += rail.time();
        }

        return new RouteStatistics(totalDistance, totalPrice, totalTime, numStops);
    }

    /**
     * Calcula estatísticas de uma rota baseado na velocidade máxima de um trem
     * específico
     */
    public RouteStatistics calculateRouteStatisticsForTrain(List<TrainStation> route, double trainMaxSpeed) {
        if (route.size() < 2) {
            throw new IllegalArgumentException("Rota deve ter pelo menos 2 estações");
        }

        double totalDistance = 0.0;
        double totalPrice = 0.0;
        double totalTime = 0.0;
        int numStops = route.size() - 1;

        for (int i = 0; i < route.size() - 1; i++) {
            TrainStation from = route.get(i);
            TrainStation to = route.get(i + 1);

            Rail rail = railwayManager.graph().neighbors(from).get(to);
            if (rail == null) {
                throw new IllegalArgumentException("Não existe conexão entre " + from.name() + " e " + to.name());
            }

            totalDistance += rail.distance();
            totalPrice += rail.price();
            // Calcula o tempo baseado na velocidade máxima do trem
            totalTime += (rail.distance() / trainMaxSpeed) * 60;
        }

        return new RouteStatistics(totalDistance, totalPrice, (int) Math.round(totalTime), numStops);
    }

    /**
     * Classe para armazenar estatísticas de uma rota
     */
    public static class RouteStatistics {
        private final double totalDistance;
        private final double totalPrice;
        private final int totalTime;
        private final int numStops;

        public RouteStatistics(double totalDistance, double totalPrice, int totalTime, int numStops) {
            this.totalDistance = totalDistance;
            this.totalPrice = totalPrice;
            this.totalTime = totalTime;
            this.numStops = numStops;
        }

        public double totalDistance() {
            return totalDistance;
        }

        public double totalPrice() {
            return totalPrice;
        }

        public int totalTime() {
            return totalTime;
        }

        public int numStops() {
            return numStops;
        }

        @Override
        public String toString() {
            return String.format("Distância: %.1f km, Preço: R$ %.2f, Tempo: %d min, Paradas: %d",
                    totalDistance, totalPrice, totalTime, numStops);
        }
    }
}