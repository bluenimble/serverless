#!/bin/sh
#
# Copyright (c) BlueNimble, Inc. (https://www.bluenimble.com)
#

echo ""
echo "     ______ _            _   _ _           _     _      "
echo "     | ___ \\ |          | \\ | (_)         | |   | |     "
echo "     | |_/ / |_   _  ___|  \\| |_ _ __ ___ | |__ | | ___ "
echo "     | ___ \\ | | | |/ _ \\ . \` | | '_ \` _ \\| '_ \\| |/ _ \\"
echo "     | |_/ / | |_| |  __/ |\\  | | | | | | | |_) | |  __/"
echo "     \\____/|_|\\__,_|\\___\\_| \\_/_|_| |_| |_|_.__/|_|\\___|"
                                                   
# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set BN_HOME if not already set
[ -f "$BN_HOME"/bnb.sh ] || BN_HOME=`cd "$PRGDIR" ; pwd`
export BN_HOME
cd "$BN_HOME"

export JAVA_OPTS

# Set JavaHome if it exists
if [ -f "${JAVA_HOME}/bin/java" ]; then 
   JAVA=${JAVA_HOME}/bin/java
else
   JAVA=java
fi
export JAVA

JAVA_OPTS_SCRIPT="-Xms60m -Djna.nosys=true -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF8 -DBN_HOME=$BN_HOME"

BN_PID=$BN_HOME/bnb.pid

if [ -f "$BN_PID" ]; then
    echo "removing old pid file $BN_PID"
    rm "$BN_PID"
fi

# TO DEBUG BlueNimble RUN WITH THESE OPTIONS:
# -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044
# AND ATTACH TO THE CURRENT HOST, PORT 1044

# BlueNimble memory options, default to 512 of heap.

if [ -z "$BN_OPTS_MEMORY" ] ; then
    BN_OPTS_MEMORY="-Xmx512m"
fi
# BN MAXIMUM DISKCACHE IN MB, EXAMPLE, ENTER -Dstorage.diskCache.bufferSize=8192 FOR 8GB
MAXDISKCACHE=""

echo $$ > $BN_PID

exec "$JAVA" $JAVA_OPTS $BN_OPTS_MEMORY $JAVA_OPTS_SCRIPT $MAXDISKCACHE \
    -cp "$BN_HOME/boot/bluenimble-jvm-sdk-[version].jar:$BN_HOME/boot/bluenimble-cli-boot-[version].jar" \
    $* com.bluenimble.platform.icli.mgm.boot.BnMgmICli
