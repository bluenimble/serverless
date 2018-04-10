#!/bin/sh
#
# Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)
#

echo       ""
echo       "BlueNimble Platform"
echo       "Copyright (c) BlueNimble, Inc. (https://www.bluenimble.com)"
echo       ""
echo       "Install BlueNimble"

if [ ! -d "/opt/bluenimble/platform" ]; then
	echo "BlueNimble isn't installed in this host"
	exit 1
fi

VERSION=$1

if [ -z "$VERSION" ] ; then
	echo "enter a version to install!"
	echo "   Example: ./upgrade.sh 1.2.0"
	echo "Check https://github.com/bluenimble/serverless/releases for available versions"
    exit 1
fi

echo       "  Stop Services"
sudo systemctl stop bnb.service

time_stamp=$(date +%Y_%m_%d_%H_%M_%S)
oldInstall=/opt/bluenimble/platform_$time_stamp

echo       "  Backup old version to $oldInstall"
sudo mv /opt/bluenimble/platform $oldInstall
sudo mkdir -p /opt/bluenimble

echo       "  Download the new version"
wget --no-cache https://github.com/bluenimble/serverless/releases/download/v${VERSION}/bluenimble-${VERSION}-bin.tar.gz && \
  sudo tar -xvzf bluenimble-${VERSION}-bin.tar.gz -C /opt/bluenimble && \
  rm -f bluenimble-${VERSION}-bin.tar.gz
  
echo       "Copying existing Spaces"  
for f in $oldInstall/spaces
do
	if [ "$(basename $f)" != "sys" ] ; then
  		echo "Copy Space $(basename $f)"
		sudo cp $f /opt/bluenimble/platform/spaces
	fi
done

chmod u+x bnb.sh
chmod u+x bnb.stop.sh
chmod u+x upgrade.sh

echo       "  Start BlueNimble Service"
sudo systemctl start bnb.service
