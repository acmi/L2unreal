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
package acmi.l2.clientmod.unreal.bytecode;

import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.unreal.UnrealException;
import acmi.l2.clientmod.unreal.bytecode.token.*;
import acmi.l2.clientmod.unreal.bytecode.token.Context;
import acmi.l2.clientmod.unreal.bytecode.token.annotation.ConversionToken;
import acmi.l2.clientmod.unreal.bytecode.token.annotation.FunctionParams;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class TokenSerializerFactory extends ReflectionSerializerFactory<BytecodeContext> {
    private static final Logger log = Logger.getLogger(TokenSerializerFactory.class.getName());

    private static final int EX_ExtendedNative = 0x60;
    private static final int EX_FirstNative = 0x70;

    private static final Map<Integer, Class<? extends Token>> mainTokenTable = new HashMap<>();
    private static final Map<Integer, Class<? extends Token>> conversionTokenTable = new HashMap<>();

    @Override
    protected Function<ObjectInput<BytecodeContext>, Object> createInstantiator(Class<?> clazz) {
        if (Token.class.isAssignableFrom(clazz)) {
            return this::instantiate;
        }
        return super.createInstantiator(clazz);
    }

    private Token instantiate(ObjectInput<BytecodeContext> input) throws UncheckedIOException {
        int opcode = input.readUnsignedByte();
        Map<Integer, Class<? extends Token>> table;
        String tableName;
        if (input.getContext().isConversion()) {
            table = conversionTokenTable;
            tableName = "Conversion";

            input.getContext().changeConversion();
        } else {
            if (opcode >= EX_ExtendedNative) {
                return readNativeCall(input, opcode);
            }

            table = mainTokenTable;
            tableName = "Main";

            if (opcode == ConversionTable.OPCODE) {
                input.getContext().changeConversion();
            }
        }

        Class<? extends Token> tokenClass = table.get(opcode);

        if (tokenClass == null) {
            throw new UncheckedIOException(new IOException(String.format("Unknown token: %02x, table: %s", opcode, tableName)));
        }

        try {
            return tokenClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new SerializerException(e);
        }
    }

    private Token readNativeCall(ObjectInput<BytecodeContext> input, int b) throws UncheckedIOException {
        int nativeIndex = (b & 0xF0) == EX_ExtendedNative ?
                ((b - EX_ExtendedNative) << 8) + input.readUnsignedByte() : b;

        if (nativeIndex < EX_FirstNative) {
            throw new UnrealException("Invalid native index: " + nativeIndex);
        }

        return new NativeFunctionCall(nativeIndex);
    }

    @Override
    protected <T> void serializer(Class type, Function<T, Object> getter, BiConsumer<T, Supplier> setter, Function<Class<? extends Annotation>, Annotation> getAnnotation, List<BiConsumer<T, ObjectInput<BytecodeContext>>> read, List<BiConsumer<T, ObjectOutput<BytecodeContext>>> write) {
        if (type == String.class) {
            read.add((object, dataInput) -> setter.accept(object, () -> readString(dataInput)));
            write.add((object, dataOutput) -> writeString(dataOutput, (String) getter.apply(object)));
        } else if (getAnnotation.apply(FunctionParams.class) != null) {
            read.add((object, dataInput) -> setter.accept(object, () -> readFunctionParams(dataInput)));
            write.add((object, dataOutput) -> writeFunctionParams(dataOutput, (Token[]) getter.apply(object)));
        } else {
            super.serializer(type, getter, setter, getAnnotation, read, write);
        }
    }

    private static String readString(DataInput input) throws UncheckedIOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = input.readUnsignedByte()) != 0) {
            baos.write(b);
        }
        return new String(baos.toByteArray(), input.getCharset());
    }

    private static void writeString(ObjectOutput<BytecodeContext> output, String string) throws UncheckedIOException {
        output.writeBytes((string + '\0').getBytes(output.getCharset()));
    }

    private static Token readToken(ObjectInput<BytecodeContext> input) throws UncheckedIOException {
        return input.readObject(Token.class);
    }

    private static Token[] readFunctionParams(ObjectInput<BytecodeContext> input) throws UncheckedIOException {
        List<Token> tokens = new ArrayList<>();
        Token tmp;
        do {
            tmp = readToken(input);
            if (tmp instanceof EndFunctionParams) {
                break;
            }
            tokens.add(tmp);
        } while (true);
        return tokens.toArray(new Token[0]);
    }

    private static void writeFunctionParams(ObjectOutput<BytecodeContext> output, Token[] params) throws UncheckedIOException {
        for (Token token : params) {
            output.write(token);
        }
        output.write(new EndFunctionParams());
    }

    public static int getNoneInd(@NonNull BytecodeContext context) {
        return context.getUnrealPackage().nameReference("None");
    }

    static {
        //Main
        register(LocalVariable.class);     //00
        register(InstanceVariable.class);  //01
        register(DefaultVariable.class);   //02

        register(Return.class);            //04
        register(Switch.class);            //05
        register(Jump.class);              //06
        register(JumpIfNot.class);         //07
        register(Stop.class);              //08
        register(Assert.class);            //09
        register(Case.class);              //0a
        register(Nothing.class);           //0b
        register(LabelTable.class);        //0c
        register(GotoLabel.class);         //0d
        register(EatString.class);         //0e
        register(Let.class);               //0f
        register(DynArrayElement.class);   //10
        register(New.class);               //11
        register(ClassContext.class);      //12
        register(Metacast.class);          //13
        register(LetBool.class);           //14

        register(EndFunctionParams.class); //16
        register(Self.class);              //17
        register(Skip.class);              //18
        register(Context.class);           //19
        register(ArrayElement.class);      //1a
        register(VirtualFunction.class);   //1b
        register(FinalFunction.class);     //1c
        register(IntConst.class);          //1d
        register(FloatConst.class);        //1e
        register(StringConst.class);       //1f
        register(ObjectConst.class);       //20
        register(NameConst.class);         //21
        register(RotatorConst.class);      //22
        register(VectorConst.class);       //23
        register(ByteConst.class);         //24
        register(IntZero.class);           //25
        register(IntOne.class);            //26
        register(True.class);              //27
        register(False.class);             //28
        register(NativeParam.class);       //29
        register(NoObject.class);          //2a

        register(IntConstByte.class);      //2c
        register(BoolVariable.class);      //2d
        register(DynamicCast.class);       //2e
        register(Iterator.class);          //2f
        register(IteratorPop.class);       //30
        register(IteratorNext.class);      //31
        register(StructCmpEq.class);       //32
        register(StructCmpNe.class);       //33

        register(StructMember.class);      //36
        register(Length.class);            //37
        register(GlobalFunction.class);    //38
        register(ConversionTable.class, true);   //39
        register(ConversionTable.class, false);
        register(Insert.class);            //40
        register(Remove.class);            //41

        register(DelegateName.class);      //44

        register(INT64Const.class);        //46
        register(DynArraySort.class);      //47

        //Conversion
        register(ByteToInt.class);         //3a
        register(ByteToBool.class);        //3b
        register(ByteToFloat.class);       //3c
        register(IntToByte.class);         //3d
        register(IntToBool.class);         //3e
        register(IntToFloat.class);        //3f
        register(BoolToByte.class);        //40
        register(BoolToInt.class);         //41
        register(BoolToFloat.class);       //42
        register(FloatToByte.class);       //43
        register(FloatToInt.class);        //44
        register(FloatToBool.class);       //45
        register(StringToName.class);      //46
        register(ObjectToBool.class);      //47
        register(NameToBool.class);        //48
        register(StringToByte.class);      //49
        register(StringToInt.class);       //4a
        register(StringToBool.class);      //4b
        register(StringToFloat.class);     //4c
        register(StringToVector.class);    //4d
        register(StringToRotator.class);   //4e
        register(VectorToBool.class);      //4f
        register(VectorToRotator.class);   //50
        register(RotatorToBool.class);     //51
        register(ByteToString.class);      //52
        register(IntToString.class);       //53
        register(BoolToString.class);      //54
        register(FloatToString.class);     //55
        register(ObjectToString.class);    //56
        register(NameToString.class);      //57
        register(VectorToString.class);    //58
        register(RotatorToString.class);   //59
        register(ByteToINT64.class);       //5a
        register(IntToINT64.class);        //5b
        register(BoolToINT64.class);       //5c
        register(FloatToINT64.class);      //5d
        register(StringToINT64.class);     //5e
        register(INT64ToByte.class);       //5f
        register(INT64ToInt.class);        //60
        register(INT64ToBool.class);       //61
        register(INT64ToFloat.class);      //62
        register(INT64ToString.class);     //63
    }

    private static void register(Class<? extends Token> clazz, boolean conversion) {
        Map<Integer, Class<? extends Token>> table;

        if (conversion) {
            table = conversionTokenTable;
        } else {
            table = mainTokenTable;
        }

        try {
            Class<? extends Token> old = table.put(clazz.getDeclaredField("OPCODE").getInt(null), clazz);
            if (old != null) {
                log.info(old + " replaced with " + clazz);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(String.format("Couldn't register %s opcode", clazz), e);
        }
    }

    public static void register(@NonNull Class<? extends Token> clazz) {
        register(clazz, clazz.isAnnotationPresent(ConversionToken.class));
    }
}
