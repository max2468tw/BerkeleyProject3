import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;

/* Maven is used to pull in these dependencies. */
import com.google.gson.Gson;


import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;


import static java.lang.StrictMath.sqrt;
import static spark.Spark.*;

/**
 * MapServer handles API calls to various other classes.
 * @author Alan Yao
 * @Author Samuel Shen
 * @Author Hong Shuo Chen
 */
public class MapServer {

    static HashMap<Point, String> pointToName;
    //The mapping of Point to ImageName
    static HashMap<Point, String> imageNameMap;
    //The mapping of ImageName to Image File
    static HashMap<String, BufferedImage> imageMap;
    //The mapping of name to coordinates
    static HashMap<String, PointSet> coordinateMap;
    //quadtree
    //The mapping of name to coordinates
    static QuadTree tree = new QuadTree();
    static Trie trie;

    //map from cleaned strings to full strings
    static HashMap<String, String> cleanedToOriginal;

    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /** Each tile is 256x256 pixels. */
    public static final int TILE_SIZE = 256;
    /** HTTP failed response. */
    private static final int HALT_RESPONSE = 403;
    /** Route stroke information: typically roads are not more than 5px wide. */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /** Route stroke information: Cyan with half transparency. */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);
    /** The tile images are in the IMG_ROOT folder. */
    private static final String IMG_ROOT = "img/";
    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    static String[] REQUIRED_RASTER_REQUEST_PARAM = {"ullat", "ullon", "lrlat", "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    static String[] REQUIREDROUTEREQUESTPARAMS = {"start_lat", "start_lon", "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB graphDB;

    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {

        cleanedToOriginal = new HashMap<>();
        graphDB = new GraphDB(OSM_DB_PATH);
        imageNameMap = new HashMap<>();
        imageMap = new HashMap<>();
        coordinateMap = new HashMap<>();
        tree.initialize();
        trie = new Trie();
        pointToName = new HashMap<>();

        for (Map.Entry<String, Point> entry: graphDB.nameToPoint.entrySet()) {
            String name = entry.getKey();
            String cleanName = cleanString(name);
            pointToName.put(entry.getValue(), name);
            if (name != null && cleanName.length() != 0) {
                trie.addWord(cleanName);
                cleanedToOriginal.put(cleanName, name);
            }
        }
    }

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });

        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> rasterParams =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAM);
            /* Required to have valid raster params */
            validateRequestParameters(rasterParams, REQUIRED_RASTER_REQUEST_PARAM);
            /* The png image is written to the ByteArrayOutputStream */
            Map<String, Object> rasteredImgParams = new HashMap<>();
            /* getMapRaster() does almost all the work for this API call */
            BufferedImage im = getMapRaster(rasterParams, rasteredImgParams);
            /* Check if we have routing parameters. */
            HashMap<String, Double> routeParams =
                    getRequestParams(req, REQUIREDROUTEREQUESTPARAMS);
            /* If we do, draw the route too. */
            if (hasRequestParameters(routeParams, REQUIREDROUTEREQUESTPARAMS)) {
                findAndDrawRoute(routeParams, rasteredImgParams, im);
            }
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                writeJpgToStream(im, os);
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
                os.flush();
                os.close();
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Check if the computed parameter map matches the required parameters on length.
     */
    private static boolean hasRequestParameters(
            HashMap<String, Double> params, String[] requiredParams) {
        return params.size() == requiredParams.length;
    }

    /**
     * Validate that the computed parameters matches the required parameters.
     * If the parameters do not match, halt.
     */
    private static void validateRequestParameters(
            HashMap<String, Double> params, String[] requiredParams) {
        if (params.size() != requiredParams.length) {
            halt(HALT_RESPONSE, "Request failed - parameters missing.");
        }
    }

    /**
     * Return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     * @param req HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (reqParams.contains(param)) {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }

    /**
     * Write a <code>BufferedImage</code> to an <code>OutputStream</code>. The image is written as
     * a lossy JPG, but with the highest quality possible.
     * @param im Image to be written.
     * @param os Stream to be written to.
     */
    static void writeJpgToStream(BufferedImage im, OutputStream os) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(1.0F); // Highest quality of jpg possible
        writer.setOutput(new MemoryCacheImageOutputStream(os));
        try {
            writer.write(im);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     *     The rastered photo must have the following properties:
     *     <ul>
     *         <li>Has dimensions of at least w by h, where w and h are the user viewport width
     *         and height.</li>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *    Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     * @param inputParams Map of the HTTP GET request's query parameters - the query bounding box
     *                    and the user viewport width and height.
     * @param rasteredImageParams A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Integer, the width of the rastered image <br>
     * "raster_height" -> Integer, the height of the rastered image <br>
     * "depth"         -> Integer, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @return a <code>BufferedImage</code>, which is the rastered result.
     * @see #REQUIRED_RASTER_REQUEST_PARAM
     */
    public static BufferedImage getMapRaster(Map<String, Double> inputParams,
            Map<String, Object> rasteredImageParams) {




        Double requestedHeight = inputParams.get("h");
        Double requestedWidth = inputParams.get("w");
        Double queryULLON = inputParams.get("ullon");
        Double queryLRLON = inputParams.get("lrlon");
        Double queryULLAT = inputParams.get("ullat");
        Double queryLRLAT = inputParams.get("lrlat");

        //the double representation of the query window's longitudional distance/pixel - min
        Double queryLDPP = ((queryLRLON - queryULLON)) / requestedWidth;
        Double queryLatDPP = ((queryULLAT - queryLRLAT)) / requestedHeight;

        //the number of levels to go down to achieve the required DPP
        //currentDPP is initialized to the DPP of the biggest square
        int depth = 1;
        Double currentDPP = (ROOT_LRLON - ROOT_ULLON) / TILE_SIZE;
        Double currentLatDPP = (ROOT_ULLAT - ROOT_LRLAT) / TILE_SIZE;
        while (currentDPP > queryLDPP && depth != 8) {
            currentDPP /= 2;
            currentLatDPP /= 2;
            depth++;
        }
        depth--;

        //change in lat/lon per picture moved
        Double deltaLAT = currentLatDPP * 256;
        Double deltaLON = currentDPP * 256;

        String topLeft = getPictureName(queryULLAT, queryULLON, depth);
        HashMap<String, Double> topLeftMap = getCoordinates(topLeft + ".png");

        //the coordinates of the top left image
        Double ullon = topLeftMap.get("ULLON");
        Double lrlon = topLeftMap.get("LRLON");
        Double ullat = topLeftMap.get("ULLAT");
        Double lrlat = topLeftMap.get("LRLAT");

        Integer width = 0;
        Integer height = 0;
        while (ullon < queryLRLON && ullon + deltaLAT <= ROOT_LRLON) {    //moves across the top row
            width++;
            ullon += deltaLON;
        }
        while (ullat > queryLRLAT) {    //moves down
            height++;
            ullat -= deltaLAT;
        }

        //reset to the top left image
        ullon = topLeftMap.get("ULLON");
        ullat = topLeftMap.get("ULLAT");

        String[][] pictureNames = new String[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                pictureNames[i][j] = getPictureName(ullat, ullon, depth) + ".png";
                ullon += deltaLON + 0.00000000000001;
            }
            ullon = topLeftMap.get("ULLON");
            ullat -= deltaLAT + 0.00000000000001;
        }
        //maps of the final image coords
        Map<String, Double> ulmap = getCoordinates(pictureNames[0][0]);
        Map<String, Double> lrmap =
                getCoordinates(pictureNames[pictureNames.length - 1][pictureNames[0].length - 1]);

        //The top left and right of the final image
        Double finalULLON = ulmap.get("ULLON");
        Double finalULLAT = ulmap.get("ULLAT");
        Double finalLRLON = lrmap.get("LRLON");
        Double finalLRLAT = lrmap.get("LRLAT");

        rasteredImageParams.put("query_success", true);
        rasteredImageParams.put("raster_ul_lon", finalULLON);
        rasteredImageParams.put("raster_ul_lat", finalULLAT);
        rasteredImageParams.put("raster_lr_lon", finalLRLON);
        rasteredImageParams.put("raster_lr_lat", finalLRLAT);
        rasteredImageParams.put("raster_width", width * 256);
        rasteredImageParams.put("raster_height", height * 256);
        rasteredImageParams.put("depth", depth);

        BufferedImage image = drawImages(pictureNames);
        return image;
    }

    public static BufferedImage drawImages(String[][] names) {
        BufferedImage bigImage = null;
        BufferedImage row = null;
        bigImage = new BufferedImage(256 * names[0].length, 256 * names.length,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bigImage.createGraphics();

        for (int i = 0; i < names.length; i++) {
            try {
                if (tree.getNode(names[i][0]).image == null) {
                    //System.out.println("Reading file " + names[i][0]);
                    row = ImageIO.read(new File(IMG_ROOT + names[i][0])); //the start of that row
                    tree.getNode(names[i][0]).image = row;
                } else {
                    //System.out.println("I have that image in memory!");
                    row = tree.getNode(names[i][0]).image;
                }
            } catch (IOException e) {
                System.out.println("Could not read row image");
            }
            for (int j = 1; j < names[i].length; j++) {     //draws the rows
                if (!(names[i][j].equals(""))) {
                    try {
                        BufferedImage temp = null;
                        if ((tree.getNode(names[i][j]).image == null)) {
                            //System.out.println("Reading file: " + IMG_ROOT + names[i][j]);
                            temp = ImageIO.read(new File(IMG_ROOT + names[i][j]));
                            tree.getNode(names[i][j]).image = temp;
                        } else {
                            //System.out.println("I have that image in memory!");
                            temp = tree.getNode(names[i][j]).image;
                        }
                        Color oldColor = graphics.getColor();
                        graphics.setColor(oldColor);
                        graphics.drawImage(temp, j * 256, i * 256, null);
                    } catch (IOException e) {
                        System.out.println("Could not read the temp file");
                    }
                }
            }
            if (bigImage == null) { //if it's the first row
                bigImage = row;
            } else {        //adds the rows to the image

                Color oldColor = graphics.getColor();
                //draw image
                graphics.setColor(oldColor);
                graphics.drawImage(row, 0, 256 * i, null);

            }
        }
        return bigImage;
    }

    /**
     * Gets the string name of the picture at ullon, ullat.
     * Checks which quadrant the coordinates are in based on the frame.
     * @param ullat is the upper left latitude
     * @param ullon is the upper left longitude
     * @param depth is the depth.
     * @return
     */
    public static String getPictureName(Double ullat, Double ullon, int depth) {

        QNode node = tree.head;
        String name = getPictureNameHelper(node, new Point(ullon, ullat), 0, depth);
        return name;
    }
    public static String getPictureNameHelper(QNode node, Point point, int now, int max) {
        String rtn = "";
        if (now < max) {
            if (node.one.contains(point)) {
                rtn =  "1" + getPictureNameHelper(node.one, point, now + 1, max);
            } else if (node.two.contains(point)) {
                rtn =  "2" + getPictureNameHelper(node.two, point, now + 1, max);
            } else if (node.three.contains(point)) {
                rtn =  "3" + getPictureNameHelper(node.three, point, now + 1, max);
            } else if (node.four.contains(point)) {
                rtn = "4" + getPictureNameHelper(node.four, point, now + 1, max);
            } else if (node.one.set.upperLeft.x > point.x) {
                rtn = "1" + getPictureNameHelper(node.one, point, now + 1, max);
            }
        }
        return rtn;
    }
    //gets the coordinates of a picture <name>
    public static HashMap<String, Double> getCoordinates(String name) {
        HashMap<String, Double> map = new HashMap<>();

        PointSet set = tree.getNode(name).set;
        map.put("LRLON", set.lowerRight.x);
        map.put("LRLAT", set.lowerRight.y);
        map.put("ULLON", set.upperLeft.x);
        map.put("ULLAT", set.upperLeft.y);
        return map;
    }

    /**
     * Gets a node.
     * @param depth is the level at which to search.
     * @param ullon is the upper left longitude
     * @param ullat is the upper left latitude
     * @param left tells whether to provide coverage on the left side or right side of image
     * @return the QTreeNode
     */

    /**
     * Searches for the shortest route satisfying the input request parameters, and returns a
     * <code>List</code> of the route's node ids. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean distance between two points
     * (lon1, lat1) and (lon2, lat2).
     * If <code>im</code> is not null, draw the route onto the image by drawing lines in between
     * adjacent points in the route. The lines should be drawn using ROUTE_STROKE_COLOR,
     * ROUTE_STROKE_WIDTH_PX, BasicStroke.CAP_ROUND and BasicStroke.JOIN_ROUND.
     * @param routeParams Params collected from the API call. Members are as
     *                    described in REQUIREDROUTEREQUESTPARAMS.
     * @param rasterImageParams parameters returned from the image rastering.
     * @param im The rastered map image to be drawn on.
     * @return A List of node ids from the start of the route to the end.
     */
    public static List<Long> findAndDrawRoute(Map<String, Double> routeParams,
            Map<String, Object> rasterImageParams,
            BufferedImage im) {
        Point start = new Point(routeParams.get("start_lon"), routeParams.get("start_lat"));
        Point end = new Point(routeParams.get("end_lon"), routeParams.get("end_lat"));
        Set p = graphDB.NodeDB.keySet();
        GraphNode nearStart = null;
        GraphNode nearEnd = null;
        Double min1 = Double.MAX_VALUE;
        Double min2 = Double.MAX_VALUE;
        for (Object x : p) {
            if (min1 > calculatedistance(graphDB.NodeDB.get(x).getP(), start)) {
                nearStart = graphDB.NodeDB.get(x);
                min1 = calculatedistance(graphDB.NodeDB.get(x).getP(), start);
            }
            if (min2 > calculatedistance(graphDB.NodeDB.get(x).getP(), end)) {
                nearEnd = graphDB.NodeDB.get(x);
                min2 = calculatedistance(graphDB.NodeDB.get(x).getP(), end);
            }
        }
        ArrayList<Long> list = graphDB.shortestPath(nearStart, nearEnd);
        if (im != null) {
            im = drawRouteHelper(im, rasterImageParams, list);
        }
        return list;
    }
    static double calculatedistance(Point from, Point to) {
        return sqrt((from.x - to.x) * (from.x - to.x) + (from.y - to.y) * (from.y - to.y));
    }



    /**
     * Draws a straight line from start to end on preImage and returns the image.
     * @param preImage
     * @return the new image with route drawn.
     */
    public static BufferedImage drawRouteHelper(BufferedImage preImage, Map<String,
            Object> rasterImageParams, List<Long> points) {
        HashMap<Long, GraphNode> nodeDB = graphDB.NodeDB;
        int max = points.size();
        int current = 0;

        Graphics2D g = preImage.createGraphics();
        BasicStroke stroke = new BasicStroke(MapServer.ROUTE_STROKE_WIDTH_PX,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setPaint(ROUTE_STROKE_COLOR);
        g.setStroke(stroke);

        Point prev;
        Point curr;
        int pxCoord = 0;
        int pyCoord = 0;
        int cxCoord = 0;
        int cyCoord = 0;
        for (Long pointName: points) {
            if (current == 0) {         //first element
                prev = nodeDB.get(pointName).getP();
                pxCoord = lonToX(rasterImageParams, prev);
                pyCoord = latToy(rasterImageParams, prev);
            } else {     //middle elements
                curr = nodeDB.get(pointName).getP();
                cxCoord = lonToX(rasterImageParams, curr);
                cyCoord = latToy(rasterImageParams, curr);

                g.drawLine(pxCoord, pyCoord, cxCoord, cyCoord);
                //System.out.println("Drawing from (" + pxCoord + ", " + pyCoord + ")
                // to (" + cxCoord + ", " + cyCoord + ")");
                prev = curr;
                pxCoord = cxCoord;
                pyCoord = cyCoord;
            }
            current++;
        }

        g.dispose();

        return preImage;
    }

    /**
     * Gets a point's X coordinate given rasterImageParams.
     * @param rasterImageParams
     * @param p
     * @return the X coordinate of that point.
     * rasteredImageParams.put("raster_ul_lon", finalULLON);
    rasteredImageParams.put("raster_ul_lat", finalULLAT);
    rasteredImageParams.put("raster_lr_lon", finalLRLON);
    rasteredImageParams.put("raster_lr_lat", finalLRLAT);
    rasteredImageParams.put("raster_width", width * 256);
    rasteredImageParams.put("raster_height", height * 256);
    rasteredImageParams.put("depth", depth);
     */
    private static int lonToX(Map<String, Object> rasterImageParams, Point p) {
        int x = 0;
        Double ullon = (Double) rasterImageParams.get("raster_ul_lon");
        Double lrlon = (Double) rasterImageParams.get("raster_lr_lon");
        Integer pixelWidth = (Integer) rasterImageParams.get("raster_width");

        Double deltaLon = p.x - ullon;

        Double lonProportion = (lrlon - ullon) / deltaLon;

        x = (int) (pixelWidth / lonProportion);

        return x;
    }

    private static int latToy(Map<String, Object> rasterImageParams, Point p) {
        int y = 0;
        Double ullat = (Double) rasterImageParams.get("raster_ul_lat");
        Double lrlat = (Double) rasterImageParams.get("raster_lr_lat");
        Integer pixelHeight = (Integer) rasterImageParams.get("raster_height");

        Double deltaLat = ullat - p.y;

        Double latProportion = (ullat - lrlat) / deltaLat;

        y = (int) (pixelHeight / latProportion);

        return y;
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        List<String> list = trie.getCompletions(cleanString(prefix));
        List<String> origList = new LinkedList<>();
        for (String word: list) {
            origList.add(cleanedToOriginal.get(word));
        }
        return origList;
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    //TODO FIX FOR CLEANED NAMES
    public static List<Map<String, Object>> getLocations(String locationName) {
        List<String> names = getLocationsByPrefix(locationName);

        List<Map<String, Object>> list = new LinkedList<>();
        for (String name: names) {
            Point p = graphDB.nameToPoint.get(name);
            Long id = graphDB.findID.get(p);

            HashMap<String, Object> map = new HashMap<>();
            map.put("lat", p.y);
            map.put("lon", p.x);
            map.put("name", name);
            map.put("id", id);


            list.add(map);
        }
        return list;
    }

    /**
     * Cleans a string.
     * @param toClean
     * @return the cleaned string.
     */
    private static String cleanString(String toClean) {
        String clean = "";
        char[] arr = toClean.toLowerCase().toCharArray();
        for (char c: arr) {
            if (c >= 97 && c <= 122 || c == ' ') {
                clean += c + "";
            }
        }
        return clean.trim();
    }
}
