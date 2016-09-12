
JAVAC_CP = "lib/*"
JAVA_CP = "lib/*:bin/"

java = java -Xmx7600m -cp $(JAVA_CP)
javac = javac -Xlint:deprecation -Xlint:unchecked -cp $(JAVAC_CP)

lib/%.jar:
	tar axvf lib.tar.xz

bin/%.class: src/no/uio/ifi/qure/%.java
	@$(javac) src/no/uio/ifi/qure/*.java -d bin/

compile: bin/*.class

run: 
	date; $(java) no.uio.ifi.qure.Qure; date
