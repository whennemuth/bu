# Push java/tomcat image to the registry
# Example calls:
#   1) source dockerpush.sh
#   2) source dockerpush.sh TOMCAT_VERSION=8.5.21
#   3) source dockerpush.sh TOMCAT_VERSION=latest REPO_URI=bu-ist/centos7-java-tomcat

[ -n "$1" ] && source "$1"
[ -n "$2" ] && source "$2"
[ -z "$TOMCAT_VERSION" ] && TOMCAT_VERSION="8.5.34"
[ -z "$REPO_URI" ] && REPO_URI="$(cat repository)"
[ ${TOMCAT_VERSION,,} == "latest" ] && TAG="latest" || TAG="tomcat${TOMCAT_VERSION}"

# login to ECR (requires ~/.aws/config)
eval $(aws ecr get-login --profile sandbox)

# Push the image
docker push $REPO_URI:$TAG
