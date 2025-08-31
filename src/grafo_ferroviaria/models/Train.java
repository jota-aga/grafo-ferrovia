package grafo_ferroviaria.models;

import java.util.List;

public class Train {
    private final String id;
    private final double maxSpeed; // km/h
    private final int capacity; // número de passageiros
    private final TrainStation currentStation;
    private final List<TrainStation> route;
    private int currentRouteIndex;
    private double currentSpeed;
    private boolean isMoving;
    private double timeToNextStation; // tempo restante para chegar à próxima estação

    public Train(String id, double maxSpeed, int capacity, TrainStation startingStation, List<TrainStation> route) {
        this.id = id;
        this.maxSpeed = maxSpeed;
        this.capacity = capacity;
        this.currentStation = startingStation;
        this.route = route;
        this.currentRouteIndex = 0;
        this.currentSpeed = 0.0;
        this.isMoving = false;
        this.timeToNextStation = 0.0;
    }

    // Getters
    public String id() {
        return id;
    }

    public double maxSpeed() {
        return maxSpeed;
    }

    public int capacity() {
        return capacity;
    }

    public TrainStation currentStation() {
        return this.route.get(this.currentRouteIndex);
    }

    public List<TrainStation> route() {
        return route;
    }

    public int currentRouteIndex() {
        return currentRouteIndex;
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

    // Métodos para controle do trem
    public void startMoving() {
        if (currentRouteIndex < route.size() - 1) {
            this.isMoving = true;
            this.currentSpeed = maxSpeed;
        }
    }

    public void stop() {
        this.isMoving = false;
        this.currentSpeed = 0.0;
    }

    public void updatePosition(double deltaTime) {
        if (!isMoving || currentRouteIndex >= route.size() - 1) {
            return;
        }

        timeToNextStation -= deltaTime;

        if (timeToNextStation <= 0) {
            // Chegou à próxima estação
            currentRouteIndex++;
            timeToNextStation = 0.0;
            isMoving = false;
            currentSpeed = 0.0;
        }
    }

    public TrainStation getNextStation() {
        if (currentRouteIndex < route.size() - 1) {
            return route.get(currentRouteIndex + 1);
        }
        return null;
    }

    public boolean hasReachedDestination() {
        return currentRouteIndex >= route.size() - 1;
    }

    public void setTimeToNextStation(double time) {
        this.timeToNextStation = time;
        this.currentSpeed = maxSpeed;
        this.isMoving = true;
    }

    /**
     * Atualiza a rota do trem dinamicamente
     */
    public void updateRoute(List<TrainStation> newRoute) {
        if (newRoute == null || newRoute.isEmpty()) {
            throw new IllegalArgumentException("Nova rota não pode ser nula ou vazia");
        }

        // Verifica se a nova rota contém a estação atual
        boolean containsCurrent = false;
        for (int i = 0; i < newRoute.size(); i++) {
            if (newRoute.get(i).equals(currentStation())) {
                this.currentRouteIndex = i;
                containsCurrent = true;
                break;
            }
        }

        if (!containsCurrent) {
            throw new IllegalArgumentException("Nova rota deve conter a estação atual: " + currentStation().name());
        }

        // Atualiza a rota
        this.route.clear();
        this.route.addAll(newRoute);

        // Para o trem para recalcular o tempo
        this.isMoving = false;
        this.currentSpeed = 0.0;
        this.timeToNextStation = 0.0;
    }

    @Override
    public String toString() {
        return String.format("Train[%s] at %s, speed=%.1f km/h, moving=%s",
                id, currentStation.name(), currentSpeed, isMoving);
    }
}
