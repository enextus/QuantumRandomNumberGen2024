package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DotController extends JPanel {
    // Константы для конфигурации панели
    private static final int SIZE = 1000;
    private static final int DOT_SIZE = 2;
    private static final int TIMER_DELAY = 0;
    private static final int DOTS_PER_UPDATE = 1;
    private static final long MIN_RANDOM_VALUE = -99999999L;
    private static final long MAX_RANDOM_VALUE = 100000000L;

    // Константы для визуализации и параметров пирамиды

    private static final int LEVEL_HEIGHT = 40;
    private static final int NUM_SPACING = 80;


    // Константы для сообщений и логирования
    private static final String ERROR_NO_RANDOM_NUMBERS = "Больше нет доступных случайных чисел: ";
    private static final String LOG_DOTS_PROCESSED = "Обработано %d новых точек.";
    private static final String LOG_ERROR_MOVEMENT = "Обнаружена ошибка при перемещении точек: %s";

    // Константы для текстов на экране
    private static final String DRAW_STRING_SAMPLE_INDEX = "Порядковый номер выборки: %d";
    private static final String DRAW_STRING_CURRENT_RANDOM = "Текущее случайное число: %d";

    private final List<Dot> dots;
    private final List<Long> usedRandomNumbers;
    private final List<Point> fallenPositions; // Список для позиций, где "упали" числа
    private final RandomNumberProvider randomNumberProvider;
    private volatile String errorMessage;
    private Point currentPoint;
    private final BufferedImage offscreenImage;
    private final ScheduledExecutorService scheduler;
    private final Random random; // Для генерации случайных смещений

    private int currentRandomValueIndex = 0;
    private Long currentRandomValue;
    private static final Logger LOGGER = LoggerConfig.getLogger();

    private final List<Integer> numbers = new ArrayList<>(); // Инициализируйте список для хранения чисел

    public DotController(RandomNumberProvider randomNumberProvider) {
        this.randomNumberProvider = randomNumberProvider;
        currentPoint = new Point(SIZE / 2, SIZE / 2);
        setPreferredSize(new Dimension(SIZE + 300, SIZE));
        setBackground(Color.WHITE);
        dots = Collections.synchronizedList(new ArrayList<>());
        usedRandomNumbers = new ArrayList<>();
        fallenPositions = new ArrayList<>(); // Инициализация списка позиций упавших чисел
        errorMessage = null;

        offscreenImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        scheduler = Executors.newScheduledThreadPool(1);
        random = new Random();
    }

    public void startDotMovement() {
        Timer timer = new Timer(TIMER_DELAY, e -> {
            if (errorMessage == null) {
                List<Dot> newDots = new ArrayList<>();
                for (int i = 0; i < DOTS_PER_UPDATE; i++) {
                    try {
                        currentRandomValueIndex++;
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);
                        currentRandomValue = randomValue;
                        usedRandomNumbers.add(currentRandomValue);
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);
                        Dot newDot = new Dot(new Point(currentPoint));
                        newDots.add(newDot);

                        // Добавление полученного числа в список numbers
                        numbers.add((int) randomValue);
                    } catch (NoSuchElementException ex) {
                        if (errorMessage == null) {
                            errorMessage = ex.getMessage();
                            LOGGER.log(Level.WARNING, ERROR_NO_RANDOM_NUMBERS + ex.getMessage());
                        }
                        ((Timer) e.getSource()).stop();
                        break;
                    }
                }

                dots.addAll(newDots);
                drawDots(newDots, Color.RED);
                repaint();
                LOGGER.fine(String.format(LOG_DOTS_PROCESSED, newDots.size()));

                scheduler.schedule(() -> {
                    drawDots(newDots, Color.BLACK);
                    repaint();
                }, 1, TimeUnit.SECONDS);
            } else {
                ((Timer) e.getSource()).stop();
                repaint();
                LOGGER.severe(String.format(LOG_ERROR_MOVEMENT, errorMessage));
            }
        });
        timer.start();

        // Запуск процесса "падения" чисел
        scheduler.scheduleAtFixedRate(this::updateFallingNumbers, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(offscreenImage, 0, 0, null);

        g.setColor(Color.BLUE);
        g.drawString(String.format(DRAW_STRING_SAMPLE_INDEX, currentRandomValueIndex), 10, 20);

        if (currentRandomValue != null) {
            g.setColor(Color.BLACK);
            g.drawString(String.format(DRAW_STRING_CURRENT_RANDOM, currentRandomValue), 10, 40);
        }

        if (errorMessage != null) {
            g.setColor(Color.RED);
            g.drawString(errorMessage, 10, 60);
        }

        drawFallingNumbers(g);

        // Draw "fallen" numbers at their respective positions
        g.setColor(Color.MAGENTA);
        for (int i = 0; i < fallenPositions.size() && i < numbers.size(); i++) {
            Point position = fallenPositions.get(i);
            g.drawString(String.valueOf(numbers.get(i)), position.x, position.y);
        }
    }


    private static final int BASE_WIDTH = 20; // Количество чисел в самом нижнем уровне
    private static final int HEIGHT = 10; // Количество уровней

    private void drawFallingNumbers(Graphics g) {
        int level = 0;
        int currentNumberIndex = 0;
        int offsetX = 900; // Смещение вправо на 600 пикселей

        // Начинаем от основания треугольника и двигаемся вверх
        for (int i = HEIGHT; i > 0; i--) {
            int numbersInLevel = BASE_WIDTH * i / HEIGHT; // Количество чисел на текущем уровне
            int startX = ((SIZE - numbersInLevel * 20) / 2) + offsetX; // Центрируем каждый уровень по X и смещаем вправо
            int startY = SIZE - (level * 20); // Смещение вверх для уровня

            for (int j = 0; j < numbersInLevel && currentNumberIndex < numbers.size(); j++) {
                int x = startX + (j * 20); // Позиция X для каждого числа, равномерно распределяя по уровню
                drawNumber(g, numbers.get(currentNumberIndex), x, startY);
                currentNumberIndex++;
            }
            level++;
        }
    }

    // Метод для отрисовки числа
    private void drawNumber(Graphics g, int number, int x, int y) {
        g.drawString(String.valueOf(number), x, y);
    }

    private void updateFallingNumbers() {
        if (!usedRandomNumbers.isEmpty()) {
            // Добавляем смещение для падения каждого нового числа
            int offsetX = (random.nextInt(3) - 1) * NUM_SPACING / 4; // случайное смещение по X
            int offsetY = (random.nextInt(3) - 1) * LEVEL_HEIGHT / 2; // случайное смещение по Y

            fallenPositions.add(new Point(offsetX, offsetY)); // Добавляем новое случайное положение для числа
            repaint();
        }
    }

    private void drawDots(List<Dot> newDots, Color color) {
        Graphics2D g2d = offscreenImage.createGraphics();
        g2d.setColor(color);
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE);
        }
        g2d.dispose();
    }

    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        Point A = new Point(SIZE / 2, 0);
        Point B = new Point(0, SIZE);
        Point C = new Point(SIZE, SIZE);

        long rangePart = (MAX_RANDOM_VALUE - MIN_RANDOM_VALUE) / 3;

        int x = currentPoint.x;
        int y = currentPoint.y;

        if (randomValue <= MIN_RANDOM_VALUE + rangePart) {
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MIN_RANDOM_VALUE + 2 * rangePart) {
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }

        return new Point(x, y);
    }
}
