#!/bin/bash

# ---------------------------------------------------------------------------------
# This is a helper script for building and uploading (to a ECR) the docker image,
# and running a container from that image locally for mock testing.
# ---------------------------------------------------------------------------------

if [ -f ./cloudformation/common-functions.sh ] ; then
  source ./cloudformation/common-functions.sh
fi

parseArgs $@

imageShortName='research-admin-reports'
containerName='research-admin-reports'

AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-${REGION:-${AWS_REGION:-"us-east-1"}}}
GLOBAL_TAG=${GLOBAL_TAG:-"research-admin-reports"}
LANDSCAPE=${LANDSCAPE:-"prod"}
PREFIX="${GLOBAL_TAG}"
[ -n "$LANDSCAPE" ] && PREFIX="${GLOBAL_TAG}/${LANDSCAPE}"
# All available job parms can be found in ./cloudformation/event-rules.yaml - look for the "Targets[].Input:" attributes.
JOB_PARM=${JOB_PARM:-"pre_award_not_billable"}

build() {
  stop 

  if activeCredentials ; then
    runCommand "docker build -t $(getImageName) ."
    if isDryrun ; then
      echo "(dryrun) Removing dangling images..."
    else
      for i in $(docker images -a -q --filter dangling=true) ; do
        docker rmi $i
      done
    fi
  fi
}

# Construct that portion of the docker run command that accounts for the environment variables.
# NOTE: 
#   In the conventional scenario, PARMSTORE_* variables are considered to indicate the name
#   of a parameter in the aws ssm parameter store, and the app will obtain values by performing
#   cli lookups against that service. However, if the LOCAL_PARMS environment variable is set to true, 
#   the app will consider that any PARMSTORE_* variable contains the value itself, not the parameter 
#   name in the store, and no cli lookup is necessary.
getEnvironmentVariables() {

  getParm() {
    local parmname="$1"
    local parmstoreKey="$2"
    local localval=${!parmname}
    if [ -n "$localval" ] ; then
      echo "PARMSTORE_$parmname=\"$localval\""
    else
      echo "PARMSTORE_$parmname=\"$parmstoreKey\""
    fi
  }

  cat <<EOF
        -e $(getParm 'DB_USER' "/${PREFIX}/oracle/user") \\
        -e $(getParm 'DB_PSWD' "/${PREFIX}/oracle/pswd") \\
        -e $(getParm 'DB_HOST' "/${PREFIX}/oracle/host") \\
        -e $(getParm 'EMAIL_FROM' "/${PREFIX}/email/from") \\
        -e $(getParm 'EMAIL_REPLYTO' "/${PREFIX}/email/reply-to") \\
        -e $(getParm 'EMAIL_GREETING' "/${PREFIX}/email/greeting") \\
        -e $(getParm 'EMAIL_SIGNATURE' "/${PREFIX}/email/signature") \\
        -e $(getParm 'CLOUDFRONT_PK' "/${PREFIX}/cloudfront/rsa-private-key") \\
        -e $(getParm 'CLOUDFRONT_TTL_DAYS' "/${PREFIX}/cloudfront/ttl-days") \\
        -e $(getParm 'DYNAMODB_TABLE' "${PREFIX}-distribution") \\
        -e LOCAL_PARMS=${LOCAL_PARMS:-"false"} \\
        -e PUBLISH_REPORTS=${PUBLISH_REPORTS:-"false"} \\
        -e REPORT_BUCKET_NAME="${REPORT_BUCKET_NAME}" \\
        -e CLOUDFRONT_PUBLIC_KEY_ID="${CLOUDFRONT_PUBLIC_KEY_ID}" \\
        -e CLOUDFRONT_URL="${CLOUDFRONT_URL}" \\
        -e AWS_PROFILE=$(getAwsProfile) \\
        -e AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-${DEFAULT_REGION:-"${REGION}"}} \\
        -e AWS_DEFAULT_OUTPUT="text" \\
        -e LANG="en_US.utf8"
EOF
}


# PARMSTORE_DB_PSWD	/research-admin-reports-stg/oracle/stg/pswd
# PARMSTORE_DB_USER	/research-admin-reports-stg/oracle/stg/user
# AWS_DEFAULT_OUTPUT	text
# PARMSTORE_EMAIL_REPLYTO	/research-admin-reports-stg/email/reply-to
# AWS_DEFAULT_REGION	us-east-1
# CLOUDFRONT_PUBLIC_KEY_ID	K1YQNIFCDCAB8G
# PARMSTORE_EMAIL_FROM	/research-admin-reports-stg/email/from
# PARMSTORE_CLOUDFRONT_PK	/research-admin-reports-stg/cloudfront/rsa-private-key
# PARMSTORE_EMAIL_SIGNATURE	/research-admin-reports-stg/email/signature
# REPORT_BUCKET_NAME	bu-research-admin-reports-stg-archive
# PARMSTORE_EMAIL_GREETING	/research-admin-reports-stg/email/greeting
# PARMSTORE_DYNAMODB_TABLE	research-admin-reports-stg-distribution
# CLOUDFRONT_URL	https://d2ljcftrunl65g.cloudfront.net/
# PARMSTORE_CLOUDFRONT_TTL_DAYS	/research-admin-reports-stg/cloudfront/ttl-days
# PARMSTORE_DB_HOST	/research-admin-reports-stg/oracle/stg/host

run() {
  stop

  if activeCredentials ; then
    export MSYS_NO_PATHCONV=1
    runCommand "docker run \\
      -d \\
      --rm \\
      --name $containerName \\
      -v ~/.aws/credentials:/root/.aws/credentials \\
      $(getEnvironmentVariables) \\
      $(getImageName) \\
      sh _loader.sh $JOB_PARM"
  fi
}

stop() {
  runCommand "docker stop $containerName 2> /dev/null"
}

push() {
  if activeCredentials ; then
    local registry=$(getEcrRegistryName)
    local repo="$registry"
    local user="AWS"
    local pswd="$(aws ecr get-login-password --region $AWS_DEFAULT_REGION)"

    if isDryrun ; then
      echo "$pswd | docker login -u $user --password-stdin $registry"
      echo "docker push $(getImageName)"
    else
      echo $pswd | docker login -u $user --password-stdin $registry
      docker push $(getImageName)
    fi
  else
    false
  fi
}


task="$1"

shift

case "$task" in
  build)
    build ;;
  run)    
    run $@ ;;
  debug)
    debug $@ ;;
  rerun)
    build && run $@ ;;
  stop)
    stop ;;
  push)
    push ;;
esac