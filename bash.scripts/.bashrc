
WORKSPACES=/c/whennemuth/workspaces
BU_WORKSPACE=$WORKSPACES/bu_workspace
BASH_SCRIPTS=$BU_WORKSPACE/bash.scripts
KUALI_INFRASTRUCTURE=$WORKSPACES/ecs_workspace/cloud-formation/kuali-infrastructure

for line in $(find $BASH_SCRIPTS/alias.include -type f -iname '*.sh') ; do 
  eval "source $line"; 
done
alias scripts='vim '$BASH_SCRIPTS'/alias.include/misc.sh'

inf() {
  cd $KUALI_INFRASTRUCTURE
}

s3(){ cd /c/whennemuth/scrap/s3/$1/$2 && ls -la; }

s3sync() {
  cd $KUALI_INFRASTRUCTURE/s3
  getCommand() {
    cat <<EOF
    aws --profile=infnprd s3 sync --delete $([ "${1,,}" == 'dryrun' ] && echo '--dryrun' ) \\
      --exclude "*" \\
      --include "ci/*" \\
      --include "prod/*" \\
      --include "qa/*" \\
      --include "sb/*" \\
      --include "stg/*" \\
      . s3://kuali-conf
EOF
  }

  eval "$(getCommand dryrun)"

  printf "That was a dryrun. Sync for real this time? [y/n]: "
  read yn
  case $yn in
    [Yy]* )
      echo "Syncing..."
      eval "$(getCommand)"
      ;;
    * )
      echo "Cancelled."
      ;;
  esac

}

pushinf() {
  local branch="$1"
  cd $KUALI_INFRASTRUCTURE
  [ -z "$branch" ] && branch="$(git rev-parse --abbrev-ref HEAD)"
  if [ -n "$(git status -s --untracked-files=no)" ] ; then
    printf "Enter commit message: " && read msg
    if [ -z "$msg" ] ; then
      echo "Commit message is empty. Cancelling..."
      exit 1
    fi
    git commit -a -m "$msg"
  fi

  if [ -n "$(git diff --name-only master remotes/bu/master)" ] ; then
    echo 'Pushing master branch to master branch on bu remote...'
    git push bu master:master
  fi
  if [ -n "$(git diff --name-only vanilla remotes/bu/vanilla)" ] ; then
    echo 'Pushing vanilla branch to vanilla branch on bu remote...'
    git push bu vanilla:vanilla
  fi

  if [ -n "$(git diff --name-only master remotes/github/bu)" ] ; then
    echo 'Pushing master branch to bu branch on github remote...'
    git push github master:bu
  fi
  if [ -n "$(git diff --name-only vanilla remotes/github/master)" ] ; then
    echo 'Pushing vanilla branch to master branch on github remote...'
    git push bu vanilla:master
  fi
}

pushIamUid() {
  local branch=${1:-'dev'}
  cd /c/whennemuth/workspaces/bu_workspace/iam-uid-api

  heading() {
    local hr='*********************************************'
    echo "$hr" && echo "$1" && echo "$hr"
  }
  
  if [ "$(git rev-parse --abbrev-ref HEAD)" != $branch ] ; then
    heading "Checking out $branch branch..."
    git checkout $branch
    if [ $? -gt 0 ] ; then
      echo "Failed checkout. Exiting..."
      return 1
    fi
  fi

  if [ $branch == 'dev' ] ; then
    if [ -n "$(git status -s --untracked-files=no)" ] ; then
      printf "Enter commit message: " && read msg
      if [ -z "$msg" ] ; then
        echo "Commit message is empty. Cancelling..."
        return 1
      fi
      heading "Committing $branch branch..."
      git commit -a -m "$msg"
      if [ $? -gt 0 ] ; then
        echo "Failed commit. Exiting..."
        return 1
      fi
    fi
    heading "Pulling $branch branch..."
    git pull origin $branch
  else
    heading "Merging dev branch into $branch branch"
    git merge dev
  fi

  if [ -z "$(git status -s --untracked-files=no)" ] ; then
    heading "Pushing $branch branch"
    git push origin $branch:$branch
  else
    echo "Pull was not fast-forward. Exiting..."
    return 1
  fi

  case $branch in
    dev) pushIamUid 'qa' ;;
    qa) pushIamUid 'prod' ;;
    prod) git checkout dev ;;
  esac
}

refreshIamUid_old() {
  local extra="$1"
  local rootdir=/c/whennemuth/workspaces/bu_workspace/iam-uid-api
  local mine=dev
  local theirs=dev-feature-shared-code
  printf "Enter your personal access token: "
  read token
  local repo=https://whennemuth:$token@github.com/bu-ist/iam-uid-api.git

  printHeader() {
    local line="*********************************************"
    printf "\n$line\n   $1\n$line\n"
  }
  branchExists() {
    local branch="$1"
    local lookup=$(git branch | grep -oP '[^\s\*]+' | grep -P '^'$branch'$')
    [ -n "$lookup" ] && true || false
  }
  checkBranchAndDir() {
    if [ "$(git rev-parse --abbrev-ref HEAD)" != "$mine" ] ; then
      echo "Wrong branch checked out!"
      exit 1
    fi
    cd $rootdir
  }
  commitChanges() {
    if [ -n "$(git status -s)" ] ; then
      printHeader "Committing changes..."
      printf "Type commit message: "
      read msg
      git add --all
      git commit -m "$msg"
    fi
  }
  backupChanges() {
    printHeader "Backing up $mine..."
    if branchExists $mine-backup ; then
      git checkout $mine-backup
      git merge -X theirs $mine
    else
      git branch $mine-backup
    fi
  }
  pullTheirs() {
    printHeader "Pulling latest from $theirs"
    git checkout $theirs
    git fetch $repo $theirs
    [ $? -eq 0 ] && true || false
  }
  mergeConflict() {
    [ -n "$(git rev-parse MERGE_HEAD 2> /dev/null)" ] && true || false
  }
  mergeTheirs() {
    local head=$(git rev-parse HEAD)
    local fetch_head=$(git rev-parse FETCH_HEAD)
    if [ "$head" != "$fetch_head" ] ; then
      git merge FETCH_HEAD
      if [ $? -gt 0 ] ; then
        echo "Error pulling from $theirs branch"
      else
        sleep 2
        git checkout $mine
        git merge $theirs
        if [ $? -eq 0 ] && ! mergeConflict ; then
          local merged='true'
        fi
      fi
    else
      echo "No upstream changes to $theirs"
      sleep 2
      git checkout $mine
      local merged='true'
    fi
    [ "$merged" == 'true' ] && true || false
  }
  pushMine() {
    echo "Do you want to push $mine (y/n): "
    read answer
    if [ "${answer,,}" == 'y' ] ; then
      printHeader "Pushing $mine..."
      git push $repo $mine
    fi
  }
  
  checkBranchAndDir

  commitChanges

  backupChanges

  if pullTheirs ; then
    if mergeTheirs ; then
      pushMine
    fi
  fi
}

getRdsPassword() {
  local landscape="$1"
  local rdsSecret=$(
    aws secretsmanager get-secret-value \
      --secret-id kuali/$landscape/kuali-oracle-rds-admin-password \
      --output text \
      --query '{SecretString:SecretString}' 2> /dev/null
  )
  echo "RDS:"
  echo "   user = $(echo "$rdsSecret" | jq '.MasterUsername')"
  echo "   pswd = $(echo "$rdsSecret" | jq '.MasterUserPassword')"
}

getKcConfigLine() {
  {
    local landscape="$1"
    local localFile="/c/whennemuth/scrap/s3/$landscape/kc/kc-config.xml"
    if [ -f $localFile ] ; then
      cat $localFile
    else
      aws s3 cp s3://kuali-research-ec2-setup/$landscape/kuali/main/config/kc-config.xml - 2> /dev/null
    fi
  } | grep $2
}
getKcConfigDbUsername() {
  getKcConfigLine $1 'datasource.username' \
    | grep -oE '>[^<>]+<' \
    | tr -d '\t[:space:]<>'
}
getKcConfigDmsDbUsername() {
  getKcConfigLine $1 'datasource.dms.username' \
    | grep -oE '>[^<>]+<' \
    | tr -d '\t[:space:]<>'
}
getKcConfigSctDbUsername() {
  getKcConfigLine $1 'datasource.sct.username' \
    | grep -oE '>[^<>]+<' \
    | tr -d '\t[:space:]<>'
}
getKcConfigDbPassword() {
  getKcConfigLine $1 'datasource.password' \
    | grep -oE '>[^<>]+<' \
    | tr -d '\t[:space:]<>'
}
getKcConfigDmsDbPassword() {
  getKcConfigLine $1 'datasource.dms.password' \
    | grep -oE '>[^<>]+<' \
    | tr -d '\t[:space:]<>'
}
getKcConfigSctDbPassword() {
  getKcConfigLine $1 'datasource.sct.password' \
    | grep -oE '>[^<>]+<' \
    | tr -d '\t[:space:]<>'
}

dbpwds() {
  local landscape="$1"
  getRdsPassword $landscape
  echo "EC2:"
  echo "   user = $(getKcConfigDbUsername $landscape)"
  echo "   pswd = $(getKcConfigDbPassword $landscape)"
  echo "   user = $(getKcConfigSctDbUsername $landscape)"
  echo "   pswd = $(getKcConfigSctDbPassword $landscape)"
  echo "   user = $(getKcConfigDmsDbUsername $landscape)"
  echo "   pswd = $(getKcConfigDmsDbPassword $landscape)"
}

dbsecrets() {
  local landscape="$1"
  source /c/whennemuth/workspaces/ecs_workspace/cloud-formation/kuali-infrastructure/scripts/common-functions.sh
  # getRdsCredentials $@
  echo 'APPLICATION:'
  _getRdsSecret 'app' $landscape | jq '.' -C
  echo 'ADMIN:'
  _getRdsSecret 'admin' $landscape | jq '.' -C
  echo 'SCHEMA CONVERSION TOOL:'
  _getRdsSecret 'sct' $landscape | jq '.' -C
  echo 'DATA MIGRATION:'
  _getRdsSecret 'dms' $landscape | jq '.' -C
}

tunnel() {
  local landscape="$1"
  local profile="${2:-$AWS_PROFILE}"
  [ -z "$landscape" ] && echo "Landscape is missing!" && return 1 
  cd $KUALI_INFRASTRUCTURE/kuali_rds/jumpbox
  sh tunnel.sh profile=$profile landscape=$landscape
}

federatedLogin() {
  local profile="$1"
  # if ! expiredToken ; then
  #   printf "Token not expired yet.\nDo you want to create a new one anyway (y/n): "
  #   read answer
  #   [ "${answer,,}" != 'y' ] && return 0
  # fi
  winpty docker run --rm -it \
    --volume "C:\\Users\\wrh\\.aws:/root/.aws" \
    -e AWS_LOGIN_URL=https://www.bu.edu/awslogin \
    -e AWS_PROFILE=$profile \
    bostonuniversity/aws-tools shib-auth $profile
}

expiredToken() {
  [ "$(aws s3 ls 2>&1 | grep -o 'ExpiredToken')" == 'ExpiredToken' ] && true || false
}

_resetRdsIngress() {
  local profile="$2"
  local landscape="$1"

  if [ -z "$profile" ] || [ -z "$landscape" ] ; then
    echo "USAGE: resetRdsIngress profile landscape"
    return 1
  fi

  sh -c "
    cd /c/whennemuth/workspaces/ecs_workspace/cloud-formation/kuali-infrastructure/kuali_rds; 
    sh main.sh set-rds-access profile=$profile landscape=$landscape"
}

jvmdebug() {
  export BASE_DIR=$KUALI_INFRASTRUCTURE
  local landscape="$1"
  shift
  sh $BASE_DIR/scripts/tunnel.sh jvm-agent profile=infnprd landscape=$landscape $@
}

compile-class() {
  (
  cd /c/kuali/kuali-research/coeus-impl/src/main/java
  javac \
    -verbose \
    -cp /c/Users/wrh/.m2/repository/javax/json/javax.json-api/1.1.4/javax.json-api-1.1.4.jar:/c/kuali/kuali-research/coeus-impl/target/classes \
    -d /c/kuali/kuali-research/coeus-impl/target/classes \
    org/kuali/coeus/sys/impl/auth/JwtServiceImplDecorator.java
  )
}

redeploy() {
  (
    cd /c/kuali/kuali-research/coeus-impl
    mvn compiler:compile source:jar package -e -Dgrm.off=true -Dmaven.test.skip=true
    cp -v \
      /c/kuali/kuali-research/coeus-impl/target/coeus-impl-2001.0040.jar \
      /c/kuali/kuali-research/coeus-webapp/target/coeus-webapp-2001.0040/WEB-INF/lib/coeus-impl-2001.0040.jar
  )
}

alias gitcoi='git_ssh /c/kuali/research-coi bu_github_id_coi_rsa github bu-ist/kuali-research-coi'
alias gitbudoc='git_ssh /c/whennemuth/bu_workspace/documentation/bu github_id_rsa github whennemuth/bu-docs.git'
alias gittest1='git_ssh /c/gittest1 gittest_rsa_key github whennemuth/gittest.git'
alias gittest2='git_ssh /c/gittest2 gittest_rsa_key github whennemuth/gittest.git'
alias gitkc='git_ssh /c/whennemuth/workspaces/kuali_workspace/kuali-research github_id_rsa github bu-ist/kuali-research.git'
alias gitkcremote='git_ssh /c/whennemuth/workspaces/kuali_workspace_remote/kuali-research github_id_rsa github bu-ist/kuali-research.git'
alias gitkualiui='git_ssh /c/kuali-ui bu_github_id_kualiui_rsa bu bu-ist/kuali-ui.git'
alias gitdocker='git_ssh \
  /c/whennemuth/workspaces/bu_workspace/kuali-research-docker \
  bu_github_id_docker_rsa \
  github \
  bu-ist/kuali-research-docker.git'
alias gitcloudformation='git_ssh '$KUALI_INFRASTRUCTURE' bu_github_id_kuali_cloudformation_rsa bu-ist/kuali-infrastructure.git'
alias cfn='cd '$KUALI_INFRASTRUCTURE'; ls -la'
alias bye='eval `ssh-agent -k` && exit'

alias versions='cat pom.xml | grep -P "(<coeus\-api\-all\.version)|(<coeus\-s2sgen\.version)|(<rice\.version)|(<schemaspy\.version)|(<version>[a-zA-Z\d\.\-]+</version>)"'

# AWS EC2 instances
alias sandbox='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.237.84'
alias sandboxcoretunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 9229:10.57.237.84:9229 -L 3000:10.57.237.84:3000 wrh@10.57.237.84'
alias sandboxcoitunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 9228:10.57.237.84:9228 wrh@10.57.237.84'
alias cicoretunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 9229:10.57.237.36:9229 wrh@10.57.237.36'
alias cicoretunnel2='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 9229:10.57.237.37:9229 wrh@10.57.237.37'
alias cicoitunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 8092:10.57.237.36:8092 wrh@10.57.237.36'
alias qacoretunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 9229:10.57.236.244:9229 -L 3000:10.57.236.244:3000 wrh@10.57.236.244'
alias ci='ssh -i  ~/.ssh/buaws-kuali-rsa wrh@10.57.237.36'
alias jenkins='ssh -i  ~/.ssh/buaws-kuali-rsa wrh@10.57.236.6'
alias qa='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.236.244'
alias qacoitunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 8092:10.57.236.244:8092 wrh@10.57.236.244'
alias staging1='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.236.68'
alias staging2='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.236.100'
alias mydevbox='ssh -i ~/.ssh/buaws-kuali-rsa-warren ec2-user@10.57.237.89'
alias mydevboxtunnel='ssh -i ~/.ssh/buaws-kuali-rsa-warren -N -v -L 8080:10.57.237.89:8080 -L 1043:10.57.237.89:1043 ec2-user@10.57.237.89'
alias mydevboxtunnel2='ssh -i ~/.ssh/buaws-kuali-rsa-warren -N -v -L 3000:10.57.237.89:3000 -L 9229:10.57.237.89:9229 ec2-user@10.57.237.89'

alias scpsandbox='echo "Enter path to the file to upload and press [ENTER]: " ; read file ; eval "scp -C -i ~/.ssh/buaws-kuali-rsa $file wrh@10.57.237.84:~/dockerbuild/"'

alias up='cd ..'

alias upp='cd ../..'

alias uppp='cd ../../..'

alias bld='mvn clean compile source:jar javadoc:jar install -Dgrm.off=true'

alias blde='mvn clean compile source:jar javadoc:jar install -e -Dgrm.off=true'

alias bldo='mvn clean compile source:jar javadoc:jar install -Poracle -Dgrm.off=true'

# alias taillog="eval \"sudo tail /var/log/kuali/tomcat/$(eval 'sudo ls -lat /var/log/kuali/tomcat | grep -P "localhost\." | head -1' | rev | cut -d' ' -f1 | rev) -f -n 2000\""

# alias cleanrmi='DANGLING=$(docker images --filter dangling=true -q); if [ -n "$DANGLING" ]; then docker rmi -f $DANGLING; else echo "No images to remove!"; fi'

# alias cleanvol='DANGLING=$(docker volume ls -qf dangling=true); if [ -n "$DANGLING" ]; then docker volume rm $DANGLING; else echo "No volumes to remove!"; fi'

# alias cleanall='cleanrmi && cleanvol'

alias killssh='eval "ssh-agent -k"'

alias buildkc='mvn clean compile source:jar javadoc:jar install -Dgrm.off=true -Dmaven.test.skip=true'
alias centos='ssh -i ~/.ssh/centos7_rsa -p 2222 root@localhost'
alias centos2='ssh -i ~/.ssh/centos7_rsa -p 2223 root@localhost'
alias ci2='ssh -i  ~/.ssh/buaws-kuali-rsa wrh@10.57.237.37'
alias sandbox2='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.237.85'
alias sandbox3='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.237.88'
alias sandbox4='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.237.93'
alias sandboxweb1='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.237.69'
alias prod1='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.242.100'
alias prod2='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.243.100'
alias agile='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.237.86'
alias pgdata='ssh -i ~/.ssh/buaws-kuali-rsa -L 63333:172.17.0.3:5432 wrh@10.57.237.86'
alias training='ssh -i ~/.ssh/buaws-sandbox-warren-rsa ec2-user@34.197.201.237'
alias coi='ssh -i ~/.ssh/centos7_rsa -p 2224 root@localhost'
alias fvi='findAndOpenInVim'
alias coitest='ssh -i ~/.ssh/buaws-coi-test ec2-user@52.3.122.103'
alias krd='cd /c/whennemuth/kuali-research-docker'
alias backup='read -p "Type the pathname of the file: " file; cat $file > ${file}.backup.$(date +"%m-%d-%y")'
alias grepnode='grepNode $1 $2'
alias compileone='compile_one'
alias compileonetest='compile_one_test'
alias coeus.impl.build='cd /c/whennemuth/workspaces/kuali_workspace/kuali-research; mvn -e -Dmaven.test.skip=true -pl coeus-impl clean compile package -Dgrm.off=true'
alias coeus.webapp.build='cd /c/whennemuth/workspaces/kuali_workspace/kuali-research; mvn -e -Dmaven.test.skip=true -pl coeus-webapp clean compile package -Dgrm.off=true'
alias webapp='cd /c/whennemuth/workspaces/kuali_workspace_remote/kuali-research/coeus-webapp'
alias impl='cd /c/whennemuth/workspaces/kuali_workspace_remote/kuali-research/coeus-impl'
alias git.bash.scripts='git_ssh ~ github_id_rsa github whennemuth/bash-scripts.git'
alias notes='vim /c/whennemuth/workspaces/bu_workspace/documentation/bu/linux/expressions.txt'
alias scrap='cd /c/whennemuth/scrap; ls -la'
alias 1705='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.237.88'
alias 1709='ssh -i ~/.ssh/buaws-kuali-rsa wrh@10.57.237.93'
alias rice='cd /c/kuali-kc-rice'
alias framework='cd /c/kuali-kc-rice/rice-framework'
alias gitselenium='git_ssh /c/whennemuth/workspaces/bu_workspace/selenium-test github_id_rsa github whennemuth/selenium-test.git'
alias gitkcbuilder='git_ssh /c/whennemuth/workspaces/bu_workspace/kcbuilder github_id_rsa github whennemuth/kcbuilder.git'
alias gitsyncusers='git_ssh /c/sync-users bu_github_id_kualiui_rsa bu bu-ist/sync-users.git'
alias kc='cd /c/whennemuth/kuali-research-docker/kuali-research/build.context; ls -la'
alias todo='vim /c/whennemuth/scrap/todo.txt'
alias bashlib='vim /c/whennemuth/kuali-research-docker/bash.lib.sh'
alias sourcebashlib='source /c/whennemuth/kuali-research-docker/bash.lib.sh'
alias core='cd /c/whennemuth/kuali-research-docker/core/build.context; ls -la'
alias coi='cd /c/whennemuth/kuali-research-docker/coi/build.context; ls -la'
alias gitcorecommon='git_ssh /c/kuali-core-common bu_github_id_core_common_rsa bu bu-ist/kuali-core-common.git'
alias portal='cd /c/whennemuth/kuali-research-docker/research-portal/build.context; ls -la'
alias findcust="grep -rP 'BU[\-\x20]CUSTOMIZATION' ."
alias coi='cd /c/whennemuth/kuali-research-docker/coi/build.context && ls -la'
alias bashrc='vim '$BASH_SCRIPTS'/.bashrc'
alias sbrc='source ~/.bashrc'
alias gitgadgets='git_ssh /c/kuali/cor-formbot-gadgets bu_github_id_cor_formbot_gadgets_rsa github bu-ist/kuali-cor-formbot-gadgets.git'
alias gitformbot='git_ssh /c/kuali/formbot bu_github_id_formbot_rsa github bu-ist/kuali-formbot.git'
alias gitlogdate='git log --oneline --decorate=full --pretty="format:%h %ci %d %s" $1 $2'
alias debuglink='docker exec core curl localhost:9229/json/list'
alias gitportal='git_ssh /c/kuali/research-portal bu_github_id_research_portal_rsa github bu-ist/kuali-research-portal.git'
alias dockermounts="docker inspect -f \"{{json .Mounts}}\" kuali-research | jq"
alias apache="cd /c/whennemuth/kuali-research-docker/apache-shib/build.context && ls -la"
alias prodcoretunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 9229:10.57.242.100:9229 wrh@10.57.242.100'
alias stagecoretunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 9229:10.57.236.68:9229 wrh@10.57.236.68'
alias stageportaltunnel='ssh -i ~/.ssh/buaws-kuali-rsa -N -v -L 9228:10.57.236.68:9228 wrh@10.57.236.68'
alias presnotes='vim /c/whennemuth/scrap/ecs.presentation.notes.txt'
alias mongo='/c/Program\ files/MongoDB/Server/4.0/bin/mongo.exe'
alias mongod='/c/Program\ files/MongoDB/Server/4.0/bin/mongod.exe'
alias mongodump='/c/Program\ files/MongoDB/Server/4.0/bin/mongodump.exe'

alias wpcron='ssh ist-wp-cmd-te1.bu.edu'
alias startmydevbox='aws ec2 start-instances --instance-ids i-0f0f920848adfa906'
alias stopmydevbox='aws ec2 stop-instances --instance-ids i-0f0f920848adfa906'

alias wpbuild='ssh wrh@ist-wp-bld-pr01'
alias wptest1='ssh webteam@ist-wp-app-test101.bu.edu'
alias wptest2='ssh webteam@ist-wp-app-test102.bu.edu'
alias wpdev='ssh -i ~/.ssh/webteam_rsa webteam@ist-wp-app-devl101.bu.edu'
alias wpdbtest1='ssh -i ~/.ssh/warren_ist_wp_db_test01_rsa wrh@ist-wp-db-test01.bu.edu'
alias docs='cd /c/whennemuth/bu_workspace/documentation/bu && ls -la'
alias wpcronlogs='MSYS_NO_PATHCONV=1 aws logs --profile=wpcron describe-log-streams --log-group-name=/wordpress/cron/test/job-output'

alias ec2validate='ec2 validate $@'
alias ec2upload='ec2 upload $@'
alias ec2create='ec2 create $@'
alias ec2update='ec2 update $@'
alias ec2delete='ec2 delete $@'
alias ec2refresh='ec2 refresh $@'

alias vscode='alias | grep vscode'
alias vscodecore='code --folder-uri "file:///c:/kuali/cor-main"'
alias vscodeportal='code --folder-uri "file:///c:/kuali/research-portal"'
alias vscoderest='code --folder-uri "file:///c:/whennemuth/bu_workspace/documentation/bu/kuali"'
alias vscodecfn='code --folder-uri "file:///c:/whennemuth/workspaces/ecs_workspace/cloud-formation/kuali-infrastructure"'
alias vscodekrd='code --folder-uri "file:///c:/whennemuth/kuali-research-docker"'
alias vscodewpcron='code --folder-uri "file:///c:/whennemuth/workspaces/bu_workspace/bu-wordpress-cron"'
alias vscodemongo='code --folder-uri "file:///c:/whennemuth/bu_workspace/documentation/bu/mongo/script"'
alias vscodeconfig='code --folder-uri "file:///c:/whennemuth/scrap/s3"'
alias vscodekc='code --folder-uri "file:///c:/kuali/kc"'
alias vscodewebdiff='code --folder-uri "file:///c:/whennemuth/workspaces/bu_workspace/bu-webdiff"'
alias vscodevisreg='code --folder-uri "file:///c:/whennemuth/workspaces/bu_workspace/bu-visual-regression"'
alias vscodedummymetrics='code --folder-uri "file:///c:/whennemuth/workspaces/ecs_workspace/dummy-metrics"'

alias dangling='docker rmi $(docker images --filter dangling=true -q) 2> /dev/null'
alias jenkins2='export BASE_DIR='$KUALI_INFRASTRUCTURE'; sh $BASE_DIR/scripts/shell.sh jenkins profile=infnprd '$@
alias jenkinsui="explorer "$(aws --profile=infnprd cloudformation describe-stacks --stack-name kuali-jenkins --output text --query 'Stacks[].Outputs[?OutputKey == `JenkinsPrivateUrl`].{val:OutputValue}' 2> /dev/null)""
alias ec2='export BASE_DIR='$KUALI_INFRASTRUCTURE'; sh $BASE_DIR/scripts/shell.sh app profile=infnprd '$@
alias ec2any='export BASE_DIR='$KUALI_INFRASTRUCTURE'; sh $BASE_DIR/scripts/shell.sh any profile=infnprd '$@
alias mongo2='export BASE_DIR='$KUALI_INFRASTRUCTURE'; sh $BASE_DIR/scripts/tunnel.sh mongo profile=infnprd '$@
alias oracle='export BASE_DIR='$KUALI_INFRASTRUCTURE'; sh $BASE_DIR/scripts/tunnel.sh any profile=infnprd '$@

alias kcbuild='sh '$BASH_SCRIPTS'/alias.exclude/dependency-check.sh /c/kuali/kc'
alias fl='federatedLogin '$@

alias LEGACY='export AWS_PROFILE=legacy'
alias kcconfig='sh '$BASH_SCRIPTS'/alias.exclude/upload.config.to.s3.sh "all" '$@
alias kcconfig-dryrun='sh '$BASH_SCRIPTS'/alias.exclude/upload.config.to.s3.sh "all" "dryrun=true" '$@
alias kcconfig-infnprd='sh '$BASH_SCRIPTS'/alias.exclude/upload.config.to.s3.sh "all" "profile=infnprd" '$@
alias kcconfig-infnprd-dryrun='sh '$BASH_SCRIPTS'/alias.exclude/upload.config.to.s3.sh "all" "profile=infnprd" "dryrun=true" '$@

