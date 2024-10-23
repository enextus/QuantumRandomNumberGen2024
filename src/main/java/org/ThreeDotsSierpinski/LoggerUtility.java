package org.ThreeDotsSierpinski;

import java.io.IOException;
import java.util.logging.*;

public class LoggerUtility {
    private static final Logger LOGGER = Logger.getLogger(LoggerUtility.class.getName());
    private static boolean isInitialized = false;

    /**
     * Initializes the logger configuration.
     * Clears previous log entries by setting append to false.
     */
    public static void initializeLogger() {
        if (isInitialized) {
            return; // Prevent re-initialization
        }

        try {
            // Remove default handlers to prevent duplicate logs
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }

            // Create a new FileHandler with append set tоo false to overwrite the log file
            FileHandler fileHandler = new FileHandler("app.log", false); // 'false' to overwrite
            fileHandler.setFormatter(new SimpleFormatter()); // Simple formatting
            fileHandler.setLevel(Level.ALL); // Capture all log levels

            // Add the FileHandler to the root logger
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL); // Set the desired logging level

            isInitialized = true;
            LOGGER.info("Logger initialized. Previous logs cleared.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize FileHandler for logger.", e);
        }
    }

    /**
     * Retrieves the logger instance.
     *
     * @return The global logger instance.
     */
    public static Logger getLogger() {
        if (!isInitialized) {
            initializeLogger();
        }
        return Logger.getLogger("");
    }

}
