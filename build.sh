

Release=$1

if [ -z "$Release" ]
then
  echo "Set Release eg. 1.2 " >&2
  exit 1
fi

# Build Server and CLI

# clear
rm -fr bluenimble-$Release.0
rm -fr bluenimble-cli-$Release.0
rm -fr bluenimble-$Release.0-bin.tar.gz
rm -fr bluenimble-cli-$Release.0-bin.tar.gz

mvn clean install

cd build

tar -czf bluenimble-$Release.0-bin.tar.gz bluenimble-$Release.0
tar -czf bluenimble-cli-$Release.0-bin.tar.gz bluenimble-cli-$Release.0
