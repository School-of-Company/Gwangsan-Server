#!/bin/bash

ECR_REPO=gwangsan-ecr
REGION=ap-northeast-2
IMAGE_TAG=latest
CONTAINER_NAME=gwangsan-container
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# ECR 로그인
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com

# 기존 컨테이너 중지 및 제거
docker stop $CONTAINER_NAME || true
docker rm $CONTAINER_NAME || true

# 최신 이미지 pull 및 실행
docker pull $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
docker run -d --name $CONTAINER_NAME -p 80:8080 $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
