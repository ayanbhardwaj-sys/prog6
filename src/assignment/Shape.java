package assignment;

import java.awt.Point;
import java.awt.geom.Point2D;

public interface Shape {
    /**
     * Finds the minimum x and y coordinates of the given shape
     *
     * @return              a point representing the minimum x and y coordinates
     */
    public Point getMinSurroundingPoint ();

    /**
     * Finds the maximum x and y coordinates of the given shape
     *
     * @return              a point representing the maximum x and y coordinates
     */
    public Point getMaxSurroundingPoint ();

    /**
     * Finds the center x and y coordinates of the given shape
     *
     * @return              a point representing the center x and y coordinates
     */
    public Point2D.Double getCenter ();

    /**
     * Finds the point that a ray would intersect with a shape
     *
     * @param origin        the starting position of the ray
     * @param direction     a vector that starts at (0,0) to represent the direction of the ray
     * @return              a double representing the point where the provided ray intersects the shape.
     *                      returns null if the line never intersects with the triangle.
     */
    public Point2D.Double findIntersection (Point origin, Vector2D direction);

    /**
     * Finds if a given point is within a shape
     *
     * @param point         the point to test
     * @return              whether the point is within the shape or not
     */
    public boolean containsPoint (Point2D.Double point);

    /**
     * Calculates the point that two lines intersect using the line line intersection method
     *
     * @return the point where the lines intersect, null if the lines are parallel or coincidental
     */
    static Point2D.Double lineLineIntersection (double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

        // Does not intersect or is parallel
        if (denominator == 0) {
            return null;
        }

        double xPoint = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denominator;
        double yPoint = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denominator;

        return new Point2D.Double(xPoint, yPoint);
    }

    static final double EPSILON = 1E-9;

    static boolean isClose(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }

        return Math.abs(a - b) <= EPSILON;
    }

    static boolean isCloseOrGreater(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }

        if (a > b) {
            return true;
        }

        return Math.abs(a - b) <= EPSILON;
    }
}