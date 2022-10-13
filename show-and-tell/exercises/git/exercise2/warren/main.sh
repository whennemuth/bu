#!/usr/bin/env bash

reset() {
  local currentdir="$(echo -n "$(pwd)" | sed 's/\//\n/g' | tail -n1)"
  case $currentdir in
    bugaboo)
      cd .. ;;
    warren)
      # do nothing
      ;;
    *)
      echo "Where the hell are you?"
      exit 1
      ;;
  esac

  rm -rf bugaboo 2> /dev/null
  mkdir bugaboo
  cd bugaboo
  git init

  cat <<EOF > bedbug.sh
#!/bin/bash

howManyInsects() {
  local totalbugs=0
  echo 'Enter some insects: '
  while read bugs ; do
    [ ! "\$bugs" ] && break
    totalbugs=\$((\$totalbugs+\$(echo "\$bugs" | grep -Po '\d+')))
  done
  echo "You have \$totalbugs total insects!"
}

howManyInsects
EOF
  git add bedbug.sh
  git commit -m "Initial commit"
}

buryTheBug() {
  cd bugaboo 2> /dev/null
  local bugdepth=$1
  [ -z "$bugdepth" ] && bugdepth=$(shuf -i 1-100 -n 1)
  for n in {1..100} ; do
    local msg="This is the commit number $n"
    if [ $n -eq $bugdepth ] ; then
      # Introduce the bug
      sed -i 's/totalbugs=0/totalbugs=1/' bedbug.sh
      msg="$msg (this is the bug commit!)"
    fi
    echo "# $msg" >> bedbug.sh
    git commit -a -m "$msg"
  done
}

digUpTheBug() {
  if ! isBash ; then
    return 1
  fi
  cd bugaboo 2> /dev/null
  local thiscommit=$(git rev-parse HEAD)
  git bisect start
  git bisect bad
  git bisect good $(git rev-parse HEAD~100)
  while true ; do
    if isBuggy ; then
      local info=$(git bisect bad)
    else
      local info=$(git bisect good)
    fi
    echo "$info"
    if [[ "$info" == *"first bad commit"* ]] ; then
      break;
    fi
  done
  # git bisect log
  git bisect reset
}

isBuggy() {
  cd bugaboo 2> /dev/null
  if ! isBash ; then
    return 1
  fi
  local correctAnswer=6
  local providedAnswer=$(bash bedbug.sh < <(echo "1 and" && echo "2 grasshoppers" && echo "3 fleas") | grep -Po '\d+')
  [ $providedAnswer -ne $correctAnswer ]
}

rebaseAFix() {
  echo ''
}

interpreter="$(ps -p $$ | awk '{print $8}' | tail -n 1 | sed 's/\//\n/g' | tail -n 1)"
isBash() {
  case "$interpreter" in
    bash) ;;
    sh) echo  'Please use bash, not sh' && interpreter="" ;;
    *) echo 'WARNING: Unidentified command interpreter.' ;;
  esac
  [ "$interpreter" == "bash" ] && true || false
}

task=$1
shift

case "${task,,}" in
  reset) reset ;;
  burythebug) buryTheBug ;;
  digupthebug) digUpTheBug ;;
  isbuggy) isBuggy && echo 'Yes, it is buggy' || echo 'No, it is not buggy' ;;
  all) reset && buryTheBug && digUpTheBug ;;
esac