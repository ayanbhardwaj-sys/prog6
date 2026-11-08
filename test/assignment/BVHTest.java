package assignment;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.regex.*;

import static org.junit.jupiter.api.Assertions.*;

public class BVHTest {

    private static BVH newBVH() {
        return new BoundedVolumeHierarchy();
    }

    private static BVH built(List<Shape> shapes, BVH.SplitMethod method) {
        BVH bvh = newBVH();
        if (method != null) bvh.setSplitMethod(method);
        bvh.buildBVH(new ArrayList<>(shapes));
        return bvh;
    }

    private static Triangle tri(int ax, int ay, int bx, int by, int cx, int cy) {
        return new Triangle(new Point(ax, ay), new Point(bx, by), new Point(cx, cy));
    }

    private static List<Shape> randomTriangles(Random r, int count, int range) {
        List<Shape> shapes = new ArrayList<>();
        while (shapes.size() < count) {
            int ax = r.nextInt(2 * range) - range, ay = r.nextInt(2 * range) - range;
            int bx = ax + r.nextInt(41) - 20, by = ay + r.nextInt(41) - 20, cx = ax + r.nextInt(41) - 20, cy = ay + r.nextInt(41) - 20;
            if ((long) (bx - ax) * (cy - ay) != (long) (by - ay) * (cx - ax)) shapes.add(tri(ax, ay, bx, by, cx, cy));
        }
        return shapes;
    }

    private static String key(Object o) {
        double[] v = o instanceof Triangle t ? new double[]{t.a.x, t.a.y, t.b.x, t.b.y, t.c.x, t.c.y} : (double[]) o;
        String[] points = {Math.round(v[0]) + "," + Math.round(v[1]), Math.round(v[2]) + "," + Math.round(v[3]),
                Math.round(v[4]) + "," + Math.round(v[5])};
        Arrays.sort(points);
        return String.join(";", points);
    }

    private record Parsed(int depth, boolean leaf, double[] v, List<Parsed> kids) {
        double[] box() {
            return !leaf ? v : new double[]{Math.min(v[0], Math.min(v[2], v[4])), Math.min(v[1], Math.min(v[3], v[5])),
                    Math.max(v[0], Math.max(v[2], v[4])), Math.max(v[1], Math.max(v[3], v[5]))};
        }

        <C extends Collection<String>> C leaves(C keys) {
            if (leaf) keys.add(key(v));
            kids.forEach(k -> k.leaves(keys));
            return keys;
        }
    }

    private static Parsed parse(String text) {
        Parsed root = null;
        Deque<Parsed> stack = new ArrayDeque<>();
        for (String line : text.split("\n")) {
            int depth = 0;
            for (int i = 0; line.startsWith("\t", i) || line.startsWith("    ", i); depth++) i += line.charAt(i) == '\t' ? 1 : 4;
            List<Double> nums = new ArrayList<>();
            for (Matcher m = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(line); m.find(); ) nums.add(Double.parseDouble(m.group()));
            if (line.isBlank()) continue;
            boolean leaf = !line.trim().startsWith("[");
            assertEquals(leaf ? 6 : 4, nums.size(), "Bad toString() line: \"" + line + "\"\n" + text);
            Parsed node = new Parsed(depth, leaf, nums.stream().mapToDouble(d -> d).toArray(), new ArrayList<>());
            while (!stack.isEmpty() && stack.peek().depth() >= depth) stack.pop();
            if (stack.isEmpty()) {
                assertTrue(root == null && depth == 0, "toString() should have one root at depth 0:\n" + text);
                root = node;
            } else {
                assertTrue(depth == stack.peek().depth() + 1 && !stack.peek().leaf(), "Bad indentation at \"" + line + "\"\n" + text);
                stack.peek().kids().add(node);
            }
            stack.push(node);
        }
        return root;
    }

    private static int checkNode(Parsed node, boolean avl, String text) {
        if (node.leaf()) return 0;
        assertEquals(2, node.kids().size(), "Internal nodes need exactly 2 children:\n" + text);
        int lh = checkNode(node.kids().get(0), avl, text), rh = checkNode(node.kids().get(1), avl, text);
        double[] l = node.kids().get(0).box(), r = node.kids().get(1).box();
        double[] tight = {Math.min(l[0], r[0]), Math.min(l[1], r[1]), Math.max(l[2], r[2]), Math.max(l[3], r[3])};
        assertArrayEquals(tight, node.v(), 1E-6, "A bounding box is not tight around its children:\n" + text);
        assertTrue(!avl || Math.abs(lh - rh) <= 1, "AVL property violated (heights " + lh + ", " + rh + "):\n" + text);
        return 1 + Math.max(lh, rh);
    }

    private static Parsed checkTree(BVH bvh, List<Shape> expected, boolean avl) {
        String text = bvh.toString();
        if (expected.isEmpty()) {
            assertTrue(text == null || text.isBlank(), "An empty BVH should print nothing:\n" + text);
            return null;
        }
        Parsed root = parse(text);
        assertNotNull(root, "toString() is empty but the BVH should hold " + expected.size() + " triangles");
        checkNode(root, avl, text);
        List<String> actual = root.leaves(new ArrayList<>());
        Collections.sort(actual);
        assertEquals(expected.stream().map(BVHTest::key).sorted().toList(), actual, "The leaves are not the triangles that were added:\n" + text);
        return root;
    }

    private static void checkQueries(BVH bvh, List<Shape> shapes, Random r) {
        for (int i = 0; i < 100; i++) {
            Point2D.Double p = i < shapes.size() ? shapes.get(i).getCenter() : new Point2D.Double(r.nextDouble() * 440 - 220, r.nextDouble() * 440 - 220);
            Set<Shape> expected = new HashSet<>();
            shapes.stream().filter(s -> s.containsPoint(p)).forEach(expected::add);
            assertEquals(expected, bvh.findCollision(p), "findCollision(" + p.x + ", " + p.y + ") is wrong");
            Point o = new Point(r.nextInt(440) - 220, r.nextInt(440) - 220);
            int dx = r.nextInt(21) - 10, dy = dx == 0 ? 1 + r.nextInt(10) : r.nextInt(21) - 10;
            Vector2D d = new Vector2D(dx, dy);
            double best = Double.POSITIVE_INFINITY;
            for (Shape s : shapes) {
                Point2D.Double hit = s.findIntersection(o, d);
                if (hit != null) best = Math.min(best, BVH.distanceBetweenPoints(o, hit));
            }
            String ray = "ray from (" + o.x + ", " + o.y + ") toward (" + dx + ", " + dy + ")";
            Shape actual = bvh.intersectRay(new Point2D.Double(o.x, o.y), d);
            if (best == Double.POSITIVE_INFINITY) assertNull(actual, "intersectRay() should miss for " + ray);
            else {
                assertTrue(actual != null && actual.findIntersection(o, d) != null, "intersectRay() missed the " + ray);
                assertEquals(best, BVH.distanceBetweenPoints(o, actual.findIntersection(o, d)), 1E-6, "Not the closest triangle for " + ray);
            }
        }
    }

    @Test
    void emptyAndSingle() {
        BVH bvh = newBVH();
        checkTree(bvh, List.of(), true);
        assertTrue(bvh.findCollision(new Point2D.Double(0, 0)).isEmpty(), "Empty BVH: findCollision() should be empty");
        assertNull(bvh.intersectRay(new Point2D.Double(0, 0), new Vector2D(1, 0)), "Empty BVH: intersectRay() should be null");
        assertDoesNotThrow(() -> bvh.remove(tri(0, 0, 1, 0, 0, 1)), "remove() on an empty BVH should do nothing");
        Triangle t = tri(0, 0, 4, 0, 0, 4);
        assertTrue(checkTree(built(List.of(t), null), List.of(t), true).leaf(), "One triangle should be a single leaf");
        bvh.buildBVH(new ArrayList<>(List.of(t)));
        assertTrue(bvh.findCollision(new Point2D.Double(3, 3)).isEmpty(), "Inside the box but outside the triangle");
    }

    @Test
    void specExampleToString() {
        List<Shape> shapes = List.of(tri(0, 0, 1, 0, 1, 2), tri(1, 5, 2, 3, 3, 5), tri(2, 2, 3, 1, 4, 2), tri(4, 5, 5, 3, 5, 5));
        BVH bvh = built(shapes, null);
        Parsed root = checkTree(bvh, shapes, true);
        String msg = "toString() for the Figure 2 example is wrong:\n" + bvh;
        assertArrayEquals(new double[]{0, 0, 5, 5}, root.v(), msg);
        assertArrayEquals(new double[]{0, 0, 3, 5}, root.kids().get(0).v(), msg);
        assertArrayEquals(new double[]{2, 1, 5, 5}, root.kids().get(1).v(), msg);
        for (int i = 0; i < 4; i++) assertEquals(key(shapes.get(i)), key(root.kids().get(i / 2).kids().get(i % 2).v()), msg);
    }

    @Test
    void medianBuilds() {
        Random random = new Random(2);
        for (int n = 1; n <= 64; n++) {
            List<Shape> shapes = randomTriangles(random, n, 200);
            BVH median = built(shapes, BVH.SplitMethod.SPLIT_MEDIAN);
            assertEquals(32 - Integer.numberOfLeadingZeros(n - 1), checkNode(checkTree(median, shapes, true), true, ""), "Median build of " + n + " should have minimal height");
            assertEquals(median.toString(), built(shapes, null).toString(), "The default split method should be the median");
            checkQueries(median, shapes, random);
        }
        int[][] centers = {{0, 0}, {1, 10}, {2, 1}, {3, 11}, {100, 0}, {101, 10}, {102, 1}, {103, 11}};
        List<Shape> shapes = Arrays.stream(centers).map(c -> (Shape) tri(c[0] - 1, c[1] - 1, c[0] + 1, c[1] - 1, c[0], c[1] + 2)).toList();
        BVH bvh = built(shapes, null);
        Set<Set<String>> pairs = new HashSet<>();
        checkTree(bvh, shapes, true).kids().forEach(k -> k.kids().forEach(g -> pairs.add(g.leaves(new HashSet<>()))));
        assertEquals(Set.of(Set.of(key(shapes.get(0)), key(shapes.get(2))), Set.of(key(shapes.get(1)), key(shapes.get(3))),
                Set.of(key(shapes.get(4)), key(shapes.get(6))), Set.of(key(shapes.get(5)), key(shapes.get(7)))), pairs,
                "The second level should split along the y-axis:\n" + bvh);
    }

    @Test
    void midrangeBuilds() {
        Random random = new Random(3);
        for (int n = 1; n <= 40; n++) {
            List<Shape> shapes = randomTriangles(random, n, 200);
            BVH bvh = built(shapes, BVH.SplitMethod.SPLIT_MIDRANGE);
            checkTree(bvh, shapes, false);
            checkQueries(bvh, shapes, random);
        }
        List<Shape> shapes = List.of(tri(0, 0, 2, 0, 1, 2), tri(3, 0, 5, 0, 4, 2), tri(6, 0, 8, 0, 7, 2),
                tri(59, 0, 61, 0, 60, 2), tri(98, 0, 100, 0, 99, 2));
        BVH bvh = built(shapes, BVH.SplitMethod.SPLIT_MIDRANGE);
        Set<List<Double>> boxes = new HashSet<>();
        checkTree(bvh, shapes, true).kids().forEach(k -> boxes.add(Arrays.stream(k.box()).boxed().toList()));
        assertEquals(Set.of(List.of(0.0, 0.0, 8.0, 2.0), List.of(59.0, 0.0, 100.0, 2.0)), boxes, "Midrange split should be at x = 50:\n" + bvh);
        Triangle wide = tri(0, 0, 100, 0, 0, 3);
        bvh = built(List.of(wide, tri(10, 0, 12, 0, 11, 2), tri(20, 0, 22, 0, 21, 2)), BVH.SplitMethod.SPLIT_MIDRANGE);
        assertTrue(parse(bvh.toString()).kids().stream().anyMatch(k -> k.leaf() && key(k.v()).equals(key(wide))),
                "The triangle nearest the split should be isolated:\n" + bvh);
    }

    @Test
    void insertPriorities() {
        List<Shape> shapes = new ArrayList<>(List.of(tri(0, 0, 2, 0, 1, 2), tri(10, 0, 12, 0, 11, 2), tri(20, 0, 22, 0, 21, 2)));
        BVH bvh = built(shapes, null);
        Parsed root = checkTree(bvh, shapes, true);
        String oldLeaf = key((root.kids().get(0).leaf() ? root.kids().get(0) : root.kids().get(1)).v());
        Triangle added = tri(100, 100, 102, 100, 101, 102);
        bvh.insert(added);
        shapes.add(added);
        for (Parsed child : checkTree(bvh, shapes, true).kids()) assertEquals(child.leaves(new HashSet<>()).contains(key(added)),
                child.leaves(new HashSet<>()).contains(oldLeaf), "insert() should prefer the child that is a leaf:\n" + bvh);
        shapes = new ArrayList<>(List.of(tri(0, 0, 2, 0, 1, 2), tri(4, 0, 6, 0, 5, 2), tri(100, 0, 102, 0, 101, 2), tri(104, 0, 106, 0, 105, 2)));
        bvh = built(shapes, null);
        bvh.insert(added = tri(110, 0, 112, 0, 111, 2));
        shapes.add(added);
        Set<Set<String>> groups = new HashSet<>();
        checkTree(bvh, shapes, true).kids().forEach(k -> groups.add(k.leaves(new HashSet<>())));
        assertTrue(groups.contains(Set.of(key(shapes.get(2)), key(shapes.get(3)), key(added))), "insert() should choose the child that grows less:\n" + bvh);
    }

    @Test
    void insertAndRemove() {
        Random random = new Random(10);
        BVH bvh = newBVH();
        List<Shape> current = new ArrayList<>();
        assertDoesNotThrow(() -> bvh.insert(null));
        for (int step = 0; step < 400; step++) {
            Shape shape = randomTriangles(random, 1, 200).get(0);
            if (!current.isEmpty() && random.nextInt(5) < 2) {
                Triangle t = (Triangle) current.remove(random.nextInt(current.size()));
                bvh.remove(new Triangle(t.a, t.b, t.c));
            } else if (!current.contains(shape)) {
                bvh.insert(shape);
                current.add(shape);
            }
            checkTree(bvh, current, true);
        }
        checkQueries(bvh, current, random);
        String before = bvh.toString();
        bvh.remove(tri(500, 500, 501, 500, 500, 501));
        assertEquals(before, bvh.toString(), "Removing a missing triangle should change nothing");
        while (!current.isEmpty()) {
            bvh.remove(current.remove(current.size() - 1));
            checkTree(bvh, current, true);
        }
    }

    private static Visualization scene(Shape... shapes) {
        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.buildBVH(new ArrayList<>(List.of(shapes)));
        Visualization v = new Visualization();
        v.setBoundingVolumeHierarchy(bvh);
        return v;
    }

    private static double edgeDistance(double x, double y, Triangle t) {
        double best = Double.POSITIVE_INFINITY;
        for (Point[] e : new Point[][]{{t.a, t.b}, {t.b, t.c}, {t.a, t.c}}) {
            double dx = e[1].x - e[0].x, dy = e[1].y - e[0].y;
            double s = Math.max(0, Math.min(1, ((x - e[0].x) * dx + (y - e[0].y) * dy) / (dx * dx + dy * dy)));
            best = Math.min(best, Math.hypot(x - e[0].x - s * dx, y - e[0].y - s * dy));
        }
        return best;
    }

    @Test
    void visualization() {
        Visualization open = scene();
        assertFalse(open.moveSquare('x'), "Keys other than WASD should not move the square");
        for (char key : "wasd".toCharArray()) assertTrue(open.moveSquare(key), "Moving in open space should succeed");
        assertTrue(open.drawGUI().isEmpty(), "An empty scene draws nothing");
        Visualization blocked = scene(tri(12, -20, 30, -20, 12, 20));
        assertFalse(blocked.moveSquare('d'), "Moving into a triangle should be blocked");
        assertTrue(blocked.moveSquare('a'), "Moving away should be allowed");
        assertTrue(blocked.moveSquare('d'), "Moving back should be allowed");
        assertFalse(blocked.moveSquare('d'), "Moving into the triangle should still be blocked");
        Triangle t = tri(0, 100, 80, 100, 40, 50);
        Visualization visualization = scene(t);
        Set<Point> before = new HashSet<>(visualization.drawGUI());
        assertTrue(before.contains(new Point(200, 300)), "Virtual (0, 100) should be drawn at GUI (200, 300)");
        before.forEach(p -> assertTrue(p.x >= 0 && p.x < 400 && p.y >= 0 && p.y < 400 && edgeDistance(p.x - 200, p.y - 200, t) <= 1.5,
                "Pixel " + p + " is outside the frame or not on the triangle's edge"));
        assertTrue(visualization.moveSquare('d'));
        Set<Point> after = new HashSet<>(visualization.drawGUI());
        assertTrue(after.contains(new Point(195, 300)), "After moving right by 5 the vertex should be at GUI (195, 300)");
        after.forEach(p -> assertTrue(edgeDistance(p.x - 195, p.y - 200, t) <= 1.5, "Pixel " + p + " is off the shifted edge"));
        assertTrue(visualization.moveSquare('a'));
        assertEquals(before, new HashSet<>(visualization.drawGUI()), "Moving back should restore the original view");
        Triangle hidden = tri(150, -10, 170, 10, 150, 10); //hidden behind the wall from x = 30 to 40
        scene(tri(30, -150, 40, 0, 30, 150), hidden).drawGUI().forEach(p ->
                assertTrue(edgeDistance(p.x - 200, p.y - 200, hidden) > 3, "Pixel " + p + " belongs to a hidden triangle"));
    }
}
