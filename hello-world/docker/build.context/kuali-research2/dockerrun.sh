docker run \
   -d \
   -p 8080:8080 \
   --restart unless-stopped \
   --name kccontainer2 \
   -v /opt/kuali/main/config:/opt/kuali/main/config \
   -v /var/log/kuali/printing:/opt/kuali/logs/printing \
   -v /var/log/kuali/tomcat:/var/log/tomcat \
   bu-ist/centos7-kuali-research
   

#bu-ist/centos7-java-tomcatdocker run \
#   -d \
#   -u root \
#   -p 8080:8080 \
#   -p 80:8080 \
#   -p 443:8080 \
#   -p 2521:2521 \
#   --restart unless-stopped \
#   --name kccontainer2 \
#   -v /var/log/kuali:/usr/share/tomcat/log \
#   -v /opt/kuali/config:/root/kuali \
#   bu-ist/kuali-research:1603.49
