#!/bin/bash

LNDSCP=""

# 1) Copy in shibboleth secure data from their mounted locations
cp -f /etc/pki/tls/certs/idp-metadata.xml /etc/shibboleth/
cp -f /etc/pki/tls/certs/sp-cert.pem /etc/shibboleth/
cp -f /etc/pki/tls/certs/sp-key.pem /etc/shibboleth/

# 2a) Find out what landscape we are running in via the HOSTNAME env variable of the EC2 instance
EC2_HOSTNAME="$(echo $EC2_HOSTNAME | tr '[A-Z]' '[a-z]')"
case "$EC2_HOSTNAME" in
   "ip-10-57-237-84") ;& "ip-10-57-237-85")
      LNDSCP="sb";;
   "ip-10-57-237-36") ;& "ip-10-57-237-37")
      LNDSCP="ci";;
   "ip-10-57-236-244")
      LNDSCP="qa";;
   "ip-10-57-236-68") ;& "ip-10-57-236-100")
      LNDSCP="stg";;
   "ip-10-57-242-100") ;& "ip-10-57-243-100")
      LNDSCP="prod";;
esac

# 2b) Alternatively the landscape could be provided directly and would override $EC2_HOSTNAME
LANDSCAPE="$(echo $LANDSCAPE | tr '[A-Z]' '[a-z]')"
case "$LANDSCAPE" in
   "sb") ;& "sandbox")
      LNDSCP="sb";;
   "ci")
      LNDSCP="ci";;
   "qa")
      LNDSCP="qa";;
   "stg") ;& "stage") ;& "staging")
      LNDSCP="stg";;
   "prod") ;& "production")
      LNDSCP="prod";;
esac

# 3) Validate environment variables
if [ -z "${LNDSCP}" ] ; then
   if [ -z '${EC2_HOSTNAME}' ] ; then
      echo 'HOSTNAME environment variable on the EC2 host was empty or not provided';
   else
      echo 'Unrecognized HOSTNAME: ${EC2_HOSTNAME}';
   fi
   if [ -z '${LANDSCAPE}' ] ; then
      echo 'LANDSCAPE environment variable not provided';
   else
      echo 'Unrecognized LANDSCAPE: ${LANDSCAPE}';
   fi
   exit 1;
fi

# 4) Assign a default entity id for shibboleth idp metadata
if [ -z "${SSO_ENTITYID}" ] ; then
   if [ $LNDSCP = 'sb' ] || [ $LNDSCP = 'ci' ] ; then
      SSO_ENTITYID='https://shib-test.bu.edu/idp/shibboleth';
   else
      SSO_ENTITYID='https://shib.bu.edu/idp/shibboleth';
   fi   
fi

# 5) Keys and certs may have been saved into the mounted directories with the landscape included in the file name(s)
# Remove the landscape portion of the filename(s) because the ssl.conf file references these files, assuming generic names.
for f in /etc/pki/tls/certs/kuali*-${LNDSCP}*.cer ; do mv $f "$(echo $f | sed "s/\-${LNDSCP}//")"; done
for f in /etc/pki/tls/private/kuali*-${LNDSCP}*.key ; do mv $f "$(echo $f | sed "s/\-${LNDSCP}//")"; done

# 5) Create/refresh configuration files from their ".docker" file content
cp /etc/httpd/conf.d/kc.conf.docker /etc/httpd/conf.d/kc.conf
cp /etc/httpd/conf.d/ssl.conf.docker /etc/httpd/conf.d/ssl.conf
cp /etc/shibboleth/shibboleth2.xml.docker /etc/shibboleth/shibboleth2.xml

# 6) Use stream editor to replace portions of conf files to reflect the landscape
#    a) grepping the output of route for the second column of a row starting with 0.0.0.0 will return the gateway ip for the container
#    This is the ip address for the docker0 network bridge outside the container. This ip is obtained to set the value for remote_ip in kc.conf
GATEWAY="$(route -n | grep -Po "(?<=^0\.0\.0\.0)\x20+[\d\.]+" | tr -d "[:blank:]")"
sed -i -r "s/\\$\{GATEWAY\}/${GATEWAY}/g" /etc/httpd/conf.d/kc.conf
sed -i -r "s/\\$\{GATEWAY\}/${GATEWAY}/g" /etc/httpd/conf.d/ssl.conf
#    b) Replace all of the landscape placeholders with the landscape value
if [ $LNDSCP = "prod" ] ; then
   sed -i -r "s/\-\\$\{LANDSCAPE\}//g" /etc/httpd/conf.d/kc.conf
   sed -i -r "s/\-\\$\{LANDSCAPE\}//g" /etc/shibboleth/shibboleth2.xml
   sed -i -r "s/\-\\$\{LANDSCAPE\}//g" /etc/httpd/conf.d/ssl.conf
   sed -i -r "s/\-\\$\{LANDSCAPE\}//g" /etc/httpd/conf.d/httpd.override.conf
   sed -i -r "s/\-\\$\{LANDSCAPE\}//g" /var/www/html/secure/index.html
else 
   sed -i -r "s/\\$\{LANDSCAPE\}/${LNDSCP}/g" /etc/httpd/conf.d/kc.conf
   sed -i -r "s/\\$\{LANDSCAPE\}/${LNDSCP}/g" /etc/shibboleth/shibboleth2.xml
   sed -i -r "s/\\$\{LANDSCAPE\}/${LNDSCP}/g" /etc/httpd/conf.d/ssl.conf
   sed -i -r "s/\\$\{LANDSCAPE\}/${LNDSCP}/g" /etc/httpd/conf.d/httpd.override.conf
   sed -i -r "s/\\$\{LANDSCAPE\}/${LNDSCP}/g" /var/www/html/secure/index.html
fi
#    c) Replace the shibboleth single-signon placeholder with the corresponding environment variable value.
sed -i -r "s/\\$\{SSO_ENTITYID\}/$(echo "${SSO_ENTITYID}" | sed -r 's/([^a-zA-Z_])/\\\1/g')/g" /etc/shibboleth/shibboleth2.xml
#    d) Replace instances of EC2_HOSTNAME variable
sed -i -r "s/\\$\{EC2_HOSTNAME\}/${EC2_HOSTNAME}/g" /var/www/html/secure/index.html

# 7) Start shibboleth
/etc/shibboleth/shibd-redhat start

# 8) Start httpd (NOTE: Apache does not like PID files pre-existing, so kill them first)
rm -rf /run/httpd/* /tmp/httpd*
# NOTE: You can make changes to configuration files and get apache to reload them without stopping with:
# apachectl -k graceful or httpd -k graceful
# Since the Apache will advise its threads to exit when idle (not kill), the container will remain running.
# However, the following will restart apache altogether, so you might as well restart the container:
# apachectl -k restart or httpd -k restart
exec /usr/sbin/apachectl -DFOREGROUND
