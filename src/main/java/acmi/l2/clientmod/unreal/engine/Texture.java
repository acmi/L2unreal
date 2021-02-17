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

import acmi.l2.clientmod.io.DataOutput;
import acmi.l2.clientmod.io.annotation.UByte;
import acmi.l2.clientmod.io.annotation.WriteMethod;

import static acmi.l2.clientmod.io.ByteUtil.sizeOfCompactInt;

public class Texture extends Material {
    public MipMapData[] mipMaps;

    public static class MipMapData {
        private int offset;
        public byte[] data;
        private int width, height;
        @UByte
        private int widthBits, heightBits;

        @WriteMethod
        public void writeMipMapData(DataOutput output) {
            output.writeInt(output.getPosition() + 4 + sizeOfCompactInt(data.length) + data.length);
            output.writeByteArray(data);
            output.writeInt(width);
            output.writeInt(height);
            output.writeByte(widthBits);
            output.writeByte(heightBits);
        }
    }
}
