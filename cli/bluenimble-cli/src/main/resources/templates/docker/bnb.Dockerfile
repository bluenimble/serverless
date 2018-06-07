############################################################
# Dockerfile to run BlueNimble on an Alpine Container
############################################################

FROM bluenimble/alpine-java

MAINTAINER BlueNimble, Inc. (community@bluenimble.com)

RUN apk update && apk add ca-certificates wget && update-ca-certificates

# Override the bluenimble download location with e.g.:
#   docker build -t mybnb --build-arg BLUENIMBLE_DOWNLOAD_SERVER=http://downloads.bluenimble.com/platform .
ARG BLUENIMBLE_DOWNLOAD_SERVER

ENV BLUENIMBLE_DOWNLOAD_URL ${BLUENIMBLE_DOWNLOAD_SERVER:-http://downloads.bluenimble.com/platform}/bluenimble-server.tar.gz

# Download distribution tar, untar
RUN mkdir -p /opt/bluenimble/platform
RUN wget --no-cache $BLUENIMBLE_DOWNLOAD_URL && \
  tar -xvzf bluenimble-server.tar.gz -C /opt/bluenimble/platform && \
  rm bluenimble-server.tar.gz

RUN chmod 755 /opt/bluenimble/platform/bn.sh
RUN chmod 755 /opt/bluenimble/platform/bn.stop.sh

COPY boot.lf /opt/bluenimble/platform/boot.lf

# expose ports
EXPOSE 80 90

# Start server
CMD ["/opt/bluenimble/platform/bn.sh"]