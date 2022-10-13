#!/bin/bash

windows() {
  [ -n "$(ls /c/ 2> /dev/null)" ] && true || false
}

getPwdForMount() {
  if windows ; then
    echo $(pwd | sed 's/\/c\//C:\//g' | sed 's/\//\\\\/g')\\\\
  else
    echo "$(pwd)/"
  fi
}