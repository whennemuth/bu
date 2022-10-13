# Increasing the java heap settings for tomcat as kuali can be a hog.
# Careful not to exceed the total memory of the AWS instance (currently is t2.medium, 4GB)
export CATALINA_OPTS="$CATALINA_OPTS -Xms512m"
export CATALINA_OPTS="$CATALINA_OPTS -Xmx3072m"
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxPermSize=256m"