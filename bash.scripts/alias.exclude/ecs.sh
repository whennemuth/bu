#!/bin/bash

declare -A defaults=(
  [bucketPath]='s3://kuali-research-ec2-setup/ecs/cloudformation'
  [templatePath]='/c/whennemuth/workspaces/ecs_workspace/cloud-formation/kuali-infrastructure'
  [templateType]='yaml'
  [ConfigBucket]='kuali-research-ec2-setup'
  [DockerImageTag]='2001.0040'
  [DockerRepositoryURI]='730096353738.dkr.ecr.us-east-1.amazonaws.com/core'
  [EC2InstanceType]='t2.small'
  [Landscape]='sb'
)

ecsValidate() {
  local here="$(pwd)"
  cd $templateDir

  local extensions=('json' 'template' 'yaml' 'yml')
  if [ "$templateType" == 'json' ] ; then
    local extensions=('json' 'template')
  elif [ "$templateType" == 'yaml' ] ; then
    local extensions=('yaml' 'yml')
  fi

  for extension in "${extensions[@]}" ; do
    # echo "$({ find . -type f -iname '*.yaml' & find . -type f -iname '*.template'; })" | \
    find . -type f -iname "*.$extension" | \
    while read line; do \
      local f=$(printf "$line" | sed 's/^.\///'); \
      [ -n "$templateName" ] && [ "$templateName" != "$f" ] && continue; \
      printf $f; \
      aws cloudformation validate-template --template-body "file://./$f"
      echo " "
    done
  done
  cd $here
}


ecsUpload() {
  if [ -f $templatePath ] ; then
    bucketPath=$bucketPath/$templateName
    aws s3 cp  $templatePath $bucketPath
  else
    aws s3 cp  $templatePath $bucketPath \
      --exclude=* \
      --include=*.template \
      --recursive
  fi
}


ecsStackAction() {  
  if [ $# -ge 1 ] ; then
    local action=$1

    if [ "$action" == 'delete-stack' ] ; then
      aws cloudformation $action --stack-name $stackName
      
      [ $? -gt 0 ] && echo "Cancelling..." && return 1
    else
      ecsupload $templatePath

      local parm1="ParameterKey=ConfigBucket,ParameterValue=$ConfigBucket"
      local parm2="ParameterKey=DockerImageTag,ParameterValue=$DockerImageTag"
      local parm3="ParameterKey=DockerRepositoryURI,ParameterValue=$DockerRepositoryURI"
      local parm4="ParameterKey=EC2InstanceType,ParameterValue=$EC2InstanceType"
      local parm5="ParameterKey=Landscape,ParameterValue=$Landscape"
      local parms="$parm1 $parm2 $parm3 $parm4 $parm5"

      aws \
        cloudformation $action \
        --stack-name $stackName \
        $([ $action != 'create-stack' ] && echo '--no-use-previous-template') \
        --template-url $templatePath \
        --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
        --parameters $parms

      [ $? -gt 0 ] && echo "Cancelling..." && return 1

      # watchStack $stackName
    fi

    return 0
  fi
  echo "INVALID/MISSING stack action parameter required."
}

ecsMetaRefresh() {
  local instanceId=$(aws cloudformation describe-stack-resources \
    --stack-name $stackName \
    --logical-resource-id MyEC2Instance \
    | jq '.StackResources[0].PhysicalResourceId' \
    | sed 's/"//g')

  echo "instanceId = $instanceId"

  printf "Enter the name of the configset to run: "
  read configset
  # NOTE: The following does not seem to work properly:
  #       --parameters commands="/opt/aws/bin/cfn-init -v --configsets $configset --region "us-east-1" --stack "ECS-EC2-test" --resource MyEC2Instance"
  # Could be a windows thing, or could be a complexity of using bash to execute python over through SSM.
  aws ssm send-command \
    --instance-ids "${instanceId}" \
    --document-name "AWS-RunShellScript" \
    --comment "Implementing cloud formation metadata changes on ec2 instance MyEC2Instance ($instanceId)" \
    --parameters \
    '{"commands":["/opt/aws/bin/cfn-init -v --configsets '"$configset"' --region \"us-east-1\" --stack \"$stackName\" --resource MyEC2Instance"]}'
}

parseValue() {
  local cmd=""

  # Blank out prior values:
  [ "$#" == '3' ] && eval "$3="
  [ "$#" == '2' ] && eval "$2="

  if [ -n "$2" ] && [ "${2:0:1}" == '-' ] ; then
    # Named arg found with no value (it is followed by another named arg)
    echo "echo 'ERROR! $1 has no value!' && exit 1"
    exit 1
  elif [ -n "$2" ] && [ "$#" == "3" ] ; then
    # Named arg found with a value
    cmd="$3=\"$2\" && shift 2"
  elif [ -n "$2" ] ; then
    # Named arg found with no value
    echo "echo 'ERROR! $1 has no value!' && exit 1"
    exit 1
  fi

  echo "$cmd"
}

parseargs() {
  local posargs=""

  while (( "$#" )); do
    case "$1" in
      --task)
        eval "$(parseValue $1 "$2" 'task')" 
        task="${task,,}";;
      -t|--template-path)
        eval "$(parseValue $1 "$2" 'templatePath')" ;;
      --template-type)
        eval "$(parseValue $1 "$2" 'templateType')" ;;
      -b|--bucket-path)
        eval "$(parseValue $1 "$2" 'bucketPath')" ;;
      -s|--stack-name)
        eval "$(parseValue $1 "$2" 'stackName')" ;;
      -c|--config-bucket)
        eval "$(parseValue $1 "$2" 'ConfigBucket')" ;;
      -i|--docker-image-tag)
        eval "$(parseValue $1 "$2" 'DockerImageTag')" ;;
      -u|--docker-repository-uri)
        eval "$(parseValue $1 "$2" 'DockerRepositoryURI')" ;;
      -e|--ec2-instance-type)
        eval "$(parseValue $1 "$2" 'EC2InstanceType')" ;;
      -l|--landscape)
        eval "$(parseValue $1 "$2" 'Landscape')" ;;
      -*|--*=) # unsupported flags
        echo "Error: Unsupported flag $1" >&2
        printusage
        exit 1
        ;;
      *) # preserve positional arguments (should not be any more than the leading command, but collect then anyway) 
        posargs="$posargs $1"
        shift
        ;;
    esac
  done

  # set positional arguments in their proper place
  eval set -- "$posargs"

  [ -z "$bucketPath" ] && bucketPath=${defaults[bucketPath]}
  # Trim off any trailing forward slashes
  templatePath=$(echo "$templatePath" | sed 's/\/*$//')
  bucketPath=$(echo "$bucketPath" | sed 's/\/*$//')

  case $task in
    validate|upload)
      [ -z "$templatePath" ] && templatePath=${defaults[templatePath]}
      if [ ! -f $templatePath ] && [ ! -d $templatePath ] ; then
        echo "INVALID PARAMETER: $templatePath does not exist"
        exit 1
      fi
      if [ -f $templatePath ] ; then
        templateDir=$(dirname $templatePath)
        templateName=$(echo $templatePath | grep -oP '[^/]+$')
      else
        templateDir=$templatePath
      fi
      if [ $task == 'validate' ] ; then
        templateType=${templateType,,}
        [ -z "$templateType" ] && templateType=${defaults[templateType]}
        if [ "$templateType" != "json" ] && [ "$templateType" != "yaml" ] ; then
          echo "INVALID PARAMETER: template-type allowed values: json or yaml"  
          exit 1
        fi
      fi
      ;;
    create|update|delete)
      [ -z "$stackName" ] && echo "MISSING PARAMETER: stack-name" && exit 1
      [ -z "$templatePath" ] && echo "MISSING PARAMETER: stack-name" && exit 1
      [ -d "$templatePath" ] && echo "INVALID PARAMETER: template-path - \"$templatePath\" is not a file." && exit 1
      [ -z "$ConfigBucket" ] && ConfigBucket=${defaults[ConfigBucket]}
      [ -z "$DockerImageTag" ] && DockerImageTag=${defaults[DockerImageTag]}
      [ -z "$DockerRepositoryURI" ] && DockerRepositoryURI=${defaults[DockerRepositoryURI]}
      [ -z "$EC2InstanceType" ] && EC2InstanceType=${defaults[EC2InstanceType]}
      [ -z "$Landscape" ] && Landscape=${defaults[Landscape]}
      ;;
    refresh)
      [ -z "$stackName" ] && echo "MISSING PARAMETER: stack-name" && exit 1

      ;;
  esac

  echo "task=$task"
  echo "templatePath=$templatePath"
  echo "templateDir=$templateDir"
  echo "templateName=$templateName"
  echo "templateType=$templateType"
  echo "bucketPath=$bucketPath"
  echo "stackName=$stackName"
  echo "ConfigBucket"=$ConfigBucket
  echo "DockerImageTag"=$DockerImageTag
  echo "DockerRepositoryURI"=$DockerRepositoryURI
  echo "EC2InstanceType"=$EC2InstanceType
  echo "Landscape"=$Landscape
}

examples() {
  cat <<EOF
    sh $templatePath/scripts/ecs.sh \
      --task validate \
      --template-path $templatePath/test/ec2-test-2.yaml \

    sh $templatePath/scripts/ecs.sh \
      --task upload

    sh $templatePath/scripts/ecs.sh \
      --task create \
      --stack-name kuali-ec2-for-ecs-test \
      --template-path $templatePath/test/ec2-test-2.yaml \
      --bucket-path s3://kuali-research-ec2-setup/ecs/cloudformation/test \
      --config-bucket kuali-research-ec2-setup \
      --docker-image-tag 2001.0040 \
      --docker-repository-uri 730096353738.dkr.ecr.us-east-1.amazonaws.com/core \
      --ec2-instance-type t2.small \
      --landscape sb

    sh $templatePath/scripts/ecs.sh \
      --task update \
      --template-path $templatePath/test/ec2-test-2.yaml \
      --stack-name kuali-ec2-for-ecs-test

EOF
}


parseargs $@
  
case "$task" in
  validate)
    echo "Validate the specified template(s)"
    ecsValidate
    ;;
  upload)
    echo "Upload the specified template(s) to s3 bucket"
    ecsUpload
    ;;
  create)
    echo "Creating stack: $stackName..."
    ecsStackAction "create-stack"
    ;;
  update)
    echo "Performing an update to stack: $stackName..."
    ecsStackAction "update-stack"
    ;;
  delete)
    echo "Deleting stack: $stackName..."
    ecsStackAction "delete-stack"
    ;;
  refresh)
    echo "Perform a metadata refresh"
    ecsMetaRefresh
    ;;
  *)
    if [ -n "$task" ] ; then
      echo "INVALID PARAMETER: No such task: $task"
    else
      echo "MISSING PARAMETER: task"
    fi
    exit 1
    ;;
esac
