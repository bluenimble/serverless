#!/bin/sh
#
# Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)
#

echo       ""
echo       "BlueNimble Platform"
echo       "Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)"
echo       ""
echo       "Upgrade Server"

echo       "  Stop Services"
systemctl stop bn.service

time_stamp=$(date +%Y_%m_%d_%H_%M_%S)
oldInstall=/opt/bluenimble/platform_$time_stamp

echo       "  Backup old version to $oldInstall"
mv /opt/bluenimble/platform $oldInstall
mkdir /opt/bluenimble/platform
cd /opt/bluenimble/platform
echo       "  Download the new version"
wget http://downloads.bluenimble.com/platform/bluenimble-server.tar.gz
tar -xvzf bluenimble-server.tar.gz -C .
rm -f bluenimble-server.tar.gz
chmod u+x bn.sh
chmod u+x bn.stop.sh
chmod u+x upgrade.sh

cp $oldInstall/boot.lf /opt/bluenimble/platform/boot.lf

echo       "  Start BlueNimble service"
systemctl start bn.service
