import java.awt.image.BufferedImage;

/**
 * A node in a QuadTree structure.  Holds the four other QNodes in coordinate planes I, II, III, and IV.
 * @Author Samuel Shen
 */

public class QNode {
    PointSet set;
    BufferedImage image;
    String name;

    QNode one;
    QNode two;
    QNode three;
    QNode four;

    public QNode() {
        name = "";
        set = new PointSet();
    }

    public QNode(QNode first, QNode second, QNode third, QNode fourth, String name) {
        one = first;
        two = second;
        three = third;
        four = fourth;
        this.name = name;
    }

    public QNode(PointSet set, BufferedImage image) {
        this.set = set;
        this.image = image;
    }

    public boolean contains(Point point) {
        if (point.x < set.lowerRight.x && point.x >= set.upperLeft.x) {
            if (point.y <= set.upperLeft.y && point.y > set.lowerRight.y) {
                return true;
            }
        }
        return false;
    }

}
