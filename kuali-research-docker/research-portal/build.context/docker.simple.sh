#!/bin/bash

source ../../bash.lib.sh

build() {
  docker build -t bu-ist/research-portal .
}

# Run the docker container
# Arguments:
#    log_group:    [OPTIONAL] If provided, indicates that logging is to occur against cloudwatch and indicates the cloudwatch log group
#    docker_image: [REQUIRED] Indicates the name of the image to run the container from.
runcontainer() {

  [ -z "$ROOT_DIR" ] && ROOT_DIR=$(pwd)
  NODEAPP_CONFIG_DIR="$ROOT_DIR/config"
  NODEAPP_DATA_DIR="$ROOT_DIR/mongo-data"
  NODEAPP_SCRIPTS_DIR="$ROOT_DIR/scripts"

  # Set the named parameters as local variables.
  eval "$(parseargs $@)"

  if [ -n "$log_group" ] ; then
    docker run \
      -d \
      -p 3005:3005 \
      -p 9229:9229 \
      --restart unless-stopped \
      --name research-portal \
      --log-driver=awslogs \
      --log-opt awslogs-region=us-east-1 \
      --log-opt awslogs-group=$log_group \
      --log-opt awslogs-create-group=true \
      -v $(getOSPath $NODEAPP_SCRIPTS_DIR):/var/bash-scripts \
      -v $(getOSPath $NODEAPP_SCRIPTS_DIR):/var/portal-config \
      $docker_image
  else
    docker run \
      -d \
      -p 3005:3005 \
      -p 9229:9229 \
      --restart unless-stopped \
      --name kuali-research \
      -v $(getOSPath $NODEAPP_SCRIPTS_DIR):/var/bash-scripts \
      -v $(getOSPath $NODEAPP_SCRIPTS_DIR):/var/portal-config \
      $docker_image
  fi
}

runcw() {
  run_container "confirm=true" "cloudwatch=true"
}

run() {
  run_container "confirm=true"
}
