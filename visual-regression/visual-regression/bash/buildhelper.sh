#!/bin/bash

source utils.sh

parseargs() {
  echo "Parsing arguments..."

  # Blank out prior settings.
  posargs=()
  task=""
  gitRepo=""
  gitUser=""
  gitPswd=""

  while (( "$#" )); do
    case "$1" in
      -t|--task)
        eval "$(parsearg $1 $2 'task')" ;;
      -r|--git-repo)
        eval "$(parsearg $1 $2 'gitRepo')" ;;
      -u|--git-user)
        eval "$(parsearg $1 $2 'gitUser')" ;;
      -p|--git-pswd)
        eval "$(parsearg $1 $2 'gitPswd')" ;;
      -h|--help)
        printusage
        exit 0
        ;;
      -*|--*=) # unsupported flags
        echo "Error: Unsupported flag $1" >&2
        printusage
        exit 1
        ;;
      *) # preserve positional arguments (should not be any more than the leading command, but collect then anyway)
        posargs=(${posargs[@]} $1)   
        shift
        ;;
    esac
  done
}

printusage() {
  cat <<EOF

USAGE: sh task.sh [OPTIONS]  
  Options:
    -t, --task              pull-node, pull-java:
    -h, --help              Print this message.
    -g, --git-repo          The url of the github repository to clone.
    -u, --git-user          The user for both github repository.
    -p, --git-pswd          The password for the git user.
  
  Examples:

    sh task.sh \
      --task pull-node \
      --git-repo https://github.com/bu-ist/bu-webdiff.git
      --git-user myself \
      --git-pswd myPassw0rd123
EOF
}

getGitUrl() {
  # Temporarily remote the protocol (http or https)
  local repo="$(echo $gitRepo | sed 's/https:\/\///' | sed 's/http:\/\///')"
  local encodePswd="$(echo -ne $gitPswd | xxd -plain | tr -d '\n' | sed 's/\(..\)/%\1/g')" 
  echo "https://$gitUser:$encodePswd@$repo"
}

dotask() {

  parseargs $@

  case "$task" in
    pull-java)
      git clone "$(getGitUrl)" "basket" ;;
    pull-node)
      git clone "$(getGitUrl)" "webdiff" ;;
    *)
      if [ -z "$task" ] ; then
        echo "Task not specified!"
      else
        echo "No such task: \"$task\""
      fi
      printusage
      ;;
  esac
}