package com.candlelight.nbja;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;

class JavaCompilerHelper {
    private DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    private List<File> javaFiles = new ArrayList<>();

    private List<String> options = new ArrayList<>();

    private File classOutputFolder;
    private List<File> platformClassPath = new ArrayList<>();
    private List<File> classPath = new ArrayList<>();

    public void setOptions(String... options) {
        for (String opt : options) {
            this.options.add(opt);
        }
    }

    public void setOutputFolder(String path) {
        this.classOutputFolder = new File(path);
    }

    public void addJavaFile(String file) {
        this.javaFiles.add(new File(file));
    }

    public void addJarFile(String file) {
        this.classPath.add(new File(file));
    }

    public void addPlatformJarFile(String file) {
        this.platformClassPath.add(new File(file));
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return this.diagnostics.getDiagnostics();
    }

    public boolean compile() throws IOException {
        JavacTool tool = JavacTool.create();
        CustomWriter customWriter = new CustomWriter();

        JavacFileManager fileManager = tool.getStandardFileManager(this.diagnostics, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(this.classOutputFolder));
        fileManager.setLocation(StandardLocation.SOURCE_PATH, javaFiles);

        if (this.platformClassPath.size() > 0) {
            fileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH,
                    this.platformClassPath);
        }

        if (this.classPath.size() > 0) {
            fileManager.setLocation(StandardLocation.CLASS_PATH, this.classPath);
        }

        JavacTask task = tool.getTask(customWriter, fileManager, this.diagnostics, this.options, null,
                this.javaFilesToFileObject());

        return task.call();
    }

    private List<SimpleJavaFileObject> javaFilesToFileObject() {
        return this.javaFiles.stream().map(file -> {
            return new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                    return Files.readString(file.toPath());
                }
            };
        }).collect(Collectors.toList());
    }

    public String getDiagnosticMessages() {
        StringBuilder messages = new StringBuilder();

        for (Diagnostic<? extends JavaFileObject> diagnostic : this.getDiagnostics()) {
            StringBuilder msg = new StringBuilder();
            JavaFileObject source = diagnostic.getSource();

            // [WARNING]:
            msg.append("[")
                    .append(diagnostic.getKind().toString())
                    .append("]:");

            // [WARNING]:file.java:
            if (source != null) {
                msg.append(source.getName())
                        .append(":");
            }

            // [WARNING]:file.java:27:
            msg.append(diagnostic.getLineNumber())
                    .append(": ");

            // [WARNING]:file.java:27: Warning message.
            msg.append(diagnostic.getMessage(Locale.getDefault()));

            messages.append(msg);
            messages.append("\n");
        }

        return messages.toString();
    }

    public void generateJarFile() throws IOException {
        this.generateJarFile("classes.jar");
    }

    public void generateJarFile(String jarName) throws IOException {
        String outputPath = this.classOutputFolder.getPath();
        File outputFile = new File(String.format("%s/%s", outputPath, jarName));

        FileOutputStream fileOutStream = new FileOutputStream(outputFile);
        JarOutputStream jarOutStream = new JarOutputStream(fileOutStream);

        for (File file : this.classOutputFolder.listFiles()) {
            if (file.getPath().equals(outputFile.getPath()))
                continue;

            addFileToJar(outputPath, file, jarOutStream);
        }

        jarOutStream.close();
        fileOutStream.close();
    }

    private void addFileToJar(String rootPath, File file, JarOutputStream jarOutStream) throws IOException {
        String fileRelativePath = file.getPath().substring(rootPath.length() + 1);
        String name = file.isDirectory() && !fileRelativePath.endsWith("/") ? fileRelativePath + "/" : fileRelativePath;

        BufferedInputStream inStream = null;

        try {
            JarEntry entry = new JarEntry(name);
            entry.setTime(file.lastModified());
            jarOutStream.putNextEntry(entry);

            if (file.isDirectory()) {
                jarOutStream.closeEntry();

                for (File childFile : file.listFiles()) {
                    addFileToJar(rootPath, childFile, jarOutStream);
                }

                return;
            }

            inStream = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[1024];

            while (true) {
                int count = inStream.read(buffer);

                if (count == -1)
                    break;

                jarOutStream.write(buffer, 0, count);
            }

            jarOutStream.closeEntry();
        } finally {
            if (inStream != null)
                inStream.close();
        }
    }

    private class CustomWriter extends Writer {
        StringBuilder sb = new StringBuilder();

        @Override
        public void close() throws IOException {
            this.flush();
        }

        @Override
        public void flush() throws IOException {
            sb.setLength(0);
        }

        @Override
        public void write(char[] buf, int off, int len) throws IOException {
            sb.append(buf, off, off + len);
        }
    }
}