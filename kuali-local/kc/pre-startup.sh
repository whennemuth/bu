#!/bin/bash

declare -r REST_API_KEY_NAME='REST_API_KEY'

USE_SSL=${USE_SSL:-"false"}

inDebugMode() {
  [[ "$-" == *x* ]] && true || false
}

outputHeading() {
  inDebugMode && set +x && local returnToDebugMode='true'
  local msg="$1"
  [ -n "$outputHeadingCounter" ] && msg="$outputHeadingCounter) $msg" && ((outputHeadingCounter++))
  local border='#####################################'
  echo ""
  echo ""
  echo "$border"
  echo "       $msg"
  echo "$border"
  [ "$returnToDebugMode" == 'true' ] && set -x || true
}

getHost() {
  [ "$USE_SSL" == 'true' ] && echo 'https://proxy' || echo 'http://cor-main:3000'
}

stripAwayFileExtension() {
  read file
  echo "$(echo $file | rev | cut -d'.' -f2- | rev)"
}

stripAwayPath() {
  read pathname
  echo "$pathname" | awk 'BEGIN {RS="/"} {print $1}' | tail -1
}

getTokenVariableName() {
  local vartype="${1,,}"
  local username="$2"
  echo "$(echo ${username^^} | sed -E 's/[^a-zA-Z0-9]/_/g')_${vartype^^}_TOKEN"
}

getToken() {
  local username=${1:-"admin"}
  local password=${2:-"admin"}
  local tokenvar="$(getTokenVariableName 'user' $username)"
  [ -n "${!tokenvar}" ] && echo "${!tokenvar}" && return 0
  [ "$USE_SSL" == 'true' ] && protocol="${protocol}s"
  [ -n "$port" ] && host="${host}:${port}"
  rm -f /tmp/LAST_TOKEN_STDOUT 2> /dev/null
  rm -f /tmp/LAST_TOKEN_STDERR 2> /dev/null
  rm -f /tmp/LAST_TOKEN_STATUS 2> /dev/null

  local statusCode="$(
  curl \
    --insecure \
    --show-error \
    --silent \
    --output /tmp/LAST_TOKEN_STDOUT \
    --stderr /tmp/LAST_TOKEN_STDERR \
    --write-out '%{http_code}' \
    -X POST \
    -H "Authorization: Basic $(echo -n "${username}:${password}" | base64 -w 0)" \
    -H "Content-Type: application/json" \
    "$(getHost)/api/v1/auth/authenticate")"

  echo "$statusCode" > /tmp/LAST_TOKEN_STATUS
  if [ -f /tmp/LAST_TOKEN_STDOUT ] ; then
    eval "$tokenvar="$(cat /tmp/LAST_TOKEN_STDOUT | jq -r '.token' 2> /dev/null)""
    if [ -n "${!tokenvar}" ] && [ "${!tokenvar}" != 'null' ]; then
      echo "${!tokenvar}"
    fi
  fi
}

getNewToken() {
  local username=${1:-"admin"}
  local password=${2:-"admin"}
  local tokenvar="$(getTokenVariableName 'user' $username)"
  eval "unset $tokenvar"
  getToken $@
}

# For a specified user, look for an api key saved off as a file and return the content if found.
# If not found, create a new one, cache it, and return that.
getApiToken() {
  local username=${1:-"admin"}
  local password=${2:-"admin"}

  # 1) Look for the api key in the file cache first.
  local keyname=${3:-"$REST_API_KEY_NAME"}
  local cachefile="/opt/kuali/api_keys/${username}.${keyname}"
  if [ -f "$cachefile" ] ; then
    local keyval="$(cat $cachefile)"
    if [ -n "$keyval" ] ; then
      echo "$keyval"
      return 0
    fi
  fi

  # 3) Create a new one and return its value
  if apiKeyAlreadyExists $username $password $keyname ; then
    echo "ERROR! A \"$keyname\" key exists in the mongo database for user \"$username\", but cannot be found anywhere locally!"
    echo "Use the core web frontend to remove the key. Then run this again to get a new one."
    return 1
  else
    [ "$USE_SSL" == 'true' ] && protocol="${protocol}s"
    [ -n "$port" ] && host="${host}:${port}"
    local id=$(getUserId "$username")
    local path="api/v1/users/${id}/tokens"
    local bearerToken=$(getToken $username $password 2> /dev/null)

    local keyval="$(curl \
      --insecure \
      -X POST \
      -H "Authorization: Bearer ${bearerToken}" \
      -H "Content-Type: application/json" \
      --data '{ "name":"'$keyname'" }' \
      "$(getHost)/api/v1/users/${id}/tokens" \
      | jq -r '.value')"

    echo "$keyval" > $cachefile
    echo "$keyval"
  fi
}

# Determine by REST call if a specified user already has an api key with a specified name.
apiKeyAlreadyExists() {
  local username=${1:-"admin"}
  local password=${2:-"admin"}
  local keyname=${3:-"$REST_API_KEY_NAME"}
  local id=$(getUserId "$username")
  local lookupId="$(curl \
    --insecure \
    --silent \
    --show-error \
    -X GET \
    "$(getHost)/api/v1/users/${id}/tokens?type=apiKey&name=$keyname" \
    --header "Content-Type: application/json" \
    --header "Authorization: Bearer $(getToken) > /dev/null" \
    | jq -r '.[0].userId')"

  [ "$id" == "$lookupId" ] && true || false
}

# Uncache any reference to a particular api key, 
getNewApiToken() {
  local username=${1:-"admin"}
  local password=${2:-"admin"}
  local tokenvar="$(getTokenVariableName 'api' $username)"
  eval "unset $tokenvar"
  getApiToken $@
}

getRestUserApiToken() {
  getApiToken 'rest.svc.user' 'password'
}

getCoreAuthToken() {
  getRestUserApiToken
}

getUser() {
  local username="$1"
  curl \
    --insecure \
    --silent \
    --show-error \
    -X GET \
    $(getHost)/api/v1/users?username=$username \
    --header "Content-Type: application/json" \
    --header "Authorization: Bearer $(getToken) > /dev/null" 
}

getUserId() {
  local username="$1"
  getUser "$username" | jq -r '.[0].id'
}

userExistsInCore() {
  local username="$1"
  local result="$(echo "$(getUser $username)" | jq -r '.[0].username')"
  [ "$username" == "$result" ] && true || false
}

addUserToCore() {
  local user="$1"
    local username="$(echo $user | stripAwayPath | stripAwayFileExtension)"
  echo "Adding $username to core..."
  curl \
    --insecure \
    -X POST \
    -H "Authorization: Bearer $(getToken)" \
    -H 'Content-Type: application/json' \
    -d @$user \
    $(getHost)/api/v1/users
  echo "$username added."
}

# The cor-main container take some seconds to initialize. During this period, it will return 4xx status codes
# when attempting to use its REST api. Use this fact to poll for initialization being complete, timing out after
# a minute (if still receiving 4xx after timeout, there must be something wrong with the container). 
waitForCore() {
  local counter=0
  local ready='false'
  local timeout=60

  isHealthyStatus() {
    local healthy='false'
    if [ -f /tmp/LAST_TOKEN_STATUS ] ; then
      local status="$(cat /tmp/LAST_TOKEN_STATUS)"
      [ "$status" -ge 200 ] && [ "$status" -lt 300 ] && healthy='true'
    fi
    [ "$healthy" == 'true' ] && true || false
  }

  while true ; do
    getToken > /dev/null
    if isHealthyStatus ; then
      ready='true'
      break;
    else
      ((counter+=5))
      if [ $counter -ge $timeout ] ; then       
        break;
      fi
      echo "cor-main container not ready yet, waiting 5 seconds..."
      sleep 5
    fi
  done
  if [ "$ready" == 'true' ] ; then
    echo "cor-main container is ready!"
    true
  else
    echo "ERROR: It has been a full $timeout seconds and core is not responding. Cancelling kuali-research container start..."
    echo "Last token retrieval error:"
    cat /tmp/LAST_TOKEN_STDERR
    false
  fi
}

initializeCore() {
  outputHeading "Initializing cor-main..."
  checkCoreAuthToken() {
    local username="$1"
    if [ "$username" == 'rest.svc.user' ] ; then
      export CORE_AUTH_TOKEN="$(getRestUserApiToken)"
      echo "CORE_AUTH_TOKEN: $CORE_AUTH_TOKEN"
    fi
  }

  if waitForCore ; then
    echo "Verifying/creating users in cor-main..."
    for user in /opt/kuali/users/*.json ; do \
      local username="$(echo $user | stripAwayPath | stripAwayFileExtension)"
      if ! userExistsInCore $username ; then
        addUserToCore $user
      else
        echo "$username already exists in cor-main mongo database."
      fi
      checkCoreAuthToken $username
    done
    true
  else
    echo "ERROR: Users not set up in cor-main and CORE_AUTH_TOKEN not set."
    false
  fi
}



# catalina.sh unsets CLASSPATH, then rebuilds in part using instructions in setenv.sh. So, you cannot simply set CLASSPATH before you start tomcat.
# Extract the log4j jars from the war file and put them into the tomcat lib directory so they can appear in the bootstrap classpath.
# Need to also supply jackson libraries as log4j2-tomcat.xml configuration has a <JsonLayout for one of its appenders.
# woodstox-core if for xml-based appenders in case those are used.
configureLog4j() {
  outputHeading "Configuring log4j2..."
 
  copyMavenDependency "log4j-core" "$DOCLIB" "$LOG4JLIB"
  copyMavenDependency "log4j-api" "$DOCLIB" "$LOG4JLIB"
  copyMavenDependency "log4j-jul" "$DOCLIB" "$LOG4JLIB"
  copyMavenDependency "log4j-appserver" "$DOCLIB" "$LOG4JLIB"
  copyMavenDependency "woodstox-core" "$DOCLIB" "$LOG4JLIB"
  copyMavenDependency "stax" "$DOCLIB" "$LOG4JLIB"
  copyMavenDependency "jackson-core" "$DOCLIB" "$LOG4JLIB"
  copyMavenDependency "jackson-databind" "$DOCLIB" "$LOG4JLIB"
  copyMavenDependency "jackson-annotations" "$DOCLIB" "$LOG4JLIB"
}

# Make sure the catalina lib directory checks out for having the necessary jars.
verifyLog4jConfig() {
  local missing=()

  verify() {
    local jar="$1"
    eval "local result=\$(ls -1 $CATALINA_HOME/lib/${jar}* 2> /dev/null)"
    if [ -z "$result" ] ; then
      missing=(${missing[@]} $jar)
    fi
  }

  verify 'log4j-core'
  verify 'log4j-api'
  verify 'log4j-jul'
  verify 'log4j-appserver'
  verify 'woodstox-core'
  verify 'stax'
  verify 'jackson-core'
  verify 'jackson-databind'
  verify 'jackson-annotations'

  if [ ${#missing} -gt 0 ] ; then
    echo "Failed to get the following jars into the tomcat lib directory:"
    printf ' - %s\n' "${missing[@]}"
    false
  else
    true
  fi
}

# Search a specified directory for a maven dependency artifact and copy it to the specified target directory if found.
# If not found, analyze the pom.xml file for enough artifact info to download the artifact from maven central to the target directory.
copyMavenDependency() {
  local artifactId="$1"
  local sourcedir="$2"
  local targetdir="$3"

  foundInSourceDir() {
    # The artifact was built and can be found in the target directory
    [ -n "$(ls -1 $sourcedir | grep $artifactId)" ] && true || false
  }
  copyFromSourceDir() {
    \cp -n -v "$sourcedir/${artifactId}"* $targetdir    
  }
  acquireFromMavenCentral() {
    # Query the pom for artifact details and download the artifact from maven central repository
    echo "No artifacts found in $sourcedir matching $artifactId. Downloading from maven central"
    local property="maven-dependency-plugin.version"
    local pluginver=$(cat $POM | grep -oP "(?<=<${property}>).*(?=</${property}>)")
    local groupId="$(getMavenDependencyAttribute $artifactId 'groupId')"
    local version="$(getMavenDependencyAttribute $artifactId 'version')"
    property="$(echo "$version" | grep -oP '(?<=\$\{).*?(?=\})')"
    # If the version holds a property, get the property value set the version with it.
    [ -n "$property" ] && version=$(cat $POM | grep -oP "(?<=<${property}>).*(?=</${property}>)")
    
    mvn -f $POM \
      org.apache.maven.plugins:maven-dependency-plugin:${pluginver}:copy \
      -Dartifact=${groupId}:${artifactId}:${version} \
      -DoutputDirectory=$targetdir
  }

  if foundInSourceDir ; then
    copyFromSourceDir
  else
    acquireFromMavenCentral
  fi
}

getMavenDependencyAttribute() {
  local artifactId="$1"
  local attribute="$2"
  tr -d '\n\t ' < $POM \
    | sed -e 's/<\/dependency>/\n/g' \
    | grep '<artifactId>log4j-appserver</artifactId>' \
    | grep -oP '(?<=<'$attribute'>).*?(?=</'$attribute'>)'
}