If Jetty is being run from a jar file (not an exploded app), then the server it runs has a WebappContext that
cannot associate with any files because these are part of the jar and cannot be referenced.
Therefore if it while starting the application it is determined that the jar file was executed, then the entire webapp
directory is extracted on to the file system first and then the WebappContext is instantiated and associated with the extracted directory.

NOTE: The jackson libraries used to write JSON response objects out for http requests are accessed at runtime as resources of the web
context container. This means that although the jar file has them in its classpath, Jersey cannot see them. Therefore the 
jersey-media-json-jackson-2.22.jar and a containing WEB-INF/lib directory should be part of the jar file extraction.

Run this jar file with:

Normal:
java -jar selenium-test-0.0.1-SNAPSHOT-jar-with-dependencies.jar desktop

Extra debugging output:
java -jar -Dorg.eclipse.jetty.LEVEL=ALL selenium-test-0.0.1-SNAPSHOT-jar-with-dependencies.jar desktop