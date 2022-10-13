#!/bin/bash

# ---------------------------------------------------------------------------------
# This is a helper script for building and uploading (to a ECR) the docker image,
# and running a container from that image locally for mock testing.
# ---------------------------------------------------------------------------------

source ../../common-functions.sh

source ../../../docker.sh

debug() {
  stop

  if activeCredentials ; then
    echo "JOB_PARM = $JOB_PARM"
    export MSYS_NO_PATHCONV=1
    runCommand "docker run \\
      -d \\
      --rm \\
      --name $containerName \\
      -p 5678:5678 \\
      -v ~/.aws/credentials:/root/.aws/credentials \\
      -v $(dirname $(dirname $(dirname $(pwd))))/automated/loader.py:/automated/loader.py \\
      $(getEnvironmentVariables) \\
      $(getImageName) \\
      python /automated/loader.py $JOB_PARM start"
  fi
}

debug