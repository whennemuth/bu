#!/bin/bash

# Get a json dump of entities info from sam.gov

key=$(aws secretsmanager get-secret-value \
  --secret-id kuali/sam/gov \
  --output text \
  --query '{SecretString:SecretString}' | jq '.API_KEY' | sed 's/"//g')

test1() {
  local out=entity-get-v$1.json
  cat <<EOF > command
    # $(date -R)
    
    curl -s \\
      -o $out \\
      -X GET \\
      -H "Content-Type: application/json" \\
      -H "Accept: application/json" \\
      https://api.sam.gov/entity-information/v$1/entities?api_key=${key}&samRegistered=No&includeSections=entityRegistration&format=JSON 


EOF
  eval "$(cat command)"
  sleep 2
  cat command $out > combined
  cat combined > $out
}

test2() {
  local out=entity-get-v$1-next-page.json
  cat <<EOF > command
    # $(date -R)
    
    curl -s \\
      -o $out \\
      -X GET \\
      -H "Content-Type: application/json" \\
      -H "Accept: application/json" \\
      https://api.sam.gov/entity-information/v$1/entities?api_key=${key}&page=1&size=10
      # https://api.sam.gov/entity-information/v$1/entities?api_key=${key}&page=1&size=10&includeSections=entityRegistration


EOF
  eval "$(cat command)"
  sleep 2
  cat command $out > combined
  cat combined > $out
}

test3() {
  local out=entity-extract-post-v$1-active.json
  cat <<EOF > command
    # $(date -R)
    
    curl -s \\
      -o $out \\
      -X POST \\
      -H "X-Api-Key: $key" \\
      -H "Content-Type: application/json" \\
      -H "Accept: application/json" \\
      https://api.sam.gov/entity-information/v$1/entities?format=JSON&registrationStatus=A&includeSections=entityRegistration


EOF
  eval "$(cat command)"
  sleep 2
  cat command $out > combined
  cat combined > $out
}

test4() {
  local out=entity-get-no-header-v$1.json
  cat <<EOF > command
    # $(date -R)

    curl -s \\
      -o $out \\
      -X GET \\
      https://api.sam.gov/entity-information/v$1/entities?api_key=${key}&includeSections=entityRegistration


EOF
  eval "$(cat command)"
  sleep 2
  cat command $out > combined
  cat combined > $out
}

test5() {
  local out=entity-extract-get-v$1-active.json
  cat <<EOF > command
    # $(date -R)
    
    curl -s \\
      -o $out \\
      -X GET \\
      https://api.sam.gov/entity-information/v$1/entities?api_key=${key}&format=json&includeSections=entityRegistration


EOF
  eval "$(cat command)"
  sleep 2
  cat command $out > combined
  cat combined > $out
}

isValidArg() {
  local valid='false'
  # Require arg be numeric
  if [ -n "$(echo "$1" | grep -P '^\d+$')" ] ; then
    if [ $1 -ge 1 ] && [ $1 -le 6 ] ; then
      valid='true'
    fi
  fi
  [ $valid == 'true' ] && true || false
}

if ! isValidArg $1 ; then
  echo "Invalid arg: \"$1\", must be numeric and be in range!"
  exit 1
else
  id=$1
  shift
fi

eval "test${id}" $@