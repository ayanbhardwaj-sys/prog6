package assignment;

import java.awt.*;
import java.util.LinkedList;

public class Visualization {
    BoundedVolumeHierarchy boundingVolumeHierarchy;

    /**
     * Initialize state of square to be at (0, 0)
     */
    public Visualization () {
        //toDo
    }

    /**
     * @param boundingVolumeHierarchy        the BVH to set the boundedVolumeHierarchy to
     */
    public void setBoundingVolumeHierarchy(BoundedVolumeHierarchy boundingVolumeHierarchy) {
        this.boundingVolumeHierarchy = boundingVolumeHierarchy;
    }

    /**
     * Moves the square within the environment if it's able to move in the given direction
     *
     * @param keyPressed        the character of the keyPressed
     * @return              if the environment should be redrawn or not
     */
    public boolean moveSquare (char keyPressed) {
        // toDo
        return false;
    }

    /**
     * Calculates all the pixels visible from the square in the center
     *
     * @return              All points that should be drawn other than the square
     */
    public LinkedList<Point> drawGUI() {
        // toDo
        return new LinkedList<>();
    }
}
