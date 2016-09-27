package no.uio.ifi.qure;

import com.vividsolutions.jts.geom.PrecisionModel;
import java.util.function.Predicate;

public class Config {
    public String rawGeoTableName;
    public String geoTableName;
    public String rawBTTableName;
    public String btTableName;
    public String geoQuerySelectFromStr;
    public String geoQueryStr;

    public int maxIterDepth = 20;
    public int blockMemberCount = 50;
    public int representationDepth = 20;
    public int overlapsArity = 3;
    public int dim = 2;

    public int maxDiff = 25;
    public int maxSplit = 10;
 
    public Config(String table, int representationDepth, int overlapsArity) {

        this.overlapsArity = overlapsArity;
        this.representationDepth = representationDepth;
        this.maxIterDepth = representationDepth;

        rawGeoTableName = table;
        geoTableName = "geo." + rawGeoTableName;

        rawBTTableName = rawGeoTableName + "_d" + representationDepth + "_k" + overlapsArity;
        btTableName = "qure." + rawBTTableName;

        geoQuerySelectFromStr = "select " + uriColumn + ", " + geoColumn + " from " + geoTableName;
        geoQueryStr = geoQuerySelectFromStr + ";";
    }

    public Config(String table, String suff, int representationDepth, int overlapsArity) {

        this.overlapsArity = overlapsArity;
        this.representationDepth = representationDepth;
        this.maxIterDepth = representationDepth;

        rawGeoTableName = table;
        geoTableName = "geo." + rawGeoTableName;

        rawBTTableName = rawGeoTableName + "_d" + representationDepth + "_k" + overlapsArity + "_" + suff;
        btTableName = "qure." + rawBTTableName;

        geoQuerySelectFromStr = "select " + uriColumn + ", " + geoColumn + " from " + geoTableName;
        geoQueryStr = geoQuerySelectFromStr + ";";
    }

    public Predicate<TreeNode> atMaxDepth = new Predicate<TreeNode>() {
        public boolean test(TreeNode node) {
            int d = node.getBlock().depth();
            return d >= maxIterDepth ||
                   (node.getOverlappingURIs().size() <= blockMemberCount);
        }
    };

    public Predicate<TreeNode> atRepDepth = new Predicate<TreeNode>() {
        public boolean test(TreeNode node) {
            return node.isGraph();
        }
    };

    public String dbName = "test";
    public String dbPWD = "test";
    public String dbUsername = "leifhka";
    public String uriColumn = "gid";
    public String blockColumn = "block";
    public String geoColumn = "geom";
    public String universeTable = "qure.universes";
    public boolean convertUriToInt = true;

    public String connectionStr = "jdbc:postgresql://localhost/" + dbName + "?user=" +
                                  dbUsername + "&password=" + dbPWD;


    public boolean verbose = true;
    public int percentStep = 1;
    public boolean writeBintreesToDB = true;

    public PrecisionModel geometryFactoryPrecision = new PrecisionModel();
    public PrecisionModel geometryPrecision = new PrecisionModel(Math.pow(10, 10));

}
