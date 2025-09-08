package grafo_ferroviaria.models;

public class Schedule {
    private final Train train;
    private final TrainStation pointA;
    private final TrainStation pointB;
    private final String departureHour;
    private final String arrivalHour;

    public Schedule(Train train, TrainStation pointA, TrainStation pointB, String departureHour, String arrivalHour) {
        this.train = train;
        this.pointA = pointA;
        this.pointB = pointB;
        this.departureHour = departureHour;
        this.arrivalHour = arrivalHour;
    }

    public Train getTrain() {
        return this.train;
    }

    public TrainStation getPointA() {
        return this.pointA;
    }

    public TrainStation getPointB() {
        return this.pointB;
    }

    public String getDepartureHour() {
        return this.departureHour;
    }

    public String getArrivalHour() {
        return this.arrivalHour;
    }

}
