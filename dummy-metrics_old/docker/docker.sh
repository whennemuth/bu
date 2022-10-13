#!/bin/bash

# Examples:
#   source docker.sh build
#   source docker.sh build run
#   source docker.sh buildbase run
main() {
  # Make sure we are in the right directory
  if [ ! -d build.context.node ] || [ ! -d build.context.react ] ; then
    printf "\nERROR! Not in the correct directory. \nExpecting build.context.node and build.context.react subdirectories.\n"
    return 1
  fi

  # Validate the parameters
  echo "$1" | prompt_for_numbered_choice "Pick one (enter the numeric value): " \
    "Build node docker image" \
    "Build node docker image and run container on console" \
    "Build node docker image and run container detached" \
    "Build node docker image and its base image" \
    "Build node docker image and its base image and run container on console" \
    "Build node docker image and its base image and run container detached" \
    "Build react docker image" \
    "Build react docker image and run container on console" \
    "Build react docker image and run container detached" \
    "Build react docker image and its base image" \
    "Build react docker image and its base image and run container on console" \
    "Build react docker image and its base image and run container detached" \
    "Run container on console" \
    "Run container detached" \
    "Cancel"

  local choice=$?

  case $choice in 4|5|6|10|11|12)
    build base ;;  
  esac

  case $choice in 1|2|3|4|5|6)
    cd docker/build.context.node
    build
    cd .. ;;
  esac

  case $choice in 7|8|9|10|11|12)
    cd docker/build.context.react
    build
    cd .. ;;
  esac

  case $choice in 2|5|8|11|13)
    run terminal ;;
  esac

  case $choice in 3|6|9|12|14)
    run detached ;;
  esac
}

build() {
  echo "Building..."
  docker rm -f nodetest
  
  if [ $1 == 'base'] ; then
    docker rmi nodeserver:v1 || return 1;
    docker rmi node8plus || return 1;
    docker build -t node8plus -f . || return 1
  else
    docker build -t nodeserver:v1 .
  fi
  docker rmi $(docker images --filter dangling=true -q) 2> /dev/null
}

run() {
  if [ -n "$(docker ps -a | grep nodetest)" ] ; then
    docker rm -f nodetest
  fi
  [ $1 == 'terminal' ] && local switch="-ti" || local switch="-d"
  docker run \
    $switch \
    --name=nodetest \
    -p 4321:8080 \
    -p 9229:9229 \
    -e TESTVAR2=GOODBYE \
    -v c:/whennemuth/scrap/node/env:/var/env \
    nodeserver:v1
}

prompt_for_numbered_choice() {
  read input
  local prompt="\n$1"
  local choices=("$@")
  local max=$((${#choices[@]}-1))
  if [[ $input =~ ^[0-9]+$ ]] && [ $input -gt 0 ] && [ $input -le $max ] ; then
    # The numbered choice was piped in, so we just want to validate it and return.
    answer=$input
  else
    [ $input ] && printf "\nYour input must be a number between 1 and $max\n"
    # 1) Construct a numbered list to display to the user based on the supplied arguments.
    for ((i = 1; i < ${#choices[@]}; i++)) ; do
      prompt="$prompt\n  $i) $(echo ${choices[i]})"
    done
    # 2) Display the choices to the user
    printf "$prompt\n: "

    # 3) Get a selection from the user from the list.
    #   a) Open a new file descriptor that points to the same file as stdout does.
    #      This will prevent the upcoming read from automatically reading any input piped into the function.
    exec 3>&1
    #   b) Read user input against the new file descriptor.
    read -u 3 answer
    #   c) But don't forget to close the file descriptor when finished.
    exec 3>&-

    echo "$answer" | prompt_for_numbered_choice "$@"
    answer=$?
  fi
  return $answer
}

main "$@"
