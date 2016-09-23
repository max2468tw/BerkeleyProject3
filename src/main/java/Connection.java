
import static java.lang.StrictMath.sqrt;

/**
 * A connection between two points on a Graph.
 * @Author Hong Shuo Chen
 */
public class Connection {
    private Long idFrom;
    private Long idTo;
    private Point from;
    private Point to;
    private Double distance;

    public Connection(Point from, Point to, Long idFrom, Long idTo) {
        this.from = from;
        this.to = to;
        this.idFrom = idFrom;
        this.idTo = idTo;
        this.distance = calculatedistance(from, to);
    }
    static double calculatedistance(Point from, Point to) {
        return sqrt((from.x - to.x) * (from.x - to.x) + (from.y - to.y) * (from.y - to.y));
    }
    public String toString() {
        return "(from , to)" + "=" + "(" + idFrom + " " + idTo + ")";
    }

    public Long getIdFrom() {
        return idFrom;
    }

    public Long getIdTo() {

        return idTo;
    }

    public Point getFrom() {
        return from;
    }

    public Double getDistance() {

        return distance;
    }
}
