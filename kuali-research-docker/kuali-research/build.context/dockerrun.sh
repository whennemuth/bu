
source ../../bash.lib.sh

# Run the docker container
# Arguments: 
#    log_group:    [OPTIONAL] If provided, indicates that logging is to occur against cloudwatch and indicates the cloudwatch log group
#    docker_image: [REQUIRED] Indicates the name of the image to run the container from.
runcontainer() {

  # Set the named parameters as local variables.
  eval "$(parseargs $@)"

  [ ! -d /var/log/newrelic ] && mkdir -p /var/log/newrelic
  
  NEW_RELIC_LICENSE_KEY="$(aws s3 cp s3://kuali-research-ec2-setup/newrelic.license.key -)"

  if [ -n "$log_group" ] ; then
    docker run \
      -d \
      -p 8080:8080 \
      -p 8009:8009 \
      --restart unless-stopped \
      --name kuali-research \
      --log-driver=awslogs \
      --log-opt awslogs-region=us-east-1 \
      --log-opt awslogs-group=$log_group \
      --log-opt awslogs-create-group=true \
      -v /opt/kuali/main/config:/opt/kuali/main/config \
      -v /var/log/tomcat:/opt/tomcat/logs \
      -v /var/log/kuali/printing:/opt/kuali/logs/printing \
      -v /var/log/kuali/javamelody:/var/log/javamelody \
      -v /var/log/kuali/attachments:/opt/tomcat/temp/dev/attachments \
      -v /var/log/newrelic:/var/log/newrelic \
      -e EC2_HOSTNAME=$(echo $HOSTNAME) \
      -e NEW_RELIC_LICENSE_KEY=$NEW_RELIC_LICENSE_KEY \
      -e NEW_RELIC_AGENT_ENABLED="true" \
      -e JAVA_ENV=$NEW_RELIC_ENVIRONMENT \
      -e REMOTE_DEBUG="true" \
      $docker_image
  else
    docker run \
      -d \
      -p 8080:8080 \
      -p 8009:8009 \
      --restart unless-stopped \
      --name kuali-research \
      -v /opt/kuali/main/config:/opt/kuali/main/config \
      -v /var/log/tomcat:/opt/tomcat/logs \
      -v /var/log/kuali/printing:/opt/kuali/logs/printing \
      -v /var/log/kuali/javamelody:/var/log/javamelody \
      -v /var/log/kuali/attachments:/opt/tomcat/temp/dev/attachments \
      -v /var/log/newrelic:/var/log/newrelic \
      -e EC2_HOSTNAME=$(echo $HOSTNAME) \
      -e NEW_RELIC_LICENSE_KEY=$NEW_RELIC_LICENSE_KEY \
      -e NEW_RELIC_AGENT_ENABLED="true" \
      -e JAVA_ENV=$NEW_RELIC_ENVIRONMENT \
      $docker_image
  fi
}

# NOTE: Newrelic has acknowledged an issue whereby "NEW_RELIC_[name]" naming convention
# does not apply when refering to the environment. For this, "NEW_RELIC_ENVIRONMENT"
# will get ignored, and you must use "JAVA_ENV" instead. Newrelic says they will fix this:
# https://support.newrelic.com/tickets/398901

run_container "confirm=true" $@
# run_container "confirm=true" "cloudwatch=true"

