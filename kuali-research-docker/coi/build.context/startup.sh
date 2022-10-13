#!/bin/bash

runAll() {
   printHeader

   if [ -n "$MONGO_CONNECTION_STRING" ] ; then
     
     startBasic

   else 
     
     configOracle

     applyMigrations

     startWithOracle
   fi
}

runAndLog() {
   echo "$(date '+%F-%T')" >> /var/research-coi/startup.log
   if [ -n "$2" ] ; then
      if [ "$2" == "/var/research-coi/startup.log" ] ; then
        runAndLog "$1"
        return $?
      fi
      echo "$1 2>&1 | tee -a $2" >> /var/research-coi/startup.log
      eval "$1" 2>&1 | tee -a $2
      # eval $1 2>&1
   else
      echo $1
      echo $1 >> /var/research-coi/startup.log
      eval $1 2>&1 | tee -a /var/research-coi/startup.log
      # eval $1 2>&1
   fi
   echo "" >> /var/research-coi/startup.log
   echo "" >> /var/research-coi/startup.log
}

echoAndLog() {
   echo "$(date '+%F-%T')" >> /var/research-coi/startup.log
   echo $1
   echo $1 >> /var/research-coi/startup.log
}

printHeader() {
   echo " " >> /var/research-coi/startup.log
   echo "====================================" >> /var/research-coi/startup.log
   echo "COI startup $(date) ..." >> /var/research-coi/startup.log

   echoAndLog "DB_PACKAGE = $DB_PACKAGE"
}

# If oracle is the database, set related environment variables
configOracle() {
   if [ "$DB_PACKAGE" == "oracledb" ] ; then
      if [ -d /var/research-coi/node_modules/mysql ] ; then
         # Uninstalling mysql here as it could not be done in the docker image build. See relate dockerfile for comments.
         echoAndLog "Uninstalling mysql module..."
         cd /var/research-coi && echoAndLog "cd /var/research-coi"
         runAndLog "npm uninstall mysql"
      fi
      if [ -z "$(echo $LD_LIBRARY_PATH | grep $LD_LIB_PATH)" ] ; then
         # Expecting LD_LIB_PATH = /usr/lib/oracle/12.2/client64/lib
         echoAndLog "Setting LD_LIBRARY_PATH=$LD_LIB_PATH:$LD_LIBRARY_PATH"
         export LD_LIBRARY_PATH=$LD_LIB_PATH:$LD_LIBRARY_PATH
      else 
         echoAndLog "Oracle client already set in LD_LIBRARY_PATH"
      fi
      if [ -z "$(echo $PATH | grep $LD_BIN_PATH)" ] ; then
         # Expecting LD_BIN_PATH = /usr/lib/oracle/12.2/client64/bin
         echoAndLog "Setting PATH=$LD_BIN_PATH:$PATH"
         export PATH=$LD_BIN_PATH:$PATH
      else
         echoAndLog "Oracle client already set in PATH"
      fi
      echoAndLog "LD_LIBRARY_PATH = $LD_LIBRARY_PATH"
      echoAndLog "PATH = $PATH"
   fi
}

applyMigrations() {
   if [ -n "$DB_NAME" ] ; then 
      if [ -n "$MIGRATIONS_TO_RUN" ] || [ ! -f /var/research-coi/migration.sh ] || [ ! -f /var/research-coi/migration.log ] ; then
         if [ -n "$(echo $MIGRATIONS_TO_RUN | grep -i all)" ] || [ -z "$MIGRATIONS_TO_RUN" ] ; then
            # The knexfile should be in the root directory of the app /var/research-coi
            # Alternatively you can place it in /var/research-coi/db/migration
            # However, supplying the location as an argument does not seem to work.
            relocateKnexFile
            cat <<-EOF > /var/research-coi/migration.sh
               node \\
                  /var/research-coi/node_modules/knex/bin/cli.js \\
                  --cwd=db/migration \\
                  migrate:latest \\
                  --env kc_coi &> /var/research-coi/migration.log \\
                  # --knexfile /var/research-coi-config/knexfile.js
	EOF
            echoAndLog "Running migration:\n"
            cat /var/research-coi/migration.sh >> /var/research-coi/startup.log
            cd /var/research-coi
            source /var/research-coi/migration.sh
            mv /var/research-coi/migration.sh /var/research-coi/migration.sh.ran.$(date "+%F-%T")
         else
            echoAndLog "Delimited list of migrations. Write more scripting to run just those."
         fi
      fi
   fi
}

relocateKnexFile() {
  local target=/var/research-coi/db/migration/knexfile.js
  local source1=/var/research-coi-config/knexfile.js
  local source2=/var/research-coi/knexfile.js

  [ -f $source1 ] && \
    echo "y" | cp -f $source1 $target

  [ -f $target ] && return 0;

  [ -f $source2 ] && \
    echo "y" | cp -f $source2 $target

  [ -f $target ] && return 0;

  return 1;
}

# Environment variables $DB_NAME and $DB_PACKAGE should be set at this point
startWithOracle() {
  if [ -n "$DB_NAME" ] && [ -n "$DB_PACKAGE" ] ; then
    startBasic
  else
    echoAndLog "One or more environment variables [DB_NAME, DB_PACKAGE] not set. npm run cancelled!"
  fi
}

# Environment variable $MONGO_CONNECTION_STRING should be set at this point
startBasic() {
  cd /var/research-coi
  local startcmd="$START_CMD"
  [ -z "$START_CMD" ] && startcmd="npm start"
  echoAndLog "Running ${startcmd}. Not logging to /var/research-coi/startup.log, but you can see output in docker log"
  eval "$START_CMD"
}

runAll

