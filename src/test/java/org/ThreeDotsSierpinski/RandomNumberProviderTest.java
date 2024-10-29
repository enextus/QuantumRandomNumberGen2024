package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomNumberProviderTest {

    private static final Logger LOGGER = Logger.getLogger(RandomNumberProviderTest.class.getName());

    @Test
    public void testRandomNumberQuality() {
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        int sampleSize = 1000;
        double alpha = 0.05; // Уровень значимости (5%)

        // Генерация выборки случайных чисел в диапазоне [0, 1)
        double[] sample = IntStream.range(0, sampleSize)
                .mapToDouble(i -> {
                    int randomValue = randomNumberProvider.getNextRandomInteger();
                    double normalizedValue = (randomValue - (double) Integer.MIN_VALUE) / (Integer.MAX_VALUE - (double) Integer.MIN_VALUE);
                    LOGGER.info("Generated normalized value: " + normalizedValue); // Логгирование значения
                    return normalizedValue;
                })
                .toArray();

        // Проверка качества случайных чисел с помощью теста Колмогорова-Смирнова
        boolean isUniform = performKolmogorovSmirnovTest(sample, alpha);

        System.out.println("Result of Kolmogorov-Smirnov Test: " + isUniform);
        assertTrue(isUniform, "Случайные числа не соответствуют ожидаемому равномерному распределению на уровне значимости " + alpha);
    }

    private boolean performKolmogorovSmirnovTest(double[] sample, double alpha) {
        Arrays.sort(sample);
        int n = sample.length;
        double maxDeviation = 0.0;

        for (int i = 0; i < n; i++) {
            double empiricalCDF = (double) (i + 1) / n;
            double theoreticalCDF = sample[i];
            double deviation = Math.abs(empiricalCDF - theoreticalCDF);
            maxDeviation = Math.max(maxDeviation, deviation);
        }

        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2)) / Math.sqrt(n);
        LOGGER.info("Max deviation: " + maxDeviation + ", Critical value: " + criticalValue);
        return maxDeviation <= criticalValue;
    }
}
