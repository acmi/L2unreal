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
