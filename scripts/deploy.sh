#!/bin/bash

ECR_REPO=gwangsan-ecr
REGION=ap-northeast-2
IMAGE_TAG=latest
CONTAINER_NAME=gwangsan-container
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com

docker stop $CONTAINER_NAME || true
docker rm $CONTAINER_NAME || true

docker pull $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
docker run -d --name $CONTAINER_NAME -p 8080:8080 --env-file /root/.env -e TZ=Asia/Seoul $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
