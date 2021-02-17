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
import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.annotation.UByte;
import acmi.l2.clientmod.io.annotation.UShort;
import acmi.l2.clientmod.io.annotation.WriteMethod;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.bytecode.BytecodeContext;
import acmi.l2.clientmod.unreal.bytecode.token.annotation.FunctionParams;

import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static acmi.l2.clientmod.io.ReflectionUtil.fieldGet;

public abstract class Token {
    private static final Map<Class<? extends Token>, Sizer> SIZERS = new HashMap<>();

    protected abstract int getOpcode();

    protected void writeOpcode(DataOutput output, int opcode) throws UncheckedIOException {
        output.writeByte(opcode);
    }

    @WriteMethod
    public void writeToken(ObjectOutput<BytecodeContext> output) throws UncheckedIOException {
        writeOpcode(output, getOpcode());
    }

    public abstract String toString(UnrealRuntimeContext context);

    protected Sizer getSizer() {
        return SIZERS.computeIfAbsent(getClass(), k -> createSizer(getClass()));
    }

    private static Sizer createSizer(Class<? extends Token> clazz) {
        return (token, context) -> 1 + //opcode
                Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> !Modifier.isStatic(f.getModifiers()))
                        .filter(f -> !Modifier.isTransient(f.getModifiers()))
                        .map(Token::fieldSizer)
                        .mapToInt(f -> f.apply(token, context))
                        .sum();
    }

    private static BiFunction<Token, BytecodeContext, Integer> fieldSizer(Field f) {
        Class type = f.getType();
        if (type == Byte.TYPE || f.isAnnotationPresent(UByte.class)) {
            return (token, context) -> 1;
        } else if (type == Short.TYPE || f.isAnnotationPresent(UShort.class)) {
            return (token, context) -> 2;
        } else if (type == Integer.TYPE) {
            return (token, context) -> 4;
        } else if (type == Float.TYPE) {
            return (token, context) -> 4;
        } else if (type == String.class) {
            return (token, context) -> {
                String s = (String) fieldGet(f, token);
                return s.getBytes(context.getUnrealPackage().getFile().getCharset()).length + 1;
            };
        } else if (Token.class.isAssignableFrom(type)) {
            return (token, context) -> ((Token) fieldGet(f, token)).getSize(context);
        } else if (f.isAnnotationPresent(FunctionParams.class)) {
            return (token, context) -> Stream.concat(Arrays.stream(((Token[]) fieldGet(f, token))), Stream.of(new EndFunctionParams()))
                    .mapToInt(t -> t.getSize(context)).sum();
        } else {
            throw new IllegalStateException("Unsupported field type: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    public int getSize(BytecodeContext context) {
        return getSizer().getSize(this, context);
    }

    interface Sizer<T extends Token> {
        int getSize(T token, BytecodeContext context);
    }
}
