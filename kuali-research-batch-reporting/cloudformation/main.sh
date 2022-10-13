#!/bin/bash

declare TEMPLATE_BUCKET=${TEMPLATE_BUCKET:-"kuali-conf"}
declare -A defaults=(
  [STACK_NAME]='research-admin-reports'
  [GLOBAL_TAG]='research-admin-reports'
  [TEMPLATE_BUCKET_PATH]='s3://'$TEMPLATE_BUCKET'/cloudformation/research-admin-reports'
  [TEMPLATE_PATH]='.'
  [NO_ROLLBACK]='true'
  # [LANDSCAPE]='prod'
)
declare PRIVATE_KEY="cloudfront-key.pem"
declare PUBLIC_KEY="cloudfront-key-pub.pem"

run() {
  source ./common-functions.sh

  if ! isCurrentDir "cloudformation" ; then
    echo "Current directory: $(pwd)"
    echo "You must run this script from the cloudformation subdirectory!."
    exit 1
  fi

  task="${1,,}"
  shift

  outputHeading "Validating/Parsing parameters..."
  # if [ "$task" != "test" ] && [ "$task" != 'validate' ]; then
  if [ "$task" != 'validate' ]; then

    parseArgs $@

    setDefaults

    validateParms
  fi

  runTask
}

existsInParameterStore() {
  export MSYS_NO_PATHCONV=1
  local nameIn="$1"
  local nameOut=$(aws ssm get-parameter --name $nameIn 2> /dev/null | jq -r '.Parameter.Name' 2> /dev/null)
  [ "$nameIn" == "$nameOut" ] && true || false
}

publicKeyExistsInParameterStore() {
  keyExistsInParameterStore 'public' && true || false
}

privateKeyExistsInParameterStore() {
  keyExistsInParameterStore 'private' && true || false
}

# Search for one of an RSA keypair in the aws ssm parameter store by name
keyExistsInParameterStore() {
  local keytype="${1,,}"
  local name='/'$GLOBAL_TAG'/'$LANDSCAPE'/cloudfront/rsa-'$keytype'-key'
  existsInParameterStore "$name" && true || false
}

# Delete one of an RSA keypair from the aws ssm parameter store by name
deleteKeyParameter() {
  local keytype="${1,,}"
  local name='/'$GLOBAL_TAG'/'$LANDSCAPE'/cloudfront/rsa-'$keytype'-key'
  echo "Deleting ssm parameter: ${name}..."
  aws ssm delete-parameter $name
  if [ $? -eq 0 ] ; then
    echo "$name deleted."
    true
  else
    echo "$name deletion failed!"
    false
  fi
}

# Format the public key properly for the AWS::CloudFront::PublicKey PublicKeyConfig.EncodedKey parameter.
# Thich means replacing all actual newlines with "\n"
flattenKey() {
  cat | tr -t '\n' '#' | sed 's/#/\\n/g'
}

# Find the public key from the file system, falling back to the aws ssm parameter store if not found.
getFlattenedKey() {
  if [ -f $PUBLIC_KEY ] ; then
    cat $PUBLIC_KEY | flattenKey
  else
    local name='/'$GLOBAL_TAG'/'$LANDSCAPE'/cloudfront/rsa-public-key'
    aws ssm get-parameter --with-decryption --name $name 2> /dev/null | \
      jq -r '.Parameter.Value' 2> /dev/null | flattenKey
  fi
}

# Create a public and private key and load both up to the aws ssm parameter store.
putKeypairToParameterStore() {
  putKey() {
    local keytype="$1"
    local keyname="$2"
    local name='/'$GLOBAL_TAG'/'$LANDSCAPE'/cloudfront/rsa-'$keytype'-key'
    local description="RSA $keytype key used to generate pre-signed CloudFront URLs to retrieve reports from S3. Public Key is defined on CloudFront distribution."
    if keyExistsInParameterStore $keytype ; then
      # Overwrite an existing key in the aws ssm parameter keystore.
      cat <<EOF > $cmdfile

      aws ssm put-parameter \\
        --name $name \\
        --description "$description" \\
        --type 'SecureString' \\
        --overwrite \\
        --value "\$(cat $keyname)"
EOF
    else
      # Put a key into the aws ssm parameter store that is not already there.
      cat <<EOF > $cmdfile

      aws ssm put-parameter \\
        --name $name \\
        --description "$description" \\
        --type 'SecureString' \\
        --value "\$(cat $keyname)" \\
        --tags \\
            Key=Name,Value=$name \\
            Key=Function,Value=${kualiTags['Service']} \\
            Key=Service,Value=${kualiTags['Function']}

EOF
    fi
    cat $cmdfile
    if ! isDryrun ; then
      source ./$cmdfile
    fi
  }

  # Remove all generated key files.
  cleanKeys() {
    rm -fv $PRIVATE_KEY 2> /dev/null
    rm -fv $PUBLIC_KEY 2> /dev/null
  }

  # Generate the rsa public/private keypair
  makeKeys() {
    echo "Adding new RSA keypair to parameter store"
    # ssh-keygen -m "PEM" -b 2048 -t rsa -f 'cloudfront-key' -q -N ""
    # ssh-keygen -e -f cloudfront-key.pub | grep -iP '^(?!Comment:).*' > $PUBLIC_KEY
    # sed -i 's/SSH2 PUBLIC KEY/PUBLIC KEY/g' $PUBLIC_KEY

    openssl genrsa -out $PRIVATE_KEY
    openssl rsa -pubout -in $PRIVATE_KEY -out $PUBLIC_KEY
  }

  putKeys() {
    putKey 'private' $PRIVATE_KEY
    putKey 'public' $PUBLIC_KEY
  }

  cleanKeys

  makeKeys

  putKeys

  # cleanKeys
}

validateParms() {
  if [ -z "$LANDSCAPE" ] ; then
    if isDnsRdsAddress $DB_HOST ; then
      LANDSCAPE="$(echo $DB_HOST | cut -d'.' -f1)"
      echo "LANDSCAPE = $LANDSCAPE"
    else
      printf "\n\nYou must provide a landscape.\n"
      printf "This landscape will reflect the database that jobs in this stack run against.\n"
      printf "Input value (or [enter] to quit): "
      read LANDSCAPE
    fi
    [ -z "$LANDSCAPE" ] && printf "LANDSCAPE not set \nCancelling...\n" && exit 0
  fi
}

# Ensure that an RSA keypair can be found in the aws ssm parameter store, prompting the user
# to accept keypair creation and upload if anything is found to be missing.
checkKeypair() {

  promptForKeypairRenewal() {
    local keytype="${1,,}"
    local keytype2="${2,,}"
    if [ -n "$keytype2" ] ; then
      local msg="Add a new keypair to the ssm parameter store?"
    else
      local msg="Remove the $keytype key from the ssm parameter store and add a new keypair?"
    fi
    if askYesNo "$msg" ; then
      putKeypairToParameterStore
    else
      echo "Handle this manually. Exiting..."
      exit 1
    fi
  }

  if privateKeyExistsInParameterStore ; then
    if publicKeyExistsInParameterStore ; then
      echo "RSA keypair found in parameter store."
    else
      echo "ERROR! Private key exists in parameter store without a public key."
      promptForKeypairRenewal 'private'
    fi
  elif publicKeyExistsInParameterStore ; then
    echo "ERROR! Public key exists in parameter store without a private key."
    promptForKeypairRenewal 'public'
  else
    echo "No RSA keypair can be found in the parameter store."
    promptForKeypairRenewal 'private' 'public'
  fi
}

checkDatabasePassword() {
  local name='/'$GLOBAL_TAG'/'$LANDSCAPE'/oracle/pswd'

  if existsInParameterStore "$name" ; then
    echo "\"$name\" found in keystore."
  elif [ -n "$DATABASE_PASSWORD" ] ; then
    echo "\"$name\" not found in keystore."
  else
    echo "\"$name\" not found in keystore and no password value has been provided as a parameter!"
    printf 'Enter the password (enter key to cancel): '
    read DATABASE_PASSWORD
    if [ -z "$DATABASE_PASSWORD" ] ; then
      echo "Skipping database password upload. No batch job will work until this is done."
      echo "Resuming stack creation/update"
      return 0
    fi
      local description="Identifies Password of Oracle User running SQLcl to generate Research Admin batch reports."
      cat <<EOF > $cmdfile

      aws ssm put-parameter \\
        --name $name \\
        --description "$description" \\
        --type 'SecureString' \\
        --value "\${DATABASE_PASSWORD}" \\
        --tags \\
            Key=Name,Value=$name \\
            Key=Function,Value=${kualiTags['Service']} \\
            Key=Service,Value=${kualiTags['Function']}

EOF
    cat $cmdfile
    if ! isDryrun ; then
      source ./$cmdfile
    fi
  fi
}

# DB_HOST provides one of either of two rds starting points:
#   1) Database hostname (ie: kuali-oracle-stg.clb9d4mkglfd.us-east-1.rds.amazonaws.com) 
#   or...
#   2) Common name (ie: feature.db.kualitest.research.bu.edu)
# The VPC security group associated with the rds instance can be determined through several cli calls starting from either of these two values.
setRdsSecurityGroupArn() {

  promptForArn() {
    printf "\n\nCould not determine the rds database VPC security group ARN.\n"
    printf "Please provide it here (example: \"sg-08ab79baa11bad5e0\")\n"
    printf "Input value (or [enter] to quit): "
    read RDS_SG_ARN
  }

  isPrivateRdsAddress() {
    endsWith "$DB_HOST" 'rds.amazonaws.com' && true || false
  }

  getHostedZone() {
    local dbhost="${1:-$(</dev/stdin)}"
    echo $dbhost | cut -d'.' -f3-
  }

  getHostedZoneId() {
    local commonName="${1:-$(</dev/stdin)}"
    [ -z "$commonName" ] && exit 1
    aws route53 list-hosted-zones \
      --output text \
      --query 'HostedZones[?starts_with(Name, `'$commonName'`) == `true`].{id:Id}'
  }

  getPrivateRdsAddress() {
    local hostedZoneId="${1:-$(</dev/stdin)}"
    [ -z "$hostedZoneId" ] && exit 1
    aws route53 list-resource-record-sets \
      --hosted-zone-id $hostedZoneId \
      --output text \
      --query 'ResourceRecordSets[?starts_with(Name, `'$DB_HOST'`) == `true`].{dns:ResourceRecords[0].Value}' 
  }

  getRdsArnFromDbAddress() {
    local address="${1:-$(</dev/stdin)}"
    [ -z "$address" ] && exit 1
    aws rds describe-db-instances \
      --output text \
      --query 'DBInstances[?Endpoint.Address==`'$address'`].{arn:DBInstanceArn}' 
  }

  getRdsVpcSecurityGroupId() {
    local instanceArn="${1:-$(</dev/stdin)}"
    [ -z "$instanceArn" ] && exit 1
    aws rds describe-db-instances \
      --db-instance-id $instanceArn \
      --output text \
      --query 'DBInstances[].VpcSecurityGroups[].{id:VpcSecurityGroupId}'
  }

  getRdsSgArn() {
    local dbhost="$1"
    
    if isPrivateRdsAddress $dbhost ; then

      echo $dbhost | \
      getRdsArnFromDbAddress | \
      getRdsVpcSecurityGroupId 

    elif isDnsRdsAddress $dbhost; then
      
      echo $dbhost | \
      getHostedZone | \
      getHostedZoneId | \
      getPrivateRdsAddress | \
      getRdsArnFromDbAddress | \
      getRdsVpcSecurityGroupId

      [ $? -gt 0 ] && echo "ERROR!" && exit 1
    fi
  }

  if [ -n "$DB_HOST" ] ; then
    RDS_SG_ARN="$(getRdsSgArn $DB_HOST)"
    if [ "${RDS_SG_ARN:0:5}" == 'ERROR' ] ; then
      promptForArn
    fi
    [ -z "$RDS_SG_ARN" ] && printf "RDS_SG_ARN not set \nCancelling...\n" && exit 0
    echo "RDS_SG_ARN: $RDS_SG_ARN"
  else
    echo "ERROR! Missing DB_HOST parameter."
    exit 1
  fi
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

getBucketName() {
  echo ${REPORT_BUCKET_NAME:-"bu-${GLOBAL_TAG}-${LANDSCAPE}-archive"}
}

createBucket() {
  bucketExistsInThisAccount $(getBucketName) && echo 'false' || echo 'true'
}

# Create, update, or delete the cloudformation stack.
stackAction() {
  local action=$1   

  [ -z "$FULL_STACK_NAME" ] && FULL_STACK_NAME=${STACK_NAME}
  if [ "$action" == 'delete-stack' ] ; then
    if isDryrun ; then
      echo "DRYRUN: aws cloudformation $action --stack-name $(getStackToDelete)"
    else
      aws cloudformation $action --stack-name $(getStackToDelete)
      if ! waitForStackToDelete ; then
        echo "Problem deleting stack!"
        exit 1
      fi
    fi
  else
    if [ "$action" == 'create-stack' ] ; then
      checkKeypair

      checkDatabasePassword
    fi

    # checkSubnets will also assign a value to VPC_ID
    outputHeading "Looking up VPC/Subnet information..."
    if ! checkSubnets ; then
      exit 1
    fi

    if [ "${SKIP_S3_UPLOAD,,}" == 'true' ] ; then
      echo "Skipping upload of templates and scripts to s3."
    else
      outputHeading "Validating and uploading templates..."

      printf "" > $cmdfile
      find . \
        -type f \
        -name "*.yaml" \
        -not -path './.metadata/*' \
        | \
        while read template; do 
          validateTemplateAndUploadToS3 \
            silent=true \
            filepath=$template \
            s3path=s3://$TEMPLATE_BUCKET_NAME/cloudformation/${GLOBAL_TAG}/
        done

    fi

    cat <<-EOF > $cmdfile
    aws \\
      cloudformation $action \\
      --stack-name ${FULL_STACK_NAME} \\
      $([ $task != 'create-stack' ] && echo '--no-use-previous-template') \\
      $([ "$NO_ROLLBACK" == 'true' ] && [ $task == 'create-stack' ] && echo '--on-failure DO_NOTHING') \\
      --template-url $BUCKET_URL/main.yaml \\
      --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \\
      --parameters '[
EOF

    add_parameter $cmdfile 'Landscape' 'LANDSCAPE'
    add_parameter $cmdfile 'VpcId' 'VpcId'
    add_parameter $cmdfile 'CampusSubnet1' 'CAMPUS_SUBNET1'
    add_parameter $cmdfile 'CampusSubnet2' 'CAMPUS_SUBNET2'
    add_parameter $cmdfile 'DockerImage' 'DOCKER_IMAGE'
    add_parameter $cmdfile 'JobQueueState' 'JOB_QUEUE_STATE'
    add_parameter $cmdfile 'EventRuleState' 'EVENT_RULE_STATE'
    add_parameter $cmdfile 'LogGroupRetentionDays' 'LOGGROUP_RETENTION_DAYS'
    add_parameter $cmdfile 'SnsSubscriptionEmail' 'SNS_SUBSCRIPTION_EMAIL'
    add_parameter $cmdfile 'EmailReplyToParameter' 'EMAIL_REPLY_TO_PARAMETER'
    add_parameter $cmdfile 'DbHost' 'DB_HOST'
    add_parameter $cmdfile 'DbUser' 'DB_USER'
    addParameter  $cmdfile 'ReportBucketName' "$(getBucketName)"
    addParameter  $cmdfile 'CreateReportBucket' "$(createBucket)"
    setRdsSecurityGroupArn # Will exit if failure to set RDS_SG_ARN
    addParameter $cmdfile  'RdsVpcSecurityGroupId' $RDS_SG_ARN
    if ! addParameter $cmdfile 'PublicKey' "$(getFlattenedKey)" ; then
      if ! isDryrun ; then
        echo "ERROR! Failed to add parameter 'PublicKey'"
        exit 1
      fi
    fi

    echo "      ]' \\" >> $cmdfile
    echo "      --tags '[" >> $cmdfile
    addStandardTags
    addTag $cmdfile 'Category' 'report'
    addTag $cmdfile 'Subcategory' 'batch'
    echo "      ]'" >> $cmdfile

    runStackActionCommand
  fi 
}


runTask() {
  case "$task" in
    validate)
      validateStack ;;
    upload)
      uploadStack ;;
    create-stack)
      stackAction "create-stack" ;;
    recreate-stack)
      PROMPT='false'
      task='delete-stack'
      stackAction "delete-stack" 2> /dev/null
      task='create-stack'
      stackAction "create-stack"
      ;;
    update-stack)
      stackAction "update-stack" ;;
    reupdate-stack)
      PROMPT='false'
      task='update-stack'
      stackAction "update-stack" ;;
    delete-stack)
      stackAction "delete-stack" ;;
    check-keys)
      checkKeypair ;;
    check-db-password)
      checkDatabasePassword ;;
    test)
      setRdsSecurityGroupArn 'feature.db.kualitest.research.bu.edu' ;;
    *)
      if [ -n "$task" ] ; then
        echo "INVALID PARAMETER: No such task: $task"
      else
        echo "MISSING PARAMETER: task"
      fi
      exit 1
      ;;
  esac
}

run $@