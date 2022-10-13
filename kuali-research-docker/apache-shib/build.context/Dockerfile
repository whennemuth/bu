#################################################################################################
#
# This dockerfile for the image for the apache server involved with the kuali-research application.
#
# NOTE: A startup script is used (start-apache.sh).
# The last command of this script starts httpd. Everything before that modifies directives and xml 
# in httpd configuration files. This is being done in start-apache.sh instead of here during image 
# creation because these configuration changes are specific to the landscape (sandbox, qa, ci, sta
# ging, etc.) and we don't want multiple images, one for each landscape. Instead, start-apache.sh
# will find the content it needs to make configuration changes in a mounted directory where landsc
# ape specific data belongs, and the changes are made when the container is started.

#################################################################################################

FROM centos:7
MAINTAINER Warren H (wrh@bu.edu)

ENV SHIB2_FIND="<\/Attributes>"
ENV SHIB2_INSERT_BEFORE="    <Attribute name=\"urn:oid:1.3.6.1.4.1.9902.2.1.9\" id=\"principalNameID\"/>"
ENV SHIB2_XML="/etc/shibboleth/attribute-map.xml"

LABEL \
    Vendor="Boston University" \
    Department=IST \
    Platform="Centos 7" \
    Apache\ Version="$APACHE_VERSION"

USER root

# Install Apache (and mod_ssl and net-tools for route command)
RUN \
    echo "Building httpd.x86_64"; \
    yum update -y && \
    yum install -y httpd.x86_64 && \
    yum install -y mod_ssl && \
    yum install -y net-tools

# Install shibboleth
# NOTE: Issue of mirror timeouts while downloading packages for yum install of shibboleth has cropped up
#       To avoid this, change repo location to downloadcontent.opensuse.org.
#       SEE: http://shibboleth.1660669.n2.nabble.com/yum-install-failed-td7637937.html
RUN \
   curl -o \
      /etc/yum.repos.d/shibboleth.repo \
      http://download.opensuse.org/repositories/security://shibboleth/CentOS_7/security:shibboleth.repo && \
   sed -i "s/download.opensuse.org/downloadcontent.opensuse.org/" /etc/yum.repos.d/shibboleth.repo && \
   yum install -y shibboleth && \
   yum clean all

EXPOSE 80
EXPOSE 443

# Add a shibboleth attribute to the attributes element within attribute-map.xml
RUN sed -i "/${SHIB2_FIND}/i ${SHIB2_INSERT_BEFORE}" ${SHIB2_XML}

# Make directories to put html pages for apache to serve up for basic access testing
RUN mkdir -p /var/www/html/secure && mkdir -p /var/www/html/server && mkdir -p /etc/snmp

# Change the timezone from UTC to EDT
RUN mv /etc/localtime /etc/localtime.orig && \
    ln -s /usr/share/zoneinfo/America/New_York /etc/localtime

# Add the extra configuration files with docker file extensions, some with docker file extensions.
# Some of these will be modified to conform to landscape by start-apache.sh and copied output with the same file name without the docker file extension
COPY httpd.override.conf /etc/httpd/conf.d/
COPY shibboleth2.xml /etc/shibboleth/shibboleth2.xml.docker
COPY secure.html /var/www/html/secure/index.html
COPY server.html /var/www/html/server/index.html
COPY snmpd.conf /etc/snmp/

# Copy the httpd/shibboleth startup script to the top directory
COPY start-apache.sh /start-apache.sh
RUN chmod -v +x /start-apache.sh && \
    chmod -v +x /etc/shibboleth/shibd-*

# Making these the last COPY instructions due to their frequently being the reason for a rebuild of the image (makes build faster if it's the last layer - reuses the most cached layers)
COPY kc.conf /etc/httpd/conf.d/kc.conf.docker
COPY ssl.conf /etc/httpd/conf.d/ssl.conf.docker

CMD ["/start-apache.sh"]
