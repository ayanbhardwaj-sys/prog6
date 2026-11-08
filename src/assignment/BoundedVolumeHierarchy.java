package assignment;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

//2D bounding volume hierarchy of triangles, each leaf stores one shape and node
//stores smallest axis-aligned box around subtree, every internal node has
//two children, and tree keeps avl property

public class BoundedVolumeHierarchy implements BVH {

    //testing points and ray against bounding boxes uses tolerance
    private static final double BOX_EPSILON = 1E-7;

    //root of hierarchy or null if empty
    private Node root;

    //split method used by buildBVH
    private SplitMethod splitMethod = SPLIT_DEFAULT;

    //breaks ties when choosing where to insert
    private final Random random = new Random();

    //once requested shape removed, set by recursive remove
    private boolean removed;

    //closest shape hit so far during intersectRay(), and its distance from the ray's origin
    private Shape closestShape;
    private double closestDistance;

    //a node of the hierarchy: 
    // a leaf holds a shape, 
    //  internal node holds two children
    private static class Node {
        Shape shape;

        Node left, right;

        int minX, minY, maxX, maxY;

        int height;

        Node(Shape shape) {
            this.shape = shape;
            update();
        }

        //creates an internal node with the given children
        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            update();
        }

        //returns true if this node is a leaf
        boolean isLeaf() {
            return left == null;
        }

        //recomputes the box and height from the shape or the children
        void update() {
            if (isLeaf()) {
                Point min = shape.getMinSurroundingPoint();
                Point max = shape.getMaxSurroundingPoint();
                minX = min.x;
                minY = min.y;
                maxX = max.x;
                maxY = max.y;
                height = 0;
            } else {
                minX = Math.min(left.minX, right.minX);
                minY = Math.min(left.minY, right.minY);
                maxX = Math.max(left.maxX, right.maxX);
                maxY = Math.max(left.maxY, right.maxY);
                height = 1 + Math.max(left.height, right.height);
            }
        }

        //returns the area of the bounding box
        long area() {
            return (long) (maxX - minX) * (maxY - minY);
        }

        //returns left subtree height minus right subtree height
        int balance() {
            return isLeaf() ? 0 : left.height - right.height;
        }

        //returns true if the point is inside or on the bounding box
        boolean containsPoint(double x, double y) {
            return x >= minX - BOX_EPSILON && x <= maxX + BOX_EPSILON
                    && y >= minY - BOX_EPSILON && y <= maxY + BOX_EPSILON;
        }

        //returns true if the box from min to max lies inside this node's box
        boolean containsBox(Point min, Point max) {
            return min.x >= minX && min.y >= minY && max.x <= maxX && max.y <= maxY;
        }

        //returns true if the given box overlaps this node's box (touching counts)
        boolean overlaps(double otherMinX, double otherMinY, double otherMaxX, double otherMaxY) {
            return otherMinX <= maxX && otherMaxX >= minX && otherMinY <= maxY && otherMaxY >= minY;
        }

        //returns true if the ray touches the bounding box
        boolean intersectsRay(Point origin, Vector2D direction) {
            double tMin = Double.NEGATIVE_INFINITY;
            double tMax = Double.POSITIVE_INFINITY;
            double[] start = {origin.getX(), origin.getY()};
            double[] dir = {direction.getX(), direction.getY()};
            double[] low = {minX - BOX_EPSILON, minY - BOX_EPSILON};
            double[] high = {maxX + BOX_EPSILON, maxY + BOX_EPSILON};

            for (int axis = 0; axis < 2; axis++) {
                if (dir[axis] == 0) {
                    if (start[axis] < low[axis] || start[axis] > high[axis]) {
                        return false;
                    }
                } else {
                    double t1 = (low[axis] - start[axis]) / dir[axis];
                    double t2 = (high[axis] - start[axis]) / dir[axis];
                    tMin = Math.max(tMin, Math.min(t1, t2));
                    tMax = Math.min(tMax, Math.max(t1, t2));
                }
            }
            return tMax >= Math.max(tMin, 0);
        }
    }

    @Override
    public void setSplitMethod(SplitMethod splitMethod) {
        this.splitMethod = (splitMethod == null) ? SPLIT_DEFAULT : splitMethod;
    }

    @Override
    public void buildBVH(List<Shape> shapeList) {
        root = null;
        if (shapeList == null) {
            return;
        }
        List<Shape> shapes = new ArrayList<>();
        for (Shape shape : shapeList) {
            if (shape != null) {
                shapes.add(shape);
            }
        }
        if (!shapes.isEmpty()) {
            root = balanceTree(build(shapes, 0));
        }
    }

    //builds a subtree for the shapes, splitting on x at even depths and y at odd depths
    private Node build(List<Shape> shapes, int depth) {
        if (shapes.size() == 1) {
            return new Node(shapes.get(0));
        }
        boolean xAxis = depth % 2 == 0;
        List<Shape> left = new ArrayList<>();
        List<Shape> right = new ArrayList<>();

        if (splitMethod == SplitMethod.SPLIT_MIDRANGE) {
            splitMidrange(shapes, xAxis, left, right);
        } else {
            splitMedian(shapes, xAxis, left, right);
        }
        return new Node(build(left, depth + 1), build(right, depth + 1));
    }

    //sorts the shapes by center and cuts them in half, which balances ties at the median
    private void splitMedian(List<Shape> shapes, boolean xAxis, List<Shape> left, List<Shape> right) {
        List<Shape> sorted = new ArrayList<>(shapes);
        sorted.sort(Comparator.comparingDouble((Shape s) -> center(s, xAxis))
                .thenComparingDouble(s -> center(s, !xAxis)));
        int mid = sorted.size() / 2;
        left.addAll(sorted.subList(0, mid));
        right.addAll(sorted.subList(mid, sorted.size()));
    }

    //splits at the midrange of the shapes' extents
    private void splitMidrange(List<Shape> shapes, boolean xAxis, List<Shape> left, List<Shape> right) {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (Shape shape : shapes) {
            Point min = shape.getMinSurroundingPoint();
            Point max = shape.getMaxSurroundingPoint();
            low = Math.min(low, xAxis ? min.x : min.y);
            high = Math.max(high, xAxis ? max.x : max.y);
        }
        double line = (low + high) / 2.0;

        List<Shape> onLine = new ArrayList<>();
        for (Shape shape : shapes) {
            double c = center(shape, xAxis);
            if (Shape.isClose(c, line)) {
                onLine.add(shape);
            } else if (c < line) {
                left.add(shape);
            } else {
                right.add(shape);
            }
        }
        for (Shape shape : onLine) {
            (left.size() <= right.size() ? left : right).add(shape);
        }

        if (left.isEmpty()) {
            moveNearest(right, left, line, xAxis);
        } else if (right.isEmpty()) {
            moveNearest(left, right, line, xAxis);
        }
    }

    //moves the shape whose center is nearest the split line from one list to the other
    private void moveNearest(List<Shape> from, List<Shape> to, double line, boolean xAxis) {
        int nearest = 0;
        for (int i = 1; i < from.size(); i++) {
            if (Math.abs(center(from.get(i), xAxis) - line) < Math.abs(center(from.get(nearest), xAxis) - line)) {
                nearest = i;
            }
        }
        to.add(from.remove(nearest));
    }

    //returns the x or y coordinate of the shape's center
    private static double center(Shape shape, boolean xAxis) {
        Point2D.Double c = shape.getCenter();
        return xAxis ? c.getX() : c.getY();
    }

    //restores the AVL property in a freshly built tree. A median build is already balanced; a
    //lopsided midrange build has the two subtrees of each node joined with AVL rotations
    private Node balanceTree(Node node) {
        if (node.isLeaf()) {
            return node;
        }
        return join(balanceTree(node.left), balanceTree(node.right), node);
    }

    //joins two balanced subtrees under the joint node, descending the taller tree's inner spine
    //until the heights are within one and rebalancing on the way back up (AVL join)
    private Node join(Node left, Node right, Node joint) {
        if (Math.abs(left.height - right.height) <= 1) {
            joint.left = left;
            joint.right = right;
            joint.update();
            return joint;
        }
        if (left.height > right.height) {
            left.right = join(left.right, right, joint);
            return rebalance(left);
        }
        right.left = join(left, right.left, joint);
        return rebalance(right);
    }

    @Override
    public void insert(Shape shape) {
        if (shape == null) {
            return;
        }
        Node leaf = new Node(shape);
        root = (root == null) ? leaf : insert(root, leaf);
    }

    //inserts the leaf below node and rebalances on the way back up; returns the new subtree root
    private Node insert(Node node, Node leaf) {
        if (node.isLeaf()) {
            return new Node(node, leaf);
        }
        if (shouldInsertLeft(node, leaf)) {
            node.left = insert(node.left, leaf);
        } else {
            node.right = insert(node.right, leaf);
        }
        return rebalance(node);
    }

    //chooses the child to insert into, in priority order: a leaf child, the child whose box grows
    //less in area, the shorter child, then a random child returns true for left child
    private boolean shouldInsertLeft(Node node, Node leaf) {
        if (node.left.isLeaf() != node.right.isLeaf()) {
            return node.left.isLeaf();
        }
        long leftGrowth = growth(node.left, leaf);
        long rightGrowth = growth(node.right, leaf);
        if (leftGrowth != rightGrowth) {
            return leftGrowth < rightGrowth;
        }
        if (node.left.height != node.right.height) {
            return node.left.height < node.right.height;
        }
        return random.nextBoolean();
    }

    //returns how much node's box area would grow if it also contained leaf
    private static long growth(Node node, Node leaf) {
        long width = Math.max(node.maxX, leaf.maxX) - Math.min(node.minX, leaf.minX);
        long height = Math.max(node.maxY, leaf.maxY) - Math.min(node.minY, leaf.minY);
        return width * height - node.area();
    }

    @Override
    public void remove(Shape shape) {
        if (root == null || shape == null) {
            return;
        }
        removed = false;
        root = remove(root, shape, shape.getMinSurroundingPoint(), shape.getMaxSurroundingPoint());
    }

    //removes one occurrence of the shape below node. A removed leaf's parent is replaced by the
    //leaf's sibling. Returns the new subtree root, or null if node itself was the removed leaf
    private Node remove(Node node, Shape shape, Point min, Point max) {
        if (!node.containsBox(min, max)) {
            return node;
        }
        if (node.isLeaf()) {
            if (node.shape.equals(shape)) {
                removed = true;
                return null;
            }
            return node;
        }

        Node newLeft = remove(node.left, shape, min, max);
        if (removed) {
            if (newLeft == null) {
                return node.right;
            }
            node.left = newLeft;
            return rebalance(node);
        }

        Node newRight = remove(node.right, shape, min, max);
        if (removed) {
            if (newRight == null) {
                return node.left;
            }
            node.right = newRight;
            return rebalance(node);
        }
        return node;
    }

    //updates node and applies the single or double rotation that restores its AVL balance
    private Node rebalance(Node node) {
        node.update();
        int balance = node.balance();
        if (balance > 1) {
            if (node.left.balance() < 0) {
                node.left = rotateLeft(node.left);
            }
            return rotateRight(node);
        }
        if (balance < -1) {
            if (node.right.balance() > 0) {
                node.right = rotateRight(node.right);
            }
            return rotateLeft(node);
        }
        return node;
    }

    //rotates right: the left child becomes the subtree's root
    private Node rotateRight(Node node) {
        Node newRoot = node.left;
        node.left = newRoot.right;
        newRoot.right = node;
        node.update();
        newRoot.update();
        return newRoot;
    }

    //rotates left: the right child becomes the subtree's root
    private Node rotateLeft(Node node) {
        Node newRoot = node.right;
        node.right = newRoot.left;
        newRoot.left = node;
        node.update();
        newRoot.update();
        return newRoot;
    }

    @Override
    public Set<Shape> findCollision(Point2D.Double point) {
        Set<Shape> collisions = new HashSet<>();
        if (root != null && point != null) {
            findCollision(root, point, collisions);
        }
        return collisions;
    }

    //adds the shapes below node that contain the point, skipping boxes that miss the point
    private void findCollision(Node node, Point2D.Double point, Set<Shape> collisions) {
        if (!node.containsPoint(point.getX(), point.getY())) {
            return;
        }
        if (node.isLeaf()) {
            if (node.shape.containsPoint(point)) {
                collisions.add(node.shape);
            }
            return;
        }
        findCollision(node.left, point, collisions);
        findCollision(node.right, point, collisions);
    }

    //Shape.findIntersection() takes integer points, so the origin is rounded to the nearest one
    @Override
    public Shape intersectRay(Point2D.Double origin, Vector2D direction) {
        if (root == null || origin == null || direction == null) {
            return null;
        }
        closestShape = null;
        closestDistance = Double.POSITIVE_INFINITY;
        intersectRay(root, new Point((int) Math.round(origin.getX()), (int) Math.round(origin.getY())), direction);
        return closestShape;
    }

    //records the closest hit below node. Leaves test their geometry; internal nodes are skipped
    //when the ray misses their box, otherwise both children are searched
    private void intersectRay(Node node, Point origin, Vector2D direction) {
        if (node.isLeaf()) {
            Point2D.Double point = node.shape.findIntersection(origin, direction);
            if (point != null && BVH.distanceBetweenPoints(origin, point) < closestDistance) {
                closestShape = node.shape;
                closestDistance = BVH.distanceBetweenPoints(origin, point);
            }
        } else if (node.intersectsRay(origin, direction)) {
            intersectRay(node.left, origin, direction);
            intersectRay(node.right, origin, direction);
        }
    }

    //returns every shape whose bounding box overlaps the given box (used by Visualization)
    Set<Shape> findShapesOverlappingBox(double minX, double minY, double maxX, double maxY) {
        Set<Shape> shapes = new HashSet<>();
        if (root != null) {
            findShapesOverlappingBox(root, minX, minY, maxX, maxY, shapes);
        }
        return shapes;
    }

    //recursive helper for findShapesOverlappingBox()
    private void findShapesOverlappingBox(Node node, double minX, double minY, double maxX, double maxY,
                                          Set<Shape> shapes) {
        if (!node.overlaps(minX, minY, maxX, maxY)) {
            return;
        }
        if (node.isLeaf()) {
            shapes.add(node.shape);
            return;
        }
        findShapesOverlappingBox(node.left, minX, minY, maxX, maxY, shapes);
        findShapesOverlappingBox(node.right, minX, minY, maxX, maxY, shapes);
    }

    //pre-order listing of the tree, one node per line, indented one tab per level
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (root != null) {
            appendNode(root, 0, builder);
        }
        return builder.toString();
    }

    //appends node and its subtree to the builder in pre-order
    private void appendNode(Node node, int depth, StringBuilder builder) {
        builder.append("\t".repeat(depth));
        if (node.isLeaf()) {
            builder.append(formatShape(node.shape)).append('\n');
            return;
        }
        builder.append("[(").append(node.minX).append(", ").append(node.minY).append("), (")
                .append(node.maxX).append(", ").append(node.maxY).append(")]\n");
        appendNode(node.left, depth + 1, builder);
        appendNode(node.right, depth + 1, builder);
    }

    //returns the text printed for a leaf's shape
    private static String formatShape(Shape shape) {
        if (shape instanceof Triangle) {
            Triangle t = (Triangle) shape;
            return formatPoint(t.a) + " " + formatPoint(t.b) + " " + formatPoint(t.c);
        }
        return shape.toString().trim();
    }

    //returns the point formatted as "(x, y)"
    private static String formatPoint(Point point) {
        return "(" + point.x + ", " + point.y + ")";
    }
}