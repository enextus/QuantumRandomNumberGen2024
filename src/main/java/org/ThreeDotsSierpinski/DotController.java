package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DotController extends JPanel {
    // Константы для конфигурации
    private static final int SIZE = 1000; // Размер панели
    private static final int DOT_SIZE = 2; // Размер точки
    private static final int TIMER_DELAY = 0; // Интервал между обновлениями в миллисекундах
    private static final int DOTS_PER_UPDATE = 1; // Количество точек, добавляемых за одно обновление
    private static final long MIN_RANDOM_VALUE = -99999999L; // Минимальное значение диапазона случайных чисел
    private static final long MAX_RANDOM_VALUE = 100000000L; // Максимальное значение диапазона случайных чисел

    // Константы для сообщений и логирования
    private static final String ERROR_NO_RANDOM_NUMBERS = "Больше нет доступных случайных чисел: ";
    private static final String LOG_DOTS_PROCESSED = "Обработано %d новых точек.";
    private static final String LOG_ERROR_MOVEMENT = "Обнаружена ошибка при перемещении точек: %s";

    private final List<Dot> dots; // Список точек
    private final List<Long> usedRandomNumbers; // Список использованных случайных чисел для визуализации
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    private int dotCounter; // Счетчик точек
    private volatile String errorMessage; // Сообщение об ошибке
    private Point currentPoint; // Текущее положение точки
    private final BufferedImage offscreenImage; // Буфер офф-скрина для рисования
    private final ScheduledExecutorService scheduler; // Планировщик для смены цвета точки

    private int currentRandomValueIndex = 0; // Порядковый номер текущего случайного числа
    private Long currentRandomValue; // Текущее случайное число

    private static final Logger LOGGER = LoggerConfig.getLogger();

    /**
     * Конструктор, принимающий RandomNumberProvider.
     *
     * @param randomNumberProvider Провайдер случайных чисел
     */
    public DotController(RandomNumberProvider randomNumberProvider) {
        this.randomNumberProvider = randomNumberProvider; // Инициализация провайдера случайных чисел
        currentPoint = new Point(SIZE / 2, SIZE / 2);
        setPreferredSize(new Dimension(SIZE + 300, SIZE));
        setBackground(Color.WHITE);
        dots = Collections.synchronizedList(new ArrayList<>());
        usedRandomNumbers = new ArrayList<>();
        dotCounter = 0;
        errorMessage = null;

        offscreenImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Запускает обновление точек с использованием Timer.
     */
    public void startDotMovement() {
        Timer timer = new Timer(TIMER_DELAY, e -> {
            if (errorMessage == null) {
                List<Dot> newDots = new ArrayList<>();

                for (int i = 0; i < DOTS_PER_UPDATE; i++) { // Цикл добавления точек
                    try {
                        currentRandomValueIndex++;

                        // Получение следующего случайного числа в указанном диапазоне
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);

                        // Сохранение текущего случайного числа и добавление его в список использованных чисел
                        currentRandomValue = randomValue;
                        usedRandomNumbers.add(currentRandomValue);

                        // Вычисление нового положения точки на основе случайного числа
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);

                        // Создание новой точки
                        Dot newDot = new Dot(new Point(currentPoint));
                        dotCounter++;
                        newDots.add(newDot);
                    } catch (NoSuchElementException ex) {
                        if (errorMessage == null) {
                            errorMessage = ex.getMessage();
                            LOGGER.log(Level.WARNING, ERROR_NO_RANDOM_NUMBERS + ex.getMessage());
                        }
                        ((Timer) e.getSource()).stop();
                        break; // Выход из цикла
                    }
                }

                dots.addAll(newDots); // Добавление всех точек в список
                drawDots(newDots, Color.RED); // Рисование новых точек красным цветом
                repaint(); // Перерисовка панели после добавления всех точек
                LOGGER.fine(String.format(LOG_DOTS_PROCESSED, newDots.size()));

                // Планируем смену цвета точки на черный через 1 секунду
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
    }

    /**
     * Отрисовывает панель.
     *
     * @param g Объект Graphics для рисования
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Вызов метода суперкласса для базовой отрисовки
        g.drawImage(offscreenImage, 0, 0, null); // Отрисовка буферного изображения

        // Отображение порядкового номера случайного числа
        g.setColor(Color.BLUE);
        g.drawString("Порядковый номер выборки: " + currentRandomValueIndex, 10, 20);

        // Отображение текущего случайного числа
        if (currentRandomValue != null) {
            g.setColor(Color.BLACK);
            g.drawString("Текущее случайное число: " + currentRandomValue, 10, 40);
        }

        // Отображение сообщения об ошибке, если есть
        if (errorMessage != null) {
            g.setColor(Color.RED);
            g.drawString(errorMessage, 10, 60);
        }

        // Отрисовка пирамиды из случайных чисел
        drawRandomNumbersPyramid(g);
    }

    /**
     * Метод для отрисовки пирамиды случайных чисел.
     *
     * @param g Объект Graphics для рисования
     */
    private void drawRandomNumbersPyramid(Graphics g) {
        g.setColor(Color.BLACK);

        int startX = SIZE + 20 + 450; // Начальная позиция по X (справа от треугольника) сдвинута вправо на 450 пикселей
        int startY = 20; // Начальная позиция по Y (вверху панели)
        int level = 1; // Текущий уровень пирамиды
        int numbersInLevel = 1; // Количество чисел на текущем уровне
        int index = 0; // Индекс текущего числа
        double shrinkFactor = 0.35; // Итоговый коэффициент уменьшения основания на 65%

        // Проходим по всем числам в usedRandomNumbers и располагаем их в форме пирамиды
        while (index < usedRandomNumbers.size()) {
            int levelWidth = (int) (numbersInLevel * 80 * shrinkFactor); // Общая ширина уровня, уменьшенная на 65%
            int xLevelStart = startX - levelWidth / 2; // Центрирование уровня по X с учетом shrinkFactor

            // Рисуем числа на текущем уровне
            for (int i = 0; i < numbersInLevel && index < usedRandomNumbers.size(); i++) {
                int x = xLevelStart + i * (int)(80 * shrinkFactor); // Учитываем уменьшенную ширину между числами
                int y = startY + level * 40;

                g.drawString(usedRandomNumbers.get(index).toString(), x, y);
                index++;
            }

            // Переход к следующему уровню
            level++;
            numbersInLevel++;
        }
    }


    /**
     * Рисует новые точки на буфере.
     *
     * @param newDots Список новых точек для рисования
     * @param color   Цвет для рисования точек
     */
    private void drawDots(List<Dot> newDots, Color color) {
        Graphics2D g2d = offscreenImage.createGraphics(); // Получение контекста графики буфера
        g2d.setColor(color); // Установка цвета для рисования точек
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE); // Рисование точки
        }
        g2d.dispose(); // Освобождение контекста графики
    }

    /**
     * Вычисляет новое положение точки на основе случайного числа.
     *
     * @param currentPoint Текущее положение точки
     * @param randomValue  Случайное число для определения направления движения
     * @return Новое положение точки
     */
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        long MinValue = MIN_RANDOM_VALUE;
        long MaxValue = MAX_RANDOM_VALUE;

        // Фиксированные вершины треугольника
        Point A = new Point(SIZE / 2, 0); // Верхняя вершина
        Point B = new Point(0, SIZE); // Левый нижний угол
        Point C = new Point(SIZE, SIZE); // Правый нижний угол

        long rangePart = (MaxValue - MinValue) / 3; // Разделение диапазона на три части

        int x = currentPoint.x;
        int y = currentPoint.y;

        if (randomValue <= MinValue + rangePart) {
            // Движение к вершине A
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MinValue + 2 * rangePart) {
            // Движение к вершине B
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            // Движение к вершине C
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }

        return new Point(x, y); // Возвращение нового положения точки
    }
}
