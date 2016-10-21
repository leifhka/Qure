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
    public String splitTable;
    public String splitQuery;
    public String schemaName;

    public int maxIterDepth;
    public int overlapsArity;
    public int numThreads = 8;
    public int blockMemberCount = 30;
    public int dim = 2;

    public int maxDiff = 25;
    public int maxSplits = 10;
 
    public Config(String table, int representationDepth, int overlapsArity) {

        this.overlapsArity = overlapsArity;
        this.maxIterDepth = representationDepth;

        rawGeoTableName = table;
        geoTableName = "geo." + rawGeoTableName;

        rawBTTableName = rawGeoTableName + "_d" + representationDepth + "_k" + overlapsArity;
        schemaName = "qure";
        btTableName = schemaName + "." + rawBTTableName;

        splitTable = "split." + rawBTTableName;
        splitQuery = "SELECT * FROM " + splitTable + ";";

        geoQuerySelectFromStr = "select " + uriColumn + ", " + geoColumn + " from " + geoTableName;
        geoQueryStr = geoQuerySelectFromStr + ";";
    }

    public Config(String table, String suff, int representationDepth, int overlapsArity) {

        this.overlapsArity = overlapsArity;
        this.maxIterDepth = representationDepth;

        rawGeoTableName = table;
        geoTableName = "geo." + rawGeoTableName;

        rawBTTableName = rawGeoTableName + "_d" + representationDepth + "_k" + overlapsArity + "_bc" + blockMemberCount + "_" + suff;
        schemaName = "qure";
        btTableName = schemaName + "." + rawBTTableName;

        splitTable = "split." + rawBTTableName;
        splitQuery = "SELECT * FROM " + splitTable + ";";

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
