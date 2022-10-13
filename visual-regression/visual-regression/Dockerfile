FROM backstopjs/backstopjs:latest

ARG git_user=""
ARG git_pswd=""

RUN \
    echo "deb http://deb.debian.org/debian stretch main contrib" >> /etc/apt/sources.list && \
    apt update && \
    apt install -y vim-common jq && \
    apt install -y default-jdk && \
    apt install -y maven

WORKDIR /app

COPY bash/buildhelper.sh bash/utils.sh ./

SHELL ["/bin/bash", "-c"]

# Pull and build the java app
RUN \
    source buildhelper.sh && \
    dotask \
      --task pull-java \
      --git-repo https://github.com/whennemuth/visual-regression2.git \
      --git-user $git_user \
      --git-pswd $git_pswd && \
    cd basket && \
    mvn clean dependency:copy-dependencies compile

# Pull and build the node app
RUN \
    source buildhelper.sh && \
    dotask \
      --task pull-node \
      --git-repo https://github.com/bu-ist/bu-webdiff.git \
      --git-user $git_user \
      --git-pswd $git_pswd && \
    cd webdiff && \
    npm install

COPY bash/task.sh ./

# COPY app/package*.json ./
# RUN npm install

# For now overriding the parent ENTRYPOINT:
# ENTRYPOINT [ "backstop" ]

ENTRYPOINT [ "" ]
CMD [ "/bin/bash", "/app/" ]

