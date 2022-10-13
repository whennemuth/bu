# Set log4j bootstrap jars here. NOTE setting CLASSPATH prior to setenv.sh being run is useless as catalina.sh unsets it.

# Increasing the java heap settings for tomcat as kuali can be a hog.
# Careful not to exceed the total memory of the AWS instance (currently is t2.medium, 4GB)
CATALINA_OPTS="$CATALINA_OPTS -Xms2048m"
CATALINA_OPTS="$CATALINA_OPTS -Xmx4096m"
CATALINA_OPTS="$CATALINA_OPTS -XX:MaxPermSize=1024m"

# Set log4j bootstrap jars here.
# CLASSPATH=$CATALINA_HOME/log4j2/lib/*:$CATALINA_HOME/log4j2/conf:$CATALINA_HOME/lib/*:$CATALINA_HOME/webapps/kc/WEB-INF/lib/*
CLASSPATH=$CATALINA_HOME/log4j2/lib/*:$CATALINA_HOME/log4j2/conf

# Set log4j LoggingManager, configuration file and kc-config.xml location system variables here.
JAVA_OPTS="-javaagent:/opt/newrelic/newrelic.jar"
if [ -f $CATALINA_HOME/lib/spring-instrument.jar ] ; then
  echo "setenv.sh: Tomcat version: ${TOMCAT_VERSION}"
  if [ "${TOMCAT_VERSION:0:1}" -ge 9 ] ; then
    JAVA_OPTS="$JAVA_OPTS -javaagent:$CATALINA_HOME/lib/spring-instrument.jar"
  else
    rm -f $CATALINA_HOME/lib/spring-instrument.jar
  fi
else
  echo "setenv.sh: spring-instrument.jar not found."
fi
JAVA_OPTS="$JAVA_OPTS -Dlog4j.configurationFile=$CATALINA_HOME/log4j2/conf/log4j2-tomcat.xml"
JAVA_OPTS="$JAVA_OPTS -Dalt.config.location=/opt/kuali/main/config/kc-config.xml"

if [ "${REMOTE_DEBUG,,}" == 'true' ] ; then
  # https://dzone.com/articles/remote-debugging-java-applications-with-jdwp
  echo "setenv.sh: The JVM will start with remote debugging on port 8787"
  JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=*:8787,server=y,suspend=n"
  # JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=0.0.0.0:8787,server=y,suspend=n"
  JAVA_OPTS="$JAVA_OPTS -noverify"
fi

if [ -f "/opt/kuali/certs/sslcert.sh" ] ; then
  if [ -z "$CERT_FILE" ] ; then
    # If no explicit path provided for the certificate, it should exist in the directory mount along with the sslcert.sh file.
    CERT_FILE="$(find /opt/kuali/certs -iname *.crt -type f 2> /dev/null | head -1)"
  fi
  if [ -f "$CERT_FILE" ] ; then
    sh /opt/kuali/certs/sslcert.sh import-cert CERT_FILE=$CERT_FILE
  fi
fi

LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"
