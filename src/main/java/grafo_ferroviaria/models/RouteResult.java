package grafo_ferroviaria.models;

import java.util.List;

public class RouteResult {
    private final List<PointHistory> path;
    private final double totalTime;
    private final double totalPrice;
    private final double totalDistance;

    public RouteResult(List<PointHistory> path, double totalTime, double totalPrice, double totalDistance) {
        this.path = path;
        this.totalTime = totalTime;
        this.totalPrice = totalPrice;
        this.totalDistance = totalDistance;
    }

    public List<PointHistory> getPath() {
        return path;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public String printRoute() {
        String finalString = "";

        if (path == null || path.isEmpty()) {
            finalString += "Nenhuma rota encontrada!";
            return finalString;
        }

        finalString += "\n=== ROTA ENCONTRADA ===\n";
        finalString += "Estações da rota:\n";
        
        for (int i = 0; i < path.size(); i++) {
            PointHistory point = path.get(i);
            String timeStr = formatTime(point.getTimestamp());
            finalString += String.format("%d. %s às %s\n", i + 1, point.getTrainStation().name(), timeStr);
            
            if (i < path.size() - 1) {
                finalString += "   ↓\n";
            }
        }
        
        finalString += "\n=== RESUMO DA VIAGEM ===\n";
        finalString += String.format("Tempo total: %.1f minutos (%.1f horas)\n", totalTime, totalTime / 60.0);
        finalString += String.format("Distância total: %.2f km\n", totalDistance);
        finalString += String.format("Custo total: R$ %.2f\n", totalPrice);

        return finalString;
    }

    private String formatTime(int minutes) {
        int days = minutes / 1440;
        int hours = (minutes % 1440) / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d:%02d", days, hours, mins);
    }
} 