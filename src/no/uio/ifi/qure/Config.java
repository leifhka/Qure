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

    public BintreeFactory bf = new BintreeFactory();
    public int maxIterDepth = 40;
    public int blockMemberCount = 30;
    public int representationDepth = 15;
    public int overlapsArity = 3;
    public int dim = 2;

    public Config(String table, int representationDepth, int overlapsArity) {

        this.overlapsArity = overlapsArity;
        this.representationDepth = representationDepth;

        rawGeoTableName = table;
        geoTableName = "geo." + rawGeoTableName;

        rawBTTableName = rawGeoTableName + "_d" + representationDepth + "_k" + overlapsArity;
        btTableName = "qure." + rawBTTableName;

        geoQuerySelectFromStr = "select " + uriColumn + ", " + geoColumn + " from " + geoTableName;
        geoQueryStr = geoQuerySelectFromStr + ";";
    }

    public Predicate<SpaceToBintree.Node> atMaxDepth = new Predicate<SpaceToBintree.Node>() {
        public boolean test(SpaceToBintree.Node node) {
            int d = node.getBlock().depth();
            return d >= maxIterDepth ||
                   (d >= representationDepth && node.getOverlappingURIs().size() <= blockMemberCount);
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
