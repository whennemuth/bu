###################################################################################
#
# Runs a container with apache and shiboleth processes.
#
# MOUNTS: Need to create 5 directories on the docker host to mount to:
#          mkdir -p /opt/kuali/tls/certs
#          mkdir -p /opt/kuali/tls/private
#          mkdir -p /var/log/httpd
#          mkdir -p /var/log/shibboleth
#          mkdir -p /var/log/shibboleth-www
#       Then copy the cert, key and idp-metadata.xml files in:
#       (assuming they are uploaded to the /tmp dir temporarily)
#          cp -f /tmp/kuali-research-sb_bu_edu_cert.cer \
#             /opt/kuali/tls/certs/kuali-research_bu_edu_cert.cer
#          cp -f /tmp/kuali-research-sb.bu.edu-2048bit-sha256-2016.key \
#             /opt/kuali/tls/private/kuali-research.bu.edu-2048bit-sha256-2016.key
#       (NOTE: The landscape portion ("sp" in this case) of the file names is removed.)
#       Also copy the idp-metadata.xml and pem files to sit beside the cert.
#       (they will be relocated by the startup script)
#          cp -f /tmp/idp-metadata.xml /opt/kuali/tls/certs/
#          cp -f /tmp/sp-cert.pem /opt/kuali/tls/certs/
#          cp -f /tmp/sp-key.pem /opt/kuali/tls/certs/
#
#   NOTE: If -e SSO_ENTITYID is optional and if ommitted, it currently defaults to:
#         "https://shib-test.bu.edu/idp/shibboleth" for sb and ci, and 
#         "https://shib.bu.edu/idp/shibboleth" for qa, stg, and prod. 
#         Also, you can override EC2_HOSTNAME for indicating the landscape by simply
#         using another optional env variable: "-e LANDSCAPE='sp' (or 'ci', 'qa', etc.)
#
###################################################################################


source ../../bash.lib.sh

# Run the docker container
# Arguments:
#    log_group:    [OPTIONAL] If provided, indicates that logging is to occur against cloudwatch and indicates the cloudwatch log group
#    docker_image: [REQUIRED] Indicates the name of the image to run the container from.
runcontainer() {

  # Set the named parameters as local variables.
  eval "$(parseargs $@)"

  if [ -n "$log_group" ] ; then
    docker run \
      -d \
      -p 80:80 \
      -p 443:443 \
      -p 8090:8090 \
      --restart unless-stopped \
      --name apache-shibboleth \
      --log-driver=awslogs \
      --log-opt awslogs-region=us-east-1 \
      --log-opt awslogs-group=$log_group \
      --log-opt awslogs-create-group=true \
      -v /etc/pki/tls/certs:/etc/pki/tls/certs \
      -v /etc/pki/tls/private:/etc/pki/tls/private \
      -v /var/log/httpd:/var/log/httpd \
      -v /var/log/shibboleth:/var/log/shibboleth \
      -v /var/log/shibboleth-www:/var/log/shibboleth-www \
      -e LANDSCAPE=$LANDSCAPE \
      -e EC2_HOSTNAME=$HOSTNAME \
      $docker_image
  else
    docker run \
      -d \
      -p 80:80 \
      -p 443:443 \
      -p 8090:8090 \
      --restart unless-stopped \
      --name apache-shibboleth \
      -v /etc/pki/tls/certs:/etc/pki/tls/certs \
      -v /etc/pki/tls/private:/etc/pki/tls/private \
      -v /var/log/httpd:/var/log/httpd \
      -v /var/log/shibboleth:/var/log/shibboleth \
      -v /var/log/shibboleth-www:/var/log/shibboleth-www \
      -e LANDSCAPE=$LANDSCAPE \
      -e EC2_HOSTNAME=$HOSTNAME \
      $docker_image
  fi
}

run_container "confirm=true" "cloudwatch=true"
