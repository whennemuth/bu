# This script copies all ssh private keys from a docker mounted folder to an unmounted folder.
# Properties of these keys can only be changed in an unmounted folder.

set -xe

echo "Jenkins launcher arguments present: JENKINS_OPTS = $JENKINS_OPTS, JAVA_OPTS = $JAVA_OPTS, USER = $USER, COPY_REFERENCE_FILE_LOG = $COPY_REFERENCE_FILE_LOG, JENKINS_HOME = $JENKINS_HOME"

# Adding the various hosts to the known_hosts file should have been done as a RUN instruction in the DockerFile.
# However test if this is true and fix the situation if it is not.
if [ -f /root/.ssh/ssh_hosts ] ; then
   if [ -! -f /root/.ssh/known_hosts ] || [ -z "$(cat /root/.ssh/known_hosts | grep github.com)" ] ; then 
      ssh-keyscan -t rsa -f /root/.ssh/ssh_hosts >> /root/.ssh/known_hosts
   fi
fi

if [ -f /var/jenkins_ssh/ssh_hosts ] ; then
   if [ ! -f /var/jenkins_ssh_mount/known_hosts ] || [ -z "$(cat /var/jenkins_ssh_mount/known_hosts | grep github.com)" ] ; then 
      ssh-keyscan -t rsa -f /var/jenkins_ssh/ssh_hosts >> /var/jenkins_ssh_mount/known_hosts
   fi
fi

# make .ssh dir (-p flag will avoid error if dir already exists).
# Then copy the mounted ssh content to the non-mounted ssh dir.
mkdir -p $JENKINS_HOME/.ssh
cp /var/jenkins_ssh_mount/. /var/jenkins_ssh/ -R
cp /var/jenkins_ssh_mount/. $JENKINS_HOME/.ssh/ -R
cp /var/jenkins_ssh_mount/. /var/lib/jenkins/.ssh/ -R

# Change permission level so that ssh agent does not complain about the ssh key being "too open".
# chgrp users /var/jenkins_ssh -R
chmod 700 /var/jenkins_ssh
chmod 600 /var/jenkins_ssh/*
chmod 600 $JENKINS_HOME/.ssh/*
chmod 600 /var/lib/jenkins/.ssh/*

# make .m2 dir and copy settings.xml to it
mkdir -p $JENKINS_HOME/.m2
cp /usr/share/jenkins/ref/settings.xml $JENKINS_HOME/.m2/

# Copy files from /usr/share/jenkins/ref into $JENKINS_HOME
# So the initial JENKINS-HOME is set with expected content.
# Don't override, as this is just a reference setup, and use from UI
# can then change this, upgrade plugins, etc.
copy_reference_file() {
	f="${1%/}"
	b="${f%.override}"
	echo "$f" >> "$COPY_REFERENCE_FILE_LOG"
	rel="${b:23}"
	dir=$(dirname "${b}")
	echo " $f -> $rel" >> "$COPY_REFERENCE_FILE_LOG"
	if [[ ! -e $JENKINS_HOME/${rel} || $f = *.override ]]
	then
		echo "copy $rel to JENKINS_HOME" >> "$COPY_REFERENCE_FILE_LOG"
		mkdir -p "$JENKINS_HOME/${dir:23}"
		cp -r "${f}" "$JENKINS_HOME/${rel}";
		# pin plugins on initial copy
		[[ ${rel} == plugins/*.jpi ]] && touch "$JENKINS_HOME/${rel}.pinned"
	fi;
}
: ${JENKINS_HOME:="/var/jenkins_home"}
export -f copy_reference_file
touch "${COPY_REFERENCE_FILE_LOG}" || (echo "Can not write to ${COPY_REFERENCE_FILE_LOG}. Wrong volume permissions?" && exit 1)
echo "--- Copying files at $(date)" >> "$COPY_REFERENCE_FILE_LOG"
find /usr/share/jenkins/ref/ -type f -exec bash -c "copy_reference_file '{}'" \;

# if `docker run` first argument start with `--` the user is passing jenkins launcher arguments
if [[ $# -lt 1 ]] || [[ "$1" == "--"* ]]; then
  # eval "exec java $JAVA_OPTS -jar /usr/share/jenkins/jenkins.war $JENKINS_OPTS \"\$@\""
  eval "exec java $JAVA_OPTS -jar /usr/share/jenkins/jenkins.war $JENKINS_OPTS"
fi

# As argument is not jenkins, assume user want to run his own process, for sample a `bash` shell to explore this image
# exec "$@"
