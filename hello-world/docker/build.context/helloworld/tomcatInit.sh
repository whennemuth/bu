#!/bin/bash
# This script will run catalina.sh, but only after it does some custom tasks first.
# Namely, copying war file from mounted directory to webapps dir so containter can start with last built war running.

if [ -f /usr/local/tomcat/webapp_mount/hello-world-0.0.1-SNAPSHOT.war ] ; then
   cp -f /usr/local/tomcat/webapp_mount/hello-world-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/hello-world.war
fi

sh /usr/local/tomcat/bin/catalina.sh "run"