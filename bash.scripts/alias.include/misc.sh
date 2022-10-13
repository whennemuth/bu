#!/bin/bash


findAndOpenInVim(){
   vi $(find . -type f -iname $1)
}

# Turn a unix style path to a windows style path.
# The first parameter either:
#   1) A string representing one or more paths (it's windows-converted value will be echoed)
#   2) The path to a file whose content contains unix paths which need to be converted in place to windows paths.
# Include a second parameter (any value) to indicate the first parameter is a file pathname
convertToWindowsPath() {
  local drive='s/\/c\//c:\\/gi'
  local slash='s/\//\\/gi'

  if [ -n "$2" ] ; then  
    sed -i $drive "$1"
    sed -i $slash "$1"
  else 
    local temp=$(echo -n "$1" | sed $drive)
    temp=$(echo -n "$temp" | sed $slash)
    echo -n "$temp"
  fi
}

# Turn a windows style path to a unix style path.
# The first parameter either:
#   1) A string representing one or more paths (it's unix-converted value will be echoed)
#      NOTE: This parameter must be surrounded by single quotes when the function is called.
#   2) The path to a file whose content contains windows paths which need to be converted in place to unix paths.
# Include a second parameter (any value) to indicate the first parameter is a file pathname
convertToUnixPath() {
  local drive='s/c:\\/\/c\//gi'
  local slash='s/\\/\//gi'
  
  if [ -n "$2" ] ; then 
    sed -i $drive "$1"
    sed -i $slash "$1"
  else
    local temp=$(echo -n "$1" | sed $drive)
    temp=$(echo -n "$temp" | sed $slash)
    echo -n "$temp"
  fi
}

grepNode() {
  local switches="$1"
  local expression="$2"
  [ -z "$expression" ] && echo "No search term provided" && return 1
  [ -z "$switches" ] && switches="-r"
  [ -z "$(echo -n $switches | grep -o '-')" ] && switches="-$switches"
  # read -p "Type an expression to search for: " expression; 
  grep $switches \
    --exclude-dir={node_modules,dist,build,test} \
    --exclude=*.bundle.js \
    --exclude=bundle.js \
    --exclude=*.min.js \
    --exclude=*.min.js.* \
    --exclude=*.map \
   "$expression" .
}

set -a

# Use a 3rd party script that queries the stack status in a loop and displays only new status items that have not already been printed out.
watchStack() {
  local stackname=$1
  local scriptlib="$HOME/bash.scripts/alias.include"
  local scriptdir="$scriptlib/aws-cloudformation-stack-status"
  local scriptfile="$scriptdir/aws-cloudformation-stack-status"
  if [ ! -f $scriptfile ] ; then
    if [ ! -d $scriptlib ] ; then
      mkdir -p $scriptlib
    fi
    cd $scriptlib
    git clone https://github.com/alestic/aws-cloudformation-stack-status.git
    cd -
  fi
  sh $scriptfile --watch --color --stack-name $stackname
}

 
watch() {
  ARGS="${@}"
  clear; 
  while(true); do 
    OUTPUT=`$ARGS`
    clear 
    echo -e "${OUTPUT[@]}"
    sleep 2
  done
}


updateIam() {
  ecsupload test/iam_for_kuali.template
  aws \
    cloudformation update-stack \
    --stack-name Kuali-IAM-EC2 \
    --no-use-previous-template \
    --template-url "https://s3.amazonaws.com/kuali-research-ec2-setup/ecs/cloudformation/test/iam_for_kuali.template" \
    --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM
}

esCreate() {
  local env=${1:-sb}
  elasticSearchStackAction create-stack $env
}

esUpdate() {
  local env=${1:-sb}
  elasticSearchStackAction update-stack $env
}

esDelete() {
  local env=${1:-sb}
  printf "Delete the elasticsearch stack for $env? (y/n): "
  read answer
  if [ "${answer,,}" == "y" ] ; then
    elasticSearchStackAction delete-stack $env
  else
    echo "Cancelled."
  fi
}

elasticSearchStackAction() {
  local action=${1:-update-stack}
  local env=${2:-sb}
  local stackname=kuali-elasticsearch-$env
  ecsupload related/es_for_kuali.yaml

  if [ "$action" == 'delete-stack' ] ; then
    aws cloudformation $action --stack-name $stackname     
    [ $? -gt 0 ] && echo "Cancelling..." && return 1
  else
    aws \
      cloudformation $action \
      --stack-name $stackname \
      $([ $action != 'create-stack' ] && echo '--no-use-previous-template') \
      --template-url "https://s3.amazonaws.com/kuali-research-ec2-setup/ecs/cloudformation/related/es_for_kuali.yaml" \
      --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
      --parameters 'ParameterKey=Landscape,ParameterValue='$env

    [ $? -gt 0 ] && echo "Cancelling..." && return 1
  fi

  watchStack $stackname
}

# Get the debuglink for a node 8.x app running inside a docker container on a remote server.
# The docker network url must be replaced with a localhost url.
# Also, for some reason, curling the json/list url may suddenly stop outputting the debuglink info, even though it is still valid.
# Therefore, we cache the last known output in the /tmp directory and output that, hoping it is still the right one.
# If not, then the only way to get a new debug link is to restart the docker container and run again.
debugurl() {

  local nodeversion="$1"

  if [ $nodeversion == "6.x" ] ; then
    docker logs core | grep "chrome-devtools" | tail -n1
    return
  fi

  # nodeversion must be 8.x
  docker exec core curl 127.0.0.1:9229/json/list  > /tmp/core.debug.link.temp

  if [ -n "(cat /tmp/core.debug.link.temp 2>/dev/null | grep -i debuglink)" ] ; then
    local refresh="true"
  elif [ ! -f /tmp/core.debug.link ] ; then
    local refresh="true"
  elif [ -z "(cat /tmp/core.debug.link 2>/dev/null | grep -i debuglink)" ] ; then
    local errmsg="Cannot find a debug link!"
    local refresh="true"
  fi

  if [ $refresh ] ; then
    [ -f /tmp/core.debug.link ] && rm -f /tmp/core.debug.link
    mv /tmp/core.debug.link.temp /tmp/core.debug.link
  fi

  rm -f /tmp/core.debug.link.temp

  [ $errmsg ] && echo $errmsg && return

  cat /tmp/core.debug.link \
    | grep devtools \
    | sed 's/[0-9]*\.[0-9]*\.[0-9]*\.[0-9]*/127.0.0.1/g' \
    | sed 's/[",]//g' \
    | sed 's/devtools[a-zA-Z]*//' \
    | sed 's/://'
}

