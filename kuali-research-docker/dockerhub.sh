#!/bin/bash

set -e

# set UNAME and password
UNAME="$1"
UPASS="$2"
REPO="$3"
ORG="$4"
[ -z "$ORG" ] && ORG="$UNAME"

# get token to be able to talk to Docker Hub
TOKEN=$(curl -s -H "Content-Type: application/json" -X POST -d '{"username": "'${UNAME}'", "password": "'${UPASS}'"}' https://hub.docker.com/v2/users/login/ | jq -r .token)

# get list of repos for that user account
REPO_LIST=$(curl -s -H "Authorization: JWT ${TOKEN}" https://hub.docker.com/v2/repositories/${ORG}/?page_size=10000 | jq -r '.results|.[]|.name')

# build a list of all images & tags
for r in ${REPO_LIST}
do
  if [ -n "$REPO" ] ; then
    [ "${r,,}" != "${REPO,,}" ] && continue;
  fi

  # get tags for repo
  IMAGE_TAGS=$(curl -s -H "Authorization: JWT ${TOKEN}" https://hub.docker.com/v2/repositories/${ORG}/${r}/tags/?page_size=10000 | jq -r '.results|.[]|.name')

  # build a list of images from tags
  if [ -z "$IMAGE_TAGS" ] ; then
    FULL_IMAGE_LIST="${FULL_IMAGE_LIST} ${ORG}/${r}:latest"
  else
    for j in ${IMAGE_TAGS}
    do
      # add each tag to list
      FULL_IMAGE_LIST="${FULL_IMAGE_LIST} ${ORG}/${r}:${j}"
    done
  fi
done

# output list of all docker images
for i in ${FULL_IMAGE_LIST}
do
  echo ${i}
done
