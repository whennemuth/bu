#!/bin/bash
#
# requires misc.sh

# Compile one .java file in the coeus-impl codebase into its maven-built target directory.
# Maven takes too long to build the whole project for the sake of a single file.
# This requires that maven has built the coeus-webapp module so that a WEB-INF/lib
# directory exists and has all the jar files needed for the javac classpath to be satisfied.
#
compile_one() {

  local JAVAFILE="$1"
  if [ -z "$JAVAFILE" ] ; then
    read -p "Type the name of the java file to compile: " JAVAFILE
    # Remove returns and newlines that come from read function.
    JAVAFILE="$(echo $JAVAFILE | tr -d '\r')"
    JAVAFILE="$(echo $JAVAFILE | tr -d '\n')"
  fi

  # Hard-coded variables (modify if necessary).
  local TAG=1705.0034-SNAPSHOT
  local KC='/c/whennemuth/workspaces/kuali_workspace_remote/kuali-research'
  local TEST="$2"

  # Composed variables (assumed correct by following maven/tomcat convention).
  local WEBAPP=$KC'/coeus-webapp'
  local LIB=$WEBAPP'/target/coeus-webapp-'$TAG'/WEB-INF/lib'
  local CLAZZPATH=$LIB'/*'
  local IMPL=$KC'/coeus-impl'
  if [ -z "$TEST" ] ; then
    local IMPL_SRC=$IMPL'/src/main/java'
    local IMPL_TARGET=$IMPL'/target/classes'
  else
    local IMPL_SRC=$IMPL'/src/test/java'
    local IMPL_TARGET=$IMPL'/target/test-classes'
  fi
   
  # Even if on windows, find is a unix function, so don't convert $IMPL_SRC to a windows path yet.
  # NOTE: Use of find assumes one search result (more than one is possible, which will cause a failure).
  local CLASS_TO_COMPILE=$(find $IMPL_SRC -type f -iname "$JAVAFILE.java")

  if [ "$(windows)" == "true" ] ; then
    IMPL_TARGET=$(convertToWindowsPath "$IMPL_TARGET")
    CLAZZPATH=$(convertToWindowsPath "$CLAZZPATH")
    CLASS_TO_COMPILE=$(convertToWindowsPath "$CLASS_TO_COMPILE")
  fi

  local CMD=$(cat <<-EOF
  javac \
     -d $IMPL_TARGET \
    -cp $CLAZZPATH \
        $CLASS_TO_COMPILE
EOF
  )

  echo $CMD
  # escape the windows path separarators
  CMD=$(echo $CMD | sed 's/\\/\\\\/g')
  eval $CMD
}


# The java file to compile is probably a junit test, which means a different src and target folder.
compile_one_test() {
  local JAVAFILE="$1"
  if [ -z "$JAVAFILE" ] ; then
    read -p "Type the name of the java file to compile: " JAVAFILE
    # Remove returns and newlines that come from read function.
    JAVAFILE="$(echo $JAVAFILE | tr -d '\r')"
    JAVAFILE="$(echo $JAVAFILE | tr -d '\n')"
  fi
  compile_one "$JAVAFILE" "TEST"
}

# Probably not the best way to test this, but assume that if one is running in a 
# windows system they are somewhere on a "c" drive.
windows() {
  [ -n "$(pwd | grep -iP '^(c:)|(/c/).*$')" ] && echo "true"
}
