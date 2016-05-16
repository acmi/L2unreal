package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.unreal.UnrealRuntimeContext;

/**
 * @since EToA Ep 04: Grand Crusade
 */
public class DynArraySort extends Token {
    public static final int OPCODE = 0x47;

    public Token array;
    public Token compareFunction;

    public DynArraySort(Token array, Token compareFunction) {
        this.array = array;
        this.compareFunction = compareFunction;
    }

    public DynArraySort() {
    }

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
