L2unreal
========
Library for reading/modifying Lineage 2 unrealscript objects.

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