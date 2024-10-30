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
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RandomNumberProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    // String constants
    private static final String API_URL = "https://lfdr.de/qrng_api/qrng";
    private static final String MAX_REQUESTS_WARNING = "Достигнуто максимальное количество запросов к API: ";
    private static final String REQUEST_SENT = "Отправка запроса: ";
    private static final String RESPONSE_RECEIVED = "Получен ответ: ";
    private static final String QUEUE_ADD_INTERRUPT = "Поток был прерван при добавлении числа в очередь: ";
    private static final String API_REQUEST_COUNT = "Количество запросов к API: ";
    private static final String ERROR_MESSAGE = "Ошибка при получении случайных чисел: ";
    private static final String UNEXPECTED_RESPONSE = "Неожиданный ответ от сервера.";
    private static final String RETRY_WARNING = "Попытка %d не удалась. Не удалось получить данные из QRNG API.";
    private static final String NO_RANDOM_NUMBERS = "Нет доступных случайных чисел.";
    private static final String MAX_REQUESTS_EXCEEDED = "Достигнуто максимальное количество запросов к API и нет доступных случайных чисел.";
    private static final String WAIT_INTERRUPTED = "Ожидание случайного числа было прервано.";
    private static final String EXECUTOR_SERVICE_NOT_TERMINATED = "ExecutorService не завершился.";
    private static final String EXECUTOR_SERVICE_SHUTDOWN = "ExecutorService успешно завершен.";
    private static final String INVALID_HEX_LENGTH = "Некорректная длина HEX-строки.";
    private static final String INVALID_HEX_CHAR = "Обнаружен некорректный символ в HEX-строке.";

    // Constants for API and queue configuration
    private static final int MAX_API_REQUESTS = 25;
    private static final int QUEUE_SIZE = 2000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private final BlockingQueue<Integer> randomNumbersQueue;
    private final ObjectMapper objectMapper;
    private int apiRequestCount = 0;

    private final Lock lock = new ReentrantLock();
    private final ExecutorService executorService;
    private volatile boolean isLoading = false;

    public RandomNumberProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        objectMapper = new ObjectMapper();
        executorService = Executors.newFixedThreadPool(2);
        loadInitialDataAsync();
    }

    private void loadInitialDataAsync() {
        lock.lock();
        try {
            if (isLoading || apiRequestCount >= MAX_API_REQUESTS) {
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    LOGGER.warning(MAX_REQUESTS_WARNING + MAX_API_REQUESTS);
                }
                return;
            }
            isLoading = true;
            executorService.submit(this::loadInitialData);
        } finally {
            lock.unlock();
        }
    }

    private void loadInitialData() {
        int retryAttempts = 0;
        boolean success = false;

        while (retryAttempts < MAX_RETRY_ATTEMPTS && !success) {
            try {
                int n = 1024;
                String requestUrl = API_URL + "?length=" + n + "&format=HEX";
                LOGGER.info(REQUEST_SENT + requestUrl);

                URI uri = new URI(requestUrl);
                URL url = uri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String responseBody = getResponseBody(conn);
                LOGGER.info(RESPONSE_RECEIVED + responseBody);

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
                            LOGGER.log(Level.WARNING, QUEUE_ADD_INTERRUPT + num, e);
                        }
                    }
                    lock.lock();
                    try {
                        apiRequestCount++;
                    } finally {
                        lock.unlock();
                    }
                    LOGGER.info(API_REQUEST_COUNT + apiRequestCount);
                    success = true;
                } else if (rootNode.has("error")) {
                    String errorMsg = rootNode.get("error").asText();
                    LOGGER.severe(ERROR_MESSAGE + errorMsg);
                } else {
                    LOGGER.warning(UNEXPECTED_RESPONSE);
                }

                conn.disconnect();
            } catch (URISyntaxException | IOException e) {
                retryAttempts++;
                LOGGER.log(Level.WARNING, String.format(RETRY_WARNING, retryAttempts), e);
            }
        }

        lock.lock();
        try {
            isLoading = false;
        } finally {
            lock.unlock();
        }
        if (randomNumbersQueue.size() < 1000 && apiRequestCount < MAX_API_REQUESTS) {
            loadInitialDataAsync();
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
            throw new IllegalArgumentException(INVALID_HEX_LENGTH);
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(s.charAt(i), 16);
            int low = Character.digit(s.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException(INVALID_HEX_CHAR);
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

    public int getNextRandomInteger() {
        int randomNum1 = getNextRandomNumber();
        int randomNum2 = getNextRandomNumber();
        int randomNum3 = getNextRandomNumber();
        int randomNum4 = getNextRandomNumber();
        return (randomNum1 << 24) | (randomNum2 << 16) | (randomNum3 << 8) | randomNum4;
    }

    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
            if (nextNumber == null) {
                lock.lock();
                try {
                    if (apiRequestCount >= MAX_API_REQUESTS) {
                        throw new NoSuchElementException(MAX_REQUESTS_EXCEEDED);
                    }
                } finally {
                    lock.unlock();
                }
                loadInitialDataAsync();
                nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
                if (nextNumber == null) {
                    throw new NoSuchElementException(NO_RANDOM_NUMBERS);
                }
            }
            if (randomNumbersQueue.size() < 1000) {
                loadInitialDataAsync();
            }
            return nextNumber;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException(WAIT_INTERRUPTED);
        }
    }

    public long getNextRandomNumberInRange(long min, long max) {
        int randomNum = getNextRandomInteger();
        double normalized = (randomNum - (double) Integer.MIN_VALUE) / ((double) Integer.MAX_VALUE - (double) Integer.MIN_VALUE);
        long range = max - min;
        return min + (long) (normalized * range);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.severe(EXECUTOR_SERVICE_NOT_TERMINATED);
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info(EXECUTOR_SERVICE_SHUTDOWN);
    }

}
