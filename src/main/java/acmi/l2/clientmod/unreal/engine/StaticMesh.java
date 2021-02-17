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

import acmi.l2.clientmod.io.ByteUtil;
import acmi.l2.clientmod.io.ObjectInput;
import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.annotation.*;
import acmi.l2.clientmod.unreal.UnrealPackageContext;

import java.util.Arrays;
import java.util.Locale;

@SuppressWarnings({"WeakerAccess", "unused"})
public class StaticMesh extends Primitive {
    public StaticMeshSection[] sections;
    public Box boundingBox2;
    public StaticMeshVertexStream vertexStream;
    public RawColorStream colorStream1;
    public RawColorStream colorStream2;
    public StaticMeshUVStream[] UVStream;
    public RawIndexBuffer indexStream1;
    public RawIndexBuffer indexStream2;
    @Compact
    public int u0;
    public int offsetAfterU1;
    public U1[] u1;
    public int offsetAfterU2;
    public U2[] u2;
    public int u;
    @Compact
    public int ref1;
    @Compact
    public int ref2;
    public float one11;
    public float one12;
    public float one13;
    public float one1;
    public float one2;
    public byte[] zeros12;
    public int u00;
    public int u01;
    public int offsetAfterU3;
    public U3[] u3;
    public int i1;
    @Compact
    public int i2;
    public int i3;
    public U4[] u4;
    public int i4;
    public int i5;
    public int i6;
    public int i7;

    @ReadMethod
    public final void readStaticMesh(ObjectInput<UnrealPackageContext> input) {
        int license = input.getContext().getUnrealPackage().getLicense();
        int version = input.getContext().getUnrealPackage().getVersion();

        sections = new StaticMeshSection[input.readCompactInt()];
        for (int i = 0; i < sections.length; i++) {
            input.getSerializerFactory().forClass(StaticMeshSection.class).readObject(sections[i] = new StaticMeshSection(), input);
        }
        input.getSerializerFactory().forClass(Box.class).readObject(boundingBox2 = new Box(), input);
        input.getSerializerFactory().forClass(StaticMeshVertexStream.class).readObject(vertexStream = new StaticMeshVertexStream(), input);
        input.getSerializerFactory().forClass(RawColorStream.class).readObject(colorStream1 = new RawColorStream(), input);
        input.getSerializerFactory().forClass(RawColorStream.class).readObject(colorStream2 = new RawColorStream(), input);
        UVStream = new StaticMeshUVStream[input.readCompactInt()];
        for (int i = 0; i < UVStream.length; i++) {
            input.getSerializerFactory().forClass(StaticMeshUVStream.class).readObject(UVStream[i] = new StaticMeshUVStream(), input);
        }
        input.getSerializerFactory().forClass(RawIndexBuffer.class).readObject(indexStream1 = new RawIndexBuffer(), input);
        input.getSerializerFactory().forClass(RawIndexBuffer.class).readObject(indexStream2 = new RawIndexBuffer(), input);
        u0 = input.readCompactInt();
        if (license >= 18) {
            offsetAfterU1 = input.readInt();
        }
        u1 = new U1[input.readCompactInt()];
        for (int i = 0; i < u1.length; i++) {
            input.getSerializerFactory().forClass(U1.class).readObject(u1[i] = new U1(), input);
        }
        if (license >= 18) {
            offsetAfterU2 = input.readInt();
        }
        u2 = new U2[input.readCompactInt()];
        for (int i = 0; i < u2.length; i++) {
            input.getSerializerFactory().forClass(U2.class).readObject(u2[i] = new U2(), input);
        }
        if (license >= 11) {
            u = input.readInt();
            ref1 = input.readCompactInt();
            ref2 = input.readCompactInt();
            one11 = input.readFloat();
            one12 = input.readFloat();
            one13 = input.readFloat();
            one1 = input.readFloat();
            one2 = input.readFloat();
        }
        if (license >= 14) {
            zeros12 = new byte[12];
            input.readFully(zeros12);
        }
        if (license >= 15) {
            u00 = input.readInt();
        }
        if (license >= 37) {
            u01 = input.readInt();
        }
        offsetAfterU3 = input.readInt();
        u3 = new U3[input.readCompactInt()];
        for (int i = 0; i < u3.length; i++) {
            input.getSerializerFactory().forClass(U3.class).readObject(u3[i] = new U3(), input);
        }
        i1 = input.readInt();
        i2 = input.readCompactInt();
        if (version >= 123) {
            i3 = input.readInt();
        }
        if (version >= 125 && license >= 38) {
            u4 = new U4[input.readCompactInt()];
            for (int i = 0; i < u4.length; i++) {
                input.getSerializerFactory().forClass(U4.class).readObject(u4[i] = new U4(), input);
            }
            i4 = input.readInt();
            i5 = input.readInt();
            i6 = input.readInt();
            i7 = input.readInt();
        }
    }

    @WriteMethod
    public final void writeStaticMesh(ObjectOutput<UnrealPackageContext> output) {
        int license = output.getContext().getUnrealPackage().getLicense();
        int version = output.getContext().getUnrealPackage().getVersion();

        output.writeCompactInt(sections.length);
        for (StaticMeshSection section : sections) {
            output.getSerializerFactory().forClass(StaticMeshSection.class).writeObject(section, output);
        }
        output.getSerializerFactory().forClass(Box.class).writeObject(boundingBox2, output);
        output.getSerializerFactory().forClass(StaticMeshVertexStream.class).writeObject(vertexStream, output);
        output.getSerializerFactory().forClass(RawColorStream.class).writeObject(colorStream1, output);
        output.getSerializerFactory().forClass(RawColorStream.class).writeObject(colorStream2, output);
        output.writeCompactInt(UVStream.length);
        for (StaticMeshUVStream aUVStream : UVStream) {
            output.getSerializerFactory().forClass(StaticMeshUVStream.class).writeObject(aUVStream, output);
        }
        output.getSerializerFactory().forClass(RawIndexBuffer.class).writeObject(indexStream1, output);
        output.getSerializerFactory().forClass(RawIndexBuffer.class).writeObject(indexStream2, output);
        output.writeCompactInt(u0);
        if (license >= 18) {
            output.writeInt(output.getPosition() + 4 + ByteUtil.sizeOfCompactInt(u1.length) + Arrays.stream(u1).mapToInt(U1::getSize).sum());
        }
        output.writeCompactInt(u1.length);
        for (U1 anU1 : u1) {
            output.getSerializerFactory().forClass(U1.class).writeObject(anU1, output);
        }
        if (license >= 18) {
            output.writeInt(output.getPosition() + 4 + ByteUtil.sizeOfCompactInt(u2.length) + Arrays.stream(u2).mapToInt(U2::getSize).sum());
        }
        output.writeCompactInt(u2.length);
        for (U2 anU2 : u2) {
            output.getSerializerFactory().forClass(U2.class).writeObject(anU2, output);
        }
        if (license >= 11) {
            output.writeInt(u);
            output.writeCompactInt(ref1);
            output.writeCompactInt(ref2);
            output.writeFloat(one11);
            output.writeFloat(one12);
            output.writeFloat(one13);
            output.writeFloat(one1);
            output.writeFloat(one2);
        }
        if (license >= 14) {
            output.writeBytes(zeros12);
        }
        if (license >= 15) {
            output.writeInt(u00);
        }
        if (license >= 37) {
            output.writeInt(u01);
        }
        int o = output.getPosition() + 4 + ByteUtil.sizeOfCompactInt(u3.length) + Arrays.stream(u3).mapToInt(U3::getSize).sum();
        output.writeInt(o);
        output.writeCompactInt(u3.length);
        for (U3 anU3 : u3) {
            output.getSerializerFactory().forClass(U3.class).writeObject(anU3, output);
        }
        output.writeInt(i1);
        output.writeCompactInt(i2);
        if (version >= 123) {
            output.writeInt(i3);
        }
        if (version >= 125 && license >= 38) {
            output.writeCompactInt(u4.length);
            for (U4 anU4 : u4) {
                output.getSerializerFactory().forClass(U4.class).writeObject(anU4, output);
            }
            output.writeInt(i4);
            output.writeInt(i5);
            output.writeInt(i6);
            output.writeInt(i7);
        }
    }

    public static class StaticMeshSection {
        public int f4;
        @UShort
        public int firstIndex;
        @UShort
        public int firstVertex;
        @UShort
        public int lastVertex;
        @UShort
        public int fE;
        @UShort
        public int numFaces;

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%d,%d,%d,%d,%d,%d]", f4, firstIndex, firstVertex, lastVertex, fE, numFaces);
        }
    }

    public static class StaticMeshVertexStream {
        public StaticMeshVertex[] vert;
        public int revision;

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%d,%s]", revision, Arrays.toString(vert));
        }
    }

    public static class StaticMeshVertex {
        public Vector pos;
        public Vector normal;

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%s,%s]", pos, normal);
        }
    }

    public static class RawColorStream {
        public Color[] color;
        public int revision;

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%d,%s]", revision, Arrays.toString(color));
        }
    }

    public static class StaticMeshUVStream {
        public MeshUVFloat[] data;
        public int f10;
        public int f1C;

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%d,%d,%s]", f10, f1C, Arrays.toString(data));
        }
    }

    public static class MeshUVFloat {
        public float u;
        public float v;

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%f,%f]", u, v);
        }
    }

    public static class RawIndexBuffer {
        @UShort
        public int[] indices;
        public int revision;

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%d,%s]", revision, Arrays.toString(indices));
        }
    }

    public static class U1 {
        public float u1;
        public float u2;
        public float u3;
        public float u4;
        public float u5;
        public float u6;
        public float u7;
        public float u8;
        public float u9;
        public float u10;
        public float u11;
        public float u12;
        public float u13;
        public float u14;
        public float u15;
        public float u16;
        @Compact
        public int u17;
        @Compact
        public int u18;
        @Compact
        public int u19;
        @Compact
        public int u20;

        int getSize() {
            return 16 * 4 +
                    ByteUtil.sizeOfCompactInt(u17) +
                    ByteUtil.sizeOfCompactInt(u18) +
                    ByteUtil.sizeOfCompactInt(u19) +
                    ByteUtil.sizeOfCompactInt(u20);
        }
    }

    public static class U2 {
        @Compact
        public int u1;
        @Compact
        public int u2;
        @Compact
        public int u3;
        @Compact
        public int u4;
        public Box u5;

        int getSize() {
            return ByteUtil.sizeOfCompactInt(u1) +
                    ByteUtil.sizeOfCompactInt(u2) +
                    ByteUtil.sizeOfCompactInt(u3) +
                    ByteUtil.sizeOfCompactInt(u4) +
                    25;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%d,%d,%d,%d,%s]", u1, u2, u3, u4, u5);
        }
    }

    public static class U3 {
        public float u1;
        public float u2;
        public float u3;
        public float u4;
        public float u5;
        public float u6;
        public float u7;
        public float u8;
        public float u9;
        @Length(Length.Type.INT)
        public U3_1[] u10;
        public int u11;
        public int u12;
        public int u13;
        public int u14;
        public int u15;

        public static class U3_1 {
            public float u1;
            public float u2;
            public float u3;
            public float u4;
            public float u5;
            public float u6;

            int getSize() {
                return 6 * 4;
            }
        }

        int getSize() {
            return 9 * 4 +
                    4 + Arrays.stream(u10).mapToInt(U3_1::getSize).sum() +
                    5 * 4;
        }
    }

    public static class U4 {
        public float u1;
        public float u2;

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[%f,%f]", u1, u2);
        }
    }
}
