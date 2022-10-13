#!/bin/bash

#################################################################################################
# 
# This script is used to run each of the steps required to go from a docker build context
# and end up with a built research-portal docker image and container running from it. 
# This script serves as both automation and documentation.
#
# Example function calls:
# 
# # (each of the following can be run separately).
# source docker.sh
# build
# runapp
# 
# or...
# source docker.sh && deploy
#
# NOTE: Directories for mounting to the container and locating configuration files will be created within the docker build context directory.
# Also, before running this script, make sure you add the following to the docker build context:
#    1) The core configuration file named "env.vars.list" (OPTIONAL).
#    2) The private SSH key for gaining pull access to the BU git repo where the codebase is stored (If building image).
#
# CONFIGURATION FILES:
# The core application was coded so that all configurations derive from environment variables if present
# before falling back to hard-coded default values. Thus you can configure the app almost entirely
# through the docker run command with -e or -env-file switches. For this reason a single environment
# variables file named environment.variables.final.env can be placed in the docker build context containing
# lines of name=value pairs to configure the app. However, you can omit such files if running a 
# container in one of the standard BU environments and this script will attempt to obtain an 
# "environment.variables.s3.env" file from one of our AWS S3 buckets. Also, an "environment.variables.local.env"
# file can also be placed in the docker build context directory to override any of the variables in the
# .s3 file. The ".s3" file serves as the base for a new ".final" file, which is further 
# overwritten/appended with matching lines from the ".local" file. The container only looks for a ".final" file.
#
# IMPORTANT!!!: Do not replace tab characters with spaces in this file, else the cat/EOF lines will not work.
# 
#################################################################################################

source ../../bash.lib.sh

# Named parameters can be passed to any function in this script as "name=value"
# These parameters will be set as session scoped variables if they match by name
# any of a set of parameter names, else they are ignored. Parameter names are case insensitive.
parseArgs() {

   # NOTE: The mongo parameters below will be used by the docker run command for the container and will be picked up by the
   # startup.sh script for access to mongodb. Otherwise startup.sh will parse local.js for these parameters.
   # So this would be a way of overriding local.js and connecting to some other mongodb for a while.
   EXPECTED=(
      'ROOT_DIR'
      'GIT_REPULL'
      'GIT_REFSPEC'
      'GIT_BRANCH'
      'RESEARCH_SECRET'
      'INTERACTIVE'
      'PORTAL_HOST'
      'PORT'
      'START_CMD'
      'DOCKER_IMAGE_NAME'
      'MONGODB_URI'
      'MONGODB_USERNAME'
      'MONGODB_PASSWORD'
      'AWS_ACCESS_KEY_ID'
      'AWS_SECRET_ACCESS_KEY'
      'AWS_DEFAULT_REGION'
      'AWS_PROFILE'
      'LANDSCAPE'
      'CONFIG_LANDSCAPE'
      'BLAST_CONTAINER'
      'BLAST_IMAGE'
      'DRYRUN'
      'KEEP_DANGLING'
   )

   args_to_session_variables "${#EXPECTED[@]}" "${EXPECTED[@]}" "$@"
}

defaults() {
  [ -z "$ROOT_DIR" ] && ROOT_DIR=$(pwd)
  # INTERACTIVE is true by default unless explicitly set to false.
  [ -z "$INTERACTIVE" ] && INTERACTIVE="true"
  [ "${INTERACTIVE,,}" == "false" ] && INTERACTIVE="" || INTERACTIVE="true"
  # DRYRUN is false by default unless explicitly set to true
  [ "${DRYRUN,,}" != "true" ] && DRYRUN=""
  # BLAST_CONTAINER is false by default unless explicitly set to true
  [ "${BLAST_CONTAINER,,}" != "true" ] && BLAST_CONTAINER=""
  # BLAST_IMAGE is false by default unless explicitly set to true
  [ "${BLAST_IMAGE,,}" != "true" ] && BLAST_IMAGE=""
  LOCAL_DOCKER_IMAGE=bu-ist/portal
  [ -z "$DOCKER_REGISTRY" ] && DOCKER_REGISTRY="730096353738.dkr.ecr.us-east-1.amazonaws.com"
  [ -z "$CONTAINER_NAME" ] && CONTAINER_NAME="research-portal"
  true
}

defaults


# Inspect the file system for the proper place to build the research-portal application
setDirectories() {
  [ $? -gt 0 ] && return $?

  echo "Setting directories..."

  if [ ! -d $ROOT_DIR ] ; then
    echo "$ROOT_DIR for root research-portal directory not found! Using $(pwd)"
    ROOT_DIR="$(pwd)";
  else
    echo "Found $ROOT_DIR"
  fi

  NODEAPP_CONFIG_DIR="$ROOT_DIR/config"
  NODEAPP_DATA_DIR="$ROOT_DIR/mongo-data"
  NODEAPP_SCRIPTS_DIR="$ROOT_DIR/scripts"

  return 0
}


# Set all local and environment variables and check that all the necessary files
# are present that the build process requires.
initialize() {
  [ $? -gt 0 ] && return $?

  [ $DEPLOYING ] && return 0

  echo "Initializing..."

  if [ ! -f ../../bash.lib.sh ] ; then
    echo "CANCELLING DUE TO MISSING SCRIPT FILE: ../../bash.lib.sh"
    return 1;
  fi

  if ! parseArgs "$@" ; then
    echo "CANCELLING DUE TO INVALID PARAMETER(S)";
    return 1;
  fi

  defaults

  setDirectories

  checkdir $NODEAPP_CONFIG_DIR
  checkdir $NODEAPP_DATA_DIR
  checkdir $NODEAPP_SCRIPTS_DIR

  return 0
}


# Build the docker image for research-portal.
build() {
  [ $? -gt 0 ] && return $?

  printHeader "BUILDING DOCKER IMAGES..."

  initialize "$@"
  [ $? -eq 1 ] && return 1

  # The bash lib and build helper files needs to be in the docker build context dir because they get copied into the image when building
  unalias cp 2> /dev/null || true
  cp ../../bash.lib.sh .
  cp ../../buildhelper.sh .

  [ ! -f bash.lib.sh ] && echo "Cancelling build! bash.lib.sh not in build context!" && return 1
  [ ! -f buildhelper.sh ] && echo "Cancelling build! buildhelper.sh not in build context!" && return 1

  # Make sure the expected git rsa key(s) are in the build context directory, pulling them from s3 if not found.
  local keys=(
    'bu_github_id_research_portal_rsa'
    'bu_github_id_kualiui_rsa')
  getGitKeys 'kuali-research-ec2-setup' "$(pwd)" "${keys[@]}"
  [ $? -eq 1 ] && return 1

  setGitRepull

  setGitRefspec

  [ -z "$DOCKER_IMAGE_NAME" ] && DOCKER_IMAGE_NAME="$LOCAL_DOCKER_IMAGE"
  echo "Building image $DOCKER_IMAGE_NAME ..."
  local CMD="docker build -t $DOCKER_IMAGE_NAME"

  [ -n "$GIT_REFSPEC" ] && CMD="${CMD} --build-arg GIT_REFSPEC=${GIT_REFSPEC} --build-arg GIT_BRANCH=${GIT_BRANCH}"

  [ -f LAST_IMAGE_BUILD_DATE ] && CMD="${CMD} --build-arg DATETIME=$(cat LAST_IMAGE_BUILD_DATE)"

  CMD="${CMD} ."

  GIT_REPULL=""
  GIT_REFSPEC=""
  GIT_BRANCH=""
    
  [ $DRYRUN ] && echo "DRYRUN:"
  echo $CMD
  if [ ! $DRYRUN ] ; then
    eval $CMD
    if [ "${KEEP_DANGLING,,}" != "true" ] ; then
      # Don't clean dangling images if the build failed. Might need to run a container off one of the dangling layers to debug.
      [ $? -eq 0 ] && cleanDanglingDockerImages
    fi
  fi
  unalias rm 2> /dev/null || true
  rm -f bash.lib.sh
  rm -f buildhelper.sh
}


# Run a new docker container. Assumes the docker image is already present, but no container has been created yet.
runapp() {
  [ $? -gt 0 ] && return $?

  printHeader "RUNNING APP CONTAINER..."

  initialize "$@"

  [ $? -gt 0 ] && return $?

  [ -z "$DOCKER_IMAGE_NAME" ] && [ ! $INTERACTIVE ] && echo "ERROR! Missing the name of the docker image." && return 1

  if ! checkContainer "name=$CONTAINER_NAME" "interactive=$INTERACTIVE" "blast_container=$BLAST_CONTAINER" ; then
    if [ $DRYRUN ] ; then
      echo "DRYRUN: Prompting the user to remove existing container '$CONTAINER_NAME'"
    else
      [ $INTERACTIVE ] && return 0
      echo "ERROR! Container '$CONTAINER_NAME' already exists. stop and/or remove and try again."
      return 1
    fi
  fi

  setHostUrl || return 1

  copyStartupFile || return 1

  copyBashLibs || return 1

  processEnvironmentVariablesFile || return 1

  setStartMode || return 1

  if sudo_exists ; then
    sudo chmod -R 777 $ROOT_DIR
  else
    chmod -R 777 $ROOT_DIR
  fi

  [ $? -gt 0 ] && return $?


  if [ -z "$DOCKER_IMAGE_NAME" ] && [ $INTERACTIVE ] ; then
    prompt_for_numbered_choice \
      "No docker image specified. Where is it?" \
      "Local, in this repository" \
      "Up in the registry (main repository)" \
      "Up in the registry (feature repository)"
    local choice=$?
    echo " "
    if [ $choice == 1 ] ; then
      read -p "Enter the name:tag value for the image: " DOCKER_IMAGE_NAME
    else
      local repo="$([ $choice == 2 ] && echo "portal" || echo "portal-feature")"
      printf "Enter the tag of the image in the registry.\n"
      printf "For example, \"1806.0044\" in \"$DOCKER_REGISTRY/$repo:1806.0044\"\n"
      printf "tag: "
      read tag
      DOCKER_IMAGE_NAME="$DOCKER_REGISTRY/$repo:$tag"
      echo " "
    fi
  fi

  if [ $BLAST_IMAGE ] ; then
    if docker_image_exists "$DOCKER_IMAGE_NAME" ; then
      dockerRMI "$DOCKER_IMAGE_NAME"
    fi
  fi

  [ $? -gt 0 ] && return $?

  checkRegistry $DOCKER_REGISTRY $DOCKER_IMAGE_NAME 

  [ $? -gt 0 ] && return $?

  local OUTPUT=$ROOT_DIR/last-docker-run.sh
  local mongodatamount="-v $NODEAPP_DATA_DIR:/data/db"
  local envfile="$ROOT_DIR/environment.variables.final.env"

  # Mongo uses fdatasync on its data store which is something not supported by windows NTFS
  # SEE: https://github.com/docker-library/mongo/issues/243
  # Therefore, mongo data will not be mounted running localhost so be aware that data will not survive container deletion.
  # An alternative would be to specify a MONGODB_URI that is not localhost.
  isWindows && mongodatamount=""

  # If mongo is not running locally, then no need to mount a data directory
  ! isLocalHost "$MONGODB_URI" && mongodatamount=""

	cat <<-EOF | sed 's/\t\+/\n/g' > $OUTPUT
	docker run \
		-d \
		-p 3005:3005 \
		--restart unless-stopped \
		--name=$CONTAINER_NAME \
		-v $(getOSPath $NODEAPP_CONFIG_DIR):/var/portal-config $mongodatamount \
		-e "START_CMD=$START_CMD" \
		--env-file $(getOSPath $ROOT_DIR/environment.variables.final.env) \
		$DOCKER_IMAGE_NAME
	EOF

  [ $DRYRUN ] && echo "DRYRUN:"
  local CMD="$(cat $OUTPUT)"
  cat $OUTPUT | sed 's/\n/ \//g'
  [ ! $DRYRUN ] && eval $CMD
}


# Make a file that can be referenced by the research-portal docker container using the --env-file parameter
processEnvironmentVariablesFile() {
  # Declare all local variables
  local envfile="environment.variables.final.env"
  local localhost="$(isLocalHost $PORTAL_HOST && echo 'true')"
  local localmongo="mongodb://localhost:27017/research-portal"

  # 1) If an environment variable file already exists, ask the user if they want to replace it.
  if [ $INTERACTIVE ] ; then
    if [ -f ${ROOT_DIR}/$envfile ] ; then
      printf "\nFound ${ROOT_DIR}/$envfile.\nYou can refresh it.\n"
      if ! askYesNo "$S3_QUESTION" ; then
        return 0
      else
        local s3pull="true"
      fi
      echo " "
    fi
  fi

  # 2) Create the environment variables file
  [ ! $INTERACTIVE ] && [ ! $localhost ] && local s3pull="true"
  local landscape="$LANDSCAPE"
  [ -z "$landscape" ] && landscape="$CONFIG_LANDSCAPE"
  createEnvironmentVariablesFile \
    "landscape=$landscape" \
    "appname=portal" \
    "interactive=$INTERACTIVE" \
    "s3pull=$s3pull" \
    "rootdir=$ROOT_DIR" \
    "localhost=$localhost"
  [ -z "$landscape" ] && CONFIG_LANDSCAPE=$get_landscape_retval

  # 3) Exit the function if there is no final environment variables file.
  if [ ! -f "$ROOT_DIR/$envfile" ] && [ ! $localhost ] ; then
    echo "ERROR! processEnvironmentVariablesFile: Could not create $ROOT_DIR/${envfile}!"
    return 1
  fi

  # 4) Check the final environment variables file for required properties.
  local isInteractive="$([ $INTERACTIVE ] && echo TRUE || echo FALSE)"
  local propfile="PROPFILE=$ROOT_DIR/$envfile"
    # a) If the property cannot be determined, don't even prompt for it - just fail immediately
  if ! insertConfigToPropertyFile "PROPNAME=PORTAL_HOST" $propfile "CONTINUE=FALSE" ; then return 1 ; fi
  if ! insertConfigToPropertyFile "PROPNAME=CORE_AUTH_BASE_URL" $propfile "CONTINUE=FALSE" ; then return 1 ; fi
  if ! insertConfigToPropertyFile "PROPNAME=RESEARCH_URL" $propfile "CONTINUE=FALSE" ; then return 1 ; fi
    # b) If the property cannot be determined, keep going - the user should be prompted for it further down
  if ! insertConfigToPropertyFile "PROPNAME=START_CMD" $propfile "CONTINUE=$isInteractive" ; then return 1 ; fi
  if ! insertConfigToPropertyFile "PROPNAME=MONGODB_URI" $propfile "CONTINUE=$isInteractive" ; then return 1 ; fi
  if ! insertConfigToPropertyFile "PROPNAME=RESEARCH_SECRET" $propfile "CONTINUE=$isInteractive" ; then return 1 ; fi
    # c) If the property cannot be determined, don't prompt for a value, just assign a default.
  if ! insertConfigToPropertyFile "PROPNAME=CACHE_RES_REQUESTS" $propfile "DEFAULTVAL=true" ; then return 1 ; fi
  if ! insertConfigToPropertyFile "PROPNAME=LOG_LEVEL" $propfile "DEFAULTVAL=debug" ; then return 1 ; fi
  if ! insertConfigToPropertyFile "PROPNAME=NODE_ENV" $propfile "DEFAULTVAL=development" ; then return 1 ; fi
  if ! insertConfigToPropertyFile "PROPNAME=PORT" $propfile "DEFAULTVAL=3005" ; then return 1 ; fi

  if [ $INTERACTIVE ] ; then
    if ! propertyExistsInFile "MONGODB_URI" "$ROOT_DIR/$envfile" ; then
      while [ -z "$MONGODB_URI" ] ; do
        prompt_for_numbered_choice "Mongo connection URI?" "$localmongo" "other..."
        local choice=$?
        if [ $choice -eq 1 ] ; then
          MONGODB_URI="$localmongo"
        else
          read -p "Enter the URI: " MONGODB_URI
        fi
      done
      echo "MONGODB_URI=$MONGODB_URI" >> $ROOT_DIR/$envfile
    fi

    # Would prompt for START_CMD here, but this will happen in setStartMode()
    ! promptToPropertyFile "PROPNAME=MONGODB_URI" "PROPFILE=$ROOT_DIR/$envfile" && return 1
    ! promptToPropertyFile "PROPNAME=RESEARCH_SECRET" "PROPFILE=$ROOT_DIR/$envfile" && return 1
  else
    [ -z "$START_CMD" ] && echo "ERROR! processEnvironmentVariablesFile: START_CMD not set!" && return 1
    [ -z "$MONGODB_URI" ] && echo "ERROR! processEnvironmentVariablesFile: MONGODB_URI not set!" && return 1
    [ -z "$RESEARCH_SECRET" ] && echo "ERROR! processEnvironmentVariablesFile: SA_SECRET not set!" && return 1
  fi

  # The portal app expects all parameters to be part of the URI (including user and password), so merge them if separate.
  local uri="$(getPropertyFromFile "MONGODB_URI" "$ROOT_DIR/$envfile")"
  local user="$(getPropertyFromFile "MONGODB_USERNAME" "$ROOT_DIR/$envfile")"
  local pswd="$(getPropertyFromFile "MONGODB_PASSWORD" "$ROOT_DIR/$envfile")"
  uri=$(getExtendedMongoURI "$uri" "$user" "$pswd")
  echo "MONGODB_URI=$uri" >> $ROOT_DIR/$envfile

  # Add environment variables for AWS access so the container can make S3 or ECR calls if it wants to.
  awsConfigsToPropertyFile "$ROOT_DIR/$envfile"
}


# If a separate user and pwd are provided for mongo connection, uri encode them and incorporate them into the main uri.
getExtendedMongoURI() {
  local uri="$1"
  local user="$2"
  local pswd="$3"
  if [ -n "$user" ] && [ -n "$pswd" ] ; then
    local numAmp=$(echo "$uri" | awk -F@ '{print NF}')
    if [ $numAmp -eq 1 ] ; then
      # There is no "@" character already in the main uri
      if ! NodeInstalled ; then
        curl --silent --location https://rpm.nodesource.com/setup_8.x | bash - && \
        yum install -y nodejs > /dev/null
      fi
      if NodeInstalled ; then
        local js="var qs = require('querystring'); console.log(qs.escape('${user}'));"
        user=$(node -e "$js")
        js="var qs = require('querystring'); console.log(qs.escape('${pswd}'));"
        pswd=$(node -e "$js")
        local numDelim=$(echo "$uri" | awk -F// '{print NF}')
        if [ $numDelim -eq 2 ] ; then
          local start=$(echo "$uri" | awk -F// '{print $1}')
          local credentials="${user}:${pswd}"
          uri=$(echo "$uri" | awk -F// '{print $2}')
          uri="${start}//${credentials}@${uri}"
        fi
      fi
    fi
  fi
  echo "$uri"
}


# Build everything from scratch. This begins with the docker image, following all the way
# through to running the docker container.
deploy() {

  initialize "$@"
  [ $? -eq 1 ] && DEPLOYING="" && return 1

  DEPLOYING=true

  build
  [ $? -eq 1 ] && DEPLOYING="" && return 1

  runapp
  [ $? -eq 1 ] && DEPLOYING="" && return 1

  DEPLOYING=""

  return 0  
}


# Prompt for the user for the environment we are currently running in and set the reply in a session variable.
setHostUrl() {
  if [ ! $INTERACTIVE ] ; then
    [ -z "$PORTAL_HOST" ] && PORTAL_HOST=$(getPropertyFromFile 'PORTAL_HOST' "$ROOT_DIR/environment.variables.final.env")
    [ -z "$PORTAL_HOST" ] && echo "ERROR! PORTAL_HOST not set" && return 1
    return 0;
  fi
  [ -n "$PORTAL_HOST" ] && return 0;

  local url=(
    localhost
    localhost:3005
    kuali-research-sb.bu.edu
    kuali-research-ci.bu.edu
    kuali-research-qa.bu.edu
    kuali-research-stg.bu.edu
    kuali-research.bu.edu
    other...
  )
  local landscape=("" "" "sb" "ci" "qa" "stg" "prod")
  while true; do
    question=$(cat <<-EOF
		How is research-portal reached from the browser? [enter numeral 1-8]:
		   1) ${url[0]}
		   2) ${url[1]}
		   3) ${url[2]}
		   4) ${url[3]}
		   5) ${url[4]}
		   6) ${url[5]}
		   7) ${url[6]}
		   8) ${url[7]}
		:
		EOF
    )
    echo " "
    read -p "$question" answer
    if [ -n "$(echo $answer | grep -P ^[1-8]$)" ] ; then
      PORTAL_HOST=${url[$(($answer - 1))]}
      LANDSCAPE=${landscape[$(($answer - 1))]}
      if [ $answer == "8" ] ; then
        echo " "
        read -p "Enter the url: " PORTAL_HOST
      fi
      if [ $answer == "1" ] || [ $answer == "2" ] ; then
        # PORTAL_HOST=127.0.0.1
        PORTAL_HOST="localhost"
        ensureHasProperty "PORTAL_HOST=$PORTAL_HOST" "$ROOT_DIR/environment.variables.local.env"
        if [ $answer == "2" ] ; then
          PORT=3005
          ensureHasProperty "PORT=$PORT" "$ROOT_DIR/environment.variables.local.env"
        fi
      fi
      break;
    else
      echo "Please enter single numeral 1 to 7"
    fi
  done

  return 0
}


# Prompt the user for how npm is to be started. Options include startup method for remote debugging with the chrome browser.
setStartMode() {
  [ $? -gt 0 ] && return $?

  if [ ! $INTERACTIVE ] ; then
    [ -z "$START_CMD" ] && START_CMD=$(getPropertyFromFile 'START_CMD' "$ROOT_DIR/environment.variables.final.env")
    [ -z "$START_CMD" ] && echo "ERROR! START_CMD not set!" && return 1
    return 0;
  fi
  [ -n "$START_CMD" ] && return 0;

  local modes=(
    'npm start_dev'
    'npm start_debug'
    'npm start_dev_no_json'
    'npm start'
    'node --inspect=0.0.0.0:9229 /var/portal/server/index.js'
    'other...'
  )
  while true; do
    question=$(cat <<-EOF
		In what mode do you want to start node? [enter numeral 1-6, see package.json for details]:
		   1) ${modes[0]}
		   2) ${modes[1]}
		   3) ${modes[2]}
		   4) ${modes[3]}
		   5) ${modes[4]}
		   6) ${modes[5]}
		:
		EOF
    )
    echo " "
    read -p "$question" answer
    if [ -n "$(echo $answer | grep -P ^[1-6]$)" ] ; then
      START_CMD=${modes[$(($answer - 1))]}
      if [ $answer == "6" ] ; then
        echo " "
        read -p "Enter the start mode: " START_CMD
      fi
      # Make the process we start output to log files, else docker logs.
      # START_CMD="$START_CMD > portal-std.log 2> portal-err.log"
      break;
    else
      echo "Please enter single numeral 1 to 6"
    fi
  done

  [ -z "$START_CMD" ] && echo "ERROR! START_CMD not set!" && return 1

  ensureHasProperty "START_CMD=$START_CMD" "$ROOT_DIR/environment.variables.final.env"

  return 0
}


# Make sure the startup.sh file is written/overwritten to docker mounted dir.
copyStartupFile() {
  copyConfigFile \
    "filename=startup.sh" \
    "filesrc=$ROOT_DIR" \
    "filedest=$NODEAPP_SCRIPTS_DIR" \
    "appname=portal" \
    "landscape=$LANDSCAPE" \
    "interactive=$INTERACTIVE" \
    "overwrite=true" \
    "skips3=true"

  return $?
}


# "Clean" the docker build context to return to a state as if it had just been cloned from the
# git repository. Pass "all", and any files downloaded from s3 will also be removed.
clean() {
  if [ "$(getCurrentDirectoryName)" != "build.context" ] || [ "$(getParentDirectoryName)" != "research-portal" ] ; then
    echo "Please navigate to research-portal/build.context from the root of the git repo in order to clean the docker build context!"
    return 1
  fi

  PORTAL_HOST=
  unalias rm 2> /dev/null || true
  rm -f environment.variables.final.env
  rm -f environment.variables.local.env
  rm -f bash.lib.sh
  rm -f buildhelper.sh
  rm -rf config
  rm -rf scripts
  rm -rf mongo-data

  if [ "${1,,}" == "all" ] ; then
    rm -f environment.variables.s3.env
    rm -f bu_github_id_*
  fi
}
