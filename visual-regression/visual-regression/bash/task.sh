#!/bin/bash

source ./utils.sh

# Default values
defaultMainClass="bu.ist.visreg.Processor"
defaultBucket="wrh1-test-bucket"
defaultFileRoot="/opt/visual-regression/baskets"

# The classpath to be used with the java -cp parameter
classpath="/app/basket/target/classes:/app/basket/target/dependency/*"

parseargs() {
  echo "Parsing arguments (task.sh)..."

  # Blank out prior settings.
  posargs=()
  task=""
  bucket=""
  mainClass=""

  while (( "$#" )); do
    case "$1" in
      -t|--task)
        eval "$(parsearg $1 $2 'task')" ;;
      -b|--bucket)
        eval "$(parsearg $1 $2 'bucket')" ;;
      -f|--file-root)
        eval "$(parsearg $1 $2 'fileRoot')" ;;
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
        posargs=(${posargs[@]} $1)   
        shift
        ;;
    esac
  done

  [ -z "$mainClass" ] && mainClass=$defaultMainClass
  [ -z "$bucket" ] && bucket=$defaultBucket
  [ -z "$fileRoot" ] && fileRoot=$defaultFileRoot
}

printusage() {
  cat <<EOF

USAGE: sh task.sh [OPTIONS]  
  Options:
    -h, --help)              Print this message.
    -t, --task               Required: Identifies what this script call is to do. 
      run-java-s3              Run a task that involves the AWS S3 service.
        -m, --main-class         The main java class (default: "$defaultMainClass").
        -b, --bucket             AWS S3 bucket name. (default: "$defaultBucket").
      run-java-fs              Run a task that involves the local file system.
        -m, --main-class         The main java class (default: "$defaultMainClass").
        -f, --file-root          Not using an S3 bucket, basketing is being done locally at this file system directory
      bucket-ls              List the contents of an S3 bucket.
        -b, --bucket           AWS S3 bucket name. (default: "$defaultBucket").        
  
  Examples:

    sh task.sh \
      --task run-java-s3 \
      --bucket MyTestBucket \
      --main-class bu.ist.visreg.basket.s3.S3Bucket

    sh task.sh \
      --task run-java-fs \
      --file-root /opt/my/path/baskets \
      --main-class bu.ist.visreg.basket.filesystem.FileBasketSystem
EOF
}

dotask() {

  parseargs $@

  case "$task" in
    run-java-s3)
      java -cp $classpath $mainClass --basket-type s3 --root $bucket ;;
    run-java-fs)
      java -cp $classpath $mainClass --basket-type filesystem --root $fileRoot ;;
    bucket-ls)
      java -cp $classpath bu.ist.visreg.basket.s3.S3Bucket --bucket $bucket ;;
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

echo "dotask $@"

dotask $@