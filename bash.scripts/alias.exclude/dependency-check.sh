#!/bin/bash

checkLocation() {
  [ ! -f pom.xml ] && echo "No pom file in current directory!" && exit 1
  [ ! -d coeus-impl ] && echo "You do not appear to be at the root of project!" && exit 1
}

printStart() {
  echo " "
  echo "----------------------------------------------------------------------------"
  echo "          M2 DEPENDENCY CHECK (schemaspy, rice, coeus-api, s2sgen)"
  echo "----------------------------------------------------------------------------"
  echo " "
  echo "1) Analyzing pom for versions..."
}

printEnd() {
  echo " "
  echo "----------------------------------------------------------------------------"
  echo "                      FINISHED M2 DEPENDENCY CHECK"
  echo "----------------------------------------------------------------------------"
  echo " "
}

findVersions() {
  # Get the content of the pom file with all return/newline characters removed.
  content=$(cat pom.xml | sed ':a;N;$!ba;s/\n//g')

  # Get versions of dependencies, use a zero width lookbehind for the open element and capture 
  # all following characters thereafter until a closing element character is encountered
  
  schemaspy_version=$(echo "$content" | grep -Po '(?<=<schemaspy\.version>)([^<]+)' || true)
  echo "schemaspy version: ${schemaspy_version}"
  
  rice_version=$(echo "$content" | grep -Po '(?<=<rice\.version>)([^<]+)' || true)
  echo "rice version: ${rice_version}"
  
  api_version=$(echo "$content" | grep -Po '(?<=<coeus\-api\-all\.version>)([^<]+)' || true)
  echo "coeus-api version: ${api_version}"
  
  s2sgen_version=$(echo "$content" | grep -Po '(?<=<coeus\-s2sgen\.version>)([^<]+)' || true)
  echo "s2sgen version: ${s2sgen_version}"

  research_resources_version=$(echo "$content" | grep -Po '(?<=<research\-resources\.version>)([^<]+)' || true)
  echo "research-resources version: ${research_resources_version}"
}

windows() {
  [ -n "$(ls /c/ 2> /dev/null)" ] && true || false
}

setLocalRepo() {
  # If the local repo location has been customized in settings.xml, then we need to parse it from maven help plugin output.
  repo=$(mvn help:effective-settings | grep 'localRepository' | cut -d '>' -f 2 | cut -d '<' -f 1)
  if windows ; then
    repo=$(echo "$repo" | sed 's|\\|/|g' | sed 's|C:|/c|')
  fi
}

organizeVersions() {
  echo " "

  setLocalRepo

  echo "2) Searching $repo for dependencies installed for above versions..."

  # file extension, group, version, artifactid, parent_artifactid, job
  m2_items=(
    "jar,co/kuali/schemaspy,${schemaspy_version},schemaspy,schemaspy,schemaspy"
    # "jar,org/kuali/rice,${rice_version},rice-archetype-quickstart,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-core-api,rice,kc-rice"
    # "jar,org/kuali/rice,${rice_version},rice-db-config,rice,kc-rice"
    # "jar,org/kuali/rice,${rice_version},rice-deploy,rice,kc-rice"
    # "jar,org/kuali/rice,${rice_version},rice-development-tools,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-impex-client-bootstrap,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-impex-master,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-impex-server-bootstrap,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-impl,rice,kc-rice"
    # "jar,org/kuali/rice,${rice_version},rice-it-config,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-ken-api,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-kew-api,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-kim-api,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-kns,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-krad-app-framework,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-krms-api,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-ksb-api,rice,kc-rice"
    # "jar,org/kuali/rice,${rice_version},rice-legacy-web,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-location-api,rice,kc-rice"
    # "war,org/kuali/rice,${rice_version},rice-serviceregistry,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-sql,rice,kc-rice"
    "war,org/kuali/rice,${rice_version},rice-standalone,rice,kc-rice"
    # "jar,org/kuali/rice,${rice_version},rice-tools-test,rice,kc-rice"
    "war,org/kuali/rice,${rice_version},rice-web,rice,kc-rice"
    "jar,org/kuali/rice,${rice_version},rice-xml,rice,kc-rice"
    "jar,org/kuali/coeus,${api_version},coeus-api-all,coeus-api,kc-api"
    "jar,org/kuali/coeus,${s2sgen_version},coeus-s2sgen-api,coeus-s2sgen,kc-s2sgen"
    "jar,org/kuali/coeus,${s2sgen_version},coeus-s2sgen-impl,coeus-s2sgen,kc-s2sgen"
    "jar,org.kuali.research,${research_resources_version},research-resources,research-resources,research-resources"
  )
}

buildMissingVersions() {
  projects_to_build=()
  git_tags=()

  for i in ${m2_items[@]}; do

    IFS=',' read -ra parts <<< "${i}"

    ext=${parts[0]}
    group=${parts[1]}
    version=${parts[2]}
    artifactid=${parts[3]}
    parentartifactid=${parts[4]}
    project=${parts[5]}
    
    [ -z "$version" ] && continue

    artifact="${repo}/${group}/${artifactid}/${version}/${artifactid}-${version}.${ext}"
    if [ -f $artifact ] ; then
      echo "found: ${artifact}";
    else
      echo "MISSING: ${artifact}";
      if [ -z "$(echo ${projects_to_build[*]} | grep ${project})" ] ; then
        projects_to_build+=(${project});
        git_tags+=("${parentartifactid}-${version}");
      fi
    fi
  done

  echo " "

  if [ ${#projects_to_build[@]} -eq 0 ] ; then
    echo "All dependencies accounted for";
  else
    echo "DEPENDENCIES MISSING. Must build the following: ${projects_to_build[*]}";
    echo " "
    for ((i=0; i<${#projects_to_build[*]}; i++));
    do
      buildProject ${projects_to_build[i]} ${git_tags[i]}
    done
  fi
}

getVersion() {
  local version="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2> /dev/null)"
  if [ -z "$version" ] ; then
    version=$(cat pom.xml | grep -Po '(?<=<version>).*?(?=</version>)' | head -1 2> /dev/null)
  fi
  echo "$version"
}

buildProject() {
  local projectFolder="$1"
  local gitTag="$2"
  local dependencyRoot=$(dirname $(pwd))/$projectFolder
  if [ ! -d "$dependencyRoot" ] ; then
    echo "$dependencyRoot does not exist! Build(s) cancelled."
    exit 1
  fi
  echo "Moving to $dependencyRoot" && cd $dependencyRoot
  local checkedOutVersion=$(getVersion)
  if [ -z "$checkedOutVersion" ] ; then
    echo "WARNING: Could not find version info in $(pwd)/pom.xml"
  fi

  if [ "$gitTag" == "$checkedOutVersion" ] ; then
    echo "Currently checked out version $checkedOutVersion matches desired version..."
  else
    echo "Currently checked out version $checkedOutVersion does not match desired version $version, checking out $gitTag..."
    git checkout $gitTag
  fi

  if [ "$projectFolder" == 'kc-rice' ] ; then
    # For some reason if tests are skipped for the rice build, the rice-core-api jar file is not created and the build fails.
    mvn clean compile install -Dgrm.off=true
  else
    mvn clean compile install -Dgrm.off=true -Dmaven.test.skip=true
  fi
}

rootdir="$1"
shift
[ -z "$rootdir" ] && rootdir=$(pwd)
cd $rootdir
echo "Running from $rootdir"

run() {
  checkLocation

  printStart

  findVersions

  organizeVersions

  buildMissingVersions

  printEnd
}

run