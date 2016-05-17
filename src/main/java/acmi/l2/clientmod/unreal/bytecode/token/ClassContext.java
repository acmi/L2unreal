/*
 * Copyright (c) 2016 acmi
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

import acmi.l2.clientmod.io.annotation.UByte;
import acmi.l2.clientmod.io.annotation.UShort;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;

public class ClassContext extends Token {
    public static final int OPCODE = 0x12;

    public Token clazz;
    @UShort
    public int wSkip;   //member size
    @UByte
    public int bSize;   //member call result size
    public Token member;

    public ClassContext(Token clazz, int wSkip, int bSize, Token member) {
        this.clazz = clazz;
        this.wSkip = wSkip;
        this.bSize = bSize;
        this.member = member;
    }

    public ClassContext() {
    }

    @Override
    protected int getOpcode() {
        return OPCODE;
    }

    @Override
    public String toString() {
        return "ClassContext("
                + clazz
                + ", " + wSkip
                + ", " + bSize
                + ", " + member
                + ')';
    }

    @Override
    public String toString(UnrealRuntimeContext context) {
        return clazz.toString(context) + "." + member.toString(context);
    }
}
