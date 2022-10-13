#!/bin/bash

cmdfile=last-cmd.sh

TEMPLATE_BUCKET=${TEMPLATE_BUCKET:-"kuali-conf"}

declare -A kualiTags=(
  [Service]='research-administration'
  [Function]='kuali'
)

# Keep a record of yaml templates that have been validated and uploaded to s3 so as to ignore repeats
declare -A validatedStacks=()
declare -A uploadedTemplates=()

inDebugMode() {
  [[ "$-" == *x* ]] && true || false
}

outputHeading() {
  inDebugMode && set +x && local returnToDebugMode='true'
  local msg="$1"
  [ -n "$outputHeadingCounter" ] && msg="$outputHeadingCounter) $msg" && ((outputHeadingCounter++))
  local border='###############################################################################################################################################'
  echo ""
  echo ""
  echo "$border"
  echo "       $msg"
  echo "$border"
  [ "$returnToDebugMode" == 'true' ] && set -x || true
}

outputSubHeading() {
  inDebugMode && set +x && returnToDebugMode='true'
  local msg="$1"
  local border='------------------------------------------------------------------------------------'
  echo ""
  echo "$border"
  echo "       $msg"
  echo "$border"
  [ "$returnToDebugMode" == 'true' ] && set -x || true
}

getCurrentDir() {
  local thisdir=$(pwd | awk 'BEGIN {RS="/"} {print $1}' | tail -1)
  if [ -z "$thisdir" ] ; then
    # For some reason, this comes up blank when run in the context of a jenkins job.
    # Using this second method works, but beware of it when running on a mac (blows up due to -P switch)
    thisdir=$(pwd | grep -oP '[^/]+$')
  fi
  echo "$thisdir"
}

isCurrentDir() {
  local askDir="$1"
  # local thisDir="$(pwd | grep -oP '[^/]+$')"  # Will blow up if run on mac (-P switch)
  local thisDir="$(getCurrentDir)"
  [ "$askDir" == "$thisDir" ] && true || false
}

parseArgs() {
  for nv in $@ ; do
    [ -z "$(grep '=' <<< $nv)" ] && continue;
    name="$(echo $nv | cut -d'=' -f1)"
    value="$(echo $nv | cut -d'=' -f2-)"
    if [ "${name^^}" != 'SILENT' ] && [ "$SILENT" != 'true' ] ; then
      echo "${name^^}=$value"
    fi
    eval "${name^^}=$value" 2> /dev/null || true
  done
  if [ -n "$PROFILE" ] ; then
    export AWS_PROFILE=$PROFILE 
    [ "$SILENT" != 'true' ] && echo "export AWS_PROFILE=$PROFILE"
  # elif [ -z "$DEFAULT_PROFILE" ] ; then
  #   if [ "$task" != 'validate' ] ; then
  #     echo "Not accepting a blank profile. If you want the default then use \"profile='default'\" or default_profile=\"true\""
  #     exit 1
  #   fi
  fi
}

# Another function can pass all it's argument list to this function and will get a string
# in return, which when run with eval, will set all any name=value pairs found in that argument list as local variables.
getEvalArgs() {
  local cse="${1,,}"
  if [ "$cse" != 'lowercase' ] && [ "$cse" != 'uppercase' ] ; then
    cse=''
  fi
    
  evalstr=""
  for nv in $@ ; do
    [ -z "$(grep '=' <<< $nv)" ] && continue;
    name="$(echo $nv | cut -d'=' -f1)"
    value="$(echo $nv | cut -d'=' -f2-)"
    local NV="$nv"
    case "$cse" in
      lowercase) NV="${name,,}=$value" ;;
      uppercase) NV="${name^^}=$value" ;;
    esac
    if [ -z "$evalstr" ] ; then
      evalstr="local $NV"
    else
      evalstr="$evalstr && local $NV"
    fi
  done
  echo "$evalstr"
}

setDefaults() {
  # Set explicit defaults first
  [ -z "$GLOBAL_TAG" ] && GLOBAL_TAG="${defaults['GLOBAL_TAG']}"
  for k in ${!defaults[@]} ; do
    [ -n "$(eval 'echo $'$k)" ] && continue; # Value is not empty, so no need to apply default
    local val="${defaults[$k]}"
    if grep -q '\$' <<<"$val" ; then
      eval "val=$val"
    elif [ ${val:0:14} == 'getLatestImage' ] ; then
      [ "$task" == 'delete-stack' ] && continue;
      val="$($val)"
    fi
    local evalstr="[ -z \"\$$k\" ] && $k=\"$val\""
    eval "$evalstr"
    [ "$SILENT" != 'true' ] && echo "$k = $val"
  done

  # Set contingent defaults second
  tempath="$(dirname "$TEMPLATE")"
  if [ "$tempath" != "." ] ; then
    TEMPLATE_PATH=$tempath
    # Strip off the directory path and reduce down to file name only.
    TEMPLATE=$(echo "$TEMPLATE" | grep -oP '[^/]+$')
    [ "$SILENT" != 'true' ] && echo "TEMPLATE = $TEMPLATE"
  fi
  # Trim off any trailing forward slashes
  TEMPLATE_PATH=$(echo "$TEMPLATE_PATH" | sed 's/\/*$//')
  [ "$SILENT" != 'true' ] && echo "TEMPLATE_PATH = $TEMPLATE_PATH"
  # Get the http location of the bucket path 
  BUCKET_URL="$(echo "$TEMPLATE_BUCKET_PATH" | sed 's/s3:\/\//https:\/\/s3.amazonaws.com\//')"
  [ "$SILENT" != 'true' ] && echo "BUCKET_URL = $BUCKET_URL"
  # Fish out just the bucket name from the larger bucket path
  TEMPLATE_BUCKET_NAME="$(echo "$TEMPLATE_BUCKET_PATH" | grep -oP '(?<=s3://)([^/]+)')"
  [ "$SILENT" != 'true' ] && echo "TEMPLATE_BUCKET_NAME = $TEMPLATE_BUCKET_NAME"
}

validateOne() {
  local f="$1"
  local root="$2"
  # Debug output would also make it into the validate.invalid file, so turn it of temporarily
  inDebugMode && set +x && local returnToDebugMode='true'
  validate "$f" >> $root/validate.valid 2>> $root/validate.invalid
  [ "$returnToDebugMode" == 'true' ] && set -x || true
}

# Validate one or all cloudformation yaml templates.
validateStack() {
  eval "$(getEvalArgs $@)"
  local root=$(pwd)
    
  [ -d "$TEMPLATE_PATH" ] && cd $TEMPLATE_PATH
  rm -f $root/validate.valid 2> /dev/null
  rm -f $root/validate.invalid 2> /dev/null

  # find . -type f -iname "*.yaml" | \
  while read line; do \
    [ "${line:0:12}" == './.metadata/' ] && continue
    local f=$(printf "$line" | sed 's/^.\///'); \
    [ -n "$TEMPLATE" ] && [ "$TEMPLATE" != "$f" ] && continue;
    if [ "${validatedStacks[$f]}" == "$f" ] ; then
      echo "$f already validated once - skipping..."
      continue
    fi

    printf "validating $f";

    validateOne "$f" "$root"

    if [ $? -gt 0 ] ; then
      echo $f >> $root/validate.invalid
    else
      validatedStacks["$f"]="$f"
    fi
    echo " "
  done <<< "$([ -n "$filepath" ] && echo $filepath || find . -type f -iname "*.yaml")"
  cd $root
  if [ -z "$silent" ] ; then
    cat $root/validate.valid
  fi
  if isValidationError "$root/validate.invalid" ; then
    cat $root/validate.invalid
    rm -f $root/validate.invalid 2> /dev/null
    rm -f $root/validate.valid 2> /dev/null
    exit 1
  else
    rm -f $root/validate.invalid 2> /dev/null
    rm -f $root/validate.valid 2> /dev/null
    echo "SUCCESS! (no errors found)"
  fi
}

validateTemplateAndUploadToS3() {
  eval "$(getEvalArgs $@)"

  if [ "${validatedStacks[$filepath]}" == "$filepath" ] ; then
    echo "$f already validated once - skipping..."
  else
    validateStack silent=$silent filepath=$filepath
    [ $? -gt 0 ] && exit 1
    validatedStacks["$filepath"]="$filepath"
  fi

  if endsWith "$s3path" '/' ; then
    # The target path indicates an s3 "directory", so the file being copied will retain the same name
    # by default. However, in order to keep track of it, it's necessary to extend the path to include the file name.
    s3path="$s3path$(echo $filepath | awk 'BEGIN {RS="/"} {print $1}' | tail -1)"
  fi
  if [ "${uploadedTemplates[$s3path]}" == "$s3path" ] ; then
    echo "$s3path already uploaded to s3 - skipping..."
  else
    copyToBucket $filepath $s3path
    [ $? -gt 0 ] && exit 1
    uploadedTemplates["$s3path"]="$s3path"
  fi
}

isValidationError() {
  local errfile="$1"
  local err='false'
  if [ -f $errfile ] && [ -n "$(cat $errfile)" ]; then
    # A template over 51200 will show up as an error, but will be ok as long as it is obtained from an s3 bucket when running stack operation. 
    [ -z "$(cat $errfile | grep 'Member must have length less than or equal to 51200')" ] && err='true'
  fi
  [ $err == 'true' ] && true || false
}

# Validate a single cloudformation yaml file
validate() {
  local template="$1"
  printf "\n\n$template:" && \
  aws cloudformation validate-template --template-body "file://./$template"
}

# Upload one or all cloudformation yaml templates to s3
uploadStack() {
  validateStack silent=true

  [ $? -gt 0 ] && exit 1

  if [ -n "$TEMPLATE" ] ; then
    if [ -f $TEMPLATE ] ; then
      echo "aws s3 cp $TEMPLATE $TEMPLATE_BUCKET_PATH/" > $cmdfile
    elif [ -f $TEMPLATE_PATH/$TEMPLATE ] ; then
      echo "aws s3 cp $TEMPLATE_PATH/$TEMPLATE $TEMPLATE_BUCKET_PATH/" > $cmdfile
    else
      echo "$TEMPLATE not found!"
      exit 1
    fi
  else
    cache() {
      echo "aws s3 cp $1 $TEMPLATE_BUCKET_PATH/" >> $cmdfile
    }
    printf "" > $cmdfile
    find . \
      -type f \
      -name "*.yaml" \
      -not -path './.metadata/*' \
      | while read file; do cache "$file"; done
  fi

  if isDebug || isDryrun ; then
    cat $cmdfile
    return 0
  fi
  
  # Create an s3 bucket for the app if it doesn't already exist
  if ! isAccessibleBucket "$TEMPLATE_BUCKET_NAME" ; then
    aws s3 mb s3://$TEMPLATE_BUCKET_NAME
  fi

  # Write stdout and stderr to separate files AND see them both as terminal output.
  # (NOTE: You have to swap stdout and stderr because tee can only accept stdout)
  (sh $cmdfile | tee last-cmd-stdout.log) 3>&1 1>&2 2>&3 | tee last-cmd-stderr.log

  local lastOutput=$?
  local retval=0
  if [ $lastOutput -gt 0 ] ; then
    retval=$lastOutput
    if [ -n "$(cat last-cmd-stderr.log | grep -P '^(?!warning: Skipping file )(.+?)$')" ] ; then
      # These errors came from warnings issued by aws s3 cp due to a bug with the length of file names. Ignore it.
      # SEE: https://github.com/aws/aws-cli/issues/3514
      retval=0
    fi
  fi
  [ -f last-cmd-stdout.log ] && rm -f last-cmd-stdout.log
  [ -f last-cmd-stderr.log ] && rm -f last-cmd-stderr.log
  return $retval
}

# Add on key=value parameter entry to the construction of an aws cli function call to 
# perform a create/update stack action.
addParameter() {
  local cmdfile="$1"
  local key="$2"
  local value="$3"
  if [ -n "$value" ] ; then
    [ -n "$(cat $cmdfile | grep 'ParameterKey')" ] && local comma=","
    cat <<-EOF >> $cmdfile
          ${comma}{
            "ParameterKey" : "$key",
            "ParameterValue" : "$value"
          }
EOF
    true
  else
    false
  fi
}

# Add on key=value parameter entry to the construction of an aws cli function call to 
# perform a create/update stack action. Only the name is provided, not the value, which must be determined here.
add_parameter() {
  eval 'local value=$'$3
  [ -z "$value" ] && return 0
  addParameter "$1" "$2" "$value"
}

# Add on key=value tag entry to the construction of an aws cli function call to 
# perform a create/update stack action.
addTag() {
  local cmdfile="$1"
  local key="$2"
  local value="$3"
  [ -n "$(cat $cmdfile | grep '\"Key\" :')" ] && local comma=","
  cat <<-EOF >> $cmdfile
        ${comma}{
          "Key" : "$key",
          "Value" : "$value"
        }
EOF
}

addStandardTags() {
  addTag $cmdfile 'Service' ${SERVICE:-${kualiTags['Service']}}
  addTag $cmdfile 'Function' ${FUNCTION:-${kualiTags['Function']}}
  if [ -n "$LANDSCAPE" ] ; then
    addTag $cmdfile 'Landscape' "$LANDSCAPE"
  fi
}

# Get the id of the default vpc in the account. (Assumes there is only one default)
getDefaultVpcId() {
  local id=$(aws ec2 describe-vpcs \
    --output text \
    --filters "Name=is-default,Values=true" \
    --query 'Vpcs[0].VpcId' 2> /dev/null)
  [ ! "$id" ] && return 1
  [ "${id,,}" == 'none' ] && return 1
  echo $id
}

# Get the id of the internet gateway within the specified vpc
getInternetGatewayId() {
  local vpcid="$1"
  if [ -n "$vpcid" ] ; then
    local gw=$(aws ec2 describe-internet-gateways \
      --output text \
      --query 'InternetGateways[].[InternetGatewayId, Attachments[].VpcId]' \
      | grep -B 1 $vpcid \
      | grep 'igw' 2> /dev/null)
  fi
  [ ! "$gw" ] && return 1
  [ "${gw,,}" == 'none' ] && return 1
  echo $gw
}

getTransitGatewayId() {
  local vpcId="$1"
  local matches=()

  for tgw in $(aws \
    ec2 describe-transit-gateways \
    --output text --query 'TransitGateways[*].[TransitGatewayId]'
  ) ; do
    for state in $(aws ec2 describe-transit-gateway-attachments \
      --output text \
      --query 'TransitGatewayAttachments[?TransitGatewayId==`'$tgw'`&&ResourceId==`'$vpcId'`].State'
    ); do
      if [ "${state,,}" == "available" ] ; then
        matches=(${matches[@]} $tgw)
      fi 
    done
  done

  [ ${#matches[@]} -eq 1 ] && echo ${matches[0]}
}

getVpcId() {
  local subnetId="$1"
  aws \
    ec2 describe-subnets \
    --subnet-ids $subnetId \
    --output text \
    --query 'Subnets[].VpcId' 2> /dev/null
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

# Provided a base identifier and one or two filters, lookup subnets matching the filter(s) and make variable assignments from
# some of the subnet properties. The variables names will start with the base identifier (ie: for base id "PUBLIC_SUBNET", cidr: "PUBLIC_SUBNET1_CIDR") 
getSubnets() {
  local globalVar="$1"
  local filter1="$2"
  local filter2="$3"

  while read subnet ; do
    local az="$(echo "$subnet" | awk '{print $1}')"
    local cidr="$(echo "$subnet" | awk '{print $2}')"
    local subnetId="$(echo "$subnet" | awk '{print $3}')"
    local vpcId="$(echo "$subnet" | awk '{print $4}')"
    if [ -n "$vpcId" ] && [ -z "$(grep -i 'VpcId' $cmdfile)" ]; then        
      echo "VpcId="$vpcId"" >> $cmdfile
      echo "VPC_ID="$vpcId"" >> $cmdfile
    fi
    if [ -z "$(eval echo "\$${globalVar}1")" ] ; then
      if [ "$(eval echo "\$${globalVar}2")" != "$subnetId" ] ; then
        echo "Found first $(echo ${globalVar,,} | sed 's/_/ /'): $subnet"
        eval "${globalVar}1="$subnetId""
        echo "${globalVar}1="$subnetId"" >> $cmdfile
        echo "${globalVar}1_AZ="$az"" >> $cmdfile
        echo "${globalVar}1_CIDR="$cidr"" >> $cmdfile
        continue
      fi
    fi
    if [ -z "$(eval echo "\$${globalVar}2")" ] ; then
      if [ "$(eval echo "\$${globalVar}1")" != "$subnetId" ] ; then
        echo "Found second $(echo ${globalVar,,} | sed 's/_/ /'): $subnet"
        eval "${globalVar}2="$subnetId""
        echo "${globalVar}2="$subnetId"" >> $cmdfile
        echo "${globalVar}2_AZ="$az"" >> $cmdfile
        echo "${globalVar}2_CIDR="$cidr"" >> $cmdfile
      fi
    fi
  done <<< "$(
    aws ec2 describe-subnets \
      --filters $filter1 $filter2 \
      --output text \
      --query 'sort_by(Subnets, &AvailabilityZone)[*].{VpcId:VpcId,SubnetId:SubnetId,AZ:AvailabilityZone,CidrBlock:CidrBlock}'
  )"
}

# Ensure that there are 6 subnets are specified (2 campus subnets, 2 private subnets and 2 public subnets).
# If any are not provided, then look them up with the cli against their tags and assign them accordingingly.
# If any are provided, look them up to validate that they exist as subnets.
checkSubnets() {
  # Clear out the last command file
  printf "" > $cmdfile

  getSubnets \
    'CAMPUS_SUBNET' \
    'Name=tag:Network,Values=Campus' \
    'Name=tag:aws:cloudformation:logical-id,Values=CampusSubnet1,CampusSubnet2'

  getSubnets \
    'PRIVATE_SUBNET' \
    'Name=tag:Network,Values=Campus' \
    'Name=tag:aws:cloudformation:logical-id,Values=PrivateSubnet1,PrivateSubnet2'

  getSubnets \
    'PUBLIC_SUBNET' \
    'Name=tag:Network,Values=World'

  cat ./$cmdfile
  source ./$cmdfile

  # Count how many subnets have values
  local subnets=$(grep -P '_SUBNET\d=' $cmdfile | wc -l)
  if [ $subnets -lt 6 ] ; then
    # Some subnets might have been explicitly provided by the user as a parameter, but look those up to verify they exist.
    if [ -z "$(grep 'PRIVATE_SUBNET1' $cmdfile)" ] ; then
      subnetExists "$PRIVATE_SUBNET1" && ((subnets++)) && echo "PRIVATE_SUBNET1=$PRIVATE_SUBNET1"
    fi    
    if [ -z "$(grep 'CAMPUS_SUBNET1' $cmdfile)" ] ; then
      subnetExists "$CAMPUS_SUBNET1" && ((subnets++)) && echo "CAMPUS_SUBNET1=$CAMPUS_SUBNET1"
    fi
    if [ -z "$(grep 'PUBLIC_SUBNET1' $cmdfile)" ] ; then
      subnetExists "$PUBLIC_SUBNET1" && ((subnets++)) && echo "PUBLIC_SUBNET1=$PUBLIC_SUBNET1"
    fi
    if [ -z "$(grep 'PRIVATE_SUBNET2' $cmdfile)" ] ; then
      subnetExists "$PRIVATE_SUBNET2" && ((subnets++)) && echo "PRIVATE_SUBNET2=$PRIVATE_SUBNET2"
    fi    
    if [ -z "$(grep 'CAMPUS_SUBNET2' $cmdfile)" ] ; then
      subnetExists "$CAMPUS_SUBNET2" && ((subnets++)) && echo "CAMPUS_SUBNET2=$CAMPUS_SUBNET2"
    fi
    if [ -z "$(grep 'PUBLIC_SUBNET2' $cmdfile)" ] ; then
      subnetExists "$PUBLIC_SUBNET2" && ((subnets++)) && echo "PUBLIC_SUBNET2=$PUBLIC_SUBNET2"
    fi
    # If we still don't have a total of 6 subnets then exit with an error code
  fi
  [ $subnets -lt 6 ] && echo "ERROR! Must have 6 subnets (2 public, 2 campus, 2 private)" && echo "1 or more are missing and could not be found with cli."
  [ $subnets -lt 6 ] && false || true
}

# If a bucket does not exist in this account, it may be a bucket in another account for which this account has access.
# Access is determined by performing ls against the bucket. 
# DISCLAIMER: This is an arbitrary test because the bucket may exist, but without the list-objects policy action granted
# Alternatively, the bucket may still exist somewhere out there globally.
# For now the list-objects test covers the use cases so far.
isAccessibleBucket() {
  local bucketname="$1"
  local accessible='false'
  if ! bucketExistsInThisAccount $bucketname ; then
    aws ls $bucketname
    [ $? == 0 ] && accessible='true'
  fi
  [ $accessible == 'true' ] && true || false
}

# Will determine if an S3 bucket exists, even if it is empty.
# NOTE: You must call scripts that use this with bash, not sh, else the pipe to the while loop syntax will not be recognized.
bucketExistsInThisAccount() {
  local bucketname="$1"

  findBucket() {
    while read b ; do
      b=$(echo "$b" | awk '{print $3}')
      if [ "$b" == "$bucketname" ] ; then
        return 0
      fi
    done
    return 1
  }

  aws s3 ls 2> /dev/null | findBucket
  [ $? -eq 0 ] && true || false
}

isDryrun() {
  if [ "${1,,}" == 'dryrun' ] || [ "${1,,}" == 'true' ] ; then
    local dryrun="$1"
  elif [ "${DRYRUN,,}" == 'true' ] ; then
    local dryrun="true"
  fi
  [ -n "$dryrun" ] && true || false
}

isDebug() {
  if [ "${1,,}" == 'debug' ] || [ "${1,,}" == 'true' ] ; then
    local debug="$1"
  elif [ "${DEBUG,,}" == 'true' ] ; then
    local debug="true"
  fi
  [ -n "$debug" ] && true || false
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
convertPath() {
  if isWindows ; then
    echo "$1" | sed -r 's/^\/([a-zA-Z])\//\1:\\\\/' | sed 's/\//\\\\/g'
  else
    echo "$1"
  fi
}

startsWith() {
  local string="$1"
  local chars="$2"
  [ "$chars" == "$(echo ${string:0:${#chars}})" ] && true || false
}

endsWith() {
  local string="$1"
  local chars="$2"
  [ -n "$(echo "$string" | grep -oP $chars'$')" ] && true || false
}

getStackToDelete() {
  [ -n "$STACK_TO_DELETE" ] && echo "$STACK_TO_DELETE" && return 0
  [ -n "$FULL_STACK_NAME" ] && echo "$FULL_STACK_NAME" && return 0
}

waitForStack() {
  local task="$1"
  local stackname="$2"
  local interval=${3:-5}
  local counter=1
  if [ -z "$stackname" ] ; then
    stackname="$STACK_NAME"
    if [ -n "$stackname" ] ; then
      [ -n "$LANDSCAPE" ] && stackname="$stackname-$LANDSCAPE"
    fi
  fi
  if [ -z "$stackname" ] ; then
    echo "This feature has been improperly called: Missing the name of the stack to monitor."
    echo "You will need to use the aws management console or the cli to acertain stack status."
    exit 0
  fi
  while true ; do
    status="$(
      aws cloudformation describe-stacks \
        --stack-name $stackname \
        --output text \
        --query 'Stacks[].{status:StackStatus}' 2> /dev/null
    )"
    case "${task,,}" in
      create) successStatus="CREATE_COMPLETE" ;;
      update) successStatus="UPDATE_COMPLETE" ;;
      delete) successStatus="DELETED" ;;
      *) echo "ERROR! Unknown stack operation: $task" && return 1
    esac
    [ -z "$status" ] && status="$successStatus"
    echo "$stackname stack status check $counter: $status"
    ([ -n "$(echo $status | grep -Pi '_COMPLETE$')" ] || [ $status == "$successStatus" ] || [ -n "$(echo $status | grep -Pi '_FAILED$')" ]) && break
    ((counter++))
    sleep $interval
  done
  if [ "$status" == $successStatus ] ; then
    if [ "$task" == 'delete' ] ; then
      echo " "
      echo "Finished."
    else
      outputHeading "Stack outputs:"
      while read p ; do
        local key="$(echo ''$p'' | jq '.OutputKey')"
        local val="$(echo ''$p'' | jq '.OutputValue')"
        echo "$key: $val"
      done <<<$(
        aws cloudformation describe-stacks \
        --stack-name=$stackname \
        --query 'Stacks[].{Outputs:Outputs}' \
        | jq -c '.[0].Outputs' \
        | jq -c '.[]' 2> /dev/null
      )
      # for p in $(
      #   aws cloudformation describe-stacks \
      #   --stack-name=$stackname \
      #   --query 'Stacks[].{Outputs:Outputs}' \
      #   | jq -c '.[0].Outputs' \
      #   | jq -c '.[]'
      # ) ; do
      #   local key="$(echo ''$p'' | jq '.OutputKey')"
      #   local val="$(echo ''$p'' | jq '.OutputValue')"
      #   echo "$key: $val"
      # done
    fi
    true
  else
    outputHeading "STACK CREATION FAILED ($stackname)"
    aws cloudformation describe-stack-events --stack-name=$stackname
    false
  fi
}

waitForStackToDelete() {
  waitForStack 'delete' "${1:-$(getStackToDelete)}" && true || false
}

waitForStackToCreate() {
  waitForStack 'create' "${1:-$FULL_STACK_NAME}" 10 && true || false
}

waitForStackToUpdate() {
  waitForStack 'update' "${1:-$FULL_STACK_NAME}" && true || false
}

# The aws cloudformation stack create/update command has just been constructed.
# Prompt the user and/or run it according to certain flags.
runStackActionCommand() {
  
  if isDryrun ; then
    outputHeading "DRYRUN: Would execute the following to trigger cloudformation..."
    cat $cmdfile
    exit 0
  fi

  if [ "$PROMPT" == 'false' ] ; then
    printf "\nExecuting the following command(s):\n\n$(cat $cmdfile)\n"
    local answer='y'
  else
    printf "\nExecute the following command?:\n\n$(cat $cmdfile)\n\n(y/n): "
    read answer
  fi

  if [ "$answer" == "y" ] ; then
    sh $cmdfile
    if [ $? -gt 0 ] ; then
      echo "Cancelling due to error..."
      exit 1
    fi
  else 
    echo "Cancelled."
    exit 0
  fi

  echo "Stack command issued."
  case "${task:0:6}" in
    create)
      if ! waitForStackToCreate ; then
        exit 1
      fi
      ;;
    update)
      if ! waitForStackToUpdate ; then
        exit 1
      fi
      ;;
  esac
}

getStackParameter() {
  local parmname="$1"
  local stackname="$2"
  aws cloudformation describe-stacks \
    --stack-name=$stackname \
    --query 'Stacks[].{Parameters:Parameters}' | \
    jq -r '.[0].Parameters[] | select(.ParameterKey == "'$parmname'").ParameterValue' 2> /dev/null
}

copyToBucket() {
  local src="$1"
  local tar="$2"
  if isDryrun ; then
    echo "Dryrun: aws s3 cp $src $tar"
  else
    eval "aws s3 cp $src $tar"
  fi
}

# Cloudformation can only delete a bucket if it is empty (and has no versioning), so empty it out here.
emptyBuckets() {
  local success='true'
  for bucket in $@ ; do
    if bucketExistsInThisAccount "$bucket" ; then
      echo "aws s3 rm s3://$bucket --recursive..."
      aws s3 rm s3://$bucket --recursive
      local errcode=$?
      if [ $errcode -gt 0 ] ; then
        echo "ERROR! Emptying bucket $bucket, error code: $errcode"
        success='false'
      fi
      # aws s3 rb --force $bucket
    else
      echo "Cannot empty bucket $bucket. Bucket does not exist."
    fi
  done
  [ $success == 'true' ] && true || false
}

getEcrRegistryName() {
  if [ -n "$REGISTRY" ] ; then
    echo "$REGISTRY"
  else
    local accountId="$(aws sts get-caller-identity --output text --query 'Account')"
    [ -z "$accountId" ] && echo "Error retrieving account ID!" && exit 1
    REGISTRY="$(getAccountId).dkr.ecr.$(getRegion).amazonaws.com"
    echo "$REGISTRY"
  fi
}

getAccountId() {
  if [ -n "$ACCOUNT_ID" ] ; then
    echo "$ACCOUNT_ID"
  else
    local accountId="$(aws sts get-caller-identity --output text --query 'Account' 2> /dev/null)"
    [ -z "$accountId" ] && echo "Error retrieving account ID!" && exit 1
    echo "$accountId"
  fi
}

getRegion() {
  if [ -n "$AWS_DEFAULT_REGION" ] ; then
    echo "$AWS_DEFAULT_REGION"
  else
    aws configure get region
  fi
}

getAwsProfile() {
  echo ${AWS_PROFILE:-${PROFILE:-""}}
}

getImageName() {
  if [ -n "$DOCKER_IMAGE" ] ; then
    echo "$DOCKER_IMAGE"
  else
    echo $(getEcrRegistryName)/$imageShortName
  fi
}

runCommand() {
  echo "$1"
  isDryrun && echo "Dryrun." || eval "$1"
}

activeCredentials() {
  aws --profile=$(getAwsProfile) sts get-caller-identity 2> /dev/null
  if [ $? -eq 0 ] ; then
    true
  else
    cat <<EOF

      Operation cancelled!
      You don't appear to have any active credentials.
      Check your ~/.aws/credentials file 
        1) Make sure the default profile is set and has not expired (AWS_SESSION_TOKEN is present)
        or...
        2) Make sure there is a named profile set in the environment:
           export AWS_PROFILE=[profile_name]
           where:
           a) "profile_name" is represented in your ~/.aws/credentials file
           and...
           b) The profile has not expired (AWS_SESSION_TOKEN is present)
      
EOF
    false
  fi
}

isDnsRdsAddress() {
  local match="$(echo $DB_HOST | grep -oP '^[a-zA-Z0-9]+\.db\.kuali.*$')"
}
