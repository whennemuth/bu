docker rm -f apache-shibboleth
source dockerbuild.sh
if [ -n "$(docker images --filter dangling=true -q)" ] ; then 
   docker rmi -f $(docker images --filter dangling=true -q); 
   echo "Removed dangling image(s)";
else
   echo "No dangling images to remove";
fi
if [ -n "$(docker volume ls -qf dangling=true)" ] ; then 
   docker volume rm $(docker volume ls -qf dangling=true); 
   echo "Removed dangling volume(s)";
else 
   echo "No dangling volumes to remove";
fi
source dockerrun.sh
