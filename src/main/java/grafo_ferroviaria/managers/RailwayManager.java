package grafo_ferroviaria.managers;

import grafo_ferroviaria.models.Rail;
import grafo_ferroviaria.models.TrainStation;
import grafo_ferroviaria.models.TrainStationType;
import grafo_ferroviaria.models.GenericGraph;
import grafo_ferroviaria.models.Train;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RailwayManager {
    private final GenericGraph<TrainStation, Rail> graph;
    private final HashMap<String, TrainStation> stations;
    private final TrainSimulator trainSimulator;

    public RailwayManager(boolean isDirected) {
        this.graph = new GenericGraph<>(isDirected);
        this.stations = new HashMap<>();
        this.trainSimulator = new TrainSimulator(this);
    }

    public java.util.List<TrainStation> getTrainRoute(String trainId) {
        for (Train t : getAllTrains()) {
            if (t.id().equals(trainId)) {
                return new java.util.ArrayList<>(t.route());
            }
        }
        return java.util.Collections.emptyList();
    }

    public enum RouteCriterion { TIME, DISTANCE, PRICE }

    public List<TrainStation> planRoute(String fromStation, String toStation, RouteCriterion criterion) {
        TrainRoutePlanner planner = new TrainRoutePlanner(this);
        return switch (criterion) {
            case TIME -> planner.planFastestRouteByTime(fromStation, toStation);
            case DISTANCE -> planner.planShortestRouteByDistance(fromStation, toStation);
            case PRICE -> planner.planCheapestRoute(fromStation, toStation);
        };
    }

    public void loadRailway(String path) {
        try (Scanner scan = new Scanner(new File(path), StandardCharsets.UTF_8)) {
            int numVertex = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numVertex; i++) {
                String name = scan.nextLine().trim();
                this.stations.put(name, new TrainStation(name, 0, 0, TrainStationType.MIXED));
                this.graph.addVertex(this.stations.get(name));
            }

            int numEdges = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numEdges; i++) {
                String[] p = scan.nextLine().split(",");
                if (p.length != 5) {
                    System.out.println("Aresta ignorada (formato inválido): " + String.join(",", p));
                    continue;
                }

                TrainStation from = this.stations.get(p[0].trim());
                TrainStation to = this.stations.get(p[1].trim());
                double distance = Double.parseDouble(p[2].trim());
                double price = Double.parseDouble(p[3].trim());
                int time = Integer.parseInt(p[4].trim());

                this.graph.addEdge(from, to, new Rail(price, time, distance, false));
            }

        } catch (Exception e) {
            System.out.println("Error to load railway file: " + e.getMessage());
            return;
        }
    }

    public GenericGraph<TrainStation, Rail> graph() {
        return this.graph;
    }

    public HashMap<String, TrainStation> stations() {
        return this.stations;
    }

    public void addTrain(String trainId, double maxSpeed, int capacity,
            String startingStationName, List<String> routeStationNames) {
        TrainStation startingStation = stations.get(startingStationName);
        if (startingStation == null) {
            throw new IllegalArgumentException("Estação inicial não encontrada: " + startingStationName);
        }

        List<TrainStation> route = new ArrayList<>();
        for (String stationName : routeStationNames) {
            TrainStation station = stations.get(stationName);
            if (station == null) {
                throw new IllegalArgumentException("Estação não encontrada: " + stationName);
            }
            route.add(station);
        }

        trainSimulator.addTrain(trainId, maxSpeed, capacity, startingStation, route);
    }

    public void removeTrain(String trainId) {
        trainSimulator.removeTrain(trainId);
    }

    public void startTrain(String trainId) {
        trainSimulator.startTrain(trainId);
    }

    public void stopTrain(String trainId) {
        trainSimulator.stopTrain(trainId);
    }

    public void updateSimulation(double deltaTime) {
        trainSimulator.updateSimulation(deltaTime);
    }

    public Map<String, TrainSimulator.TrainStatus> getTrainStatus() {
        return trainSimulator.getTrainStatus();
    }

    public TrainSimulator.TrainStatus getTrainStatus(String trainId) {
        return trainSimulator.getTrainStatus(trainId);
    }

    public Collection<Train> getAllTrains() {
        return trainSimulator.getAllTrains();
    }

    public TrainSimulator getTrainSimulator() {
        return trainSimulator;
    }

    public List<TrainStation> planFastestRouteForTrain(String fromStation, String toStation) {
        TrainRoutePlanner planner = new TrainRoutePlanner(this);
        return planner.planFastestRouteByTime(fromStation, toStation);
    }

    public TrainRoutePlanner.RouteStatistics calculateRouteStatisticsForTrain(List<TrainStation> route,
            double trainMaxSpeed) {
        TrainRoutePlanner planner = new TrainRoutePlanner(this);
        return planner.calculateRouteStatisticsForTrain(route, trainMaxSpeed);
    }

    public TrainRoutePlanner.RouteStatistics calculateRouteStatisticsForExistingTrain(List<TrainStation> route,
            String trainId) {
        Train train = trainSimulator.getAllTrains().stream()
                .filter(t -> t.id().equals(trainId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Trem não encontrado: " + trainId));

        return calculateRouteStatisticsForTrain(route, train.maxSpeed());
    }
}
