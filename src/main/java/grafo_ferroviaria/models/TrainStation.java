package grafo_ferroviaria.models;

public class TrainStation {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final TrainStationType type;

    public TrainStation(String name, double latitude, double longitude, TrainStationType type) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
    }

    public String name() {
        return name;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public TrainStationType type() {
        return type;
    }

    @Override
    public String toString() {
        return name;
    }
}
