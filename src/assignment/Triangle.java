package assignment;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Objects;

public class Triangle implements Shape {
    public Point a, b, c;

    public Triangle(Point a, Point b, Point c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public Point getMinSurroundingPoint() {
        Point minPos = new Point();
        minPos.x = (int) Math.min(a.getX(), Math.min(b.getX(), c.getX()));
        minPos.y = (int) Math.min(a.getY(), Math.min(b.getY(), c.getY()));
        return minPos;
    }

    @Override
    public Point getMaxSurroundingPoint() {
        Point maxPos = new Point();
        maxPos.x = (int) Math.max(a.getX(), Math.max(b.getX(), c.getX()));
        maxPos.y = (int) Math.max(a.getY(), Math.max(b.getY(), c.getY()));
        return maxPos;
    }

    @Override
    public Point2D.Double getCenter() {
        Point2D.Double centerPoint = new Point2D.Double();
        centerPoint.x = (a.getX() + b.getX() + c.getX()) / 3;
        centerPoint.y = (a.getY() + b.getY() + c.getY()) / 3;
        return centerPoint;
    }

    @Override
    public Point2D.Double findIntersection (Point origin, Vector2D direction) {
        Point2D.Double closestPoint = null;
        Point2D.Double additionalPoint = new Point2D.Double(origin.getX() + direction.getX(), origin.getY() + direction.getY());

        Point2D.Double sampleIntersection = Shape.lineLineIntersection(origin.getX(), origin.getY(), additionalPoint.getX(), additionalPoint.getY(), a.getX(), a.getY(), b.getX(), b.getY());

        if (sampleIntersection == null) {
            double testA = (additionalPoint.getY() - origin.getY());
            double testB = (additionalPoint.getX() - origin.getX());
            double testC = (additionalPoint.getX() * origin.getY() - origin.getX() * additionalPoint.getY());

            double triangleA = (a.getY() - b.getY());
            double triangleB = (a.getX() - b.getX());
            double triangleC = (a.getX() * b.getY() - b.getX() * a.getY());

            if (triangleC != 0) {
                triangleA = triangleA * testC / triangleC;
                triangleB = triangleB * testC / triangleC;
            } else if (testC != 0) {
                testA = testA * triangleC / testC;
                testB = testB * triangleC / testC;
            }

            if (Shape.isClose(testA, triangleA) && Shape.isClose(testB, triangleB)) {
                if (BVH.distanceBetweenPoints(origin, a) < BVH.distanceBetweenPoints(origin, b)) {
                    sampleIntersection = new Point2D.Double(a.getX(), a.getY());
                } else {
                    sampleIntersection = new Point2D.Double(b.getX(), b.getY());
                }
            }
        }

        if (sampleIntersection != null) {
            if (Shape.isCloseOrGreater(sampleIntersection.getX(), Math.min(a.getX(), b.getX())) && Shape.isCloseOrGreater(Math.max(a.getX(), b.getX()), sampleIntersection.getX()) && Shape.isCloseOrGreater(sampleIntersection.getY(), Math.min(a.getY(), b.getY())) && Shape.isCloseOrGreater(Math.max(a.getY(), b.getY()), sampleIntersection.getY())) {
                if (((Shape.isCloseOrGreater(sampleIntersection.getX() , origin.getX()) && Shape.isCloseOrGreater(direction.getX(), 0)) || (Shape.isCloseOrGreater(origin.getX(), sampleIntersection.getX()) && Shape.isCloseOrGreater(0, direction.getX()))) && ((Shape.isCloseOrGreater(sampleIntersection.getY(), origin.getY()) && Shape.isCloseOrGreater(direction.getY(), 0)) || (Shape.isCloseOrGreater(origin.getY(), sampleIntersection.getY()) && Shape.isCloseOrGreater(0, direction.getY())))) {
                    if (closestPoint == null || BVH.distanceBetweenPoints(origin, sampleIntersection) < BVH.distanceBetweenPoints(origin, closestPoint)) {
                        closestPoint = sampleIntersection;
                    }
                }
            }
        }

        sampleIntersection = Shape.lineLineIntersection(origin.getX(), origin.getY(), additionalPoint.getX(), additionalPoint.getY(), a.getX(), a.getY(), c.getX(), c.getY());

        if (sampleIntersection == null) {
            double testA = (additionalPoint.getY() - origin.getY());
            double testB = (additionalPoint.getX() - origin.getX());
            double testC = (additionalPoint.getX() * origin.getY() - origin.getX() * additionalPoint.getY());

            double triangleA = (a.getY() - c.getY());
            double triangleB = (a.getX() - c.getX());
            double triangleC = (a.getX() * c.getY() - c.getX() * a.getY());

            if (triangleC != 0) {
                triangleA = triangleA * testC / triangleC;
                triangleB = triangleB * testC / triangleC;
            } else if (testC != 0) {
                testA = testA * triangleC / testC;
                testB = testB * triangleC / testC;
            }

            if (Shape.isClose(testA, triangleA) && Shape.isClose(testB, triangleB)) {
                if (BVH.distanceBetweenPoints(origin, a) < BVH.distanceBetweenPoints(origin, c)) {
                    sampleIntersection = new Point2D.Double(a.getX(), a.getY());
                } else {
                    sampleIntersection = new Point2D.Double(c.getX(), c.getY());
                }
            }
        }

        if (sampleIntersection != null) {
            if (Shape.isCloseOrGreater(sampleIntersection.getX(), Math.min(a.getX(), c.getX())) && Shape.isCloseOrGreater(Math.max(a.getX(), c.getX()), sampleIntersection.getX()) && Shape.isCloseOrGreater(sampleIntersection.getY(), Math.min(a.getY(), c.getY())) && Shape.isCloseOrGreater(Math.max(a.getY(), c.getY()), sampleIntersection.getY())) {
                if (((Shape.isCloseOrGreater(sampleIntersection.getX() , origin.getX()) && Shape.isCloseOrGreater(direction.getX(), 0)) || (Shape.isCloseOrGreater(origin.getX(), sampleIntersection.getX()) && Shape.isCloseOrGreater(0, direction.getX()))) && ((Shape.isCloseOrGreater(sampleIntersection.getY(), origin.getY()) && Shape.isCloseOrGreater(direction.getY(), 0)) || (Shape.isCloseOrGreater(origin.getY(), sampleIntersection.getY()) && Shape.isCloseOrGreater(0, direction.getY())))) {
                    if (closestPoint == null || BVH.distanceBetweenPoints(origin, sampleIntersection) < BVH.distanceBetweenPoints(origin, closestPoint)) {
                        closestPoint = sampleIntersection;
                    }
                }
            }
        }

        sampleIntersection = Shape.lineLineIntersection(origin.getX(), origin.getY(), additionalPoint.getX(), additionalPoint.getY(), b.getX(), b.getY(), c.getX(), c.getY());

        if (sampleIntersection == null) {
            double testA = (additionalPoint.getY() - origin.getY());
            double testB = (additionalPoint.getX() - origin.getX());
            double testC = (additionalPoint.getX() * origin.getY() - origin.getX() * additionalPoint.getY());

            double triangleA = (b.getY() - c.getY());
            double triangleB = (b.getX() - c.getX());
            double triangleC = (b.getX() * c.getY() - c.getX() * b.getY());

            if (triangleC != 0) {
                triangleA = triangleA * testC / triangleC;
                triangleB = triangleB * testC / triangleC;
            } else if (testC != 0) {
                testA = testA * triangleC / testC;
                testB = testB * triangleC / testC;
            }

            if (Shape.isClose(testA, triangleA) && Shape.isClose(testB, triangleB)) {
                if (BVH.distanceBetweenPoints(origin, b) < BVH.distanceBetweenPoints(origin, c)) {
                    sampleIntersection = new Point2D.Double(b.getX(), b.getY());
                } else {
                    sampleIntersection = new Point2D.Double(c.getX(), c.getY());
                }
            }
        }

        if (sampleIntersection != null) {
            if (Shape.isCloseOrGreater(sampleIntersection.getX(), Math.min(b.getX(), c.getX())) && Shape.isCloseOrGreater(Math.max(b.getX(), c.getX()), sampleIntersection.getX()) && Shape.isCloseOrGreater(sampleIntersection.getY(), Math.min(b.getY(), c.getY())) && Shape.isCloseOrGreater(Math.max(b.getY(), c.getY()), sampleIntersection.getY())) {
                if (((Shape.isCloseOrGreater(sampleIntersection.getX() , origin.getX()) && Shape.isCloseOrGreater(direction.getX(), 0)) || (Shape.isCloseOrGreater(origin.getX(), sampleIntersection.getX()) && Shape.isCloseOrGreater(0, direction.getX()))) && ((Shape.isCloseOrGreater(sampleIntersection.getY(), origin.getY()) && Shape.isCloseOrGreater(direction.getY(), 0)) || (Shape.isCloseOrGreater(origin.getY(), sampleIntersection.getY()) && Shape.isCloseOrGreater(0, direction.getY())))) {
                    if (closestPoint == null || BVH.distanceBetweenPoints(origin, sampleIntersection) < BVH.distanceBetweenPoints(origin, closestPoint)) {
                        closestPoint = sampleIntersection;
                    }
                }
            }
        }

        return closestPoint;
    }

    @Override
    public boolean containsPoint(Point2D.Double point) {
        double triangleArea = Math.abs((a.getX() * (b.getY() - c.getY()) + b.getX() * (c.getY() - a.getY()) + c.getX() * (a.getY() - b.getY())) / 2.0);

        double triangle1Area = Math.abs((point.getX() * (b.getY() - c.getY()) + b.getX() * (c.getY() - point.getY()) + c.getX() * (point.getY() - b.getY())) / 2.0);
        double triangle2Area = Math.abs((a.getX() * (point.getY() - c.getY()) + point.getX() * (c.getY() - a.getY()) + c.getX() * (a.getY() - point.getY())) / 2.0);
        double triangle3Area = Math.abs((a.getX() * (b.getY() - point.getY()) + b.getX() * (point.getY() - a.getY()) + point.getX() * (a.getY() - b.getY())) / 2.0);

        return (Shape.isClose(triangleArea, (triangle1Area + triangle2Area + triangle3Area)));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Triangle)) {
            return false;
        }
        Triangle tri = (Triangle) obj;
        return (a.equals(tri.a) && b.equals(tri.b) && c.equals(tri.c))
                || (a.equals(tri.a) && b.equals(tri.c) && c.equals(tri.b))
                || (a.equals(tri.b) && b.equals(tri.a) && c.equals(tri.c))
                || (a.equals(tri.b) && b.equals(tri.c) && c.equals(tri.a))
                || (a.equals(tri.c) && b.equals(tri.a) && c.equals(tri.b))
                || (a.equals(tri.c) && b.equals(tri.b) && c.equals(tri.a));
    }

    @Override
    public int hashCode() {
        int a_hash = a == null ? 0 : a.hashCode();
        int b_hash = b == null ? 0 : b.hashCode();
        int c_hash = c == null ? 0 : c.hashCode();
        return a_hash + b_hash + c_hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(a.getX()).append(", ").append(a.getY()).append(") ");
        sb.append("(").append(b.getX()).append(", ").append(b.getY()).append(") ");
        sb.append("(").append(c.getX()).append(", ").append(c.getY()).append(")\n");
        return sb.toString();
    }
}
