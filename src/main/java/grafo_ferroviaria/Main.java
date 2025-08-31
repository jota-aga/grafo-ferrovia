package main.java.grafo_ferroviaria;

import grafo_ferroviaria.managers.RailwayManager;
import grafo_ferroviaria.managers.TrainSimulator;
import grafo_ferroviaria.models.TrainStation;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("railway file path is required");
            System.exit(1);
        }

        String path = args[0];

        RailwayManager railwayManager = new RailwayManager(false);
        railwayManager.loadRailway(path);

        // Menu principal
        showMainMenu(railwayManager);
    }

    private static void showMainMenu(RailwayManager railwayManager) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            clearConsole();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    SISTEMA FERROVIÁRIO                       ║");
            System.out.println("╠══════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. Iniciar Simulação de Trens (Tempo Real)                   ║");
            System.out.println("║ 2. Sair                                                      ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.print("Escolha uma opção: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    startRealTimeSimulation(railwayManager);
                    break;
                case "2":
                    System.out.println("Saindo do sistema...");
                    scanner.close();
                    System.exit(0);
                default:
                    System.out.println("Opção inválida! Pressione Enter para continuar...");
                    scanner.nextLine();
            }
        }
    }

    private static void startRealTimeSimulation(RailwayManager railwayManager) {
        Scanner scanner = new Scanner(System.in);

        // Configuração inicial dos trens
        setupTrains(railwayManager);

        System.out.println("\nSimulação iniciada! Pressione Ctrl+C para sair da simulação.");
        System.out.println("A cada 5 segundos, 5 minutos passam no tempo dos trens.");
        System.out.println("Pressione Enter para continuar...");
        scanner.nextLine();

        // Inicia a simulação em tempo real
        runRealTimeSimulation(railwayManager);
        scanner.close();
    }

    private static void setupTrains(RailwayManager railwayManager) {
        try {
            // Trem 1: Rota que pode causar conflito
            railwayManager.addTrain("TREM-001", 180.0, 200, "EstacaoA",
                    railwayManager.planFastestRouteForTrain("EstacaoA", "EstacaoC").stream().map(TrainStation::name)
                            .collect(Collectors.toList()));

            // Trem 2: Rota que pode conflitar com o Trem-001 na mesma aresta
            railwayManager.addTrain("TREM-002", 100.0, 150, "EstacaoB",
                    railwayManager.planFastestRouteForTrain("EstacaoB", "EstacaoC").stream().map(TrainStation::name)
                            .collect(Collectors.toList()));

            System.out.println("✓ 2 trens configurados com sucesso!");
            System.out.println("  - TREM-001: EstacaoA → EstacaoC (120 km/h)");
            System.out.println("  - TREM-002: EstacaoB → EstacaoC (100 km/h)");

            // Inicia todos os trens
            railwayManager.startTrain("TREM-001");
            railwayManager.startTrain("TREM-002");

            System.out.println("✓ Todos os trens iniciados!");
            System.out
                    .println("⚠️  O sistema de controle de tráfego irá detectar e resolver conflitos automaticamente!");

        } catch (Exception e) {
            System.out.println("Erro ao configurar trens: " + e.getMessage());
        }
    }

    private static void runRealTimeSimulation(RailwayManager railwayManager) {
        final int TICK_INTERVAL_MS = 5000; // 5 segundos
        final double SIMULATION_TIME_STEP = 5.0; // 5 minutos por tick
        int tickCount = 0;

        System.out.println("Simulação iniciada! Pressione Ctrl+C para sair.");

        try {
            while (true) {

                clearConsole();
                displaySimulationStatus(railwayManager, tickCount, SIMULATION_TIME_STEP);

                railwayManager.updateSimulation(SIMULATION_TIME_STEP);
                tickCount++;
                Thread.sleep(TICK_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            System.out.println("\nSimulação interrompida pelo usuário.");
        } catch (Exception e) {
            System.out.println("Erro na simulação: " + e.getMessage());
        }

        System.out.println("\nSimulação finalizada. Pressione Enter para voltar ao menu...");
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignora exceções
        }
    }

    private static void displaySimulationStatus(RailwayManager railwayManager, int tickCount, double timeStep) {
        double totalSimulationTime = tickCount * timeStep;
        Map<String, TrainSimulator.TrainStatus> status = railwayManager.getTrainStatus();

        System.out.println(
                "╔══════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println(
                "║                                    SIMULAÇÃO FERROVIÁRIA EM TEMPO REAL                              ║");
        System.out.println(
                "╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf(
                "║ Tick: %d | Tempo da Simulação: %.0f minutos | Frequência: 5 segundos = 5 minutos                    ║\n",
                tickCount, totalSimulationTime);
        System.out.println(
                "╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        System.out.println(
                "║ STATUS DOS TRENS:                                                                                   ║");
        System.out.println(
                "╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣");

        if (status.isEmpty()) {
            System.out.println(
                    "║ Nenhum trem em operação                                                                                    ║");
        } else {
            for (TrainSimulator.TrainStatus trainStatus : status.values()) {
                displayTrainStatus(trainStatus);
            }
        }

        System.out.println(
                "╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        System.out.println(
                "║ Pressione Ctrl+C para sair da simulação                                                           ║");
        System.out.println(
                "╚══════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void displayTrainStatus(TrainSimulator.TrainStatus status) {
        String trainId = status.trainId();
        String currentStation = status.currentStation() != null ? status.currentStation().name() : "N/A";
        String nextStation = status.nextStation() != null ? status.nextStation().name() : "N/A";
        double speed = status.currentSpeed();
        boolean isMoving = status.isMoving();
        double timeToNext = status.timeToNextStation();
        boolean reachedDestination = status.hasReachedDestination();
        boolean isWaiting = status.isWaiting();
        double waitingTime = status.waitingTime();

        // Ícones para status visual
        String statusIcon = isMoving ? "🚂" : "⏸️";
        String destinationIcon = reachedDestination ? "🏁" : "🎯";
        String waitingIcon = isWaiting ? "⏳" : "";

        System.out.printf("║ %s %s | Estação Atual: %-10s | Próxima: %-10s | Velocidade: %5.1f km/h | ║\n",
                statusIcon, trainId, currentStation, nextStation, speed);

        if (isWaiting) {
            System.out.printf(
                    "║    %s Aguardando controle de tráfego: %.1f minutos restantes                                    ║\n",
                    waitingIcon, waitingTime);
        } else if (isMoving && !reachedDestination) {
            System.out.printf(
                    "║    ⏱️  Tempo para próxima estação: %.1f minutos                                                    ║\n",
                    timeToNext);
        } else if (reachedDestination) {
            System.out.printf(
                    "║    %s Trem chegou ao destino final                                                          ║\n",
                    destinationIcon);
        } else {
            System.out.printf(
                    "║    ⏸️  Trem parado na estação                                                               ║\n");
        }

        System.out.println(
                "║                                                                                                    ║");
    }

    private static void clearConsole() {
        try {
            // Tenta limpar o console usando comandos do sistema
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // Linux/Mac
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Se falhar, apenas imprime várias linhas em branco
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
}
