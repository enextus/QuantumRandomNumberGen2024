package org.ThreeDotsSierpinski;

import java.util.Arrays;
import java.util.Random;

public class KolmogorovSmirnovTest {

    // Constants
    private static final int SAMPLE_SIZE = 1000;
    private static final int MIN_RANDOM_VALUE = -99999999;
    private static final int MAX_RANDOM_VALUE = 100000000;
    private static final double ALPHA = 0.05;

    // Message Constants
    private static final String RESULT_SUCCESS = "Выборка соответствует теоретическому распределению на уровне значимости %.2f%n";
    private static final String RESULT_SUMMARY_SUCCESS = "Резюме: Да, все в порядке. Случайные числа соответствуют ожидаемому распределению.";
    private static final String RESULT_FAILURE = "Выборка не соответствует теоретическому распределению на уровне значимости %.2f%n";
    private static final String RESULT_SUMMARY_FAILURE = "Резюме: Нет, случайные числа некачественные. Они не соответствуют ожидаемому распределению.";

    public static void main() {
        // Генерация выборки случайных чисел
        int[] sample = new int[SAMPLE_SIZE];
        Random random = new Random();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            sample[i] = random.nextInt(MAX_RANDOM_VALUE - MIN_RANDOM_VALUE) + MIN_RANDOM_VALUE;
        }

        // Проверка выборки на соответствие теоретическому распределению
        boolean result = test(sample, ALPHA);

        // Вывод результата в консоль с использованием шаблона
        if (result) {
            System.out.printf(RESULT_SUCCESS, ALPHA);
            System.out.println(RESULT_SUMMARY_SUCCESS);
        } else {
            System.out.printf(RESULT_FAILURE, ALPHA);
            System.out.println(RESULT_SUMMARY_FAILURE);
        }
    }

    // Rest of the class remains unchanged...
    public static boolean test(int[] sample, double alpha) {
        Arrays.sort(sample);
        double maxDeviation = 0.0;
        int n = sample.length;
        for (int i = 0; i < n; i++) {
            double empiricalCDF = (double) (i + 1) / n;
            double theoreticalCDF = calculateTheoreticalCDF(sample[i]);
            double deviation = Math.abs(empiricalCDF - theoreticalCDF);
            maxDeviation = Math.max(maxDeviation, deviation);
        }
        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2)) / Math.sqrt(n);
        return maxDeviation <= criticalValue;
    }

    private static double calculateTheoreticalCDF(double value) {
        if (value < MIN_RANDOM_VALUE) {
            return 0.0;
        } else if (value > MAX_RANDOM_VALUE) {
            return 1.0;
        } else {
            return (value - MIN_RANDOM_VALUE) / (double)(MAX_RANDOM_VALUE - MIN_RANDOM_VALUE);
        }
    }
}
