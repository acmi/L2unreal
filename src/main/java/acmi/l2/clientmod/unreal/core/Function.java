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
package acmi.l2.clientmod.unreal.core;

import acmi.l2.clientmod.io.DataInput;
import acmi.l2.clientmod.io.DataOutput;
import acmi.l2.clientmod.io.annotation.ReadMethod;
import acmi.l2.clientmod.io.annotation.UByte;
import acmi.l2.clientmod.io.annotation.UShort;
import acmi.l2.clientmod.io.annotation.WriteMethod;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class Function extends Struct {
    @UShort
    public int nativeIndex;
    @UByte
    public int operatorPrecedence;
    public int functionFlags;
    @UShort
    public int replicationOffset;

    @ReadMethod
    public void readFunction(DataInput input) {
        nativeIndex = input.readUnsignedShort();
        operatorPrecedence = input.readUnsignedByte();
        functionFlags = input.readInt();
        replicationOffset = (functionFlags & Flag.NET.getMask()) != 0 ? input.readUnsignedShort() : 0;
    }

    @WriteMethod
    public final void writeFunction(DataOutput output) {
        output.writeShort(nativeIndex);
        output.writeByte(operatorPrecedence);
        output.writeInt(functionFlags);
        if ((functionFlags & Flag.NET.getMask()) != 0) {
            output.writeShort(replicationOffset);
        }
    }

    @Getter
    public enum Flag {
        /**
         * Function is final (prebindable, non-overridable function).
         */
        FINAL,
        /**
         * Function has been defined (not just declared).
         */
        DEFINED,
        /**
         * Function is an iterator.
         */
        ITERATOR,
        /**
         * Function is a latent state function.
         */
        LATENT,
        /**
         * Unary operator is a prefix operator.
         */
        PRE_OPERATOR,
        /**
         * Function cannot be reentered.
         */
        SINGULAR,
        /**
         * Function is network-replicated.
         */
        NET,
        /**
         * Function should be sent reliably on the network.
         */
        NET_RELIABLE,
        /**
         * Function executed on the client side.
         */
        SIMULATED,
        /**
         * Executable from command line.
         */
        EXEC,
        /**
         * Native function.
         */
        NATIVE,
        /**
         * Event function.
         */
        EVENT,
        /**
         * Operator function.
         */
        OPERATOR,
        /**
         * Static function.
         */
        STATIC,
        /**
         * Don't export intrinsic function to C++.
         */
        NO_EXPORT,
        /**
         * Function doesn't modify this object.
         */
        CONST,
        /**
         * Return value is purely dependent on parameters; no state dependencies or internal state changes.
         */
        INVARIANT,
        PROTECTED,
        Flag18,
        Flag19,
        /**
         * Function is a delegate
         */
        DELEGATE;

        private final int mask = 1 << ordinal();

        @Override
        public String toString() {
            return "FF_" + name();
        }

        public static Collection<Flag> getFlags(int flags) {
            return Arrays.stream(values())
                    .filter(e -> (e.getMask() & flags) != 0)
                    .collect(Collectors.toList());
        }

        public static int getFlags(Flag... flags) {
            int v = 0;
            for (Flag flag : flags) {
                v |= flag.getMask();
            }
            return v;
        }
    }
}
