#!/bin/bash

IMAGE_NAME=gwangsan-app
CONTAINER_NAME=gwangsan-container
JAVA_HOME=/usr/lib/jvm/jdk-21.0.7

docker load < /home/ec2-user/docker-image/${IMAGE_NAME}.tar


docker rm -f $CONTAINER_NAME || true

docker run -d --name $CONTAINER_NAME -p 8080:8080 \
  --env-file /home/ec2-user/.env \
  $IMAGE_NAME
