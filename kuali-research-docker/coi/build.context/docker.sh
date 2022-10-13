#!/bin/bash

#################################################################################################
# 
# This script is used to run each of the steps required to go from a docker build context
# and end up with a built coi docker image and container running from it. 
# This script serves as both automation and documentation.
# COI runs against an oracle database server, which means that the docker image is based on a 
# on a mysql docker image and mysql is demoted by oracle client drivers.
#
# Example function calls:
# 
# Each of the following can be run separately... 
#    source docker.sh && initialize oracle
#    build oracle
#    runapp oracle
# 
# or...
#    source docker.sh && deploy
#
# NOTE: Directories for mounting to the container and locating configuration files will be created within a subdirectory in the docker build context.
# The docker build context directory is specified with the "ROOT_DIR" argument, but if this is not specified, 
# it will be assumed to be the current directory.
# Also, before running this script, make sure you add the following to the docker build context:
#    1) The database connection file "knexfile.js" corresponding to the environment/landscape
#    2) OPTIONAL if s3 is accessible: Environment variable configuration file(s). See below.
#    3) The private SSH key for gaining pull access to the BU git repo where the codebase is stored (If building image).
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
# any of a set of parameter names, else they are ignored. Paremeter names are case insensitive.
parseArgs() {

   EXPECTED=(
      'ROOT_DIR'
      'INTERACTIVE'
      'NO_BUILD_INIT'
      'NODEAPP_CONFIG_DIR'
      'NODEAPP_ATTACHMENTS_DIR'
      'NODEAPP_BROWSER_PORT'
      'MIGRATIONS_TO_RUN'
      'AWS_ACCESS_KEY_ID'
      'AWS_SECRET_ACCESS_KEY'
      'AWS_DEFAULT_REGION'
      'AWS_PROFILE'
      'GIT_RSA_KEY'
      'GIT_REPULL'
      'GIT_BRANCH'
      'GIT_REFSPEC'
      'START_CMD'
      'BU_COI_URL'
      'CORE_HOST'
      'CORE_PORT'
      'AUTH_ENABLED'
      'LANDSCAPE'
      'CONFIG_LANDSCAPE'
      'DOCKER_IMAGE_NAME'
      'DOCKER_REGISTRY'
      'RENEW_CONFIGS'
      'BLAST_CONTAINER'
      'BLAST_IMAGE'
      'DRYRUN'
      'CONTAINER_NAME'
      'KEEP_DANGLING'
   )

  args_to_session_variables "${#EXPECTED[@]}" "${EXPECTED[@]}" "$@"
}

# Set all local and environment variables and check that all the necessary files
# are present that the build process requires.
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
  [ -z "$DOCKER_IMAGE_NAME" ] && DOCKER_IMAGE_NAME=bu-ist/coi
  [ -z "$DOCKER_REGISTRY" ] && DOCKER_REGISTRY="730096353738.dkr.ecr.us-east-1.amazonaws.com"
  [ -z "$CONTAINER_NAME" ] && CONTAINER_NAME="coi"
  get_landscape_retval=""
  true
}

defaults


setDirectories() {

  echo "Setting directories..."

  if [ ! -d $ROOT_DIR ] ; then
    echo "$ROOT_DIR for root coi directory not found! Using $(pwd)"
    ROOT_DIR="$(pwd)";
  else
    echo "Found $ROOT_DIR"
  fi

  [ -z "$NODEAPP_CONFIG_DIR" ] && NODEAPP_CONFIG_DIR="$ROOT_DIR/config"
  [ -z "$NODEAPP_SCRIPTS_DIR" ] && NODEAPP_SCRIPTS_DIR="$ROOT_DIR/scripts"
  [ -z "$NODEAPP_ATTACHMENTS_DIR" ] && NODEAPP_ATTACHMENTS_DIR="$ROOT_DIR/attachments"

  return 0
}


# Set all local and environment variables and check that all the necessary files
# are present that the build process requires.
initialize() {

  [ $DEPLOYING ] && return 0

  echo "Initializing..."
  
  if [ ! -f ../../bash.lib.sh ] ; then
    echo "CANCELLING DUE TO MISSING SCRIPT FILE: ../../bash.lib.sh"
    return 1;
  elif ! parseArgs "$@" ; then 
    echo "CANCELLING DUE TO INVALID PARAMETER(S)";
    return 1;
  fi

  defaults || return 1
  
  setDirectories || return 1
  
  checkdir $NODEAPP_CONFIG_DIR
  checkdir $NODEAPP_SCRIPTS_DIR
  checkdir $NODEAPP_ATTACHMENTS_DIR

  return 0
}


# Build the docker image
build() {
 
  if ! parseArgs "$@" ; then
    echo "CANCELLING DUE TO INVALID PARAMETER(S)";
    return 1
  fi

  printHeader "BUILDING DOCKER IMAGES..."

  initialize "$@"
  [ $? -eq 1 ] && echo "ERROR! initialize failure!" && return 1

  # The bash lib and build helper files needs to be in the docker build context dir because they get copied into the image when building.
  unalias cp 2> /dev/null || true
  cp ../../bash.lib.sh .
  cp ../../buildhelper.sh .

  [ ! -f bash.lib.sh ] && echo "Cancelling build! bash.lib.sh not in build context!" && return 1
  [ ! -f buildhelper.sh ] && echo "Cancelling build! buildhelper.sh not in build context!" && return 1

  # Make sure the expected git rsa key(s) are in the build context directory, pulling them from s3 if not found.
  local keys=(
    'bu_github_id_coi_rsa'
    'bu_github_id_kualiui_rsa'
    'bu_github_id_core_common_rsa'
    'bu_github_id_formbot_rsa'
    'bu_github_id_cor_formbot_gadgets_rsa')
  getGitKeys 'kuali-research-ec2-setup' "$(pwd)" "${keys[@]}"
  [ $? -eq 1 ] && return 1

  setGitRepull

  setGitRefspec

  echo "Building image $DOCKER_IMAGE_NAME ..."
  local CMD="docker build -t $DOCKER_IMAGE_NAME --network=host"
  
  [ -n "$GIT_REFSPEC" ] && CMD="${CMD} --build-arg GIT_REFSPEC=${GIT_REFSPEC} --build-arg=GIT_BRANCH=${GIT_BRANCH}"

  [ -f LAST_IMAGE_BUILD_DATE ] && CMD="${CMD} --build-arg DATETIME=$(cat LAST_IMAGE_BUILD_DATE)"

  CMD="${CMD} ."

  GIT_REPULL=""
  GIT_REFSPEC=""
  GIT_BRANCH=""

  [ $DRYRUN ] && echo "DRYRUN:"
  echo $CMD
  if [ ! $DRYRUN ] ; then
    eval $CMD
    local retval=$?
    if [ "${KEEP_DANGLING,,}" != "true" ] ; then
      # Don't clean dangling images if the build failed. Might need to run a container off one of the dangling layers to debug.
      [ $retval -eq 0 ] && cleanDanglingDockerImages
    fi
  fi
  unalias rm 2> /dev/null || true
  rm -f bash.lib.sh
  rm -f buildhelper.sh

  return 0 
}

# Run the docker container
runapp() {

  printHeader "RUNNING APP CONTAINER..."

  # 1) Initialize variables, prompting user for some of them.

  initialize "$@"
  [ $? -eq 1 ] && return 1

  [ -z "$DOCKER_IMAGE_NAME" ] && [ ! $INTERACTIVE ] && echo "ERROR! Missing the name of the docker image." && return 1

  if ! checkContainer "name=$CONTAINER_NAME" "interactive=$INTERACTIVE"  "blast_container=$BLAST_CONTAINER"; then
    if [ $DRYRUN ] ; then
      echo "DRYRUN: Prompting the user to remove existing container '$CONTAINER_NAME'"
    else
      [ $INTERACTIVE ] && return 0
      echo "ERROR! Container '$CONTAINER_NAME' already exists. stop and/or remove and try again."
      return 1
    fi
  fi

  setHostUrl || return 1

  processEnvironmentVariablesFile || return 1

  askRenewConfigs "knexfile.js" || return 1

  copyStartupFile || return 1

  # copyKnexFile || return 1

  setStartMode || return 1

  setMigrations || return 1

  setAttachmentsDir

  if sudo_exists ; then
    sudo chmod -R 777 $ROOT_DIR
  else
    chmod -R 777 $ROOT_DIR
  fi

  if [ "${MIGRATIONS_TO_RUN^^}" == "ALL" ] ; then
    local MIGR_ENV='-e "MIGRATIONS_TO_RUN=all"'
  fi

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
      local repo="$([ $choice == 2 ] && echo "coi" || echo "coi-feature")"
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

  checkRegistry $DOCKER_REGISTRY $DOCKER_IMAGE_NAME

  # 2) Run the coi container
  local OUTPUT=$ROOT_DIR/last-docker-oracle-run.sh

		cat <<-EOF | sed 's/\t\+/\n/g' > $OUTPUT
		docker run \
			-d \
			-p 8092:8090 \
			-p 9228:9229 \
			-p 1521:1521 \
			--restart unless-stopped \
			--name $CONTAINER_NAME \
			-v $(getOSPath $NODEAPP_CONFIG_DIR):/var/research-coi-config \
			-v $(getOSPath $NODEAPP_ATTACHMENTS_DIR):/var/research-coi-uploads \
			-e "START_CMD=$START_CMD" $MIGR_ENV \
			--env-file $ROOT_DIR/environment.variables.final.env \
			$DOCKER_IMAGE_NAME
		EOF

  [ $DRYRUN ] && echo "DRYRUN:"
  local CMD="$(cat $OUTPUT)"
  cat $OUTPUT | sed 's/\n/ \//g'
  [ ! $DRYRUN ] && eval $CMD

  return 0
}


# Make a file that can be referenced by the coi docker container using the --env-file parameter
processEnvironmentVariablesFile(){

  # Declare all local variables
  local envfile="environment.variables.final.env"
  local localhost="$(isLocalHost $CORE_HOST && echo 'true')"

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
    "appname=coi" \
    "interactive=$INTERACTIVE" \
    "s3pull=$s3pull" \
    "rootdir=$ROOT_DIR" \
    "localhost=$localhost"

  # 3) Exit the function if there is no final environment variables file or CORE_HOST is missing.
  if [ ! -f $ROOT_DIR/$envfile ] && [ ! $localhost ] ; then
    echo "ERROR! processEnvironmentVariablesFile: Could not create $ROOT_DIR/${envfile}!"
    return 1
  fi

  local propfile="PROPFILE=$ROOT_DIR/$envfile"
  if ! insertConfigToPropertyFile "PROPNAME=AUTH_URL" $propfile "CONTINUE=TRUE" ; then return 1 ; fi

  if [ -z "$CORE_HOST" ] ; then
    [ -z "$AUTH_URL" ] && AUTH_URL=$(getPropertyFromFile "AUTH_URL" "$ROOT_DIR/$envfile")    
    if [ -n "$AUTH_URL" ] ; then
      # Assume that CORE_HOST will be the AUTH_URL (minus any port specification that might exist).
      CORE_HOST=$(echo -n "$AUTH_URL" | cut -d':' -f1)
    fi
    if [ -z "$CORE_HOST" ] ; then
      echo "ERROR! AUTH_URL and CORE_HOST not set!"
      return 1
    fi
  fi

  # 4) Add the environment variable for the core url to the file if it is missing from the environment variables file
  if ! propertyExistsInFile 'AUTH_URL' $ROOT_DIR/$envfile ; then
    if [ -z "$AUTH_URL" ] ; then
      local authurl="$CORE_HOST"
      # Assume the AUTH_URL is the CORE_HOST plus any port if specified with CORE_PORT.
      [ -n "$CORE_PORT" ] && authurl="${authurl}:${CORE_PORT}"
    fi
    echo "AUTH_URL=$authurl" >> $ROOT_DIR/$envfile
  fi

  # 5) Add the environment variable for the coi url if it is missing from the environment variables file
  if ! propertyExistsInFile 'BU_COI_URL' $ROOT_DIR/$envfile ; then
    local buCoiUrl="$BU_COI_URL"
    [ -z "$buCoiUrl" ] && buCoiUrl=$CORE_HOST
    [ -n "$NODEAPP_BROWSER_PORT" ] && buCoiUrl="${buCoiUrl}:${NODEAPP_BROWSER_PORT}"
    [ -z "$BU_COI_URL" ] && buCoiUrl="${buCoiUrl}/coi/"
    echo "BU_COI_URL=$buCoiUrl" >> $ROOT_DIR/$envfile
  fi

  # 6) Add the environment variable for the kc url if it is missing from the environment variables file
  if ! propertyExistsInFile 'RESEARCH_CORE_URL' $ROOT_DIR/$envfile ; then
    echo "RESEARCH_CORE_URL=https://$CORE_HOST/kc" >> $ROOT_DIR/$envfile
  fi

  # 7) Add environment variables for AWS access so the container can make S3 or ECR calls if it wants to.
  awsConfigsToPropertyFile "$ROOT_DIR/$envfile"

  return 0
}


# Carry out all steps (build docker image, run database, run docker container)
deploy() {

  initialize "$@"
  [ $? -eq 1 ] && DEPLOYING="" && return 1

  DEPLOYING=true

  build $1
  [ $? -eq 1 ] && DEPLOYING="" && return 1

  runapp $1
  [ $? -eq 1 ] && DEPLOYING="" && return 1

  DEPLOYING=""

  return 0
}


copyKnexFile() {

  copyConfigFile \
    "filename=knexfile.js" \
    "filesrc=$ROOT_DIR" \
    "filedest=$NODEAPP_CONFIG_DIR" \
    "appname=coi" \
    "landscape=$LANDSCAPE" \
    "interactive=$INTERACTIVE" \
    "overwrite=$RENEW_CONFIGS"

  # If a knexfile.js file still eludes the deployment directory, return 1, indicating to exit out early.
  if [ ! -f $NODEAPP_CONFIG_DIR/knexfile.js ] ; then
    if canBuildKnexFile ; then
      # We have enough information to build a knexfile.js file from scratch.
			cat <<-EOF > $NODEAPP_CONFIG_DIR/knexfile.js
			module.exports = {
			  kc_coi: {
			    client: '$DB_PACKAGE',
			    connection: {
			      host: '$DB_HOST',
			      port: '$DB_PORT',
			      database: '$DB_NAME',
			      user: '$DB_USER',
			      password: '$DB_PASSWORD'
			    }
			  },
			  pool: {
			    min: 2,
			    max: 20
			  }
			};
			EOF
    else
      echo "ERROR! No knexfile.js available for oracle!"
      return 1;
    fi
  else
    # Make sure the environment variables file gets an entry for each of the following database connection parameters.
    local knexfile="$NODEAPP_CONFIG_DIR/knexfile.js"
    local envfile="$ROOT_DIR/environment.variables.final.env"

    ensureDatabaseParm 'DB_NAME' 'database' $knexfile $envfile
    [ $? -eq 1 ] && return 1
    
    ensureDatabaseParm 'DB_USER' 'user' $knexfile $envfile
    [ $? -eq 1 ] && return 1
    
    ensureDatabaseParm 'DB_PACKAGE' 'client' $knexfile $envfile
    [ $? -eq 1 ] && return 1
    
    ensureDatabaseParm 'DB_HOST' 'host' $knexfile $envfile
    [ $? -eq 1 ] && return 1
    
    ensureDatabaseParm 'DB_PORT' 'port' $knexfile $envfile
    [ $? -eq 1 ] && return 1  
    
    ensureDatabaseParm 'DB_PASSWORD' 'password' $knexfile $envfile
    [ $? -eq 1 ] && return 1
  fi

  # Change exist status back to zero here because failing condition [ $? -eq 1 ] would have reset it to 1
  return 0
}

# Indicate if enough environment variables have been set to build a knexfile from scratch.
canBuildKnexFile() {
  if [ -n "$DB_NAME" ] && \
    [ -n "$DB_USER" ] && \
    [ -n "$DB_PACKAGE" ] && \
    [ -n "$DB_HOST" ] && \
    [ -n "$DB_PORT" ] && \
    [ -n "$DB_PASSWORD" ] ; then

    true;
    return;
  fi

  false;
}


# Ensure the environment variables file that the docker container starts up against contains an
# entry for the specified property name (arg1) If not, and the property name does not exist as 
# an environment variable, search the knexfile (arg3) for the value under the specified name (arg2).
ensureDatabaseParm() {
  local envvar="$1"
  local knexvar="$2"
  local knexfile="$3"
  local envfile="$4"

  [ ! -f "$envfile" ] && echo "ERROR! ensureDatabaseParm: $envfile does not exist" && return 1
  local envvarval=$(eval "echo -n \$$envvar")

  if ! propertyExistsInFile $envvar $envfile; then
    if [ -n "$envvarval" ] ; then
      # The sought property exists as an environment variable, so add it to the environment variables file.
      echo "${envvar}=${envvarval}" >> $envfile
    elif [ -f $knexfile ] ; then
      # The sought property is not an environment variable nor is it in the environmnet variables file.
      envvarval=$(cat $knexfile | grep ${knexvar}: | sed "s/['\"\\x20\\x9,]//g" | sed "s/${knexvar}://")
      if [ -n "$envvarval" ] ; then
        # The sought property was found in the knexfile.js, so add it to the environment variables file.
        echo "${envvar}=${envvarval}" >> $envfile
      else
        echo "ERROR! ensureDatabaseParm: Cannot find $knexvar in $knexfile for $envvar" && return 1
      fi
    else
      echo "ERROR! ensureDatabaseParm: Cannot determine $envvar ($knexfile does not exist)" && return 1
    fi
  fi

  return 0

}

# Make sure the startup.sh file is written/overwritten to docker mounted dir.
copyStartupFile() {

  copyConfigFile \
    "filename=startup.sh" \
    "filesrc=$ROOT_DIR" \
    "filedest=$NODEAPP_SCRIPTS_DIR" \
    "appname=core" \
    "landscape=$LANDSCAPE" \
    "interactive=$INTERACTIVE" \
    "overwrite=$RENEW_CONFIGS" \
    "skips3=true"

  [ $? -eq 1 ] && return 1

  chmod 777 $NODEAPP_SCRIPTS_DIR/startup.sh
  return 0
}


# Prompt for the user for the environment we are currently running in and set the reply in a session variable.
setHostUrl() {
  if [ ! $INTERACTIVE ] ; then
    [ -z "$CORE_HOST" ] && [ -z "$START_CMD" ] && CORE_HOST=$(getPropertyFromFile 'CORE_HOST' "$ROOT_DIR/environment.variables.final.env")
    [ -z "$CORE_HOST" ] && echo "ERROR! CORE_HOST not set" && return 1
    return 0;
  fi
  [ -n "$CORE_HOST" ] && return 0;

  local url=(
    localhost
    localhost:8090
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
		How is the coi host reached from the browser? [enter numeral 1-8]:
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
      CORE_HOST=${url[$(($answer - 1))]}
      LANDSCAPE=${landscape[$(($answer - 1))]}
      if [ $answer == "8" ] ; then
        echo " "
        read -p "Enter the url: " CORE_HOST 
      fi
      if [ $answer == "1" ] || [ $answer == "2" ] ; then
        #CORE_HOST=127.0.0.1
        CORE_HOST="localhost"
        ensureHasProperty "CORE_HOST=$CORE_HOST" "$ROOT_DIR/environment.variables.local.env"
        if [ $answer == "2" ] ; then
          CORE_PORT=3000
          ensureHasProperty "CORE_PORT=$CORE_PORT" "$ROOT_DIR/environment.variables.local.env"
        fi
        echo " "
        if askYesNo "In the browser, is coi reachable on port 8090?" ; then
          NODEAPP_BROWSER_PORT=8090;
        else
          while true; do
            echo " "
            read -p "Enter the port: " NODEAPP_BROWSER_PORT
            if [ -z "$(echo $NODEAPP_BROWSER_PORT | grep -P '^\d{2,4}$')" ] ; then
              echo "INVALID ENTRY! Port must be numeric and between 2 to 4 characters long";
            else
              break;
            fi
          done
        fi
        ensureHasProperty "NODEAPP_BROWSER_PORT=$NODEAPP_BROWSER_PORT" "$ROOT_DIR/environment.variables.local.env"
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
  if [ ! $INTERACTIVE ] ; then
    [ -z "$START_CMD" ] && START_CMD=$(getPropertyFromFile 'START_CMD' "$ROOT_DIR/environment.variables.final.env")
    [ -z "$START_CMD" ] && echo "ERROR! START_CMD not set!" && return 1
    return 0;
  fi
  [ -n "$START_CMD" ] && return 0;

  local modes=(
    'npm run start'
    'npm run start_dev'
    'npm run start_prod'
    'node --inspect=0.0.0.0:9229 /var/research-coi/start-server.js'
    'other...'
   )
   while true; do
                question=$(cat <<-EOF
		In what mode do you want to start node? [enter numeral 1-5, see package.json for details]:
		   1) ${modes[0]}
		   2) ${modes[1]}
		   3) ${modes[2]}
		   4) ${modes[3]}
		   5) ${modes[4]}
		:
		EOF
		)
  echo " "
  read -p "$question" answer
  if [ -n "$(echo $answer | grep -P ^[1-5]$)" ] ; then
    START_CMD=${modes[$(($answer - 1))]}
    if [ $answer == "5" ] ; then
      echo " "
      read -p "Enter the start mode: " START_CMD
    fi
      # Make the process we start output to log files, else docker logs.
      # START_CMD="$START_CMD > coi-std.log 2> coi-err.log"
      break;
    else
      echo "Please enter single numeral 1 to 5"
    fi
  done

  [ -z "$START_CMD" ] && echo "ERROR! START_CMD not set!" && return 1

  return 0
}

# Prompt user for migrations, else default is 'none'
setMigrations() {
  if [ ! $INTERACTIVE ] ; then 
    [ -z "$MIGRATIONS_TO_RUN" ] && MIGRATIONS_TO_RUN=none 
    return 0;
  fi
  [ -n "$MIGRATIONS_TO_RUN" ] && return 0;
 
  echo " "
  if askYesNo "Apply Migrations [y/n]? (This will create the tables and insert bootstrap data needed for the application to run)." ; then
    MIGRATIONS_TO_RUN=all
  fi
  
  return 0
}

# Make sure the directory for mounting to the container for attachments is specified in the environment variables file
setAttachmentsDir() {
  local envfile="$ROOT_DIR/environment.variables.final.env"
  if ! propertyExistsInFile "LOCAL_FILE_DESTINATION" $envfile; then
    [ -z "$LOCAL_FILE_DESTINATION" ] && LOCAL_FILE_DESTINATION="/var/research-coi-uploads"
    echo "LOCAL_FILE_DESTINATION=$LOCAL_FILE_DESTINATION" >> "$envfile"
  fi

  return 0
}  
