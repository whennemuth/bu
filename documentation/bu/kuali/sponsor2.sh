#!/bin/bash

key=$(aws secretsmanager get-secret-value \
  --secret-id kuali/sam/gov \
  --output text \
  --query '{SecretString:SecretString}' | jq '.API_KEY' | sed 's/"//g')
  
# -------------------------------------------------
#                Entity Structure
# -------------------------------------------------
#   2J Sole Proprietorship
#   2K Partnership or Limited Liability Partnership
#   2L Corporate Entity (Not Tax Exempt)
#   8H Corporate Entity (Tax Exempt)
#   2A U.S. Government Entity
#   CY Country - Foreign Government
#   X6 International Organization
#   ZZ Other

# --------------------------------------------------------------------
#     Split calls into chunks to avoid the >= 1,000,000 size limit.
# --------------------------------------------------------------------
# sh sponsor2.sh link SAM_REGISTERED=No ENTITY_STRUCTURE_CODE=2J
# sh sponsor2.sh link SAM_REGISTERED=No ENTITY_STRUCTURE_CODE=2K
# sh sponsor2.sh link SAM_REGISTERED=No ENTITY_STRUCTURE_CODE=2L
# sh sponsor2.sh link SAM_REGISTERED=No ENTITY_STRUCTURE_CODE=8H
# sh sponsor2.sh link SAM_REGISTERED=No ENTITY_STRUCTURE_CODE=2A
# sh sponsor2.sh link SAM_REGISTERED=No ENTITY_STRUCTURE_CODE=CY
# sh sponsor2.sh link SAM_REGISTERED=No ENTITY_STRUCTURE_CODE=X6
# sh sponsor2.sh link SAM_REGISTERED=No ENTITY_STRUCTURE_CODE=ZZ
#
# sh sponsor2.sh link SAM_REGISTERED=Yes ENTITY_STRUCTURE_CODE=2J
# sh sponsor2.sh link SAM_REGISTERED=Yes ENTITY_STRUCTURE_CODE=2K
# sh sponsor2.sh link SAM_REGISTERED=Yes ENTITY_STRUCTURE_CODE=2L
# sh sponsor2.sh link SAM_REGISTERED=Yes ENTITY_STRUCTURE_CODE=8H
# sh sponsor2.sh link SAM_REGISTERED=Yes ENTITY_STRUCTURE_CODE=2A
# sh sponsor2.sh link SAM_REGISTERED=Yes ENTITY_STRUCTURE_CODE=CY
# sh sponsor2.sh link SAM_REGISTERED=Yes ENTITY_STRUCTURE_CODE=X6
# sh sponsor2.sh link SAM_REGISTERED=Yes ENTITY_STRUCTURE_CODE=ZZ

buildLink() {
  local link=''
  local separator='?'
  for part in $@ ; do
    if [ -z "$link" ] ; then
      link="${part}"
    else
      link="${link}${separator}${part}"
      [ $separator == '?' ] && separator='&'
    fi
  done
  echo "$link"
}

buildOutputFileName() {
  local gz="$1"
  local f="response"
  isDebug && f="test-$f"
  f="$f-$VERSION"
  [ "$SAM_REGISTERED" == 'Yes' ] && f="$f-sry" || f="$f-srn"
  f="$f-$ENTITY_STRUCTURE_CODE"
  f="${f}.${FORMAT,,}"
  f="${f}${gz}"
  echo "$f"
}

getDownloadLink() {
  local urlparts=(
    https://api.sam.gov/entity-information/$VERSION/entities
    format=$FORMAT
    samRegistered=$SAM_REGISTERED
    includeSections=entityRegistration,coreData
    # registrationStatus=A
    # emailId=Yes
  )

  if [ -n "$ENTITY_STRUCTURE_CODE" ] ; then
    if [ "$SAM_REGISTERED" == 'Yes' ] ; then
      urlparts=(${urlparts[@]} "q=(entityStructureCode:'$ENTITY_STRUCTURE_CODE')")
    else
      echo "INVALID PARAMETER! entityStructureCode is not a valid filter for non-sam registered entities"
      exit 1
    fi
  fi

  local out="$(buildOutputFileName)"
  
  cat <<EOF > command
    # $(date -R)
    
    curl -s \\
      -o $out \\
      -X POST \\
      -H "X-Api-Key: $key" \\
      -H "Content-Type: application/json" \\
      -H "Accept: application/json" \\
      "$(buildLink ${urlparts[@]})"


EOF
  if isDryrun ; then
    cat 'command'
  else
    eval "$(cat command)"
    sleep 2
    if isDebug ; then
      cat command $out > combined
      cat combined > $out
    fi
  fi
}

getDownloadFile() {
  # https://api.sam.gov/entity-information/v3/download-entities?token=agOfMloHZz
  local urlparts=(
    https://api.sam.gov/entity-information/$VERSION/download-entities
    token=$(getToken)
  )
  
  cat <<EOF > command
    # $(date -R)
    
    curl -s \\
      -o $(buildOutputFileName '.gz') \\
      -X POST \\
      -H "X-Api-Key: $key" \\
      -H "Content-Type: application/json" \\
      -H "Accept: application/zip" \\
      "$(buildLink ${urlparts[@]})"


EOF
  if isDryrun ; then
    cat 'command'
  else
    eval "$(cat command)"
  fi
}

getToken() {
  local token="$TOKEN"
  if [ -n "$token" ] ; then
    echo $token
  else
    local linkfile="$(buildOutputFileName)"
    [ ! -f "$linkfile" ] && echo "ERROR! $linkfile does not exist" && exit 1
    # TODO: find out how the token is presented in the file
    cat $linkfile | grep -oP '(?<=token=)[\S]+'
  fi
}

isDebug() {
  [ "$DEBUG" == 'true' ] && true || false
}
isDryrun() {
  [ "$DRYRUN" == 'true' ] && true || false
}
isCSV() {
  [ "$FORMAT" == 'CSV' ] && true || false
}
isJson() {
  [ "$FORMAT" == 'JSON' ] && true || false
}

parseargs() {
  task="${1,,}"

  [ -z "$task" ] && echo "Task parameter required!" && exit 1

  shift

  for nv in $@ ; do
    eval "$nv" 2> /dev/null
  done

  # Set defaults
  VERSION=${VERSION:-"v3"}
  FORMAT=${FORMAT:-"CSV"}
  SAM_REGISTERED=${SAM_REGISTERED:-"No"}
  # ENTITY_STRUCTURE_CODE=${ENTITY_STRUCTURE_CODE:-"2L"}
}


parseargs $@ && shift

case $task in
  link)
    getDownloadLink ;;
  file)
    getDownloadFile ;;
  both)
    getDownloadLink
    printf "\nUse token now to download the file? (y/n): "
    read answer
    if [ "${answer,,}" == 'y' ] ; then
      getDownloadFile 
    fi
    ;;
  test)
    testparts=(
      https://api.sam.gov/entity-information/v3/entities
      format=JSON
      # registrationStatus=A
      includeSections=entityRegistration
    )
    buildLink ${testparts[@]}
    ;;
esac
