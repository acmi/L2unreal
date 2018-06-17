package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class INT64Const extends Token {
    public static final int OPCODE = 0x46;

    public int h;
    public int l;

    public INT64Const(long value) {
        this.h = (int) (value >> 32);
        this.l = (int) value;
    }

    public long getValue() {
        return (((long) h) << 32) | (l & 0xffffffffL);
    }

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @Override
    public String toString() {
        return "INT64Const("
                + getValue()
                + ")";
    }

    @Override
    public String toString(UnrealRuntimeContext context) {
        return String.valueOf(getValue());
    }
}
