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

# Only set BNB_HOME if not already set
[ -f "$BNB_HOME"/bnb.sh ] || BNB_HOME=`cd "$PRGDIR" ; pwd`
export BNB_HOME
cd "$BNB_HOME"

BNB_PID=$BNB_HOME/bnb.pid

if [ -f "$BNB_PID" ]
then
    echo "Stopping BlueNimble Platform"
    kill -15 $(cat "$BNB_PID")
    
    while [ $(kill -0 "$BNB_PID") ]; do
	  sleep 1
	done
	
	echo "BlueNimble went down!"
    rm "$BNB_PID"
else
    echo "BlueNimble Platform not running"
fi