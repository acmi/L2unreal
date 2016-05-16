package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.io.annotation.Compact;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.annotation.ObjectRef;

public class DefaultVariable extends Token {
    public static final int OPCODE = 0x02;

    @Compact
    @ObjectRef
    private int objRef;

    public DefaultVariable(int objRef) {
        this.objRef = objRef;
    }

    public DefaultVariable() {
    }

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @Override
    public String toString() {
        return "DefaultVariable("
                + objRef
                + ')';
    }

    @Override
    public String toString(UnrealRuntimeContext context) {
        return context.getUnrealPackage().objectReference(objRef).getObjectName().getName();
    }
}
