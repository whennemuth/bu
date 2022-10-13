# Docker build for Conflict of Interest (COI) 

## Overview

[Back to main page...](/../..)

Overview pending...

### Instructions to build and run

These directions are intended as a step-by-step guide for getting COI running in the Boston University cloud-based AWS server environment. It is supplemental to the README instructions at the KualiCo github page.

1. Shell into the EC2 instance and pull the source code from COI from git as follows:

   ```
   if [ ! -d /opt/kuali-research-docker ] ; then
     # There is no git build context for core, coi, etc. so clone it from remote.
     cd /opt
     git clone https://github.com/bu-ist/kuali-research-docker.git
     cd  kuali-research-docker
     git checkout master
   elif [ ! -d /opt/kuali-research-docker/.git ] ; then
     # The build context directory was found, but it does not appear to be 
     # a git repository, so initialize it and pull the master branch down.
     cd /opt/kuali-research-docker
     git init
     git remote add bu https://github.com/bu-ist/kuali-research-docker.git
     git pull bu master
   else
     cd /opt/kuali-research-docker
     git checkout master
     git pull bu master

     # If the branch is not master and that branch only exists upstream, then 
     # assuming the branch name is "feature" and the remote name is origin do the following:
     cd /opt/kuali-research-docker
     git fetch origin feature
     git checkout -b feature remotes/origin/feature
   fi
     
   cd coi/build.context
   ```

   ​

2. Additional files need to be added to the docker build context:

   - **Github key**: The COI docker image build requires that a private SSH granting access to the git repository for COI be present in the docker build context and that it match in name with "*_rsa".
   - Further documentation pending...