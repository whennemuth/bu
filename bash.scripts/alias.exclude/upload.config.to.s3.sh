OUTPUT_FILE=s3-upload.config.to.s3.cmd.sh
declare -A kualiTags=(
  [Service]='research-administration'
  [Function]='kuali'
)
LANDSCAPES=(
  'sb' 'ci' 'qa' 'stg' 'prod'
)
declare -A LANDSCAPE_ALIASES=(
  [sb]='sandbox'
  [ci]='ci'
  [qa]='qa'
  [stg]='stg stage staging'
  [prod]='prod production'
)
declare -A LOCAL_CONFIGS=(
  [legacy]='/c/whennemuth/scrap/s3/LANDSCAPE/kc/kc-config.xml'
  [infnprd]='/c/whennemuth/workspaces/ecs_workspace/cloud-formation/kuali-infrastructure/s3/LANDSCAPE/kc/kc-config.xml'
  [infprd]='/c/whennemuth/workspaces/ecs_workspace/cloud-formation/kuali-infrastructure/s3/LANDSCAPE/kc/kc-config.xml'
)
declare -A S3_CONFIGS=(
  [legacy]='s3://kuali-research-ec2-setup/LANDSCAPE/kuali/main/config/kc-config.xml' 
  [infnprd]='s3://kuali-conf/LANDSCAPE/kc/kc-config.xml'
  [infprd]='s3://kuali-conf/LANDSCAPE/kc/kc-config.xml'
)
declare -A EC2_CONFIGS=(
  [legacy]='/opt/kuali/main/config/kc-config.xml'
  [infnprd]='/opt/kuali/s3/kc/kc-config.xml'
  [infprd]='/opt/kuali/s3/kc/kc-config.xml'
)
declare -A RESTART_COMMANDS=(
  [legacy]='docker restart kuali-research'
  [infnprd]='aws ecs update-service --service kuali-research --force-new-deployment'
  [infprd]='aws ecs update-service --service kuali-research --force-new-deployment'
)

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

# Get the standard name for the landscape from known aliases
formatLandscape() {
  local landscape="$1"
  if [ -n "$landscape" ] ; then
    for landscape_name in ${LANDSCAPES[@]} ; do
      for landscape_alias in ${LANDSCAPE_ALIASES[$landscape_name]} ; do
        if [ ${landscape,,} == $landscape_alias ] ; then
          echo $landscape_name
        fi
      done
    done
  fi
}

setProfile() {
  export AWS_PROFILE=$profile
  if [ ! -f $OUTPUT_FILE ] ; then
    echo "export AWS_PROFILE=$profile" > $OUTPUT_FILE
    echo " " >> $OUTPUT_FILE
  fi
}

# Upload the kc-config.xml file to the target ec2 instance
push() {
  eval "$(getEvalArgs lowercase $@)"

  landscape="$(formatLandscape $landscape)"
  [ -z "$landscape" ] && echo "Unknown or missing landscape" && exit 1

  local sourceconfig=$(echo ${LOCAL_CONFIGS[${profile,,}]} | sed 's|LANDSCAPE|'$landscape'|')
  local targetconfig=$(echo ${S3_CONFIGS[${profile,,}]} | sed 's|LANDSCAPE|'$landscape'|')
  ([ -z "$sourceconfig" ] || [ -z "$targetconfig" ]) && echo "Unknown profile: $profile" && exit 1

  setProfile $profile
  echo "echo 'Pushing kc-config.xml to s3...'" >> $OUTPUT_FILE
  echo "aws s3 cp $sourceconfig $targetconfig" >> $OUTPUT_FILE
  echo " " >> $OUTPUT_FILE

  if [ "$pull" == 'true' ] ; then
    pull $@
  fi
}

# Issue a command to the target ec2 instance to download the newly uploaded kc-config.xml from s3
pull() {
  eval "$(getEvalArgs lowercase $@)"
  setProfile $profile
  echo "echo 'Pulling kc-config.xml from s3 to target ec2 instance...'" >> $OUTPUT_FILE

  local sourceconfig=$(echo ${S3_CONFIGS[${profile,,}]} | sed 's|LANDSCAPE|'$landscape'|')
  local targetconfig=$(echo ${EC2_CONFIGS[${profile,,}]} | sed 's|LANDSCAPE|'$landscape'|')

counter=0
  while read instanceId ; do
    [ -z "$instanceId" ] && continue 
  	cat <<-EOF >> $OUTPUT_FILE
aws ssm send-command \\
  --instance-ids "${instanceId}" \\
  --document-name "AWS-RunShellScript" \\
  --comment "Refreshing kc-config.xml on $instanceId" \\
  --parameters \\
  '{"commands":["aws s3 cp $sourceconfig $targetconfig"]}'
EOF
  done <<< $(
    aws resourcegroupstaggingapi get-resources \
      --resource-type-filters ec2:instance \
      --tag-filters \
        'Key=Service,Values='${kualiTags["Service"]} \
        'Key=Function,Values='${kualiTags["Function"]} \
        "Key=Landscape,Values=$landscape" \
      --output text \
      --query 'ResourceTagMappingList[].{ARN:ResourceARN}' | cut -d'/' -f2 | sed 's/\n//g' 2> /dev/null
  )

  if [ "$restart" == 'true' ] ; then
    restart $@
  fi
}

restart() {
  eval "$(getEvalArgs lowercase $@)"
  setProfile $profile
  echo "echo 'Restarting kuali research docker container(s)'..."

  if [ "${RESTART_COMMANDS[$profile]:4:3}" == 'ecs' ] ; then
    direct_restart $@
  else
    remote_restart $@
  fi
}

# Restart the docker container(s) on the target ec2 instance by sending it the appropriate command for it to execute
remote_restart() {
  eval "$(getEvalArgs lowercase $@)"

  while read instanceId ; do
  	cat <<-EOF >> $OUTPUT_FILE
aws ssm send-command \\
  --instance-ids "${instanceId}" \\
  --document-name "AWS-RunShellScript" \\
  --comment "Refreshing kc-config.xml on $instanceId" \\
  --parameters \\
  '{"commands":["${RESTART_COMMANDS[$profile]}"]}'
EOF

  done <<< $(
    aws resourcegroupstaggingapi get-resources \
      --resource-type-filters ec2:instance \
      --tag-filters \
        'Key=Service,Values='${kualiTags["Service"]} \
        'Key=Function,Values='${kualiTags["Function"]} \
        "Key=Landscape,Values=$landscape" \
      --output text \
      --query 'ResourceTagMappingList[].{ARN:ResourceARN}' | cut -d'/' -f2 | sed 's/\n//g' 2> /dev/null
  )
}

# Restart the docker container(s) by executing the appropriate command directly
direct_restart() {
  eval "$(getEvalArgs lowercase $@)"
  setProfile $profile
  eval "${RESTART_COMMANDS[$profile]}"
}

run() {
  eval "$(getEvalArgs lowercase $@)"
  if [ "$dryrun" == 'true' ] ; then
    cat $OUTPUT_FILE
  else
    printf "\nExecute the following command:\n\n$(cat $OUTPUT_FILE)\n\n(y/n): "
    read answer
    [ "$answer" == "y" ] && sh $OUTPUT_FILE || echo "Cancelled."
  fi
}

task="${1,,}"

case "${task,,}" in
  push)
    shift
    rm -f $OUTPUT_FILE
    push $@
    ;;
  pull)
    shift
    rm -f $OUTPUT_FILE
    pull $@
    ;;
  restart)
    shift
    rm -f $OUTPUT_FILE
    restart $@
    ;;
  all)
    shift
    rm -f $OUTPUT_FILE
    push $@ pull=true restart=true
    ;;
  *)
    echo "No recognized task!"
    exit 1
    ;;
esac

run $@