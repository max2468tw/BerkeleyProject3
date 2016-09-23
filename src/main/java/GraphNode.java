import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single node on a Graph.
 * @Author Hong Shuo Chen
 */
public class GraphNode {
    private Long id;
    private Point p;
    private Map tag;
    private String name;
    private boolean used;
    private double distance;
    public GraphNode(Long id, Point p) {
        this.id = id;
        this.p = p;
        tag = new HashMap();
        distance = 0;
    }
    public boolean used() {
        return used;
    }
    public String name() {
        return name;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
    public double distance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
    public Long getId() {
        return id;
    }

    public Point getP() {
        return p;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void pushTag(String k, String v) {
        tag.put(k, v);
    }

    public String toString()    {
        return "id : " + id + " name : " + name + " point:" + p;
    }
}
