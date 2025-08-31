package grafo_ferroviaria;

import grafo_ferroviaria.managers.RailwayManager;
import grafo_ferroviaria.managers.TrainSimulator;
import grafo_ferroviaria.models.TrainStation;
import grafo_ferroviaria.models.Rail;
import grafo_ferroviaria.models.Train;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Edge;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class MainGUI {
    private RailwayManager railwayManager;
    private TrainSimulator trainSimulator;
    private Graph graph;
    private Viewer viewer;
    private JFrame frame;
    private JPanel controlPanel;
    private JLabel timeLabel;
    private JTextArea statusArea;
    private Timer simulationTimer;
    private double simulationTime = 0.0;
    private final double TIME_STEP = 1.0; // 1 minuto por tick
    private final int TIMER_DELAY = 1000; // 1 segundo real = 1 minuto simulação

    public MainGUI(String railwayFile) {
        // Inicializa o sistema ferroviário
        railwayManager = new RailwayManager(false);
        railwayManager.loadRailway(railwayFile);
        trainSimulator = railwayManager.getTrainSimulator();

        // Configura o GraphStream
        System.setProperty("org.graphstream.ui", "swing");
        setupGraph();
        setupGUI();
        setupTrains();
        startSimulation();
    }

    private void setupGraph() {
        graph = new SingleGraph("Ferrovia");
        graph.setAttribute("ui.stylesheet", getStylesheet());
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");

        // Adiciona as estações como nós
        for (TrainStation station : railwayManager.stations().values()) {
            Node node = graph.addNode(station.name());
            node.setAttribute("ui.label", station.name());
            node.setAttribute("ui.class", "station");

            // Posiciona as estações em um layout circular
            int index = station.name().hashCode() % 360;
            double angle = Math.toRadians(index);
            double x = Math.cos(angle) * 200;
            double y = Math.sin(angle) * 200;
            node.setAttribute("xy", x, y);
        }

        // Adiciona as ferrovias como arestas
        for (TrainStation from : railwayManager.stations().values()) {
            for (TrainStation to : railwayManager.stations().values()) {
                if (!from.equals(to)) {
                    Rail rail = railwayManager.graph().neighbors(from).get(to);
                    if (rail != null) {
                        String edgeId = from.name() + "-" + to.name();
                        Edge edge = graph.addEdge(edgeId, from.name(), to.name(), true);
                        edge.setAttribute("ui.label", String.format("%.1f km", rail.distance()));
                        edge.setAttribute("ui.class", "rail");
                        edge.setAttribute("weight", rail.distance());
                    }
                }
            }
        }
    }

    private String getStylesheet() {
        return """
                graph {
                    fill-color: #f0f0f0;
                    padding: 20px;
                }

                node {
                    size: 20px;
                    fill-color: #4CAF50;
                    stroke-color: #2E7D32;
                    stroke-width: 2px;
                    text-size: 14px;
                    text-color: #333;
                    text-style: bold;
                }

                node.station {
                    fill-color: #2196F3;
                    stroke-color: #1976D2;
                }

                node.train {
                    fill-color: #FF5722;
                    stroke-color: #D84315;
                    size: 15px;
                }

                edge {
                    fill-color: #666;
                    size: 3px;
                    text-size: 12px;
                    text-color: #333;
                }

                edge.rail {
                    fill-color: #795548;
                    size: 4px;
                }

                edge.occupied {
                    fill-color: #F44336;
                    size: 5px;
                }
                """;
    }

    private void setupGUI() {
        frame = new JFrame("Simulação Ferroviária - GraphStream");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
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
        statusArea = new JTextArea(10, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Status dos Trens"));
        controlPanel.add(scrollPane);

        frame.add(controlPanel, BorderLayout.EAST);

        // Viewer do GraphStream
        viewer = graph.display();
        JPanel graphPanel = new JPanel(new BorderLayout());
        graphPanel.add((Component) viewer.getDefaultView(), BorderLayout.CENTER);
        frame.add(graphPanel, BorderLayout.CENTER);

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

        // Limpa o grafo
        graph.nodes().forEach(node -> {
            if (node.hasAttribute("ui.class") &&
                    node.getAttribute("ui.class").equals("train")) {
                graph.removeNode(node);
            }
        });

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
        // Remove trens antigos do grafo
        graph.nodes().forEach(node -> {
            if (node.hasAttribute("ui.class") &&
                    node.getAttribute("ui.class").equals("train")) {
                graph.removeNode(node);
            }
        });

        // Adiciona trens atuais
        for (Train train : trainSimulator.getAllTrains()) {
            String trainId = train.id();
            TrainStation currentStation = train.currentStation();

            if (currentStation != null) {
                Node trainNode = graph.addNode(trainId);
                trainNode.setAttribute("ui.label", trainId);
                trainNode.setAttribute("ui.class", "train");

                // Posiciona o trem na estação atual
                Node stationNode = graph.getNode(currentStation.name());
                if (stationNode != null) {
                    double x = stationNode.getAttribute("xy", 0.0);
                    double y = stationNode.getAttribute("xy", 1.0);
                    trainNode.setAttribute("xy", x + 30, y + 30);
                }
            }
        }

        // Atualiza cores das arestas baseado na ocupação
        for (Edge edge : graph.getEachEdge()) {
            String[] stations = edge.getId().split("-");
            if (stations.length == 2) {
                TrainStation from = railwayManager.stations().get(stations[0]);
                TrainStation to = railwayManager.stations().get(stations[1]);

                if (from != null && to != null) {
                    // Verifica se há trens na aresta
                    boolean isOccupied = trainSimulator.getTrafficController()
                            .getTrainsInRailPublic(from, to).size() > 0;

                    if (isOccupied) {
                        edge.setAttribute("ui.class", "rail,occupied");
                    } else {
                        edge.setAttribute("ui.class", "rail");
                    }
                }
            }
        }
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
            System.out.println("Uso: java MainGUI <arquivo_ferrovia>");
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            new MainGUI(args[0]);
        });
    }
}