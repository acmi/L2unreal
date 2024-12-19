L2unreal
========
![GitHub](https://img.shields.io/github/license/acmi/l2unreal)
[![](https://jitpack.io/v/acmi/l2unreal.svg)](https://jitpack.io/#acmi/l2unreal)

Library for reading/modifying Lineage 2 unrealscript objects.

Note
----
`UnrealSerializerFactory` uses `Thread.stackSize`, which is not available on certain JVMs.
In that case you should disable it with `-DL2unreal.loadThreadStackSize=0` and set stack size via `-Xss` or `-XX:ThreadStackSize` parameter (at least 8MB).

Usage
-----
```java
import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.unreal.Environment;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.core.TextBuffer;


String l2SystemFolder = "C:\\Lineage 2\\system";

Environment environment = Environment.fromIni(new File(l2SystemFolder, "l2.ini"));
UnrealSerializerFactory serializerFactory = new UnrealSerializerFactory(environment);

String fileName = "Engine.u";
String entryName = "Actor.ScriptText";

try (UnrealPackage up = new UnrealPackage(new File(l2SystemFolder, fileName), true)) {
    TextBuffer textBuffer = up.getExportTable()
            .parallelStream()
            .filter(e -> e.getObjectInnerFullName().equals(entryName))
            .findAny()
            .map(serializerFactory::getOrCreateObject)
            .map(o -> (TextBuffer) o)
            .orElseThrow(() -> new IllegalArgumentException(entryName + " not found"));
    System.out.println(textBuffer.text);
}
```

Build
-----
```
gradlew build
```
Append `-x test` to skip tests.

Install to local maven repository
---------------------------------
```
gradlew install
```

Maven
-----
```maven
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>acmi.l2.clientmod</groupId>
    <artifactId>l2unreal</artifactId>
    <version>1.5.6</version>
</dependency>
```

Gradle
------
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile group:'acmi.l2.clientmod', name:'l2unreal', version: '1.5.6'
}
```