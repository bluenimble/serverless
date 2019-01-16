#!/bin/sh
#
# Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)
#

echo       ""
echo       "BlueNimble Message Broker (Native, Socket-IO / Websockets) Version [version]"
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

BNB_PID=$BNB_HOME/bnb.pid

if [ -f "$BNB_PID" ]
then
    echo "Stopping BlueNimble Message Broker"
    
    PID=$(cat "$BNB_PID")

    kill -15 "$PID"

    while [ $(kill -0 "$PID") ]; do
	  sleep 1
	done
	
	echo "BlueNimble Message Broker went down!"
    rm "$BNB_PID"
else
    echo "BlueNimble Message Broker isn't running in this node"
fi