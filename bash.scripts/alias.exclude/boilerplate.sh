#!/bin/bash

processJava() {
  find $(pwd) -name "*.java" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'java' {} \; 
}

processJavascript() {
  find $(pwd) -name "*.js" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'js' {} \;
}

processHtm() {
  find $(pwd) -name "*.htm" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'htm' {} \;
  find $(pwd) -name "*.html" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'html' {} \;
}

processTag() {
  find $(pwd) -name "*.tag" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'tag' {} \;
}

processJsp() {
  find $(pwd) -name "*.jsp" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'jsp' {} \;
}

processJsx() {
  find $(pwd) -name "*.jsx" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'jsx' {} \;
}

processDtd() {
  find $(pwd) -name "*.dtd" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'dtd' {} \;
}

processXml() {
  find $(pwd) -name "*.xml" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'xml' {} \;
}

processXsl() {
  find $(pwd) -name "*.xsl" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'xsl' {} \;
}

processXsd() {
  find $(pwd) -name "*.xsd" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'xsd' {} \;
}

processCss() {
  find $(pwd) -name "*.css" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'css' {} \;
}

processFtl() {
  find $(pwd) -name "*.ftl" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'ftl' {} \;
}

processTld() {
  find $(pwd) -name "*.tld" -exec sh /c/whennemuth/scrap/copyright/boilerplate.sh 'tld' {} \;
}

processAll() {

  processJava

  processJavascript

  processHtm

  processTag

  processJsp

  processJsx

  processDtd

  processXml

  processXsl

  processXsd

  processCss

  processFtl

  processTld
}

removeJavaBoilerPlate() {
  local targetfile="$1"
  if [ -n "$(grep -iP '\s*\*\s*Copyright (((\(C\))|©) )?2005' $targetfile)" ] ; then
    echo $targetfile
    # sed ':a;N;$!ba;s/\/\*.*\*\///' $targetfile
    sed -i ':a;N;$!ba;s/\/\*.*\*\///' $targetfile
  fi
}

removeHtmBoilerPlate() {
  local targetfile="$1"
  if [ -n "$(grep -iP '\s*Copyright (((\(C\))|©) )?2005' $targetfile)" ] ; then
    echo $targetfile
    # sed ':a;N;$!ba;s/<!--.*-->//g' $targetfile
    sed -i ':a;N;$!ba;s/<!--.*-->//g' $targetfile
  fi
}

removeJspBoilerPlate() {
  local targetfile="$1"
  if [ -n "$(grep -iP '\s*Copyright (((\(C\))|©) )?2005' $targetfile)" ] ; then
    echo $targetfile
    # sed ':a;N;$!ba;s/<%--.*--%>//g' $targetfile
    sed -i ':a;N;$!ba;s/<%--.*--%>//g' $targetfile
  fi
}

removeFtlBoilerPlate() {
  local targetfile="$1"
  if [ -n "$(grep -iP '\s*Copyright (((\(C\))|©) )?2005' $targetfile)" ] ; then
    echo $targetfile
    # sed ':a;N;$!ba;s/<#--.*-->//g' $targetfile
    sed -i ':a;N;$!ba;s/<#--.*-->//g' $targetfile
  fi
}

removeJavascriptBoilerPlate() {
  removeJavaBoilerPlate "$1"
}

removeTagBoilerPlate() {
  removeJspBoilerPlate "$1"
}

removeJsxBoilerPlate() {
  removeHtmBoilerPlate "$1"
  removeJavaBoilerPlate "$1"
}

removeDtdBoilerPlate() {
  removeHtmBoilerPlate "$1"
}

removeXmlBoilerPlate() {
#  removeHtmBoilerPlate "$1"

  local xmlexpr='<beans xmlns="[^"]+" xmlns:xsi="[^"]+" xmlns:p="[^"]+" xsi:schemaLocation="[^"]+">'
  if [ -n "$(grep -P "$xmlexpr" "$1")" ] ; then
    echo "Removing namespace bean element: $1"
    sed -i -E "s/$xmlexpr//" $1
  fi
}

removeXslBoilerPlate() {
  removeHtmBoilerPlate "$1"
}

removeXsdBoilerPlate() {
  removeHtmBoilerPlate "$1"
}

removeCssBoilerPlate() {
  removeJavaBoilerPlate "$1"
}

removeTldBoilerPlate() {
  removeHtmBoilerPlate "$1"
}

if [ "$1" == "java" ] ; then
  if [ -n "$2" ] ; then
    removeJavaBoilerPlate "$2"
  fi
fi

if [ "$1" == "js" ] ; then
  if [ -n "$2" ] ; then
    removeJavascriptBoilerPlate "$2"
  fi
fi

if [ "$1" == "htm" ] || [ "$1" == "html" ] ; then
  if [ -n "$2" ] ; then
    removeHtmBoilerPlate "$2"
  fi
fi

if [ "$1" == "tag" ] ; then
  if [ -n "$2" ] ; then
    removeTagBoilerPlate "$2"
  fi
fi

if [ "$1" == "jsp" ] ; then
  if [ -n "$2" ] ; then
    removeJspBoilerPlate "$2"
  fi
fi

if [ "$1" == "jsx" ] ; then
  if [ -n "$2" ] ; then
    removeJsxBoilerPlate "$2"
  fi
fi

if [ "$1" == "dtd" ] ; then
  if [ -n "$2" ] ; then
    removeDtdBoilerPlate "$2"
  fi
fi

if [ "$1" == "xml" ] ; then
  if [ -n "$2" ] ; then
    removeXmlBoilerPlate "$2"
  fi
fi

if [ "$1" == "xsd" ] ; then
  if [ -n "$2" ] ; then
    removeXsdBoilerPlate "$2"
  fi
fi

if [ "$1" == "xsl" ] ; then
  if [ -n "$2" ] ; then
    removeXslBoilerPlate "$2"
  fi
fi

if [ "$1" == "css" ] ; then
  if [ -n "$2" ] ; then
    removeCssBoilerPlate "$2"
  fi
fi

if [ "$1" == "ftl" ] ; then
  if [ -n "$2" ] ; then
    removeFtlBoilerPlate "$2"
  fi
fi

if [ "$1" == "tld" ] ; then
  if [ -n "$2" ] ; then
    removeTldBoilerPlate "$2"
  fi
fi



