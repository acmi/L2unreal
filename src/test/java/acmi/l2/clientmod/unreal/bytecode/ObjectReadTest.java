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
package acmi.l2.clientmod.unreal.bytecode;

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.unreal.Environment;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import org.junit.Test;

import java.io.File;

public class ObjectReadTest {
    @Test
    public void test() {
        Environment environment = Environment.fromIni(new File(getClass().getResource("/system/L2.ini").getFile()));
        UnrealSerializerFactory serializerFactory = new UnrealSerializerFactory(environment);

        File[] files = new File("src/test/resources/system")
                .listFiles((dir, name) -> name.endsWith(".u"));
        for (File file : files) {
            try (UnrealPackage up = new UnrealPackage(file, true)) {
                up.getExportTable()
                        .stream()
                        .filter(e -> e.getFullClassName().equalsIgnoreCase("Core.Class"))
                        .forEach(serializerFactory::getOrCreateObject);
            }
        }
    }
}
