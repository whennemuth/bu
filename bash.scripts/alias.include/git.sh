git_ssh(){
  local path="$1"   # path of local git repo
  local key="$2"    # name of ssh private key
  local remote="$3" # name of the git remote
  local repo="$4"   # name of the git repository
  if [ -z "$repo" ] ; then
    repo="$(git remote get-url --push $remote 2> /dev/null)"
    [ -z "$repo" ] && echo "ERROR! Could not establish repository" && return 1
  else
    repo="git@github.com:$repo"
  fi
  eval "ssh-agent -k"
  cd $path
  eval `ssh-agent -s`
  ssh-add ~/.ssh/$key
  ssh -T git@github.com
  if [ -z "$(git remote | grep -P ^$remote\$)" ] ; then
    git remote add $remote $repo
  fi
}
