SCRIPTDIR=/var/bash-scripts
[ ! -d $SCRIPTDIR ] && SCRIPTDIR=$(pwd)

source $SCRIPTDIR/bash.lib.sh && \
if [ -f $SCRIPTDIR/build.variables.sh ] ; then
  source $SCRIPTDIR/build.variables.sh 
else
  echo "WARNING! build.variables.sh NOT FOUND!!!"
  echo "The build will fail, but it can at least be sourced"
fi

getCoreSourceFromGit() {
  
  pullCoreSource && \

  pullKualiuiSource && \

  pullCommonSource

  # pullFluffleSource
}


getCoiSourceFromGit() {

  pullCoiSource && \
  
  pullKualiuiSource && \

  pullCommonSource && \

  pullFormbotGadgetsSource && \

  pullFormbotSource
}


getPortalSourceFromGit() {

  pullResearchPortalSource && \

  pullKualiuiSource
}


pullKualiuiSource() {
  setVersionProperty "kuali-ui"
  local npmVersion="$(getPropertyFromFile kuali-ui.version $DEPENDENCY_INFO_FILE)"
  [ ! $npmVersion ] && return 0
  fetchModuleSource "kuali-ui" "master" && \
  setModuleRefspec "kuali-ui" "kuali-ui" && \
  checkoutSource "kuali-ui" "kuali-ui"
}


pullCommonSource() {
  setVersionProperty "common"
  local npmVersion="$(getPropertyFromFile common.version $DEPENDENCY_INFO_FILE)"
  [ ! $npmVersion ] && return 0
  fetchModuleSource "cor-common" "master" && \
  setModuleRefspec "cor-common" "common" && \
  checkoutSource "cor-common" "common"
}


pullFormbotGadgetsSource() {
  setVersionProperty "cor-formbot-gadgets" && \
  local npmVersion="$(getPropertyFromFile cor-formbot-gadgets.version $DEPENDENCY_INFO_FILE)"
  [ ! $npmVersion ] && return 0
  fetchModuleSource "cor-formbot-gadgets" "master" && \
  setModuleRefspec "cor-formbot-gadgets" "cor-formbot-gadgets" && \
  checkoutSource "cor-formbot-gadgets" "cor-formbot-gadgets"
}


pullFluffleSource() {

  # HACK: cor-main asks for a version of fluffle where the major portion is "0". There are no commits in fluffle with a major
  # version less than "1", so semantic versioning will always fail when building cor-main and an attempt is made to aquire a
  # version of fluffle from the registry that was never published having the same major version.
  # Changing the cor-main dependency entry for fluffle to be ^1.0.0. The most recent commit in the fluffle git repo that has
  # the same major version, "1", will be published. Remove this hack when cor-main has a proper entry for fluffle.
  changeDependencyVersion package=@kuali/fluffle dependencyGroup=dependencies pkgJsonFile=/var/core/package.json newVersion=^1.0.0 && \
  
  setVersionProperty "fluffle"
  local npmVersion="$(getPropertyFromFile fluffle.version $DEPENDENCY_INFO_FILE)"
  [ ! $npmVersion ] && return 0
  fetchModuleSource "fluffle" "master" && \
  setModuleRefspec "fluffle" "fluffle" && \
  checkoutSource "fluffle" "fluffle"
}


# Formbot is a little different in that the main module is not installed and built by npm, but instead a group
# of sub-modules in its packages directory are installed and built as these are the artifacts that are 
# referenced in the nodejs applications package.json file.
pullFormbotSource() {
  fetchModuleSource "formbot.parent" "master" && \
  checkoutGitBranch branchname=master refspec=FETCH_HEAD
  if [ $? -gt 0 ] || [ ! -f $DEPENDENCY_DIR/formbot.parent/package.json ] ; then
    echo "ERROR! Failed to pull formbot module from github"
    return 1
  fi
  
  setVersionProperty  "formbot" && \
  setVersionProperty  "formbot-react" && \
  setVersionProperty  "formbot-react-components" && \
  setVersionProperty  "formbot-validation" && \
  setVersionProperty  "gadgets-preset-basic" && \
  setVersionProperty  "gadgets-preset-basic-impl" && \
  setVersionProperty  "gadgets-preset-layout" && \
  \
  setModuleRefspec \
    "formbot.parent/packages/formbot" \
    "formbot" \
    "packages/formbot/package.json" && \
  setModuleRefspec \
    "formbot.parent/packages/formbot-react" \
    "formbot-react" \
    "packages/formbot-react/package.json" && \
  setModuleRefspec \
    "formbot.parent/packages/formbot-react-components" \
    "formbot-react-components" \
    "packages/formbot-react-components/package.json" && \
  setModuleRefspec \
    "formbot.parent/packages/formbot-validation" \
    "formbot-validation" \
    "packages/formbot-validation/package.json" && \
  setModuleRefspec \
    "formbot.parent/packages/gadgets-preset-basic" \
    "gadgets-preset-basic" \
    "packages/gadgets-preset-basic/package.json" && \
  setModuleRefspec \
    "formbot.parent/packages/gadgets-preset-basic-impl" \
    "gadgets-preset-basic-impl" \
    "packages/gadgets-preset-basic-impl/package.json" && \
  setModuleRefspec \
    "formbot.parent/packages/gadgets-preset-layout" \
    "gadgets-preset-layout" \
    "packages/gadgets-preset-layout/package.json"

  # There's no single refspec that can be checked out here - a checkout for each *_GIT_REFSPEC must occur 
  # just before the npm install and build for the corresponding module, so it cannot be done here and must wait for later.
}


pullCoreSource() {
  pullAppSource
}

pullCoiSource() {
  pullAppSource
}

pullResearchPortalSource() {
  pullAppSource
}


# Pull the specified branch and checkout the specified refspec from the git repository of the nodejs application.
pullAppSource() {
  gitpull \
    "BASEDIR=$NODEAPP_ROOT_DIR" \
    "BRANCH=$GIT_BRANCH" \
    "REFSPEC=$GIT_REFSPEC"

  if [ $? -gt 0 ] || [ ! -f "$NODEAPP_ROOT_DIR/package.json" ] ; then
    echo "ERROR! Cannot pull core source code from git repository"
    return 111
  fi
}


# Determine the version of a specified dependency that node application needs and write it out to a properties file.
setVersionProperty() {
  local pkgName="$1"
  local dependencyType="$2"
  [ -z "$dependencyType" ] && dependencyType="dependencies"
  
  cd $NODEAPP_ROOT_DIR

  for pkgJson in $(find . -iname package.json -type f) ; do
    echo "Checking $pkgJson for $pkgName in $dependencyType ..."

    eval "local version=$(node \
      -pe \
      "JSON.parse(process.argv[1]).${dependencyType}.myversion" \
      "$(cat $pkgJson | sed "s/@kuali\/${pkgName}/myversion/g")" 2> /dev/null)"

    if [ $? -gt 0 ] || [ -z "$version" ] ; then
      echo "WARNING! cannot parse $pkgJson file for $dependencyType"
      continue
    elif [ "$version" == "undefined" ] ; then
      continue
    elif [ -n "$version" ] ; then
      echo "INFO: Found that $NODEAPP_NAME needs version $version of @kuali/$pkgName"
      echo "${pkgName}.version=${version}" >> $DEPENDENCY_INFO_FILE
      # Breaking here is arbitrary. It may not necessarily be true that the first package.json
      # file found with an entry for the specified package has that packages correct version?
      break;
    fi
  done

  if [ "$version" == "undefined" ] || [ -z "$version" ] ; then
    if [ "$dependencyType" == "dependencies" ] ; then
      echo "Cannot find @kuali/$pkgName in dependencies. Checking devDependencies..."
      setVersionProperty "$pkgName" "devDependencies"
    else
      echo "WARNING! It appears that @kuali/$pkgName is no longer a required dependency."
    fi
  fi
}


# Fetch a specified branch of a module from github.
fetchModuleSource() {
  local pkgDir="$1"
  local branch="$2"
  gitfetch \
    "BASEDIR=$DEPENDENCY_DIR/$pkgDir" \
    "BRANCH=$branch"

  if [ $? -gt 0 ] ; then
    echo "ERROR! Cannot fetch $pkgDir source code from git repository!"
    return $?
  fi
}


# Add a variable to a properties file for the most recent git commit SHA in the repo for the 
# specified module that lists this version for @kuali/$pkgName in its package.json file.
setModuleRefspec() {
  local pkgDir="$1"
  local pkgName="$2"
  local packageDotJson="$3"
  local npmVersion="$(getPropertyFromFile ${pkgName}.version $DEPENDENCY_INFO_FILE)"

  cd $DEPENDENCY_DIR/$pkgDir
  local refspec=$(getGitRefForNpmVersion "$npmVersion" "FETCH_HEAD" "$packageDotJson")
  
  if [ ! $? -eq 0 ] || [ -z "$refspec" ] ; then
    echo "ERROR! Cannot find correct git revision for version $npmVersion of $pkgDir"
    return $?
  else
    echo "${pkgName}.refspec=${refspec}" >> $DEPENDENCY_INFO_FILE
    echo "${pkgName}.directory=$DEPENDENCY_DIR/$pkgDir" >> $DEPENDENCY_INFO_FILE
  fi
}


# Checkout the specified refspec from the fetched module branch.
checkoutSource() {
  local pkgDir="$1"
  local pkgName="$2"
  local npmVersion="$(getPropertyFromFile ${pkgName}.version $DEPENDENCY_INFO_FILE)"
  # Remove characters that are illegal in git branch names and replace with their english pronounciations (preserves semver meaning).
  npmVersion=$(echo "$npmVersion" \
    | sed 's/\~/tilda/g' \
    | sed 's/\^/caret/g' \
    | sed 's/>\=/gte/g' \
    | sed 's/>/gt/g' \
    | sed 's/\=/eq/g' \
    | sed 's/[^0-9a-zA-Z\.\-\+]//g')
  local refspec="$(getPropertyFromFile ${pkgName}.refspec $DEPENDENCY_INFO_FILE)"

  checkoutGitBranch "branchname=$pkgName@$npmVersion" "refspec=$refspec"

  if [ $? -gt 0 ] ; then
    echo "ERROR! Problem checking refspec "$refspec" to new branch "$BRANCH" from $pkgDir git repository!"
    return $?
  elif [ ! -f "$DEPENDENCY_DIR/$pkgDir/package.json" ] ; then
    echo "ERROR! No package.json file found in source checked out from $pkgDir. Did you fetch from the correct repository?"
  fi
}


doNpmActions() {

  local line="---------------------------------------------------------------------------"
  local indent="    "

  for argpair in "$@" ; do

    local action=$(echo "${argpair,,}" | cut -d ':' -f1)
    local pkgpath=$(echo "$argpair" | cut -d ':' -f2)

    printf "\n\n\n%s\n%s%s%s\n%s\n\n" \
      "$line" \
      "$indent" \
      "START ${action^^}: " \
      "$pkgpath" \
      "$line"

    case $action in
      'install')
        _install rootpath=$pkgpath ;;
      'build')
        _build rootpath=$pkgpath ;;
      'publish')
        _publish rootpath=$pkgpath ;;
    esac

    local retval=$?
    [ $retval -gt 0 ] && [ $retval -ne 999 ] && local result="FAILED" || local result="SUCCESS"

    printf "%s\n%s%s%s\n%s\n" \
      "$line" \
      "$indent" \
      "${action^^} $result! " \
      "$pkgpath" \
      "$line"

  done
}


_install() {
  echo "_install($@)"
  _prepare "$@"
  local retval=$?
  # Cancel all install if non-zero return (cancels build and publish as well if they are up the call stack)
  [ $retval -eq 999 ] && return 999 # Already published
  [ $retval -gt 0 ] && return 1

  echo "Installing $@ ..."
  removePackageFromDependencies "package=@kuali/utils" "dependencyGroup=devDependencies" "pkgJsonFile=$(pwd)/package.json"
  if YarnInstalled ; then
    # If the module that installs the current module as a dependency uses yarn, it will look at its optional 
    # dependencies. That install will fail if optionalDependencies cannot be found.
    # For some reason, using the yarn --ignore-optional flag does not work (maybe it's not recursive).
    removePackageFromDependencies "package=@kuali/utils" "dependencyGroup=optionalDependencies" "pkgJsonFile=$(pwd)/package.json"
  fi
  removeScriptFromScripts postinstall
  disablePackageLocking
  if [ ! -f $DEPENDENCY_DIR/publish.nested.in.progress ] ; then
    _publishNestedDependencies dependencyType=devDependencies
  fi
  npm install --no-optional
}

_build() {
  echo "_build($@)"
  _install "$@"
  local retval=$?
  # Cancel all install if non-zero return (cancels publish as well if it is up the call stack)
  [ $retval -eq 999 ] && return 999 # Already published
  [ $retval -gt 0 ] && return 1

  echo "Building $@ ..."
  if hasScript "build" "$(pwd)" ; then
    echo "Running build script for $(pwd)/package.json"
    npm run build
  else
    echo "No build script found for: $(pwd)/package.json"
  fi
}

_publish() {
  echo "_publish($@)"
  _build "$@"
  local retval=$?
  [ $retval -eq 999 ] && return 999 # Already published
  [ $retval -gt 0 ] && return 1
  removeScriptFromScripts prepublishOnly
  removeScriptFromScripts prepare
  removeScriptFromScripts postpublish
  echo "Publishing $@ ..."
  npm publish
}


# Prepare for running an npm install, build, publish command(s).
# This includes 
#   1) Getting into the correct directory.
#   2) Making sure the correct git reference is checked out.
#   3) Logging into the local npm registry.
#   4) Determine if the applicable npm package is already published (and cancelling if it is.)
# ARGS:
#     rootpath: The root directory of the package
#      pkgName: The name of the package as declared in its package.json file
#   npmVersion: The version of the package (may not be satisfied by what's checked out).
#      refspec: The git reference to checkout of git.
_prepare() {

  echo "_prepare($@)"

  eval "$(parseargs $@)"

  # The provided rootpath can be ommitted (defaults to pwd), be relative to DEPENDENCY_DIR, or be an absolute path
  # Of the the 3 forms, convert to the 3rd (absolute)
  if [ -n "$rootpath" ] ; then
    local len=$(printf "$DEPENDENCY_DIR/" | wc -m)
    local beginning=${rootpath:0:$len}
    if [ ! "$beginning" == "$DEPENDENCY_DIR/" ] ; then
      rootpath="$DEPENDENCY_DIR/$rootpath"
    fi
  else
    rootpath=$(pwd)
  fi
  cd $rootpath

  [ -z "$pkgName" ] && local pkgName=$(node -pe "JSON.parse(process.argv[1]).name" "$(cat package.json)")
  [ "$pkgName" == 'undefined' ] && pkgName=""
  pkgName="$(echo $pkgName | sed -e 's/@kuali\///g')" # Remove the scope portion
  if [ -n "$pkgName" ] ; then

    [ -z "$npmVersion" ] && local npmVersion="$(getPropertyFromFile $pkgName.version $DEPENDENCY_INFO_FILE)"
    [ -z "$npmVersion" ] && echo "ERROR! No version set for package: $pkgName" && return 1

    [ -z "$refspec" ] && local refspec="$(getPropertyFromFile $pkgName.refspec $DEPENDENCY_INFO_FILE)"
    [ -z "$refspec" ] && echo "ERROR! No git refspec set for package: $pkgName" && return 1

    local branchname="$pkgName@$npmVersion"
    local fullname="@kuali/$branchname"

    if ! checkoutGitBranch "branchname=$branchname" "refspec=$refspec" ; then
      echo "ERROR! Cannot find git reference $refspec for $branchname"
      return 1
    fi
  fi

  [ -f .npmrc ] && mv -f .npmrc .npmrc.disabled
  [ -f .yarnrc ] && mv -f .yarnrc .yarnrc.disabled

  loginToLocalNpmRegistry

  if [ $? -gt 0 ] ; then
    return 1
  elif [ -n "$pkgName" ] ; then
    if isPublishedToLocalNpmRegistry scope=@kuali module=$pkgName version=$npmVersion ; then
      echo "Already published: $fullname"
      return 999
    fi
  fi

  enableUnsafePerm

  return 0
}


# Publish to the local npm registry versions of @kuali packages indicated in the specified 
# dependencies of a package.json file that are not there already. 
# ARGS:
#          pkgfile: The package.json file to analyze for dependencies to publish
#   dependencyType: The type of dependency (dependencies, devDependencies, optionalDependencies)
#
_publishNestedDependencies() {

  eval "$(parseargs $@)"

  [ -z "$pkgfile" ] && local pkgfile=$(pwd)/package.json

  # Clean up the marker file if it somehow was not removed already.
  local markerfile=$DEPENDENCY_DIR/publish.nested.in.progress
  rm -f $markerfile

  for d in $(getKualiDependencies $pkgfile $dependencyType) ; do
    local fullPkgName=$d
    d="$(echo $d | sed -e 's/@kuali\///g')" # Strip off the @kuali/ scope
    local pkgname=$(echo "$d" | cut -d ':' -f1)
    local version=$(echo "$d" | cut -d ':' -f2)
    if ! isPublishedToLocalNpmRegistry scope=kuali module=$pkgname version=$version semantically='true' ; then
      echo "Version $version of @kuali/pkgname is not satisfied by anything published to the local npm registry"
      local whereYouStarted=$(pwd)
      local whereYouGoing="$(getPropertyFromFile $pkgname.directory $DEPENDENCY_INFO_FILE)"
      if [ -z "$whereYouGoing" ] ; then
        echo "WARNING! No directory entry for $pkgname"
        continue
      fi

      # Switch to the directory of the dependency module and get the proper git ref where the needed version is first encountered.
      cd "$whereYouGoing"
      local refspec="$(getGitRefForNpmVersion $version "FETCH_HEAD" "package.json")"

      # Create a marker file that will be checked so as to prevent recursion.
      echo "Publishing nested dependency:" >> $markerfile
      echo "For: $pkgfile" >> $markerfile
      echo "Type: $dependencyType" >> $markerfile
      echo "Package: $pkgname" >> $markerfile
      echo "Version: $version" >> $markerfile
      
      # Publish the package to satisfy the dependency.
      _publish pkgName=$pkgname npmVersion=$version refspec=$refspec

      # Remove the marker file and go back where you started.
      rm -f $markerfile
      cd $whereYouStarted

    fi
  done
}


disablePackageLocking() {
  if [ -f package-lock.json.disabled ] ; then
    [ -f package-lock.json ]  && rm -f package-lock.json
  elif [ -f package-lock.json ] ; then
    mv package-lock.json package-lock.json.disabled
  fi

  if [ -f yarn.lock.disabled ] ; then
    [ -f yarn.lock ]  && rm -f yarn.lock
  elif [ -f yarn.lock ] ; then
    mv yarn.lock yarn.lock.disabled
  fi

  if packageLockingEnabled ; then
    echo "Disabling package locking globally"
    npm config set package-lock false
  fi
}


packageLockingEnabled() {
  if npmBooleanConfigEnabled "package-lock" ; then true; else false; fi
}

prepublishIgnored() {
  if npmBooleanConfigEnabled "ignore-prepublish" ; then true; else false; fi
}

enableUnsafePerm() {
  if ! npmBooleanConfigEnabled "unsafe-perm" ; then
    echo "Enabling unsafe perm for npm globally"
    npm config set unsafe-perm true
  fi
}

npmBooleanConfigEnabled() {
  local config="$1"
  config=$(echo $config | sed 's/-/\\\\-/g')
  for n in $(npm config ls -l | awk '
    $0 ~ /\s*unsafe-perm\s*=\s*((true)|(false))\s*/ {
      x=$0; gsub(/\s+/,"",x); print x
  }') ;
  do
    [ -z "$(echo $n | grep -i 'overridden')" ] && echo $n | cut -d '=' -f2
  done
  [ "$enabled" == 'true' ] && true || false
}


# Given a directory that contains a package.json file, find out if that file contains a scripts.$scriptName child.
hasScript() {
  local scriptName="$1"
  local moduleDir="$2"

  cd $moduleDir

  local scripts=$(node -pe \
   "var scripts = JSON.parse(process.argv[1]).scripts; \
    for(var s in scripts) { \
      if(s == \"$scriptName\") { \
        console.log(s + \":\" + scripts[s]); \
      } \
    }" \
  "$(cat package.json)")

  # "undefined" generated as one of the results (don't know why), 
  # so strip it out and if there is still something left then return true.
  local script=""
  for s in $scripts ; do
    [ $s != 'undefined' ] && script="$s"
  done
  [ -n "$script" ] && true || false
}


fileExistsInGit() {
  local path="$1"
  local refspec="$2"
  [ -z "$refspec" ] && refspec="HEAD"
  git show "${refspec}:${path}" > /dev/null 2>&1
  [ $? -eq 0 ] && true || false
}


# Checkout from a git repo the specified refspec as a branch with a specified name
# ARGS:
#    branchname: The name of the branch to checkout to
#       refspec: The git reference to checkout
#   removeHooks: [Optional] Clean out the hooks directory of whatever is checked out.
#
checkoutGitBranch() {
  eval "$(parseargs_lowercase $@)"
  local refspecnow="$(git rev-parse HEAD)"

  if [ "${refspec:0:7}" == "${refspecnow:0:7}" ] ; then
    echo "Cancelling checkout of ${refspec}, HEAD is already there."
    true
  else
    if [ "${removeHooks,,}" == "true" ] ; then
      # We don't want any of kualicos git hooks that they configure npm to install, so remove them:
      [ -d .git/hooks ] && rm -f .git/hooks/*
      [ -d ../.git/hooks ] && rm -f ../.git/hooks/*
      [ -d ../../.git/hooks ] && rm -f ../../.git/hooks/*
      [ -d ../../../.git/hooks ] && rm -f ../../../.git/hooks/*
    fi

    if [ -n "$(git diff --stat)" ] ; then
      # Changes to staged files detected (new unstaged files won't show up this way, but we don't care about those).
      git commit --no-verify -a -m "Committing changes to staged files to allow for new branch checkout"
    fi

    git checkout -B $branchname $refspec

    [ $? -eq 0 ] && true || false
  fi
}


# Hack attack! "Fix" the authentication issue (read comments below)
# NOTE: This also requires disabling the decrlfSAMLResponse in the login routes.js file
fixShibbolethWithHack() {

   local SIGXML=/var/core/node_modules/xml-crypto/lib/signed-xml.js
   if [ -f $SIGXML ] ; then
      if [ -z "$(cat $SIGXML | grep 'BU CUSTOMIZATION')" ] ; then
         echo "No BU customization to $SIGXML found. Editing file in place."
         search='\([\t[:space:]\r\n]*\)\([^\t[:space:]]\)\(.*\)digest\x20*!=\x20*ref.digestValue\([^\r\n]*\)'
         replace="\1\/**\n \
            \1* BU CUSTOMIZATION: I am loathe to futz with a 3rd party lib, but shibboleth is sending us a saml response\n \
            \1* whose xml has a digest value with a newline character in it. Stripping this out of the xml in our own core\n \
            \1* codebase before it gets to this lib will fix this digest check, but will cause the signature value\n \
            \1* verification check to fail later on since the signature value was formed from the signature info element that\n \
            \1* contains the faulty digest value and would not compare successfully to that element now that the whitespace\n \
            \1* is stripped out. Got no choice but to fix it here and wait for a shibboleth patch\/fix.\n \
            \1* \n \
            \1* if \(digest!=ref.digestValue\) {\n \
            \1*\/\n \
            \1\2\3digest!=ref.digestValue.replace(\/\\\\s+\/, \"\")\4"
         replace=$(echo $replace | sed -e 's/[[:space:]]+/ /g')
         regex="s/$search/$replace/g"
         # echo "sed -e '$regex' $SIGXML"
         eval "sed -i -e '$regex' $SIGXML"
      else
         echo "BU customization to $SIGXML already made."
      fi
   fi
}

