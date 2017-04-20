
JAVAC_CP = "lib/*"
JAVA_CP = "lib/*:bin/"

src_prefix = src/
bin_prefix = bin/
package_name = no/uio/ifi/qure/

java = java -Xmx7600m -cp $(JAVA_CP)
#javac = javac -Xlint:deprecation -Xlint:unchecked -cp $(JAVAC_CP)
javac = javac -Xlint:all -cp $(JAVAC_CP)

CLASSES = \
        Config.java \
        Qure.java \
        bintree/Bintree.java \
        bintree/Block.java \
        dataprovider/DBDataProvider.java \
        dataprovider/RawDataProvider.java \
        dataprovider/UnparsedIterator.java \
        dataprovider/UnparsedSpace.java \
        traversal/EvenSplit.java \
        traversal/Relationship.java \
        traversal/RelationshipGraph.java \
        traversal/Representation.java \
        traversal/SID.java \
        traversal/SpaceToBintree.java \
        traversal/TreeNode.java \
        space/GeometryProvider.java \
        space/GeometrySpace.java \
        relation/Relation.java \
        relation/Overlaps.java \
        relation/PartOf.java \
        relation/Before.java \
        relation/AtomicRelation.java \
        relation/RelationSet.java \
        space/Space.java \
        space/SpaceProvider.java \
        space/TimeProvider.java \
        space/TimeSpace.java  \
        space/Intersection.java \
        util/Progress.java \
        util/Reporter.java \
        util/Pair.java \
        util/Utils.java

source_files = $(addprefix $(src_prefix), $(addprefix $(package_name), $(CLASSES)))

lib/jts.jar lib/postgresql-9.4.1208.jar:
	tar axvf lib.tar.gz

libs : lib/jts.jar lib/postgresql-9.4.1208.jar

bin :
	mkdir -p bin

compile : libs bin $(source_files)
	@$(javac) $(source_files) -d bin/

run: 
	date; $(java) no.uio.ifi.qure.Qure; date
