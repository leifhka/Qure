package no.uio.ifi.qure;

import com.vividsolutions.jts.geom.PrecisionModel;
import java.util.function.Predicate;

import no.uio.ifi.qure.space.*;
import no.uio.ifi.qure.traversal.*;
import no.uio.ifi.qure.relation.*;

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
	public int blockMemberCount;
	public int numThreads = Runtime.getRuntime().availableProcessors()*4; //32;
	public int dim = 2;

	//public int maxDiff = 25;
	public double minRatio = 0.9;
	public int maxSplits = 7;

	public RelationSet relationSet;
 
	public Config(String table, String suff, int representationDepth, 
				  int blockMemberCount, int maxSplits, RelationSet relationSet) {

		this.maxIterDepth = representationDepth;
		this.blockMemberCount = blockMemberCount;
		this.maxSplits = maxSplits;
		this.relationSet = relationSet;

		rawGeoTableName = table;
		geoTableName = "geo." + rawGeoTableName;
		//geoTableName = rawGeoTableName;

		rawBTTableName = rawGeoTableName + "_d" + representationDepth + "_" + relationSet.getName() + "_bc" + blockMemberCount + "_ms" + maxSplits + "_" + suff;
		schemaName = "qure";
		btTableName = schemaName + "." + rawBTTableName;

		splitTable = "split." + rawBTTableName;
		splitQuery = "SELECT * FROM " + splitTable + ";";

		geoQuerySelectFromStr = "SELECT " + uriColumn + ", " + geoColumn + " FROM " + geoTableName;
		geoQueryStr = geoQuerySelectFromStr + ";";

	}

	public Predicate<TreeNode> atMaxDepth = new Predicate<TreeNode>() {
		public boolean test(TreeNode node) {
			int d = node.getBlock().depth();
			return d >= maxIterDepth ||
			       (node.getOverlappingURIs().size() <= blockMemberCount);
		}
	};

	public int blockSize = 63;
	public boolean compactBlocks = false;
	public String dbName = "test";
	public String dbPWD = "test";
	public String dbUsername = "leifhka";
	public String uriColumn = "gid";
	public String blockColumn = "block";
	public int limit = 1000000;
	public String geoColumn = "geom";
	//public String geoColumn = "starttime, stoptime";
	public String universeTable = "qure.universes";
	public boolean convertUriToInt = true;

	public String jdbcDriver = "org.postgresql.Driver";
	public String connectionStr = "jdbc:postgresql://localhost/" + dbName + "?user=" +
	                              dbUsername + "&password=" + dbPWD;


	public boolean verbose = true;
	public int percentStep = 1;
	public boolean writeBintreesToDB = true;

	public PrecisionModel geometryFactoryPrecision = new PrecisionModel();
	public PrecisionModel geometryPrecision = new PrecisionModel(Math.pow(10, 7));

}
