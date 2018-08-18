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


&nbsp; 
&nbsp; 

Release Notes 
---------

This project includes some utility classes that are a small subset of a much larger library. These classes are **kept in sync** with the main utilities library, so "jar hell" is not an issue, and the latest release will always include the same version of utility files as all of the other projects in the dorkbox repository at that time. 
  
  Please note that the utility source code is included in the release and on our [Git Server](https://git.dorkbox.com/dorkbox/Utilities) repository.
  
  
Maven Info
---------
```
<dependencies>
    ...
    <dependency>
      <groupId>com.dorkbox</groupId>
      <artifactId>Annotations</artifactId>
      <version>2.14</version>
    </dependency>
</dependencies>
```

  
Gradle Info
---------
````
dependencies {
    ...
    compile 'com.dorkbox:Annotations:2.14'
}
````

Or if you don't want to use Maven, you can access the files directly here:  
https://repo1.maven.org/maven2/com/dorkbox/Annotations/  


License
---------
This project is © 2011 - 2014, XIAM Solutions B.V. (http://www.xiam.nl) and © 2014 dorkbox llc, and is distributed under the terms of the Apache v2.0 License. See file "LICENSE" for further references.

