package grafo_ferroviaria.models;

public class TrainStation {
    private final String name;

    public TrainStation(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
