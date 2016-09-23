import java.awt.image.BufferedImage;

public class QuadTree {
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    QNode head;


    public QuadTree() {
        head = new QNode();
        head.one = new QNode();
        head.two = new QNode();
        head.three = new QNode();
        head.four = new QNode();
    }

    public QuadTree(QNode head) {
        this.head = head;
    }

    public void setImage(String name, BufferedImage image) {
        QNode node = getNode(name);
        node.image = image;
    }

    public void setPointSet(String name, PointSet newSet) {
        QNode node = getNode(name);
        node.set = newSet;
    }

    /**
     * Gets the node at name.
     * @param name of the node to get
     * @return
     */
    public QNode getNode(String name) {
        QNode current = head;
        if (name.contains(".")) {   //cuts out any .png endings
            name = name.substring(0, name.indexOf("."));
        }
        char[] arr = name.toCharArray();
        for (char c: arr) {
            if (c == '1') {
                current = current.one;
            } else if (c == '2') {
                current = current.two;
            } else if (c == '3') {
                current = current.three;
            } else if (c == '4') {
                current = current.four;
            }
        }
        return current;
    }

    public void initialize() {
        initializeHelper(head, ROOT_ULLAT, ROOT_ULLON, ROOT_LRLAT, ROOT_LRLON, 1);
    }

    /**
     * Helps with initalization.  Coordinates are coordinates of node.
     * @param node is the node whose children are to be modified.
     * @param ullat
     * @param ullon
     * @param lrlat
     * @param lrlon
     * @param count is the current level.
     */
    public void initializeHelper(QNode node, Double ullat, Double ullon, Double lrlat,
            Double lrlon, int count) {
        node.one = new QNode();
        node.two = new QNode();
        node.three = new QNode();
        node.four = new QNode();

        Double deltaLON = (lrlon - ullon) / 2;
        Double deltaLAT = (ullat - lrlat) / 2;

        Point upperLeft = new Point(ullon, ullat);
        Point upperMid = new Point(ullon + deltaLON, ullat);
        Point middle = new Point(ullon + deltaLON, ullat - deltaLAT);
        Point midRight = new Point(lrlon, ullat - deltaLAT);
        Point midLeft = new Point(ullon, ullat - deltaLAT);
        Point lowerMid = new Point(ullon + deltaLON, lrlat);
        Point lowerRight = new Point(lrlon, lrlat);

        if (count <= 7) {
            node.one.set = new PointSet(upperLeft, middle);
            node.two.set = new PointSet(upperMid, midRight);
            node.three.set = new PointSet(midLeft, lowerMid);
            node.four.set = new PointSet(middle, lowerRight);

            node.one.name = node.name + "1";
            node.two.name = node.name + "2";
            node.three.name = node.name + "3";
            node.four.name = node.name + "4";

            PointSet thisSet = node.one.set;

            initializeHelper(node.one, thisSet.upperLeft.y, thisSet.upperLeft.x,
                    thisSet.lowerRight.y, thisSet.lowerRight.x, count + 1);
            thisSet = node.two.set;
            initializeHelper(node.two, thisSet.upperLeft.y, thisSet.upperLeft.x,
                    thisSet.lowerRight.y, thisSet.lowerRight.x, count + 1);
            thisSet = node.three.set;
            initializeHelper(node.three, thisSet.upperLeft.y, thisSet.upperLeft.x,
                    thisSet.lowerRight.y, thisSet.lowerRight.x, count + 1);
            thisSet = node.four.set;
            initializeHelper(node.four, thisSet.upperLeft.y, thisSet.upperLeft.x,
                    thisSet.lowerRight.y, thisSet.lowerRight.x, count + 1);

        }
    }
}
