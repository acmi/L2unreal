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

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import acmi.l2.clientmod.io.annotation.UShort;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class State extends Struct {
    public long probeMask;
    public long ignoreMask;
    @UShort
    public int labelTableOffset;
    public int stateFlags;

    @RequiredArgsConstructor
    @Getter
    public enum Flags {
        /**
         * Flags should be user-selectable in UnrealEd.
         */
        Editable(0x00000001),
        /**
         * Flags is automatic (the default state).
         */
        Auto(0x00000002),
        /**
         * Flags executes on client side.
         */
        Simulated(0x00000004);

        private final int mask;

        public static Collection<Flags> getFlags(int flags) {
            return Arrays.stream(values())
                    .filter(e -> (e.getMask() & flags) != 0)
                    .collect(Collectors.toList());
        }

        public static int getFlags(Flags... flags) {
            int v = 0;
            for (Flags flag : flags) {
                v |= flag.getMask();
            }
            return v;
        }

        @Override
        public String toString() {
            return "STATE_" + name();
        }
    }
}
