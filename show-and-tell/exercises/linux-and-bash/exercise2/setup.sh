#!/bin/bash

# STEPS:
#   1) cd into same folder as this this script file.
#   2) sh setup.sh clean
#   3) sh setup.sh run
#      (Note the host ports that the containers are linking to their port 22
#       These ports get printed out to the console when the containers are run).
#   4) sh setup.sh auth
#   5) docker exec -ti ssh-daemon_1 bash
#      sh /tmp/devops/bash/auth.sh
#      exit
#   6) docker exec -ti ssh-daemon_2 bash
#      sh /tmp/devops/bash/auth.sh
#      exit
#   7) 
#      docker exec -ti ssh-daemon_1 bash
#      ssh -i /home/myuser1/.ssh/id_rsa2 myuser2@172.17.0.1 -p [host port as noted above in step 3]
#   8) ssh -i /home/myuser2/.ssh/id_rsa1 myuser1@172.17.0.1 -p [host port as noted above in step 3]

if [ -f ../../lib.sh ] ; then
  source ../../lib.sh
elif [ -f ../../../lib.sh ] ; then
  source ../../../lib.sh
fi

image='wrh1/ssh-daemon'
container1="ssh-daemon_1"
container2="ssh-daemon_2"

runContainers() {
  ( ( [ ! -d mount1 ] && mkdir -p mount1 ) || true ) && \
  ( ( [ ! -d mount2 ] && mkdir -p mount2 ) || true ) && \
  (docker rm -f $container1 2> /dev/null || true) && \
  (docker rm -f $container2 2> /dev/null || true) && \
  \
  docker run -d -P \
    --name  $container1 \
    --hostname $container1 \
    --mount type=bind,source=$(getPwdForMount)mount1,target=/tmp \
    $image && \
  \
  docker run -d -P \
    --name  $container2 \
    --hostname $container2 \
    --mount type=bind,source=$(getPwdForMount)mount2,target=/tmp \
    $image && \
  \
  docker port $container1 22 | awk 'BEGIN { FS=":" } { print "Connect with '"$container1"' over ssh using \"ssh root@localhost -p " $2 "\"" }' && \
  docker port $container2 22 | awk 'BEGIN { FS=":" } { print "Connect with '"$container2"' over ssh using \"ssh root@localhost -p " $2 "\"" }'
}

task="$1"

main() {
  case "$task" in
    'rerun')
      docker rm -f $container1 2> /dev/null
      docker rm -f $container2 2> /dev/null
      ;;
    'clean')
      docker rm -f $container1 2> /dev/null
      docker rm -f $container2 2> /dev/null
      rm -rf mount1 2> /dev/null
      rm -rf mount2 2> /dev/null
      ;;
    *)
      runContainers ;;
  esac
}

main $@