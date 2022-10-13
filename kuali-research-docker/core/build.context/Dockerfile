#################################################################################################
#
# Docker build for the Boston University core rice web application.
#
# REFS: https://github.com/bu-ist/kuali-core-main/blob/master/readme.md
# 
# NOTE: You can test the RUN commands by executing each of their line items in sequence
#       in a basic centos7 container started in the foreground with:
#          docker run -ti -u root --name core centos:7 bash 
#       and all subsequent starts:
#          docker start -i core
# 
# THIS IS A MULTI-STAGE BUILD!
#
#################################################################################################

# NOTE: Must declare ARGS before the first FROM instruction in a multi-stage build to use them inline of a FROM instruction.
#       But you must then redeclare them without without a default value if you want to use them AFTER a FROM instruction.
#       SEE: https://docs.docker.com/engine/reference/builder/#understand-how-arg-and-from-interact
# Pass in a build arg of "LOCAL_MONGO" if you want to include installation of mongodb to run a local copy of mongo.
ARG BUILD_STAGE="BASELINE"

# The first stage of this build:
#   1) Runs all yum updates
#   2) Installs required software
#   3) Pulls all application basecode from git
#   4) Copies in all artifacts from the build context
# but stops short of building anything.

FROM centos:7 AS BASELINE

MAINTAINER Warren H (wrh@bu.edu)

ARG GIT_USER=""
ARG GIT_PASSWORD=""
ARG GIT_BRANCH="master"
ARG GIT_REFSPEC=""

LABEL \
    Vendor="Boston University" \
    Department=IST \
    Platform="Centos 7" \
    Node.js\ Version="8"

USER root

# Install GCC, git, docker, redis, and other tools.
RUN \
   yum update -y && \
   yum install -y gcc && \
   yum group install -y "Development Tools" && \
   yum install -y git && \
   yum install -y wget && \
   yum install -y dos2unix && \
   yum install -y epel-release && \
   yum install -y redis

# Install node version manager
RUN \
   NVM_VERSION=$(git ls-remote --tags https://github.com/creationix/nvm \
      # | sort -t '/' -k 3 -V \
      | tail -n 1 \
      | grep -Po "\d+\.\d+\.\d+") && \
   curl https://raw.githubusercontent.com/creationix/nvm/v${NVM_VERSION}/install.sh | bash && \
   source ~/.bash_profile && \
   export NVM_DIR="$HOME/.nvm" && \
   [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  # This loads nvm

# Install node from the Epel repository and create the core root directory
# NOTE: Redhat broke EPEL for http-parser dependents (ie: nodejs) around Aug 2017.
#       http-parser was added to the RedHat Base repository for 7.4, therefore EPEL removed it.
#       This results in a gap in time where CentOS 7.4 has not yet been released, and the EPEL 
#       has a Nodejs package with http-parser as a dependency which cannot be resolved.
#       Thus the custom rpm install below. This should no longer be necessary when the 
#       dockerhub central updates the centos:7 image to derive from version 7.4 from 7.3
# RUN \
#   yum install -y epel-release && \
#   yum update -y; \
#   [ -z "$(yum list all | grep 'http-parser')" ] && \
#      rpm -ivh https://kojipkgs.fedoraproject.org//packages/http-parser/2.7.1/3.el7/x86_64/http-parser-2.7.1-3.el7.x86_64.rpm; \
#   yum install -y nodejs

# Install node from the 10.x rpm (Will be compatible with versions of gulp below 4.x, like the one in cor-common)
RUN \
   curl --silent --location https://rpm.nodesource.com/setup_10.x | bash - && \
   curl --silent --location https://dl.yarnpkg.com/rpm/yarn.repo | tee /etc/yum.repos.d/yarn.repo && \
   yum install -y nodejs && \
   yum install -y gcc-c++ make && \
   yum install -y yarn
   # yum install epel-release -y && \
   # yum install jq -y && \



# Create the core root directory and prepare it for git activity, also create a 
# directory for mounting that will contain environment specific configurations
RUN \
   if [ ! -d ~/.ssh ] ; then \
      mkdir ~/.ssh && \
      chmod 700 ~/.ssh; \
   fi && \
   mkdir /var/core && \
   mkdir /var/bash-scripts && \
   mkdir /var/bash-scripts-mount && \
   mkdir /var/core-config && \
   mkdir -p /var/core-dependencies/kuali-ui && \
   mkdir -p /var/core-dependencies/cor-common && \
   mkdir -p /var/core-dependencies/fluffle && \
   cd /var/core && git init

# Copy in all necessary items from the docker build context directory
COPY *_core_rsa /var/core/
COPY *_common_rsa /var/core-dependencies/cor-common/
COPY *_kualiui_rsa /var/core-dependencies/kuali-ui/
COPY *_fluffle_rsa /var/core-dependencies/fluffle/
COPY git.config.core /var/core/.git/config
COPY git.config.core.common /var/core-dependencies/cor-common/.git/config
COPY git.config.kualiui /var/core-dependencies/kuali-ui/.git/config
COPY git.config.fluffle /var/core-dependencies/fluffle/.git/config
COPY bash.lib.sh /var/bash-scripts
COPY buildhelper.sh /var/bash-scripts
COPY build.variables.sh /var/bash-scripts
COPY dbtest.js /var/core/
COPY startup.sh /var/bash-scripts
COPY mongodb-org-3.6.repo /etc/yum.repos.d/

# COPY instruction seems to put \r\n into files when building on windows, even if there was no "\r" in the source file.
RUN \
   dos2unix /var/core/*_rsa && \
   find /var/core-dependencies/ -type f -exec dos2unix {} \; && \
   dos2unix /var/core/.git/config && \
   dos2unix /var/bash-scripts/* && \
   dos2unix /var/core/dbtest.js

# RUN instructions futher down will need helper functions found in bash.lib.sh.
# This file is not stored in git inside the build context directory because it is shared between
# multiple docker builds. So, to avoid having multiple copies of the same file, it is stored once in
# the git root directory (2 directories up from this build context). It is expected that the file
# will be copied from there to this build context before the docker build command is run.
RUN \
   if [ -f /var/bash-scripts/bash.lib.sh ] ; then \
      . /var/bash-scripts/bash.lib.sh; \
   else \
      echo "ERROR! bash.lib.sh file not found in build context. Cancelling build." && \
      exit 1; \
   fi

# Define an ARG that, if provided a value on the command line through --build-arg, 
# will cause a cache miss for the following RUN instruction and force to be re-executed.
ARG DATETIME

# Pull the source code from git.
RUN \
  source /var/bash-scripts/buildhelper.sh && \
  getCoreSourceFromGit && \
  npm install --global gulp-cli && \
  npm install -g verdaccio@4.12.0
  # NOTE: verdaccio@4.12.0 is the last version that will accept node v10x

# Install mongo client (for running local - daemon will not be started if not running locally).
RUN \
   dos2unix /etc/yum.repos.d/mongodb-org-3.6.repo; \
   yum install -y mongodb-org; \
   mkdir -p /data/db;










# The 2nd stage performs all of the building (npm install, npm build, dependency builds, etc.
# The bulk of the megabytes generated here can be omitted from the final image.
# However, you can build an image from this stage as follows:
# docker build --target BLOATED -t [registry]/[repo]:[tag]

FROM BASELINE AS BLOATED

USER root

# Install, build and publish (to local npm registry) all private required dependencies of cor-main from their source.
# The published artifacts are then installed into the main cor-main module.
# IMPORTANT! The order in which these npm installs take place may be specific so as accomodate peer dependency validation.
RUN \
   source /var/bash-scripts/buildhelper.sh && \
   # Yarn or npm both crash the install phase when they can't find fluffle, even though it's an optionalDepenency.
   # The issue might have something to do with: https://github.com/yarnpkg/yarn/issues/5878
   removePackageFromDependencies \
     package=@kuali/fluffle \
     dependencyGroup=optionalDependencies \
     pkgJsonFile=/var/core/package.json && \
   doNpmActions \
     publish:cor-common \
     publish:kuali-ui
     # publish:fluffle 

# Install and build the app
RUN \
   # Raise the version of nodejs from 10.x to 12.x now that cor-common has been built.
   curl --silent --location https://rpm.nodesource.com/setup_12.x | bash - && \
   npm cache clean -f && \
   npm install -g n && \
   n stable && \
   PATH="$PATH" && \
   # Node is now 12.x, ready to build.
   cd /var/core && \
   source /var/bash-scripts/buildhelper.sh && \
   loginToLocalNpmRegistry && \
   disablePackageLocking && \
   # npm install -g npm@3; 
   # above npm call will cause a conflict as centos rpm package for npm and npm update repo overlap but have different source code. 
   # Settle with the npm version that loads with nodejs. 
   # (see: https://github.com/nodesource/distributions/issues/383)
   #   
   yarn cache clean && \
   # NOTE: Cannot install with --prod flag or NODE_ENV=production because devDependencies are skipped, 
   #       which include the needed plugins html-webpack-plugin, webpack-cli, etc. used by build scripts.
   #       Luckily the devDependencies don't carry any @kuali/ modules, or we'd have to "tgz"ify them as well.
   # NOTE: Must use --ignore-engines because seed/package.json has a reference: "engines": { "node": "4.4.5" }, 
   #       which is way out of date and is not superceded by the engine reference for node in the parent project package.json
   yarn install --ignore-optional --ignore-engines && \
   yarn build;

# Fix an issue with a 3rd party library.
RUN \
   source /var/bash-scripts/buildhelper.sh && \
   fixShibbolethWithHack;

CMD ["sh", "/var/bash-scripts/startup.sh"]








# The 3rd stage combines the first stage with only the node app root directory included from the 2nd stage.
# Specifically, the /var/core-dependencies folder is ommitted, which is HUGE.
# This is equivalent to building something like a java app and making sure the .m2 directory is excluded.
# Pulling the image built here is much faster if it is "squashed" in this way.

FROM ${BUILD_STAGE} AS SQUASHED

COPY --from=BLOATED /var/core /var/core

RUN \
   rm -rf /var/core-dependencies \
   # Raise the version of nodejs from 10.x to 12.x (again) now that cor-common has been built.
   curl --silent --location https://rpm.nodesource.com/setup_12.x | bash - && \
   npm cache clean -f && \
   npm install -g n && \
   n stable && \
   PATH="$PATH"

CMD ["sh", "/var/bash-scripts/startup.sh"]
