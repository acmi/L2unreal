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

import acmi.l2.clientmod.io.ObjectInput;
import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.annotation.ReadMethod;
import acmi.l2.clientmod.io.annotation.UShort;
import acmi.l2.clientmod.io.annotation.WriteMethod;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.annotation.Offset;
import acmi.l2.clientmod.unreal.bytecode.BytecodeContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.UncheckedIOException;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class Case extends Token {
    public static final int OPCODE = 0x0a;

    public static final int DEFAULT = 0xffff;

    @UShort
    @Offset
    public int nextOffset;
    public Token condition;

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @ReadMethod
    public void readFrom(ObjectInput<BytecodeContext> input) throws UncheckedIOException {
        nextOffset = input.readUnsignedShort();
        if (nextOffset != DEFAULT) {
            condition = input.readObject(Token.class);
        }
    }

    @WriteMethod
    public void writeCase(ObjectOutput<BytecodeContext> output) throws UncheckedIOException {
        output.writeShort(nextOffset);
        if (nextOffset != DEFAULT) {
            output.write(condition);
        }
    }

    private static final Sizer<Case> sizer =
            (token, context) -> 1 +                              //opcode
                    2 +                                          //nextOffset
                    (token.condition != null ?                   //condition
                            token.condition.getSize(context) : 0);

    @Override
    protected Sizer<Case> getSizer() {
        return sizer;
    }

    @Override
    public String toString() {
        return "Case("
                + String.format("0x%04x", nextOffset)
                + ", " + condition
                + ')';
    }

    @Override
    public String toString(UnrealRuntimeContext context) {
        return nextOffset != DEFAULT ? "case " + condition.toString(context) + ":" : "default:";
    }
}
