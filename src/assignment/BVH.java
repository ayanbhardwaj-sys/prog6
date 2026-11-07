package assignment;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Set;

public interface BVH {
    public static enum SplitMethod {
        SPLIT_MIDRANGE,
        SPLIT_MEDIAN,
        SPLIT_SURFACE_AREA // Split surface area is used for karma
    }

    public static final SplitMethod SPLIT_DEFAULT = SplitMethod.SPLIT_MEDIAN;

    /**
     * Changes the current split method that buildBVH() uses
     *
     * @param splitMethod   the new split method that buildBVH() uses
     */
    public void setSplitMethod (SplitMethod splitMethod);

    /**
     * Builds a BVH of the triangles contained in buildBVH
     *
     * @param shapeList   the list of triangles that the BVH should contain
     */
    public void buildBVH (List<Shape> shapeList);

    /**
     * Inserts the triangle provided and balances according to AVL properties
     *
     * @param shape   the new triangle to insert
     */
    public void insert (Shape shape);

    /**
     * Removes the triangle provided and balances according to AVL properties
     *
     * @param shape   the triangle to remove
     */
    public void remove (Shape shape);

    /**
     * Finds objects in the BVH where a point would be inside the object
     *
     * @param point   the point to detect collisions against
     * @return        a set of triangles that contains the passed in point.
     *                returns an empty set if no triangles contain the given point.
     */
    public Set<Shape> findCollision (Point2D.Double point);

    /**
     * Finds the first triangle that a ray would hit
     *
     * @param origin        the starting position of the ray
     * @param direction     a vector that represents the direction of the ray
     * @return              the triangle that the given origin and direction first intersects with.
     *                      if it intersects two shapes at the same point, it may return either shape.
     *                      returns null if it intersects with neither shape.
     */
    public Shape intersectRay (Point2D.Double origin, Vector2D direction);

    /**
     * A human-readable version of a BVH.
     *
     * The bounding box should be represented as [(min_X, min_Y), (max_X, max_Y)]
     * Triangles should be represented as (a_X, a_Y), (b_X, b_Y), (c_X, c_Y)
     *
     * @return string representation of a BVH
     */
    public String toString ();


    /**
     * Calculates the distance between two distinct points
     *
     * @param p1 The first point
     * @param p2 The second point
     * @return the distance between the two points
     */
    static double distanceBetweenPoints(Point p1, Point p2) {
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    static double distanceBetweenPoints(Point2D p1, Point2D p2) {
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
