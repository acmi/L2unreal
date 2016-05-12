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
package acmi.l2.clientmod.unreal.properties;

import acmi.l2.clientmod.io.DataInput;
import acmi.l2.clientmod.io.ObjectInput;
import acmi.l2.clientmod.io.ObjectInputStream;
import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.unreal.UnrealException;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.core.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.Class;
import java.lang.Object;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PropertiesUtil {
    public static List<L2Property> readProperties(ObjectInput<UnrealRuntimeContext> objectInput, String objClass) throws UnrealException {
        List<L2Property> properties = new ArrayList<>();

        List<Property> classTemplate = null;

        UnrealPackage up = objectInput.getContext().getUnrealPackage();

        try {
            String name;
            while (!(name = objectInput.getContext().getUnrealPackage().getNameTable().get(objectInput.readCompactInt()).getName()).equals("None")) {
                if (classTemplate == null)
                    classTemplate = getPropertyFields(objectInput.getContext().getSerializer(), objClass).collect(Collectors.toList());

                int info = objectInput.readUnsignedByte();
                Type propertyType = Type.values()[info & 0b1111];
                int sizeType = (info >> 4) & 0b111;
                boolean array = info >> 7 == 1;

                String structName = propertyType.equals(Type.STRUCT) ?
                        up.getNameTable().get(objectInput.readCompactInt()).getName() : null;
                int size = readPropertySize(sizeType, objectInput);
                int arrayIndex = array && !propertyType.equals(Type.BOOL) ? objectInput.readCompactInt() : 0;

                byte[] objBytes = new byte[size];
                objectInput.readFully(objBytes);

                final String n = name;
                L2Property property = PropertiesUtil.getAt(properties, n);
                if (property == null) {
                    Property template = classTemplate.parallelStream()
                            .filter(pt -> pt.entry.getObjectName().getName().equalsIgnoreCase((n)))
                            .filter(pt -> match(pt.getClass(), propertyType))
                            .findAny()
                            .orElseThrow(() -> new UnrealException(objClass + ": Property template not found: " + n));

                    property = new L2Property(template);
                    properties.add(property);
                }

                if (structName != null &&
                        !"Vector".equals(structName) &&
                        !"Rotator".equals(structName) &&
                        !"Color".equals(structName)) {
                    StructProperty structProperty = (StructProperty) property.getTemplate();
                    structName = structProperty.struct.entry.getObjectFullName();
                }
                Property arrayInner = null;
                if (propertyType.equals(Type.ARRAY)) {
                    ArrayProperty arrayProperty = (ArrayProperty) property.getTemplate();
                    arrayInner = arrayProperty.inner;
                }

                ObjectInput<UnrealRuntimeContext> objBuffer = new ObjectInputStream<>(new ByteArrayInputStream(objBytes), objectInput.getCharset(), objectInput.getSerializerFactory(), objectInput.getContext());
                property.putAt(arrayIndex, read(objBuffer, propertyType, array, arrayInner, structName));
            }
        } catch (UnrealException e) {
            throw e;
        } catch (Exception e) {
            throw new UnrealException(e);
        }

        return properties;
    }

    private static int readPropertySize(int sizeType, DataInput dataInput) throws IOException {
        switch (sizeType) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 12;
            case 4:
                return 16;
            case 5:
                return dataInput.readUnsignedByte();
            case 6:
                return dataInput.readUnsignedShort();
            case 7:
                return dataInput.readInt();
            default:
                throw new IllegalArgumentException();
        }
    }

    public enum Type {
        NONE,
        BYTE,
        INT,
        BOOL,
        FLOAT,
        OBJECT,
        NAME,
        DELEGATE,
        CLASS,
        ARRAY,
        STRUCT,
        VECTOR,
        ROTATOR,
        STR,
        MAP,
        FIXED_ARRAY
    }

    private static Object read(ObjectInput<UnrealRuntimeContext> objBuffer, Type propertyType, boolean array, Property arrayInner, String structName) throws IOException {
        switch (propertyType) {
            case NONE:
                return null;
            case BYTE:
                return objBuffer.readUnsignedByte();
            case INT:
                return objBuffer.readInt();
            case BOOL:
                return array;
            case FLOAT:
                return objBuffer.readFloat();
            case OBJECT:
                return objBuffer.readCompactInt();
            case NAME:
                return objBuffer.readCompactInt();
            case ARRAY:
                int arraySize = objBuffer.readCompactInt();
                List<Object> arrayList = new ArrayList<>(arraySize);

                String a = arrayInner.getClass().getSimpleName().toUpperCase().replace("PROPERTY", "");
                Property f = arrayInner;

                array = false;
                arrayInner = null;
                structName = null;
                propertyType = Type.valueOf(a);
                if (propertyType == Type.STRUCT) {
                    StructProperty structProperty = (StructProperty) f;
                    structName = structProperty.struct.entry.getObjectFullName();
                }
                if (propertyType == Type.ARRAY) {
                    array = true;
                    ArrayProperty arrayProperty = (ArrayProperty) f;
                    arrayInner = arrayProperty.inner;
                }

                for (int i = 0; i < arraySize; i++) {
                    arrayList.add(read(objBuffer, propertyType, array, arrayInner, structName));
                }
                return arrayList;
            case STRUCT:
                return readStruct(objBuffer, structName);
            case STR:
                return objBuffer.readLine();
            default:
                throw new IllegalStateException("Unk type(" + structName + "): " + propertyType);
        }
    }

    private static List<L2Property> readStruct(ObjectInput<UnrealRuntimeContext> objBuffer, String structName) throws IOException {
        switch (structName) {
            case "Vector":
                return readStructBin(objBuffer, "Core.Object.Vector");
            case "Rotator":
                return readStructBin(objBuffer, "Core.Object.Rotator");
            case "Color":
                return readStructBin(objBuffer, "Core.Object.Color");
            default:
                return readProperties(objBuffer, structName);
        }
    }

    public static List<L2Property> readStructBin(ObjectInput<UnrealRuntimeContext> objBuffer, String structName) throws UnrealException, UncheckedIOException {
        List<Property> properties = getPropertyFields(objBuffer.getContext().getSerializer(), structName).collect(Collectors.toList());

        switch (structName) {
            case "Core.Object.Vector": {
                L2Property x = new L2Property(properties.get(0));
                x.putAt(0, objBuffer.readFloat());
                L2Property y = new L2Property(properties.get(1));
                y.putAt(0, objBuffer.readFloat());
                L2Property z = new L2Property(properties.get(2));
                z.putAt(0, objBuffer.readFloat());
                return Arrays.asList(x, y, z);
            }
            case "Core.Object.Rotator": {
                L2Property pitch = new L2Property(properties.get(0));
                pitch.putAt(0, objBuffer.readInt());
                L2Property yaw = new L2Property(properties.get(1));
                yaw.putAt(0, objBuffer.readInt());
                L2Property roll = new L2Property(properties.get(2));
                roll.putAt(0, objBuffer.readInt());
                return Arrays.asList(pitch, yaw, roll);
            }
            case "Core.Object.Color": {
                L2Property b = new L2Property(properties.get(0));
                b.putAt(0, objBuffer.readUnsignedByte());
                L2Property g = new L2Property(properties.get(1));
                g.putAt(0, objBuffer.readUnsignedByte());
                L2Property r = new L2Property(properties.get(2));
                r.putAt(0, objBuffer.readUnsignedByte());
                L2Property a = new L2Property(properties.get(3));
                a.putAt(0, objBuffer.readUnsignedByte());
                return Arrays.asList(b, g, r, a);
            }
            case "Fire.FireTexture.Spark": {
                L2Property type = new L2Property(properties.get(0));
                type.putAt(0, objBuffer.readUnsignedByte());
                L2Property heat = new L2Property(properties.get(1));
                heat.putAt(0, objBuffer.readUnsignedByte());
                L2Property x = new L2Property(properties.get(2));
                x.putAt(0, objBuffer.readUnsignedByte());
                L2Property y = new L2Property(properties.get(3));
                y.putAt(0, objBuffer.readUnsignedByte());
                L2Property byteA = new L2Property(properties.get(4));
                byteA.putAt(0, objBuffer.readUnsignedByte());
                L2Property byteB = new L2Property(properties.get(5));
                byteB.putAt(0, objBuffer.readUnsignedByte());
                L2Property byteC = new L2Property(properties.get(6));
                byteC.putAt(0, objBuffer.readUnsignedByte());
                L2Property byteD = new L2Property(properties.get(7));
                byteD.putAt(0, objBuffer.readUnsignedByte());
                return Arrays.asList(type, heat, x, y, byteA, byteB, byteC, byteD);
            }
            default:
                throw new IllegalStateException("Not implemented: " + structName); //TODO
        }
    }

    public static L2Property getAt(List<L2Property> properties, String name) {
        return properties.stream()
                .filter(p -> p.getName().equalsIgnoreCase((name)))
                .findFirst()
                .orElse(null);
    }

    public static Stream<Property> getPropertyFields(UnrealSerializerFactory serializer, String structName) {
        Stream<Property> props = Stream.empty();
        while (structName != null) {
            Struct struct = serializer.getStruct(structName)
                    .orElse(new Struct());
            props = Stream.concat(props, StreamSupport.stream(struct.spliterator(), false)
                    .filter(f -> f instanceof Property)
                    .map(f -> (Property) f));
            structName = struct.entry != null && struct.entry.getObjectSuperClass() != null ?
                    struct.entry.getObjectSuperClass().getObjectFullName() : null;
        }
        return props;
    }

    private static boolean match(Class<? extends Property> clazz, Type type) {
        switch (type) {
            case BYTE:
                return ByteProperty.class.isAssignableFrom(clazz);
            case INT:
                return IntProperty.class.isAssignableFrom(clazz);
            case BOOL:
                return BoolProperty.class.isAssignableFrom(clazz);
            case FLOAT:
                return FloatProperty.class.isAssignableFrom(clazz);
            case OBJECT:
                return ObjectProperty.class.isAssignableFrom(clazz);
            case NAME:
                return NameProperty.class.isAssignableFrom(clazz);
            case DELEGATE:
                return DelegateProperty.class.isAssignableFrom(clazz);
            case CLASS:
                return ClassProperty.class.isAssignableFrom(clazz);
            case ARRAY:
                return ArrayProperty.class.isAssignableFrom(clazz);
            case STRUCT:
                return StructProperty.class.isAssignableFrom(clazz);
            case STR:
                return StrProperty.class.isAssignableFrom(clazz);
            default:
                throw new IllegalStateException();
        }
    }
}
