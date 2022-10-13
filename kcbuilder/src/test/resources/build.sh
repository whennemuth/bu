curl -s https://raw.githubusercontent.com/bu-ist/kuali_research/master/pom.xml > pom.xml
KC_PROJECT_VERSION=$(cat pom.xml | egrep -m 1 "<version>" | sed 's/<version>//' | sed 's/\..*//' | awk '{print $1}')
echo $KC_PROJECT_VERSION

KC_API_PROJECT_VER=$(egrep "<coeus-api-all.version>" pom.xml | awk '{print $1}' | cut -d'>' -f2 | cut -d'<' -f1)
echo $KC_API_PROJECT_VER

KC_S2SGEN_PROJECT_VER=$(egrep "<coeus-s2sgen.version>" pom.xml | awk '{print $1}' | cut -d'>' -f2 | cut -d'<' -f1)
echo $KC_S2SGEN_PROJECT_VER

KC_RICE_PROJECT_VER=$(egrep "<rice.version>" pom.xml | awk '{print $1}' | cut -d'>' -f2 | cut -d'<' -f1)
echo $KC_RICE_PROJECT_VER

SCHEMASPY_PROJECT_VER=$(egrep "<schemaspy.version>" pom.xml | awk '{print $1}' | cut -d'>' -f2 | cut -d'<' -f1)
echo $SCHEMASPY_PROJECT_VER

KC_PROJECT_VER=$(head -n 30 pom.xml | egrep "<version>" | awk '{print $1}' | cut -d'>' -f2 | cut -d'<' -f1)
Echo $KC_PROJECT_VER

# BUILD SCHEMASPY
#cd schemaspy
#git checkout tags/schemaspy-1507.2
#mvn clean compile source:jar javadoc:jar install -Dgrm.off=true

 
# BUILD KUALI RICE
#cd ../kc-rice/
#git checkout tags/rice-2.5.3.1509.0002-kualico
#mvn clean compile source:jar javadoc:jar install -Dgrm.off=true

 
# BUILD KUALI COEUS API
#cd ../kc-api
#git checkout tags/coeus-api-1509.0003
#mvn clean compile source:jar javadoc:jar install -Dgrm.off=true
 

# BUILD KUALI COEUS S2SGEN
#cd ../kc-s2sgen
#git checkout tags/coeus-s2sgen-1509.0014
#mvn clean compile source:jar javadoc:jar install -Dgrm.off=true

# BUILD KUALI COEUS
#cd ../../kuali-research
#mvn clean compile source:jar javadoc:jar install -Dgrm.off=true