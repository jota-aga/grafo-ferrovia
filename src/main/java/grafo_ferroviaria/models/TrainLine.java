package grafo_ferroviaria.models;

public class TrainLine {
    private final Train train;
    private final int time;
    private final double price;

    public TrainLine(Train train, int time, double price) {
        this.train = train;
        this.time = time;
        this.price = price;
    }

    public Train getTrain() {
        return this.train;
    }

    public int getTime() {
        return this.time;
    }

    public double getPrice() {
        return this.price;
    }
}
