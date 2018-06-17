package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @since EToA Ep 04: Grand Crusade
 */
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class DynArraySort extends Token {
    public static final int OPCODE = 0x47;

    public Token array;
    public Token compareFunction;

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @Override
    public String toString() {
        return "DynArraySort(" +
                array +
                ", " + compareFunction +
                ')';
    }

    @Override
    public String toString(UnrealRuntimeContext context) {
        return array.toString(context) + ".Sort(" + compareFunction.toString(context) + ")";
    }
}
