package grafo_ferroviaria.managers;

import grafo_ferroviaria.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrainSimulator {
    private final RailwayManager railwayManager;
    private final TrainRoutePlanner routePlanner;
    private final TrafficController trafficController;
    private final Map<String, Train> trains;
    private final Map<String, List<TrainStation>> trainRoutes;
    private double simulationTime;

    public TrainSimulator(RailwayManager railwayManager) {
        this.railwayManager = railwayManager;
        this.routePlanner = new TrainRoutePlanner(railwayManager);
        this.trafficController = new TrafficController(railwayManager, routePlanner);
        this.trains = new ConcurrentHashMap<>();
        this.trainRoutes = new HashMap<>();
        this.simulationTime = 0.0;

        this.trafficController.setTrainSimulator(this);
    }

    public void addTrain(String trainId, double maxSpeed, int capacity,
            TrainStation startingStation, List<TrainStation> route) {
        Train train = new Train(trainId, maxSpeed, capacity, startingStation, route);
        trains.put(trainId, train);
        trainRoutes.put(trainId, new ArrayList<>(route));
        trafficController.registerTrain(trainId, train);
    }

    public void removeTrain(String trainId) {
        trains.remove(trainId);
        trainRoutes.remove(trainId);
        trafficController.unregisterTrain(trainId);
    }

    public void startTrain(String trainId) {
        Train train = trains.get(trainId);
        if (train != null) {
            train.startMoving();
            calculateTimeToNextStation(train);
        }
    }

    public void stopTrain(String trainId) {
        Train train = trains.get(trainId);
        if (train != null) {
            train.stop();
        }
    }

    private void calculateTimeToNextStation(Train train) {
        TrainStation current = train.currentStation();
        TrainStation next = train.getNextStation();

        if (next != null) {
            Rail rail = railwayManager.graph().neighbors(current).get(next);
            if (rail != null) {
                double distance = rail.distance();
                double time = (distance / train.maxSpeed()) * 60;
                train.setTimeToNextStation(time);
            }
        }
    }

    public void updateSimulation(double deltaTime) {
        simulationTime += deltaTime;

        trafficController.updateWaitingTimes(deltaTime);

        for (Train train : trains.values()) {
            if (!train.isMoving() && !train.hasReachedDestination() && !trafficController.isTrainWaiting(train.id())) {
                calculateTimeToNextStation(train);
            }

            trafficController.updateTrainPosition(train.id(), train, deltaTime);

            if (!trafficController.isTrainWaiting(train.id())) {
                train.updatePosition(deltaTime);
            }
        }
    }

    public Map<String, TrainStatus> getTrainStatus() {
        Map<String, TrainStatus> status = new HashMap<>();

        for (Map.Entry<String, Train> entry : trains.entrySet()) {
            Train train = entry.getValue();
            TrainStation current = train.currentStation();
            TrainStation next = train.getNextStation();

            status.put(entry.getKey(), new TrainStatus(
                    train.id(),
                    current,
                    next,
                    train.currentSpeed(),
                    train.isMoving(),
                    train.timeToNextStation(),
                    train.hasReachedDestination(),
                    trafficController.isTrainWaiting(train.id()),
                    trafficController.getWaitingTime(train.id())));
        }

        return status;
    }

    public TrainStatus getTrainStatus(String trainId) {
        Train train = trains.get(trainId);
        if (train == null) {
            return null;
        }

        return new TrainStatus(
                train.id(),
                train.currentStation(),
                train.getNextStation(),
                train.currentSpeed(),
                train.isMoving(),
                train.timeToNextStation(),
                train.hasReachedDestination(),
                trafficController.isTrainWaiting(trainId),
                trafficController.getWaitingTime(trainId));
    }

    public Collection<Train> getAllTrains() {
        return trains.values();
    }
    
    public double getSimulationTime() {
        return simulationTime;
    }

    public TrafficController getTrafficController() {
        return trafficController;
    }

    public boolean isTrainWaiting(String trainId) {
        return trafficController.isTrainWaiting(trainId);
    }

    public double getTrainWaitingTime(String trainId) {
        return trafficController.getWaitingTime(trainId);
    }

    public static class TrainStatus {
        private final String trainId;
        private final TrainStation currentStation;
        private final TrainStation nextStation;
        private final double currentSpeed;
        private final boolean isMoving;
        private final double timeToNextStation;
        private final boolean hasReachedDestination;
        private final boolean isWaiting;
        private final double waitingTime;

        public TrainStatus(String trainId, TrainStation currentStation, TrainStation nextStation,
                double currentSpeed, boolean isMoving, double timeToNextStation,
                boolean hasReachedDestination, boolean isWaiting, double waitingTime) {
            this.trainId = trainId;
            this.currentStation = currentStation;
            this.nextStation = nextStation;
            this.currentSpeed = currentSpeed;
            this.isMoving = isMoving;
            this.timeToNextStation = timeToNextStation;
            this.hasReachedDestination = hasReachedDestination;
            this.isWaiting = isWaiting;
            this.waitingTime = waitingTime;
        }

        public String trainId() {
            return trainId;
        }

        public TrainStation currentStation() {
            return currentStation;
        }

        public TrainStation nextStation() {
            return nextStation;
        }

        public double currentSpeed() {
            return currentSpeed;
        }

        public boolean isMoving() {
            return isMoving;
        }

        public double timeToNextStation() {
            return timeToNextStation;
        }

        public boolean hasReachedDestination() {
            return hasReachedDestination;
        }

        public boolean isWaiting() {
            return isWaiting;
        }

        public double waitingTime() {
            return waitingTime;
        }

        @Override
        public String toString() {
            String status = String.format("Train[%s] at %s -> %s, speed=%.1f km/h, moving=%s, time=%.1f min",
                    trainId, currentStation, nextStation, currentSpeed, isMoving, timeToNextStation);

            if (isWaiting) {
                status += String.format(", waiting=%.1f min", waitingTime);
            }

            return status;
        }
    }
}