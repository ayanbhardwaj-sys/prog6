package assignment;

import java.awt.Point;
import java.awt.geom.Point2D;

public class Rectangle implements Shape{

    public Point minPos;
    public Point maxPos;

    public Rectangle (Point minPos, Point maxPos) {
        this.minPos = minPos;
        this.maxPos = maxPos;
    }

    /**
     * Calculates if a given ray intersects the rectangle
     *
     * @param origin        the starting position of the ray
     * @param direction     a vector that starts at (0,0) to represent the direction of the ray
     * @return              if the ray intersects the rectangle or not
     */
    public boolean doesRayIntersect (Point origin, Vector2D direction) {
        Triangle triangle = new Triangle(new Point((int) minPos.getX(), (int) minPos.getY()), new Point((int) minPos.getX(), (int) maxPos.getY()), new Point((int) maxPos.getX(), (int) minPos.getY()));
        if (triangle.findIntersection(origin, direction) != null) {
            return true;
        }

        triangle = new Triangle(new Point((int) maxPos.getX(), (int) maxPos.getY()), new Point((int) minPos.getX(), (int) maxPos.getY()), new Point((int) maxPos.getX(), (int) minPos.getY()));
        return triangle.findIntersection(origin, direction) != null;
    }

    @Override
    public Point getMinSurroundingPoint() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Point getMaxSurroundingPoint() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Point2D.Double getCenter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Point2D.Double findIntersection(Point origin, Vector2D direction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsPoint(Point2D.Double point) {
        if (point.getX() < minPos.getX() || point.getX() > maxPos.getX()) {
            return false;
        }

        if (point.getY() < minPos.getY() || point.getY() > maxPos.getY()) {
            return false;
        }

        return true;
    }
}
