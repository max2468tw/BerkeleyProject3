/**
 * A simple Point class holding just an X and Y coordinate.
 * @Author Samuel Shen
 */
public class Point {
    Double x;
    Double y;

    public Point() { }

    public Point(Double x, Double y) {
        this.x = x;
        this.y = y;
    }

    public int hashCode() {
        Double result = Math.sqrt((this.x * this.x) + (this.y * this.y));
        String resultString = result.toString();
        return resultString.hashCode();
    }

    public boolean equals(Object o) {
        return (((Point) o).x.equals(this.x) && ((Point) o).y.equals(this.y));
    }

    public String toString() {
        return "(" + x + " , " + y + ")";
    }
}
