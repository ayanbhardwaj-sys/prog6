package assignment;

public class Vector2D {
    private final double x, y;

    public Vector2D(double x, double y) {
        if (x == 0 && y == 0) {
            throw new IllegalArgumentException("Vector2D Can't Be the 0 Vector.");
        }

        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
