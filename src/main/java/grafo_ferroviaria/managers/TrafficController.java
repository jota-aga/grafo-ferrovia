package grafo_ferroviaria.managers;

import grafo_ferroviaria.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controlador de tráfego para prevenir colisões entre trens na mesma aresta
 */
public class TrafficController {
    private final RailwayManager railwayManager;
    private final TrainRoutePlanner routePlanner;
    private TrainSimulator trainSimulator; // Referência ao simulador

    // Mapa que rastreia quais trens estão em cada aresta (from -> to ->
    // List<TrainInfo>)
    private final Map<TrainStation, Map<TrainStation, List<TrainInfo>>> railOccupancy;

    // Mapa que rastreia a posição atual de cada trem
    private final Map<String, TrainPosition> trainPositions;

    // Mapa de trens aguardando (trem -> tempo de espera)
    private final Map<String, Double> waitingTrains;

    public TrafficController(RailwayManager railwayManager, TrainRoutePlanner routePlanner) {
        this.railwayManager = railwayManager;
        this.routePlanner = routePlanner;
        this.railOccupancy = new ConcurrentHashMap<>();
        this.trainPositions = new ConcurrentHashMap<>();
        this.waitingTrains = new ConcurrentHashMap<>();
    }

    /**
     * Define a referência ao simulador de trens
     */
    public void setTrainSimulator(TrainSimulator trainSimulator) {
        this.trainSimulator = trainSimulator;
    }

    /**
     * Registra um trem no sistema de controle de tráfego
     */
    public void registerTrain(String trainId, Train train) {
        TrainPosition position = new TrainPosition(train.currentStation(), null, 0.0);
        trainPositions.put(trainId, position);
    }

    /**
     * Remove um trem do sistema de controle de tráfego
     */
    public void unregisterTrain(String trainId) {
        trainPositions.remove(trainId);
        waitingTrains.remove(trainId);

        // Remove o trem de todas as arestas
        for (Map<TrainStation, List<TrainInfo>> toMap : railOccupancy.values()) {
            for (List<TrainInfo> trainList : toMap.values()) {
                trainList.removeIf(info -> info.trainId.equals(trainId));
            }
        }
    }

    /**
     * Atualiza a posição de um trem e verifica conflitos
     */
    public void updateTrainPosition(String trainId, Train train, double deltaTime) {
        TrainPosition currentPos = trainPositions.get(trainId);
        if (currentPos == null)
            return;

        TrainStation currentStation = train.currentStation();
        TrainStation nextStation = train.getNextStation();

        if (nextStation == null)
            return;

        // Atualiza o tempo restante dos trens nas arestas
        updateRailOccupancyTimes(deltaTime);

        // Verifica se pode entrar na próxima aresta (se não está em uma aresta e não
        // está aguardando)
        if (currentPos.currentRail == null && !isTrainWaiting(trainId)) {
            if (canEnterRail(trainId, currentStation, nextStation)) {
                // Pode entrar na aresta
                enterRail(trainId, currentStation, nextStation, train.timeToNextStation());
                currentPos.currentRail = new RailSegment(currentStation, nextStation);
                currentPos.timeInRail = 0.0;

                // Se o trem não estava se movendo, inicia o movimento
                if (!train.isMoving()) {
                    train.startMoving();
                }
            } else {
                // Não pode entrar, tenta encontrar rota alternativa
                handleCollision(trainId, currentStation, nextStation, train);
            }
        }

        // Atualiza o tempo na aresta atual
        if (currentPos.currentRail != null) {
            currentPos.timeInRail += deltaTime;

            // Verifica se chegou ao final da aresta
            if (currentPos.timeInRail >= train.timeToNextStation()) {
                exitRail(trainId, currentPos.currentRail.from, currentPos.currentRail.to);
                currentPos.currentRail = null;
                currentPos.timeInRail = 0.0;
            }
        }

        // Atualiza posição
        currentPos.currentStation = currentStation;
        currentPos.nextStation = nextStation;
    }

    /**
     * Verifica se um trem pode entrar em uma aresta
     */
    private boolean canEnterRail(String trainId, TrainStation from, TrainStation to) {
        List<TrainInfo> trainsInRail = getTrainsInRail(from, to);

        // Se não há trens na aresta, pode entrar
        if (trainsInRail.isEmpty()) {
            return true;
        }

        // Verifica se há conflito de tempo
        for (TrainInfo info : trainsInRail) {
            if (info.trainId.equals(trainId)) {
                continue; // É o próprio trem
            }

            // Se há outro trem na aresta, não pode entrar
            return false;
        }

        return true;
    }

    /**
     * Registra que um trem entrou em uma aresta
     */
    private void enterRail(String trainId, TrainStation from, TrainStation to, double timeInRail) {
        railOccupancy.computeIfAbsent(from, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(to, k -> new ArrayList<>())
                .add(new TrainInfo(trainId, timeInRail));
    }

    /**
     * Registra que um trem saiu de uma aresta
     */
    private void exitRail(String trainId, TrainStation from, TrainStation to) {
        Map<TrainStation, List<TrainInfo>> toMap = railOccupancy.get(from);
        if (toMap != null) {
            List<TrainInfo> trainList = toMap.get(to);
            if (trainList != null) {
                trainList.removeIf(info -> info.trainId.equals(trainId));
            }
        }
    }

    /**
     * Obtém a lista de trens em uma aresta específica
     */
    private List<TrainInfo> getTrainsInRail(TrainStation from, TrainStation to) {
        Map<TrainStation, List<TrainInfo>> toMap = railOccupancy.get(from);
        if (toMap != null) {
            List<TrainInfo> trainList = toMap.get(to);
            if (trainList != null) {
                return new ArrayList<>(trainList);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Obtém a lista de trens em uma aresta específica (método público para GUI)
     */
    public List<TrainInfo> getTrainsInRailPublic(TrainStation from, TrainStation to) {
        return getTrainsInRail(from, to);
    }

    /**
     * Trata uma colisão detectada
     */
    private void handleCollision(String trainId, TrainStation from, TrainStation to, Train train) {
        // Calcula o tempo de espera necessário
        double waitTime = calculateWaitTime(from, to);

        // Calcula o tempo de uma rota alternativa
        double alternativeTime = calculateAlternativeRouteTime(from, to, train);

        System.out.println(
                "🚨 CONFLITO DETECTADO: Trem " + trainId + " tentando entrar em " + from.name() + " → " + to.name());
        System.out.println("   ⏱️  Tempo de espera: " + String.format("%.1f", waitTime) + " minutos");
        System.out.println("   🛤️  Tempo rota alternativa: " + String.format("%.1f", alternativeTime) + " minutos");

        // Escolhe a melhor opção
        if (alternativeTime < waitTime) {
            // Usa rota alternativa
            List<TrainStation> alternativeRoute = findAlternativeRoute(from, to, train);
            if (alternativeRoute != null) {
                // Atualiza a rota do trem
                updateTrainRoute(trainId, alternativeRoute);
                System.out.println("   ✅ Escolhida: Rota alternativa");
            } else {
                // Se não há rota alternativa, aguarda
                waitingTrains.put(trainId, waitTime);
                System.out.println("   ⏳ Escolhido: Aguardar (sem rota alternativa)");
            }
        } else {
            // Aguarda o trem atual sair da aresta
            waitingTrains.put(trainId, waitTime);
            System.out.println("   ⏳ Escolhido: Aguardar (mais rápido que rota alternativa)");
        }
    }

    /**
     * Calcula o tempo de espera necessário para uma aresta ficar livre
     */
    private double calculateWaitTime(TrainStation from, TrainStation to) {
        List<TrainInfo> trainsInRail = getTrainsInRail(from, to);
        if (trainsInRail.isEmpty()) {
            return 0.0;
        }

        // Retorna o tempo máximo que um trem ainda vai ficar na aresta
        // Considera apenas trens que ainda têm tempo restante
        return trainsInRail.stream()
                .filter(info -> info.remainingTime > 0)
                .mapToDouble(info -> info.remainingTime)
                .max()
                .orElse(0.0);
    }

    /**
     * Calcula o tempo de uma rota alternativa
     */
    private double calculateAlternativeRouteTime(TrainStation from, TrainStation to, Train train) {
        try {
            List<TrainStation> alternativeRoute = findAlternativeRoute(from, to, train);
            if (alternativeRoute != null) {
                TrainRoutePlanner.RouteStatistics stats = routePlanner.calculateRouteStatisticsForTrain(
                        alternativeRoute, train.maxSpeed());
                return stats.totalTime();
            }
        } catch (Exception e) {
            // Se não consegue calcular rota alternativa, retorna infinito
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Encontra uma rota alternativa que evita a aresta conflitante
     */
    private List<TrainStation> findAlternativeRoute(TrainStation from, TrainStation to, Train train) {
        // Obtém a rota atual do trem
        List<TrainStation> currentRoute = train.route();
        int currentIndex = train.currentRouteIndex();

        // Se estamos no final da rota, não há alternativa
        if (currentIndex >= currentRoute.size() - 1) {
            return null;
        }

        // Tenta encontrar um caminho alternativo para o destino final
        TrainStation destination = currentRoute.get(currentRoute.size() - 1);

        try {
            // Usa o algoritmo de caminho mais curto, mas exclui a aresta conflitante
            return railwayManager.graph().shortestPathExcludingEdge(
                    from, destination, Rail::distance, from, to).path;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Atualiza a rota de um trem
     */
    private void updateTrainRoute(String trainId, List<TrainStation> newRoute) {
        if (trainSimulator != null) {
            Train train = trainSimulator.getAllTrains().stream()
                    .filter(t -> t.id().equals(trainId))
                    .findFirst()
                    .orElse(null);

            if (train != null) {
                train.updateRoute(newRoute);
                System.out.println("Trem " + trainId + " usando rota alternativa: " + newRoute);
            }
        }
    }

    /**
     * Verifica se um trem está aguardando
     */
    public boolean isTrainWaiting(String trainId) {
        return waitingTrains.containsKey(trainId);
    }

    /**
     * Obtém o tempo de espera restante de um trem
     */
    public double getWaitingTime(String trainId) {
        return waitingTrains.getOrDefault(trainId, 0.0);
    }

    /**
     * Atualiza os tempos de espera
     */
    public void updateWaitingTimes(double deltaTime) {
        for (Map.Entry<String, Double> entry : waitingTrains.entrySet()) {
            double remainingTime = entry.getValue() - deltaTime;
            if (remainingTime <= 0) {
                String trainId = entry.getKey();
                waitingTrains.remove(trainId);
                System.out.println("✅ Trem " + trainId + " liberado da espera - pode continuar!");
            } else {
                entry.setValue(remainingTime);
            }
        }
    }

    /**
     * Atualiza os tempos restantes de todos os trens nas arestas
     */
    private void updateRailOccupancyTimes(double deltaTime) {
        for (Map<TrainStation, List<TrainInfo>> toMap : railOccupancy.values()) {
            for (List<TrainInfo> trainList : toMap.values()) {
                for (TrainInfo info : trainList) {
                    info.updateRemainingTime(deltaTime);
                }
                // Remove trens que já saíram da aresta
                trainList.removeIf(info -> info.remainingTime <= 0);
            }
        }
    }

    /**
     * Classe para armazenar informações sobre um trem em uma aresta
     */
    private static class TrainInfo {
        final String trainId;
        double remainingTime;

        TrainInfo(String trainId, double remainingTime) {
            this.trainId = trainId;
            this.remainingTime = remainingTime;
        }

        void updateRemainingTime(double deltaTime) {
            remainingTime -= deltaTime;
        }
    }

    /**
     * Classe para representar a posição atual de um trem
     */
    private static class TrainPosition {
        TrainStation currentStation;
        TrainStation nextStation;
        RailSegment currentRail;
        double timeInRail;

        TrainPosition(TrainStation currentStation, TrainStation nextStation, double timeInRail) {
            this.currentStation = currentStation;
            this.nextStation = nextStation;
            this.currentRail = null;
            this.timeInRail = timeInRail;
        }
    }

    /**
     * Classe para representar um segmento de ferrovia
     */
    private static class RailSegment {
        final TrainStation from;
        final TrainStation to;

        RailSegment(TrainStation from, TrainStation to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            RailSegment that = (RailSegment) obj;
            return Objects.equals(from, that.from) && Objects.equals(to, that.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }
}