package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.unreal.UnrealRuntimeContext;

public class Stop extends Token {
    public static final int OPCODE = 0x08;

    public Stop() {
    }

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @Override
    public String toString() {
        return "Stop()";
    }

    @Override
    public String toString(UnrealRuntimeContext context) {
        return "";
    }
}
