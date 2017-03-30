
JAVAC_CP = "lib/*"
JAVA_CP = "lib/*:bin/"

src_prefix = src/
bin_prefix = bin/
package_name = no/uio/ifi/qure/

java = java -Xmx7600m -cp $(JAVA_CP)
javac = javac -Xlint:deprecation -Xlint:unchecked -cp $(JAVAC_CP)

CLASSES = \
        bintree/Bintree.java \
        bintree/Block.java \
        Config.java \
        dataprovider/DBDataProvider.java \
        traversal/EvenSplit.java \
        space/GeometryProvider.java \
        space/GeometrySpace.java \
        traversal/Intersection.java \
        util/Progress.java \
        Qure.java \
        dataprovider/RawDataProvider.java \
        relation/Relation.java \
        traversal/Relationship.java \
        traversal/RelationshipGraph.java \
        traversal/Representation.java \
        util/Reporter.java \
        traversal/SID.java \
        space/Space.java \
        space/SpaceProvider.java \
        traversal/SpaceToBintree.java \
        space/TimeProvider.java \
        space/TimeSpace.java  \
        traversal/TreeNode.java \
        dataprovider/UnparsedIterator.java \
        dataprovider/UnparsedSpace.java \
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
