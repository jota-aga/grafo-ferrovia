package grafo_ferroviaria.models;

public class PointHistory {
    private final TrainStation trainStation;
    private final int timestamp;

    public PointHistory(TrainStation trainStation, int timestamp) {
        this.trainStation = trainStation;
        this.timestamp = timestamp;
    }

    public TrainStation getTrainStation() {
        return this.trainStation;
    }

    public int getTimestamp() {
        return this.timestamp;
    }
}
