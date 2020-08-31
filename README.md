Annotations
===========

###### [![Dorkbox](https://badge.dorkbox.com/dorkbox.svg "Dorkbox")](https://git.dorkbox.com/dorkbox/Annotations) [![Github](https://badge.dorkbox.com/github.svg "Github")](https://github.com/dorkbox/Annotations) [![Gitlab](https://badge.dorkbox.com/gitlab.svg "Gitlab")](https://gitlab.com/dorkbox/Annotations)


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

Maven Info
---------
```
<dependencies>
    ...
    <dependency>
      <groupId>com.dorkbox</groupId>
      <artifactId>Annotations</artifactId>
      <version>3.1</version>
    </dependency>
</dependencies>
```
  
Gradle Info
---------
````
dependencies {
    ...
    compile "com.dorkbox:Annotations:3.1"
}
````

License
---------
This project is © 2014, XIAM Solutions B.V. (http://www.xiam.nl) and © 2020 dorkbox llc, and is distributed under the terms of the
 Apache v2.0 License. See file "LICENSE" for further references.

