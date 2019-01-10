#!/bin/sh
#
# Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)
#

echo ""
echo "     ______ _            _   _ _           _     _      "
echo "     | ___ \\ |          | \\ | (_)         | |   | |     "
echo "     | |_/ / |_   _  ___|  \\| |_ _ __ ___ | |__ | | ___ "
echo "     | ___ \\ | | | |/ _ \\ . \` | | '_ \` _ \\| '_ \\| |/ _ \\"
echo "     | |_/ / | |_| |  __/ |\\  | | | | | | | |_) | |  __/"
echo "     \\____/|_|\\__,_|\\___\\_| \\_/_|_| |_| |_|_.__/|_|\\___|"
                                                   
echo       ""
echo       "BlueNimble Serverless Platform - Server Node - Version [version]"
echo       "Copyright (c) BlueNimble, Inc. (https://www.bluenimble.com)"
echo       ""

# Set BNB_RUNTIME to an empty directory outside the installation directory. Recommended for clean upgrades!  
#                 Default to . (install directory)
# Set BNB_TENANT  to an empty directory outside the installation directory if you are going to run multiple bluenimble nodes 
#                 in the same machine using the same binaries. Default to none 
BNB_RUNTIME=. 
BNB_TENANT=none

echo ""

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

# Only set BNB_HOME if not already set
[ -f "$BNB_HOME"/bnb.sh ] || BNB_HOME=`cd "$PRGDIR" ; pwd`
export BNB_HOME
cd "$BNB_HOME"

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

JAVA_OPTS_SCRIPT="-Djna.nosys=true -XX:+HeapDumpOnOutOfMemoryError -XX:MaxDirectMemorySize=7879m -Djava.awt.headless=true -Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true -DBNB_HOME=$BNB_HOME"

BNB_PID=$BNB_HOME/bnb.pid

if [ -f "$BNB_PID" ]; then
    rm "$BNB_PID"
fi

# TO DEBUG BlueNimble RUN WITH THESE OPTIONS:
# -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044
# AND ATTACH TO THE CURRENT HOST, PORT 1044

# BlueNimble memory options, default to 512 of heap.

if [ -z "$BNB_OPTS_MEMORY" ] ; then
    BNB_OPTS_MEMORY="-Xmx512m"
fi

echo $$ > $BNB_PID

exec "$JAVA" $JAVA_OPTS $BNB_OPTS_MEMORY $JAVA_OPTS_SCRIPT \
    -cp "boot/bluenimble-jvm-sdk-[version].jar:boot/bluenimble-server-boot-[version].jar" \
    $* com.bluenimble.platform.server.boot.BlueNimble $BNB_RUNTIME $BNB_TENANT