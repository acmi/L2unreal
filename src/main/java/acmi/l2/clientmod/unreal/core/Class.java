/*
 * Copyright (c) 2016 acmi
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

import acmi.l2.clientmod.io.annotation.Custom;
import acmi.l2.clientmod.unreal.UUIDSerializer;
import acmi.l2.clientmod.unreal.annotation.NameRef;
import acmi.l2.clientmod.unreal.annotation.ObjectRef;

import java.util.UUID;

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
}
