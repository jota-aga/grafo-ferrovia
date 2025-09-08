package grafo_ferroviaria.models;

import java.util.List;

public class Train {
    private final String id;
    private final double averageSpeed;
    private final double pricePerKm;
    private final List<Schedule> schedules;

    public Train(String id, double averageSpeed, double pricePerKm, List<Schedule> schedules) {
        this.id = id;
        this.averageSpeed = averageSpeed;
        this.pricePerKm = pricePerKm;
        this.schedules = schedules;
    }

    public String getId() {
        return this.id;
    }

    public double getAverageSpeed() {
        return this.averageSpeed;
    }

    public double pricePerKm() {
        return this.pricePerKm;
    }

    public List<Schedule> getSchedules() {
        return this.schedules;
    }

    public void addSchedule(Schedule schedule) {
        this.schedules.add(schedule);
    }
}
