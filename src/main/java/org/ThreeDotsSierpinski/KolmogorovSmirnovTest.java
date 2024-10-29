package org.ThreeDotsSierpinski;

import java.util.Arrays;
import java.util.Random;

public class KolmogorovSmirnovTest {

    public static void main(String[] args) {
        // Генерация выборки случайных чисел
        int sampleSize = 1000;
        int[] sample = new int[sampleSize];
        Random random = new Random();
        for (int i = 0; i < sampleSize; i++) {
            sample[i] = random.nextInt(200000000) - 99999999; // Случайные числа в диапазоне [-99999999, 100000000]
        }

        // Уровень значимости
        double alpha = 0.05;

        // Проверка выборки на соответствие теоретическому распределению
        boolean result = test(sample, alpha);

        // Вывод результата в консоль
        if (result) {
            System.out.println("Выборка соответствует теоретическому распределению на уровне значимости " + alpha);
            System.out.println("Резюме: Да, все в порядке. Случайные числа соответствуют ожидаемому распределению.");
        } else {
            System.out.println("Выборка не соответствует теоретическому распределению на уровне значимости " + alpha);
            System.out.println("Резюме: Нет, случайные числа некачественные. Они не соответствуют ожидаемому распределению.");
        }
    }

    /**
     * Выполняет тест Колмогорова-Смирнова на соответствие равномерному распределению.
     *
     * @param sample выборка случайных чисел
     * @param alpha  уровень значимости
     * @return true, если выборка соответствует ожидаемому распределению, иначе false
     */
    public static boolean test(int[] sample, double alpha) {
        // Сортируем выборку по возрастанию
        Arrays.sort(sample);

        // Вычисляем эмпирическую функцию распределения и находим максимальное отклонение
        double maxDeviation = 0.0;
        int n = sample.length;
        for (int i = 0; i < n; i++) {
            double empiricalCDF = (double) (i + 1) / n;
            double theoreticalCDF = calculateTheoreticalCDF(sample[i]);

            double deviation = Math.abs(empiricalCDF - theoreticalCDF);
            maxDeviation = Math.max(maxDeviation, deviation);
        }

        // Вычисляем критическое значение
        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2)) / Math.sqrt(n);

        // Проверяем условие
        return maxDeviation <= criticalValue;
    }

    /**
     * Вычисляет теоретическую функцию распределения для равномерного распределения.
     *
     * @param value значение случайного числа
     * @return значение функции распределения в этой точке
     */
    private static double calculateTheoreticalCDF(double value) {
        double a = -99999999;
        double b = 100000000;
        if (value < a) {
            return 0.0;
        } else if (value > b) {
            return 1.0;
        } else {
            return (value - a) / (b - a);
        }
    }
}
