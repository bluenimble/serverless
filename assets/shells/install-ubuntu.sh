#!/bin/sh
#
# Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)
#

echo       ""
echo       "BlueNimble Platform"
echo       "Copyright (c) BlueNimble, Inc. (https://www.bluenimble.com)"
echo       ""
echo       "Install BlueNimble"

if [ -d "/opt/bluenimble/platform" ]; then
	echo "[ERROR] BlueNimble is already installed in this host"
	exit 1
fi

echo       "Install BlueNimble in an Ubuntu host"

CLEAN=$1

# change machine limits
sudo sysctl -w net.core.rmem_max=16777216
sudo sysctl -w net.core.wmem_max=16777216
sudo sysctl -w net.ipv4.tcp_rmem="4096 87380 16777216"
sudo sysctl -w net.ipv4.tcp_wmem="4096 16384 16777216"
sudo sysctl -w net.core.somaxconn=60000
sudo sysctl -w net.core.netdev_max_backlog=16384
sudo sysctl -w net.ipv4.tcp_max_syn_backlog=3240000
sudo sysctl -w net.ipv4.tcp_syncookies=1
sudo sysctl -w net.ipv4.ip_local_port_range="1024 65535"
sudo sysctl -w net.ipv4.tcp_tw_recycle=1
sudo sysctl -w net.ipv4.tcp_congestion_control=cubic
sudo sysctl -w net.ipv4.tcp_max_tw_buckets=1440000
sudo sysctl -w net.ipv4.tcp_fin_timeout=15
sudo sysctl -w net.ipv4.tcp_window_scaling=1

echo       "  Create /opt/bluenimble folder"

sudo mkdir -p /opt/bluenimble

echo "Update OS"
# update linux software repo
sudo apt-get update

sudo apt-cache pkgnames libaio || sudo apt-get install libaio
sudo apt-cache pkgnames wget || sudo apt-get install wget
sudo apt-cache pkgnames nfs-utils || sudo apt-get install nfs-utils
sudo apt-cache pkgnames java-1.8.0-openjdk || sudo apt-get install java-1.8.0-openjdk
sudo apt-cache pkgnames systemd || sudo apt-get install systemd

echo "Download and install BlueNimble"
wget --no-cache https://github.com/bluenimble/serverless/releases/download/v2.40.0/bluenimble-2.40.0-bin.tar.gz && \
  sudo tar -xvzf bluenimble-2.40.0-bin.tar.gz -C /opt/bluenimble && \
  rm -f bluenimble-2.40.0-bin.tar.gz

sudo mv /opt/bluenimble/bluenimble-2.40.0 /opt/bluenimble/platform

if [ $CLEAN = 'clean' ] ; then
	sudo rm -fr /opt/bluenimble/plugins/bluenimble-plugin-dev.playground-2.40.0
	sudo rm -fr /opt/bluenimble/spaces/playground
fi

echo "Create BlueNimble auto-start Service"
sudo chmod u+x /opt/bluenimble/platform/bnb.sh
sudo chmod u+x /opt/bluenimble/platform/bnb.stop.sh

sudo cp /opt/bluenimble/platform/bnb.service /etc/systemd/system/bnb.service
sudo chmod 664 /etc/systemd/system/bnb.service
sudo systemctl enable /etc/systemd/system/bnb.service
sudo systemctl daemon-reload

echo "Start BlueNimble Service"
sudo systemctl start bnb.service
