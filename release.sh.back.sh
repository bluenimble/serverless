

CurrentRelease=$1
Release=$2
NextRelease=$3

if [ -z "$CurrentRelease" ]
then
  echo "Set Current Release eg. 1.1 " >&2
  exit 1
fi

if [ -z "$Release" ]
then
  echo "Set Release eg. 1.2 " >&2
  exit 1
fi

if [ -z "$NextRelease" ]
then
  echo "Set Next Release eg. 1.3 " >&2
  exit 1
fi

# Update POMs - Remove -SNAPSHOT
java -cp assets/utilities/release.jar com.bluenimble.platform.release.Replace pom.xml "<version>$Release.0-SNAPSHOT</version><!--bn.version-->" "<version>$Release.0</version><!--bn.version-->"

# Update README
java -cp assets/utilities/release.jar com.bluenimble.platform.release.Replace README.md "$CurrentRelease.0" "$Release.0"

git add .
git commit -m "Update POMs for Release $Release.0"
git push origin master

# Build Server and CLI
mvn clean deploy
mvn clean install

# Build Broker
cd tools/bluenimble-broker
mvn clean install

cd ../..

# Create branches
git checkout -b $Release
git tag -a v$Release.0 -m "Release V$Release.0"
git push -u origin $Release
git push origin v$Release.0

# Update POMs - Replace $Release by $NextRelease-SNAPSHOT
java -cp assets/utilities/release.jar com.bluenimble.platform.release.Replace pom.xml "<version>$Release.0</version><!--bn.version-->" "<version>$NextRelease.0-SNAPSHOT</version><!--bn.version-->"

git checkout master
git add .
git commit -m "Update POMs for next snapshot $NextRelease.0-SNAPSHOT"
git push origin master

cd build
# remove netty-socketio
rm bluenimble-broker-$Release.0/lib/netty-socketio-1.7.17.jar

tar -czf bluenimble-$Release.0-bin.tar.gz bluenimble-$Release.0
tar -czf bluenimble-cli-$Release.0-bin.tar.gz bluenimble-cli-$Release.0
tar -czf bluenimble-broker-$Release.0-bin.tar.gz bluenimble-broker-$Release.0

zip -r bluenimble-$Release.0-bin.zip bluenimble-$Release.0
zip -r bluenimble-cli-$Release.0-bin.zip bluenimble-cli-$Release.0
zip -r bluenimble-broker-$Release.0-bin.zip bluenimble-broker-$Release.0