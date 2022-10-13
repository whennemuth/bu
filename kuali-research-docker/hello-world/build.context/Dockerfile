FROM bu-ist/centos7-java-tomcat
MAINTAINER Warren

# ENV JAVA_OPTS="-Xmx8192m"
ENV JAVA_OPTS="-Xmx1024m"
ENV TOMCAT_LOCATION="/var/lib/tomcat"
ENV TARGET_WAR="${TOMCAT_LOCATION}/webapps/hello-world.war"

ARG SOURCE_WAR

USER root

# Allow write access for jenkins to save war files
RUN chmod a+w -R $TOMCAT_LOCATION/webapps

# Add in the hello-world war file into the image from its jenkins build location.
# NOTE: Cannot use RUN curl... Using ADD instead because it runs outside on the docker host and can reach the jenkins localhost port 8080.
ADD $SOURCE_WAR $TARGET_WAR

# Can use the following if SOURCE_WAR is a URL using port 80 or some way to map other ports in the same instruction with && is used.
# This would be necessary if the SOURCE_WAR required authentication, which is not supported by the ADD instruction.
# RUN curl -sSL $SOURCE_WAR > $TARGET_WAR

CMD ["sh", "/usr/libexec/tomcat/server", "start"]