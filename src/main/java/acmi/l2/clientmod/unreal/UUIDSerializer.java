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
package acmi.l2.clientmod.unreal;

import acmi.l2.clientmod.io.Context;
import acmi.l2.clientmod.io.ObjectInput;
import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.Serializer;

import java.io.UncheckedIOException;
import java.util.UUID;

import static acmi.l2.clientmod.io.ByteUtil.uuidFromBytes;
import static acmi.l2.clientmod.io.ByteUtil.uuidToBytes;

public class UUIDSerializer implements Serializer<UUID, Context> {
    @Override
    public UUID instantiate(ObjectInput<Context> input) throws UncheckedIOException {
        byte[] uuid = new byte[16];
        input.readFully(uuid);
        return uuidFromBytes(uuid);
    }

    @Override
    public <S extends UUID> void readObject(S obj, ObjectInput<Context> input) throws UncheckedIOException {
    }

    @Override
    public <S extends UUID> void writeObject(S obj, ObjectOutput<Context> output) throws UncheckedIOException {
        output.writeBytes(uuidToBytes(obj));
    }
}
