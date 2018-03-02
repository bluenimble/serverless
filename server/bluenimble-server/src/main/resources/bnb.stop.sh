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
[ -f "$BN_HOME"/bnb.sh ] || BN_HOME=`cd "$PRGDIR" ; pwd`
export BN_HOME
cd "$BN_HOME"

BN_PID=$BN_HOME/bnb.pid

if [ -f "$BN_PID" ]
then
    echo "Stopping BlueNimble Platform"
    kill -15 $(cat "$BN_PID")
    
    while [ $(kill -0 "$BN_PID") ]; do
	  sleep 1
	done
	
	echo "BlueNimble went down!"
    rm "$BN_PID"
else
    echo "BlueNimble Platform not running"
fi