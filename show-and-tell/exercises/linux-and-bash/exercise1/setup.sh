#!/bin/bash

if [ -f ../../lib.sh ] ; then
  source ../../lib.sh
elif [ -f ../../../lib.sh ] ; then
  source ../../../lib.sh
fi

docker rm -f devops-bash-exercise1 2> /dev/null || true

# Create directories
( [ ! -d bash ] && mkdir -p bash ) && \
cat <<EOF > bash/unsorted
pears
oranges
bannannas
apples
EOF

# Run docker container
winpty docker run \
  --rm \
  -ti \
  --name devops-bash-exercise1 \
  -v $(getPwdForMount)bash:/tmp \
  centos:7 \
  bash
