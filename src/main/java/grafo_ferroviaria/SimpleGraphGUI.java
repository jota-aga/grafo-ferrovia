package main.java.grafo_ferroviaria;

import grafo_ferroviaria.managers.RailwayManager;
import grafo_ferroviaria.managers.TrainSimulator;
import grafo_ferroviaria.models.TrainStation;
import grafo_ferroviaria.models.Rail;
import grafo_ferroviaria.models.Train;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

public class SimpleGraphGUI {
    private RailwayManager railwayManager;
    private TrainSimulator trainSimulator;
    private JFrame frame;
    private JPanel controlPanel;
    private JLabel timeLabel;
    private JTextArea statusArea;
    private JPanel graphPanel;
    private Timer simulationTimer;
    private double simulationTime = 0.0;
    private final double TIME_STEP = 0.5; // 1 minuto por tick
    private final int TIMER_DELAY = 100; // 100ms para animação suave

    // Dados para animação dos trens
    private Map<String, TrainPosition> trainPositions;
    private Map<String, Point> stationPositions;

    // Classe para representar a posição de um trem
    private static class TrainPosition {
        TrainStation fromStation;
        TrainStation toStation;
        double progress; // 0.0 a 1.0 (progresso na aresta)
        double speed; // velocidade de animação
        boolean isMoving;

        TrainPosition(TrainStation from, TrainStation to, double progress, double speed) {
            this.fromStation = from;
            this.toStation = to;
            this.progress = progress;
            this.speed = speed;
            this.isMoving = true;
        }
    }

    public SimpleGraphGUI(String railwayFile) {
        // Inicializa o sistema ferroviário
        railwayManager = new RailwayManager(false);
        railwayManager.loadRailway(railwayFile);
        trainSimulator = railwayManager.getTrainSimulator();

        // Inicializa dados de animação
        trainPositions = new HashMap<>();
        stationPositions = new HashMap<>();

        setupGUI();
        setupTrains();
        startSimulation();
    }

    private void setupGUI() {
        frame = new JFrame("Simulação Ferroviária - Visualização Gráfica");
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

        // Painel de visualização gráfica
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph(g);
            }
        };
        graphPanel.setPreferredSize(new Dimension(800, 600));
        graphPanel.setBorder(BorderFactory.createTitledBorder("Visualização da Ferrovia"));
        frame.add(graphPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void drawGraph(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Limpa o painel
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, graphPanel.getWidth(), graphPanel.getHeight());

        // Calcula posições das estações
        calculateStationPositions();

        // Desenha as ferrovias
        drawRailways(g2d);

        // Desenha as estações
        drawStations(g2d);

        // Desenha os trens animados
        drawAnimatedTrains(g2d);
    }

    private void calculateStationPositions() {
        Map<String, TrainStation> stations = railwayManager.stations();
        int centerX = graphPanel.getWidth() / 2;
        int centerY = graphPanel.getHeight() / 2;
        int radius = Math.min(graphPanel.getWidth(), graphPanel.getHeight()) / 3;

        int index = 0;
        for (TrainStation station : stations.values()) {
            double angle = 2 * Math.PI * index / stations.size();
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY + (int) (radius * Math.sin(angle));
            stationPositions.put(station.name(), new Point(x, y));
            index++;
        }
    }

    private void drawRailways(Graphics2D g2d) {
        Map<String, TrainStation> stations = railwayManager.stations();

        g2d.setStroke(new BasicStroke(3));
        for (TrainStation from : stations.values()) {
            for (TrainStation to : stations.values()) {
                if (!from.equals(to)) {
                    Rail rail = railwayManager.graph().neighbors(from).get(to);
                    if (rail != null) {
                        Point fromPos = stationPositions.get(from.name());
                        Point toPos = stationPositions.get(to.name());

                        // Verifica se há trens na aresta
                        boolean isOccupied = trainSimulator.getTrafficController()
                                .getTrainsInRailPublic(from, to).size() > 0;

                        if (isOccupied) {
                            g2d.setColor(Color.RED);
                            g2d.setStroke(new BasicStroke(5));
                        } else {
                            g2d.setColor(Color.GRAY);
                            g2d.setStroke(new BasicStroke(3));
                        }

                        g2d.drawLine(fromPos.x, fromPos.y, toPos.x, toPos.y);

                        // Desenha a distância
                        g2d.setColor(Color.BLACK);
                        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                        int midX = (fromPos.x + toPos.x) / 2;
                        int midY = (fromPos.y + toPos.y) / 2;
                        g2d.drawString(String.format("%.1f km", rail.distance()), midX, midY);
                    }
                }
            }
        }
    }

    private void drawStations(Graphics2D g2d) {
        for (Map.Entry<String, Point> entry : stationPositions.entrySet()) {
            Point pos = entry.getValue();
            String name = entry.getKey();

            // Círculo da estação
            g2d.setColor(Color.BLUE);
            g2d.fillOval(pos.x - 15, pos.y - 15, 30, 30);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(pos.x - 15, pos.y - 15, 30, 30);

            // Nome da estação
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(name);
            g2d.drawString(name, pos.x - textWidth / 2, pos.y + 5);
        }
    }

    private void drawAnimatedTrains(Graphics2D g2d) {
        for (Train train : trainSimulator.getAllTrains()) {
            String trainId = train.id();
            TrainPosition trainPos = trainPositions.get(trainId);

            if (trainPos != null && trainPos.isMoving) {
                // Calcula posição atual do trem na aresta
                Point fromPos = stationPositions.get(trainPos.fromStation.name());
                Point toPos = stationPositions.get(trainPos.toStation.name());

                if (fromPos != null && toPos != null) {
                    int currentX = fromPos.x + (int) ((toPos.x - fromPos.x) * trainPos.progress);
                    int currentY = fromPos.y + (int) ((toPos.y - fromPos.y) * trainPos.progress);

                    // Desenha o sprite do trem
                    drawTrainSprite(g2d, currentX, currentY, trainId, train);
                }
            } else {
                // Trem parado na estação
                TrainStation currentStation = train.currentStation();
                if (currentStation != null) {
                    Point pos = stationPositions.get(currentStation.name());
                    if (pos != null) {
                        drawTrainSprite(g2d, pos.x, pos.y, trainId, train);
                    }
                }
            }
        }
    }

    private void drawTrainSprite(Graphics2D g2d, int x, int y, String trainId, Train train) {
        // Salva o estado atual do gráfico
        AffineTransform originalTransform = g2d.getTransform();

        // Calcula a direção do trem se estiver se movendo
        TrainPosition trainPos = trainPositions.get(trainId);
        if (trainPos != null && trainPos.isMoving && trainPos.progress < 1.0) {
            Point fromPos = stationPositions.get(trainPos.fromStation.name());
            Point toPos = stationPositions.get(trainPos.toStation.name());
            if (fromPos != null && toPos != null) {
                double angle = Math.atan2(toPos.y - fromPos.y, toPos.x - fromPos.x);
                g2d.rotate(angle, x, y);
            }
        }

        // Corpo do trem (retângulo)
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x - 12, y - 6, 24, 12);

        // Janelas do trem
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(x - 8, y - 4, 4, 4);
        g2d.fillRect(x - 2, y - 4, 4, 4);
        g2d.fillRect(x + 4, y - 4, 4, 4);

        // Rodas do trem
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x - 8, y + 4, 4, 4);
        g2d.fillOval(x + 4, y + 4, 4, 4);

        // Borda do trem
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(x - 12, y - 6, 24, 12);

        // ID do trem
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(trainId);
        g2d.drawString(trainId, x - textWidth / 2, y - 10);

        // Indicador de movimento
        if (train.isMoving()) {
            g2d.setColor(Color.GREEN);
            g2d.fillOval(x + 8, y - 8, 4, 4);
        } else if (trainSimulator.isTrainWaiting(trainId)) {
            g2d.setColor(Color.RED);
            g2d.fillOval(x + 8, y - 8, 4, 4);
        }

        // Restaura o estado original do gráfico
        g2d.setTransform(originalTransform);
    }

    private void updateTrainPositions() {
        for (Train train : trainSimulator.getAllTrains()) {
            String trainId = train.id();
            TrainStation currentStation = train.currentStation();
            TrainStation nextStation = train.getNextStation();

            if (nextStation != null && train.isMoving()) {
                // Trem está se movendo entre estações
                TrainPosition trainPos = trainPositions.get(trainId);
                if (trainPos == null || !trainPos.fromStation.equals(currentStation) ||
                        !trainPos.toStation.equals(nextStation)) {
                    // Novo movimento
                    trainPos = new TrainPosition(currentStation, nextStation, 0.0, 0.02);
                    trainPositions.put(trainId, trainPos);
                }

                // Atualiza progresso baseado no tempo restante
                double timeToNext = train.timeToNextStation();
                double totalTime = (railwayManager.graph().neighbors(currentStation).get(nextStation).distance()
                        / train.maxSpeed()) * 60;
                double elapsedTime = totalTime - timeToNext;
                trainPos.progress = Math.min(1.0, elapsedTime / totalTime);
                trainPos.isMoving = true;

            } else {
                // Trem parado na estação
                TrainPosition trainPos = trainPositions.get(trainId);
                if (trainPos != null) {
                    trainPos.isMoving = false;
                    trainPos.progress = 0.0;
                }
            }
        }
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
        trainPositions.clear();

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

        // Atualiza posições dos trens
        updateTrainPositions();

        // Atualiza a visualização
        graphPanel.repaint();
        updateStatus();
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
            System.out.println("Uso: java SimpleGraphGUI <arquivo_ferrovia>");
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            new SimpleGraphGUI(args[0]);
        });
    }
}