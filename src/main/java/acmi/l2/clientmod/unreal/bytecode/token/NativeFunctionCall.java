package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.io.DataOutput;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.bytecode.token.annotation.FunctionParams;
import acmi.l2.clientmod.unreal.core.Function;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NativeFunctionCall extends Token {
    public transient int nativeIndex;
    @FunctionParams
    public Token[] params;

    public NativeFunctionCall(int nativeIndex, Token... params) {
        this.nativeIndex = nativeIndex;
        this.params = params;
    }

    public NativeFunctionCall() {
    }

    @Override
    protected int getOpcode() {
        return nativeIndex;
    }

    @Override
    protected void writeOpcode(DataOutput output, int opcode) throws UncheckedIOException {
        if (opcode > 0xff) {
            output.writeByte(0x60 + ((opcode >> 8) & 0x0f));
            output.writeByte(opcode & 0xff);
        } else {
            output.writeByte(opcode);
        }
    }

    @Override
    protected Sizer getSizer() {
        return (token, context) -> {
            NativeFunctionCall nativeFunctionCall = (NativeFunctionCall) token;
            return (nativeFunctionCall.nativeIndex > 0xff ? 2 : 1) +
                    Stream.concat(Arrays.stream(nativeFunctionCall.params), Stream.of(new EndFunctionParams()))
                            .mapToInt(t -> t.getSize(context)).sum();
        };
    }

    @Override
    public String toString() {
        return "NativeFunctionCall("
                + nativeIndex
                + (params == null || params.length == 0 ? "" : ", " + Arrays.stream(params).map(Objects::toString).collect(Collectors.joining(", ")))
                + ')';
    }

    public String toString(UnrealRuntimeContext context) {
        Optional<Function> function = context.getSerializer().getNativeFunction(nativeIndex);
        if (function.isPresent()) {
            Function f = function.get();
            Collection<Function.Flag> flags = Function.Flag.getFlags(f.functionFlags);
            if (flags.contains(Function.Flag.PRE_OPERATOR)) {
                return f.friendlyName + params[0].toString(context);
            } else if (flags.contains(Function.Flag.OPERATOR)) {
                if (f.operatorPrecedence > 0) {
                    return params[0].toString(context) + " " + f.friendlyName + " " + params[params.length - 1].toString(context);
                } else {
                    return params[0].toString(context) + f.friendlyName;
                }
            } else {
                return f.friendlyName + Arrays.stream(this.params).map((p) -> p.toString(context)).collect(Collectors.joining(", ", "(", ")"));
            }
        } else {
            return "native" + this.nativeIndex + Arrays.stream(this.params).map((p) -> p.toString(context)).collect(Collectors.joining(", ", "(", ")"));
        }
    }
}
