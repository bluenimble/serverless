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
  
sudo mv /opt/bluenimble/bluenimble-${VERSION} /opt/bluenimble/platform  
  
sudo chmod u+x /opt/bluenimble/platform/bnb.sh
sudo chmod u+x /opt/bluenimble/platform/bnb.stop.sh
sudo chmod u+x /opt/bluenimble/platform/upgrade.sh

echo       "  Start BlueNimble Service"
sudo systemctl start bnb.service
