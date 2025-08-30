package grafo_ferroviaria.models;

public class TrainStation {
    private final String name;
    private final double latitude;
    private final double longitude;

    public TrainStation(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
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

    @Override
    public String toString() {
        return name;
    }
}
