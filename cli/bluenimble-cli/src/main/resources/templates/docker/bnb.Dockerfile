############################################################
# Dockerfile to run BlueNimble on an Alpine Container
############################################################

FROM bluenimble/alpine-java

MAINTAINER BlueNimble, Inc. (community@bluenimble.com)

RUN apk update && apk add ca-certificates wget && update-ca-certificates

# Override the bluenimble download location with e.g.:
#   docker build -t mybnb --build-arg BLUENIMBLE_DOWNLOAD_SERVER=http://downloads.bluenimble.com/platform .
ARG BLUENIMBLE_DOWNLOAD_SERVER

ENV BLUENIMBLE_DOWNLOAD_URL ${BLUENIMBLE_DOWNLOAD_SERVER:-https://github.com/bluenimble/serverless/releases/download/v[version]/bluenimble-[version]-bin.tar.gz

# Download distribution tar, untar
RUN mkdir -p /opt/bluenimble
RUN wget --no-cache $BLUENIMBLE_DOWNLOAD_URL && \
  tar -xvzf bluenimble-[version]-bin.tar.gz -C /opt/bluenimble && \
  rm bluenimble-[version]-bin.tar.gz

RUN chmod 755 /opt/bluenimble/bnb.sh
RUN chmod 755 /opt/bluenimble/bnb.stop.sh

[[#if plugins.discard]]
RUN rm -rf /opt/bluenimble/plugins/[[this]]
[[/if]]

# expose ports
EXPOSE 9090 7070

# Start server
CMD ["/opt/bluenimble/bnb.sh"]