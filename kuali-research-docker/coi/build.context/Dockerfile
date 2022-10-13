#################################################################################################
#
# Docker build for the Boston University coi web application.
#
# REFS: https://github.com/KualiCo/research-coi/blob/master/README.md
# 
# NOTE: You can test the RUN commands by executing each of their line items in sequence
#       in a basic centos7 container started in the foreground with:
#          docker run -ti -u root --name coi centos:7 bash 
#       and all subsequent starts:
#          docker start -i coi
# 
# THIS IS A MULTI-STAGE BUILD!
#
#################################################################################################

# NOTE: Must declare ARGS before the first FROM instruction in a multi-stage build to use them inline of a FROM instruction.
#       But you must then redeclare them without without a default value if you want to use them AFTER a FROM instruction.
#       SEE: https://docs.docker.com/engine/reference/builder/#understand-how-arg-and-from-interact
# USAGE:
#       Get a build based with oracle database driver (default):
#         docker build -t [registry]/[repo]:[tag]
#         or...
#         docker build -t [registry]/[repo]:[tag] --target ORACLE
#         or...
#         docker build -t [registry]/[repo]:[tag] --build-arg BUILD_STAGE=ORACLE
#
#       Get a build with mongodb installed as a local database:
#         docker build -t [registry]/[repo]:[tag] --target MONGO
#         or...
#         docker build -t [registry]/[repo]:[tag] --build-arg BUILD_STAGE=MONGO
#
#       Get a build with no databases or database drivers installed:
#         docker build -t [registry]/[repo]:[tag] --target SQUASHED
#         or...
#         docker build -t [registry]/[repo]:[tag] --build-arg BUILD_STAGE=SQUASHED
#      
#       Get a "bloated" build that retains the supporting module builds workspace directory (docker image is much larger):
#         docker build -t [registry]/[repo]:[tag] --target BUILDS
#         or...
#         docker build -t [registry]/[repo]:[tag] --build-arg BUILD_STAGE=BUILDS
#       

ARG BUILD_STAGE="MONGO"
# ARG BUILD_STAGE="ORACLE"

# The first stage of this build:
#   1) Runs all yum updates
#   2) Installs required software
#   3) Pulls all application basecode from git
#   4) Copies in all artifacts from the build context
# but stops short of building anything.

FROM centos:7 AS BASELINE

MAINTAINER Warren H (wrh@bu.edu)

# Set defaults (override by passing them with --build-arg arg of docker build command)
ARG PYTHON_VERSION="2.7.13"
ARG GIT_BRANCH="master"
ARG GIT_REFSPEC=""
ARG GIT_USER=""
ARG GIT_PASSWORD=""

LABEL \
    Description="coi.mysql" \
    Vendor="Boston University" \
    Department=IST \
    Platform="Centos 7" \
    Python\ Version="$PYTHON_VERSION" \
    Node.js\ Version="8"

USER root

# Install GCC, git and docker, and oracle client
RUN \
   yum update -y && \
   yum install -y gcc && \
   yum group install -y "Development Tools" && \
   yum install -y git && \
   yum install -y docker && \
   yum install -y wget && \
   yum install -y dos2unix
   # yum install epel-release -y && \
   # yum install jq -y && \

# Download Python
#RUN \
#   cd /usr/src && \
#   wget https://www.python.org/ftp/python/${PYTHON_VERSION}/Python-${PYTHON_VERSION}.tgz

# Extract Archive and Compile Python
# NOTE: make altinstall is used to prevent replacing any default python binary file /usr/bin/python
#RUN \
#   tar xzf Python-${PYTHON_VERSION}.tgz && \
#   cd Python-${PYTHON_VERSION} && \
#   ./configure && \
#   make altinstall && \
#   VERSION=$(python -V 2>&1) && \
#   if [ -z "$(echo ${VERSION} | grep ${PYTHON_VERSION})" ] ; then \
#      echo "ERROR! Python ${PYTHON_VERSION} install NOT successful"; \
#   else \
#      echo "Python ${PYTHON_VERSION} install successfull"; \
#   fi

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

# Install node from the Epel repository and create the coi root directory
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

# Install node from the 8.x rpm
RUN \
   curl --silent --location https://rpm.nodesource.com/setup_12.x | bash - && \
   yum install -y nodejs && \
   yum install -y gcc-c++ make
      
# Create 3 directories for mounting, 4 for building dependency modules, and one to hold all private SSH keys for access to github for needed source code.
RUN \ 
   if [ ! -d ~/.ssh ] ; then \
      mkdir ~/.ssh && \
      chmod 700 ~/.ssh; \
   fi && \
   mkdir /var/research-coi && \
   mkdir /var/research-coi-config && \
   mkdir /var/research-coi-uploads && \
   mkdir /var/bash-scripts/ && \
   mkdir /var/bash-scripts-mount && \
   mkdir -p /var/research-coi-dependencies/kuali-ui && \
   mkdir -p /var/research-coi-dependencies/cor-common && \
   mkdir -p /var/research-coi-dependencies/cor-formbot-gadgets && \
   mkdir -p /var/research-coi-dependencies/formbot.parent && \
   cd /var/research-coi && git init && \
   cd /var/research-coi-dependencies/kuali-ui && git init && \
   cd /var/research-coi-dependencies/cor-common && git init && \
   cd /var/research-coi-dependencies/cor-formbot-gadgets && git init && \
   cd /var/research-coi-dependencies/formbot.parent && git init

# Copy in the git config files, npm startup script, and the github private keys, and supporting scripts.
COPY *coi_rsa /var/research-coi/
COPY *kualiui_rsa /var/research-coi-dependencies/kuali-ui/
COPY *core_common_rsa /var/research-coi-dependencies/cor-common/
COPY *formbot_gadgets_rsa /var/research-coi-dependencies/cor-formbot-gadgets/
COPY *formbot_rsa /var/research-coi-dependencies/formbot.parent/
COPY git.config.coi /var/research-coi/.git/config
COPY git.config.kualiui /var/research-coi-dependencies/kuali-ui/.git/config
COPY git.config.cor.common /var/research-coi-dependencies/cor-common/.git/config
COPY git.config.cor.formbot.gadgets /var/research-coi-dependencies/cor-formbot-gadgets/.git/config
COPY git.config.formbot /var/research-coi-dependencies/formbot.parent/.git/config
COPY dbtest.js /var/research-coi
COPY bash.lib.sh /var/bash-scripts
COPY buildhelper.sh /var/bash-scripts
COPY build.variables.sh /var/bash-scripts
COPY startup.sh /var/bash-scripts

# COPY instruction seems to put \r\n into files when building on windows, even if there was no "\r" in the source file.
RUN \
   dos2unix /var/research-coi/*_rsa && \
   find /var/research-coi-dependencies/ -type f -exec dos2unix {} \; && \
   dos2unix /var/research-coi/.git/config && \
   dos2unix /var/bash-scripts/* && \
   dos2unix /var/research-coi/dbtest.js

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

# Define an ARG that, if provided a value on the command line through --build-arg
# will cause a cache miss for the following RUN instruction and force to be re-executed.
ARG DATETIME

# Pull the source code from git.
RUN \
  source /var/bash-scripts/buildhelper.sh && \
  getCoiSourceFromGit && \
  npm install --global gulp-cli && \
  npm install -g verdaccio@4.12.0
  # NOTE: verdaccio@4.12.0 is the last version that will accept node v10x











# The 2nd stage performs all of the building (npm install, npm build, dependency builds, etc.
# The bulk of the megabytes generated here can be omitted from the final image.

FROM BASELINE AS BUILDS

# Install, build and publish (to local npm registry) all private required dependencies of research-coi from their source.
# The published artifacts are then installed into the main research-coi module.
# NOTE: For some reason webpack blows up unless npm installs devDependencies as well as the primary ones.
#   However, devDependencies for formbot contain a reference to a private package "@kuali/utils" which I cannot 
#   find in the kualico github account for downloading and building. Removal does not seem to cause any issue.
# IMPORTANT! The order in which these npm installs take place is specific so as accomodate peer dependency validation.
RUN \
  source /var/bash-scripts/buildhelper.sh && \
  doNpmActions \
    publish:cor-common \
    publish:kuali-ui \
    install:formbot.parent \
    publish:formbot.parent/packages/formbot \
    publish:formbot.parent/packages/formbot-react-components \
    publish:formbot.parent/packages/formbot-react \
    publish:formbot.parent/packages/formbot-validation \
    publish:formbot.parent/packages/gadgets-preset-basic-impl \
    publish:formbot.parent/packages/gadgets-preset-basic \
    publish:formbot.parent/packages/gadgets-preset-layout \
    publish:cor-formbot-gadgets

# Install and build the app and clean up
RUN \
  cd /var/research-coi && \
  source /var/bash-scripts/buildhelper.sh && \
  loginToLocalNpmRegistry && \
  disablePackageLocking && \
  npm install --unsafe-perm && \
  npm install --unsafe-perm knex && \
  npm run webpack && \
  npm run build && \
  rm -rf /var/research-coi-dependencies

CMD ["sh", "/var/bash-scripts/startup.sh"]








# This stage copies in only the target folder of the build and leaves behind the workspace where all of the
# supporting modules were built (reduces size of image).

FROM BASELINE AS SQUASHED

COPY --from=BUILDS /var/research-coi /var/research-coi

CMD ["sh", "/var/bash-scripts/startup.sh"]







# Same as the SQUASHED stage, but with mongodb installed to run a local mongo database accessible from localhost.

FROM SQUASHED AS MONGO

COPY mongodb-org-3.6.repo /etc/yum.repos.d/

RUN \
   dos2unix /etc/yum.repos.d/mongodb-org-3.6.repo && \
   yum install -y mongodb-org && \
   mkdir -p /data/db;

CMD ["sh", "/var/bash-scripts/startup.sh"]







# This stage installs the oracledb drivers.
# This stage cannot "inherit" from BASELINE, and so the supporting modules build workspace is deleted explicitly.

FROM SQUASHED AS ORACLE

# Copy the oracle client npm files from the build context
COPY oracle-instantclient12.2-basic-12.2.0.1.0-1.x86_64.rpm /var/research-coi/
COPY oracle-instantclient12.2-devel-12.2.0.1.0-1.x86_64.rpm /var/research-coi/

# Uninstall mysql and install the oracledb client
RUN \
   cd /var/research-coi && \
   # Cannot delete directories that were created in a layer prior to this one with the current kernel of centos7
   # npm uninstall mysql; \
   rpm -Uvh oracle-instantclient12.2-basic-12.2.0.1.0-1.x86_64.rpm && \
   rpm -Uvh oracle-instantclient12.2-devel-12.2.0.1.0-1.x86_64.rpm && \
   export OCI_LIB_DIR=/usr/lib/oracle/12.2/client64/lib && \
   export OCI_INC_DIR=/usr/include/oracle/12.2/client64 && \
   npm install oracledb --save && \
   unset OCI_LIB_DIR OCI_INC_DIR;
   # Exported variable are only available in the intermediate container created for this image layer alone.
   # They won't be persisted to the final image. So, using ENV instead (see below).
   # export LD_LIBRARY_PATH=/usr/lib/oracle/12.2/client64/lib:$LD_LIBRARY_PATH; \
   # export PATH=/usr/lib/oracle/12.2/client64/bin:$PATH

ENV LD_LIB_PATH=/usr/lib/oracle/12.2/client64/lib
ENV LD_BIN_PATH=/usr/lib/oracle/12.2/client64/bin

# NOTE: Make sure volume declarations are made AFTER file creation/manipulation in
# those directories, otherwise all but COPY instructions are rendered undone.
# Uncomment these lines if:
#   1) Not bind-mounting (-m, --mount) containers to directories and using volumes (-v, --volume) instead.
#   2) VOLUMES are created for the application inside the container to write to (versus, taking in content from outside the container).
#      Other implications for including a VOLUME instruction in vs. using -v or -m are at: https://docs.docker.com/engine/reference/builder/#volume
# VOLUME /var/research-coi-config
# VOLUME /var/research-coi-uploads

# Startup script will:
#    1) Apply migrations (if not already applied, as for containers very first run)
#    2) Run npm run
# NOTE: It is assumed that prior to running the CMD instruction below, the desired copy of startup.sh has been placed
#       In the mounted directory for internal container access at /var/research-coi/startup.sh.
# TODO: Figure out how to run /var/research-coi/startup.sh instead if /var/research-coi/startup.sh is not present.

CMD ["sh", "/var/bash-scripts/startup.sh", "oracledb"]







FROM ${BUILD_STAGE} AS FINAL



