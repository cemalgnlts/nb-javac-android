# nb-javac-android

[nb-javac](https://github.com/oracle/nb-javac) is a patched version of OpenJDK "javac", i.e., the Java compiler. This has long been part of NetBeans, providing a highly tuned Java compiler specifically for the Java editor i.e., parsing and lexing for features such as syntax coloring, code completion.

`nb-javac-android` is a patched version of `nb-javac`. It allows developers to use `javac` i.e. the Java Compiler in android applications.


## Usage Example

```java
// Create a folder to collect class outputs.
File cacheDir = File.getCacheDir();
File outputFolder = new File(cacheDir.getPath() + "/output");

JavaCompilerHelper helper = new JavaCompilerHelper();
helper.setOptions("-proc:none", "-source", "7", "-target", "7");
helper.setOutputFolder(outputFolder.getpath());
helper.addPlatformJarFile("./android.jar");
// helper.addJarFile("./extra-lib.jar");
helper.addJavaFile("./App.java");

// Returns false if Java files could not be compiled.
boolean compileResult = helper.compile();

// When the process is complete, add the class files into the jar file.
if (compileResult == true) {
   // or 'helper.generateJarFile("output.jar")' default 'classes.jar'
   helper.generateJarFile(); // outputFolder + "/classes.jar"
} else {
   // Show warning and error messages.
   Log.e(helper.getDiagnosticMessages());
}
```