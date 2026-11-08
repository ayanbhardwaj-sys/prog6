package assignment;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


//21 by 21 square stays in middle of 400 by 400 frame while
//triangles around it shift as it moves through virtual plane
//virtual coords map to gui coords
public class Visualization {
    //width and height of the GUI frame in pixels
    static final int FRAME_SIZE = 400;

    //GUI coordinate (on both axes) of the square's center
    static final int CENTER = 200;

    //distance from the square's center to each of its sides
    static final int HALF_SQUARE = 10;

    //distance the square moves in the virtual plane per key press
    static final int STEP = 5;

    //spacing in pixels between the border points that rays are aimed at
    private static final double RAY_SPACING = 0.5;

    //the hierarchy holding the triangles in the scene
    BoundedVolumeHierarchy boundingVolumeHierarchy;

    //position of the square's center in the virtual plane
    private int squareX, squareY;

    //puts the square at (0, 0) and loads a small default scene so the GUI has something to show
    public Visualization () {
        squareX = 0;
        squareY = 0;
        List<Shape> scene = new ArrayList<>();
        scene.add(new Triangle(new Point(0, 100), new Point(80, 100), new Point(40, 50)));
        scene.add(new Triangle(new Point(-150, -120), new Point(-60, -150), new Point(-100, -60)));
        scene.add(new Triangle(new Point(120, -80), new Point(170, -40), new Point(110, -20)));
        scene.add(new Triangle(new Point(-170, 60), new Point(-90, 40), new Point(-120, 130)));
        boundingVolumeHierarchy = new BoundedVolumeHierarchy();
        boundingVolumeHierarchy.buildBVH(scene);
    }

    //sets the BVH that holds the triangles in the scene
    public void setBoundingVolumeHierarchy(BoundedVolumeHierarchy boundingVolumeHierarchy) {
        this.boundingVolumeHierarchy = boundingVolumeHierarchy;
    }

    //moves the square 5 units for a WASD key unless that would make it hit a triangle
    //returns whether the environment should be redrawn
    public boolean moveSquare (char keyPressed) {
        int dx = 0;
        int dy = 0;
        switch (Character.toLowerCase(keyPressed)) {
            case 'w': dy = -STEP; break;
            case 's': dy = STEP; break;
            case 'a': dx = -STEP; break;
            case 'd': dx = STEP; break;
            default: return false;
        }
        if (squareCollides(squareX + dx, squareY + dy)) {
            return false;
        }
        squareX += dx;
        squareY += dy;
        return true;
    }

    //returns true if the square centered at (centerX, centerY) would touch or overlap a shape
    //the BVH narrows the search to shapes whose boxes overlap the square
    private boolean squareCollides(int centerX, int centerY) {
        if (boundingVolumeHierarchy == null) {
            return false;
        }
        int minX = centerX - HALF_SQUARE;
        int minY = centerY - HALF_SQUARE;
        int maxX = centerX + HALF_SQUARE;
        int maxY = centerY + HALF_SQUARE;
        for (Shape shape : boundingVolumeHierarchy.findShapesOverlappingBox(minX, minY, maxX, maxY)) {
            if (shapeIntersectsBox(shape, minX, minY, maxX, maxY)) {
                return true;
            }
        }
        return false;
    }

    //returns true if the shape touches or overlaps the box
    static boolean shapeIntersectsBox(Shape shape, double minX, double minY, double maxX, double maxY) {
        if (shape instanceof Triangle) {
            return triangleIntersectsBox((Triangle) shape, minX, minY, maxX, maxY);
        }
        double[][] samples = {{minX, minY}, {minX, maxY}, {maxX, minY}, {maxX, maxY},
                {(minX + maxX) / 2, (minY + maxY) / 2}};
        for (double[] sample : samples) {
            if (shape.containsPoint(new Point2D.Double(sample[0], sample[1]))) {
                return true;
            }
        }
        return false;
    }

    //separating axis test: a triangle and a box are disjoint exactly when their projections are
    //disjoint on the x-axis, the y-axis, or the normal of one of the triangle's edges.
    //returns true if the triangle touches or overlaps the box
    static boolean triangleIntersectsBox(Triangle triangle, double minX, double minY, double maxX, double maxY) {
        Point[] vertices = {triangle.a, triangle.b, triangle.c};
        double[][] corners = {{minX, minY}, {minX, maxY}, {maxX, minY}, {maxX, maxY}};

        //axes 1 and 2: the x and y axes, i.e. the triangle's bounding box against the box
        double triMinX = Math.min(vertices[0].x, Math.min(vertices[1].x, vertices[2].x));
        double triMaxX = Math.max(vertices[0].x, Math.max(vertices[1].x, vertices[2].x));
        double triMinY = Math.min(vertices[0].y, Math.min(vertices[1].y, vertices[2].y));
        double triMaxY = Math.max(vertices[0].y, Math.max(vertices[1].y, vertices[2].y));
        if (triMaxX < minX || triMinX > maxX || triMaxY < minY || triMinY > maxY) {
            return false;
        }

        //axes 3 to 5: the normal of each triangle edge
        for (int i = 0; i < 3; i++) {
            Point p = vertices[i];
            Point q = vertices[(i + 1) % 3];
            double normalX = -(q.y - p.y);
            double normalY = q.x - p.x;

            double triLow = Double.POSITIVE_INFINITY;
            double triHigh = Double.NEGATIVE_INFINITY;
            for (Point v : vertices) {
                double projection = v.x * normalX + v.y * normalY;
                triLow = Math.min(triLow, projection);
                triHigh = Math.max(triHigh, projection);
            }
            double boxLow = Double.POSITIVE_INFINITY;
            double boxHigh = Double.NEGATIVE_INFINITY;
            for (double[] corner : corners) {
                double projection = corner[0] * normalX + corner[1] * normalY;
                boxLow = Math.min(boxLow, projection);
                boxHigh = Math.max(boxHigh, projection);
            }
            if (triHigh < boxLow || boxHigh < triLow) {
                return false;
            }
        }
        return true;
    }

    //calculates all the pixels visible from the square in the center
    //returns all the points that should be drawn other than the square
    public LinkedList<Point> drawGUI() {
        LinkedList<Point> pixels = new LinkedList<>();
        if (boundingVolumeHierarchy == null) {
            return pixels;
        }
        Set<Point> seen = new HashSet<>();
        int last = FRAME_SIZE - 1;
        for (double t = 0; t <= last; t += RAY_SPACING) {
            castRay(t, 0, seen, pixels);
            castRay(t, last, seen, pixels);
            castRay(0, t, seen, pixels);
            castRay(last, t, seen, pixels);
        }
        return pixels;
    }

    //casts a ray from the square's center toward the GUI point (guiX, guiY) and adds the pixel
    //where it first hits a shape, if that pixel is inside the frame and not already added
    private void castRay(double guiX, double guiY, Set<Point> seen, LinkedList<Point> pixels) {
        Vector2D direction = new Vector2D(guiX - CENTER, guiY - CENTER);
        Shape hit = boundingVolumeHierarchy.intersectRay(new Point2D.Double(squareX, squareY), direction);
        if (hit == null) {
            return;
        }
        Point2D.Double hitPoint = hit.findIntersection(new Point(squareX, squareY), direction);
        if (hitPoint == null) {
            return;
        }
        int pixelX = (int) Math.round(hitPoint.getX() - squareX + CENTER);
        int pixelY = (int) Math.round(hitPoint.getY() - squareY + CENTER);
        if (pixelX < 0 || pixelY < 0 || pixelX >= FRAME_SIZE || pixelY >= FRAME_SIZE) {
            return;
        }
        Point pixel = new Point(pixelX, pixelY);
        if (seen.add(pixel)) {
            pixels.add(pixel);
        }
    }
}