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

    public int insertTotal = 100;
    public int insertOffset = 10;
    public String insertTable;

    public Config() {
        rawGeoTableName = "dallas";
        geoTableName = "geo." + rawGeoTableName;
        rawBTTableName = rawGeoTableName + "_d" + representationDepth + "_k" + overlapsArity+ "";
        btTableName = "qure." + rawBTTableName;
        geoQuerySelectFromStr = "select " + uriColumn + ", " + geoColumn + " from " + geoTableName;
        geoQueryStr = geoQuerySelectFromStr + " " + limitStr + ";";
        insertTable = "ins." + rawGeoTableName + "_100";
    }

    public Config(String table, String suff, int representationDepth, int overlapsArity) {
        this.overlapsArity = overlapsArity;
        this.representationDepth = representationDepth;
        rawGeoTableName = table;
        geoTableName = "geo." + rawGeoTableName;
        rawBTTableName = rawGeoTableName + "_d" + representationDepth + "_k" + overlapsArity + "_" + suff;
        btTableName = "qure." + rawBTTableName;
        geoQuerySelectFromStr = "select " + uriColumn + ", " + geoColumn + " from " + geoTableName;
        geoQueryStr = geoQuerySelectFromStr + " " + limitStr + ";";
        insertTable = "ins." + table + "_500";
    }

    public Predicate<SpaceToBintree.Node> atMaxDepth = new Predicate<SpaceToBintree.Node>() {
        public boolean test(SpaceToBintree.Node node) {
            int d = node.getBlock().depth();
            //if (d < representationDepth)
            //    return false;
            //if (d >= maxIterDepth || node.getSpaceProvider().keySet().size() <= blockMemberCount)
            //    return true;

            //Space[] lr = node.getSpaceProvider().getUniverse().split(node.split());
            //int badness = 0;
            //for (Integer uri : node.getSpaceProvider().keySet()) {
            //    Space s = node.getSpaceProvider().get(uri);
            //    Relation lRel = lr[0].relate(s);
            //    Relation rRel = lr[1].relate(s);

            //    if (!lRel.isCoveredBy() && lRel.isIntersects() && !rRel.isCoveredBy() && rRel.isIntersects())
            //         badness++;
            //}

            //int n = node.getSpaceProvider().keySet().size();
            //return (((double) badness)/n) <= 0.2;

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
    public int limit = 0; //500000;
    private String limitStr = (limit == 0) ? "" : "limit " + limit;

    public String connectionStr = "jdbc:postgresql://localhost/" + dbName + "?user=" +
                                  dbUsername + "&password=" + dbPWD;


    public boolean verbose = true;
    public int percentStep = 1;
    public boolean writeBintreesToDB = true;

    public PrecisionModel geometryFactoryPrecision = new PrecisionModel();
    public PrecisionModel geometryPrecision = new PrecisionModel(Math.pow(10, 10));

}
