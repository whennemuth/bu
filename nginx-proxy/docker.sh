#!/bin/bash

IMAGE_NAME="nginx-image"
CONTAINER_NAME="nginx-container"

explore() {
  if docker_container_running ; then
    winpty docker exec -ti $CONTAINER_NAME bash
  elif docker_container_sleeping ; then
    docker start -i $CONTAINER_NAME
    winpty docker exec -ti $CONTAINER_NAME bash
  else
    if ! docker_image_exists ; then
      build
    fi
    run
  fi
}


build() {
  docker build -t $IMAGE_NAME .
}


run() {
  docker run -d --name $CONTAINER_NAME $IMAGE_NAME
  winpty docker exec -ti $CONTAINER_NAME bash
}


refresh() {
  docker rm -f $CONTAINER_NAME 2> /dev/null
  build
  run
}


# Indicate if a docker image exists by name or name:tag
docker_image_exists() {
  local img="$1"
  [ ! $img ] && img=$IMAGE_NAME
  [ -n "$(docker images -q $img)" ] && true || false
}

docker_container_running() {
  local container="$1"
  [ !$container ] && container=$CONTAINER_NAME
  [ -n "$(docker ps -a --filter name=$container -q)" ] && true || false
}

docker_container_sleeping() {
  local container="$1"
  [ !$container ] && container=$CONTAINER_NAME
  [ -n "$(docker ps --filter name=$container -q)" ] && true || false
}
