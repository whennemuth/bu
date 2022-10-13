#!/bin/bash

# -----------------------------------------------------------------------------------------------------------------------------
# This functionality traverses down a directory tree, renaming any .git directory it finds to "dot_git"
# This attempts to bypass the problem introduced by nested git repositories.
# Git will ignore the contents of nested git repositories when adding content to the staging area of the top-level repository.
# Submodules will not solve this simply since they require:
#   1) The existence of content in separate repositories 
#   2) Cloners must be able to access each submodule git repository
#   3) Cloners must also perform git submodule init, and git submodule update commands
# -----------------------------------------------------------------------------------------------------------------------------

renameDotGit() {
  local state="$1"
  case "${state,,}" in
    on)
      local findname1='.git'
      local findname2='\.git'
      local swapname='dot_git'
      ;;
    off)
      local findname1='dot_git'
      local findname2='dot_git'
      local swapname='\.git'
      ;;
  esac
  find . \
    -type d \
    -name $findname1 \
    -not -path './.git' \
    | \
    while read path ; do
      local newpath="$(echo "$path" | sed 's|'$findname2'|'$swapname'|')"
      echo "mv $path $newpath"
      [ "${2,,}" == 'dryrun' ] && continue;
      eval "mv $path $newpath"
    done;
}

workspacebackup() {

  if [ -z "$(git status -s -z)" ] ; then
    echo "Nothing to commit!"
  fi

  renameDotGit 'on'

  git_ssh /c/whennemuth/workspaces/bu_workspace github_id_rsa github
  git add --all
  git commit -m 'routine commit'
  git push github master

  renameDotGit 'off'
}
