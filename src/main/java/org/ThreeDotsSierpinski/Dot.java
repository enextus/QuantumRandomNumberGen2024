package org.ThreeDotsSierpinski;

import java.awt.*;

/**
 * Запись Dot представляет точку с координатами на плоскости.
 *
 * @param point Координаты точки
 */
public record Dot(Point point) {
    /**
     * Конструктор, принимающий начальные координаты точки.
     * Создает копию переданной точки для обеспечения неизменяемости.
     *
     * @param point Начальная точка
     */
    public Dot(Point point) {
        this.point = new Point(point); // Создает копию для обеспечения неизменяемости
    }

    /**
     * Возвращает копию точки для предотвращения изменения оригинала.
     *
     * @return Копия точки
     */
    @Override
    public Point point() {
        return new Point(point); // Возвращает копию точки для безопасности
    }

    /**
     * Переопределяет метод toString для предоставления представления через запятую.
     *
     * @return Строка в формате "x, y"
     */
    @Override
    public String toString() {
        return point.x + "," + point.y;
    }

}
