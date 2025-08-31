package main.java.grafo_ferroviaria;

import grafo_ferroviaria.managers.RailwayManager;
import grafo_ferroviaria.managers.TrainSimulator;
import grafo_ferroviaria.models.TrainStation;
import grafo_ferroviaria.models.Train;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleGUI {
    private RailwayManager railwayManager;
    private TrainSimulator trainSimulator;
    private JFrame frame;
    private JPanel controlPanel;
    private JLabel timeLabel;
    private JTextArea statusArea;
    private JTextArea graphArea;
    private Timer simulationTimer;
    private double simulationTime = 0.0;
    private final double TIME_STEP = 1.0; // 1 minuto por tick
    private final int TIMER_DELAY = 1000; // 1 segundo real = 1 minuto simulação

    public SimpleGUI(String railwayFile) {
        // Inicializa o sistema ferroviário
        railwayManager = new RailwayManager(false);
        railwayManager.loadRailway(railwayFile);
        trainSimulator = railwayManager.getTrainSimulator();

        setupGUI();
        setupTrains();
        startSimulation();
    }

    private void setupGUI() {
        frame = new JFrame("Simulação Ferroviária - Interface Simples");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLayout(new BorderLayout());

        // Painel de controles
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controles"));

        // Controles de tempo
        JPanel timePanel = new JPanel(new FlowLayout());
        timeLabel = new JLabel("Tempo: 0 minutos");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timePanel.add(timeLabel);
        controlPanel.add(timePanel);

        // Botões de controle
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton startButton = new JButton("Iniciar");
        startButton.addActionListener(e -> startSimulation());

        JButton pauseButton = new JButton("Pausar");
        pauseButton.addActionListener(e -> pauseSimulation());

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetSimulation());

        JButton speedButton = new JButton("Velocidade 2x");
        speedButton.addActionListener(e -> toggleSpeed());

        buttonPanel.add(startButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(speedButton);
        controlPanel.add(buttonPanel);

        // Área de status
        statusArea = new JTextArea(15, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setBorder(BorderFactory.createTitledBorder("Status dos Trens"));
        controlPanel.add(statusScrollPane);

        frame.add(controlPanel, BorderLayout.EAST);

        // Área de visualização do grafo (textual)
        graphArea = new JTextArea(20, 60);
        graphArea.setEditable(false);
        graphArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane graphScrollPane = new JScrollPane(graphArea);
        graphScrollPane.setBorder(BorderFactory.createTitledBorder("Visualização da Ferrovia"));
        frame.add(graphScrollPane, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupTrains() {
        try {
            // Configura os trens
            List<String> route1 = railwayManager.planFastestRouteForTrain("EstacaoA", "EstacaoC")
                    .stream().map(TrainStation::name).collect(Collectors.toList());
            List<String> route2 = railwayManager.planFastestRouteForTrain("EstacaoB", "EstacaoC")
                    .stream().map(TrainStation::name).collect(Collectors.toList());

            railwayManager.addTrain("TREM-001", 180.0, 200, "EstacaoA", route1);
            railwayManager.addTrain("TREM-002", 100.0, 150, "EstacaoB", route2);

            // Inicia os trens
            railwayManager.startTrain("TREM-001");
            railwayManager.startTrain("TREM-002");

            log("✓ 2 trens configurados:");
            log("  - TREM-001: EstacaoA → EstacaoC (180 km/h)");
            log("  - TREM-002: EstacaoB → EstacaoC (100 km/h)");
            log("✓ Todos os trens iniciados!");

        } catch (Exception e) {
            log("Erro ao configurar trens: " + e.getMessage());
        }
    }

    private void startSimulation() {
        if (simulationTimer == null || !simulationTimer.isRunning()) {
            simulationTimer = new Timer(TIMER_DELAY, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateSimulation();
                }
            });
            simulationTimer.start();
            log("Simulação iniciada!");
        }
    }

    private void pauseSimulation() {
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
            log("Simulação pausada!");
        }
    }

    private void resetSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        simulationTime = 0.0;
        timeLabel.setText("Tempo: 0 minutos");

        // Remove trens existentes
        for (Train train : trainSimulator.getAllTrains()) {
            railwayManager.removeTrain(train.id());
        }

        // Reconfigura os trens
        setupTrains();
        log("Simulação resetada!");
    }

    private void toggleSpeed() {
        if (simulationTimer != null) {
            if (simulationTimer.getDelay() == TIMER_DELAY) {
                simulationTimer.setDelay(TIMER_DELAY / 2);
                log("Velocidade aumentada para 2x!");
            } else {
                simulationTimer.setDelay(TIMER_DELAY);
                log("Velocidade normal!");
            }
        }
    }

    private void updateSimulation() {
        simulationTime += TIME_STEP;
        timeLabel.setText(String.format("Tempo: %.0f minutos", simulationTime));

        // Atualiza a simulação
        railwayManager.updateSimulation(TIME_STEP);

        // Atualiza a visualização
        updateVisualization();
        updateStatus();
    }

    private void updateVisualization() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== VISUALIZAÇÃO DA FERROVIA ===\n\n");

        // Desenha o grafo em ASCII
        Map<String, TrainStation> stations = railwayManager.stations();

        // Cabeçalho
        sb.append("Estações:\n");
        for (TrainStation station : stations.values()) {
            sb.append(String.format("  %s\n", station.name()));
        }
        sb.append("\n");

        // Ferrovias
        sb.append("Ferrovias:\n");
        for (TrainStation from : stations.values()) {
            for (TrainStation to : stations.values()) {
                if (!from.equals(to)) {
                    var rail = railwayManager.graph().neighbors(from).get(to);
                    if (rail != null) {
                        sb.append(String.format("  %s ──%.1fkm── %s\n",
                                from.name(), rail.distance(), to.name()));
                    }
                }
            }
        }
        sb.append("\n");

        // Posição dos trens
        sb.append("Posição dos Trens:\n");
        for (Train train : trainSimulator.getAllTrains()) {
            TrainStation current = train.currentStation();
            TrainStation next = train.getNextStation();

            sb.append(String.format("  %s: %s", train.id(), current.name()));
            if (next != null) {
                sb.append(String.format(" → %s", next.name()));
            }

            if (train.isMoving()) {
                sb.append(" [MOVENDO]");
            } else if (train.hasReachedDestination()) {
                sb.append(" [DESTINO]");
            } else if (trainSimulator.isTrainWaiting(train.id())) {
                sb.append(String.format(" [AGUARDANDO %.1f min]",
                        trainSimulator.getTrainWaitingTime(train.id())));
            } else {
                sb.append(" [PARADO]");
            }
            sb.append("\n");
        }

        graphArea.setText(sb.toString());
    }

    private void updateStatus() {
        Map<String, TrainSimulator.TrainStatus> status = railwayManager.getTrainStatus();
        StringBuilder sb = new StringBuilder();

        for (TrainSimulator.TrainStatus trainStatus : status.values()) {
            sb.append(String.format("🚂 %s\n", trainStatus.trainId()));
            sb.append(String.format("   Estação: %s → %s\n",
                    trainStatus.currentStation() != null ? trainStatus.currentStation().name() : "N/A",
                    trainStatus.nextStation() != null ? trainStatus.nextStation().name() : "N/A"));
            sb.append(String.format("   Velocidade: %.1f km/h\n", trainStatus.currentSpeed()));
            sb.append(String.format("   Movendo: %s\n", trainStatus.isMoving() ? "Sim" : "Não"));

            if (trainStatus.isWaiting()) {
                sb.append(String.format("   ⏳ Aguardando: %.1f min\n", trainStatus.waitingTime()));
            } else if (trainStatus.isMoving() && !trainStatus.hasReachedDestination()) {
                sb.append(String.format("   ⏱️ Tempo restante: %.1f min\n", trainStatus.timeToNextStation()));
            } else if (trainStatus.hasReachedDestination()) {
                sb.append("   🏁 Chegou ao destino!\n");
            }
            sb.append("\n");
        }

        statusArea.setText(sb.toString());
    }

    private void log(String message) {
        statusArea.append(message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java SimpleGUI <arquivo_ferrovia>");
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            new SimpleGUI(args[0]);
        });
    }
}