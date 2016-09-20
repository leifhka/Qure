
JAVAC_CP = "lib/*"
JAVA_CP = "lib/*:bin/"

src_prefix = src/
bin_prefix = bin/
package_name = no/uio/ifi/qure/

java = java -Xmx7600m -cp $(JAVA_CP)
javac = javac -Xlint:deprecation -Xlint:unchecked -cp $(JAVAC_CP)

CLASSES = \
        BintreeFactory.java \
        Bintree.java \
        Block.java \
        Config.java \
        GeometryProvider.java \
        GeometryRelation.java \
        GeometrySpace.java \
        Intersection.java \
        Progress.java \
        Qure.java \
        Relation.java \
        RelationshipGraph.java \
        Representation.java \
        Space.java \
        SpaceProvider.java \
        SpaceToBintree.java

source_files = $(addprefix $(src_prefix), $(addprefix $(package_name), $(CLASSES)))

lib/jts.jar lib/postgresql-9.4.1208.jar:
	tar axvf lib.tar.gz

libs : lib/jts.jar lib/postgresql-9.4.1208.jar

bin :
	mkdir -p bin

compile : libs bin $(source_files)
	@$(javac) src/no/uio/ifi/qure/*.java -d bin/

run: 
	date; $(java) no.uio.ifi.qure.Qure; date