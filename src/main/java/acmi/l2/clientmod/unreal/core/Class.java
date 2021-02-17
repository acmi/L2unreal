/*
 * Copyright (c) 2021 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.unreal.core;

import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.annotation.Custom;
import acmi.l2.clientmod.io.annotation.WriteMethod;
import acmi.l2.clientmod.unreal.UUIDSerializer;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.annotation.NameRef;
import acmi.l2.clientmod.unreal.annotation.ObjectRef;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static acmi.l2.clientmod.io.ByteUtil.uuidToBytes;

public class Class extends State {
    public int classFlags;
    @Custom(UUIDSerializer.class)
    public UUID classUuid;
    public Dependency[] dependencies;
    @NameRef
    public String[] packageImports;
    @ObjectRef
    public Object classWithin;
    @NameRef
    public String classConfigName;
    @NameRef
    public String[] hideCategories;

    public static class Dependency {
        @ObjectRef
        public acmi.l2.clientmod.unreal.core.Class clazz;
        public int deep;
        public int scriptTextCRC;
    }

    @WriteMethod
    public final void writeClass(ObjectOutput<UnrealRuntimeContext> output) {
        output.writeInt(classFlags);
        output.writeBytes(uuidToBytes(classUuid));
        Dependency[] dependencies = Optional.ofNullable(this.dependencies).orElse(new Dependency[0]);
        output.writeCompactInt(dependencies.length);
        Arrays.stream(dependencies).forEach(output::write);
        String[] packageImports = Optional.ofNullable(this.packageImports).orElse(new String[0]);
        output.writeCompactInt(packageImports.length);
        Arrays.stream(packageImports).mapToInt(n -> output.getContext().getUnrealPackage().nameReference(n)).forEach(output::writeCompactInt);
        output.writeCompactInt(Optional.ofNullable(classWithin).map(o -> output.getContext().getUnrealPackage().objectReferenceByName(o.entry.getObjectFullName(), c -> c.equalsIgnoreCase(o.entry.getFullClassName()))).orElse(0));
        output.writeCompactInt(output.getContext().getUnrealPackage().nameReference(classConfigName));
        String[] hideCategories = Optional.ofNullable(this.hideCategories).orElse(new String[0]);
        output.writeCompactInt(hideCategories.length);
        Arrays.stream(hideCategories).mapToInt(n -> output.getContext().getUnrealPackage().nameReference(n)).forEach(output::writeCompactInt);
        PropertiesUtil.writeProperties(output, properties);
    }
}
