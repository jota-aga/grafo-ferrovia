package grafo_ferroviaria.managers;

import grafo_ferroviaria.models.Rail;
import grafo_ferroviaria.models.Schedule;
import grafo_ferroviaria.models.TrainStation;
import grafo_ferroviaria.models.GenericGraph;
import grafo_ferroviaria.models.Train;
import grafo_ferroviaria.models.TrainLine;
import grafo_ferroviaria.models.PointHistory;
import grafo_ferroviaria.models.RouteResult;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.Scanner;
import java.util.function.ToDoubleFunction;

public class RailwayManager {
    private static final int DAY = 1440;

    private final GenericGraph<TrainStation, Rail> railway_graph;
    private final GenericGraph<PointHistory, TrainLine> train_graph;
    private final HashMap<String, TrainStation> stations;
    private final HashMap<String, Train> trains;
    private final HashMap<String, List<Schedule>> schedules;

    private final Map<TrainStation, Map<Integer, PointHistory>> nodeByStationTime;
    private final Map<TrainStation, NavigableSet<Integer>> timesByStation;

    public RailwayManager() {
        this.railway_graph = new GenericGraph<>(false);
        this.train_graph   = new GenericGraph<>(true);
        this.stations = new HashMap<>();
        this.trains = new HashMap<>();
        this.schedules = new HashMap<>();
        this.nodeByStationTime = new HashMap<>();
        this.timesByStation = new HashMap<>();
    }

    public void loadRailway(String path) {
        try (Scanner scan = new Scanner(new File(path), StandardCharsets.UTF_8)) {
            int numVertex = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numVertex; i++) {
                String name = scan.nextLine().trim();
                this.stations.put(name, new TrainStation(name));
                this.railway_graph.addVertex(this.stations.get(name));
            }

            int numEdges = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numEdges; i++) {
                String[] p = scan.nextLine().split(",");
                if (p.length != 3) {
                    System.out.println("Edge ignored (invalid format): " + String.join(",", p));
                    continue;
                }

                TrainStation from = this.stations.get(p[0].trim());
                TrainStation to   = this.stations.get(p[1].trim());
                double distance   = Double.parseDouble(p[2].trim());

                this.railway_graph.addEdge(from, to, new Rail(distance));
            }

            int numTrains = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numTrains; i++) {
                String[] p = scan.nextLine().split(",");

                if (p.length != 6) {
                    System.out.println("Train ignored (invalid format): " + String.join(",", p));
                    continue;
                }

                String id            = p[0].trim();
                double averageSpeed  = Double.parseDouble(p[1].trim());
                double pricePerKm    = Double.parseDouble(p[2].trim());

                this.trains.put(id, new Train(id, averageSpeed, pricePerKm, new ArrayList<>()));
            }

            int numSchedules = Integer.parseInt(scan.nextLine().trim());
            for (int i = 0; i < numSchedules; i++) {
                String[] p = scan.nextLine().split(",");

                if (p.length != 5) {
                    System.out.println("Schedule ignored (invalid formato): " + String.join(",", p));
                    continue;
                }

                String trainId       = p[0].trim();
                String stationAId    = p[1].trim();
                String stationBId    = p[2].trim();
                String departureHour = p[3].trim();
                String arrivalHour   = p[4].trim();

                Train train = this.trains.get(trainId);
                if (train == null) {
                    System.out.println("Schedule ignored (unknown train): " + trainId);
                    continue;
                }

                Schedule sch = new Schedule(
                    train,
                    this.stations.get(stationAId),
                    this.stations.get(stationBId),
                    departureHour,
                    arrivalHour
                );
                train.addSchedule(sch);
                this.schedules.put(trainId, train.getSchedules());
            }

            for (List<Schedule> scheduleList : schedules.values()) {
                for (Schedule sch : scheduleList) {
                    TrainStation A = sch.getPointA();
                    TrainStation B = sch.getPointB();

                    int depMinutes = parseTimeToMinutes(sch.getDepartureHour());
                    int arrMinutes = parseTimeToMinutes(sch.getArrivalHour());

                    if (arrMinutes <= depMinutes) {
                        arrMinutes += DAY;
                    }

                    PointHistory u = getOrCreate(A, depMinutes);
                    PointHistory v = getOrCreate(B, arrMinutes);

                    Rail rail = this.railway_graph.neighbors(A).get(B);
                    if (rail == null) {
                        System.out.println("Waning: Edge not found ignored: " + A + " -> " + B);
                        continue;
                    }

                    double distanceKm = rail.distance();
                    double price      = distanceKm * sch.getTrain().pricePerKm();
                    int travel        = arrMinutes - depMinutes;

                    this.train_graph.addEdge(u, v, new TrainLine(sch.getTrain(), travel, price));
                }
            }

            for (var entry : timesByStation.entrySet()) {
                TrainStation s = entry.getKey();
                NavigableSet<Integer> times = entry.getValue();

                Integer prev = null;
                for (Integer t : times) {
                    if (prev != null) {
                        PointHistory a = getOrCreate(s, prev);
                        PointHistory b = getOrCreate(s, t);
                        int dt = t - prev;
                        if (dt > 0) {
                            this.train_graph.addEdge(a, b, new TrainLine(null, dt, 0.0));
                        }
                    }
                    prev = t;
                }
            }

        } catch (Exception e) {
            System.out.println("Error to load railway file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int parseTimeToMinutes(String timeStr) {
        String s = timeStr.trim();
        int idx = s.indexOf("(+");
        if (idx >= 0) {
            s = s.substring(0, idx).trim();
        }
        String[] parts = s.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid hourly format: " + timeStr);
        }
        int hours = Integer.parseInt(parts[0].trim());
        int minutes = Integer.parseInt(parts[1].trim());
        if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Invalid hourly: " + timeStr);
        }
        return hours * 60 + minutes;
    }

    private PointHistory getOrCreate(TrainStation s, int t) {
        return this.nodeByStationTime
            .computeIfAbsent(s, k -> new HashMap<>())
            .computeIfAbsent(t, k -> {
                PointHistory ph = new PointHistory(s, t);
                this.train_graph.addVertex(ph);
                this.timesByStation.computeIfAbsent(s, k2 -> new TreeSet<>()).add(t);
                return ph;
            });
    }

    public PointHistory addOriginNode(String stationName, String departureHour) {
        TrainStation station = stations.get(stationName);
        if (station == null) {
            throw new IllegalArgumentException("Station not found: " + stationName);
        }   
    
        int t0 = parseTimeToMinutes(departureHour);
    
        Map<Integer, PointHistory> byTime = this.nodeByStationTime.get(station);
        if (byTime != null) {
            PointHistory exact = byTime.get(t0);
            if (exact != null) {
                return exact;
            }
        }
    
        PointHistory origin = new PointHistory(station, t0);
        this.train_graph.addVertex(origin);
    
        NavigableSet<Integer> times = this.timesByStation.get(station);
        if (times != null && !times.isEmpty()) {
            Integer next = times.ceiling(t0);
            if (next != null) {
                PointHistory nextNode = this.nodeByStationTime.get(station).get(next);
                int wait = next - t0;
                this.train_graph.addEdge(origin, nextNode, new TrainLine(null, wait, 0.0));
            }
        }
        return origin;
    }

    public RouteResult findFastestRoute(String startStation, String endStation, String startHour) {
        TrainStation start = stations.get(startStation);
        TrainStation end   = stations.get(endStation);
        if (start == null || end == null) {
            throw new IllegalArgumentException("Station not found");
        }

        PointHistory origin = addOriginNode(startStation, startHour);

        ToDoubleFunction<TrainLine> timeCost = TrainLine::getTime;
        Map<PointHistory, Double> distances = train_graph.dijkstra(origin, timeCost, null);

        PointHistory bestDestination = null;
        double minTime = Double.POSITIVE_INFINITY;

        for (PointHistory node : train_graph.vertices()) {
            if (node.getTrainStation().equals(end)) {
                double time = distances.getOrDefault(node, Double.POSITIVE_INFINITY);
                if (time < minTime) {
                    minTime = time;
                    bestDestination = node;
                }
            }
        }

        if (bestDestination == null || Double.isInfinite(minTime)) {
            return null;
        }

        List<PointHistory> path = reconstructPath(origin, bestDestination, distances, timeCost);
        return new RouteResult(path, minTime, calculateTotalPrice(path), calculateTotalDistance(path));
    }

    public RouteResult findCheapestRoute(String startStation, String endStation, String startHour) {
        TrainStation start = stations.get(startStation);
        TrainStation end   = stations.get(endStation);
        if (start == null || end == null) {
            throw new IllegalArgumentException("Station not found");
        }

        PointHistory origin = addOriginNode(startStation, startHour);

        ToDoubleFunction<TrainLine> priceCost = TrainLine::getPrice;
        Map<PointHistory, Double> distances = train_graph.dijkstra(origin, priceCost, null);

        PointHistory bestDestination = null;
        double minPrice = Double.POSITIVE_INFINITY;

        for (PointHistory node : train_graph.vertices()) {
            if (node.getTrainStation().equals(end)) {
                double price = distances.getOrDefault(node, Double.POSITIVE_INFINITY);
                if (price < minPrice) {
                    minPrice = price;
                    bestDestination = node;
                }
            }
        }

        if (bestDestination == null || Double.isInfinite(minPrice)) {
            return null;
        }

        List<PointHistory> path = reconstructPath(origin, bestDestination, distances, priceCost);
        return new RouteResult(path, calculateTotalTime(path), minPrice, calculateTotalDistance(path));
    }

    private List<PointHistory> reconstructPath(
            PointHistory source,
            PointHistory destination,
            Map<PointHistory, Double> dist,
            ToDoubleFunction<TrainLine> costFn
    ) {
        List<PointHistory> rev = new ArrayList<>();
        PointHistory cur = destination;

        final double EPS = 1e-7;

        while (cur != null && !cur.equals(source)) {
            rev.add(cur);

            PointHistory bestPred = null;
            double bestDist = Double.POSITIVE_INFINITY;

            for (PointHistory u : train_graph.vertices()) {
                Map<PointHistory, TrainLine> out = train_graph.neighbors(u);
                TrainLine e = out.get(cur);
                if (e == null) continue;

                double du = dist.getOrDefault(u, Double.POSITIVE_INFINITY);
                double w  = costFn.applyAsDouble(e);
                double dv = dist.getOrDefault(cur, Double.POSITIVE_INFINITY);

                if (!Double.isFinite(du) || !Double.isFinite(dv)) continue;

                if (Math.abs((du + w) - dv) <= EPS && du < bestDist) {
                    bestDist = du;
                    bestPred = u;
                }
            }

            if (bestPred == null) {
                return new ArrayList<>();
            }
            cur = bestPred;
        }

        if (cur == null) return new ArrayList<>();
        rev.add(source);

        List<PointHistory> path = new ArrayList<>();
        for (int i = rev.size() - 1; i >= 0; i--) path.add(rev.get(i));
        return path;
    }

    private double calculateTotalTime(List<PointHistory> path) {
        if (path == null || path.size() < 2) return 0.0;
        PointHistory first = path.get(0);
        PointHistory last  = path.get(path.size() - 1);
        return last.getTimestamp() - first.getTimestamp();
    }

    private double calculateTotalPrice(List<PointHistory> path) {
        double totalPrice = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            PointHistory from = path.get(i);
            PointHistory to   = path.get(i + 1);
            TrainLine edge = train_graph.neighbors(from).get(to);
            if (edge != null) {
                totalPrice += edge.getPrice();
            }
        }
        return totalPrice;
    }

    private double calculateTotalDistance(List<PointHistory> path) {
        double totalDistance = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            PointHistory from = path.get(i);
            PointHistory to   = path.get(i + 1);
            TrainLine edge = train_graph.neighbors(from).get(to);
            if (edge != null && edge.getTrain() != null) {
                double timeInHours = edge.getTime() / 60.0;
                double distance = edge.getTrain().getAverageSpeed() * timeInHours;
                totalDistance += distance;
            }
        }
        return totalDistance;
    }

    public void printTrainGraph() {
        System.out.println("\n=== VISUALIZAÇÃO DO GRAFO TRAIN_GRAPH ===");
        System.out.println("Total de vértices: " + train_graph.vertices().size());
        System.out.println("\nVértices (Estações + Horários):");

        for (PointHistory vertex : train_graph.vertices()) {
            String timeStr = formatTime(vertex.getTimestamp());
            System.out.printf("- %s às %s\n", vertex.getTrainStation().name(), timeStr);
        }

        System.out.println("\nArestas (Conexões entre estações):");
        int edgeCount = 0;
        for (PointHistory vertex : train_graph.vertices()) {
            Map<PointHistory, TrainLine> neighbors = train_graph.neighbors(vertex);
            for (Map.Entry<PointHistory, TrainLine> entry : neighbors.entrySet()) {
                PointHistory neighbor = entry.getKey();
                TrainLine edge = entry.getValue();

                String fromTime = formatTime(vertex.getTimestamp());
                String toTime   = formatTime(neighbor.getTimestamp());

                System.out.printf("%d. %s (%s) → %s (%s) | ",
                    ++edgeCount,
                    vertex.getTrainStation().name(), fromTime,
                    neighbor.getTrainStation().name(), toTime);

                if (edge.getTrain() != null) {
                    System.out.printf("Trem: %s | Tempo: %d min | Preço: R$ %.2f\n",
                        edge.getTrain().getId(), edge.getTime(), edge.getPrice());
                } else {
                    System.out.printf("Espera: %d min | Preço: R$ %.2f\n",
                        edge.getTime(), edge.getPrice());
                }
            }
        }

        System.out.println("\nTotal de arestas: " + edgeCount);
    }

    private String formatTime(int minutes) {
        int totalMinutes = minutes;
        int days = totalMinutes / DAY;
        int dayMinutes = totalMinutes % DAY;

        int hours = dayMinutes / 60;
        int mins  = dayMinutes % 60;

        if (days > 0) {
            return String.format("%02d:%02d (+%d)", hours, mins, days);
        } else {
            return String.format("%02d:%02d", hours, mins);
        }
    }

    public GenericGraph<TrainStation, Rail> getRailwayGraph() {
        return railway_graph;
    }

    public GenericGraph<PointHistory, TrainLine> getTrainGraph() {
        return train_graph;
    }
    
    public HashMap<String, TrainStation> getStations() {
        return stations;
    }

    public HashMap<String, Train> getTrains() {
        return trains;
    }
}
