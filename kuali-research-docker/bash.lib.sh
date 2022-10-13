# -----------------------------------------------------------------------------------------------------------------------
#
# The functions below are common and useful to scripts run from any docker build context.
# To use these functions, you can either source or sh this file or for convenience, do the following:
#   1) Add yourself to the root group and give group write access to all docker build contexts.
#      sudo su root
#      usermod -a -G root yourself
#      cd /opt
#      chmod -R g+w kuali-research-docker
#   2) Next make bash.lib.sh functions accessible to aliased commands.
#      echo "[ -f /opt/kuali-research-docker/bash.lib.sh ] && source /opt/kuali-research-docker/bash.lib.sh" >> ~/.bashrc
#      echo "alias ecrdo='ecr_choose'" >> ~/.bashrc
#      echo "alias ecrdebug='ecr_choose debug=true'" >> ~/.bashrc
#  
# -----------------------------------------------------------------------------------------------------------------------

# Take in parameters of the form "arg1=value1 arg2=value2 arg3..." and echo them out
# so that code within functions calling this function can eval something that establishes them as local variables.
# It's one method of implementing named arguments for functions.
_parseargs() {
  local s=""
  local case="$1"

  for arg in "${@:2}" ; do
    local name=$(echo -n "$arg" | cut -d'=' -f1 | trim)
    local value=$(echo -n "$arg" | cut -d'=' -f2- | trim)
    if [ "$case" == "uppercase" ] ; then
      name="${name^^}";
    elif [ "$case" == "lowercase" ] ; then
      name="${name,,}";
    fi

    [ -n "$s" ] && s="$s && local" || s="local"
    s="$s ${name}=\"${value}\""
  done
  
  echo $s
}

trimArg() {
  echo -n "$(printf $1 | sed -E 's/^[ \t\n]*//' | sed -E 's/[ \t\n]*$//')"
}

trim() {
  # This not working for non gnu sed, which seems to be the case in unidentifiable scenarios on amazon linux ami 2 
  # (even though gnu sed seems to be installed and works when used manually)
  # read arg && printf "$arg" | sed -E 's/^[ \t\n]*//' | sed -E 's/[ \t\n]*$//'

  # So, using...
  read arg && printf "$arg" | awk '{ print $1 }' | sed '/^$/d'
}

parseargs() {
  _parseargs "" "$@"
}

# Parse all arguments (see comments for _parseargs), but assign the values to the lowercase of the variable names provided.
parseargs_uppercase() {
  _parseargs "uppercase" "$@"
}

# Parse all arguments (see comments for _parseargs), but assign the values to the uppercase of the variable names provided.
parseargs_lowercase() {
  _parseargs "lowercase" "$@"
}

sudo_exists() {
  [ -z "$(sudo 2>&1 | grep -i 'command not found')" ] && true || false
}

# Indicate if a docker image exists by name or name:tag
docker_image_exists() {
  local img="$1"
  [ -n "$(docker images -q $img)" ] && true || false
}

prompt_for_numbered_choice() {
  while true ; do
    local prompt="\n$1"
    local choices=("$@")
    for ((i = 1; i < ${#choices[@]}; i++)) ; do
      prompt="$prompt\n  $i) $(echo ${choices[i]})"
    done
    printf "$prompt\n: "
    read answer
    # answer must be numeric
    [ -z "$(echo $answer | grep -P '^\d+$')" ] && echo "INVALID ENTRY! Your input must be numeric" && continue;
    local max=$((${#choices[@]}-1))
    # answer must be greater than zero and less than the size of the array.
    [ $answer -lt 1 ] || [ $answer -gt $max ] && echo "INVALID ENTRY! Expecting a number between 1 and $max" && continue;
    break;
  done
  return $answer
}

# Use the AWS cli to push or pull docker images against the ECR (elastic container registry)
ecr_do() {
  # Construct the usage message
  local arg1="profile=[ECR profile name (optional)]"
  local arg2="url=[docker registry url (optional)]"
  local arg3="task=['push' or 'pull' (required with url - pushes or pulls against url after the login)]"
  local usage="USAGE:\n  awslogin arg1 arg2 arg3\n    arg1:$arg1\n    arg2:$arg2\n    arg3:$arg3"

  # Set the named parameters as local variables.
  eval "$(parseargs $@)"

  # Login to the AWS ECR docker registry.
  if ! logIntoRegistry "$@" ; then
    return 1
  fi
  
  # Validate the remaining args
  [ -z "$url" ] && return 0
  [ -z "$task" ] && echo " " && echo "MISSING ARG: task" && printf $usage && return 1
  task="${task,,}"
  [ $task != 'push' ] && [ $task != 'pull' ] && echo " " && echo "BAD ARG: task=$task" && printf $usage && return 1
  
  if [ -n "$retag" ]  && docker_image_exists $retag ; then
    if docker_image_exists $url ; then
      docker rmi $url
    fi
    docker tag $retag $url
  fi

  # Push or pull against the registry.
  if [ -n "$task" ] ; then
    eval "docker $task $url"
  fi
}

# Provide a numbered list of choices for a user to push or pull docker images against the amazon ECR (elastic container registry).
# This reduces the level of familiarity one must have with the ecr_do functions method signature.
ecr_choose() {

  # Set the named parameters as local variables.
  eval "$(parseargs $@)"

  local REGISTRY='730096353738.dkr.ecr.us-east-1.amazonaws.com'
  [ -n "$registry" ] && REGISTRY="$registry"
  local TAG='1709.0037'
  [ -n "$tag" ] && TAG="$tag"
 
  prompt_for_numbered_choice "Pushing or pulling from the registry?" "Pushing" "Pulling"
  local task="$( [ $? == 1 ] && echo push || echo pull)"
  local taskdir="$( [ $task == 'push' ] && echo 'Push to' || echo 'Pull from')"
  cmd="$cmd task=$task"

  prompt_for_numbered_choice "$taskdir which module?" "coeus" "core" "coi" "apache-shibboleth"
  local mod=$?
  local localimg=""
    case $mod in
    1) mod="coeus" && localimg="bu-ist/centos7-kuali-research" ;;
    2) mod="core" && localimg="bu-ist/core"  ;;
    3) mod="coi" && localimg="bu-ist/coi-oracle"   ;;
    4) mod="apache-shibboleth" && localimg="bu-ist/apache-shibboleth" ;;
  esac

  prompt_for_numbered_choice "How is the registry docker image tagged?" "$TAG" "latest" "other..."
  local tag="$?"
  case $tag in
    1) tag="$TAG" ;;
    2) tag="latest" ;;
    3) read -p "Enter the tag value: " tag ;;
  esac

  prompt_for_numbered_choice "$taskdir the registry a feature?" "yes" "no"
  [ $? == 1 ] && local feature="-feature"

  local url="${REGISTRY}/${mod}${feature}:$tag"
  local retag=""
  if [ "$task" == "push" ] ; then
    prompt_for_numbered_choice "Retag? $localimg to $url" "yes" "no"
    [ $? == 1 ] && retag="retag=$localimg"
  fi

  # Let the user review and OK selections.
  if [ -n "$retag" ] ; then
    prompt_for_numbered_choice "SUMMARY: Retag $localimg and $task $url" "yes" "no"
  else
    prompt_for_numbered_choice "SUMMARY: $task $url" "yes" "no"
  fi
  [ $? == 2 ] && echo "Cancelled!" && return 0

  local cmd="ecr_do task=$task url=$url $retag"
  echo $cmd
  [ "$debug" == "true" ] && return 0
  eval $cmd
}


# Make a quick call to cloudwatch to make sure the docker version supports it.
# Also, any other cloudwatch logging errors would be caught here.
cloudwatchIsOk() {
  local loggroup="$1"
  testoutput=$(docker run -t --rm \
    --log-driver=awslogs \
    --log-opt awslogs-region=us-east-1 \
    --log-opt awslogs-group=kuali-research-container-startup \
    --log-opt awslogs-create-group=true \
    busybox sh -c 'echo "STARTING CONTAINER FOR $loggroup $(date)";' 2>&1)
  [ -n "$(echo $testoutput | grep -i 'error')" ] && false || true
}

# If the base cloudwatch home environment variable is set, then get the "environment" (sb, ci, qa, stg, prod) 
# From the kc-config.xml file. 
getEnvironment() {
  if [ -n "$(env | grep 'AWS_CLOUDWATCH_HOME')" ] ; then
    local kcconfig=/opt/kuali/main/config/kc-config.xml
    if [ -f $kcconfig ] ; then
      local domain="$(cat $kcconfig | grep -oP 'kuali\-research(\-[a-z]+)?' | head -n 1)"
      if [ -n "$domain" ] ; then
        echo $domain
      fi
    fi
  fi
}

# Prompt the user for each parameter that was not provided in the function call.
# Once all parameters are gathered, they are used to form the docker image used in the container run command.
# NOTE: Although core and coi are offered as choices, they have their own individual scripts and are not currently supported.
#
# Arguments:
#    docker_image: The full name of the docker image to run the container from.
#                  If not included, prompts will be made for input.                  
#        registry: The url of the docker registry (AWS elastic container registry, or "ECR")
#                  Ignored if docker_image provided.
#      cloudwatch: Indicates if logging is to go to AWS cloudwatch
#    reload_image: If the image already exists, remove it and pull again from the registry
#         confirm: Show the image to the user before running the associated container and ask if ok.
#
run_container() {
  local registry=""
 
  # Set the named parameters as local variables.
  eval "$(parseargs $@)"

  if [ -z "$docker_image" ] ; then
    
    local docker_image=""

    prompt_for_numbered_choice "Which module?" "coeus" "core" "coi" "apache-shibboleth"
    local module=$?
    case $module in
      1) module="coeus" && docker_image="bu-ist/centos7-kuali-research" ;;
      2) module="core" && docker_image="bu-ist/core"  ;;
      3) module="coi" && docker_image="bu-ist/coi-oracle"   ;;
      4) module="apache-shibboleth" && docker_image="bu-ist/apache-shibboleth" ;;
      5) module="research-portal" && docker_image="bu-ist/research-portal" ;;
    esac
     
    prompt_for_numbered_choice \
      "What kind of docker image to select?" \
      "From ECR docker registry in AWS" \
      "Built locally"

    # If the docker image comes from the registry, the overall image url can have variations. Get more data from the user.
    if [ $? == 1 ] ; then 
      [ -z "$registry" ] && local registry='730096353738.dkr.ecr.us-east-1.amazonaws.com'

      # Determine how the docker image is tagged
      local default_tag="1709.0037"
      prompt_for_numbered_choice \
        "Currently tagging for modules coeus, coi and core follow the maven version for coeus.\nHow is this image tagged?" \
        "$default_tag" \
        "latest" \
        "other..."
      local tag="$?"
      case $tag in
        1) tag="$default_tag" ;;
        2) tag="latest" ;;
        3) read -p "Enter the tag value: " tag ;;
      esac
      
      # Determine if this is a feature build
      prompt_for_numbered_choice \
        "What is the docker image from the registry based on?" \
        "Main branch build" \
        "Feature build"
      [ $? == 2 ] && local feature="-feature"

      docker_image="${registry}/${module}${feature}:$tag"

      # Delete the docker image and pull again if specified to do so.
      if docker_image_exists "$docker_image" ; then
        if [ -z "$reload_image" ] ; then
          prompt_for_numbered_choice \
            "This image has already been pulled from the registry" \
            "Pull the image again for changes" \
            "Keep existing image"
          [ $? == 1 ] && local reload_image="true"
        fi
        if [ "${reload_image,,}" == "true" ] ; then
          ecr_do "task=pull" "url=$docker_image"
          [ $? -gt 0 ] && echo "Registry connection failure! Cancelling docker run." && return 1
        fi
      else
        ecr_do "task=pull" "url=$docker_image"
        [ $? -gt 0 ] && echo "Registry connection failure! Cancelling docker run." && return 1
      fi
    fi
  fi

  if [ "${cloudwatch,,}" == "true" ] ; then
    local log_group="$(getEnvironment)"
  fi

  if [ "${confirm,,}" == "true" ] ; then
    prompt_for_numbered_choice "Run a container from the following docker image?:\n$docker_image" "Yes" "no"
    [ $? == 2 ] && return 0
  fi

  if [ -n "$log_group" ] && cloudwatchIsOk $log_group ; then
    [ "${cloudwatch,,}" != "true" ] && echo "Cloudwatch not configured!!!" && return 1
    echo "Starting container with cloudwatch logging"
    runcontainer "docker_image=$docker_image" "log_group=$log_group"
  else
    echo "Starting container WITHOUT cloudwatch logging"
    runcontainer "docker_image=$docker_image"
  fi
}

# Get a "Y/y" or "N/n" response from the user to a question.
askYesNo() {
  local answer="n";
  while true; do
    printf "$1 [y/n]: "
    read yn
    case $yn in
      [Yy]* ) answer="y"; break;;
      [Nn]* ) break;;
      * ) echo "Please answer yes or no.";;
    esac
  done
  if [ $answer = "y" ] ; then true; else false; fi
}

# Indicate if a specified directory has any child folders/files in it.
isEmptyDir() {
  if [ "$(ls -1 $1 | wc -l)" = "0" ] ; then true; else false; fi
}

# Build a string containing a command to copy one file to another directory.
# The main purpose is to catch file not found and other conditions that should be thrown as errors.
getCopyCommand() {
  local SOURCE_DIR=$1
  local SOURCE_FILE=$2
  local SOURCE=$1/$2
  local TARGET_DIR=$3
  local TARGET_FILE=$4
  local TARGET=$3/$4

  if [ ! -f $SOURCE ] ; then
    echo "WARNING! Could not locate source file: $SOURCE"
    return 1;
  fi
  if [ ! -d $TARGET_DIR ] ; then
    echo "WARNING! Could not locate target directory: $TARGET"
    return 1;
  fi
  if [ -z "$TARGET_FILE" ] ; then
    TARGET=$TARGET_DIR/$SOURCE_FILE
  fi
  if [ -f $TARGET ] ; then
    unalias rm 2> /dev/null || true
    rm -f $TARGET
  fi
  echo "cp -f $SOURCE $TARGET"
}

# Copy a single file to another directory via getCopyCommand.
copyFile() {
  local CMD="$(getCopyCommand $@)"
  if [ -z "$(echo $CMD | grep -P '^cp ')" ] ; then
    echo $CMD
    return 1;
  fi
  echo $CMD
  # Some .bashrc files alias cp with "cp -i", but we don't want any console prompts. So unalias cp.
  unalias cp 2> /dev/null || true
  eval $CMD
}

# Check that a directory exists and create it if it does not.
checkdir() {
  if [ ! -d $1 ] ; then
    echo "$1 not found, creating ..."
    mkdir -p $1
  else
    echo "$1 found"
  fi
}


# Clean dangling docker images
cleanDanglingDockerImages() {
  if [ -n "$(docker images --filter dangling=true -q)" ] ; then
    echo "Removing dangling images ..."
    if [ "${DRYRUN,,}" == 'true' ] ; then
      echo "DRYRUN:" && echo "docker rmi -f \$(docker images --filter dangling=true -q)"
    else
      docker rmi -f $(docker images --filter dangling=true -q)
    fi
  fi
}

cleanDanglingVolumes() {
  if [ -n "$(docker volume ls -qf dangling=true)" ] ; then
    echo "Removing dangling images ..." 
    if [ "${DRYRUN,,}" == 'true' ] ; then
      echo 'docker volume rm $(docker volume ls -qf dangling=true)';
    else
      docker volume rm $(docker volume ls -qf dangling=true);
      echo "Removed dangling volume(s)";
    fi
  else 
    echo "No dangling volumes to remove";
  fi
}

# Being more agressive with the cleanup.
# WARNING! This will remove:
#        - all stopped containers
#        - all networks not used by at least one container
#        - all images without at least one container associated to them
#        - all build cache
dockerPrune() {
  echo "Pruning the entire docker system..."
  docker system prune -a -f
}

# Before trying to run a container, check to see if it is already running. If so, prompt the user for removal.
# If the user accepts, remove the container and return true (proceed to run new container).
# Otherwise return false (cancel running of new container).
# ARGS:
#    interactive: true/false - indicates if a person is present to respond to prompts.
#           name: The name of the container to check.
#
checkContainer() {
  # Get named parameters.
  eval "$(parseargs_lowercase $@)"
  [ "${interactive,,}" != "true" ] && local interactive=""
  [ "${blast_container,,}" != "true" ] && local blast_container=""

  local rerun="true"
  if containerExists "$name" ; then
    if [ $blast_container ] ; then
      echo "$name container already exists, deleting..."
      docker rm -f $name
    elif [ $interactive ] ; then
      echo " "
      if askYesNo "The $name docker container already exists. Delete it and rerun? " ; then
        docker rm -f $name
      else
        rerun="false"
      fi
    else
      rerun="false"
    fi
  fi
  if [ "$rerun" == "true" ] ; then true; else false; fi
}

# Using the --filter=name=value switch for docker ps returns containers whose name CONTAINS the supplied value.
# This will potentially yield multiple results because this does not involve an exact match.
# The approach used here loops through all containers names and returns true when an exact match is found.
containerExists() {
  local found=""
  for c in $(docker ps -a --format='{{.Names}}') ; do
    [ "$1" == "$c" ] && found="true" && break;
  done
  [ $found ] && true || false
}


printHeader() {
  if [ -n "$1" ] ; then
    echo " "
    echo "================================================================="
    echo "   $1 ..."
    echo "================================================================="
  fi
}

# The docker image name may indicate it comes from the AWS ecr registry. If so, and the same name can not be found in the local repository,
# log into the registry so the upcoming docker run command will be able to pull the image down to the local repository.
checkRegistry() {
  # 2 arguments expected.
  local reg_name="$1"
  local img_name="$2"
  [ -z "$img_name" ] && return 0

  # Split the img:tag combo into its parts
  local repo_uri="$(echo -n $img_name | cut -s -d':' -f1)"
  [ $repo_uri ] && local repo_tag="$(echo -n $img_name | cut -s -d':' -f2)" || local repo_uri="$img_name"
  [ ! $repo_tag ] && local repo_tag="latest"

  # Does the image reflect the registry as part of its name?
  local registry="$(echo -n $repo_uri | grep $reg_name)"

  # Does the image exist in the local repository?
  local image_found="$(docker images --format '{{.Tag}}' $repo_uri:$repo_tag)"

  if [ ! $image_found ]  && [ $registry ] ; then
    echo "The specified docker image ($repo_uri:$repo_tag) does not exist in the local repository!"
    echo "However, it appears to refer to something in the AWS ecr registry."
    echo "Therefore logging into the registry so docker run command will work..."
    logIntoRegistry
  fi
}

logIntoRegistry() {
  # Get named parameters.
  eval "$(parseargs $@)"

  if [ -n "$profile" ] ; then
    evalstr="$(aws ecr get-login --profile=$profile)"
  else
    if [ -z "$profile" ] ; then
      # Expected region to be part of the profile, but no profile so..."
      local region="$AWS_DEFAULT_REGION"
      [ -z "$region" ] && local region="us-east-1"
    fi
    evalstr="$(aws ecr get-login --region=$region)"
  fi

  # There may be a version mismatch between aws command returned and the aws command line client that will execute it.
  # Specifically, the -e switch has been removed from later versions, so strip it out if it is found.
  evalstr="$(echo "$evalstr" | sed 's/ -e none//')"
  echo "Logging into registry..."
  echo "$evalstr"
  local result=$(eval $evalstr)
  printf "$result"
  [ -z "$(echo $result | grep -i 'succeeded')" ] && false || true
}

# Named parameters can be passed to any function in this script as "name=value"
# These parameters will be set as session scoped variables if they match by name
# any of a set of parameter names, else they are ignored. Parameter names are case insensitive.
# Arguments:
#   arg1: The length of the arg2 array
#   arg2: An array comprising all names of all accepted arguments.
#   arg3, arg4, ... last arg: All the arguments that were passed.
args_to_session_variables() {

  echo "Parsing arguments..."

  local expected_names=("${@:2:$1}")
  local name_value_pairs=("${@:(($1+2))}")

  # Clear out all expected session variables if they already have a value.
  for arg in "${expected_names[@]}" ; do
    eval "$arg=''"
  done

  defaults

  local invalidParm=""

  for arg in "${name_value_pairs[@]}" ; do
    if ! [[ $arg =~ .*=.* ]] ; then
      echo "Unamed parameter ($arg) detected. Use name=value syntax."
      invalidParm="$arg"
      break;
    else
      # Break the named parameter into its name and value parts.
      local name=$(echo -n "$arg" | cut -d'=' -f1 | trim)
      name="${name^^}"
      local value=$(echo -n "$arg" | cut -d'=' -f2- | trim)
      local valid_name=""

      if [ -n "$name" ] ; then
        # Ignore the name of the parameter if it is not in our expected list.
        # Otherwise set it as a session variable.
        for e in "${expected_names[@]}" ; do
          if [ "$name" == "${e^^}" ] ; then
            local valid_name="$e"
            break;
          fi
        done
        if [ -z "$valid_name" ] ; then
          echo "Unrecognized parameter: $name"
          invalidParm="$name"
          break;
        elif [ -n "$value" ] ; then
          local CMD="$valid_name=\"$value\""
          echo $CMD
          eval $CMD
        fi
      fi
    fi
  done

  [ -z "$invalidParm" ] && true || false;
}

# Get the git reference from which to build the application within the docker image.
# NOTE: INTERACTIVE and GIT_REFSPEC are assumed to be environment variables.
setGitRefspec() {
  [ "${INTERACTIVE,,}" != "true" ] && return 0;
  [ -n "$GIT_REFSPEC" ] && return 0;
  if ! isGitRepull ; then return 0; fi

  echo " "
  local question="Enter the name of the git a branch the application will be pulled from.\n"
  question="${question}Type enter for the default value ('master' branch): "
  printf "$question" 
  read GIT_BRANCH
  if [ -z "$GIT_BRANCH" ] ; then
    GIT_BRANCH="master"
  fi

  echo " "
  question="Enter a git tag or commit reference etc to checkout and build the application from.\n"
  question="${question}Type enter for the default value (HEAD of $GIT_BRANCH branch): "
  printf "$question"
  read GIT_REFSPEC
  if [ -z "$GIT_REFSPEC" ] ; then
    GIT_REFSPEC="$GIT_BRANCH"
  fi
}


# Prompt the user to determine if the docker image should pull and rebuild from git.
# This git pull would occur anyway if any items in the docker build context directory
# Have changed whose corresponding instruction in the Dockerfile occurs BEFORE the git
# pull, but it would not otherwise. So to ensure the git pull occurs, an ARG is included
# just before the RUN instruction for the git activity. If the value passed on the
# command line with the --build-arg parameter has changed since last the image was built,
# then the RUN instruction will NOT be pulled from the cache and a brand new layer will
# be created. Specifying "y" here will cause the --build-arg to be included and set its
# value to the current timestamp.
# NOTE: GIT_REPULL and INTERACTIVE are assumed to be environment variables
setGitRepull() {

  if [ -z "$GIT_REPULL" ] && [ "${INTERACTIVE,,}" == "true" ] ; then
    local question="IMPORTANT! If you have built this image before, the current build will skip over instructions in the docker file until it reaches the first one that has since changed.\n"
    question="${question}Docker will use cached image layers for each skipped instruction, including the instruction that pulls new source code from git.\n"
    question="${question}This would mean building against stale source code, but you can avoid this and force the build to go to git for new source.\n"
    question="${question}Force a fresh git pull?"
    echo " "
    if askYesNo "$question" ; then
      GIT_REPULL="Y"
    fi
  fi

  if [ ! -f .gitignore ] ; then
    echo "LAST_IMAGE_BUILD_DATE" > .gitignore
  elif [ -z "$(cat .gitignore | grep LAST_IMAGE_BUILD_DATE)" ] ; then
    echo "LAST_IMAGE_BUILD_DATE" >> .gitignore
  fi

  if isGitRepull ; then
    date "+%F-%T" > LAST_IMAGE_BUILD_DATE
  fi
}


isGitRepull() {
  [ "${GIT_REPULL^^}" == "Y" ] || [ ! -f LAST_IMAGE_BUILD_DATE ]
}

# Download from s3 a file or directory.
# Named Arguments (name=value, name is case-insensitive):
#   Individual credentials (can be ommitted if they exist as environment variables).
#   --------------------------------------------------------------------------------
#   AWS_ACCESS_KEY_ID: The id for the aws access key
#   AWS_SECRET_ACCESS_KEY: The aws access key
#   AWS_DEFAULT_REGION: The aws region s3 is to be accessed from (defaults to: "us-east-1")
#   or...
#   Profile that specified credentials (can be ommitted if it exists as environment variable).
#   ------------------------------------------------------------------------------------------
#   AWS_PROFILE: The name of the profile contained in ~/.aws/credentials or ~/.aws/config file.
#   or...
#   Neither of the above. It will be assumed access exists through group/vpn trust setup.
#   and...
#   source [REQUIRED]: The path within s3 of the file or directory
#   target [REQUIRED]: The local directory or file path where s3 content is to be copied to.
# Non-named Arguments
#   arg1: source, if not provided as a named argument, it will be assumed that the first arg is the source.
#   arg2: target, if not provided as a named argument, it will be assumed that the second arg is the target.
#   
s3Get() {
  # Get environment variables.
  local keyId="$AWS_ACCESS_KEY_ID"
  local keyAccess="$AWS_SECRET_ACCESS_KEY"
  local profile="$AWS_PROFILE"
  local region="$AWS_DEFAULT_REGION"

  # Get named parameters.
  eval "$(parseargs_uppercase $@)"
  
  # Get required parameters (named or not).
  [ -z "$SOURCE" ] && local source="$1" || local source="$SOURCE"
  [ -z "$TARGET" ] && local target="$2" || local target="$TARGET"
  if [ -z "$source" ] || [ -z "$target" ] ; then
    echo "MISSING ARGUMENTS: source and target parameters required for s3 copy"
    return 0;
  fi

  # Named parameters take precedence. Override all matching environment variables.
  [ -n "$AWS_ACCESS_KEY_ID" ] && local keyId="$AWS_ACCESS_KEY_ID"
  [ -n "$AWS_SECRET_ACCESS_KEY" ] && local keyAccess="$AWS_SECRET_ACCESS_KEY"
  [ -n "$AWS_PROFILE" ] && local profile="$AWS_PROFILE"
  [ -n "$AWS_DEFAULT_REGION" ] && local region="$AWS_DEFAULT_REGION" 
  
  # Set a default for the region
  [ -z "$region" ] && region="us-east-1"

  # Make sure the s3 source path begins as an s3 url
  local s3Prefix="s3://"
  local firstFive="${source:0:5}"
  [ i"${firstFive,,}" != "$s3Prefix" ] && source="${s3Prefix}${source}"

  if [ -n "$keyId" ] && [ -n "$keyAccess" ] ; then
    # export the variables in case they were not globally scoped.
    eval "export AWS_ACCESS_KEY_ID=$keyId"
    eval "export AWS_SECRET_ACCESS_KEY=$keyAccess"
    local src="aws s3 cp --region=$region $source $target"
  elif [ -n "$profile" ] && [ -n "$region" ] ; then
    local src="aws s3 cp --region=$region --profile=$profile $source $target"
  elif [ -n "$profile" ] ; then
    local src="aws s3 cp --profile=$profile $source $target"
  else
    # May have access through group/vpn trust setup.
    local src="aws s3 cp --region=$region $source $target"
  fi

  if [ -n "$src" ] ; then
    echo "$src"
    eval "$src"
  fi
}


# The aws ssm send-command has a parameter that indicates that the stdout of the command is to be saved as 
# a file in a specified s3 bucket. This function will determine that url based on the specified bucket and
# command id returned by the send-command. NOTE: the url can almost be predicted without this function due
# to the url of the stdout following a path - however, sometimes it includes a ':' character and sometimes
# not (no way to predict which will be the case).
# ARGUMENTS:
#     profile : The aws cli profile that gains you access to the s3 bucket
#      bucket : The name of the bucket where the stdout file resides
#   commandID : The name of the command ID returned by the send command
#   keyPrefix : An identifier allows the bucket to be divided into subdirectories for different types of commands
s3GetSendCommandOutputFileUrl() {
  eval "$(parseargs_lowercase $@)"
  
  local parm=$profile
  [ -n "$parm" ] && parm="--profile=$parm"
  [ -z "$parm" ] && parm="$AWS_DEFAULT_REGION"
  [ -z "$parm" ] && parm="us-east-1"
  [ -z "$profile" ] && parm="--region=$parm"

  local url=$(aws $parm s3 ls --recursive \
    "s3://$bucket/$keyprefix/$commandid" \
    | grep -oP '[^\s]+?\/stdout$')
  [ -n "$url" ] && echo "s3://$bucket/$url" || echo ""
}


s3GetCoiSendCommandOutputFileUrl() {
  local url=$(
    s3GetSendCommandOutputFileUrl \
      "bucket=${2:-"kuali-docker-run-stdout"}" \
      "keyPrefix=coi" \
      "commandId=$1"
  )
  echo "$url"
}


s3GetCoreSendCommandOutputFileUrl() {
  local url=$(
    s3GetSendCommandOutputFileUrl \
      "bucket=${2:-"kuali-docker-run-stdout"}" \
      "keyPrefix=core" \
      "commandId=$1"
  )
  echo "$url"
}


s3GetKcSendCommandOutputFileUrl() {
  local url=$(
    s3GetSendCommandOutputFileUrl \
      "bucket=${2:-"kuali-docker-run-stdout"}" \
      "keyPrefix=kc" \
      "commandId=$1"
  )
  echo "$url"
}


s3GetApacheSendCommandOutputFileUrl() {
  local url=$(
    s3GetSendCommandOutputFileUrl \
      "bucket=${2:-"kuali-docker-run-stdout"}" \
      "keyPrefix=apache" \
      "commandId=$1"
  )
  echo "$url"
}


s3GetDashboardSendCommandOutputFileUrl() {
  local url=$(
    s3GetSendCommandOutputFileUrl \
      "bucket=${2:-"kuali-docker-run-stdout"}" \
      "keyPrefix=dashboard" \
      "commandId=$1"
  )
  echo "$url"
}


s3GetPdfSendCommandOutputFileUrl() {
  local url=$(
    s3GetSendCommandOutputFileUrl \
      "bucket=${2:-"kuali-docker-run-stdout"}" \
      "keyPrefix=pdf" \
      "commandId=$1"
  )
  echo "$url"
}

# Two files containing name=value lines are to be merged together.
#
# FILE1: field1=value1  FILE2: field3=value4      MERGED: field1=value1
#        field2=value2         field4=value5 --->         field2=value2
#        field3=value3         field5=value6              field3=value4
#                                                         field4=value5
# (Notice how the common property - field3                field5=value6
#  is picked from the second file)
mergePropertyFiles() {
  # Validate both files
  local file1="$1"
  [ ! -f $file1 ] && echo "ERROR! mergePropertyFiles: file1, $file1 does not exist!" && return 1
  local file2="$2"
  [ ! -f $file2 ] && echo "ERROR! mergePropertyFiles: file2, $file2 does not exist!" && return 1
  local file3="$3"

  printf "Merging the following 2 files: \n    $file1 \n    $file2 \n"
  
  # Read the lines of each file into its own array
  local a1=() && readarray -t a1 < $file1
  local a2=() && readarray -t a2 < $file2
  local a3=()

  # Put all properties from file1 that do not appear in file2 into the a3 array
  local i=1
  for line1 in "${a1[@]}" ; do
    # split the line on the "=" character
    local name1=$(echo -n "$line1" | cut -d'=' -f1 | trim)
    [ -z "$name1" ] && continue;
    local value1=$(echo -n "$line1" | cut -d'=' -f2- | trim)
    [ -z "$value1" ] && continue;
    local match=""
    for line2 in "${a2[@]}" ; do
      local name2=$(echo -n "$line2" | cut -d'=' -f1 | trim)
      [ "${name2:0:1}" == "#" ] && continue;  # Ignore commented properties
      [ "$name1" == "$name2" ] && match="$line2"
      # Don't exit loop on a match because the same property may be on a later line.
      # If so, we want this later property to override its earlier appearance.
    done
    [ "${name1:0:1}" == "#" ] && continue;  # Ignore commented properties
    [ -z "$match" ] && a3=("${a3[@]}" "$name1=$value1")
    echo "    processing line $i of ${#a1[@]}"
    ((i+=1))
  done

  # Put all properties from file2 into the a3 array
  for line2 in "${a2[@]}" ; do
    local name2=$(echo -n "$line2" | cut -d'=' -f1 | trim)
    [ -z "$name2" ] && continue;
    local value2=$(echo -n "$line2" | cut -d'=' -f2- | trim)
    [ -z "$value2" ] && continue;
    [ "${name2:0:1}" == "#" ] && continue;  # Ignore commented properties
    a3=("${a3[@]}" "$name2=$value2")
  done

  # Write out the a3 array to file3
  for line in "${a3[@]}" ; do
    echo "$line" >> $file3
  done
}

# Determine if a properties file with name=value lines contains a line with a specified name.
# ARGS:
#   arg1: The property name
#   arg2: The pathname of the properties file
propertyExistsInFile() {
  if [ -f $2 ] ; then
    local value="$(getPropertyFromFile $1 $2)"
  else
    echo "ERROR in propertyExistsInFile: file $2 does not exist"
  fi
  [ -n "$value" ] && true || false;
}

# From a properties file with name=value lines, obtain the value for a specified property name.
# ARGS:
#   arg1: The property name
#   arg2: The pathname of the properties file
getPropertyFromFile() {
  if [ -f $2 ] ; then
    local regex="^[\\x20\\t]*$1[\\x20\\t]*=[\\x20\\t]*([^\\s]+)"
    # use of tail will get the last entry if the property appears more than once in the file.
    # use of cut to get what's on the right side of the equals sign
    local value=$(cat $2 | grep -iP $regex | tail -n1 | cut -d'=' -f2- | trim)
    [ -n "$value" ] && echo -n "$value"
  fi
}


# Determine if an entry name= (not name=value) exists in a properties file.
# This would be an empty entry, not necessarily a non-entry, and could indicate
# that a property is to be nulled out instead of invoking any default value.
#   arg1: The property name
#   arg2: The pathname of the properties file
hasEmptyPropfileEntry() {
  local retval="false"
  if [ -f $2 ] ; then
    local regex="^[\\x20\\t]*$1[\\x20\\t]*="
    local line=$(cat $2 | grep -iP $regex | tail -n1)
    local name=$(echo "$line" | cut -d'=' -f1 | trim)
    local value=$(echo "$line" | cut -d'=' -f2- | trim)
  fi
  [ -n "$name" ] && [ -z "$value" ] && true || false
}


ensureHasProperty() {
  local propname=$(echo "$1" | cut -d'=' -f1 | trim)
  local propval=$(echo "$1" | cut -d'=' -f2- | trim)
  local propfile="$2"
  if [ ! -f "$propfile" ] ; then
    local addprop="true"
  elif ! propertyExistsInFile "$propname" "$propfile" ; then
    if ! hasEmptyPropfileEntry "$propname" "$propfile" ; then
      local addprop=true
    fi
  fi
  
  [ $addprop ] && echo "$propname=$propval" >> $propfile
}

# If the specified property is not in the specified file, prompt the user for it's value and insert it into the file.
promptToPropertyFile() {
  local success="true"
  eval "$(parseargs $@)"
  [ ! -f $PROPFILE ] && echo "Creating $PROPFILE" && touch $PROPFILE
  if ! propertyExistsInFile "$PROPNAME" "$PROPFILE" ; then
    echo "\"$PROPNAME\" value not found in ${PROPFILE}!"
    eval "read -p \"Type a value (\\\"Enter\\\" to cancel): \" $PROPNAME"
    eval "propval="$(echo \$$PROPNAME)""
    if [ -n "$propval" ] ; then
      echo "$PROPNAME=$propval" >> $PROPFILE
    else
      success=""
    fi
  fi

  [ $success ] && true || false
}


# Print out all the environment variables of a docker container
docker_env() {
  echo ""
  # docker inspect --format='{{.Config.Env}}' $1 | sed 's/ /\n/g'
  docker inspect --format='{{range .Config.Env}}{{"\n"}}{{.}}{{end}}' $1
  echo ""
}


# Make an environment variables file containing name=value lines that can be referenced by
# a docker container using the --env-file parameter. The name of the created file is 
# "environment.variables.final.env" and is created/recreated from scratch by combining the name=value
# pairs found in a file obtained from an s3 bucket with a local file in the root directory.
# Matching entries in the optional local file take precedence over those in the s3 file, providing a way
# to override some or all of the s3 file. Each of these 2 files become the final file if the other is missing.
#
# NAMED ARGUMENTS:
#     landscape: indicates environment (sb, ci, qa, stg, prod)
#       appname: indicates the name of the application (core, coi, etc.)
#   or...
#        s3path: indicates the url within the s3 bucket where a required environment variables file 
#                resides. If not included, s3path is built using in part landscape and appname
#   and...
#        s3pull: OPTIONAL (true/false) indicates if environment variables are to be pulled from the
#                s3 bucket. Defaults to false.
#   interactive: indicates a human is present, in which case prompt for permission to pull from s3 bucket.
#                Defaults to false.
#       rootdir: indicates a root directory where the environment variables file is to be created.
#                Defaults to the current directory.
#
S3_QUESTION=$(cat <<EOF
All sensitive configurations (passwords, etc) are stored in an s3 bucket in a file called 'environment.variables.s3.env'.
If downloaded, these will be passed to the docker run commmand to set all environment variables to be used by the docker container.
To override any of these properties, store alternative properties in an 'environment.variables.local.env' file at the root of the docker build context.
The overridden result is output to a file called 'environment.variables.final.env'
Download 'environment.variables.s3.env' from the s3 bucket?
EOF
)
createEnvironmentVariablesFile() {

  # Convert named arguments to local variables
  eval "$(parseargs_lowercase $@)"

  [ "$s3pull" != "true" ] && local s3pull=""
  [ "$interactive" != "true" ] && local interactive=""
  [ "$localhost" != "true" ] && local localhost=""
  [ -z "$rootdir" ] && local rootdir=$(pwd)

  # Validate the arguments supplied.
  local envfileS3="environment.variables.s3.env"
  if [ -z "$s3path" ] ; then
    if [ -z "$landscape" ] || [ -z "$appname" ] ; then
      if [ $interactive ] ; then
        getLandscape "ec2" && landscape="$get_landscape_retval"
      fi
      if [ ! $localhost ] ; then
        echo "ERROR! createEnvironmentVariablesFile: s3path or landscape and appname parameters required!"
        return 1
      fi
    fi
    local s3path="kuali-research-ec2-setup/$landscape/$appname/$envfileS3"
  fi

  # Declare all remaining local variables
  local envfile="environment.variables.final.env"
  local envfileBackup="${envfile}.backup"
  local envfileS3Backup="${envfileS3}.backup"
  local envfileS3Path="$s3path"
  local envfileLocal="environment.variables.local.env"
  local pullFromS3="false"

  # 1) Determine whether or not to pull the environment variables file from the s3 bucket.
  if [ $s3pull ] ; then
    pullFromS3="true"
  elif [ $interactive ] ; then
    echo " "
    if askYesNo "$S3_QUESTION" ; then
      echo " "
      pullFromS3="true"
    else
      echo " "
    fi
  elif [ ! -f "$rootdir/$envfile" ] && [ -n "$landscape" ] ; then
    pullFromS3="true"
  fi

  # 2) Pull the environment variables file from the s3 bucket.
  if [ "$pullFromS3" == "true" ] ; then
    if [ -f $rootdir/$envfileS3 ] ; then
      # Backup any existing s3 env var file in case the pull from the s3 bucket fails
      mv $rootdir/$envfileS3 $rootdir/$envfileS3Backup
    fi

    s3Get "source=$envfileS3Path" "target=$rootdir/" "aws_profile=$profile"

    if [ ! -f $rootdir/$envfileS3 ] && [ -f $rootdir/$envfileS3Backup ] ; then
      echo "ERROR! createEnvironmentVariableFile: Failed to pull environment variables file from s3 bucket"
      mv $rootdir/$envfileS3Backup $rootdir/$envfileS3
      return 1
    else
      # Remove any carriage return characters in case file originated from a windows system.
      sed -i 's/\r//g' $rootdir/$envfileS3
      if [ -f $rootdir/$envfileS3Backup ] ; then
        unalias rm 2> /dev/null || true
        rm -f $rootdir/$envfileS3Backup
        local mergeEnvFiles="true"
      fi
    fi
  fi

  # 3) Create some convenient booleans that indicate what environment variables files exist.
  [ -f "$rootdir/$envfile" ] && local existsFinal=true
  [ -f "$rootdir/$envfileS3" ] && local existsS3=true
  [ -f "$rootdir/$envfileLocal" ] && local existsLocal=true

  # 4) Merge anything obtained from the s3 bucket with any local env vars file, favoring the local file where overlap exists.
  if [ $mergeEnvFiles ] || [ ! $existsFinal ] ; then
    if [ $existsS3 ] && [ $existsLocal ] ; then
      # Backup any previously merged files (must be restored in case new merge fails).
      if [ $existsFinal ] ; then
        mv $rootdir/$envfile $rootdir/$envfileBackup
      fi

      # Merge the files
      mergePropertyFiles $rootdir/$envfileS3 $rootdir/$envfileLocal $rootdir/$envfile

      # Remove the backup file if the merged succeeded, else restore the backup to its original name.
      if [ -f $rootdir/$envfile ] ; then
        unalias rm 2> /dev/null || true
        rm -f $rootdir/$envfileBackup
      else
        echo "ERROR! createEnvironmentVariableFile: Merge of properties files failed"
        mv $rootdir/$envfileBackup $rootdir/$envfile
        return 1
      fi
    elif [ $existsS3 ] ; then
      # The environment variables pulled from the s3 bucket alone become the final environment variables file.
      unalias cp 2> /dev/null || true
      cp $rootdir/$envfileS3 $rootdir/$envfile
    elif [ $existsLocal ] ; then
      # The environment variables in the local file become the final environment variables file.
      unalias cp 2> /dev/null || true
      cp $rootdir/$envfileLocal $rootdir/$envfile
    fi
  fi

  return 0
}


isLandscape() {
  [ "${1,,}" == "sb" ] && return 0
  [ "${1,,}" == "ci" ] && return 0
  [ "${1,,}" == "qa" ] && return 0
  [ "${1,,}" == "stg" ] && return 0
  [ "${1,,}" == "prod" ] && return 0

  return 1
}


getLandscape() {
  if [ "${1,,}" == "ec2" ] ; then
    prompt_for_numbered_choice "What ec2 environment? (if running localhost, you can still specify an enviroment to connect to its datasource)" "sandbox" "ci" "qa" "staging" "production"
  else
    prompt_for_numbered_choice "What environment?" "sandbox" "ci" "qa" "staging" "production" "localhost"
  fi
  # Cannot echo out the return value because of STDIN use in above function call, therefore set env var with the return value.
  local lndscp=$?
  case $lndscp in
    1) get_landscape_retval="sb" ;;
    2) get_landscape_retval="ci" ;;
    3) get_landscape_retval="qa" ;;
    4) get_landscape_retval="stg" ;;
    5) get_landscape_retval="prod" ;;
  esac
}


# Copy a file from a directory A to directory B in the local file system.
# If the file does not exist in directory A (or the current directory), attempt to aquire
# it from a specified s3 bucket.
#
# ARGUMENTS:
#      filename: The name of the file to copy
#       filesrc: The directory to find the file.
# or...
#         s3url: The location of the file within s3 as a url
# and...
#      filedest: The directory to copy the file to.
#   interactive: Indicates if a person is present to provided feedback.
#     landscape: Indicates the landscape (sb, ci, qa, stg, prod)
#       appname: Indicates the application (core, coi, etc)
#     overwrite: Copy filename from filesrc to filedest even if it already exists at filedest.
#
copyConfigFile() {

  # Convert named arguments to local variables
  eval "$(parseargs_lowercase $@)"

  # Validate the the arguments
  [ "${interactive,,}" != "true" ] && local interactive=""
  [ "${overwrite,,}" != "true" ] && local overwrite=""
  [ "${skips3,,}" != "true" ] && local skips3=""
  [ "${s3only,,}" == "true" ] && local skiplocal="true"
  [ -z "$filedest" ] && echo "ERROR! copyConfigFile: filedest required!" && return 1
  [ -z "$filename" ] && echo "ERROR! copyConfigFile: filename required!" && return 1

  # Try simple local file system copy first
  if [ ! $skiplocal ] ; then
    local refreshed=""
    if refreshConfigFile "$@" "verbose=true" ; then # Try root directory first
      refreshed="true"
    else
      if [ "$filesrc" != "$(pwd)" ] ; then
        if refreshConfigFile "$@" "filesrc_override=$(pwd)" "verbose=true"; then # Try current directory second
          refreshed="true"
        fi
      fi
    fi
  fi

  # Could not or didn't want to find a local copy of the file, so get it from the s3 bucket.
  if [ ! $refreshed ] && [ ! $skips3 ] ; then

    # Get the url of the file within the s3 bucket.
    if [ -z "$s3url" ] ; then
      if [ -z "$landscape" ] ; then
        # Common usage usually includes a session scoped environment variable called "LANDSCAPE"
        [ -n "$LANDSCAPE" ] && local landscape="$LANDSCAPE"
        if [ -z "$landscape" ] && [ -n "$appname" ] && [ $interactive ] ; then
          echo " "
          if askYesNo "Cannot find ${filename}\nDownload from s3 bucket?" ; then
            getLandscape "ec2" && local landscape="$get_landscape_retval" && LANDSCAPE="$landscape"
          fi
        fi
      fi
    fi

    if [ -z "$s3url" ] && [ -n "$appname" ] && [ -n "$landscape" ] ; then
      local s3url="kuali-research-ec2-setup/$landscape/$appname/$filename"
    fi

    # Pull the file from the s3 bucket directly to the source directory.
    if [ -n "$s3url" ] ; then
      [ -n $profile ] && local profile="aws_profile=$profile"
      s3Get \
        "source=$s3url" \
        "target=$filedest/" \
        "$profile"
    fi
  fi

  if [ ! -f $filedest/$filename ] ; then
    echo "ERROR! copyConfigFile: $filename required in $filedest"
    [ ! $skips3 ] && local s3msg=" or download it from the aws s3 bucket"
    echo "Could not find $filename in local filesystem${s3msg}!"
    return 1
  else
    # Remove any carriage return characters from files that came from a windows system
    sed -i 's/\r//g' $filedest/$filename
  fi

  return 0
}


# Copy a single file from one location to another, overwriting if specified to do so.
# ARGUMENTS: 
#         filename: The name of the file to copy
#          filesrc: The directory to find the file.
#         filedest: The directory to copy the file to.
# filesrc_override: Ignore filesrc and use this value instead.
#        overwrite: Copy filename from filesrc to filedest even if it already exists at filedest.
#          verbose: Echo out actions even if none taken.
#
refreshConfigFile() {
  eval "$(parseargs_lowercase $@)"
  [ $filesrc_override ] && local filesrc="$filesrc_override"

  local success=""
 
  unalias mv 2> /dev/null || true
  unalias cp 2> /dev/null || true
  unalias rm 2> /dev/null || true

  if [ -f $filesrc/$filename ] ; then
    if [ ! -f $filedest/$filename ] ; then
      copyFile $filesrc $filename $filedest
      [ -f $filedest/$filename ] && success="true"
    elif [ $overwrite ] ; then
      [ -f "$filedest/$filename.backup" ] && rm -f "$filedest/$filename.backup"
      mv "$filedest/$filename" "$filedest/$filename.backup"
      copyFile $filesrc $filename $filedest
      if [ -f $filedest/$filename ] ; then
        rm -f "$filedest/$filename.backup"
        success="true"
      else
        mv "$filedest/$filename.backup" "$filedest/$filename"
      fi
    elif [ -f $filedest/$filename ] ; then
      # File already exists at destination and not required to overwrite.
      echo "Reusing existing file: $filedest/$filename"
      success="true"
    fi
  elif [ ! $overwrite ] ; then
    if [ -f $filedest/$filename ] ; then
      # Nothing in the root directory to refresh the file with, but it still exists and we were not asked to overwrite
      [ "$verbose" == "true" ] && echo "Using existing file: $filedest/$filename"
      success="true"
    else
      echo "Neither $filedest/$filename or $filesrc/$filename exist!"
    fi
  fi

  [ $success ] && true || false
}


# If interactive, then ask the user if all config files are to be deleted from their target 
# directories so that fresh default (unaltered) copies from the root directory or s3 bucket
# will be put there. If not interactive, then this property will be set to true.
askRenewConfigs() {
  # If RENEW_CONFIGS was set as a boolean, convert to the equivalent default.
  [ "${RENEW_CONFIGS,,}" == "true" ] && RENEW_CONFIGS="s3"
  [ "${RENEW_CONFIGS,,}" == "false" ] && RENEW_CONFIGS="none"
  # Only continue if RENEW_CONFIGS has no value at all.
  [ $RENEW_CONFIGS ] && return

  local filelist=""
  for file in "${@}" ; do
    filelist="$filelist $file"
  done

  if [ $INTERACTIVE ] ; then
    echo " "
    echo "Configuration files $filelist go to a directory mounted to the docker container."
    printf "If they are not there, or need to be refreshed, you have several options."
    prompt_for_numbered_choice "Select one: " \
      "Find copies in the root or current directory and refresh with these" \
      "Refresh with copies from the s3 bucket" \
      "Both of above: try 2) if 1) fails" \
      "Do nothing (use existing copies)"
    local choice=$?
    [ $choice -eq 1 ] && RENEW_CONFIGS="local"
    [ $choice -eq 2 ] && RENEW_CONFIGS="s3" && local need_landscape="true"
    [ $choice -eq 3 ] && RENEW_CONFIGS="both" && local need_landscape="true"
    [ $choice -eq 4 ] && RENEW_CONFIGS="none"

    if [ $need_landscape ] ; then  
      if ! isLandscape "$LANDSCAPE" ; then
        if ! isLandscape "$get_landscape_retval" ; then
          [ $RENEW_CONFIGS == "both" ] && printf "\nIn case there is no file in the root directory, the landscape will identify the s3 bucket."
          getLandscape "ec2" && CONFIG_LANDSCAPE="$get_landscape_retval"
        fi
      fi
    fi
  else
    # If no user to prompt, assume configs need to be refreshed from the s3 bucket.
    RENEW_CONFIGS="s3"
  fi
}


copyBashLibs() {

  local up_two_directories="$(dirname $(dirname $(pwd)))"

  copyConfigFile \
    "filename=bash.lib.sh" \
    "filesrc=$up_two_directories" \
    "filedest=$NODEAPP_SCRIPTS_DIR" \
    "appname=core" \
    "landscape=$LANDSCAPE" \
    "interactive=$INTERACTIVE" \
    "overwrite=true" \
    "skips3=true"

  copyConfigFile \
    "filename=buildhelper.sh" \
    "filesrc=$up_two_directories" \
    "filedest=$NODEAPP_SCRIPTS_DIR" \
    "appname=core" \
    "landscape=$LANDSCAPE" \
    "interactive=$INTERACTIVE" \
    "overwrite=true" \
    "skips3=true"
}


# If locatable, determine the aws configuration variables sufficient for authentication for calls through the cli
# If successful, add the variables to the specified properties file (arg 1) as name=value lines.
awsConfigsToPropertyFile() {
  echo "Analyzing aws configuration settings..."
  local propfile="$1"
  if ! propertyExistsInFile 'AWS_ACCESS_KEY_ID' $propfile ; then
    if [ -z "$AWS_ACCESS_KEY_ID" ] && [ -f ~/.aws/config ] ; then
      AWS_ACCESS_KEY_ID=$(cat ~/.aws/config | grep -o -m1 -P '(?<=aws_access_key_id=).*')
    fi
    [ -n "$AWS_ACCESS_KEY_ID" ] && echo "AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID" >> $propfile
  fi
  if ! propertyExistsInFile 'AWS_SECRET_ACCESS_KEY' $propfile ; then
    if [ -z "$AWS_SECRET_ACCESS_KEY" ] && [ -f ~/.aws/config ] ; then
      AWS_SECRET_ACCESS_KEY=$(cat ~/.aws/config | grep -o -m1 -P '(?<=aws_secret_access_key=).*')
    fi
    [ -n "$AWS_SECRET_ACCESS_KEY" ] && echo "AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY" >> $propfile
  fi
  if ! propertyExistsInFile 'AWS_DEFAULT_REGION' $propfile ; then
    if [ -z "$AWS_DEFAULT_REGION" ] && [ -f ~/.aws/config ] ; then
      AWS_DEFAULT_REGION=$(cat ~/.aws/config | grep -o -m1 -P '(?<=region=).*')
    fi
    [ -n "$AWS_DEFAULT_REGION" ] && echo "AWS_DEFAULT_REGION=$AWS_DEFAULT_REGION" >> $propfile
  fi

  return 0
}


# Check that a property of specified name exists as a name=value line in a specified properties file.
# If it does not exist, check if it is set as an environment variable and if found insert into the properties file.
# Otherwise, insert the property into the file with a default value if supplied
# Otherwise, echo an error message if indicated to do so.
# ARGUMENTS:
#     PROPNAME: The name of the property
#     PROPFILE: The pathname of the properties file
#   DEFAULTVAL: The value to put
#     CONTINUE: [true/false] If unsuccessful, echo and error and return false, otherwise return true.
#
insertConfigToPropertyFile() {
  # Convert named arguments to local variables  
  eval "$(parseargs_uppercase $@)"
  echo "Looking for $PROPNAME..."

  local retval="true"
  local propval_file="$(getPropertyFromFile $PROPNAME $PROPFILE)"
  local propval_env=$(eval "echo -n \$$PROPNAME")
  [ "$CONTINUE" != "TRUE" ] && local CONTINUE=""

  if [ -n "$propval_file" ] ; then
    # If no environment variable of the same name exists, set one to the value found in the file.
    echo "found."
    [ -z "$propval_env" ] && eval "$PROPNAME=\"$propval_file\""
  else
    # The property does not exist in the properties file, so look for it as an environment variable.
    if [ -n "$propval_env" ] ; then
      echo "$PROPNAME=$propval_env >> $PROPFILE"
      echo "$PROPNAME=$propval_env" >> $PROPFILE
    elif [ -n "$DEFAULTVAL" ] ; then
      echo "APPLYING DEFAULT: $PROPNAME=$DEFAULTVAL >> $PROPFILE"
      echo "$PROPNAME=$DEFAULTVAL" >> $PROPFILE
    elif [ ! $CONTINUE ] ; then
      # The property cannot be found in the properties file or as an environment variable.
      echo "ERROR! insertConfigToPropertyFile: $PROPNAME not set!"
      retval="false"
    else
      echo "Not found, but optional - skipping..."
    fi
  fi

  [ $retval == "true" ] && true || false
}


# Get the portion of a mongo command line client command that contains all the connection information.
# The uri argument indicates what other mongo client switches to include in
# the command.
# ARGUMENTS:
#           uri: The uri to get to locate the mongo server to connect to.
#        Examples:
#          "localhost"
#          "mongodb://localhost/mydatabase
#          "mongodb://ci-cluster-shard-0/
#             ci-cluster-shard-00-00-nzjcq.mongodb.net:27017,
#             ci-cluster-shard-00-01-nzjcq.mongodb.net:27017,
#             ci-cluster-shard-00-02-nzjcq.mongodb.net:27017/
#             core-development?replicaSet=ci-cluster-shard-0"
#          user: The user known by the mongo database.
#      password: The password to the mongo database for the user.
#        dbname: The name of the mongo database (required if not part of uri)
#    replicaset: The name of the mongo replica set (required if not part of the uri)
#
getMongoParameters() {
  # Convert named arguments to local variables
  eval "$(parseargs_lowercase $@)"

  local host=""
  local querystring=""

  # Break down the uri into parts (if possible). It will be reconstituted later.
  if [ ! $uri ] ; then
    host='localhost'
  else
    # Temporarily strip off any leading "mongodb://" prefix
    uri="$(echo $uri | sed 's/mongodb:\/\///')"
    if [ ${uri,,} == 'localhost' ] || [ $uri == '127.0.0.1' ] ; then
      host=${uri,,}
    else
      host=$(echo $uri | cut -s -d'/' -f1)
      local db=$(echo $uri | cut -s -d'/' -f2)
      if [ $host ] ; then
        if [ $db ] ; then
          querystring=$(echo $db | cut -s -d'?' -f2)
          if [ ! $dbname ] ; then
            dbname=$(echo $db | cut -s -d'?' -f1)
            [ ! $dbname ] && dbname=$db
          fi
        fi
      else
        host=$(echo $db | cut -s -d'?' -f1)
        querystring=$(echo $db | cut -s -d'?' -f2)
        if [ ! $host ] ; then
          host=$uri
          querystring=$(echo $host | cut -s -d'?' -f2)
          if [ $querystring ] ; then
            host=$(echo $host | cut -s -d'?' -f1)
          fi
        fi
      fi
    fi
  fi

  # Validate database name
  if [ ! $dbname ] ; then
    echo "ERROR! getMongoParameters: mongo database name not specified." && return 1
  fi

  # Determine if uri is for localhost
  isLocalHost "$host" && local localhost="true"

  # Put the uri back together again in a standard format.
  uri="mongodb://$host/$dbname"
  if [ $querystring ] ; then
    local qs=""
    for s in $(echo $querystring | cut -d'&' -f1- --output-delimiter=' ') ; do 
      local name="$(echo $s | cut -s -d'=' -f1)"
      local value="$(echo $s | cut -s -d'=' -f2)"
      # Flag the fact that replicaset is in the querystring so it can be made the first querystring argument, which
      # is what the mongo command line client seems to want.
      [ "${name,,}" == "replicaset" ] && local rs="$value" && continue;
      # Exclude ssl as a parameter on the querystring. The mongo command line client wants it as an --ssl switch instead.
      # NOTE: Mongoose needs it as a querystring, which is why it would appear in the querystring in the first place.
      [ "${name,,}" == "ssl" ] && [ "${value,,}" == "true" ] && local ssl="true" && continue;
      [ $qs ] && qs="$qs&$name=$value" || qs="$name=$value"
    done
    [ $replicaset ] && local rs="$replicaset"
    if [ $rs ] ; then
      [ $qs ] && qs="replicaSet=$rs&$qs" || qs="replicaSet=$rs"
    fi
    [ $qs ] && uri="$uri?$qs"
  fi 

  # Build the mongo command with parameters based on the type of host.
  local cmd="mongo --verbose "
  if [ $localhost ] ; then
    cmd="$cmd --host \"$uri\""
  else
    cmd="$cmd --host \"$uri\""
    [ $ssl ] && cmd="$cmd --ssl"
    cmd="$cmd --authenticationDatabase admin"
    cmd="$cmd --username $user"
    cmd="$cmd --password $password"
  fi

  printf "$cmd"
  return 0
}

getMongoParms() {
  local parms="$(getMongoParameters \
    "uri=$MONGO_URI" \
    "dbname=$MONGO_DBNAME" \
    "replicaset=$MONGO_REPLICASET" \
    "user=$MONGO_USER" \
    "password=$MONGO_PASS")"
  echo "$parms"
}

# Pull a git repository into a BLANK specified directory
# A credential search will determine how the upstream repo is accessed.
# NOTE: All GIT_* variables are expected to be session scoped. GIT_USER & GIT_PASSWORD will be tried first,
#       and if blank, GIT_KEY will be used, and if also blank, a key will be searched for in the current
#       directory. Failing that, public access is assumed.
# ARGUMENTS:
#        BASEDIR: Pull git content into this directory.
#         BRANCH: The git branch that is to be pulled.
#        REFSPEC: The git refspec that is to be checked out once the pull is complete.
#       GIT_USER: [Optional] The git user that, along with GIT_PASSWORD will be used to authenticate.
#   GIT_PASSWORD: [Optional] The git password that, along with GIT_USER will be used to authenticate.
#                 NOTE: If GIT_USER and/or GIT_PASSWORD are excluded, a search for a SSH key will be 
#                       made in the current directory. Failing that, public access to the git repo is assumed.
#
gitpull() {
  # Convert named arguments to local variables
  eval "$(parseargs_uppercase $@)"

  [ -z "$BRANCH" ] && BRANCH="master"
  [ -z "$REFSPEC" ] && REFSPEC="FETCH_HEAD"
  local CMD="git fetch"

  cd $BASEDIR
  local GIT_KEY=$(find . -iname "*_rsa" | sed -n 1p);
  if [ -n "$GIT_KEY" ] ; then
     ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts
     chmod 600 *_rsa
  fi

  dos2unix .git/config

  if [ -n "$GIT_USER" ] && [ -n "$GIT_PASSWORD" ] ; then
    echo "Trying http protocol with username and password"
    GIT_HTTP_REPO="$(git remote -v | grep -ioP "(?<=https://)(.*?)\.git")"
    # url encode the password
    ENCODED_PASSWORD="$(echo -ne $GIT_PASSWORD | xxd -plain | tr -d '\n' | sed 's/\(..\)/%\1/g')"
    CMD="$CMD https://$GIT_USER:$ENCODED_PASSWORD@$GIT_HTTP_REPO $BRANCH"
  elif [ -n "$GIT_KEY" ] ; then
    echo "No user/password, but git private ssh key found. Using ssh to pull source code from git repo"
    eval `ssh-agent -s`
    ssh-add $GIT_KEY
    CMD="$CMD github-ssh $BRANCH"
  else
    echo "No git user, password, or ssh key available. Assuming public access ..."
    CMD="$CMD github-http $BRANCH"
  fi

  # Initialize the repo if not already
  if [ ! -f .git ] ; then
    git init
  elif [ $(ls -1 | wc -l) -lt 7 ] ; then
    # There should be at least 7 items in .git directory of an initialized git repo
    git init
  fi

  # Pull the branch with tags
  echo $CMD && eval $CMD

  [ "$REFSPEC" == "FETCH_ONLY" ] && return 0

  # REFSPEC might be a tag name, so run rev-parse against it to ensure it is refspec.
  SHA="$(git rev-parse $REFSPEC 2>&1 | grep -P '^[a-fA-F0-9]+$')"

  # Check out the refspec.
  if [ -n "$SHA" ] ; then
    CMD="git checkout -B $BRANCH $SHA"
    echo $CMD && eval $CMD
  else
    echo "WARNING! Could not find ref $REFSPEC in what was fetched from remote"
    echo "Checking out FETCH_HEAD instead."
    CMD="git checkout -B $BRANCH FETCH_HEAD"
    echo $CMD && eval $CMD
  fi
}


gitfetch() {
  # Convert named arguments to local variables
  eval "$(parseargs_uppercase $@)"

  gitpull \
    "BASEDIR=$BASEDIR" \
    "BRANCH=$BRANCH" \
    "GIT_USER=$GIT_USER" \
    "GIT_PASSWORD=$GIT_PASSWORD" \
    "REFSPEC=FETCH_ONLY"
}

dockerRMI() {
  local imagename="$1"
  for container in $(docker ps -a --format '{{.ID}}#{{.Image}}') ; do
    local img="$(echo "$container" | cut -d'#' -f2-)";
    if [ "$img" == "$imagename" ] ; then
      local id="$(echo "$container" | cut -d'#' -f1)";
      echo "Removing container id=$id in order to remove image $imagename..."
      docker rm -f "$id"
    fi
  done
  docker rmi "$imagename"
}

# Add a user to the npm registry and receive a token in return for use in publishing
#
# NOTE: Cannot add a user to an npm registry through npm commands because this requires a tty and you
# will be prompted for user and password values. Even piping these values does not seem to work.
# So, for example the following will fail:
#    echo kualiui > /tmp/answers && \
#    echo password >> /tmp/answers && \
#    echo kualiui@bu.edu >> /tmp/answers && \
#    npm adduser --registry http://localhost:4873 < /tmp/answers && \
# Therefore we must use the couchdb REST api for adding the user (bypassing npm).
# SEE: https://github.com/rlidwka/sinopia/issues/329
#      (It has sometimes occurred that BOTH curl attempts below return a message that the user already
#       exists. Not sure why. This requires clearing out the kuali user from
#       /root/.config/verdaccio/htpasswd  and trying the build again.)
loginToLocalNpmRegistry() {
  
  if ! npmRegistryIsRunning ; then
    echo "Starting npm registry..."
    nohup bash -c "verdaccio &" && sleep 2
    # Since we are restarting the registry, dump the old token from the config (a new one will be set below).
    [ -f ~/.npmrc ] && rm -f ~/.npmrc
  elif [ -f ~/.npmrc ] ; then
    echo "Npm registry already running"
    if [ -f ~/.npmrc ] ; then
      local registry="$(cat ~/.npmrc | grep '@kuali:registry=http://localhost:4873')"
      local token="$(cat  ~/.npmrc | grep 'localhost' | grep 'authToken')"
      if [ -n "$registry" ] && [ -n "$token" ] ; then
        echo "Already logged into npm registry, details:"
        cat ~/.npmrc
        [ -f .npmrc ] && mv .npmrc .npmrc.disabled
        return 0
      fi
    fi
  fi

  RESTURL="http://localhost:4873/-/user/org.couchdb.user:kuali"

  # 1) Add a user to the npm registry using REST api call and capture returned JSON (adduser).
  JSON=$(curl -s \
    -H "Accept: application/json" \
    -H "Content-Type:application/json" \
    -X PUT --data '{"name": "kuali", "password": "mypassword"}' \
    $RESTURL 2>&1)
   
  # 2) If user already exists, get associated JSON from REST call to npm registry (login).
  local error="$(echo -n $JSON | grep -Po '(?<="error": ")[^"]*')"
  local registered="$(echo -n "$error" | grep -io 'already')"
  
  [ "${registered,,}" == "already" ] && \
  JSON=$(curl -s \
    -H "Accept: application/json" \
    -H "Content-Type:application/json" \
    -X PUT --data '{"name": "kuali", "password": "mypassword"}' \
    --user kuali:mypassword $RESTURL 2>&1) 
  
  # 3) Extract the token from the JSON
  TOKEN="$(echo -n $JSON | grep -Po '(?<="token": ")[^"]*')"
  if [ -n "$TOKEN" ] ; then
    echo "Token received from npm registry login: $TOKEN"
  else
    echo "ERROR! No Token! Cancelling docker build..."
    return 1;
  fi
  
  # 4) Configure npm with the registry details and the token to publish to it.
  npm config set @kuali:registry http://localhost:4873
  npm set //localhost:4873/:_authToken $TOKEN
  # The above 2 lines modified the ~/.npmrc file, but the local .npmrc overrides, so disable it.
  [ -f .npmrc ] && mv .npmrc .npmrc.disabled

  # 5) Configure yarn as npm was.
  if YarnInstalled ; then
    yarn config set @kuali:registry http://localhost:4873 -g
    yarn config set //localhost:4873/:_authToken $TOKEN -g
  fi
  # The above 2 lines modified the /usr/local/share/.yarnrc file, but the local .yarnrc overrides, so disable it.
  [ -f .yarnrc ] && mv .yarnrc .yarnrc.disabled
  
  return 0
}


# Check if a specified version of a specified scoped module is published to a local npm registry
# Named args:
#      scope, ie: "@kuali"
#         module: The module name, ie: "kuali-ui", "common", etc.
#        version: The version, ie: "1.0.0"
#   semantically: [default=false] The version must match identically, otherwise a match is ok that satisfies semantic versioning.
isPublishedToLocalNpmRegistry() {
  eval "$(parseargs_lowercase $@)"
  [ ! "${scope:0:1}" == "@" ] && scope="@$scope"

  published=""
  local errormsg='ERROR_ERROR'
  local versions=$(npm show $scope/$module@* version 2>&1 || echo $errormsg)
  local error=$(echo $versions | grep -o $errormsg)
  if [ -z "$error" ] ; then
    for v in $versions ; do

      # Extract the numeric value of the version from the npm output
      # ie: Get '1.0.2' from '@kuali/common@1.0.2'
      v=$(echo "$v" | cut -d'/' -f2 | cut -d'@' -f2)
      if [ "$v" == "$version" ] ; then
        published="true";
        break;
      fi

      if [ "${semantically,,}" == 'true' ] ; then
        if npmVersionSatisfied "$version" "$v" ; then
          published="true"
          break;
        fi
      fi
    done
  fi
  
  [ -n "$published" ] && true || false
}


# Check if a specified version of a specified scoped module is published to a local npm registry
# Named args:
#   scope, ie: "@kuali"
#   module: The module name, ie: "kuali-ui", "common", etc.
#   version: The version, ie: "1.0.0"
unpublishFromLocalNpmRegistry() {
  eval "$(parseargs_lowercase $@)"
  [ ! "${scope:0:1}" == "@" ] && scope="@$scope"

  local registryUrl="http://localhost:4873"
  local fullPkgRef="$scope/$module-$version"

  if isPublishedToLocalNpmRegistry $@ ; then
    npm unpublish --force $registryUrl/$fullPkgRef
    # The .tgz file for the deleted version should be gone at /root/.config/verdaccio/storage/$scope/$module
  else
    echo "Cannot unpublish $fullPkgRef: Not found at $registryUrl"
  fi
}


npmRegistryIsRunning() {
  [ -n "$(ps -ax | awk '$5 ~ /(V|v)erdaccio/ { print "Running" }')" ] && true || false
}


checkJq() {
  # Assumes centos
  jq --version > /dev/null 2>&1
  if [ $? -gt 0 ] ; then
    yum install epel-release -y && \
    yum install jq -y && \
    jq --version
  fi
}

# Walk down the tip of the current git branch until a commit can be found that contains a
# package.json at the root directory that contains a version property that satisfies the provided npm version semantic expression.
# ARGUMENTS:
#   arg1: The semantic version expression. Probably obtained from an item in the dependencies collection of a package.json file.
#   arg2: The git tree reference (like a branch name) with the commits that are to be searched.
getGitRefForNpmVersion() {
  local semantic="$1"
  local symref="$2"
  local packageDotJson="$3"
  [ ! $symref ] && symref="HEAD"
  [ ! $packageDotJson ] && packageDotJson="package.json"

  # BUG in node when accessed from gitbash or cygwin: "stdout is not a tty"
  # Workaround seems to be to call node.exe instead of just node.
  # Not an issue on linux. Mac?
  # https://github.com/nodejs/node/issues/14100
  local gitbash="$(node.exe --version > /dev/null 2>&1 && [ "$?" == "0" ] && echo 'true')"

  # Create the a node command to parse package.json as json and output the version property.
  local cmd="node"
  [ $gitbash ] && cmd="${cmd}.exe"
  cmd="$cmd -pe 'JSON.parse(process.argv[1]).version'"

  # "Walk" down the commits and pass the contents of package.json to the node command to get the version.
  # Exit the loop it the version satisfies the provided version semantic expression.
  #   NOTE: can use git log to see changes and there would be less commits to search through.
  #         But the output would probably give the earliest commit where the content changed for a version match,
  #         not the latest commit with that same version just before it gets advance in the next commit.
  #         EXAMPLE: for ref in $(git log --oneline --format='%H' -- package.json) ; do
  for ref in $(git rev-list "$symref") ; do
    local pkg="$(git show $ref:$packageDotJson 2> /dev/null)"
    [ -z "$pkg" ] && echo "$packageDotJson does not exist in $ref" && continue
    local CMD="local actual=\"\$($cmd \"\$pkg\")\""
    eval "$CMD"
    if npmVersionSatisfied "$semantic" "$actual" ; then
      echo -n "$actual" > computed.version
      echo "$ref"
      break
    elif [ $? -eq 99 ] ; then
      return 99 
    fi
  done
}


# Use the semver package to determine if a specified version satisfies a specified version semantic expression
# ARGUMENTS:
#   semantic: The version semantic expression (can contain "^" and "~" characters, etc.)
#   actual: The actual version value (Checking this to see if it satisfies the semantic expression)
#   
npmVersionSatisfied() {

  # Try to install semver (semantic version package) to help analyze the versions we find in package.json
  if [ -z "$SEMVER_INSTALLED" ] && [ ! $SEMVER_INSTALL_ATTEMPTED ] ; then
    NPM_INSTALLED="$(npm 2> /dev/null)"
    if [ -n "$NPM_INSTALLED" ] ; then
      SEMVER_INSTALLED="$(semver -h 2> /dev/null)"
      if [ -z "$SEMVER_INSTALLED" ] ; then
        local smvr=$(npm install -g semver) # Executing install in a separate shell because we don't want output to go to stdout.
        SEMVER_INSTALLED="$(semver -h 2> /dev/null)"
      fi
    fi
    SEMVER_INSTALL_ATTEMPTED="true"
  fi

  local semantic="$1"
  local actual="$2"

  if [ -n "$SEMVER_INSTALLED" ] ; then
    satisfied="$(semver -r "$semantic" "$actual" 2>&1)"
    if [ $? -gt 0 ] && [ -n "$satisfied" ] ; then
      echo "ERROR: $satisfied"
      return 99
    fi
  else
    # Not going to write my own semantic analysis logic here (That's what semver was supposed to be for).
    # Getting rid of all the semantic characters and replacing "x" characters with lowest value of "0".
    # Then try and match literally on the resulting value, and hope for the best.
    local literal=$(echo -n "$semantic" | sed 's/[^x0-9\.]//g' | sed 's/x/0/g')
    [ "$literal" == "$actual" ] && local satisfied="true"
  fi
  [ $satisfied ] && true || false
}


# Get the version of a specified module from within a specified package.json file
# ARGUMENTS:
#        module: The name of the module listed as a dependency inside the package.json file
#   packageFile: The path and name of the package.json file
#
getNpmDependencyVersion() {
  # Convert named arguments to local variables
  eval "$(parseargs_lowercase $@)"

  # Escape forward slashes so they don't conflict with separators once incorporated into future sed expressions.
  module=$(echo "$module" | sed 's/\//\\\//g')

  # Establish the arguments for a function to run with node that will get the version info
  local commandRequired="JSON.parse(process.argv[1]).dependencies.myversion"
  local commandOptional="JSON.parse(process.argv[1]).optionalDependencies.myversion"
  local commandDev="JSON.parse(process.argv[1]).devDependencies.myversion"
  local parameter="$(cat $packagefile | sed "s/$module/myversion/g")"

  # Run the node commands to get the version info for the module in the dependencies sections of package.json

  local requiredVersion=$(node -pe "$commandRequired" "$parameter" 2> /dev/null)
  if [ $? -eq 0 ] && [ -n "$requiredVersion" ] && [ "$requiredVersion" != "undefined" ] ; then
    echo "dependencies|$requiredVersion"
    return 0
  fi

  local devVersion=$(node -pe "$commandDev" "$parameter" 2> /dev/null)
  if [ $? -eq 0 ] && [ -n "$devVersion" ] && [ "$devVersion" != "undefined" ] ; then
    echo "devDependencies|$devVersion"
    return 0
  fi

  local optionalVersion=$(node -pe "$commandOptional" "$parameter" 2> /dev/null)
  if [ $? -eq 0 ] && [ -n "$optionalVersion" ] && [ "$optionalVersion" != "undefined" ] ; then
    echo "optionalDependencies|$optionalVersion"
    return 0
  fi

  return 1
}


# Remove a package from a specified dependency type (dev, optional, peer, etc).
# ARGS:
#   package: The name of the dependency to remove
#   dependencyGroup: The dependency type (dev, optional, peer, etc)
#   pkgJsonFile: The package.json file from which to remove the dependency.
#
removePackageFromDependencies() {
  eval "$(parseargs $@)"

  local hasType=$(hasDependencyType $@)
  if [ "$hasType" == "false" ] ; then
    printf "$dependencyGroup not present in $pkgJsonFile, removal of package $package unnecessary\n"
    return 0
  fi

  local javascriptParm="$(cat $pkgJsonFile)"
  local javascript="$(cat <<EOF
    obj = JSON.parse(process.argv[1]);
    delete obj.${dependencyGroup}['${package}'];
    JSON.stringify(obj, null, 2);
EOF
  )"
  echo "$(node -pe "${javascript}" "${javascriptParm}")" > $pkgJsonFile
}


changeDependencyVersion() {
  eval "$(parseargs $@)"

  local hasType=$(hasDependencyType $@)
  if [ "$hasType" == "false" ] ; then
    printf "$dependencyGroup not present in $pkgJsonFile, cannot edit $package version\n"
    return 0
  fi

  local javascriptParm="$(cat $pkgJsonFile)"
  local javascript="$(cat <<EOF
    obj = JSON.parse(process.argv[1]);
    obj.${dependencyGroup}['${package}'] = "$newVersion";
    JSON.stringify(obj, null, 2);
EOF
  )"
  echo "$(node -pe "${javascript}" "${javascriptParm}")" > $pkgJsonFile

}


removeScriptFromScripts() {
  local script="$1"
  local pkgJsonFile="$2"
  [ ! $pkgJsonFile ] && pkgJsonFile=$(pwd)/package.json
  if [ ! -f $pkgJsonFile ] ; then
    echo "ERROR! No such file: $pkgJsonFile"
    return 1
  fi
  local javascriptParm="$(cat $pkgJsonFile)"
  local javascript="$(cat <<EOF
    obj = JSON.parse(process.argv[1]);
    delete obj.scripts['${script}'];
    JSON.stringify(obj, null, 2);
EOF
  )"
  echo "$(node -pe "${javascript}" "${javascriptParm}")" > $pkgJsonFile
}


getKualiDependencies() {
  local pkgfile="$1"
  local dependencyType="$2"
  local dependencies=$(node -pe \
  "var dependencies = JSON.parse(process.argv[1]).$dependencyType; \
    for(var d in dependencies) { \
      if(/@kuali/.test(d)) { \
        console.log(d + \":\" + dependencies[d]); \
      } \
    }" \
  "$(cat $pkgfile)")
  for d in $dependencies ; do
    [ $d == 'undefined' ] && continue
    echo $d
  done
}


# Determine if a package exists in a specified dependency type (dev, optional, peer, etc).
# ARGS:
#   dependencyGroup: The dependency type (dev, optional, peer, etc)
#   pkgJsonFile: The package.json file from which to remove the dependency.
#
hasDependencyType() {
  eval "$(parseargs $@)"
  local javascriptParm="$(cat $pkgJsonFile)"
  local missing=$(node -pe "obj = JSON.parse(process.argv[1]); console.log(obj.${dependencyGroup} == undefined)" "${javascriptParm}" | grep -P '^(true)|(false)$')
  [ "$missing" == "true" ] && echo "false" || echo "true"
}


# Make sure the expected git rsa keys are in the build context directory, pulling them from s3 if not found.
getGitKeys() {
  local s3bucket="$1"
  local targetdir="$2"
  local keynames=${@:3}
 
  getRsaKeys \
    "$s3bucket" \
    "$targetdir" \
    "${keynames[@]}"

  local missingKeys=$(listRsaKeys $targetdir ${keynames[@]})

  if [ -n "$missingKeys" ] ; then
    missingKeys=$(echo "$missingKeys" | sed 's/[[:space:]]/, /g')
    local msg="\nERROR! Missing one or more rsa keys: $missingKeys \n"
    msg="${msg}Docker image creation requires the key(s) be present in the build context directory \n"
    msg="${msg}in order to access the BU github repository for the source code. \n"
    msg="${msg}Cancelling the build. Rerun when the keys have been obtained.\n"
    printf "$msg"
    return 1
  fi
}


# Each arg to this function is the name of a file that is expected to be found in
# the current directory. Each file that is not found is echoed out.
# ARGUMENTS:
#   arg1: The directory being searched for the rsa keys
#   arg...argN: All remaining arguments are the rsa key names.
#
listRsaKeys() {
  local keydir="$1"
  # An expression like to search for files ending in "_rsa" NOTE: sed used to fix "//" in lsexpr if $keydir already ended in "/".
  local needKeys=(${@:2})
  local haveKeys=($(echo "$(ls -1 $keydir | grep '_rsa$')"))
  local missKeys=()

  for nkey in ${needKeys[@]} ; do
    for hkey in ${haveKeys[@]} ; do
      # Strip off any leading directory path
      local key=$(echo $hkey | grep -oP '/?[^/]+$' | sed 's/\///g')
      [ "${nkey,,}" == "${key,,}" ] && continue 2
    done
    missKeys=(${missKeys[@]} $nkey)
  done

  [ ${#missKeys[@]} -eq 0 ] && return

  echo "${missKeys[@]}"
}

# Download from s3 each specified rsa key that cannot be found in the specified directory.
# ARGUMENTS:
#   arg1: The name of the bucket or bucket/path where the keys are stored in s3.
#   arg2: The local directory where the keys are supposed to be found or downloaded to.
#   arg3...argN: All remaining arguments are the rsa key names.
#
getRsaKeys() {
  local s3bucket="$1"
  local localdir="$2"
  local missing=$(listRsaKeys $localdir ${@:3})

  for key in ${missing[@]} ; do
    echo "Cannot find $localdir/$key! Downloading from s3 bucket"
    s3Get \
      "source=$s3bucket/$key" \
      "target=$localdir/$key"
  done
}


detectOS() {
  local os="$(echo $OSTYPE)"
  [ -n "$(echo "$os" | grep -iP '(msys)|(windows)')" ] && echo "windows" && return 0
  [ -n "$(echo "$os" | grep -iP 'linux')" ] && echo "linux" && return 0
  # Assuming only 3 types of operating systems for now.
  echo "mac"
}
isWindows() { [ "$(detectOS)" == "windows" ] && true || false; }
isLinux() { [ "$(detectOS)" == "linux" ] && true || false; }
isMac() { [ "$(detectOS)" == "mac" ] && true || false; }

# Convert a UNIX path to a DOS path if on windows (forward slash to double backslash), if not already.
# Assumes that any UNIX path starting with a single letter path segment (like /c/) refers to a DOS drive letter.
getOSPath() {
  if isWindows ; then
    echo "$1" | sed -r 's/^\/([a-zA-Z])\//\1:\\\\/' | sed 's/\//\\\\/g'
  else
    echo "$1"
  fi
}

AwsCliInstalled() {
  aws --version > /dev/null 2>&1 && [ "$?" == "0" ] && true || false
}

GitInstalled() {
  git --version > /dev/null 2>&1 && [ "$?" == "0" ] && true || false
}

DockerInstalled() {
  docker --version > /dev/null 2>&1 && [ "$?" == "0" ] && true || false
}

DockerRunning() {
  docker ps > /dev/null 2>&1 && [ "$?" == "0" ] && true || false
}

isLocalHost() {
  # Determine if uri is for localhost
  retval=$(echo "${1,,}" | grep -iP '((localhost)|(127\.0\.0\.1))(:\d+)?')
}

YarnInstalled() {
  yarn --version > /dev/null 2>&1 && [ "$?" == "0" ] && true || false
}

NodeInstalled() {
  node --version > /dev/null 2>&1 && [ "$?" == "0" ] && true || false
}

getCurrentDirectoryName() {
  echo -n "$(pwd)" | sed 's/\//\n/g' | tail -n1
}

getParentDirectoryName() {
  echo -n "$(dirname $(pwd))" | sed 's/\//\n/g' | tail -n1
}

