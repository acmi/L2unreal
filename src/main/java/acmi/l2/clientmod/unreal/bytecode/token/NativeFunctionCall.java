/*
 * Copyright (c) 2021 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.unreal.bytecode.token;

import acmi.l2.clientmod.io.DataOutput;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.bytecode.token.annotation.FunctionParams;
import acmi.l2.clientmod.unreal.core.Function;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class NativeFunctionCall extends Token {
    public transient int nativeIndex;
    @FunctionParams
    public Token[] params;

    public NativeFunctionCall(int nativeIndex, Token... params) {
        this.nativeIndex = nativeIndex;
        this.params = params;
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
    protected Sizer<NativeFunctionCall> getSizer() {
        return (token, context) -> (token.nativeIndex > 0xff ? 2 : 1) +
                Stream.concat(Arrays.stream(token.params), Stream.of(new EndFunctionParams()))
                        .mapToInt(t -> t.getSize(context)).sum();
    }

    @Override
    public String toString() {
        return "NativeFunctionCall("
                + nativeIndex
                + (params == null || params.length == 0 ? ")" : Arrays.stream(params).map(Objects::toString).collect(Collectors.joining(", ", ", ", ")")));
    }

    @Override
    public String toString(UnrealRuntimeContext context) {
        if (context.getSerializer() != null) {
            Optional<Function> function = context.getSerializer().getNativeFunction(nativeIndex);
            if (function.isPresent()) {
                Function f = function.get();
                Collection<Function.Flag> flags = Function.Flag.getFlags(f.functionFlags);
                if (flags.contains(Function.Flag.PRE_OPERATOR)) {
                    String b = params[0].toString(context);
                    if (params[0] instanceof NativeFunctionCall) {
                        Function inner = context.getSerializer().getNativeFunction(nativeIndex).orElse(null);
                        if (inner != null) {
                            Collection<Function.Flag> innerFuncFlags = Function.Flag.getFlags(inner.functionFlags);
                            if (innerFuncFlags.contains(Function.Flag.OPERATOR)) {
                                b = "(" + b + ")";
                            }
                        }
                    }
                    return f.friendlyName + b;
                } else if (flags.contains(Function.Flag.OPERATOR)) {
                    if (f.operatorPrecedence > 0) {
                        Token left = params[0];
                        Token right = params[params.length - 1];
                        boolean needLeftBrackets = needBrackets(left, context, leftOpPrecedence -> leftOpPrecedence > f.operatorPrecedence);
                        boolean needRightBrackets = needBrackets(right, context, rightOpPrecedence -> rightOpPrecedence >= f.operatorPrecedence);
                        return (needLeftBrackets ? "(" : "") + left.toString(context) + (needLeftBrackets ? ")" : "")
                                + " " + f.friendlyName + " "
                                + (needRightBrackets ? "(" : "") + right.toString(context) + (needRightBrackets ? ")" : "");
                    } else {
                        return params[0].toString(context) + f.friendlyName;
                    }
                } else {
                    return f.friendlyName + Arrays.stream(this.params).map((p) -> p.toString(context)).collect(Collectors.joining(", ", "(", ")"));
                }
            }
        }
        return "native" + this.nativeIndex + Arrays.stream(this.params).map((p) -> p.toString(context)).collect(Collectors.joining(", ", "(", ")"));
    }

    private boolean needBrackets(Token token, UnrealRuntimeContext context, IntPredicate p) {
        if (token instanceof NativeFunctionCall) {
            NativeFunctionCall nfc = (NativeFunctionCall) token;
            Optional<Function> funcOpt = context.getSerializer().getNativeFunction(nfc.nativeIndex);
            if (funcOpt.isPresent()) {
                Function func = funcOpt.get();
                return p.test(func.operatorPrecedence);
            }
        } else {
            return false;
        }
        return true;
    }
}
