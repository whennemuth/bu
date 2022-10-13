#!/bin/bash

# This script is in s3. You can run it with:
#   sudo su root
#   aws s3 cp s3://kuali-research-ec2-setup/misc/bash/prunelogs.sh - | sh -s "pruneAll"

# create a gzip archive of every log file and empty the log file.
pruneHttpd() {
  echo "Pruning httpd..."
  if [ ! -d /var/log/httpd ] ; then
    echo "Cannot prune httpd logs: /var/log/httpd directory not found."
    return 1
  fi
  cd /var/log/httpd
  docker stop apache-shibboleth 2> /dev/null

  if [ -f access_log ] ; then
    echo "pruning $(pwd)/access_log..."
    gzip -c access_log > access_log.$(date +%m%d%Y).gz
    echo > access_log
    removeAllButLatest "access_log.*.gz"
  fi     

  if [ -f error_log ] ; then
    echo "pruning $(pwd)/error_log..."
    gzip -c error_log > error_log.$(date +%m%d%Y).gz
    echo > error_log
    removeAllButLatest "error_log.*.gz"
  fi

  if [ -f ssl_access_log ] ; then
    echo "pruning $(pwd)/ssl_access_log..."
    gzip -c ssl_access_log > ssl_access_log.$(date +%m%d%Y).gz
    echo > ssl_access_log
    removeAllButLatest "ssl_access_log.*.gz"
  fi

  if [ -f ssl_error_log ] ; then
    echo "pruning $(pwd)/ssl_error_log..."
    gzip -c ssl_error_log > ssl_error_log.$(date +%m%d%Y).gz
    echo > ssl_error_log
    removeAllButLatest "ssl_error_log.*.gz"
  fi

  if [ -f ssl_request_log ] ; then
    echo "pruning $(pwd)/ssl_request_log..."
    gzip -c ssl_request_log > ssl_request_log.$(date +%m%d%Y).gz
    echo > ssl_request_log
    removeAllButLatest "ssl_request_log.*.gz"
  fi

  docker start apache-shibboleth 2> /dev/null
  ls -la
}

# tar and gzip all logs (one .tar.gz file for each year) that were written before 2019
pruneTomcat() {
  echo "Pruning tomcat..."
  if [ ! -d /var/log/tomcat ] ; then
    echo "Cannot archive tomcat logs: /var/log/tomcat directory not found."
    return 1
  fi
  cd /var/log/tomcat
  if [ -n "$(find $(pwd) -maxdepth 1 -type f \( -iname "*2016*" ! -iname "*.tar.gz" \))" ] ; then
    tar -cvzf logs-2016.tar.gz --remove-files $(pwd)/*2016*
  fi
  if [ -n "$(find $(pwd) -maxdepth 1 -type f \( -iname "*2017*" ! -iname "*.tar.gz" \))" ] ; then
    tar -cvzf logs-2017.tar.gz --remove-files $(pwd)/*2017*
  fi
  if [ -n "$(find $(pwd) -maxdepth 1 -type f \( -iname "*2018*" ! -iname "*.tar.gz" \))" ] ; then
    tar -cvzf logs-2018.tar.gz --remove-files $(pwd)/*2018*
  fi

}

# Delete every javamelody metric directory except the one with the most recent date.
pruneJavaMelody() {
  echo "Pruning javamelody..."
  if [ ! -d /var/log/kuali/javamelody ] ; then
    echo "Cannot prune httpd logs: /var/log/kuali/javamelody directory not found."
    return 1
  fi
  cd /var/log/kuali/javamelody
  removeAllButLatest
}

# Remove all files in the current directory that match the supplied pattern except for the one with the most recent date.
removeAllButLatest() {
  local i=0
  for n in $(eval "ls -t $1") ; do
    if [ $i -gt 0 ] ; then
      rm -rf $n
    fi
    (( i++ ))
  done
}

pruneAll() {

  du -x -d1 -h /var/log | sort -h -r > /tmp/before_prune.txt

  pruneJavaMelody

  pruneHttpd

  pruneTomcat

  printf "\nBEFORE:\n"

  cat /tmp/before_prune.txt

  printf "\nAFTER:\n"

  du -x -d1 -h /var/log | sort -h -r
}

if [ "${1,,}" == "pruneall" ] ; then
  pruneAll
fi
