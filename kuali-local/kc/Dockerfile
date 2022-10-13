ARG JAVA_VERSION=11
ARG TOMCAT_VERSION=9.0.41
FROM wrh1/centos7-java-tomcat:java${JAVA_VERSION}-tomcat${TOMCAT_VERSION}

USER root

ENV TOMCAT_HOME /opt/tomcat
ENV CATALINA_HOME="$TOMCAT_HOME"
ENV TOMCAT_VERSION=${TOMCAT_VERSION}
ENV DOCLIB=/opt/kuali/webapp/WEB-INF/lib
ENV LOG4JLIB=${CATALINA_HOME}/log4j2/lib
ENV POM=/opt/kuali/pom.xml
ENV LOGJ2_LOCALHOST_LEVEL=info
ENV LOGJ2_CATALINA_LEVEL=info

RUN \
   mkdir -p /opt/kuali/main/config && \
   mkdir -p /opt/kuali/logs/printing/logs && \
   mkdir -p /opt/kuali/logs/printing/xml && \
   mkdir -p /opt/kuali/workdir && \
   mkdir -p ${CATALINA_HOME}/temp/dev/attachments && \
   mkdir -p ${CATALINA_HOME}/log4j2/lib && \
   mkdir -p ${CATALINA_HOME}/log4j2/conf && \
   mkdir -p ${CATALINA_HOME}/conf/Catalina/localhost && \
   mkdir -p /var/log/javamelody

RUN \
  yum install -y epel-release && \
  yum update -y && \
  yum install -y jq

RUN \
  mkdir -p /usr/share/maven && \
  curl -fsSL http://apache.osuosl.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz \
  | tar -xzC /usr/share/maven --strip-components=1 && \
  ln -s /usr/share/maven/bin/mvn /usr/bin/mvn && \
  echo "MAVEN_HOME=/usr/share/maven" >> /etc/bashrc && \
  mvn --version

COPY setenv.sh $CATALINA_HOME/bin
COPY ojdbc*.jar $CATALINA_HOME/lib
COPY eclipselink-*.jar $CATALINA_HOME/lib
# COPY context.xml $CATALINA_HOME/conf/context.xml
COPY kc.xml ${CATALINA_HOME}/conf/Catalina/localhost/kc.xml
COPY SI_KCRM_PROCESS_OUTBOUND_v9.wsdl /opt/kuali/main/SI_KCRM_PROCESS_OUTBOUND_v9.wsdl
COPY log4j2-tomcat.xml $CATALINA_HOME/conf
COPY log4j2-tomcat.xml $CATALINA_HOME/log4j2/conf
COPY spring*instrument*.jar $CATALINA_HOME/lib/spring-instrument.jar

# Disable tomcat authentication on the AJP connector within the Catalina service by adding a 'tomcatAuthentication="false"' attribute to it.
RUN sed -i -r "s/(protocol=[\"']AJP\/1\.3[\"'])/\1 tomcatAuthentication=\"false\"/g" $CATALINA_HOME/conf/server.xml

# Change the timezone from UTC to EDT
RUN rm -f /etc/localtime && \
    ln -s /usr/share/zoneinfo/US/Eastern /etc/localtime

CMD ["/opt/tomcat/bin/catalina.sh", "run"]