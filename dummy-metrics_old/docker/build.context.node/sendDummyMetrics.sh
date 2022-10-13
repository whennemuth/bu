#!/bin/bash

# This function streams a json file from s3 that indicates dummy metrics to send to cloudwatch.
# The purpose is to simulate higher (or lower) thresholds that would trigger alarms to engage
# a scale-in or scale-out event for auto-scaling activity.
sendDummyMetrics() {
  # local json=$(cat /c/whennemuth/workspaces/ecs_workspace/dummy-metrics/dummy-metrics.json)
  local json=$(aws s3 cp s3://kuali-research-ec2-setup/dummydata/dummy-metrics.json -)
  local namespace=$(echo "$json" | jq -r '.Namespace')
  local instances=$(echo $json | jq -r -c '.Instances[]')
  local evals=()
  while read -r instance; do
    local instanceId=$(echo "$instance" | jq -r '.InstanceId')
    local metrics=$(echo $instance | jq -r -c '.Metrics[]')
    while read -r metric; do
      local name=$(echo "$metric" | jq -r '.MetricName')
      local baseline=$(echo "$metric" | jq -r '.BaselineValue')
      local deviation=$(echo "$metric" | jq -r '.DeviationValue')i
      # Examples to set until to a value offset from current UTC time
      # date +%s --date='+5 minute'
      # date +%s --date='-1 hour'
      local until=$(echo "$metric" | jq -r '.DeviateUntil')
      local value=$baseline
      local now=$(date +%s)
      [ $until -gt $now ] && value=$deviation
      local evalstr="$(cat <<EOF
        aws cloudwatch put-metric-data \
          --region us-east-1 \
          --dimensions Instance=$instanceId \
          --namespace "$namespace" \
          --metric-name $name \
          --value $value
EOF
      )"
      evals=("${evals[@]}" "$(echo "$evalstr" | sed -E 's/[[:space:]]+/ /g' | xargs)")
    done <<< "$metrics"
  done <<< "$instances"

  for cmd in "${evals[@]}" ; do
    echo "$cmd"
    eval "$cmd"
  done
}

sendDummyMetrics
