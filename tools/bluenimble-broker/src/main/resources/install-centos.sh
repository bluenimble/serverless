#!/bin/sh
#
# Copyright (c) BlueNimble, Inc. (http://www.bluenimble.com)
#

echo       ""
echo       "BlueNimble Platform"
echo       "Copyright (c) BlueNimble, Inc. (https://www.bluenimble.com)"
echo       ""
echo       "Install BlueNimble Broker"

if [ -d "/opt/bluenimble/platform" ]; then
	echo "[ERROR] BlueNimble is already installed in this host"
	exit 1
fi

echo       "Install BlueNimble Broker in a Centos host"

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

echo "Update yum"
# update linux software repo
sudo yum -y update

sudo rpm -qa | grep -qw wget || sudo yum install -y wget
sudo rpm -qa | grep -qw libaio || sudo yum install -y libaio
sudo rpm -qa | grep -qw nfs-utils || sudo yum install -y nfs-utils
sudo rpm -qa | grep -qw java-1.8.0-openjdk || sudo yum -y install java-1.8.0-openjdk

BNB_RELEASE=$1

BNB_CONFIG_FOLDER=$2

TAR_FILE=bluenimble-broker-${BNB_RELEASE}-bin.tar.gz

BLUENIMBLE_DOWNLOAD_URL=https://github.com/bluenimble/serverless/releases/download/v${BNB_RELEASE}/$TAR_FILE

echo "Download and install BlueNimble Broker"
sudo mkdir -p /opt/displaystream && \
    sudo wget --no-cache $BLUENIMBLE_DOWNLOAD_URL && \
    sudo tar -xvzf $TAR_FILE -C /opt/bluenimble && \
    sudo rm $TAR_FILE && \
    sudo mv /opt/bluenimble/bluenimble-broker-$BNB_RELEASE /opt/bluenimble/broker

if [ -n "$BNB_CONFIG_FOLDER" ];
then 
  sudo cp $BNB_CONFIG_FOLDER/* /opt/bluenimble/broker
fi

echo "Grant Exec Permissions"
sudo chmod u+x /opt/bluenimble/broker/bnb.sh
sudo chmod u+x /opt/bluenimble/broker/bnb.stop.sh

echo "Create BlueNimble auto-start Service"
sudo cp /opt/bluenimble/broker/bnb.service /etc/systemd/system/bnb.service
sudo chmod 664 /etc/systemd/system/bnb.service
sudo systemctl enable /etc/systemd/system/bnb.service
sudo systemctl daemon-reload

echo "Start BlueNimble Broker Service"
sudo systemctl start bnb.service
