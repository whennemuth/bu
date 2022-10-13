#!/bin/bash

source $(pwd)/snaplogic_api.v1.identity.sh

parseArgs() {
  for nv in $@ ; do
    [ -z "$(grep '=' <<< $nv)" ] && continue;
    name="$(echo $nv | cut -d'=' -f1)"
    value="$(echo $nv | cut -d'=' -f2-)"
    eval "${name^^}=$value" 2> /dev/null || true
  done
  if [ "$task" == 'get-instprop' ] ; then
    [ -z "$INSTPROP_ID" ] && echo "Required: instprop_id" && exit 1
  else
    [ -z "$BUID" ] && echo "Required: buid" && exit 1
  fi
  local profile=${PROFILE:-"infnprd"}
  export AWS_PROFILE=$profile
  echo "AWS_PROFILE=$profile"
  [ -z "$LANDSCAPE" ] && LANDSCAPE='stg' && echo "LANDSCAPE: defaulting to $LANDSCAPE"
  [ -z "$SNAPLOGIC_ENV" ] && SNAPLOGIC_ENV='BUTest' && echo "SNAPLOGIC_ENV: defaulting to $SNAPLOGIC_ENV"
  [ -z "$SNAPLOGIC_BEARER_TOKEN" ] && echo "SNAPLOGIC_BEARER_TOKEN is empty. Will look it up in secrets manager."  
  [ -z "$CORE_SERVICE_USER_NAME" ] && CORE_SERVICE_USER_NAME='bu_service_iam' && echo "CORE_SERVICE_USER_NAME: defaulting to $CORE_SERVICE_USER_NAME"
  [ -z "$CORE_SERVICE_USER_PSWD" ] && echo "CORE_SERVICE_USER_PSWD is empty. Will look it up in secrets manager."
  [ -z "$HOST" ] && HOST="$(getHost)" && echo "HOST: defaulting to $HOST"

  echo ""
  echo "--------------------------------------"
  echo "   ARGS:"
  echo "--------------------------------------"
  echo "DRYRUN=$DRYRUN"
  echo "BUID=$BUID"
  echo "LANDSCAPE=$LANDSCAPE"
  echo "HOST=$HOST"
  echo "AWS_PROFILE=$AWS_PROFILE"
  echo "BEARER_TOKEN=$BEARER_TOKEN"
  echo "SNAPLOGIC_ENV=$SNAPLOGIC_ENV"
  echo "SNAPLOGIC_BEARER_TOKEN=$SNAPLOGIC_BEARER_TOKEN"
  echo "CORE_SERVICE_USER_NAME=$CORE_SERVICE_USER_NAME"
  echo "CORE_SERVICE_USER_PSWD=$CORE_SERVICE_USER_PSWD"
  echo ""
}

validArgs() {
  local msg="Missing args: "
}

SECRETS_JSON=""
checkSecrets() {
  if [ -z "$SECRETS_JSON" ] ; then
    SECRETS_JSON=$(aws secretsmanager get-secret-value \
      --secret-id kuali/cor-main/secrets \
      --output text \
      --query '{SecretString:SecretString}' 2> /dev/null)
  fi
  [ -z "$SECRETS_JSON" ] && echo "Secrets manager lookup failed!" && exit 1
}

getHost() {
  [ -n "$HOST" ] && echo "$HOST" && return 0
  local domain='kualitest.research.bu.edu'
  local subdomain="$LANDSCAPE"
  if [ "$subdomain" == 'prod' ] ; then
    domain='kuali.research.bu.edu'
    subdomain=""
  else
    subdomain="${subdomain}."
  fi
  echo "https://${subdomain}${domain}"
}

getHostDomain() {
  getHost | sed -E 's|^https?://||'
}

getCoreServiceUserPassword() {
  [ -n "$CORE_SERVICE_USER_PSWD" ] && echo "$CORE_SERVICE_USER_PSWD" && return 0
  checkSecrets
  echo "$SECRETS_JSON" | jq -r '.snaplogic.dev.password' 2> /dev/null
}

getSnapLogicBearerToken() {
  [ -n "$SNAPLOGIC_BEARER_TOKEN" ] && echo "$SNAPLOGIC_BEARER_TOKEN" && return 0
  checkSecrets
  echo "$SECRETS_JSON" | jq -r '.snaplogic.dev.bearer_token' 2> /dev/null
}

# Staging: kc-config.xml/auth.system.token
getCoreAuthToken() {
  [ -n "$BEARER_TOKEN" ] && echo "$BEARER_TOKEN" && return 0
  local env="$LANDSCAPE"
  [ "$env" == 'prod' ] && env="" || env="${env}."
  local url="https://$(getHostDomain)/api/v1/auth/authenticate"

  curl --insecure \
    -X POST \
    -H "Authorization: Basic $(echo -n "$CORE_SERVICE_USER_NAME:$(getCoreServiceUserPassword)" | base64 -w 0)" \
    -H "Content-Type: application/json" \
    "$url" \
    | sed 's/token//g' \
    | sed "s/[{}\"':]//g" \
    | sed "s/[[:space:]]//g"
}

makeSnapLogicRequest() {
  local token="$(getCoreAuthToken)"
  local snaplogicUrl=$(cat <<EOF
    https://elastic.snaplogic.com:443/
    api/
    1/
    rest/
    slsched/
    feed/
    $SNAPLOGIC_ENV/
    Admin-Research-Systems/
    createAccountsOnKuali/
    createKualiUser_Triggered_Task
EOF
  )
  snaplogicUrl=$(echo $snaplogicUrl | sed 's/[[:space:]]//g')

echo "-------------------------------------"
echo "$CORE_SERVICE_USER_NAME:$(getCoreServiceUserPassword)"  
echo "-------------------------------------"

  cat <<EOF >> $outfile
  curl --insecure \\
    -X POST \\
    -H "Authorization: Bearer $(getSnapLogicBearerToken)" \\
    -H 'Content-Type: application/json' \\
    -d '{
      "UNIVERSITY_ID": "$BUID",
      "Token": "$token"
    }' \\
    $snaplogicUrl
EOF
  cat $outfile

  [ "$DRYRUN" == 'true' ] && exit 0

  sh $outfile
}


outfile='snaplogic-request.sh'
rm -f $outfile

task="$1"
shift

parseArgs $@

[ "$DEBUG" == 'true' ] && set -x

case "$task" in
  call-snaplogic)
    makeSnapLogicRequest
    ;;
  get-user)
    getUser
    ;;
  create-user)
    createUser
    ;;
  get-instprop)
    getInstProp
    ;;
  get-token)
    getCoreAuthToken
    ;;
  *)
    echo "Invalid task: $task"
esac

# Example: 
# sh snaplogic.sh \
#   call-snaplogic
#   profile=infnprd \
#   landscape=stg \
#   core_service_user_name=bu_service_iam \
#   core_service_user_pswd=the-password \
#   bearer_token=my_bearer_token
#   snaplogic_env=BUDev \
#   snaplogic_bearer_token=snaplogic.token
#   buid=U21967744
#   DRYRUN=true
#
# sh snaplogic.sh call-snaplogic profile=infnprd buid=TEST12345 snaplogic_bearer_token=snaplogic.token DRYRUN=true
#
# sh snaplogic.sh get-user profile=infnprd buid=U21967744 HOST=10.58.34.9 CORE_SERVICE_USER_PSWD=password DRYRUN=true
# sh snaplogic.sh get-user profile=infnprd buid=U21967744 HOST=stg.kuali.research.bu.edu DRYRUN=true

