package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class App {
    // Константы для строковых значений
    private static final String APPLICATION_TITLE = "Dot Mover";
    private static final String LOG_APP_STARTED = "Приложение запущено.";
    private static final String LOG_GUI_STARTED = "GUI успешно запущен.";
    private static final String LOG_APP_SHUTTING_DOWN = "Завершение работы приложения.";

    // Константы для параметров JFrame
    private static final int FRAME_CLOSE_OPERATION = JFrame.EXIT_ON_CLOSE;
    private static final int FRAME_STATE = JFrame.MAXIMIZED_BOTH;
    private static final String FRAME_LAYOUT = BorderLayout.CENTER;

    private static final Logger LOGGER = LoggerConfig.getLogger();

    public static void main(String[] args) {
        // Инициализация логгирования
        LoggerConfig.initializeLogger();
        LOGGER.info(LOG_APP_STARTED);

        // Создание объектов
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        DotController dotController = new DotController(randomNumberProvider);

        // Запуск GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(APPLICATION_TITLE); // Создание окна приложения
            frame.setLayout(new BorderLayout()); // Установка менеджера компоновки
            frame.add(dotController, FRAME_LAYOUT); // Добавление контроллера точек в центр
            frame.setDefaultCloseOperation(FRAME_CLOSE_OPERATION); // Установка операции закрытия по умолчанию
            frame.setExtendedState(FRAME_STATE); // Развертывание окна на весь экран

            // Запуск движения точек
            dotController.startDotMovement();
            frame.setVisible(true); // Отображение окна
            LOGGER.info(LOG_GUI_STARTED);

            // Добавление обработчика закрытия окна для корректного завершения ExecutorService
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    LOGGER.info(LOG_APP_SHUTTING_DOWN);
                    randomNumberProvider.shutdown(); // Корректное завершение пула потоков
                    super.windowClosing(windowEvent);
                }
            });
        });
    }
}
