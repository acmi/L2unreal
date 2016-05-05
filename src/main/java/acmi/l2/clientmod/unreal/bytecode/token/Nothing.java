package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.unreal.UnrealPackageContext;

public class Nothing extends Token {
    public static final int OPCODE = 0x0b;

    public Nothing() {
    }

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @Override
    public String toString() {
        return "Nothing()";
    }

    @Override
    public String toString(UnrealPackageContext context) {
        return "";
    }
}
