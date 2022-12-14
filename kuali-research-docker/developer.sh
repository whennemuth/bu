source bash.lib.sh

REGISTRY='730096353738.dkr.ecr.us-east-1.amazonaws.com'

# This is the starting point.
# If you include any of the following values as parameters, then the operation will be limited
# to building and/or running the specified applications ("core", "coi", "kc")
# If no parameters are supplied, then a prompt will ask for the information at runtime.
run() {
 
  ! softwareInstalled && return 1

  ! softwareConfigured && return 1

  ! setBuildParms "$@" && return 1

  startApps

  if [ $? -eq 0 ] ; then
    echo "FINISHED SUCCESSFULLY!"
  fi
}


softwareInstalled() {

  printHeader "Checking required software installed"
  local cancel

  echo "Checking docker installed..."
  if ! DockerInstalled ; then
    printf "Docker does not seem to be installed!\nDirections to install:\n"
    
    if isWindows ; then
      printf "   https://docs.docker.com/docker-for-windows/install/" && false
    elif isMac ; then
      printf "   https://docs.docker.com/docker-for-mac/install/" && false
    elif isLinux ; then
      printf "   https://runnable.com/docker/install-docker-on-linux" && false
    else
      printf "Unknown operating system: $(echo $OSTYPE)"
    fi
    printf "\nInstall docker and rerun.\n"
    cancel="true"
  else
    echo "ok"
  fi
  
  echo "Checking docker running..."
  if ! DockerRunning ; then
    printf "Docker does not seem to be running!\nDid you start the docker daemon?\nStart docker and rerun. "
    cancel="true"
  else
    echo "ok"
  fi
  
  echo "Checking git installed..."
  if ! GitInstalled ; then
    printf "Git does not seem to be installed!\nDirections to install:\n"
    if isWindows ; then
      printf "   https://git-scm.com/download/win" && false
    elif isMac ; then
      printf "   https://git-scm.com/download/mac" && false
    elif isLinux ; then
      printf "   https://git-scm.com/download/linux" && false
    else
      printf "Unknown operating system: $(echo $OSTYPE)"
    fi
    printf "\nInstall git and rerun.\n"
    cancel="true"
  else
    echo "ok"
  fi

  echo "Checking AWS cli installed..."
  if ! AwsCliInstalled ; then
    printf "The AWS cli does not seem to be installed!\nDirections to install:\n"
    if isWindows ; then
      printf "   https://docs.aws.amazon.com/cli/latest/userguide/awscli-install-windows.html" && false
    elif isMac ; then
      printf "   https://docs.aws.amazon.com/cli/latest/userguide/cli-install-macos.html" && false
    elif isLinux ; then
      printf "   https://docs.aws.amazon.com/cli/latest/userguide/awscli-install-linux.html" && false
    else
      printf "Unknown operating system: $(echo $OSTYPE)"
    fi
    printf "\nInstall the AWS cli and rerun.\n"
    cancel="true"
  else
    echo "ok"
  fi

  if [ $cancel ] ; then false; else true; fi
}


softwareConfigured() {

  printHeader "Checking software properly configured"

  if ! awsCliCredentialsOk ; then
    false
  # elif ! someOtherCheck ; then
  #   false
  else
    echo "Software configured."
  fi

}


# Use the s3 command to list the contents of a bucket is a sufficient test for aws cli credentials.
awsCliCredentialsOk() {
  echo "Checking aws cli credentials by testing s3 bucket access..."
  local cancel

  # Check for access using expected profile first
  aws --profile=ecr.access s3 ls "s3://kuali-research-ec2-setup" > /dev/null 2>&1
  if [ "$?" != "0" ] ; then
    # Check for access assuming credentials are:
    #   1) In standardly named session variables
    #   2) In the default profile
    #   3) We are operating from an ec2 instance and privileges are granted with a trust relationship through a role applied to the ec2 instance.
    aws s3 ls "s3://kuali-research-ec2-setup" > /dev/null 2>&1
    if [ "$?" != "0" ] ; then
      prompt_for_numbered_choice \
        "AWS cli credentials are unknown/insufficient! What do you want to do?" \
        "Enter them here" \
        "Abort"
      if [ $? == 2 ] ; then
        cancel="true"
      elif ! promptForAwsCliCredentials ; then
        cancel="true"
      fi
    fi
  fi

  if [ $cancel ] ; then
    false;
  else
    echo "Access confirmed!"
    true;
  fi
}


promptForAwsCliCredentials() {
  while true; do
    printf "What is the aws region? (example: us-east-1):\n" && read region
    [ -n "$region" ] && break
  done
  while true; do
    printf "What is the aws access key id? (aws_access_key_id):\n" && read id
    [ -n "$id" ] && break
  done
  while true; do
    printf "What is the aws secret access key? (aws_secret_access_key):\n" && read secret
    [ -n "$secret" ] && break
  done

  aws configure set output json
  aws configure set region $region
  aws configure set aws_access_key_id $id
  aws configure set aws_secret_access_key $secret
  aws s3 ls "s3://kuali-research-ec2-setup" > /dev/null 2>&1

  if [ "$?" != 0 ] ; then
    prompt_for_numbered_choice \
      "Supplied credentials are invalid! What do you want to do?" \
      "Re-enter credentials" \
      "Abort"
    if [ $? == 1 ] ; then
      promptForAwsCliCredentials
    else
      false
    fi
  else
    # Now that we know these credentials work, make a profile out of them so we don't have to prompt for them again.
    [ ! -d ~/.aws ] && mkdir ~/.aws
    echo "" >> ~/.aws/config
    echo "[profile ecr.access]" >> ~/.aws/config
    echo "aws_access_key_id=$id" >> ~/.aws/config
    echo "aws_secret_access_key=$secret" >> ~/.aws/config
    echo "region=$region" >> ~/.aws/config
    echo "output=json" >> ~/.aws/config
    true
  fi
}

# Make sure the user is in the root directory of the git repository where this script file resides.
checkRootDirectory() {
  local ok=""
  local passes=0
  local silent="$1"
  [ -d ../kuali-research-docker ] && ((passes++))
  [ -f bash.lib.sh ] && ((passes++))
  [ -f developer.sh ]  && ((passes++))
  if [ $passes -lt 3 ] ; then
    if [ -d ../build.context ] ; then
      cd ../..
      checkRootDirectory "silent" && passes=3
    fi
  fi
  [ $passes -lt 3 ] && printf "\nYou do not appear in the git repository root directory!!!\nCancelling."
  [ $passes -eq 3 ] && true || false
}


# Prompt for which applications the user wants to run in docker containers (and optionally be built as images).
# For each app selected, gather up the parameters needed for the docker activity into arrays of name=value pairs.
# As of now, there are 3 applications that can be configured, so there can be as much as 3 arrays populated.
setBuildParms() {

  if ! checkRootDirectory ; then return 1; fi

  printHeader "What do you want to do?"

  # Reset session variables since last execution.
  coreparms=()
  kcparms=()
  coiparms=()

  for parm in $@ ; do
    [ ${parm,,} == "core" ] && local core="true"
    [ ${parm,,} == "coi" ] && local coi="true"
    # Allow 3 values to indicate kc
    [ ${parm,,} == "kc" ] && local kc="true"
    [ ${parm,,} == "kuali" ] && local kc="true"
    [ ${parm,,} == "coeus" ] && local kc="true"
    # Look for the dryrun parameter. Docker commands are printed, but not run.
    [ ${parm,,} == "dryrun" ] && local dryrun="true"
  done;
  [ ! $core ] && [ ! $coi ] && [ ! $kc ] && local askapps="true"

  if [ $core ] || ([ $askapps ] && askYesNo "Do you want to run core?") ; then
    setAppBuildParms core $dryrun
  fi

  if [ $coi ] || ([ $askapps ] && echo "" && askYesNo "Do you want to run coi?") ; then
    setAppBuildParms coi $dryrun
  fi

  if [ $kc ] || ([ $askapps ] && echo "" && askYesNo "Do you want to run kc?") ; then
    setAppBuildParms kc $dryrun
  fi
}

# Gather up the parameters needed for a docker build or run (or both) of a particular application in an array of name=value pairs
setAppBuildParms() {
  local app="$1"
  local dryrun="$2"

  if ! checkRootDirectory ; then return 1; fi

  [ $dryrun ] && addParm "$app" "DRYRUN=true"
  addParm "$app" "ROOT_DIR=$(pwd)/core/build.context"
  prompt_for_numbered_choice \
    "Parameters can be gathered from you in 2 ways:" \
    "SPARSE (recommended): Will only prompt for git details and/or docker image name - invokes default values for remainder of parameters" \
    "DETAILED: Will prompt for all parameters (Pick this over SPARSE if you intend a variation of the 'out of the box' run"
  if [ $? == 1 ] ; then
    local sparse="true"
    # NOTE: These variables will be overridden by anything found in environment.variables.final
    addParm "$app" "INTERACTIVE=false"
    addParm "$app" "CORE_HOST=localhost"
    addParm "$app" "NODEAPP_BROWSER_PORT=3000"
    addParm "$app" "START_CMD=node --inspect /var/core/dist/index.js"
    addParm "$app" "MONGO_URI=mongodb://localhost:27017/core-development"
  fi

  prompt_for_numbered_choice \
    "Source of $app docker image:" \
    "It's already in the repository" \
    "Download it from the registry" \
    "Build it from scratch"

  local choice=$?

  if [ $choice == 1 ] ; then
    # Docker image already exists in local repo
    local imgs=($(docker images --format={{.Repository}}:{{.Tag}}))
    prompt_for_numbered_choice "Select :" "${imgs[@]}"
    local i=$? && (( i-- ))
    addParm "$app" "image_source=local" 
    addParm "$app" "DOCKER_IMAGE_NAME=${imgs[$i]}"
    return
  fi

  if [ $choice == 2 ] ; then
    # Pull the docker image from the registry
    local repo="$app"
    if askYesNo "Is the image based on a feature build?" ; then repo="${repo}-feature"; fi
    local tag=""
    while [ ! $tag ] ; do
      printf "Specify which $app image in the registry you want by tag\nEnter tag: "
      read tag
      [ $tag ] && break;
      echo "\"$tag\" is not a valid entry"
    done
    addParm "$app" "image_source=registry"
    addParm "$app" "DOCKER_IMAGE_NAME=$REGISTRY/$repo:$tag"
    addParm "$app" "BLAST_IMAGE=true"
  fi

  if [ $choice == 3 ] ; then
    # Build the docker image from scratch
    addParm "$app" "image_source=build"
    addParm "$app" "GIT_REPULL=Y"
    addParm "$app" "BLAST_IMAGE=true"
    
    if [ ! $sparse ] ; then
      INTERACTIVE=TRUE
      GIT_REFSPEC=""
      GIT_BRANCH=""

      setGitRefspec

      addParm "$app" "GIT_BRANCH=$GIT_BRANCH"
      addParm "$app" "GIT_REFSPEC=$GIT_REFSPEC"
      GIT_REFSPEC=""
      GIT_BRANCH=""
      INTERACTIVE=""
    fi
  fi
}

# Add a name=value parameter to the array of parameters meant for $1
addParm() {
  local app="$1"
  local item="$2"
  eval "${app}parms=(\"\${${app}parms[@]}\" \"${item}\")"
}

# echo out the value of a name=value pair found via a search through an array (identified by $1)
# of name=value pairs where a name match is found.
getParm() {
  local app="$1"
  local name="$2"
  eval "local a=(\"\${${app}parms[@]}\")"
  for nv in "${a[@]}" ; do
    local n=$(echo -n "$nv" | cut -d'=' -f1 | xargs)
    if [ "${n,,}" == "${name,,}" ] ; then
      local v=$(echo -n "$nv" | cut -d'=' -f2- | xargs)
      echo "$v" && return
    fi
  done
}

# Remove from an array of name=value pairs (identified by $1) those whose name matches any of the remaining parameters
removeParms() {
  local app="$1"
  local newparms=()
  local badparms=("${@:2}")
  eval "local oldparms=(\"\${${app}parms[@]}\")"

  for oldparmNV in "${oldparms[@]}" ; do
    local oldname=$(echo -n "$oldparmNV" | cut -d'=' -f1 | xargs)
    for badname in "${badparms[@]}" ; do
      [ "${badname,,}" == "${oldname,,}" ] && continue 2
    done
    newparms=("${newparms[@]}" "$oldparmNV")
  done

  eval "${app}parms=(\"\${newparms[@]}\")"
}

# A docker container with the same name as the one to be run already exists.
# If the user chooses not to remove the existing container, that container is "in the way"
container_in_the_way() {
  local app="$1"
  local interact="$(getParm "$app" "interactive")"
  local containerName="$app"
  local retval=""
  [ "${app,,}" == "kc" ] && containerName="kuali-research"
  if [ "${interact,,}" != "true" ] ; then
    if ! checkContainer "name=$containerName" "interactive=true" ; then
      local retval="true"
    fi
  fi
  [ $retval ] && true || false
}


# With the necessary arrays of name=value pairs populated, trigger the scripts that consume
# the parameters and execute the docker activity.
startApps() {
  if ! checkRootDirectory ; then return 1; fi

  printHeader "Starting selected applications"

  echo "TODO: run apache?"

  if [ ${#coreparms[@]} -gt 0 ] ; then
    for i in "${coreparms[@]}" ; do echo $i; done
    local imgSrc="$(getParm "core" "image_source")" && removeParms "core" "image_source"
    local rootdir="$(getParm "core" "root_dir")"

    container_in_the_way "core" && return 1

    cd $rootdir 
    source docker.sh
    if [ "$imgSrc" == "build" ] ; then 
      build "${coreparms[@]}"
      [ $? -gt 0 ] && return 1
    fi
    runapp "${coreparms[@]}"
    [ $? -gt 0 ] && return 1
  fi

  if [ ${#coiparms[@]} -gt 0 ] ; then
    local imgSrc="$(getParm "coi" "image_source")" && removeParms "coi" "image_source"
    local rootdir="$(getParm "coi" "root_dir")"

    container_in_the_way "coi" && return 1

    cd $rootdir
    source docker.sh
    if [ "$imgSrc" == "build" ] ; then
      build "${coiparms[@]}"
      [ $? -gt 0 ] && return 1
    fi
    runapp "${coiparms[@]}"
    [ $? -gt 0 ] && return 1
  fi

  if [ ${#kcparms[@]} -gt 0 ] ; then
    local imgSrc="$(getParm "kc" "image_source")" && removeParms "kc" "image_source"
    local rootdir="$(getParm "kc" "root_dir")"

    container_in_the_way "kc" && return 1

    cd $rootdir
    if [ "$imgSrc" == "build" ] ; then
      echo "TODO: Get proper maven commands from scripts on mydevbox"
    elif [ "$imgSrc" == "registry" ] ; then
      echo "TODO: Log into registry"
    fi
    echo "TODO: Get a basic copy of kc-config.xml for localhost run"
    echo "TODO: make call to dockerrun.sh or do something equivalent to kuali-research-4-docker-run-container"
  fi
  
}

