import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 * Handles Graph operations.
 * @Author Hong Shuo Chen
 * @Author Samuel Shen
 */
public class GraphDB {
    /**
     * Example constructor shows how to create and start an XML parser.
     * @param dbPath Path to the XML file to be parsed.
     */
    Point end;
    HashMap<Long, GraphNode> NodeDB;
    HashMap<Point, Long> findID;
    HashMap<Long, ArrayList<Connection>> con;
    HashMap<String, Point> nameToPoint;

    public GraphDB(String dbPath) {
        NodeDB = new HashMap<>();
        findID = new HashMap<>();
        con = new HashMap<>();
        nameToPoint = new HashMap<>();
        try {
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            MapDBHandler maphandler = new MapDBHandler(this);
            saxParser.parse(inputFile, maphandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        Set<Long> p = NodeDB.keySet();
        LinkedList<Long> pas = new LinkedList<>();
        for (Long x: p) {
            if (!NodeDB.get(x).used()) {
                pas.add(x);
            }
        }
        for (Long x: pas) {
            NodeDB.remove(x);
        }
    }
    public ArrayList<Long> shortestPath(GraphNode startVertex, GraphNode endVertex) {


        end = endVertex.getP();
        Queue<GraphNode> fringe = new PriorityQueue<>(100, new NodeComparator());

        ArrayList<Long> rtn = new ArrayList<>();
        HashSet<Long> done = new HashSet<>();
        HashMap<Long, Double> d = new HashMap<>();
        HashMap<Long, Long> from = new HashMap<>();
        fringe.add(startVertex);
        d.put(startVertex.getId(), 0.0);
        startVertex.setDistance(0.0);
        while (!fringe.isEmpty()) {
            GraphNode value = fringe.poll();
            if (done.contains(value.getId())) continue;
            if (value.getId() == endVertex.getId()) {
                Long p = value.getId();
                while (!startVertex.getId().equals(p)) {
                    rtn.add(0, p);
                    p = from.get(p);
                }
                rtn.add(0, p);

                return rtn;
            }
            done.add(value.getId());
            for (Connection x : con.get(value.getId())) {
                Double distance = x.getDistance() + value.distance();
                if (!d.containsKey(x.getIdTo()) || (!done.contains(x.getIdTo())
                        && distance < d.get(x.getIdTo()))) {
                    NodeDB.get(x.getIdTo()).setDistance(distance);
                    fringe.add(NodeDB.get(x.getIdTo()));
                    d.put(x.getIdTo(), distance);
                    from.put(x.getIdTo(), x.getIdFrom());
                }
            }
        }
        return rtn;
    }

    public double h(Point curr) {
        return calculatedistance(curr, end);

    }
    static double calculatedistance(Point from, Point to) {
        return Math.sqrt((from.x - to.x) * (from.x - to.x) + (from.y - to.y) * (from.y - to.y));
    }
    public class NodeComparator implements Comparator<GraphNode> {
        @Override
        public int compare(GraphNode o1, GraphNode o2) {
            if (o1.distance() + h(o1.getP()) < o2.distance() + h(o2.getP())) {
                return -1;
            } else if (o1.distance() + h(o1.getP()) > o2.distance() + h(o2.getP())) {

                return 1;
            } else {
                return 0;
            }
        }
    }
}

