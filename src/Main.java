import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake Xenzia");
            frame.setSize(500, 500);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            // Panel menu
            JPanel menuPanel = new JPanel();
            menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
            menuPanel.setBackground(Color.BLACK);

            JLabel title = new JLabel("SNAKE XENZIA", SwingConstants.CENTER);
            title.setForeground(Color.GREEN);
            title.setFont(new Font("Arial", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            menuPanel.add(title);

            JButton startButton = createButton("Start Game", Color.GREEN, Color.BLACK);
            JButton instructionsButton = createButton("Instructions", Color.GREEN, Color.BLACK);
            JButton exitButton = createButton("Exit", Color.RED, Color.WHITE);

            JLabel highScoreLabel = new JLabel("High Score: -", SwingConstants.CENTER);
            highScoreLabel.setForeground(Color.YELLOW);
            highScoreLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            highScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            menuPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            menuPanel.add(highScoreLabel);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            menuPanel.add(startButton);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            menuPanel.add(instructionsButton);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            menuPanel.add(exitButton);

            SnakeGame gamePanel = new SnakeGame();

            mainPanel.add(menuPanel, "Menu");
            mainPanel.add(gamePanel, "Game");

            startButton.addActionListener(e -> cardLayout.show(mainPanel, "Game"));
            instructionsButton.addActionListener(e -> showMessage(frame, "Use arrow keys to move the snake.\nEat the food to grow!"));
            exitButton.addActionListener(e -> {
                frame.dispose();
                System.exit(0);
            });

            frame.add(mainPanel);

            // Load high score from database
            loadHighScore(highScoreLabel);

            frame.setVisible(true);
        });
    }

    private static JButton createButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 16));
        return button;
    }

    private static void showMessage(JFrame frame, String message) {
        JOptionPane.showMessageDialog(frame, message);
    }

    private static void loadHighScore(JLabel label) {
        String url = "jdbc:mysql://localhost:3306/project_snake";
        String username = "root";
        String password = "";
        String query = "SELECT name, score FROM high_scores ORDER BY score DESC LIMIT 1";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            if (resultSet.next()) {
                String name = resultSet.getString("name");
                int score = resultSet.getInt("score");
                label.setText("High Score: " + name + " - " + score);
            } else {
                label.setText("High Score: -");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            label.setText("High Score: Database Error");
        }
    }
}

class SnakeGame extends JPanel implements ActionListener, KeyListener {
    private final int TILE_SIZE = 24;
    private final int GRID_WIDTH = 20;
    private final int GRID_HEIGHT = 20;
    private final int GAME_SPEED = 150;

    private Timer timer;
    private List<Point> snake = new ArrayList<>();
    private Point food;
    private List<Point> goldenFoods = new ArrayList<>();
    private final int MAX_GOLDEN_FOODS = 5;
    private final double GOLDEN_FOOD_CHANCE = 0.15;
    private int direction = KeyEvent.VK_RIGHT;
    private boolean gameOver = false;

    public SnakeGame() {
        setPreferredSize(new Dimension(GRID_WIDTH * TILE_SIZE, GRID_HEIGHT * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        startGame();
    }

    private void startGame() {
        snake.clear();
        goldenFoods.clear();
        snake.add(new Point(5, 5));
        direction = KeyEvent.VK_RIGHT;
        spawnFood();
        gameOver = false;
        timer = new Timer(GAME_SPEED, this);
        timer.start();
    }

    private void spawnFood() {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(GRID_WIDTH);
            y = rand.nextInt(GRID_HEIGHT);
        } while (snake.contains(new Point(x, y)) || goldenFoods.contains(new Point(x, y)));
        food = new Point(x, y);

        if (goldenFoods.size() < MAX_GOLDEN_FOODS && rand.nextDouble() < GOLDEN_FOOD_CHANCE) {
            do {
                x = rand.nextInt(GRID_WIDTH);
                y = rand.nextInt(GRID_HEIGHT);
            } while (snake.contains(new Point(x, y)) || food.equals(new Point(x, y)) || goldenFoods.contains(new Point(x, y)));
            goldenFoods.add(new Point(x, y));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            moveSnake();
            checkCollision();
            repaint();
        }
    }

    private void moveSnake() {
        Point head = snake.get(0);
        Point newHead = switch (direction) {
            case KeyEvent.VK_UP -> new Point(head.x, head.y - 1);
            case KeyEvent.VK_DOWN -> new Point(head.x, head.y + 1);
            case KeyEvent.VK_LEFT -> new Point(head.x - 1, head.y);
            case KeyEvent.VK_RIGHT -> new Point(head.x + 1, head.y);
            default -> head;
        };

        snake.add(0, newHead);

        if (newHead.equals(food)) {
            spawnFood();
        } else if (goldenFoods.contains(newHead)) {
            goldenFoods.remove(newHead);
            for (int i = 0; i < 5; i++) {
                snake.add(snake.get(snake.size() - 1));
            }
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private void checkCollision() {
        Point head = snake.get(0);
        if (head.x < 0 || head.y < 0 || head.x >= GRID_WIDTH || head.y >= GRID_HEIGHT || snake.subList(1, snake.size()).contains(head)) {
            gameOver = true;
            timer.stop();
            saveScore(snake.size() - 1);
            JOptionPane.showMessageDialog(this, "Game Over! Skor: " + (snake.size() - 1), "Game Over", JOptionPane.INFORMATION_MESSAGE);
            startGame();
        }
    }

    private void saveScore(int score) {
        String url = "jdbc:mysql://localhost:3306/project_snake";
        String username = "root";
        String password = "";
        String query = "INSERT INTO high_scores (name, score, date) VALUES (?, ?, ?)";

        String playerName = JOptionPane.showInputDialog(this, "Enter your name:", "Player");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, playerName);
            statement.setInt(2, score);
            statement.setString(3, currentDate);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving score to database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= GRID_WIDTH; i++) {
            g.drawLine(i * TILE_SIZE, 0, i * TILE_SIZE, GRID_HEIGHT * TILE_SIZE);
        }
        for (int i = 0; i <= GRID_HEIGHT; i++) {
            g.drawLine(0, i * TILE_SIZE, GRID_WIDTH * TILE_SIZE, i * TILE_SIZE);
        }

        g.setColor(Color.GREEN);
        for (Point point : snake) {
            g.fillRect(point.x * TILE_SIZE, point.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        g.setColor(Color.RED);
        g.fillRect(food.x * TILE_SIZE, food.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        g.setColor(Color.YELLOW);
        for (Point goldenFood : goldenFoods) {
            g.fillRect(goldenFood.x * TILE_SIZE, goldenFood.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        g.setColor(Color.WHITE);
        g.drawString("Skor: " + (snake.size() - 1), 10, 10);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int newDirection = e.getKeyCode();
        if ((newDirection == KeyEvent.VK_UP && direction != KeyEvent.VK_DOWN) ||
                (newDirection == KeyEvent.VK_DOWN && direction != KeyEvent.VK_UP) ||
                (newDirection == KeyEvent.VK_LEFT && direction != KeyEvent.VK_RIGHT) ||
                (newDirection == KeyEvent.VK_RIGHT && direction != KeyEvent.VK_LEFT)) {
            direction = newDirection;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}
