################################################################################################
#
# Docker build for the Boston University kc-research web application.
#
#  Tomcat directories/symlinks are as follows:
#  bin
#  conf -> /etc/tomcat
#  lib -> /usr/share/java/tomcat
#  logs -> /var/log/tomcat
#  temp -> /var/cache/tomcat/temp
#  webapps -> /var/lib/tomcat/webapps
#  work -> /var/cache/tomcat/work
# 
# To prepare the docker host, create the following directories:
#    sudo mkdir -p /var/log/tomcat
#    sudo mkdir -p /opt/kuali/main/config
#    sudo mkdir -p /var/log/kuali/printing
#    sudo mkdir -p /var/log/kuali/javamelody
#    sudo mkdir -p /var/log/kuali/attachments
#    sudo cp kc-config.xml /opt/kuali/main/config/
#
#################################################################################################

ARG JAVA_VERSION=11
ARG TOMCAT_VERSION=9.0.41
ARG REPO_URI=730096353738.dkr.ecr.us-east-1.amazonaws.com/centos7-java-tomcat
FROM ${REPO_URI}:java${JAVA_VERSION}-tomcat${TOMCAT_VERSION}
MAINTAINER Warren H (wrh@bu.edu)

USER root

ENV TOMCAT_HOME /opt/tomcat
ENV CATALINA_HOME="$TOMCAT_HOME"
ENV TOMCAT_VERSION=${TOMCAT_VERSION}

# Do the following:
# 1) Beef up the memory to get the most it can for a m4.larg (8GB)
#    NOTE: This is best done by including a setenv.sh file in the build context with these settings.
#          Alternatively you could set the environment variables by uncommenting them below, but CLASSPATH would not be used as 
#          catalina.sh unsets it before running setenv.sh.
# 2) Make tomcat start up with a system variable set (java: System.getProperty()) to specify the location of kc-config.xml
# 
# NOTE: JAVA_OPTS can also be set in /etc/tomcat/tomcat.conf
#
# ENV CATALINA_OPTS="-Xmx4096m -Xms2048m -XX:MaxPermSize=1024m"
# ENV JAVA_OPTS="-Dalt.config.location=/opt/kuali/main/config/kc-config.xml"
# ENV JAVA_OPTS="$JAVA_OPTS -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"
# ENV JAVA_OPTS="$JAVA_OPTS -Dlog4j.configurationFile=$CATALINA_HOME/conf/log4j2-tomcat.xml"
# ENV CLASSPATH="$CATALINA_HOME/lib/*:$CATALINA_HOME/conf"

ARG SOURCE_WAR="kc.war"
ARG TARGET_WAR="$CATALINA_HOME/webapps/kc.war"
ARG SPRING_INSTRUMENT_JAR="spring-instrument.jar"

RUN \
   mkdir -p /opt/kuali/main/config && \
   mkdir -p /opt/kuali/logs/printing/logs && \
   mkdir -p /opt/kuali/logs/printing/xml && \
   mkdir -p /opt/tomcat/temp/dev/attachments && \
   mkdir -p /var/log/javamelody

# Copy the setenv.sh file into the the catalina bin directory to increase the java heap size.
COPY setenv.sh $CATALINA_HOME/bin

# Copy the oracle jdbc driver to the tomcat lib directory
COPY ojdbc10.jar $CATALINA_HOME/lib

# Copy the mysql driver to the tomcat lib directory
# COPY mysql-connector-java-5.1.38-bin.jar $CATALINA_HOME/lib
COPY eclipselink-2.7.5.jar $CATALINA_HOME/lib

# Addresses issue: https://kuali-research.zendesk.com/hc/en-us/requests/5779, until kualico fixes it.
COPY context.xml $CATALINA_HOME/conf/context.xml

# Copy KC/SAP wsdl
COPY SI_KCRM_PROCESS_OUTBOUND_v9.wsdl /opt/kuali/main/SI_KCRM_PROCESS_OUTBOUND_v9.wsdl

# Copy the log4j2 configuration xml file into the tomcat conf directory (System property set in setenv.sh to point to it).
COPY log4j2-tomcat.xml $CATALINA_HOME/conf

# Replace tomcat-coyote.jar with one that has patch to enable Proxy Protocol
# RUN rm -f $CATALINA_HOME/lib/tomcat-coyote.jar
# COPY tomcat-coyote-pp.jar $CATALINA_HOME/lib
# or...
# Replace java.util.logging with log4j. NOTE: Causes class path errors for tomcat 8x
# COPY tomcat-juli-adapters.jar $CATALINA_HOME/lib/tomcat-juli-adapters.jar
# COPY tomcat-juli.jar $CATALINA_HOME/bin/tomcat-juli.jar
# COPY log4j.properties $CATALINA_HOME/lib/log4j.properties
# RUN rm -f /etc/tomcat/logging.properties

# Disable tomcat authentication on the AJP connector within the Catalina service by adding a 'tomcatAuthentication="false"' attribute to it.
# RUN sed -i -r "s/(protocol=[\"']AJP\/1\.3[\"'])/\1 tomcatAuthentication=\"false\" proxyProtocol=\"optional\"/g" $CATALINA_HOME/conf/server.xml
# Overwrite the server.xml in the catalina conf directory with one that has AJP changes
# COPY server.xml $CATALINA_HOME/conf/server.xml
#
# Disable tomcat authentication on the AJP connector within the Catalina service by adding a 'tomcatAuthentication="false"' attribute to it.
RUN sed -i -r "s/(protocol=[\"']AJP\/1\.3[\"'])/\1 tomcatAuthentication=\"false\"/g" $CATALINA_HOME/conf/server.xml

# Change the timezone from UTC to EDT
RUN rm -f /etc/localtime && \
    ln -s /usr/share/zoneinfo/US/Eastern /etc/localtime

# This image does not enable systemd, therefore the tomcat logging 
# 8080 is already exposed in parent image. Expose port 8009 as well.
EXPOSE 8009

# METHOD 1: Use the curl command to get kc.war from jenkins via localhost of the docker host. 
# You can curl against "localhost" if you use the default port 80 because that is mapped to the default gateway.
# Using other ports, you cannot curl against "localhost" because that does not go out over the default gateway, which is the docker0
# network bridge that the default gateway (localhost) of the docker host shares. To get the ip address of the network bridge
# from inside the image (as would be the case from inside a container), you must grep for it from the route command.
# Alternatively you could use the ADD instruction as it works outside on the docker host network stack.
# RUN \
#   yum install -y net-tools && \
#   GATEWAY="$(route -n | grep -Po "(?<=^0\.0\.0\.0)\x20+[\d\.]+" | tr -d "[:blank:]")" && \
#   URL=$(echo "${SOURCE_WAR}" | sed "s/localhost/${GATEWAY}/") && \
#   curl -X POST ${URL} > ${TARGET_WAR}

# METHOD 2: The kc.war is in the docker build context directory - no need to obtain it in from jenkins
# COPY $SOURCE_WAR $TARGET_WAR

# METHOD 3: Add in the kc war file into the image from its jenkins build url.
# NOTE: Cannot use ADD against a jenkins url if jenkins has security enabled (you will get a 403 forbidden reply).
ADD $SOURCE_WAR $TARGET_WAR

# Addresses issue: https://kuali-research.zendesk.com/hc/en-us/requests/5779, until kualico fixes it.
RUN coeusImplJar="$(jar tvf $TARGET_WAR | grep -iPo '.*/(coeus\-impl\-.*\.jar)' | sed 's/\//\n/g' | grep coeus-impl)" && \
    sed -i -r "s/\\$\\{coeusImplJar\\}/${coeusImplJar}/g" $CATALINA_HOME/conf/context.xml

# catalina.sh unsets CLASSPATH, then rebuilds in part using instructions in setenv.sh. So, you cannot simply set CLASSPATH before you start tomcat.
# Extract the log4j jars from the war file and put them into the tomcat lib directory so they can appear in the bootstrap classpath.
# Need to also supply jackson libraries as log4j2-tomcat.xml configuration has a <JsonLayout for one of its appenders.
# woodstox-core if for xml-based appenders in case those are used.
RUN mkdir -p $CATALINA_HOME/log4j2/lib && \
    mkdir -p $CATALINA_HOME/log4j2/conf && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'log4j-core') && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'log4j-api') && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'log4j-jul') && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'log4j-appserver') && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'woodstox-core') && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'stax') && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'jackson-core') && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'jackson-databind') && \
    jar -xf $TARGET_WAR $(jar -tf $TARGET_WAR | grep 'jackson-annotations') && \
    cp WEB-INF/lib/* $CATALINA_HOME/log4j2/lib/ && \
    cp $CATALINA_HOME/conf/log4j2-tomcat.xml $CATALINA_HOME/log4j2/conf/ && \
    rm -rf WEB-INF

# Install newrelic
RUN yum install -y unzip && \
    cd /opt/ && \
    curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip && \
    unzip newrelic-java.zip && \
    rm -f newrelic-java.zip && \
    mkdir -p /var/log/newrelic

COPY newrelic.yml /opt/newrelic/newrelic.yml

# The git readme file says you don't need to do this for tomcat 9.x and above, but still getting ClassNotFoundException from KcConfigVerifier 
COPY $SPRING_INSTRUMENT_JAR $CATALINA_HOME/lib/spring-instrument.jar

# Start tomcat by running the file that systemd would if it were enabled.
CMD ["/opt/tomcat/bin/catalina.sh", "run"]
