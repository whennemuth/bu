#!/bin/bash

if [ -f /var/bash-scripts-mount/bash.lib.sh ] ; then
  # Used for development. Convenient way to override the bash library with an edited version.
  # The docker run command would have to include the additional mount argument.
  source /var/bash-scripts-mount/bash.lib.sh
elif [ -f /var/bash-scripts/bash.lib.sh ] ; then
  source /var/bash-scripts/bash.lib.sh
elif [ -f bash.lib.sh ] ; then
  source bash.lib.sh
elif [ -f ../../bash.lib.sh ] ; then
  source ../../bash.lib.sh
else
  echo "ERROR! Cannot find bash.lib.sh"
  exit 1
fi

[ "$1" == 'debug' ] && debug="true";

[ -f /var/core-config/export.sh ] && echo "Sourcing /var/core-config/export.sh..." && source /var/core-config/export.sh

checkError() {
   [ -n "$1" ] && echo "$1" 1>&2
   if [ -z "$debug" ] ; then
      echo "TODO: finish this"
   fi
}

initialize() {
  
  if [ -z "$CORE_HOST" ] ; then
    echo "ERROR! CORE_HOST must be set."
    echo "Cancelling startup!"
    return 1
  fi

  local localhost=""
  if isLocalHost "$CORE_HOST" ; then localhost="true"; fi

  if [ -n "$LANDSCAPE" ] ; then
    # It is assumed that LANDSCAPE and CORE_HOST were passed in with compatible values.
    # Could assume otherwise and put a big pile of contingency logic here, but that might be overkill.
    if [ $localhost ] ; then
      echo "WARNING! CORE_HOST = $CORE_HOST, which indicates a local deployment. LANDSCAPE should be empty!"
      echo "DISCARDING LANDSCAPE value of $LANDSCAPE"
      LANDSCAPE=""
    fi
  elif [ ! $localhost ] ; then
    echo "ERROR! Must set a LANDSCAPE value that matches CORE_HOST value or $CORE_HOST"
    echo "Cancelling startup!"
    return 1
  fi

  if [ -n "$LANDSCAPE" ] && [ -n "${SHIB_HOST}" ] ; then
    SHIB_IDP_METADATA_URL=https://$SHIB_HOST/idp/shibboleth
    SHIB_SP_METADATA_URL=https://$CORE_HOST/Shibboleth.sso/Metadata
    SHIB_SP_CERT_ISSUER=https://$CORE_HOST/shibboleth
    SHIB_ENTRY_POINT=https://$SHIB_HOST/idp/profile/SAML2/Redirect/SSO
    SAML_META_DIR=/var/core/services/auth/server
    SAML_META_FILE=$SAML_META_DIR/samlMeta.xml
  fi
}


startCore() {
   echo "Core startup $(date) ..." > /var/core/startup.log

   initialize 

   refreshMountedContent $?

   cd /var/core && echoAndLog "cd /var/core"

   checkMongoParameters $?

   checkMongoIsLocalhost $?

   checkRedisIsLocalhost $?

   checkMigrations $?

   checkJobsScheduler $?

   updateIncommons $?

   updateInstitution $?

   runCoreApp
}


# Provided a string command, log it and then run (eval) it.
runAndLog() {
   echo "$(date '+%F-%T')" >> /var/core/startup.log
   if [ -n "$2" ] ; then
      if [ "$2" == "/var/core/startup.log" ] ; then
        runAndLog "$1"
        return $?
      fi
      echo "$1 2>&1 | tee -a $2" >> /var/core/startup.log
      eval "$1" 2>&1 | tee -a $2
      # eval $1 2>&1
   else
      echo $1
      echo $1 >> /var/core/startup.log
      eval $1 2>&1 | tee -a /var/core/startup.log
      # eval $1 2>&1
   fi
   echo "" >> /var/core/startup.log
   echo "" >> /var/core/startup.log
}


# Log the provided string to the log file.
echoAndLog() {
   echo "$(date '+%F-%T')" >> /var/core/startup.log
   echo "$1"
   echo "$1" >> /var/core/startup.log
   echo "" >> /var/core/startup.log
   echo "" >> /var/core/startup.log
}


# Check if the core main application directory has a git readme.md file in it.
hasReadmeFile() {
   local readme=$(ls /var/core | tr '\n' '\n' | grep -iP '^readme\.md$')
   if [ -n "$readme" ] ; then true; else false; fi
}


# Copy files from the mounted configuration directory into their proper destinations
refreshMountedContent() {
   [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1
  
   if [ -f /var/core-config/local.js ] ; then
      runAndLog "cat /var/core-config/local.js > /var/core/config/local.js"
   else
      local msg="/var/core-config/local.js not found!"
      if [ -f /var/core/config/local.js ] ; then
        msg="WARNING: $msg Using pre-existing: /var/core/config/local.js"
      elif [ -f /var/core/config/default.js ] ; then
        msg="WARNING: $msg /var/core/config/local.js not found either! Application will default to /var/core/config/default.js"
      else
        msg="ERROR: $msg /var/core/config/local.js and /var/core/config/default.js not found either!"
      fi
      echoAndLog "$msg"
      [ -n "$(echo "$msg" | grep 'ERROR')" ] && return 1
   fi

   [ ! $SHIB_HOST ] && ! isLocalHost "$CORE_HOST" && return
   [ ! $SAML_META_FILE ] && return

   if [ -f /var/core-config/samlMeta.xml ] ; then
      runAndLog "cat /var/core-config/samlMeta.xml > $SAML_META_FILE"
   else
      echoAndLog "WARNING! file not found: /var/core-config/samlMeta.xml"
      if [ ! -f $SAML_META_FILE ] && [ -n "$SHIB_SP_METADATA_URL" ] ; then
        if [ -n "$LANDSCAPE" ] ; then
          createSamlMetaFromScratch
        else
          echoAndLog "ERROR! LANDSCAPE environment variable not set. Cannot create samlMeta.xml without it."
          return 1
        fi
      else
        return 0
      fi
   fi
   cat $SAML_META_FILE > $SAML_META_DIR/samlMeta2.xml
}


# Create a samlMeta.xml file from what is returned by apache when its Shibboleth SP metadata url is curled.
# This returned file must have the service consumer assertions added that reflect core, coi, kc, and kc via core.
createSamlMetaFromScratch() {
   echoAndLog "WARNING! file not found $SAML_META_FILE"
   echoAndLog "Attempting to obtain from $SHIB_SP_METADATA_URL ..."
   runAndLog "curl $SHIB_SP_METADATA_URL > /var/core-config/samlMeta.xml"
   if [ -n "$(cat /var/core-config/samlMeta.xml | grep 'EntityDescriptor')" ] ; then
      # Having curled in the SP metadata file, this is not enough - it is incomplete and more content must be added to it.
      # The metadata generator at http:[host]/Shibboleth.sso/Metadata produces an xml file that does not
      # include the addtional <md:AssertionConsumerService elements for redirecting back to the respective 
      # application landing pages for core and coi (only the kc ones). This is because it does not include the
      # ApplicationOverride elements in the Shibboleth2.xml configuration file it references during generation.
      # SEE: https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPApplicationOverride#NativeSPApplicationOverride-UsingtheOverride

      landscape="-${LANDSCAPE,,}"
      [ -n "$(echo "$landscape" | grep -i 'prod')" ] && landscape=""
      element="  <md:AssertionConsumerService Binding=\"BINDING\" Location=\"LOCATION\" index=\"INDEX\" \\\\\/>"
      corelocation="https:\\\\\/\\\\\/kuali-research${landscape}.bu.edu\\\\\/auth\\\\\/saml\\\\\/consume"
      coilocation="$corelocation?redirect_to=https:\\\\\/\\\\\/kuali-research${landscape}.bu.edu\\\\\/coi\\\\\/"
      kclocation="$corelocation?redirect_to=https:\\\\\/\\\\\/kuali-research${landscape}.bu.edu\\\\\/kc\\\\\/"
      kclocation2="${kclocation}kc-krad\\\\\/landingPage"
      bindings=(
        'urn:oasis:names:tc:SAML:1.0:profiles:artifact-01'
        'urn:oasis:names:tc:SAML:1.0:profiles:browser-post'
        'urn:oasis:names:tc:SAML:2.0:bindings:PAOS'
        'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact'
        'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign'
        'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST'
      )

      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[5]}/" | sed "s/LOCATION/${corelocation}/" | sed 's/INDEX/7/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[4]}/" | sed "s/LOCATION/${corelocation}/" | sed 's/INDEX/8/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[3]}/" | sed "s/LOCATION/${corelocation}/" | sed 's/INDEX/9/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[2]}/" | sed "s/LOCATION/${corelocation}/" | sed 's/INDEX/10/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[1]}/" | sed "s/LOCATION/${corelocation}/" | sed 's/INDEX/11/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${corelocation}/" | sed 's/INDEX/12/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml

      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[5]}/" | sed "s/LOCATION/${coilocation}/" | sed 's/INDEX/13/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[4]}/" | sed "s/LOCATION/${coilocation}/" | sed 's/INDEX/14/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[3]}/" | sed "s/LOCATION/${coilocation}/" | sed 's/INDEX/15/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[2]}/" | sed "s/LOCATION/${coilocation}/" | sed 's/INDEX/16/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[1]}/" | sed "s/LOCATION/${coilocation}/" | sed 's/INDEX/17/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${coilocation}/" | sed 's/INDEX/18/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml

      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation}/" | sed 's/INDEX/19/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation}/" | sed 's/INDEX/20/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation}/" | sed 's/INDEX/21/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation}/" | sed 's/INDEX/22/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation}/" | sed 's/INDEX/23/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation}/" | sed 's/INDEX/24/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml

      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation2}/" | sed 's/INDEX/25/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation2}/" | sed 's/INDEX/26/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation2}/" | sed 's/INDEX/27/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation2}/" | sed 's/INDEX/28/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation2}/" | sed 's/INDEX/29/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
      sed -i "s/<\/md:SPSSODescriptor>/$(echo "$element" | sed "s/BINDING/${bindings[0]}/" | sed "s/LOCATION/${kclocation2}/" | sed 's/INDEX/30/')\n  <\/md:SPSSODescriptor>/" /var/core-config/samlMeta.xml
   else
      echoAndLog "ERROR! Could not obtain metadata from $SHIB_SP_METADATA_URL"
   fi
}


# local.js will provide mongo connection parameters one of two ways:
#   1) Return the values of the corresponding environment variables
#   2) Alternatively provide hard-coded values.
# In any case, if local.js is converted to an object and that object does not yield any values, then we must raise
# an error here because mongo database connectivity is not possible.
checkMongoParameters() {
   [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1
   
   local configjs='/var/core/config/local.js'
   [ ! -f $configjs ] && configjs='/var/core-config/local.js'
   if [ ! -f $configjs ] ; then
     if isLocalHost "$MONGO_URI" ; then
       configjs='/var/core/config/default.js'
       if [ ! -f $configjs ] ; then
         echo "ERROR!!! Cannot find local.js (or default.js, since running as localhost)" && return 1
       else
         echo "WARNING! Cannot find local.js, but since running as localhost, substituting default.js"
       fi
     else
       echo "ERROR!!! Cannot find local.js" && return 1
     fi
   fi

   # "Clean" the local.js file (get rid of comments and anything else that javascript would not accept if treating json as an object).
   # NOTE: getting an error from "name: defer(cfg => cfg.app.name)" and associated const declaration, so substituting a simple text value instead.
   local processedJs="$(node -pe "$(cat $configjs | sed 's/defer([^()]*)/"SUBSTITUTION"/g' | sed 's/const defer.*/const defer = "SUBSTITUTION"/g')")"

   # Process mongo uri
   local uri=$(node -pe 'eval("var o = " + process.argv[1]); o.db.uri' "$processedJs")
   [ -z "$uri" ] && echo "ERROR! No mongo database URI found!" && return 1
   [ -z "$MONGO_URI" ] && MONGO_URI="$uri"

   # Leave function here if mongo is on localhost
   if isLocalHost "$MONGO_URI" ; then
      echo "Mongo is localhost. Skipping check for mongo username and password"
      return 0
   fi
   
   # Process mongo user
   local usr=$(node -pe 'eval("var o = " + process.argv[1]); o.db.options ? o.db.options.user : null' "$processedJs")
   if [ -z "$usr" ] ; then
     echo "ERROR! No mongo username provided!"
     return 1
   fi

   # Process mongo password
   local pwd=$(node -pe 'eval("var o = " + process.argv[1]); o.db.options ? o.db.options.pass : null' "$processedJs")
   if [ -z "$pwd" ] ; then
     echo "ERROR! No mongo password provided!" 
     return 1
   fi
}


# If in addition to the mongo client the mongo daemon was installed during building
# of the docker image, then by default a /data/db directory would have been created
# for data storage. Since the daemon was installed locally, assume we are running
# mondodb locally and mount to this data directory.
# If the docker image were built without the daemon, then it is assumed that
# the mongo data source is remote (probably running in a cluster on AWS) and no
# data directory applies.
checkMongoIsLocalhost() {
   [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1

   if isLocalHost "$MONGO_URI" && [ -d /data/db ] ; then
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
   elif [ -z "$MONGO_URI" ] ; then
      echoAndLog "ERROR!!! No mongo database defined"
      return 1
   else
      echoAndLog "Using remote mongo database: $MONGO_URI"
   fi
}


checkRedisIsLocalhost() {
   [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1
   
   # For local redis server use 127.0.0.1
   if isLocalHost "$REDIS_URI" ; then
      # Start redis daemon as a detached background process.
      echoAndLog "nohup redis-server > /dev/null 2>&1 &"
      nohup redis-server > /dev/null 2>&1 &

      # Keep pinging redis until the service is in a ready state.
      # Give it ten seconds and then give up
      local i=1
      local ready=""
      while ((i<50)) ; do
        pingReply=$(exec 3<>/dev/tcp/$REDIS_URI/6379 && \
                    echo -e "PING\r\n" >&3 && \
                    head -c 7 <&3 | \
                    sed 's/[^a-zA-Z]//g')
        if [ "${pingReply,,}" == 'pong' ] ; then
          echo "Redis readiness check successful!"
          ready="true"
          break;
        else
          printf "Redis service not ready yet!\nWaiting..."
        fi
        ((i+=1))
        sleep .2
      done
      if [ ! $ready ] ; then
        echoAndLog "ERROR! redis service not ready after 10 second wait!"
        return 1
      fi
   elif [ -z "$REDUS_URI" ] ; then
      echoAndLog "ERROR!!! No redis service defined"
      return 1
   else
      echoAndLog "Using remote redis service: $REDIS_URI"
   fi
}


# The core app should run migrate:up on startup (if started using npm run start), however we want this table to
# be updated before then, so we run the migrations now. The _migrations table will be created if it does not
# already exist, else :
#   1) entries will be added to it whose corresponding items are not found in the migrations directory.
#   2) all added migration items will be run to update the related mongo collections/documents.
checkMigrations() {
  [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1

  runAndLog "npm run migrate:up" migrate.up.log
}


# Run the job scheduler if necessary
checkJobsScheduler() {
   [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1

   [ -f /var/core/jobs.scheduler.log ] && rm -f /var/core/jobs.scheduler.log
   if [ "$JOBS_SCHEDULER" == "true" ] ; then
      # Run the jobs scheduler as a background process.
      echoAndLog "npm run jobs_scheduler > /var/core/jobs.scheduler.log 2>&1 &"
      nohup npm run jobs_scheduler > /var/core/jobs.scheduler.log 2>&1 &
   fi
}


# Make sure the incommons collection in mongodb has an entry for the shibboleth IDP cert
# and that it matches the corresponding value that can be curled directly from the IDP metadata url.
updateIncommons() {
  [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1

  [ ! $SHIB_HOST ] && ! isLocalHost "$CORE_HOST" && return

  [ ! $SHIB_IDP_METADATA_URL ] && return

	local SELECT=$(cat << EOF
	EXISTING_CERT=\$($(getMongoParms) \
	   --eval 'db.getCollection("incommons")
	      .find({
	         "idp": "$SHIB_IDP_METADATA_URL"
	      }
	   )'
	)
EOF
	)
   runAndLog "$SELECT"

   EXISTING_CERT=$(echo $EXISTING_CERT | grep -ioP '"cert"\x20*:\x20*"([^\"]+)"' | grep -Po '[^"]{10,}')
   DB_CERT="$EXISTING_CERT"
   echoAndLog "DB_CERT = $DB_CERT"

   [ -z "$EXISTING_CERT" ] && EXISTING_CERT="$IDP_CERT"
   if [ -z "$EXISTING_CERT" ] ; then
      EXISTING_CERT="$(curl $SHIB_IDP_METADATA_URL)"
      EXISTING_CERT="$(echo $EXISTING_CERT | grep -o -P '(?<=<ds:X509Certificate>).*?(?=</ds:X509Certificate>)' | sed s/[[:space:]]//g)"
   else
      echoAndLog "No IDP cert in database and not included as environment variable"
   fi

	local INSERT=$(cat << EOF
	$(getMongoParms) \
	   --eval 'db.getCollection("incommons")
	      .insertOne({ 
	         "idp": "$SHIB_IDP_METADATA_URL", 
	         "cert": "$EXISTING_CERT", 
	         "entryPoint": "$SHIB_ENTRY_POINT" 
	      }
	   )'
EOF
	)

   if [ -z "$EXISTING_CERT" ] ; then
      echoAndLog "ERROR: No shibboleth authentication cert in mongodb, as environment variable, or obtainable from $SHIB_IDP_METADATA_URL"
   elif [ "$EXISTING_CERT" != "$DB_CERT" ] ; then
      echoAndLog "EXISTING_CERT = $EXISTING_CERT"
      runAndLog "$INSERT"
   fi
}


updateInstitution() {
   [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1

   [ ! $SHIB_HOST ] && ! isLocalHost "$CORE_HOST" && return

   updateInstitutionIdp

   updateInstitutionIdpsSet

   updateInstitutionValidRedirectHosts
}


# Update the institutions collection idp fields to reflect BU
# NOTE: It is not clear if this data was replaced by the introduction of the "idps" set.
updateInstitutionIdp() {
  
  [ ! $SHIB_IDP_METADATA_URL ] && return

	local UPDATE=$(cat << EOF
	$(getMongoParms) \
	  --eval 'db.getCollection("institutions")
	    .updateOne(
	      { "name":"Kuali" },
	      { \$set: { 
	        "provider": "saml", 
	        "idp": "$SHIB_IDP_METADATA_URL",
	        "eppn": "buPrincipal",
	        "issuer": "$SHIB_SP_CERT_ISSUER"
	      }
	    })'
EOF
	)

   runAndLog "$UPDATE"
}


# Make sure the BU idp data has an entry in the "idps" set.
# NOTE: It is not clear if this data replaces the single idp data fields of the overall institution document.
updateInstitutionIdpsSet() {

  [ ! $SHIB_IDP_METADATA_URL ] && return

	local SELECT=$(cat << EOF
	EXISTING_IDP=\$($(getMongoParms) \
	  --eval 'db.getCollection("institutions")
	    .find({"idps":{
	      \$elemMatch: {
	        "eppn": "buPrincipal"
	      }
	    }},
	    {"idps": true}
	  )' | grep buPrincipal
	)
EOF
	)

	local INSERT=$(cat << EOF
	$(getMongoParms) \ 
	  --eval 'db.getCollection("institutions")
	    .updateOne(
	      { "name":"Kuali" },
	      {
	        \$addToSet: {
	          "idps": {
	            "idp": "$SHIB_IDP_METADATA_URL",
	            "eppn": "buPrincipal",
	            "issuer": "$SHIB_SP_CERT_ISSUER",
	            "name": "Kuali",
	            "provider": "saml"
	          }
	        }
	      })'
EOF
	)

   runAndLog "$SELECT"

   if [ -z "$EXISTING_IDP" ] ; then
      runAndLog "$INSERT" 
   fi
}


updateInstitutionValidRedirectHosts() {
	local SELECT=$(cat << EOF
	EXISTING_HOST=\$($(getMongoParms) \
	  --eval 'db.getCollection("institutions")
	    .find(
	      {"name":"Kuali"}, 
	      {"validRedirectHosts": true}
	  )' | grep "$CORE_HOST"
	)
EOF
	)

	
	local INSERT=$(cat << EOF
	$(getMongoParms) \
	  --eval 'db.getCollection("institutions")
	    .updateOne(
	      { "name":"Kuali" },
	      {
	        \$addToSet: {
	          "validRedirectHosts": "$CORE_HOST"
	        }
	      })'
EOF
	)

   runAndLog "$SELECT"

   if [ -z "$EXISTING_IDP" ] ; then
      runAndLog "$INSERT"
   fi
}


# Call npm to run core
runCoreApp() {
   [ -n "$1" ] &&  [ $1 -gt 0 ] && return 1
   local startcmd="$START_CMD"
   [ -z "$START_CMD" ] && startcmd="npm start"
   echoAndLog "Running ${startcmd}. Not logging to /var/core/startup.log, but you can see output in docker log"
   eval "$START_CMD"   
}


startCore

