#################################################################################################
#
#  Example builds (this file saved as "bu-centos7-tomcat"):
#    jdk8, tomcat 8: 
#    docker build \
#              -f bu-centos7-tomcat \
#              -t bu-ist/centos7-java-tomcat \
#              --build-arg JAVA_VERSION=8 \
#              --build-arg JAVA_RELEASE=JDK \
#              --build-arg TOMCAT_VERSION=8.5.5 .
#    jre7, tomcat 7: 
#    jre7: docker build \
#              -f bu-centos7-tomcat \
#              -t bu-ist/centos7-java-tomcat \
#              --build-arg JAVA_VERSION=7 \
#              --build-arg JAVA_RELEASE=JRE \
#              --build-arg TOMCAT_VERSION=7 .
#    Or except all defaults:
#          docker build -t bu-ist/centos7-java-tomcat .
#          NOTE: If there is a tar file with the name term 'tomcat' in it, this will be used.
#                Otherwise, the default value for TOMCAT_VERSION will indicate the download from internet.
#
#  Run container detached:
#    docker run -td --name tomcatcontainer bu-ist/centos7-java-tomcat
#    NOTE: you cannot run without -t because all containers exit automatically without a continually
#          running process (/bin/bash is not a continually running background process).
#  Run container interactive:
#    docker run -td --name tomcatcontainer bu-ist/centos7-java-tomcat
#    docker exec -ti tomcatcontainer bash
#    or...
#    docker run -ti --name tomcatcontainer bu-ist/centos7-java-tomcat bash
#    and all subsequent starts...
#    docker start -i bu-ist/centos7-java-tomcat
#
#################################################################################################

FROM centos:7
MAINTAINER Warren H (wrh@bu.edu)

ARG JAVA_VERSION=11
ARG JAVA_RELEASE=JDK
ARG TCAT_VERSION=9.0.41

ENV TOMCAT_VERSION=$TCAT_VERSION
ENV TOMCAT apache-tomcat-$TOMCAT_VERSION
ENV TOMCAT_GZ $TOMCAT.tar.gz
ENV TOMCAT_TGZ_URL https://archive.apache.org/dist/tomcat/tomcat-?/v$TOMCAT_VERSION/bin/$TOMCAT_GZ

LABEL \
    Vendor="Boston University" \
    Department=IST \
    Platform="Centos 7" \
    OpenJDK\ Version="$JAVA_VERSION" \
    OpenJDK\ Release="$JAVA_RELEASE" \
    Tomcat\ Version="$TOMCAT_VERSION"

ENV JAVA_HOME=/usr

# Install Java
RUN \
    echo "Building Boston University centos7-java image"; \
    [ "$JAVA_RELEASE" == "JDK" ] && pkgType="devel" || pkgType="headless"; \
    if [ $JAVA_VERSION -gt 8 ] ; then \
      pkg="java-$JAVA_VERSION-openjdk-$pkgType.x86_64"; \
    else \
      pkg="java-1.$JAVA_VERSION.0-openjdk-$pkgType"; \
    fi; \
    yum update -y && \
    yum install -y "$pkg" && \
    yum clean all

# Install Tomcat
COPY *tomcat*.gz /opt/
RUN \
    cd /opt; \
    GZ="$(ls -la /opt | grep '.*tomcat.*\.tar' | head -n1 | grep -oP '([^\x20]*tomcat.*\.gz)')"; \
    if [ -z "$GZ" ] ; then \
      TOMCAT_MAJOR=${TOMCAT_VERSION:0:1}; \
      TOMCAT_TGZ_URL=$(echo $TOMCAT_TGZ_URL | sed s/?/${TOMCAT_MAJOR}/1); \
      curl -sSL $TOMCAT_TGZ_URL > $TOMCAT_GZ; \
    else \
      TOMCAT_GZ="$GZ"; \
    fi; \
    tar xzf $TOMCAT_GZ && \
    mv -f $TOMCAT tomcat && \
    rm -f $TOMCAT_GZ && \
    rm -f dummy.tomcat.file.gz

EXPOSE 8080

# TODO: Variable substitution works for neither of the 2 forms below. Figure out how to fix this.
# CMD "/opt/tomcat/bin/catalina.sh", "run"
# CMD ["sh", "-c", "/opt/tomcat/bin/catalina.sh", "run"]

CMD ["/opt/tomcat/bin/catalina.sh", "run"]
