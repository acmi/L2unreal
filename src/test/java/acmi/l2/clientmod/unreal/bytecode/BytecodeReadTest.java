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
package acmi.l2.clientmod.unreal.bytecode;

import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.unreal.Environment;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.bytecode.token.Token;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BytecodeReadTest {
    @Test
    public void test() {
        Environment environment = Environment.fromIni(new File(getClass().getResource("/system/L2.ini").getFile()));
        UnrealSerializerFactory serializerFactory = new UnrealSerializerFactory(environment);
        serializerFactory.getOrCreateObject("Core.Object", t -> true);

        File[] files = new File("src/test/resources/system")
                .listFiles((dir, name) -> name.endsWith(".u"));
        for (File file : files) {
            try (UnrealPackage up = new UnrealPackage(file, true)) {
                up.getExportTable().stream()
                        .filter(exportEntry -> exportEntry.getFullClassName().equalsIgnoreCase("Core.Struct") ||
                                exportEntry.getFullClassName().equalsIgnoreCase("Core.Flags") ||
                                exportEntry.getFullClassName().equalsIgnoreCase("Core.Class") ||
                                exportEntry.getFullClassName().equalsIgnoreCase("Core.Function"))
                        .forEach(entry -> {
                            System.out.println(entry.getObjectFullName());
                            byte[] entryBytes = entry.getObjectRawData();
                            BytecodeContext context = new BytecodeContext(up);
                            TokenSerializerFactory tokenSerializerFactory = new TokenSerializerFactory();
                            ObjectInput<BytecodeContext> input = new ObjectInputStream<>(
                                    new ByteArrayInputStream(entryBytes),
                                    up.getFile().getCharset(),
                                    entry.getOffset(),
                                    tokenSerializerFactory,
                                    context
                            );
                            if (!entry.getFullClassName().equalsIgnoreCase("Core.Class")) {
                                input.readCompactInt();
                            }
                            input.readCompactInt();
                            input.readCompactInt();
                            input.readCompactInt();
                            input.readCompactInt();
                            input.readCompactInt();
                            input.readCompactInt();
                            input.readInt();
                            input.readInt();
                            int size = input.readInt();
                            int pos = input.getPosition();
                            int readSize = 0;
                            List<Token> tokens = new ArrayList<>();
                            while (readSize < size) {
                                Token token = input.readObject(Token.class);

                                System.out.println("\t" + String.format("/*0x%04x*/\t%s\t/* %s */", readSize, token, token.toString(new UnrealRuntimeContext(entry, serializerFactory))));

                                readSize += token.getSize(input.getContext());
                                tokens.add(token);
                            }
                            assertEquals(readSize, size);

                            int bytecodeSize = input.getPosition() - pos;

                            byte[] originalBytecode = new byte[bytecodeSize];
                            System.arraycopy(entryBytes, pos - entry.getOffset(), originalBytecode, 0, bytecodeSize);
                            System.out.println("\torig: " + javax.xml.bind.DatatypeConverter.printHexBinary(originalBytecode));

                            ByteArrayOutputStream baos = new ByteArrayOutputStream(bytecodeSize);
                            ObjectOutput objectOutput = new ObjectOutputStream<>(baos, up.getFile().getCharset(),
                                    tokenSerializerFactory,
                                    context);
                            tokens.forEach(objectOutput::write);
                            byte[] newBytecode = baos.toByteArray();
                            System.out.println("\t new: " + javax.xml.bind.DatatypeConverter.printHexBinary(newBytecode));
                            assertArrayEquals(originalBytecode, newBytecode);
                        });
            }
        }
    }
}
