/**
 * A set of two points to represent the upper left and bottom right coordinates of an image.
 * @Author Samuel Shen
 */

public class PointSet {
    Point upperLeft;
    Point lowerRight;

    public PointSet() {

    }

    public PointSet(Double ullat, Double ullon, Double lrlat, Double lrlon) {
        upperLeft = new Point();
        lowerRight = new Point();
        upperLeft.x = ullon;
        upperLeft.y = ullat;
        lowerRight.x = lrlon;
        lowerRight.y = lrlat;
    }

    public PointSet(Point upperLeft, Point lowerRight) {
        this.upperLeft = upperLeft;
        this.lowerRight = lowerRight;
    }
}
