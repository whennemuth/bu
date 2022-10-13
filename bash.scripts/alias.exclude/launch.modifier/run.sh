#!/bin/bash
#
# Run this script to create output to manually add to your eclipse (Mars version) launch configuration
# in order to specify a large list of jars that will be referenced for source code.
# This corresponds to what you would enter manually in the "Source" tab of the launch configuration.
# The launch configuration file is located at:
#      workspace\.metadata\.plugins\org.eclipse.debug.core\.launches\
# The output file contains content that can be injected at the end of the following element:
#      <stringAttribute key="org.eclipse.debug.core.source_locator_memento"
# Specifically, just before:
#      /sourceContainers&gt;&#13;&#10;&lt;/sourceLookupDirector&gt;&#13;&#10;"/>
# To run
#   cd [this directory]
#   source run.sh
#   execute [.m2 subfolder] [filter] [workspace] [launch name]
# where 
#   1) ".m2 subfolders" is the subdirectory that you want to recurse through to locate source jars.
#   2) "filter" is a string you want to filter for in the name or path of the source jar file.
#   3) "workspace" is the path to your eclipse workspace directory
#   4) "launch name" is the name of your launch configuration
# Example: to get all rice source jars for version 1705.0034 for a workspace c:\myworkspace and launch configuration "mylaunchconfig": 
#    execute ~/.m2/repository/org/kuali/rice 1705.0034 c:\myworkspace mylaunchconfig

# misc.sh should automatically have been sourced by .bashrc
# source ~/bash.scripts/alias.include/misc.sh

execute() {

  clean

  local m2=$(convertToUnixPath $1)
  local filter=$2
  local workspace=$(convertToUnixPath $3)
  local launchname=$4

  ! makeJarList1 $m2 $filter && return 1

  ! makeJarList2 && return 1

  ! makeJarList3 && return 1

  ! makeLaunchFile $workspace $launchname && return 1
}

# This is an example of what you would enter on the command line if you wanted to add source jars
# To a debug configuration called "coeus-webapp", in the "C:\whennemuth\workspaces\kuali_workspace"
# eclipse workspace, where the jars path or name contains the expression "1705.0004" and are 
# searched for recursively in the directory ~/.m2/repository/org/kuali/rice
executeShortcut() {
  execute \
    ~/.m2/repository/org/kuali/rice \
    1705.0004 \
    'C:\whennemuth\workspaces\kuali_workspace' \
    coeus-webapp
}

clean() {
  LAUNCH_ERROR=""
  rm -f jar.list.1
  rm -f jar.list.2
  rm -f jar.list.3
  rm -f launch.1
  rm -f launch.2
  rm -f launch.3
}

makeJarList1() {
  if [ ! -d $1 ] ; then
    echo "The specified directory to find jars does not exist: $1"
    false
    return 1
  fi
  find $1 -type f -iname *.jar | grep sources.jar | grep $2 > jar.list.1
  true
}

makeJarList2() {
  if [ ! -f jar.list.1 ] ; then
    echo "Cannot create jar.list.2 because jar.list.1 does not exist!"
    false
    return 1
  fi
  cp jar.list.1 jar.list.2
  convertToWindowsPath jar.list.2 file
}

makeJarList3() {
  if [ ! -f jar.list.2 ] ; then
    echo "Cannot create jar.list.3 because jar.list.2 does not exist!"
    false
    return 1
  fi
  echo -n "" > jar.list.3
  local temp="$(tr -d '\n' < ~/bash.scripts/alias.exclude/launch.modifier/template | tr -d '\r')"
  while read -r jar; do
    local jar2=$(echo $jar | sed 's/\\/\\\\/g')
    local jarstring="$(echo -n $temp | sed s/INSERT_PATH/$jar2/)"
    echo -n $jarstring >> jar.list.3
  done <jar.list.2
}

makeLaunchFile() {
  if [ ! -f jar.list.2 ] ; then
    echo "Cannot launchfile because jar.list.3 does not exist!"
    false
    return 1
  elif [ ! -d $1 ] ; then
    echo "Cannot find the specified eclipse workspace directory: $1"
    false
    return 1
  elif [ -z "$2" ] ; then
    echo "Missing parameter! What is the name of launch configuration?"
    false
    return 1
  else
    local launchfile="$1/.metadata/.plugins/org.eclipse.debug.core/.launches/${2}.launch"
    if [ ! -f $launchfile ] ; then 
      echo "Bad parameter! No launch configuration can be found as specified: $2";
      false
      return 1
    fi
  fi
  
  rm -f launch.1
  rm -f launch.2
  rm -f launch.3
 
  cp $launchfile launch.1

  endtags='\/sourceContainers&gt;&#13;&#10;&lt;\/sourceLookupDirector&gt;&#13;&#10;'
               
  # Putting a newline directly before endtags expression
  sed -i "s/$endtags/###MARKER###\\n&/" launch.1

  # Using very obscure feature of sed, the r flag with -e switch.
  # It inserts the content of the jar.list.3 file directly at the 
  # beginning of the line found to contain the endtags expression.
  insertbefore='<stringAttribute key="org.eclipse.debug.core.source_locator_memento"'
  sed -e "/$insertbefore/rjar.list.3" launch.1 > launch.2

  # Remove the newline added earlier.
  local lastline=""
  local thisline=""
  while read -r line; do
    if [ -n "$(echo $line | grep '###MARKER###')" ] ; then
      # This line needs to sit on the same line as the next line.
      # Effectively removes the newline character between two lines.
      lastline="$line"
    else
      echo $lastline$line | sed 's/###MARKER###//' >> launch.3
      lastline=""
    fi
  done <launch.2

  cp launch.3 "$(echo $launchfile | sed s/\.launch$/-with-sources.launch/)"
}
