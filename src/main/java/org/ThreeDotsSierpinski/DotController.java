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
    private static final int SIZE = 900; // Основной размер панели по высоте и ширине (в пикселях)
    private static final int DOT_SIZE = 2; // Размер отображаемой точки (ширина и высота в пикселях)

    // Константы для управления скоростью наполнения треугольников
    /**
     * FILLING_SPEED_MAIN определяет интервал времени (в миллисекундах) между добавлениями новых точек в основной треугольник.
     * Например, если FILLING_SPEED_MAIN = 10, то новые точки будут добавляться каждые 10 миллисекунд.
     */
    private static final int FILLING_SPEED_MAIN = 10; // Интервал добавления новых точек в основной треугольник (в миллисекундах)

    /**
     * FILLING_SPEED_SECONDARY определяет интервал времени (в миллисекундах) между добавлениями новых чисел в правый треугольник.
     * Например, если FILLING_SPEED_SECONDARY = 1000, то новые числа будут добавляться каждую секунду.
     */
    private static final int FILLING_SPEED_SECONDARY = 500; // Интервал добавления новых чисел в правый треугольник (в миллисекундах)

    // **Константа для смещения правого треугольника**
    /**
     * RIGHT_TRIANGLE_OFFSET_X определяет горизонтальное смещение правого треугольника со случайными числами.
     * Значение 1000 пикселей позволяет разместить треугольник справа от основного треугольника Серпинского.
     */
    private static final int RIGHT_TRIANGLE_OFFSET_X = 900; // Горизонтальное смещение правого треугольника (в пикселях)

    private static final long MIN_RANDOM_VALUE = -99999999L; // Минимальное значение для генерации случайных чисел
    private static final long MAX_RANDOM_VALUE = 100000000L; // Максимальное значение для генерации случайных чисел

    // **Константы для треугольника случайных чисел**
    /**
     * BASE_WIDTH определяет количество чисел на самом нижнем уровне треугольника.
     * Увеличение этого значения увеличивает ширину основания треугольника,
     * позволяя разместить больше чисел на нижнем уровне.
     * Например, BASE_WIDTH = 16 означает, что на самом нижнем уровне будет 16 чисел,
     * и с каждым верхним уровнем количество чисел будет уменьшаться пропорционально.
     */
    private static final int BASE_WIDTH = 15;

    /**
     * HEIGHT определяет количество уровней в треугольнике случайных чисел.
     * Увеличение этого значения увеличивает высоту треугольника,
     * добавляя больше уровней чисел.
     * Например, HEIGHT = 48 означает, что треугольник будет состоять из 48 уровней,
     * начиная с 16 чисел на нижнем уровне и уменьшаясь по одному числу на каждый верхний уровень.
     */
    private static final int HEIGHT = 48;

    /**
     * LEVEL_HEIGHT определяет вертикальное расстояние между уровнями треугольника.
     * Увеличение этого значения увеличивает высоту каждого уровня,
     * делая треугольник выше и уменьшая плотность размещения чисел по вертикали.
     * Например, LEVEL_HEIGHT = 19 пикселей означает, что между каждым уровнем будет 19 пикселей вертикального пространства.
     */
    private static final int LEVEL_HEIGHT = 19;

    /**
     * NUM_SPACING определяет горизонтальное расстояние между числами на одном уровне треугольника.
     * Увеличение этого значения увеличивает расстояние между числами,
     * делая треугольник шире и числа менее плотными по горизонтали.
     * Например, NUM_SPACING = 60 пикселей означает, что между двумя соседними числами на одном уровне будет 60 пикселей горизонтального пространства.
     */
    private static final int NUM_SPACING = 60;

    // Константы для сообщений и логирования
    private static final String ERROR_NO_RANDOM_NUMBERS = "Больше нет доступных случайных чисел: ";
    private static final String LOG_DOTS_PROCESSED = "Обработано %d новых точек.";

    // Константы для текстов на экране
    private static final String DRAW_STRING_SAMPLE_INDEX = "Порядковый номер выборки: %d";
    private static final String DRAW_STRING_CURRENT_RANDOM = "Текущее случайное число: %d";

    private final List<Dot> dots; // Список всех точек, отображаемых на основном треугольнике
    private final List<Integer> numbers; // Список для хранения чисел, используемых в правом треугольнике
    private final List<Point> fallenPositions; // Список позиций, где "упали" числа
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    private volatile String errorMessage; // Сообщение об ошибке, если оно возникло
    private Point currentPoint; // Текущая позиция точки для рисования
    private final BufferedImage offscreenImage; // Буфер для двойной буферизации графики
    private final ScheduledExecutorService scheduler; // Планировщик задач для отложенных действий
    private final Random random; // Генератор случайных чисел для смещений

    private int currentRandomValueIndex = 0; // Текущий индекс случайного числа (счетчик)
    private Long currentRandomValue; // Текущее случайное число
    private static final Logger LOGGER = LoggerConfig.getLogger(); // Логгер для записи событий

    // Таймеры для наполнения треугольников
    private Timer mainFillingTimer; // Таймер для основного треугольника
    private Timer secondaryFillingTimer; // Таймер для правого треугольника

    // **Инициализация списка использованных случайных чисел**
    private final List<Long> usedRandomNumbers; // Список использованных случайных чисел для предотвращения повторений

    public DotController(RandomNumberProvider randomNumberProvider) {
        this.randomNumberProvider = randomNumberProvider;
        currentPoint = new Point(SIZE / 2, SIZE / 2); // Инициализация текущей точки в центре панели

        // **Увеличиваем размеры панели на 33% по ширине и высоте**
        // Это обеспечивает дополнительное пространство для отображения правого треугольника со случайными числами
        setPreferredSize(new Dimension((int)((SIZE + 300) * 1.33), (int)(SIZE * 1.33)));
        setBackground(Color.WHITE); // Установка фона панели в белый цвет

        dots = Collections.synchronizedList(new ArrayList<>()); // Инициализация синхронизированного списка точек
        numbers = new ArrayList<>(); // Инициализация списка чисел для правого треугольника
        fallenPositions = new ArrayList<>(); // Инициализация списка позиций упавших чисел
        usedRandomNumbers = new ArrayList<>(); // Инициализация списка использованных случайных чисел
        errorMessage = null; // Инициализация отсутствием ошибок

        offscreenImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB); // Создание буфера для графики
        scheduler = Executors.newScheduledThreadPool(1); // Создание планировщика с одним потоком
        random = new Random(); // Инициализация генератора случайных чисел

        // Инициализация таймеров для наполнения треугольников
        initializeMainFillingTimer();
        initializeSecondaryFillingTimer();
    }

    /**
     * Инициализация таймера для наполнения основного треугольника точками.
     * Точки будут добавляться с интервалом, определяемым FILLING_SPEED_MAIN.
     */
    private void initializeMainFillingTimer() {
        mainFillingTimer = new Timer(FILLING_SPEED_MAIN, e -> {
            try {
                currentRandomValueIndex++;
                long randomValue = randomNumberProvider.getNextRandomNumberInRange(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);
                currentRandomValue = randomValue;
                currentPoint = calculateNewDotPosition(currentPoint, randomValue);
                Dot newDot = new Dot(new Point(currentPoint));
                dots.add(newDot);

                // Рисование новой точки красным цветом
                drawDots(Collections.singletonList(newDot), Color.RED);
                repaint();
                LOGGER.fine(String.format(LOG_DOTS_PROCESSED, 1));

                // Запланировать смену цвета точки на черный через 1 секунду
                scheduler.schedule(() -> {
                    drawDots(Collections.singletonList(newDot), Color.BLACK);
                    repaint();
                }, 1, TimeUnit.SECONDS);
            } catch (NoSuchElementException ex) {
                if (errorMessage == null) {
                    errorMessage = ex.getMessage();
                    LOGGER.log(Level.WARNING, ERROR_NO_RANDOM_NUMBERS + ex.getMessage());
                }
                mainFillingTimer.stop(); // Остановка таймера при ошибке
            }
        });
        mainFillingTimer.start(); // Запуск таймера наполнения основного треугольника
    }

    /**
     * Инициализация таймера для наполнения правого треугольника случайными числами.
     * Числа будут добавляться с интервалом, определяемым FILLING_SPEED_SECONDARY.
     */
    private void initializeSecondaryFillingTimer() {
        secondaryFillingTimer = new Timer(FILLING_SPEED_SECONDARY, e -> {
            try {
                long randomValue = randomNumberProvider.getNextRandomNumberInRange(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);
                currentRandomValue = randomValue;
                usedRandomNumbers.add(randomValue);

                // Определение новой позиции для числа в правом треугольнике
                Point newPosition = calculateNewNumberPosition();
                numbers.add((int) randomValue);

                // Добавление новой позиции в список упавших чисел
                fallenPositions.add(newPosition);

                repaint(); // Перерисовка панели для отображения нового числа
                LOGGER.fine(String.format(LOG_DOTS_PROCESSED, 1));
            } catch (NoSuchElementException ex) {
                if (errorMessage == null) {
                    errorMessage = ex.getMessage();
                    LOGGER.log(Level.WARNING, ERROR_NO_RANDOM_NUMBERS + ex.getMessage());
                }
                secondaryFillingTimer.stop(); // Остановка таймера при ошибке
            }
        });
        secondaryFillingTimer.start(); // Запуск таймера наполнения правого треугольника
    }

    public void startDotMovement() {
        // Запуск процесса "падения" чисел каждые 500 миллисекунд
        scheduler.scheduleAtFixedRate(this::updateFallingNumbers, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(offscreenImage, 0, 0, null); // Отрисовка буферного изображения

        // Отображение индекса текущей выборки
        g.setColor(Color.BLUE);
        g.drawString(String.format(DRAW_STRING_SAMPLE_INDEX, currentRandomValueIndex), 10, 20);

        // Отображение текущего случайного числа, если оно существует
        if (currentRandomValue != null) {
            g.setColor(Color.BLACK);
            g.drawString(String.format(DRAW_STRING_CURRENT_RANDOM, currentRandomValue), 10, 40);
        }

        // Отображение сообщения об ошибке, если оно существует
        if (errorMessage != null) {
            g.setColor(Color.RED);
            g.drawString(errorMessage, 10, 60);
        }

        drawFallingNumbers(g); // Отрисовка падающих чисел

        // Отрисовка "упавших" чисел в их позициях
        g.setColor(Color.MAGENTA);
        for (int i = 0; i < fallenPositions.size() && i < numbers.size(); i++) {
            Point position = fallenPositions.get(i);
            g.drawString(String.valueOf(numbers.get(i)), position.x, position.y);
        }
    }

    /**
     * Метод для отрисовки чисел в правом треугольнике.
     * Использует константу RIGHT_TRIANGLE_OFFSET_X для смещения треугольника вправо.
     *
     * @param g графический контекст.
     */
    private void drawFallingNumbers(Graphics g) {
        int level = 0; // Текущий уровень треугольника
        int currentNumberIndex = 0; // Индекс текущего числа для отрисовки

        // Использование константы для смещения правого треугольника
        // Смещение вправо на 1000 пикселей

        // Начинаем от основания треугольника и двигаемся вверх по уровням
        for (int i = HEIGHT; i > 0; i--) {
            // Определяем количество чисел на текущем уровне
            // Пропорционально уменьшенное число для верхних уровней
            int numbersInLevel = BASE_WIDTH * i / HEIGHT;

            // Обеспечиваем, что numbersInLevel >= 1
            numbersInLevel = Math.max(1, numbersInLevel);

            // Рассчитываем начальную позицию X для текущего уровня
            // Мы центрируем уровень по X и смещаем вправо на offsetX
            int startX = ((SIZE - numbersInLevel * NUM_SPACING) / 2) + RIGHT_TRIANGLE_OFFSET_X;

            // Рассчитываем позицию Y для текущего уровня
            // Мы начинаем от нижней части панели и двигаемся вверх с шагом LEVEL_HEIGHT
            int startY = SIZE - (level * LEVEL_HEIGHT);

            // Мы отрисовываем числа на текущем уровне
            for (int j = 0; j < numbersInLevel && currentNumberIndex < numbers.size(); j++) {
                // Позиция X для каждого числа на уровне
                int x = startX + (j * NUM_SPACING);

                // Отрисовка числа на рассчитанной позиции
                drawNumber(g, numbers.get(currentNumberIndex), x, startY);
                currentNumberIndex++;
            }
            level++; // Переход к следующему уровню выше
        }
    }

    // Метод для отрисовки отдельного числа
    private void drawNumber(Graphics g, int number, int x, int y) {
        g.drawString(String.valueOf(number), x, y);
    }

    /**
     * Метод для расчёта новой позиции числа в правом треугольнике.
     * Вычисляет позицию на основе случайного выбора уровня и позиции на уровне.
     *
     * @return новая позиция числа.
     */
    private Point calculateNewNumberPosition() {
        // Выбор случайного уровня
        int level = random.nextInt(HEIGHT);
        int numbersInLevel = BASE_WIDTH * (HEIGHT - level) / HEIGHT;

        // Обеспечиваем, что numbersInLevel >= 1
        numbersInLevel = Math.max(1, numbersInLevel);

        // Рассчитываем начальную позицию X для уровня с использованием константы
        int startX = ((SIZE - numbersInLevel * NUM_SPACING) / 2) + RIGHT_TRIANGLE_OFFSET_X; // Смещение вправо на 1000 пикселей

        // Рассчитываем позицию Y для уровня
        int startY = SIZE - (level * LEVEL_HEIGHT);

        // Выбор случайной позиции на уровне
        int j = random.nextInt(numbersInLevel);
        int x = startX + (j * NUM_SPACING);

        return new Point(x, startY);
    }

    private void updateFallingNumbers() {
        if (!usedRandomNumbers.isEmpty()) {
            // Генерация случайных смещений для создания эффекта "падения" чисел
            // Смещение по X: случайное значение от -1 до 1, умноженное на четверть расстояния между числами
            int offsetX = (random.nextInt(3) - 1) * NUM_SPACING / 4; // случайное смещение по X

            // Смещение по Y: случайное значение от -1 до 1, умноженное на половину высоты уровня
            int offsetY = (random.nextInt(3) - 1) * LEVEL_HEIGHT / 2; // случайное смещение по Y

            // Добавление нового положения для числа с учётом смещений
            fallenPositions.add(new Point(offsetX, offsetY));
            repaint(); // Перерисовка панели для отображения изменений
        }
    }

    // Метод для отрисовки новых точек на буферном изображении
    private void drawDots(List<Dot> newDots, Color color) {
        Graphics2D g2d = offscreenImage.createGraphics(); // Получение графического контекста
        g2d.setColor(color); // Установка цвета для отрисовки точек
        for (Dot dot : newDots) {
            // Отрисовка точки как заполненного прямоугольника
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE);
        }
        g2d.dispose(); // Освобождение графического контекста
    }

    // Метод для расчёта новой позиции точки на основе случайного значения
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        Point A = new Point(SIZE / 2, 0); // Вершина треугольника Серпинского
        Point B = new Point(0, SIZE); // Левая нижняя вершина треугольника Серпинского
        Point C = new Point(SIZE, SIZE); // Правая нижняя вершина треугольника Серпинского

        // Деление диапазона случайных чисел на три части для выбора вершины треугольника
        long rangePart = (MAX_RANDOM_VALUE - MIN_RANDOM_VALUE) / 3;

        int x = currentPoint.x; // Текущая координата X точки
        int y = currentPoint.y; // Текущая координата Y точки

        if (randomValue <= MIN_RANDOM_VALUE + rangePart) {
            // Выбор вершины A и перемещение точки к ней
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MIN_RANDOM_VALUE + 2 * rangePart) {
            // Выбор вершины B и перемещение точки к ней
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            // Выбор вершины C и перемещение точки к ней
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }

        return new Point(x, y); // Возвращение новой позиции точки
    }
}
