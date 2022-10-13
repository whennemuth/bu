#!/bin/bash
#
# Synchronize files in a local eclipse maven project that is setup only to remote debug
# to an app running on a remote linux server. Therefore all maven building and app launching
# is done on the remote server and a remote debugging launch configuration is started in 
# the local eclipse environment. This requires:
#   1) All the .m2 artifacts (jars and source jars) built on the remote server be "pulled" 
#      down to the local .m2 repository to establish a synchronized state.
#   2) Any changes made to files (.java, .xml, .js, etc) in the local eclipse workspace be 
#      "pushed" to the remote server to establish a synchronized state.
# The functions below perform these operations.
#
# ARGS USED:
# -c checksum
# -h show help screen
# -a "archive" mode, which ensures that symbolic links, devices, attributes, permissions, ownerships etc are preserved in the transfer
# -v verbose
# -z Compress file data during the transfer
# -P Keep partially transferred files and show progress during transfer. Equivalent to --partial --progress
# -e Specify rsh replacement (like 'ssh')
# --delete Delete files that don't exist on the sending side
#
# NOTE: rsync.exe does not come with the basic git bash installation. You need to install the Git SDK.
#       https://github.com/git-for-windows/build-extra/releases


# "Pull" the upstream .m2 repository down to your local .m2 repo
syncm2() {
  rsync \
    -chavxP \
    -e "ssh -i ~/.ssh/buaws-kuali-rsa-warren -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" \
    ec2-user@10.57.237.89:/home/ec2-user/.m2/repository/ \
    ~/.m2/repository/
}


# "Push" changes made in your local eclipse coeus-impl project upstream to the corresponding location there.
syncimpl() {
  rsync \
    -chavxP \
    -e "ssh -i ~/.ssh/buaws-kuali-rsa-warren -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" \
    /c/whennemuth/workspaces/kuali_workspace_remote/kuali-research/coeus-impl/src/main/ \
    ec2-user@10.57.237.89:/opt/kuali/kc/coeus-impl/src/main/
}

# "Pull" the upstream target directory (except for the war file) to the local project.
syncImplTargetDir() {
  local VERSION="$1"
  if [ -z "$VERSION" ] ; then
    echo "ERROR! No version provided to syncImplTargetDir! Cancelling rsync"
    return 1
  fi

  rsync \
    -chavxP \
    --exclude *.war \
    -e "ssh -i ~/.ssh/buaws-kuali-rsa-warren -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" \
    ec2-user@10.57.237.89:/opt/kuali/kc/coeus-webapp/target/coeus-webapp-$VERSION/ \
    /c/whennemuth/workspaces/kuali_workspace_remote/kuali-research/coeus-webapp/target/coeus-webapp-$VERSION/
}
