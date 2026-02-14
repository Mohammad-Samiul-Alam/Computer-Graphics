import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Elevator (Lift) Operation Simulation
 * Implements a graphical elevator with floor buttons, moving cabin,
 * door animation, and floor indicators.
 */
public class ElevatorSimulation extends JFrame {
    private static final int NUM_FLOORS = 6; // 0 (ground) to 5
    private static final int GROUND_FLOOR = 0;
    private static final int TOP_FLOOR = 5;

    private BuildingPanel buildingPanel;
    private JLabel statusLabel;

    public ElevatorSimulation() {
        setTitle("Elevator Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create the drawing panel for the elevator shaft and car
        buildingPanel = new BuildingPanel();
        add(buildingPanel, BorderLayout.CENTER);

        // Create control panel with floor buttons
        JPanel controlPanel = new JPanel(new GridLayout(NUM_FLOORS + 2, 1, 5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Floor Requests"));

        for (int floor = TOP_FLOOR; floor >= GROUND_FLOOR; floor--) {
            final int f = floor;
            JButton button = new JButton("Floor " + (floor == 0 ? "G" : floor));
            button.addActionListener(e -> buildingPanel.requestFloor(f));
            controlPanel.add(button);
        }

        // Status label
        statusLabel = new JLabel("Status: Idle at Ground Floor");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        controlPanel.add(statusLabel);

        add(controlPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Custom JPanel that draws the elevator shaft, car, doors, and floor indicators.
     * Also contains the elevator logic and animation timer.
     */
    class BuildingPanel extends JPanel implements ActionListener {
        // Dimensions
        private static final int SHAFT_WIDTH = 200;
        private static final int SHAFT_HEIGHT = 500;
        private static final int MARGIN = 50;
        private static final int FLOOR_HEIGHT = SHAFT_HEIGHT / NUM_FLOORS;

        // Elevator state
        private enum State { IDLE, MOVING, DOOR_OPENING, DOOR_OPEN, DOOR_CLOSING }

        private State currentState = State.IDLE;
        private double currentFloor = GROUND_FLOOR; // precise position (floors)
        private int targetFloor = GROUND_FLOOR;
        private List<Integer> requestQueue = new ArrayList<>();
        private double doorExtent = 0.0; // 0 = closed, 1 = fully open
        private int waitCounter = 0;
        private static final int WAIT_TICKS = 50; // about 1.5 sec at 30ms tick
        private static final double SPEED = 0.03; // floors per tick
        private static final double DOOR_SPEED = 0.05; // per tick

        private Timer timer;

        public BuildingPanel() {
            setPreferredSize(new Dimension(SHAFT_WIDTH + 2 * MARGIN, SHAFT_HEIGHT + 2 * MARGIN));
            setBackground(Color.LIGHT_GRAY);

            timer = new Timer(30, this); // 30 ms per tick
            timer.start();
        }

        /**
         * Called by floor buttons to request a floor.
         */
        public void requestFloor(int floor) {
            if (floor < GROUND_FLOOR || floor > TOP_FLOOR) return;
            // Avoid duplicate requests
            if (!requestQueue.contains(floor) && floor != (int) Math.round(currentFloor)) {
                requestQueue.add(floor);
            }
            // If idle, start moving to the first request
            if (currentState == State.IDLE && !requestQueue.isEmpty()) {
                targetFloor = requestQueue.get(0);
                currentState = State.MOVING;
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // State machine for elevator
            switch (currentState) {
                case IDLE:
                    // Nothing to do
                    break;

                case MOVING:
                    moveStep();
                    break;

                case DOOR_OPENING:
                    doorExtent += DOOR_SPEED;
                    if (doorExtent >= 1.0) {
                        doorExtent = 1.0;
                        currentState = State.DOOR_OPEN;
                        waitCounter = WAIT_TICKS;
                    }
                    break;

                case DOOR_OPEN:
                    waitCounter--;
                    if (waitCounter <= 0) {
                        currentState = State.DOOR_CLOSING;
                    }
                    break;

                case DOOR_CLOSING:
                    doorExtent -= DOOR_SPEED;
                    if (doorExtent <= 0.0) {
                        doorExtent = 0.0;
                        // Remove current floor from queue if it was a request
                        int currentInt = (int) Math.round(currentFloor);
                        requestQueue.remove(Integer.valueOf(currentInt));

                        if (!requestQueue.isEmpty()) {
                            targetFloor = requestQueue.get(0);
                            currentState = State.MOVING;
                        } else {
                            currentState = State.IDLE;
                        }
                    }
                    break;
            }

            // Update status label
            updateStatus();

            // Repaint the panel
            repaint();
        }

        /**
         * Move the car one step towards the target floor.
         */
        private void moveStep() {
            double diff = targetFloor - currentFloor;
            if (Math.abs(diff) < SPEED) {
                currentFloor = targetFloor;
                // Arrived at floor
                currentState = State.DOOR_OPENING;
                doorExtent = 0.0;
            } else {
                currentFloor += Math.signum(diff) * SPEED;
            }
        }

        /**
         * Update the status label with current floor, direction, and door state.
         */
        private void updateStatus() {
            int floorInt = (int) Math.round(currentFloor);
            String floorName = (floorInt == 0) ? "Ground" : String.valueOf(floorInt);
            String direction = "";
            if (currentState == State.MOVING) {
                direction = (targetFloor > currentFloor) ? "Up" : "Down";
            }
            String doorStatus = "";
            if (currentState == State.DOOR_OPENING || currentState == State.DOOR_OPEN || currentState == State.DOOR_CLOSING) {
                doorStatus = "Door " + (currentState == State.DOOR_OPENING ? "Opening" :
                                        currentState == State.DOOR_OPEN ? "Open" : "Closing");
            }
            statusLabel.setText(String.format("Floor: %s  %s  %s", floorName, direction, doorStatus));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Draw background and shaft
            g2.setColor(Color.WHITE);
            g2.fillRect(MARGIN, MARGIN, SHAFT_WIDTH, SHAFT_HEIGHT);
            g2.setColor(Color.BLACK);
            g2.drawRect(MARGIN, MARGIN, SHAFT_WIDTH, SHAFT_HEIGHT);

            // Draw floor lines and labels
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            for (int i = 0; i <= NUM_FLOORS; i++) {
                int y = MARGIN + SHAFT_HEIGHT - i * FLOOR_HEIGHT;
                g2.drawLine(MARGIN, y, MARGIN + SHAFT_WIDTH, y);
                String floorLabel = (i == 0) ? "G" : String.valueOf(i);
                g2.drawString(floorLabel, MARGIN - 25, y - 5);
            }

            // Draw elevator car (rectangle)
            int carY = MARGIN + SHAFT_HEIGHT - (int) (currentFloor * FLOOR_HEIGHT) - FLOOR_HEIGHT;
            g2.setColor(Color.BLUE);
            g2.fillRect(MARGIN + 10, carY, SHAFT_WIDTH - 20, FLOOR_HEIGHT - 2);
            g2.setColor(Color.BLACK);
            g2.drawRect(MARGIN + 10, carY, SHAFT_WIDTH - 20, FLOOR_HEIGHT - 2);

            // Draw doors (if open)
            if (doorExtent > 0) {
                int doorWidth = (SHAFT_WIDTH - 20) / 2;
                int doorHeight = FLOOR_HEIGHT - 2;
                int leftDoorX = MARGIN + 10;
                int rightDoorX = MARGIN + 10 + doorWidth;

                // Left door slides left, right door slides right
                int leftOffset = (int) (doorWidth * doorExtent);
                int rightOffset = (int) (doorWidth * doorExtent);

                g2.setColor(new Color(139, 69, 19)); // brown doors
                g2.fillRect(leftDoorX - leftOffset, carY, doorWidth, doorHeight);
                g2.fillRect(rightDoorX + rightOffset, carY, doorWidth, doorHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(leftDoorX - leftOffset, carY, doorWidth, doorHeight);
                g2.drawRect(rightDoorX + rightOffset, carY, doorWidth, doorHeight);
            }

            // Draw direction arrow if moving
            if (currentState == State.MOVING) {
                g2.setColor(Color.RED);
                int arrowX = MARGIN + SHAFT_WIDTH + 10;
                int arrowY = carY + FLOOR_HEIGHT / 2;
                if (targetFloor > currentFloor) {
                    // Up arrow
                    g2.fillPolygon(new int[]{arrowX, arrowX + 10, arrowX + 5},
                                   new int[]{arrowY + 5, arrowY + 5, arrowY - 5}, 3);
                } else {
                    // Down arrow
                    g2.fillPolygon(new int[]{arrowX, arrowX + 10, arrowX + 5},
                                   new int[]{arrowY - 5, arrowY - 5, arrowY + 5}, 3);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ElevatorSimulation::new);
    }
}