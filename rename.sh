
# Update POMs - Remove -SNAPSHOT
java -cp assets/utilities/release.jar com.bluenimble.platform.release.Replace pom.xml "<version>2.56.0-SNAPSHOT</version><!--bn.version-->" "<version>2.55.0</version><!--bn.version-->"
