package grafo_ferroviaria.models;

public class Rail {
    private final double price;
    private final double time;
    private final double distance;
    private final boolean highSpeed;

    public Rail(double price, double time, double distance, boolean highSpeed) {
        this.price = price;
        this.time = time;
        this.distance = distance;
        this.highSpeed = highSpeed;
    }

    public double price() {
        return price;
    }

    public double time() {
        return time;
    }

    public double distance() {
        return distance;
    }

    public boolean highSpeed() {
        return highSpeed;
    }
}
