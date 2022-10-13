docker run \
   -d \
   -p 8080:8080 \
   --restart unless-stopped \
   --name centostomcat8.5 \
   bu-ist/centos7-java-tomcat:tomcat8.5
