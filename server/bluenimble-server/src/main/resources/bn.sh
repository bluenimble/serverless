#!/bin/sh
#
# Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)
#

echo       ""
echo       "BlueNimble Platform"
echo       "Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)"
echo       ""

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
[ -f "$BN_HOME"/bn.sh ] || BN_HOME=`cd "$PRGDIR" ; pwd`
export BN_HOME
cd "$BN_HOME"

# Raspberry Pi check (Java VM does not run with -server argument on ARMv6)
if [ `uname -m` != "armv6l" ]; then
  JAVA_OPTS="$JAVA_OPTS -server "
fi
export JAVA_OPTS

# Set JavaHome if it exists
if [ -f "${JAVA_HOME}/bin/java" ]; then 
   JAVA=${JAVA_HOME}/bin/java
else
   JAVA=java
fi
export JAVA

JAVA_OPTS_SCRIPT="-Djna.nosys=true -XX:+HeapDumpOnOutOfMemoryError -XX:MaxDirectMemorySize=7879m -Djava.awt.headless=true -Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true -DBN_HOME=$BN_HOME"

BN_PID=$BN_HOME/bn.pid

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

if [ -z "$BN_RUNTIME" ] ; then
	BN_RUNTIME="/data/bluenimble/runtime"
fi

if [ -z "$BN_TENANT" ] ; then
	BN_TENANT="/data/bluenimble/tenant"
fi

echo $$ > $BN_PID

echo "BlueNimble Runtime: $BN_RUNTIME"

echo "BlueNimble Tenant: $BN_TENANT"

exec "$JAVA" $JAVA_OPTS $BN_OPTS_MEMORY $JAVA_OPTS_SCRIPT \
    -cp "boot/bluenimble-jvm-sdk-[version].jar:boot/bluenimble-server-boot-[version].jar" \
    $* com.bluenimble.platform.server.boot.BlueNimble $BN_RUNTIME $BN_TENANT