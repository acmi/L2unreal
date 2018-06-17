package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.io.annotation.Compact;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.annotation.ObjectRef;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class DefaultVariable extends Token {
    public static final int OPCODE = 0x02;

    @Compact
    @ObjectRef
    public int objRef;

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
        return "default." + context.getUnrealPackage().objectReference(objRef).getObjectName().getName();
    }
}
