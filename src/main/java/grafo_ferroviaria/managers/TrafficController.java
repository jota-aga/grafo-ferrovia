package grafo_ferroviaria.managers;

import grafo_ferroviaria.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficController {
    private final RailwayManager railwayManager;
    private final TrainRoutePlanner routePlanner;
    private TrainSimulator trainSimulator;
    private final Map<TrainStation, Map<TrainStation, List<TrainInfo>>> railOccupancy;

    private final Map<String, TrainPosition> trainPositions;

    private final Map<String, Double> waitingTrains;

    public TrafficController(RailwayManager railwayManager, TrainRoutePlanner routePlanner) {
        this.railwayManager = railwayManager;
        this.routePlanner = routePlanner;
        this.railOccupancy = new ConcurrentHashMap<>();
        this.trainPositions = new ConcurrentHashMap<>();
        this.waitingTrains = new ConcurrentHashMap<>();
    }

    public void setTrainSimulator(TrainSimulator trainSimulator) {
        this.trainSimulator = trainSimulator;
    }

    public void registerTrain(String trainId, Train train) {
        TrainPosition position = new TrainPosition(train.currentStation(), null, 0.0);
        trainPositions.put(trainId, position);
    }

    public void unregisterTrain(String trainId) {
        trainPositions.remove(trainId);
        waitingTrains.remove(trainId);

        for (Map<TrainStation, List<TrainInfo>> toMap : railOccupancy.values()) {
            for (List<TrainInfo> trainList : toMap.values()) {
                trainList.removeIf(info -> info.trainId.equals(trainId));
            }
        }
    }

    public void updateTrainPosition(String trainId, Train train, double deltaTime) {
        TrainPosition currentPos = trainPositions.get(trainId);
        if (currentPos == null)
            return;

        TrainStation currentStation = train.currentStation();
        TrainStation nextStation = train.getNextStation();

        if (nextStation == null)
            return;

        updateRailOccupancyTimes(deltaTime);

        if (currentPos.currentRail == null && !isTrainWaiting(trainId)) {
            if (canEnterRail(trainId, currentStation, nextStation)) {
                enterRail(trainId, currentStation, nextStation, train.timeToNextStation());
                currentPos.currentRail = new RailSegment(currentStation, nextStation);
                currentPos.timeInRail = 0.0;

                if (!train.isMoving()) {
                    train.startMoving();
                }
            } else {
                handleCollision(trainId, currentStation, nextStation, train);
            }
        }

        if (currentPos.currentRail != null) {
            currentPos.timeInRail += deltaTime;

            if (currentPos.timeInRail >= train.timeToNextStation()) {
                exitRail(trainId, currentPos.currentRail.from, currentPos.currentRail.to);
                currentPos.currentRail = null;
                currentPos.timeInRail = 0.0;
            }
        }

        currentPos.currentStation = currentStation;
        currentPos.nextStation = nextStation;
    }

    private boolean canEnterRail(String trainId, TrainStation from, TrainStation to) {
        List<TrainInfo> trainsInRail = getTrainsInRail(from, to);

        if (trainsInRail.isEmpty()) {
            return true;
        }

        for (TrainInfo info : trainsInRail) {
            if (info.trainId.equals(trainId)) {
                continue;
            }

            return false;
        }

        return true;
    }

    private void enterRail(String trainId, TrainStation from, TrainStation to, double timeInRail) {
        railOccupancy.computeIfAbsent(from, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(to, k -> new ArrayList<>())
                .add(new TrainInfo(trainId, timeInRail));
    }

    private void exitRail(String trainId, TrainStation from, TrainStation to) {
        Map<TrainStation, List<TrainInfo>> toMap = railOccupancy.get(from);
        if (toMap != null) {
            List<TrainInfo> trainList = toMap.get(to);
            if (trainList != null) {
                trainList.removeIf(info -> info.trainId.equals(trainId));
            }
        }
    }

    private List<TrainInfo> getTrainsInRail(TrainStation from, TrainStation to) {
        Map<TrainStation, List<TrainInfo>> toMap = railOccupancy.get(from);
        if (toMap != null) {
            List<TrainInfo> trainList = toMap.get(to);
            if (trainList != null) {
                return new ArrayList<>(trainList);
            }
        }
        return new ArrayList<>();
    }

    public List<TrainInfo> getTrainsInRailPublic(TrainStation from, TrainStation to) {
        return getTrainsInRail(from, to);
    }

    private void handleCollision(String trainId, TrainStation from, TrainStation to, Train train) {
        double waitTime = calculateWaitTime(from, to);

        double alternativeTime = calculateAlternativeRouteTime(from, to, train);

        if (alternativeTime < waitTime) {
            List<TrainStation> alternativeRoute = findAlternativeRoute(from, to, train);
            if (alternativeRoute != null) {                
                updateTrainRoute(trainId, alternativeRoute);
            } else {
                waitingTrains.put(trainId, waitTime);
            }
        } else {
            waitingTrains.put(trainId, waitTime);
        }
    }

    private double calculateWaitTime(TrainStation from, TrainStation to) {
        List<TrainInfo> trainsInRail = getTrainsInRail(from, to);
        if (trainsInRail.isEmpty()) {
            return 0.0;
        }

        return trainsInRail.stream()
                .filter(info -> info.remainingTime > 0)
                .mapToDouble(info -> info.remainingTime)
                .max()
                .orElse(0.0);
    }

    private double calculateAlternativeRouteTime(TrainStation from, TrainStation to, Train train) {
        try {
            List<TrainStation> alternativeRoute = findAlternativeRoute(from, to, train);
            if (alternativeRoute != null) {
                TrainRoutePlanner.RouteStatistics stats = routePlanner.calculateRouteStatisticsForTrain(
                        alternativeRoute, train.maxSpeed());
                return stats.totalTime();
            }
        } catch (Exception e) {
        }
        return Double.POSITIVE_INFINITY;
    }

    private List<TrainStation> findAlternativeRoute(TrainStation from, TrainStation to, Train train) {
       
        List<TrainStation> currentRoute = train.route();
        int currentIndex = train.currentRouteIndex();

        if (currentIndex >= currentRoute.size() - 1) {
            return null;
        }

        TrainStation destination = currentRoute.get(currentRoute.size() - 1);

        try {
            return railwayManager.graph().shortestPathExcludingEdge(
                    from, destination, Rail::distance, from, to).path;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateTrainRoute(String trainId, List<TrainStation> newRoute) {
        if (trainSimulator != null) {
            Train train = trainSimulator.getAllTrains().stream()
                    .filter(t -> t.id().equals(trainId))
                    .findFirst()
                    .orElse(null);

            if (train != null) {
                train.updateRoute(newRoute);
            }
        }
    }

    public boolean isTrainWaiting(String trainId) {
        return waitingTrains.containsKey(trainId);
    }

    public double getWaitingTime(String trainId) {
        return waitingTrains.getOrDefault(trainId, 0.0);
    }

    public void updateWaitingTimes(double deltaTime) {
        for (Map.Entry<String, Double> entry : waitingTrains.entrySet()) {
            double remainingTime = entry.getValue() - deltaTime;
            if (remainingTime <= 0) {
                String trainId = entry.getKey();
                waitingTrains.remove(trainId);
            } else {
                entry.setValue(remainingTime);
            }
        }
    }

    private void updateRailOccupancyTimes(double deltaTime) {
        for (Map<TrainStation, List<TrainInfo>> toMap : railOccupancy.values()) {
            for (List<TrainInfo> trainList : toMap.values()) {
                for (TrainInfo info : trainList) {
                    info.updateRemainingTime(deltaTime);
                }
                trainList.removeIf(info -> info.remainingTime <= 0);
            }
        }
    }

    private static class TrainInfo {
        final String trainId;
        double remainingTime;

        TrainInfo(String trainId, double remainingTime) {
            this.trainId = trainId;
            this.remainingTime = remainingTime;
        }

        void updateRemainingTime(double deltaTime) {
            remainingTime -= deltaTime;
        }
    }

    private static class TrainPosition {
        TrainStation currentStation;
        TrainStation nextStation;
        RailSegment currentRail;
        double timeInRail;

        TrainPosition(TrainStation currentStation, TrainStation nextStation, double timeInRail) {
            this.currentStation = currentStation;
            this.nextStation = nextStation;
            this.currentRail = null;
            this.timeInRail = timeInRail;
        }
    }

    private static class RailSegment {
        final TrainStation from;
        final TrainStation to;

        RailSegment(TrainStation from, TrainStation to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            RailSegment that = (RailSegment) obj;
            return Objects.equals(from, that.from) && Objects.equals(to, that.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }
}