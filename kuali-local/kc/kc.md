## Build and Install Kuali-Research with Maven

This assumes that the following have already been built and installed with maven separately:

-  schemaspy
- rice
- coeus-api
- coeus-s2sgen

```
git clone https://github.com/bu-ist/kuali-research.git
cd kuali-research
git checkout bu-master
```

There are some dependencies whose source is not available to us.
In order to get maven to build without error, we need to trick it into finding these dependencies by installing dummy versions:

```
mkdir kc-s3
cd kc-s3
cat <<EOF > pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>co.kuali</groupId>
  <artifactId>kc-s3</artifactId>
  <version>1912.0008</version>
  <name>kc-s3</name>
  <description>Fake kc-s3 to shut maven up</description>
  <dependencies>
	<dependency>
	    <groupId>com.oracle.ojdbc</groupId>
	    <artifactId>ojdbc10</artifactId>
	    <version>19.3.0.0</version>
	</dependency>  
	<dependency>
	    <groupId>com.mchange</groupId>
	    <artifactId>c3p0</artifactId>
	    <version>0.9.5.4</version>
	</dependency>
	<dependency>
	    <groupId>com.oracle.ojdbc</groupId>
	    <artifactId>osdt_cert</artifactId>
	    <version>19.3.0.0</version>
	</dependency>  
	<dependency>
	    <groupId>com.oracle.ojdbc</groupId>
	    <artifactId>osdt_core</artifactId>
	    <version>19.3.0.0</version>
	</dependency>  
  </dependencies>
</project>
EOF

mvn install
```

Now build coeus:

```
cd ..
mvn clean compile source:jar package -e -Dgrm.off=true -Dmaven.test.skip=true
```

or if it has already been built and you only want to recompile coeus-impl classes, use:

```
redeploy() {
  (
    cd /c/kuali/kuali-research/coeus-impl
    mvn compiler:compile source:jar package -e -Dgrm.off=true -Dmaven.clean.skip=true -Dmaven.test.skip=true
    cp -v \
      /c/kuali/kuali-research/coeus-impl/target/coeus-impl-2001.0040.jar \
      /c/kuali/kuali-research/coeus-webapp/target/coeus-webapp-2001.0040/WEB-INF/lib/coeus-impl-2001.0040.jar
  )
}

redeploy 
```



