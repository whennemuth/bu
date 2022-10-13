#!/bin/bash

createMounts() {
  echo "Creating new directory mounts..."
  createMount() {
    local dir="$1"
    if [ -d $dir ] ; then
      echo "$dir already exists."
    else
      echo "Creating ${dir}..."
      mkdir $dir
    fi
  }

  # Setup bind-mounts for mongo container
  createMount mongo/cor-main-data 
  createMount mongo/research-portal-data 
  createMount mongo/research-pdf-data 

  # Setup bind-mounts for kc container
  createMount kc/kc_tomcat_logs
  createMount kc/kc_api_keys
  createMount kc/kc_log4j

  # Setup bind-mounts and ssl certificates nginx & cor-main containers
  createMount nginx/certs
}

clearMounts() {
  echo "Deleting prior directory mounts..."
  deleteMount() {
    local dir="$1"
    if [ -d $dir ] ; then
      echo "Deleting ${dir}..."
      rm -rf $dir 2> /dev/null
    fi
  }
  deleteMount mongo/cor-main-data
  deleteMount mongo/research-portal-data
  deleteMount mongo/research-pdf-data
  deleteMount kc/kc_tomcat_logs
  deleteMount kc/kc_api_keys
  deleteMount kc/kc_log4j
  deleteMount nginx/certs
}

createCertificates() {
  (
    cd nginx/certs
    if [ ! -f self-signed.crt ] || [ ! -f self-signed.key ] ; then
      rm -rf self-signed.* 2> /dev/null
      export MSYS_NO_PATHCONV=1
      openssl req -newkey rsa:2048 \
        -x509 \
        -sha256 \
        -days 3650 \
        -nodes \
        -out ./self-signed.crt \
        -keyout ./self-signed.key \
        -subj "/C=US/ST=MA/L=Boston/O=BU/OU=IST/CN=${VIRTUAL_HOST}"
    fi
  )
}

up() {
  createMounts
  createCertificates
  docker-compose up --quiet-pull --detach --remove-orphans $@
}

task="${1,,}"
shift

if [ -f parameters.sh ] ; then
  source ./parameters.sh
else
  echo "File not found: parameters.sh, Cancelling."
  exit 1
fi

case "$task" in
  restart)
    docker-compose stop $@
    up $@
    ;;
  stop)
    docker-compose stop $@
    ;;
  up)
    up $@
    ;;
  down)
    docker-compose down $@
    ;;
  new)
    docker-compose down
    clearMounts
    up
    ;;
esac

