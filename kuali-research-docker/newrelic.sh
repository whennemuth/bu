#!/bin/bash

# Use the aws systems manager to issue a command to an ec2 instance.
# To deal with double and single quotes issues, the command is base64 encoded going out and decoded on arrival, then executed.
# This script has functionality prepending the command to reacquire itself from github for the app host.
sendCommand() {
  local parameters="$1"
  [ -z "$parameters" ] && "MISSING PARAMETER(S): bash command parameters are required." && exit 1

  local comment="${2:-'No comment provided'}"
  local task="$(echo $task | cut -d'-' -f2)"

  local encoded=$(cat <<EOF | base64 -w 0
    curl \
      -H "Authorization: token $githubApiToken" \
      -L https://api.github.com/repos/bu-ist/kuali-research-docker/contents/newrelic.sh \
      | jq '.content' \
      | sed 's/\\\n//g' \
      | sed 's/\"//g' \
      | base64 --decode | sh -s $parameters  2>&1 > /tmp/nr-$task-output
EOF
)

  aws ssm send-command \
    --instance-ids "$ec2InstanceId" \
    --document-name "AWS-RunShellScript" \
    --comment "$comment" \
    --parameters commands="echo $encoded | base64 --decode | sh 2>&1 > /tmp/nr-$task-cmd-output" \
    --output text \
    --query "Command.CommandId"
}

# Install the infrastructure agent.
# SEE: https://docs.newrelic.com/docs/infrastructure/install-configure-manage-infrastructure/linux-installation/install-infrastructure-linux-using-package-manager
installInfrastructureAgent() {
  if [ "$send" ] ; then
    sendCommand \
      "install $@" \
      "Installing the infrastructure agent on $ec2IntanceId"
  else
    local elNum='6'
    # If the version id does not include a year (4 digits) then it's the amazon linux 2 ami
    [ "$amazon2" ] && elNum='7'
    curl \
      -o /etc/yum.repos.d/newrelic-infra.repo \
      https://download.newrelic.com/infrastructure_agent/linux/yum/el/$elNum/x86_64/newrelic-infra.repo
    yum -q makecache -y --disablerepo='*' --enablerepo='newrelic-infra'
    yum install newrelic-infra -y
  fi
}

# Acquire the newrelic-infra.yml from s3 and drop it on the app host where newrelic can "see" it.
# Requires a restart of the newrelic service.
# SEE: https://docs.newrelic.com/docs/infrastructure/install-configure-infrastructure/configuration/configure-infrastructure-agent
configure() {
  if [ "$send" ] ; then
    sendCommand \
      "config $@" \
      "Fetching the newrelic-infra.yml file on $ec2Instance"
  else
    aws s3 cp s3://kuali-research-ec2-setup/newrelic/newrelic-infra.yml /etc/
    if [ "$amazon2" ] ; then
      systemctl restart newrelic-infra
    else
      initctl restart newrelic-infra
    fi
  fi
}

# Start or stop the newrelic infrastructure agent. If stopping, modify the startup policy so it does not come back upon boot up.
enable() {
  if [ "$send" ] ; then
    sendCommand \
      "enable $@" \
      "${1}ing the newrelic infrastructrue agent on $ec2Instance"
  else
    case "$1" in
      start) 
        echo "Starting new relic infrastructure agent..."
        sed -i "s/#start on/start on/" /etc/init/newrelic-infra.conf
        ;;
      stop) 
        echo "Stopping new relic infrastructure agent..."
        sed -i "s/start on/#start on/" /etc/init/newrelic-infra.conf
        ;;
      *) 
        echo "Invalid/missing operation: $1"
        exit 1
        ;;
    esac

    if [ "$amazon2" ] ; then
      systemctl $1 newrelic-infra
    else
      initctl $1 newrelic-infra
    fi
  fi
}

# Enable or disable log forwarding for tomcat and apache to newrelic logs
logging() {
  if [ "$send" ] ; then
    sendCommand \
      "logging $@" \
      "Setting newrelic log forwarding to $1 for $ec2Instance"
  else
    case "${1,,}" in
      on|true|start)
        aws s3 cp \
          s3://kuali-research-ec2-setup/newrelic/logs.yml \
          /etc/newrelic-infra/logging.d/
        ;;
      off|false|stop)
        rm -f /etc/newrelic-infra/logging.d/logs.yml
        ;;
      *)
        echo "Invalid/missing parameter: $1"
        exit 1
        ;;
    esac
    if [ "$amazon2" ] ; then
      systemctl restart newrelic-infra
    else
      initctl restart newrelic-infra
    fi
  fi
}

# Parse parameters.
# Example call: 
#   sh newrelic.sh send-enable i-090d188ea237c8bcf myGithubApiToken on
task=$1
shift
if [ "${task:0:4}" == "send" ] ; then
  send="true"
  ec2InstanceId="$1"
  if [ -z "$ec2InstanceId" ] ; then
    echo "MISSING PARAMETER: ec2 instance ID"
    exit 1
  else
    shift
    githubApiToken="$1"
    if [ -z "$ec2InstanceId" ] ; then
      echo "MISSING PARAMETER: github api token"
      exit 1
    else
      shift
    fi
  fi
fi
[ -z "$(cat /etc/*-release | grep VERSION_ID | grep -P '\d{4}')" ] && amazon2="true"

# Call the function
case "$task" in
  send)
    sendCommand $@ ;;
  *install)
    installInfrastructureAgent $@ ;; 
  *config)
    configure $@ ;;
  *enable)
    enable $@ ;;
  *logging)
    logging $@ ;;
esac

