#!/bin/bash
# This script will run catalina.sh, but only after it does some custom tasks first.
# Namely, copying war file from mounted directory to webapps dir so container can start with last built war running.

# Copy war file from mounted directory to non-mounted webapps dir.
if [ -f /usr/local/tomcat/webapp_mount/kc.war ] ; then
   cp -f /usr/local/tomcat/webapp_mount/kc.war /usr/local/tomcat/webapps/kc.war
fi

sh /usr/local/tomcat/bin/catalina.sh "run"