import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;


/**
 *  Parses OSM XML files using an XML SAX parser. Used to construct the graph of roads for
 *  pathfinding, under some constraints.
 *  See OSM documentation on
 *  <a href="http://wiki.openstreetmap.org/wiki/Key:highway">the highway tag</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Way">the way XML element</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Node">the node XML element</a>,
 *  and the java
 *  <a href="https://docs.oracle.com/javase/tutorial/jaxp/sax/parsing.html">SAX parser tutorial</a>.
 *  @author Alan Yao
 *  @Author Samuel Shen
 */
public class MapDBHandler extends DefaultHandler {
    /**
     * Only allow for non-service roads; this prevents going on pedestrian streets as much as
     * possible. Note that in Berkeley, many of the campus roads are tagged as motor vehicle
     * roads, but in practice we walk all over them with such impunity that we forget cars can
     * actually drive on them.
     */
    private static final Set<String> ALLOWED_HIGHWAY_TYPES = new HashSet<>(Arrays.asList
            ("motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
                    "residential", "living_street", "motorway_link", "trunk_link", "primary_link",
                    "secondary_link", "tertiary_link"));
    private String activeState = "";
    private final GraphDB g;
    private Long lastput;
    private LinkedList<Long> later;
    public MapDBHandler(GraphDB g) {
        this.g = g;
    }

    /**
     * Called at the beginning of an element. Typically, you will want to handle each element in
     * here, and you may want to track the parent element.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available. This tells us which element we're looking at.
     * @param attributes The attributes attached to the element. If there are no attributes, it
     *                   shall be an empty Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     * @see Attributes
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        /* Some example code on how you might begin to parse XML files. */
        if (qName.equals("node")) {
            activeState = "node";
            Long id = Long.valueOf(attributes.getValue("id"));
            Double lon = Double.valueOf(attributes.getValue("lon"));
            Double lat = Double.valueOf(attributes.getValue("lat"));
            g.NodeDB.put(id, new GraphNode(id, new Point(lon, lat)));
            g.findID.put(new Point(lon, lat), id);
            lastput = id;
        } else if (qName.equals("way")) {
            activeState = "way";
            later = new LinkedList<>();
            //System.out.println("Beginning a way...");
        } else if (activeState.equals("way") && qName.equals("nd")) {
            later.add(Long.valueOf(attributes.getValue("ref")));
        } else if (activeState.equals("way") && qName.equals("tag") && attributes.getValue("k")
                .equals("highway")) {
            String k = attributes.getValue("k");
            String v = attributes.getValue("v");
            if (ALLOWED_HIGHWAY_TYPES.contains(v)) {
                for (int i = 0; i < later.size() - 1; i++) {
                    g.NodeDB.get(later.get(i)).setUsed(true);
                    g.NodeDB.get(later.get(i + 1)).setUsed(true);
                    if (g.con.get(later.get(i)) != null) {
                        g.con.get(later.get(i))
                                .add(new Connection(g.NodeDB.get(later.get(i)).getP(),
                                        g.NodeDB.get(later.get(i + 1)).getP(), later.get(i),
                                later.get(i + 1)));
                    } else {
                        ArrayList a = new ArrayList();
                        a.add(new Connection(g.NodeDB.get(later.get(i)).getP(),
                                g.NodeDB.get(later.get(i + 1)).getP(),
                                later.get(i), later.get(i + 1)));
                        g.con.put(later.get(i), a);
                    }
                    if (g.con.get(later.get(i + 1)) != null) {
                        g.con.get(later.get(i + 1))
                                .add(new Connection(g.NodeDB.get(later.get(i + 1)).getP(),
                                        g.NodeDB.get(later.get(i))
                                        .getP(), later.get(i + 1), later.get(i)));
                    } else {
                        ArrayList a = new ArrayList();
                        a.add(new Connection(g.NodeDB.get(later.get(i + 1)).getP(),
                                g.NodeDB.get(later.get(i)).getP(), later.get(i + 1),
                                later.get(i)));
                        g.con.put(later.get(i + 1), a);
                    }
                }
            }
            //System.out.println("Tag with k=" + k + ", v=" + v + ".");
        } else if (activeState.equals("node") && qName.equals("tag") && attributes.getValue("k")
                .equals("name")) {
            g.nameToPoint.put(attributes.getValue("v"), g.NodeDB.get(lastput).getP());
            g.NodeDB.get(lastput).setName(attributes.getValue("v"));
            g.NodeDB.get(lastput).pushTag("name", attributes.getValue("v"));
        }
    }

    /**
     * Receive notification of the end of an element. You may want to take specific terminating
     * actions here, like finalizing vertices or edges found.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available.
     * @throws SAXException  Any SAX exception, possibly wrapping another exception.
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

    }

}
