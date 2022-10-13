# Build the java/tomcat image
# Example calls:
#   1) source dockerbuild.sh
#   2) source dockerbuild.sh TOMCAT_VERSION=8.5.21
#   3) source dockerbuild.sh TOMCAT_VERSION=latest REPO_URI=bu-ist/centos7-java-tomcat

[ -n "$1" ] && source "$1"
[ -n "$2" ] && source "$2"
[ -z "$TOMCAT_VERSION" ] && TOMCAT_VERSION="8.5.34"
[ -z "$REPO_URI" ] && REPO_URI="$(cat repository)"
TAG="tomcat${TOMCAT_VERSION}"

docker build \
   -t $REPO_URI:$TAG \
   --build-arg TCAT_VERSION=${TOMCAT_VERSION} \
   --build-arg JAVA_VERSION=8 \
   --build-arg JAVA_RELEASE=JDK .
