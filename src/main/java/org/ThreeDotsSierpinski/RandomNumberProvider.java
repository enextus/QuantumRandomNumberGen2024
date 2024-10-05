package org.ThreeDotsSierpinski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class RandomNumberProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    private static final String API_URL = "https://lfdr.de/qrng_api/qrng"; // URL API для получения случайных чисел
    private static final int MAX_API_REQUESTS = 25; // Максимальное количество запросов к API
    private final BlockingQueue<Integer> randomNumbersQueue; // Очередь для хранения случайных чисел
    private final ObjectMapper objectMapper; // Объект для обработки JSON
    private int apiRequestCount = 0; // Счетчик количества выполненных API-запросов
    private final Object lock = new Object(); // Блокировка для синхронизации вызовов loadInitialData

    private final ExecutorService executorService; // Пул потоков для асинхронных задач
    private volatile boolean isLoading = false; // Флаг загрузки данных



    public RandomNumberProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>();
        objectMapper = new ObjectMapper();
        executorService = Executors.newSingleThreadExecutor();
        loadInitialDataAsync();
    }

    private void loadInitialDataAsync() {
        synchronized (lock) {
            if (isLoading || apiRequestCount >= MAX_API_REQUESTS) {
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    LOGGER.warning("Достигнуто максимальное количество запросов к API: " + MAX_API_REQUESTS);
                }
                return;
            }
            isLoading = true;
            executorService.submit(this::loadInitialData);
        }
    }

    private void loadInitialData() {
        try {
            int n = 1024; // Количество случайных байтов для загрузки
            String requestUrl = API_URL + "?length=" + n + "&format=HEX";

            LOGGER.info("Отправка запроса: " + requestUrl);

            URI uri = new URI(requestUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String responseBody = getResponseBody(conn);
            LOGGER.info("Получен ответ: " + responseBody);

            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("qrn")) {
                String hexData = rootNode.get("qrn").asText();
                byte[] byteArray = hexStringToByteArray(hexData);

                for (byte b : byteArray) {
                    int num = b & 0xFF;
                    try {
                        randomNumbersQueue.put(num);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.log(Level.WARNING, "Поток был прерван при добавлении числа в очередь: " + num, e);
                    }
                }
                synchronized (lock) {
                    apiRequestCount++;
                }
                LOGGER.info("Количество запросов к API: " + apiRequestCount);
            } else if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").asText();
                LOGGER.severe("Ошибка при получении случайных чисел: " + errorMsg);
            } else {
                LOGGER.warning("Неожиданный ответ от сервера.");
            }

            conn.disconnect();
        } catch (URISyntaxException | IOException e) {
            LOGGER.log(Level.SEVERE, "Не удалось получить данные из QRNG API.", e);
        } finally {
            synchronized (lock) {
                isLoading = false;
            }
            if (randomNumbersQueue.size() < 1000 && apiRequestCount < MAX_API_REQUESTS) {
                loadInitialDataAsync();
            }
        }
    }

    private static @NotNull String getResponseBody(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String responseLine;

        while ((responseLine = in.readLine()) != null) {
            response.append(responseLine.trim());
        }
        in.close();

        return response.toString();
    }


    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Некорректная длина HEX-строки.");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(s.charAt(i), 16);
            int low = Character.digit(s.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Обнаружен некорректный символ в HEX-строке.");
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

    /**
     * Получает следующее случайное число в диапазоне от Integer.MIN_VALUE до Integer.MAX_VALUE.
     *
     * @return Случайное целое число в диапазоне от Integer.MIN_VALUE до Integer.MAX_VALUE.
     * @throws NoSuchElementException Если нет доступных случайных чисел.
     */
    public int getNextRandomInteger() {
        int randomNum1 = getNextRandomNumber(); // Получение случайного числа от 0 до 255
        int randomNum2 = getNextRandomNumber(); // Второе случайное число для большей энтропии
        int randomNum3 = getNextRandomNumber(); // Третье случайное число для большей энтропии
        int randomNum4 = getNextRandomNumber(); // Четвертое случайное число для большей энтропии

        // Комбинируем 4 случайных байта в одно 32-битное число
        return (randomNum1 << 24) | (randomNum2 << 16) | (randomNum3 << 8) | randomNum4;
    }


    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
            if (nextNumber == null) {
                synchronized (lock) {
                    if (apiRequestCount >= MAX_API_REQUESTS) {
                        throw new NoSuchElementException("Достигнуто максимальное количество запросов к API и нет доступных случайных чисел.");
                    }
                }
                loadInitialDataAsync();
                nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
                if (nextNumber == null) {
                    throw new NoSuchElementException("Нет доступных случайных чисел.");
                }
            }
            if (randomNumbersQueue.size() < 1000) {
                loadInitialDataAsync();
            }
            return nextNumber;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Ожидание случайного числа было прервано.");
        }
    }

    /**
     * Получает следующее случайное число в диапазоне от min до max.
     *
     * @param min Минимальное значение диапазона
     * @param max Максимальное значение диапазона
     * @return Случайное число в заданном диапазоне
     * @throws NoSuchElementException Если нет доступных случайных чисел
     */
    public long getNextRandomNumberInRange(long min, long max) {
        int randomNum = getNextRandomInteger(); // Получение случайного числа от Integer.MIN_VALUE до Integer.MAX_VALUE

        // Нормализуем случайное число в диапазон от 0 до 1
        double normalized = (randomNum - (double) Integer.MIN_VALUE) / ((double) Integer.MAX_VALUE - (double) Integer.MIN_VALUE);

        // Преобразуем нормализованное число в заданный диапазон [min, max]
        long range = max - min;
        return min + (long) (normalized * range);
    }



    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.severe("ExecutorService не завершился.");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("ExecutorService успешно завершен.");
    }
}
