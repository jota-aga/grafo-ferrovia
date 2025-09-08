package main.java.grafo_ferroviaria;

import grafo_ferroviaria.managers.RailwayManager;
import grafo_ferroviaria.models.TrainStation;
import grafo_ferroviaria.models.Rail;
import grafo_ferroviaria.models.Train;
import grafo_ferroviaria.models.RouteResult;
import grafo_ferroviaria.models.GenericGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Main {
    private RailwayManager railwayManager;
    private JFrame frame;
    private JPanel controlPanel;
    private JLabel timeLabel;
    private JTextArea statusArea;
    private JPanel graphPanel;
    private Timer simulationTimer;
    private double simulationTime = 0.0;
    private final double TIME_STEP = 0.5;
    private final int TIMER_DELAY = 100;

    private JComboBox<String> originComboBox;
    private JComboBox<String> destinationComboBox;
    private JComboBox<String> searchTypeComboBox;
    private JTextField startTimeField;
    private JButton findRouteButton;
    private JTextArea routeResultArea;

    private Map<String, TrainPosition> trainPositions;
    private Map<String, Point> stationPositions;
    private boolean simulationRunning = false;
    private String currentRouteOrigin = null;
    private String currentRouteDestination = null;
    private int simulationStartTime = 0;
    private Point userPosition = null;
    private String userCurrentStation = null;
    private List<grafo_ferroviaria.models.PointHistory> currentRoute = null;
    private int currentRouteIndex = 0;

    private static class TrainPosition {
        TrainStation fromStation;
        TrainStation toStation;
        double progress;
        double speed;
        boolean isMoving;
        String trainId;
        int departureTime;
        int arrivalTime;

        TrainPosition(TrainStation from, TrainStation to, double progress, double speed, 
                     String trainId, int departureTime, int arrivalTime) {
            this.fromStation = from;
            this.toStation = to;
            this.progress = progress;
            this.speed = speed;
            this.isMoving = true;
            this.trainId = trainId;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
        }
    }

    public Main(String railwayFile) {
        railwayManager = new RailwayManager();
        railwayManager.loadRailway(railwayFile);

        trainPositions = new HashMap<>();
        stationPositions = new HashMap<>();

        setupGUI();
    }

    private void setupGUI() {
        frame = new JFrame("Simulação Ferroviária - Visualização Gráfica");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1600, 1000);
        frame.setLayout(new BorderLayout());

        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controles"));

        JPanel timePanel = new JPanel(new FlowLayout());
        timeLabel = new JLabel("Tempo: 0 minutos");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timePanel.add(timeLabel);
        controlPanel.add(timePanel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Controles da Simulação"));

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

        JPanel routePanel = new JPanel();
        routePanel.setBorder(BorderFactory.createTitledBorder("Parâmetros da Rota"));
        routePanel.setLayout(new GridLayout(0, 2, 5, 5));
        
        JPanel originPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        originPanel.add(new JLabel("Origem:"));
        originComboBox = new JComboBox<>();
        populateStationComboBox(originComboBox);
        originPanel.add(originComboBox);
        routePanel.add(originPanel);
        
        JPanel destinationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        destinationPanel.add(new JLabel("Destino:"));
        destinationComboBox = new JComboBox<>();
        populateStationComboBox(destinationComboBox);
        destinationPanel.add(destinationComboBox);
        routePanel.add(destinationPanel);
        
        JPanel timePanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timePanel2.add(new JLabel("Horário:"));
        startTimeField = new JTextField("08:00", 8);
        startTimeField.setToolTipText("Formato: HH:MM (ex: 08:00)");
        timePanel2.add(startTimeField);
        routePanel.add(timePanel2);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Buscar por:"));
        searchTypeComboBox = new JComboBox<>(new String[]{"Tempo", "Preço"});
        searchPanel.add(searchTypeComboBox);
        routePanel.add(searchPanel);
        
        JPanel buttonPanel2 = new JPanel(new FlowLayout());
        findRouteButton = new JButton("Encontrar Rota");
        findRouteButton.addActionListener(e -> findRoute());
        buttonPanel2.add(findRouteButton);
        
        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.add(buttonPanel2, BorderLayout.CENTER);
        routePanel.add(buttonWrapper);
        
        routePanel.add(new JPanel());
        
        controlPanel.add(routePanel);
        
        routeResultArea = new JTextArea(8, 30);
        routeResultArea.setEditable(false);
        routeResultArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        routeResultArea.setBorder(BorderFactory.createTitledBorder("Resultado da Rota"));
        JScrollPane routeScrollPane = new JScrollPane(routeResultArea);
        controlPanel.add(routeScrollPane);

        frame.add(controlPanel, BorderLayout.EAST);

        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph(g);
            }
        };
        graphPanel.setPreferredSize(new Dimension(1000, 700));
        graphPanel.setBorder(BorderFactory.createTitledBorder("Visualização da Ferrovia"));
        frame.add(graphPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void populateStationComboBox(JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        
        HashMap<String, TrainStation> stations = railwayManager.getStations();
        for (String stationName : stations.keySet()) {
            comboBox.addItem(stationName);
        }
    }

    private void findRoute() {
        try {
            String origin = (String) originComboBox.getSelectedItem();
            String destination = (String) destinationComboBox.getSelectedItem();
            String startTime = startTimeField.getText().trim();
            String searchType = (String) searchTypeComboBox.getSelectedItem();
            
            if (origin == null || destination == null) {
                routeResultArea.setText("Erro: Selecione origem e destino!");
                return;
            }
            
            if (origin.equals(destination)) {
                routeResultArea.setText("Erro: Origem e destino devem ser diferentes!");
                return;
            }
            
            if (!isValidTimeFormat(startTime)) {
                routeResultArea.setText("Erro: Formato de horário inválido! Use HH:MM (ex: 08:00)");
                return;
            }
            
            routeResultArea.setText("Procurando rota...\n");
            routeResultArea.repaint();
            
            RouteResult result = null;
            if ("Tempo".equals(searchType)) {
                result = railwayManager.findFastestRoute(origin, destination, startTime);
            } else {
                result = railwayManager.findCheapestRoute(origin, destination, startTime);
            }
            
            if (result != null) {
                displayRouteResult(result, searchType);
                startRouteSimulation(origin, destination, startTime, result.getPath());
            } else {
                routeResultArea.setText("Nenhuma rota encontrada entre " + origin + " e " + destination + "!");
            }
            
        } catch (Exception e) {
            routeResultArea.setText("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayRouteResult(RouteResult result, String searchType) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ROTA ENCONTRADA (").append(searchType).append(") ===\n\n");
        
        sb.append("Estações da rota:\n");
        List<grafo_ferroviaria.models.PointHistory> path = result.getPath();
        
        for (int i = 0; i < path.size(); i++) {
            grafo_ferroviaria.models.PointHistory point = path.get(i);
            String timeStr = formatTime(point.getTimestamp());
            sb.append(String.format("%d. %s às %s\n", i + 1, point.getTrainStation().name(), timeStr));
            
            if (i < path.size() - 1) {
                sb.append("   ↓\n");
            }
        }
        
        sb.append("\n=== RESUMO DA VIAGEM ===\n");
        sb.append(String.format("Tempo total: %.1f minutos (%.1f horas)\n", 
            result.getTotalTime(), result.getTotalTime() / 60.0));
        sb.append(String.format("Distância total: %.2f km\n", result.getTotalDistance()));
        sb.append(String.format("Custo total: R$ %.2f\n", result.getTotalPrice()));
        
        routeResultArea.setText(sb.toString());
    }

    private boolean isValidTimeFormat(String time) {
        if (time == null || time.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = time.trim().split(":");
        if (parts.length != 2) {
            return false;
        }
        
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String formatTime(int minutes) {
        int totalMinutes = minutes;
        int days = totalMinutes / 1440;
        int dayMinutes = totalMinutes % 1440;
        
        int hours = dayMinutes / 60;
        int mins = dayMinutes % 60;
        
        if (days > 0) {
            return String.format("%02d:%02d (+%d)", hours, mins, days);
        } else {
            return String.format("%02d:%02d", hours, mins);
        }
    }

    private void startRouteSimulation(String origin, String destination, String startTime, 
                                    List<grafo_ferroviaria.models.PointHistory> route) {    
        for (int i = 0; i < route.size(); i++) {
            grafo_ferroviaria.models.PointHistory point = route.get(i);
        }
        
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
        
        currentRouteOrigin = origin;
        currentRouteDestination = destination;
        currentRoute = route;
        currentRouteIndex = 0;
        simulationStartTime = parseTimeToMinutes(startTime);
        simulationTime = 0.0;
        simulationRunning = true;

        calculateStationPositions(railwayManager.getRailwayGraph());

        userCurrentStation = origin;
        Point originPos = stationPositions.get(origin);

        if (originPos != null) {
            userPosition = new Point(originPos.x, originPos.y);
        }
        
        initializeTrainPositions();
                
        simulationTimer = new Timer(TIMER_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSimulation();
            }
        });
        simulationTimer.start();
    }

    private void initializeTrainPositions() {
        trainPositions.clear();
        
        HashMap<String, Train> trains = railwayManager.getTrains();
        
        for (Train train : trains.values()) {
            for (grafo_ferroviaria.models.Schedule schedule : train.getSchedules()) {
                String trainId = train.getId();
                TrainStation from = schedule.getPointA();
                TrainStation to = schedule.getPointB();
                
                int departureTime = parseTimeToMinutes(schedule.getDepartureHour());
                int arrivalTime = parseTimeToMinutes(schedule.getArrivalHour());
                
                if (arrivalTime <= departureTime) {
                    arrivalTime += 1440;
                }
                
                Point fromPos = stationPositions.get(from.name());
                Point toPos = stationPositions.get(to.name());
                
                if (fromPos != null && toPos != null) {
                    double distance = calculateDistance(fromPos, toPos);
                    double timeInHours = (arrivalTime - departureTime) / 60.0;
                    double speed = distance / timeInHours;
                    
                    TrainPosition trainPos = new TrainPosition(from, to, 0.0, speed, 
                                                             trainId, departureTime, arrivalTime);
                    trainPositions.put(trainId + "_" + departureTime, trainPos);
                }
            }
        }
    }

    private void updateSimulation() {
        if (!simulationRunning) {
            return;
        }
        
        simulationTime += TIME_STEP;
        int currentTime = simulationStartTime + (int) simulationTime;
                
        timeLabel.setText(String.format("Tempo: %s", formatTime(currentTime)));
        
        updateTrainPositions(currentTime);
        
        updateUserPosition(currentTime);
        
        if (userCurrentStation != null && userCurrentStation.equals(currentRouteDestination)) {
            boolean userIsMoving = false;
            for (TrainPosition trainPos : trainPositions.values()) {
                if (isTrainOnUserRoute(trainPos) && trainPos.isMoving) {
                    userIsMoving = true;
                    break;
                }
            }
            
            if (!userIsMoving) {
                simulationRunning = false;
                simulationTimer.stop();
                routeResultArea.append("\n\n CHEGOU AO DESTINO! 🎉");
            }
        }
        
        graphPanel.repaint();
    }

    private void updateTrainPositions(int currentTime) {
        int movingTrains = 0;
        int totalTrains = trainPositions.size();
        int waitingTrains = 0;
        int arrivedTrains = 0;

        for (TrainPosition trainPos : trainPositions.values()) {
            boolean wasMoving = trainPos.isMoving;
            
            if (currentTime >= trainPos.departureTime && currentTime < trainPos.arrivalTime) {
                int totalTime = trainPos.arrivalTime - trainPos.departureTime;
                int elapsedTime = currentTime - trainPos.departureTime;
                trainPos.progress = Math.min(1.0, (double) elapsedTime / totalTime);
                trainPos.isMoving = true;
                movingTrains++;
            } else if (currentTime >= trainPos.arrivalTime) {
                trainPos.progress = 1.0;
                trainPos.isMoving = false;
                arrivedTrains++;
            } else {
                trainPos.progress = 0.0;
                trainPos.isMoving = false;
                waitingTrains++;
            }
        }
    }

    private void updateUserPosition(int currentTime) {
        if (currentRoute == null || currentRouteIndex >= currentRoute.size()) {
            return;
        }
        
        grafo_ferroviaria.models.PointHistory currentPoint = currentRoute.get(currentRouteIndex);
        
        if (currentRouteIndex < currentRoute.size() - 1) {
            grafo_ferroviaria.models.PointHistory nextPoint = currentRoute.get(currentRouteIndex + 1);
            if (currentTime >= nextPoint.getTimestamp()) {
                currentRouteIndex++;
                grafo_ferroviaria.models.PointHistory reachedPoint = currentRoute.get(currentRouteIndex);
                userCurrentStation = reachedPoint.getTrainStation().name();
    
                Point nextPos = stationPositions.get(userCurrentStation);
                if (nextPos != null) {
                    userPosition = new Point(nextPos.x, nextPos.y);
                }
            }
        }
        
        updateUserFollowingTrain(currentTime);
    }

    private void updateUserFollowingTrain(int currentTime) {
        for (TrainPosition trainPos : trainPositions.values()) {
            if (isTrainOnUserRoute(trainPos)) {
                if (trainPos.isMoving) {
                    Point fromPos = stationPositions.get(trainPos.fromStation.name());
                    Point toPos = stationPositions.get(trainPos.toStation.name());
                    
                    if (fromPos != null && toPos != null) {
                        int userX = (int) (fromPos.x + trainPos.progress * (toPos.x - fromPos.x));
                        int userY = (int) (fromPos.y + trainPos.progress * (toPos.y - fromPos.y));
                        
                        userPosition = new Point(userX, userY);
                    }
                } else if (trainPos.progress >= 1.0) {
                    Point destPos = stationPositions.get(trainPos.toStation.name());
                    if (destPos != null) {
                        userPosition = new Point(destPos.x, destPos.y);
                        userCurrentStation = trainPos.toStation.name();
                    }
                }
                break;
            }
        }
    }

    private boolean isTrainOnUserRoute(TrainPosition trainPos) {
        if (currentRoute == null || currentRouteIndex >= currentRoute.size() - 1) {
            return false;
        }
        
        grafo_ferroviaria.models.PointHistory fromPoint = currentRoute.get(currentRouteIndex);
        grafo_ferroviaria.models.PointHistory toPoint = currentRoute.get(currentRouteIndex + 1);
        
        boolean matchesCurrentSegment = 
            trainPos.fromStation.name().equals(fromPoint.getTrainStation().name()) &&
            trainPos.toStation.name().equals(toPoint.getTrainStation().name());
        
        return matchesCurrentSegment;
    }

    private int parseTimeToMinutes(String timeStr) {
        String[] parts = timeStr.trim().split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato de horário inválido: " + timeStr);
        }
        int hours = Integer.parseInt(parts[0].trim());
        int minutes = Integer.parseInt(parts[1].trim());
        return hours * 60 + minutes;
    }

    private double calculateDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }

    private void drawGraph(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, graphPanel.getWidth(), graphPanel.getHeight());

        GenericGraph<TrainStation, Rail> railwayGraph = railwayManager.getRailwayGraph();
        
        if (railwayGraph == null || railwayGraph.vertices().isEmpty()) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String message = "Nenhum grafo ferroviário carregado";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (graphPanel.getWidth() - fm.stringWidth(message)) / 2;
            int y = graphPanel.getHeight() / 2;
            g2d.drawString(message, x, y);
            return;
        }

        calculateStationPositions(railwayGraph);
        
        drawRails(g2d, railwayGraph);
        
        drawStations(g2d, railwayGraph);
        
        drawTrains(g2d);
        
        drawUserPosition(g2d);
        
        drawStationLabels(g2d, railwayGraph);
    }

    private void calculateStationPositions(GenericGraph<TrainStation, Rail> railwayGraph) {
        stationPositions.clear();
        
        List<TrainStation> stations = new ArrayList<>(railwayGraph.vertices());
        int numStations = stations.size();
        
        if (numStations == 0) return;
        
        int panelWidth = graphPanel.getWidth() - 100;
        int panelHeight = graphPanel.getHeight() - 100;
        
        if (numStations <= 6) {
            int centerX = graphPanel.getWidth() / 2;
            int centerY = graphPanel.getHeight() / 2;
            int radius = Math.min(panelWidth, panelHeight) / 3;
            
            for (int i = 0; i < numStations; i++) {
                double angle = 2 * Math.PI * i / numStations - Math.PI / 2;
                int x = centerX + (int) (radius * Math.cos(angle));
                int y = centerY + (int) (radius * Math.sin(angle));
                stationPositions.put(stations.get(i).name(), new Point(x, y));
            }
        } else {
            int cols = (int) Math.ceil(Math.sqrt(numStations));
            int rows = (int) Math.ceil((double) numStations / cols);
            
            int cellWidth = panelWidth / cols;
            int cellHeight = panelHeight / rows;
            
            for (int i = 0; i < numStations; i++) {
                int row = i / cols;
                int col = i % cols;
                int x = 50 + col * cellWidth + cellWidth / 2;
                int y = 50 + row * cellHeight + cellHeight / 2;
                stationPositions.put(stations.get(i).name(), new Point(x, y));
            }
        }
    }

    private void drawRails(Graphics2D g2d, GenericGraph<TrainStation, Rail> railwayGraph) {
        g2d.setStroke(new BasicStroke(3.0f));
        
        for (TrainStation station : railwayGraph.vertices()) {
            Point stationPos = stationPositions.get(station.name());
            if (stationPos == null) continue;
            
            Map<TrainStation, Rail> neighbors = railwayGraph.neighbors(station);
            for (Map.Entry<TrainStation, Rail> entry : neighbors.entrySet()) {
                TrainStation neighbor = entry.getKey();
                Rail rail = entry.getValue();
                
                Point neighborPos = stationPositions.get(neighbor.name());
                if (neighborPos == null) continue;
                
                if (station.name().compareTo(neighbor.name()) > 0) continue;
                
                g2d.setColor(new Color(100, 100, 100));
                g2d.drawLine(stationPos.x, stationPos.y, neighborPos.x, neighborPos.y);
                
                drawRailwaySleepers(g2d, stationPos, neighborPos, rail.distance());
                
                drawDistanceLabel(g2d, stationPos, neighborPos, rail.distance());
            }
        }
    }

    private void drawRailwaySleepers(Graphics2D g2d, Point start, Point end, double distance) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length == 0) return;
        
        double unitX = dx / length;
        double unitY = dy / length;
        
        double perpX = -unitY;
        double perpY = unitX;
        
        int numSleepers = Math.max(3, (int) (distance / 10));
        g2d.setColor(new Color(139, 69, 19));
        g2d.setStroke(new BasicStroke(1.0f));
        
        for (int i = 1; i < numSleepers; i++) {
            double t = (double) i / numSleepers;
            int x = (int) (start.x + t * dx);
            int y = (int) (start.y + t * dy);
            
            int sleeperLength = 8;
            int x1 = (int) (x + perpX * sleeperLength);
            int y1 = (int) (y + perpY * sleeperLength);
            int x2 = (int) (x - perpX * sleeperLength);
            int y2 = (int) (y - perpY * sleeperLength);
            
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawDistanceLabel(Graphics2D g2d, Point start, Point end, double distance) {
        int midX = (start.x + end.x) / 2;
        int midY = (start.y + end.y) / 2;
        
        String distanceText = String.format("%.1f km", distance);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(distanceText);
        int textHeight = fm.getHeight();
        
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRoundRect(midX - textWidth/2 - 2, midY - textHeight/2 - 1, 
                         textWidth + 4, textHeight + 2, 3, 3);
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString(distanceText, midX - textWidth/2, midY + textHeight/3);
    }

    private void drawStations(Graphics2D g2d, GenericGraph<TrainStation, Rail> railwayGraph) {
        for (TrainStation station : railwayGraph.vertices()) {
            Point pos = stationPositions.get(station.name());
            if (pos == null) continue;
            
            g2d.setColor(new Color(0, 100, 200));
            g2d.fillOval(pos.x - 8, pos.y - 8, 16, 16);
            
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawOval(pos.x - 8, pos.y - 8, 16, 16);
            
            g2d.setColor(Color.WHITE);
            g2d.fillOval(pos.x - 3, pos.y - 3, 6, 6);
        }
    }

    private void drawStationLabels(Graphics2D g2d, GenericGraph<TrainStation, Rail> railwayGraph) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        
        for (TrainStation station : railwayGraph.vertices()) {
            Point pos = stationPositions.get(station.name());
            if (pos == null) continue;
            
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(station.name());
            int textHeight = fm.getHeight();
            
            int labelX = pos.x + 12;
            int labelY = pos.y + textHeight / 3;
            
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.fillRoundRect(labelX - 2, labelY - textHeight + 2, 
                             textWidth + 4, textHeight, 3, 3);
            
            g2d.setColor(Color.BLACK);
            g2d.drawString(station.name(), labelX, labelY);
        }
    }

    private void drawTrains(Graphics2D g2d) {
        int visibleTrains = 0;
        int movingTrains = 0;
        
        for (TrainPosition trainPos : trainPositions.values()) {
            visibleTrains++;
            
            Point fromPos = stationPositions.get(trainPos.fromStation.name());
            Point toPos = stationPositions.get(trainPos.toStation.name());
            
            if (fromPos == null || toPos == null) continue;
            

            int currentX = (int) (fromPos.x + trainPos.progress * (toPos.x - fromPos.x));
            int currentY = (int) (fromPos.y + trainPos.progress * (toPos.y - fromPos.y));
            

            if (trainPos.isMoving) {
    
                g2d.setColor(new Color(255, 100, 100));
                movingTrains++;
            } else if (trainPos.progress >= 1.0) {
    
                g2d.setColor(new Color(100, 255, 100));
            } else {
    
                g2d.setColor(new Color(150, 150, 150));
            }
            
            g2d.fillOval(currentX - 8, currentY - 8, 16, 16);
            

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawOval(currentX - 8, currentY - 8, 16, 16);
            

            if (trainPos.isMoving) {
                drawMovementArrow(g2d, fromPos, toPos, currentX, currentY);
            }

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            g2d.drawString(trainPos.trainId, currentX + 10, currentY - 10);
            

            drawProgressBar(g2d, currentX, currentY, trainPos.progress);
            

            drawTrainStatus(g2d, currentX, currentY, trainPos);
        }
    }

    private void drawMovementArrow(Graphics2D g2d, Point from, Point to, int currentX, int currentY) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length == 0) return;
        
        double unitX = dx / length;
        double unitY = dy / length;
        
        g2d.setColor(new Color(0, 150, 0));
        g2d.setStroke(new BasicStroke(2.0f));
        
        int arrowLength = 12;
        int arrowX = (int) (currentX + unitX * arrowLength);
        int arrowY = (int) (currentY + unitY * arrowLength);
        
        g2d.drawLine(currentX, currentY, arrowX, arrowY);
        
        double angle = Math.atan2(dy, dx);
        int arrowSize = 6;
        
        int x1 = (int) (arrowX - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (arrowY - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (arrowX - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (arrowY - arrowSize * Math.sin(angle + Math.PI / 6));
        
        g2d.fillPolygon(new int[]{arrowX, x1, x2}, new int[]{arrowY, y1, y2}, 3);
    }

    private void drawProgressBar(Graphics2D g2d, int x, int y, double progress) {
        int barWidth = 20;
        int barHeight = 4;
        int barX = x - barWidth / 2;
        int barY = y + 12;
        
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillRect(barX, barY, barWidth, barHeight);
        
        g2d.setColor(new Color(0, 200, 0));
        g2d.fillRect(barX, barY, (int) (barWidth * progress), barHeight);
        
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }

    private void drawUserPosition(Graphics2D g2d) {
        if (userPosition == null) return;
        
        g2d.setColor(new Color(255, 0, 0));
        g2d.fillOval(userPosition.x - 10, userPosition.y - 10, 20, 20);
        
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawOval(userPosition.x - 10, userPosition.y - 10, 20, 20);
        
        g2d.setColor(Color.WHITE);
        g2d.fillOval(userPosition.x - 4, userPosition.y - 4, 8, 8);
        
        g2d.setColor(new Color(255, 0, 0));
        int[] xPoints = {userPosition.x, userPosition.x - 6, userPosition.x + 6};
        int[] yPoints = {userPosition.y + 15, userPosition.y + 25, userPosition.y + 25};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRoundRect(userPosition.x - 20, userPosition.y - 35, 40, 20, 5, 5);
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth("VOCÊ");
        g2d.drawString("VOCÊ", userPosition.x - textWidth/2, userPosition.y - 20);
        
        if (userCurrentStation != null) {
            g2d.setColor(new Color(0, 0, 255, 200));
            g2d.fillRoundRect(userPosition.x - 30, userPosition.y + 30, 60, 15, 3, 3);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            fm = g2d.getFontMetrics();
            textWidth = fm.stringWidth(userCurrentStation);
            g2d.drawString(userCurrentStation, userPosition.x - textWidth/2, userPosition.y + 42);
        }
    }

    private void drawTrainStatus(Graphics2D g2d, int x, int y, TrainPosition trainPos) {
        String status;
        Color statusColor;
        
        if (trainPos.isMoving) {
            status = "MOVENDO";
            statusColor = new Color(0, 150, 0);
        } else if (trainPos.progress >= 1.0) {
            status = "CHEGOU";
            statusColor = new Color(0, 100, 0);
        } else {
            status = "PARADO";
            statusColor = new Color(100, 100, 100);
        }
        
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRoundRect(x - 25, y - 25, 50, 12, 3, 3);
        
        g2d.setColor(statusColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 8));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(status);
        g2d.drawString(status, x - textWidth/2, y - 15);
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
        }
    }

    private void pauseSimulation() {
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
    }

    private void resetSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        simulationTime = 0.0;
        simulationRunning = false;
        currentRoute = null;
        currentRouteIndex = 0;
        userPosition = null;
        userCurrentStation = null;
        trainPositions.clear();
        
        timeLabel.setText("Tempo: 0 minutos");
    }

    private void toggleSpeed() {
        if (simulationTimer != null) {
            if (simulationTimer.getDelay() == TIMER_DELAY) {
                simulationTimer.setDelay(TIMER_DELAY / 2);
            } else {
                simulationTimer.setDelay(TIMER_DELAY);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java Main <arquivo_ferrovia>");
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            new Main(args[0]);
        });
    }
}