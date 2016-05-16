package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.io.annotation.Compact;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.annotation.ObjectRef;

public class LocalVariable extends Token {
    public static final int OPCODE = 0x00;

    @Compact
    @ObjectRef
    public int objRef;

    public LocalVariable(int objRef) {
        this.objRef = objRef;
    }

    public LocalVariable() {
    }

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @Override
    public String toString() {
        return "LocalVariable("
                + objRef
                + ')';
    }

    @Override
    public String toString(UnrealRuntimeContext context) {
        return context.getUnrealPackage().objectReference(objRef).getObjectName().getName();
    }
}
