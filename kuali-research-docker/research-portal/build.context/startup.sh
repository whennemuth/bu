#!/bin/bash

startPortal() {
  cd /var/portal

  checkDebug "$1"

  sourceExports $?

  sourceBashLibs $?

  checkHost $?

  checkMongoIsLocalhost $?

  runPortalApp
}

# Provided a string command, log it and then run (eval) it.
runAndLog() {
   echo "$(date '+%F-%T')" >> /var/portal/startup.log
   if [ -n "$2" ] ; then
      if [ "$2" == "/var/portal/startup.log" ] ; then
        runAndLog "$1"
        return $?
      fi
      echo "$1 2>&1 | tee -a $2" >> /var/portal/startup.log
      eval "$1" 2>&1 | tee -a $2
      # eval $1 2>&1
   else
      echo $1
      echo $1 >> /var/portal/startup.log
      eval $1 2>&1 | tee -a /var/portal/startup.log
      # eval $1 2>&1
   fi
   echo "" >> /var/portal/startup.log
   echo "" >> /var/portal/startup.log
}


# Log the provided string to the log file.
echoAndLog() {
   echo "$(date '+%F-%T')" >> /var/portal/startup.log
   echo "$1"
   echo "$1" >> /var/portal/startup.log
   echo "" >> /var/portal/startup.log
   echo "" >> /var/portal/startup.log
}

checkErrors() {
  [ -n "$1" ] &&  [ $1 -gt 0 ] && exit 1
}

checkDebug() {
  [ "$1" == 'debug' ] && debug="true" || return 0;
}

sourceExports() {
  checkErrors $1
  if [ -f /var/portal-config/export.sh ] ; then
    echo "Sourcing /var/portal-config/export.sh..."
    source /var/portal-config/export.sh
  else
    return 0
  fi
}

sourceBashLibs() {
  checkErrors $1
  if [ -f /var/bash-scripts/bash.lib.sh ] ; then
    source /var/bash-scripts/bash.lib.sh
  elif [ -f bash.lib.sh ] ; then
    source bash.lib.sh
  elif [ -f ../../bash.lib.sh ] ; then
    source ../../bash.lib.sh
  else
    echo "ERROR! Cannot find bash.lib.sh"
    exit 1
  fi
}

checkHost() {
  checkErrors $1
  if [ -z "$PORTAL_HOST" ] ; then
    echo "ERROR! PORTAL_HOST must be set."
    echo "Cancelling startup!"
    return 1
  fi

  local localhost=""
  if isLocalHost "$PORTAL_HOST" ; then localhost="true"; fi

  if [ -n "$LANDSCAPE" ] ; then
    # It is assumed that LANDSCAPE and PORTAL_HOST were passed in with compatible values.
    # Could assume otherwise and put a big pile of contingency logic here, but that might be overkill.
    if [ $localhost ] ; then
      echo "WARNING! PORTAL_HOST = $PORTAL_HOST, which indicates a local deployment. LANDSCAPE should be empty!"
      echo "DISCARDING LANDSCAPE value of $LANDSCAPE"
      LANDSCAPE=""
    fi
  elif [ ! $localhost ] ; then
    echo "ERROR! Must set a LANDSCAPE value that matches PORTAL_HOST value or $PORTAL_HOST"
    echo "Cancelling startup!"
    return 1
  fi
  return 0
}

# If in addition to the mongo client the mongo daemon was installed during building
# of the docker image, then by default a /data/db directory would have been created
# for data storage. Since the daemon was installed locally, assume we are running
# mondodb locally and mount to this data directory.
# If the docker image were built without the daemon, then it is assumed that
# the mongo data source is remote (probably running in a cluster on AWS) and no
# data directory applies.
checkMongoIsLocalhost() {
   checkErrors $1
   if isLocalHost "$MONGODB_URI" && [ -d /data/db ] ; then
      # Start mongo daemon as a detached background process.
      echoAndLog "nohup mongod > /dev/null 2>&1 &"
      nohup mongod > /dev/null 2>&1 &

      # Keep pinging mongo until the database is in a ready state and will accept connections.
      # Give it ten seconds and then give up
      local i=1
      local ready=""
      while ((i<50)) ; do
        if mongo --host localhost --eval 'db.version()' > /dev/null 2>&1 ; then
          echo "Mongo connection success!"
          ready="true"
          break;
        else
          printf "Mongo connection not ready yet!\nWaiting..."
        fi
        ((i+=1))
        sleep .2
      done
      if [ ! $ready ] ; then
        echoAndLog "ERROR! mongo connection not ready after 10 second wait!"
        return 1
      fi
   elif [ -z "$MONGODB_URI" ] ; then
      echoAndLog "ERROR!!! No mongo database defined"
      return 1
   else
      echoAndLog "Using remote mongo database: $MONGODB_URI"
   fi
}

runPortalApp() {
   checkErrors $1

   local startcmd="$START_CMD"
   [ -z "$START_CMD" ] && startcmd="npm start"
   echoAndLog "Running ${startcmd}. Not logging to /var/portal/startup.log, but you can see output in docker log"
   eval "$START_CMD"
}

startPortal
