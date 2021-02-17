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
package acmi.l2.clientmod.unreal.engine;

import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.io.annotation.ReadMethod;
import acmi.l2.clientmod.io.annotation.WriteMethod;
import acmi.l2.clientmod.unreal.UnrealPackageContext;
import acmi.l2.clientmod.unreal.core.Object;

import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;

public class Material extends Object {
    public byte[] unk;

    @ReadMethod
    public final void readMaterial(ObjectInput<UnrealPackageContext> input) {
        unk = readUnk(input, input.getContext().getUnrealPackage().getVersion(), input.getContext().getUnrealPackage().getLicense());
    }

    @WriteMethod
    public final void write(ObjectOutput<UnrealPackageContext> output) {
        output.writeBytes(unk);
    }

    public byte[] readUnk(DataInput input, int version, int license) throws UncheckedIOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutput output = new DataOutputStream(baos, input.getCharset());

        if (license <= 10) {
            //???
        } else if (license <= 28) {
            //c0-ct0
            output.writeInt(input.readInt());
        } else if (license <= 32) {
            //???
        } else if (license <= 35) {
            //ct1-ct22
            byte[] unk = new byte[1067];
            input.readFully(unk);
            output.writeBytes(unk);
            for (int i = 0; i < 16; i++) {
                output.writeLine(input.readLine());
            }
            output.writeLine(input.readLine());
            output.writeInt(input.readInt());
        } else if (license == 36) {
            //ct22
            byte[] unk = new byte[1058];
            input.readFully(unk);
            output.writeBytes(unk);
            for (int i = 0; i < 16; i++) {
                output.writeLine(input.readLine());
            }
            output.writeLine(input.readLine());
            output.writeInt(input.readInt());
        } else if (license <= 39) {
            //Epeisodion
            if (version == 129) {
                byte[] unk = new byte[92];
                input.readFully(unk);
                output.writeBytes(unk);
                int stringCount = input.readCompactInt();
                output.writeCompactInt(stringCount);
                for (int i = 0; i < stringCount; i++) {
                    output.writeLine(input.readLine());
                    int addStringCount = input.readUnsignedByte();
                    output.writeByte(addStringCount);
                    for (int j = 0; j < addStringCount; j++) {
                        output.writeLine(input.readLine());
                    }
                }
                output.writeLine(input.readLine());
                output.writeInt(input.readInt());
            } else {
                //ct23-Lindvior
                byte[] unk = new byte[36];
                input.readFully(unk);
                output.writeBytes(unk);
                int stringCount = input.readCompactInt();
                output.writeCompactInt(stringCount);
                for (int i = 0; i < stringCount; i++) {
                    output.writeLine(input.readLine());
                    int addStringCount = input.readUnsignedByte();
                    output.writeByte(addStringCount);
                    for (int j = 0; j < addStringCount; j++) {
                        output.writeLine(input.readLine());
                    }
                }
                output.writeLine(input.readLine());
                output.writeInt(input.readInt());
            }
        } else {
            //Ertheia+
            byte[] unk = new byte[92];
            input.readFully(unk);
            output.writeBytes(unk);
            int stringCount = input.readCompactInt();
            output.writeCompactInt(stringCount);
            for (int i = 0; i < stringCount; i++) {
                output.writeLine(input.readLine());
                int addStringCount = input.readUnsignedByte();
                output.writeByte(addStringCount);
                for (int j = 0; j < addStringCount; j++) {
                    output.writeLine(input.readLine());
                }
            }
            output.writeLine(input.readLine());
            output.writeInt(input.readInt());
        }

        return baos.toByteArray();
    }
}
