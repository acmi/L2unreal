package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.unreal.UnrealPackageContext;

public class Return extends Token {
    public static final int OPCODE = 0x04;

    public Token value;

    public Return(Token value) {
        this.value = value;
    }

    public Return() {
    }

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @Override
    public String toString() {
        return "Return("
                + value
                + ')';
    }

    @Override
    public String toString(UnrealPackageContext context) {
        return "return " + value.toString(context);
    }
}
