docker run \
   -d \
   -p 8080:8080 \
   --restart unless-stopped \
   --name hwcontainer \
   hello-world

