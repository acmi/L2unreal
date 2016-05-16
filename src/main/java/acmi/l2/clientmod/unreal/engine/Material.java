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
            for (int i = 0; i < 16; i++)
                output.writeLine(input.readLine());
            output.writeLine(input.readLine());
            output.writeInt(input.readInt());
        } else if (license == 36) {
            //ct22
            byte[] unk = new byte[1058];
            input.readFully(unk);
            output.writeBytes(unk);
            for (int i = 0; i < 16; i++)
                output.writeLine(input.readLine());
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
                    for (int j = 0; j < addStringCount; j++)
                        output.writeLine(input.readLine());
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
                    for (int j = 0; j < addStringCount; j++)
                        output.writeLine(input.readLine());
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
                for (int j = 0; j < addStringCount; j++)
                    output.writeLine(input.readLine());
            }
            output.writeLine(input.readLine());
            output.writeInt(input.readInt());
        }

        return baos.toByteArray();
    }
}
