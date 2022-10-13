#################################################################################################
#
# Docker build for the Boston University research-portal application.
#
# REFS: https://github.com/bu-ist/kuali-research-portal/blob/master/readme.md
#
# NOTE: You can test the RUN commands by executing each of their line items in sequence
#       in a basic centos7 container started in the foreground with:
#          docker run -ti -u root --name research-portal centos:7 bash
#       and all subsequent starts:
#          docker start -i research-portal
#
# THIS IS A MULTI-STAGE BUILD!
#
##################################################################################################

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

# Set defaults (override by passing them with --build-arg arg of docker build command)
ARG GIT_USER=""
ARG GIT_PASSWORD=""
ARG GIT_BRANCH="master"
ARG GIT_REFSPEC=""

ENV CACHE_RES_REQUESTS="true"
ENV CORE_AUTH_BASE_URL="localhost:3000"
ENV LOG_LEVEL="debug"
ENV MONGODB_URI="mongodb://localhost:27017/research-portal"
ENV NODE_ENV=""
ENV PORT="3005"
ENV RESEARCH_URL="localhost:8080/kc"
ENV SA_SECRET=""

LABEL \
    Vendor="Boston University" \
    Department=IST \
    Platform="Centos 7" \
    Node.js\ Version="8"

USER root

# Install GCC, git and docker, and oracle client
RUN \
   yum update -y && \
   yum install -y gcc && \
   yum group install -y "Development Tools" && \
   yum install -y git && \
   yum install -y wget && \
   yum install -y dos2unix
   # yum install epel-release -y && \
   # yum install jq -y && \

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

# Install node from the Epel repository and create the research-portal root directory
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

# Install node from the 10.x rpm (will encounter a problem building kuali-ui: https://github.com/sass/node-sass/issues/2547)
RUN \
   curl --silent --location https://rpm.nodesource.com/setup_10.x | bash - && \
   yum install -y nodejs && \
   yum install -y gcc-c++ make

# Create the root directory and prepare it for git activity, also create a
# directory for mounting that will contain environment specific configurations
RUN \
   if [ ! -d ~/.ssh ] ; then \
      mkdir ~/.ssh && \
      chmod 700 ~/.ssh; \
   fi && \
   mkdir /var/portal && \
   mkdir /var/bash-scripts && \
   mkdir /var/bash-scripts-mount && \
   mkdir /var/portal-config && \
   mkdir -p /var/portal-dependencies/kuali-ui && \
   cd /var/portal && git init

# Copy in all necessary items from the docker build context directory
COPY *_portal_rsa /var/portal/
COPY *_kualiui_rsa /var/portal-dependencies/kuali-ui/
COPY git.config.portal /var/portal/.git/config
COPY git.config.kualiui /var/portal-dependencies/kuali-ui/.git/config
COPY bash.lib.sh /var/bash-scripts
COPY buildhelper.sh /var/bash-scripts
COPY build.variables.sh /var/bash-scripts
COPY startup.sh /var/bash-scripts

# COPY instruction seems to put \r\n into files when building on windows, even if there was no "\r" in the source file.
RUN \
   dos2unix /var/portal/*_portal_rsa && \
   find /var/portal-dependencies/ -type f -exec dos2unix {} \; && \
   dos2unix /var/portal/.git/config && \
   dos2unix /var/bash-scripts/*

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
  getPortalSourceFromGit && \
  npm install --global gulp-cli && \
  npm install -g verdaccio@4.12.0
  # NOTE: verdaccio@4.12.0 is the last version that will accept node v10x

COPY mongodb-org-3.6.repo /etc/yum.repos.d/

RUN \
   dos2unix /etc/yum.repos.d/mongodb-org-3.6.repo; \
   yum install -y mongodb-org; \
   mkdir -p /data/db;








# The 2nd stage performs all of the building (npm install, npm build, dependency builds, etc.
# The bulk of the megabytes generated here can be omitted from the final image.
# However, you can build an image from this stage as follows:
# docker build --target BLOATED -t [registry]/[repo]:[tag]

FROM BASELINE AS BLOATED

# Install, build and publish (to local npm registry) all private required dependencies of research-portal from their source.
# The published artifacts are then installed into the main research-portal module.
# IMPORTANT! The order in which these npm installs take place is specific so as accomodate peer dependency validation.
RUN \
   source /var/bash-scripts/buildhelper.sh && \
   doNpmActions publish:kuali-ui

# Install the app
RUN \
   # Raise the version of nodejs from 10.x to 12.x now that cor-common has been built.
   curl --silent --location https://rpm.nodesource.com/setup_12.x | bash - && \
   npm cache clean -f && \
   npm install -g n && \
   n stable && \
   PATH="$PATH" && \
   # Node is now 12.x, ready to build.
   # 
   source /var/bash-scripts/buildhelper.sh && \
   cd /var/portal && \
   source /var/bash-scripts/buildhelper.sh && \
   loginToLocalNpmRegistry && \
   disablePackageLocking && \
   # For some reason webpack blows up unless npm installs devDependencies as well as the primary ones.
   # However, devDependencies contain a reference to a private package "@kuali/utils" which I cannot
   # find in the kualico github account for downloading and building. Removal does not seem to cause any issue.
   removePackageFromDependencies "package=@kuali/utils" "dependencyGroup=devDependencies" "pkgJsonFile=/var/portal/package.json" && \
   # npm install -g npm@3;
   # above npm call will cause a conflict as centos rpm package for npm and npm update repo overlap but have different source code.
   # Settle with the npm version that loads with nodejs.
   # (see: https://github.com/nodesource/distributions/issues/383)
   #
   # npm install --unsafe-perm --no-optional --development
   npm install --force --save-dev

# Build the app if it has a build script
RUN \
   source /var/bash-scripts/buildhelper.sh; \
   hasScript "build" "/var/portal" && npm run build || true; \
   [ $? -gt 0 ] && exit 1 || true; \
   hasScript "webpack" "/var/portal" && npm run webpack || true; \
   [ $? -gt 0 ] && exit 1 || true;

CMD ["sh", "/var/bash-scripts/startup.sh"]









# The 3rd stage combines the first stage with only the node app root directory included from the 2nd stage.
# Specifically, the /var/portal-dependencies folder is ommitted, which is HUGE.
# This is equivalent to building something like a java app and making sure the .m2 directory is excluded.
# Pulling the image built here is much faster if it is "squashed" in this way.

FROM ${BUILD_STAGE} AS SQUASHED

COPY --from=BLOATED /var/portal /var/portal

RUN rm -rf /var/portal-dependencies

CMD ["sh", "/var/bash-scripts/startup.sh"]
