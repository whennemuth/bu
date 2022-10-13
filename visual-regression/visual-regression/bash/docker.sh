#!/bin/bash

source ./utils.sh

defaultRegion="us-east-1"
defaultImage="bu-visual-regression"
defaultMainClass="bu.ist.visreg.Processor"
defaultBucket="wrh1-test-bucket"

parseargs() {
  echo "Parsing arguments (docker.sh)..."

  local posargs=""

  while (( "$#" )); do
    case "$1" in
      -t|--task)
        eval "$(parsearg $1 $2 'task')" ;;
      -i|--image-name)
        eval "$(parsearg $1 $2 'imageName')" ;;
      -k|--secret-access-key)
        eval "$(parsearg $1 $2 'secretAccessKey')" ;;
      -a|--access-key-id)
        eval "$(parsearg $1 $2 'accessKeyId')" ;;
      -r|--region)
        eval "$(parsearg $1 $2 'region')" ;;
      -b|--bucket)
        eval "$(parsearg $1 $2 'bucket')" ;;
      -m|--main-class)
        eval "$(parsearg $1 $2 'mainClass')" ;;
      -h|--help)
        printusage
        exit 0
        ;;
      -*|--*=) # unsupported flags
        echo "Error: Unsupported flag $1" >&2
        printusage
        exit 1
        ;;
      *) # preserve positional arguments (should not be any more than the leading command, but collect then anyway)      
        posargs="$posargs $1"
        shift
        ;;
    esac
  done

  [ -z "$region" ] && region=$defaultRegion
  [ -z "$imageName" ] && imageName=$defaultImage
  [ -z "$mainClass" ] && mainClass=$defaultMainClass
  [ -z "$bucket" ] && bucket=$defaultBucket
}

printusage() {
  cat <<EOF

USAGE: sh docker.sh [OPTIONS]  
  Options:
    -t, --task               The task to perform (test, bucket-ls)
    -i, --image-name         The name of the docker image to run (default "$defaultImage").
    -k, --secret-access-key  AWS secret access key.
    -a, --access-key-id      AWS access key ID.
    -r, --region             AWS region (default "$defaultRegion")
    -b, --bucket             AWS S3 bucket name.
    -m, --main-class         The main java class (default "$defaultMainClass").
    -h, --help)              Print this message.

  Example:
    sh docker.sh run \
      --task test \
      --main-class bu.ist.visreg.basket.s3.S3Bucket \
      --image-name myTestImage \
      --access-key-id MYACCESSKEYID \
      --secret-access-key MYSECRETACCESSKEY 
EOF
}

run() {
  case $task in
    test)
      docker run \
        --rm \
        -e AWS_ACCESS_KEY_ID="$accessKeyId" \
        -e AWS_SECRET_ACCESS_KEY="$secretAccessKey" \
        -e AWS_REGION="$region" \
        $imageName \
        java \
        -cp /app/basket/target/classes:/app/basket/target/dependency/* \
        bu.ist.visreg.basket.s3.S3Bucket \
        --bucket $bucket
      ;;
    bucket-ls)
      docker run \
        --rm \
        -e AWS_ACCESS_KEY_ID="$accessKeyId" \
        -e AWS_SECRET_ACCESS_KEY="$secretAccessKey" \
        -e AWS_REGION="$region" \
        $imageName \
        bash task.sh --task bucket-ls
        ;;
    *)
      if [ -z "$task" ] ; then
        echo "Task not specified!"
      else
        echo "No such task: \"$task\""
      fi
      printusage
      ;;
  esac
}

parseargs $@

run