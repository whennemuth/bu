# Build the java/tomcat image
docker build \
   -f centos7-java-tomcat \
   -t bu-ist/centos7-java-tomcat \
   --build-arg JAVA_VERSION=8 \
   --build-arg JAVA_RELEASE=JDK .

