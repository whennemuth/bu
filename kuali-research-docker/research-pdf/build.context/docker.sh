#!/bin/bash

source ../../bash.lib.sh

# Not needed for standard jenkins deployment, but if you're debugging and need a local mongo database,
# using docker-compose to network the pdf and mongo containers together is more convenient.
installDockerCompose() {
  if [ -z "$(docker-compose --version 2> /dev/null)" ] ; then
    curl -L https://github.com/docker/compose/releases/download/1.25.4/docker-compose-$(uname -s)-$(uname -m) \
        -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
  fi
}

# Goto s3 and use the environment variables for the cor-main deployment.
# The mongodb user and password values will be the same and the service to service secret will also be the same.
setVariables() {
  local environment=$1
  BASE_URL="https://kuali-research-${environment}.bu.edu"
  [ "$environment" == 'prod' ] && baseurl='https://kuali-research.bu.edu'
  local envarFile=s3://kuali-research-ec2-setup/$environment/core/environment.variables.s3.env
  while read -r line ; do
    local pair=($(echo $line | tr "=" "\n"))
    local name="${pair[0]^^}"
    local val="${pair[1]}"
    case $name in
      MONGO_PASS)
        encoded_password="$(printf "$val" | xxd -plain | tr -d '\n' | sed 's/\(..\)/%\1/g')" ;;
      SERVICE_SECRET_2)
        SERVICE_SECRET_2="$val" ;;
      MONGO_URI)
        # Cherry pick pieces of the longer sharded URI string for content to make the cluster name for the newer mongo+srv style URI.
        local matches=($(printf "$val" | grep -Po '[^\-\.]+\.mongodb\.net'))
        local cluster="$environment-cluster-${matches[0]}" ;;
    esac
    MONGO_URI="mongodb+srv://admin:${encoded_password}@${cluster}/test?retryWrites=true&w=majority"
  done <<< "$(aws s3 cp $envarFile -)"
}


run() {
  local environment=$1
  local image=$2
  local replace=$3

  setVariables $environment

  cat <<EOF > $(pwd)/docker.pdf.run.sh
#!/bin/bash

source ../../bash.lib.sh

replace=$3

echo "Executing: sh docker.sh run $environment $image $replace ..."
if containerExists 'research-pdf' ; then
  echo 'Removing existing research-pdf container...'
  docker rm -f research-pdf;
fi
if [ "\${replace,,}" == "true" ] ; then
  echo 'Removing existing image $image'
  docker rmi  $image > /dev/null
fi
if [ -n "$( echo $image | grep 'amazonaws' | grep '\.ecr\.')" ] ; then
  # This is an ecr image
  logIntoRegistry
fi

echo 'Running new research-pdf container...'
docker run \\
  -d \\
  --restart unless-stopped \\
  --name research-pdf \\
  -p 3006:3006 \\
  -e AUTH_ENABLED=true \\
  -e AUTH_BASEURL=$BASE_URL \\
  -e AUTH_SERVICE2SERVICE_SECRETS=${SERVICE_SECRET_2} \\
  -e MONGO_ENABLED=true \\
  -e SPRING_DATA_MONGODB_URI='${MONGO_URI}' \\
  -e AWS_REGION='us-east-1' \\
  -e AWS_S3_ENABLED=true \\
  -e AWS_S3_BUCKET=kuali-research-pdf-${environment} \\
  $image
EOF
}


task=$1
shift

case ${task,,} in
  install)
    installDockerCompose ;;
  docker-compose)
    docker-compose up -d ;;
  run)
    run $@ ;;
esac