Annotations
===========


The Annotations project is based on, and **almost** identical to the excellent [infomas AnnotationDetector](https://github.com/rmuller/infomas-asl).

There are **major** changes to the API to provide for a way to easily extend the scanning functionality, hence it lives here on it's own.  

This library can be used to scan (part of) the class path for annotated classes, methods or instance variables. Main advantages of this library compared with similar solutions are: light weight (simple API, **20 kb jar file**) and **very fast** (fastest annotation detection library as far as I know).

The main features of this annotations scanning library:  
- scans the bytecode for annotation information, meaning classes do not have to be loaded by the JVM
- small footprint
- *extremely fast* performance (on moderate hardware, about 200 MB/s)!
- can scan the classpath, classloader, or specified location
- uses the SLF4j logging interface
- simple builder style API
- lightweight

- This is for cross-platform use, specifically - linux 32/64, mac 32/64, and windows 32/64. Java 6+


``` java
try {
    // Get a list of all classes annotated with @Module, inside the "dorkbox.client" and "dorkbox.common" packages.
    List<Module> classModules = AnnotationDetector.scanClassPath("dorkbox.client", "dorkbox.common")
                                     .forAnnotations(Module.class)  // one or more annotations
                                     .on(ElementType.METHOD) // optional, default ElementType.TYPE. One ore more element types
                                     .filter((File dir, String name) -> !name.endsWith("Client.class")) // optional, default all *.class files
                                     .collect(AnnotationDefaults.getType);
} catch (IOException e) {
    throw new IllegalArgumentException("Unable to start the client", e);
}
```



<h4>We now release to maven!</h4> 

There is a hard dependency in the POM file for the utilities library, which is an extremely small subset of a much larger library; including only what is *necessary* for this particular project to function.

This project is **kept in sync** with the utilities library, so "jar hell" is not an issue. Please note that the util library (in it's entirety) is not added since there are **many** dependencies that are not *necessary* for this project. No reason to require a massive amount of dependencies for one or two classes/methods. 
```
<dependency>
  <groupId>com.dorkbox</groupId>
  <artifactId>Annotations</artifactId>
  <version>2.0</version>
</dependency>
```

Or if you don't want to use Maven, you can access the files directly here:  
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/Annotations/  
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/Annotations-Dorkbox-Util/  


https://repo1.maven.org/maven2/org/slf4j/slf4j-api/


<h2>License</h2>

This project is distributed under the terms of the Apache v2.0 License. See file "LICENSE" for further references.



