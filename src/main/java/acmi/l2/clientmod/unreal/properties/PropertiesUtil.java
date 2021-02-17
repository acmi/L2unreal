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
package acmi.l2.clientmod.unreal.properties;

import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.unreal.UnrealException;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.core.*;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.lang.Class;
import java.lang.Object;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("unchecked")
public class PropertiesUtil {
    private static final Logger log = Logger.getLogger(PropertiesUtil.class.getName());

    public static List<L2Property> readProperties(ObjectInput<UnrealRuntimeContext> objectInput, String objClass) throws UnrealException {
        List<L2Property> properties = new ArrayList<>();

        List<Property> classTemplate = null;

        UnrealPackage up = objectInput.getContext().getUnrealPackage();

        try {
            String name;
            while (!(name = objectInput.getContext().getUnrealPackage().getNameTable().get(objectInput.readCompactInt()).getName()).equals("None")) {
                if (classTemplate == null) {
                    classTemplate = getPropertyFields(objectInput.getContext().getSerializer(), objClass).collect(Collectors.toList());
                }

                int info = objectInput.readUnsignedByte();
                Type propertyType = getPropertyType(info);
                int sizeType = getPropertySizeType(info);
                boolean array = isArray(info);

                String structName = propertyType == Type.STRUCT ? up.nameReference(objectInput.readCompactInt()) : null;
                int size = readPropertySize(sizeType, objectInput);
                int arrayIndex = array && propertyType != Type.BOOL ? objectInput.readCompactInt() : 0;

                final String n = name;
                L2Property property = PropertiesUtil.getAt(properties, n);
                if (property == null) {
                    Property template = classTemplate.parallelStream()
                            .filter(pt -> pt.entry.getObjectName().getName().equalsIgnoreCase((n)))
                            .filter(pt -> match(pt.getClass(), propertyType))
                            .findAny()
                            .orElse(null);
                    if (template == null) {
                        log.warning(() -> objClass + ": Property template not found: " + n);
                    } else {
                        property = new L2Property(template);
                        properties.add(property);
                    }
                }

                if (property != null) {
                    Struct struct = null;
                    if (propertyType == Type.STRUCT) {
                        StructProperty structProperty = (StructProperty) property.getTemplate();
                        struct = structProperty.struct;
                    }
                    Property arrayInner = null;
                    if (propertyType.equals(Type.ARRAY)) {
                        ArrayProperty arrayProperty = (ArrayProperty) property.getTemplate();
                        arrayInner = arrayProperty.inner;
                    }
                    property.putAt(arrayIndex, read(objectInput, propertyType, array, arrayInner, struct));
                } else {
                    objectInput.skip(size);
                }
            }
        } catch (UnrealException e) {
            throw e;
        } catch (Exception e) {
            throw new UnrealException(e);
        }

        return properties;
    }

    public static Object read(ObjectInput<UnrealRuntimeContext> objBuffer, Type propertyType, boolean array, Property arrayInner, Struct struct) throws UncheckedIOException {
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
                struct = null;
                propertyType = Type.valueOf(a);
                if (propertyType == Type.STRUCT) {
                    StructProperty structProperty = (StructProperty) f;
                    struct = structProperty.struct;
                }
                if (propertyType == Type.ARRAY) {
                    array = true;
                    ArrayProperty arrayProperty = (ArrayProperty) f;
                    arrayInner = arrayProperty.inner;
                }

                for (int i = 0; i < arraySize; i++) {
                    arrayList.add(read(objBuffer, propertyType, array, arrayInner, struct));
                }
                return arrayList;
            case STRUCT:
                return readStruct(objBuffer, struct);
            case STR:
                return objBuffer.readLine();
            default:
                throw new IllegalStateException("Unk type: " + propertyType);
        }
    }

    public static List<L2Property> readStruct(ObjectInput<UnrealRuntimeContext> objBuffer, Struct struct) throws UncheckedIOException {
        String structName = struct.entry.getObjectFullName();
        switch (structName) {
            case "Core.Object.Vector":
            case "Core.Object.Rotator":
            case "Core.Object.Color":
                return readStructBin(objBuffer, structName);
            default:
                return readProperties(objBuffer, structName);
        }
    }

    public static List<L2Property> readStructBin(ObjectInput<UnrealRuntimeContext> objBuffer, String structName) throws UnrealException, UncheckedIOException {
        List<Property> properties = getPropertyFields(objBuffer.getContext().getSerializer(), structName).collect(Collectors.toList());

        return properties.stream()
                .map(L2Property::new)
                .peek(l2Property -> l2Property.putAt(0, read(objBuffer, getType(l2Property.getTemplate()), false, null, null)))
                .collect(Collectors.toList());
    }

    public static void writeProperties(ObjectOutput<UnrealRuntimeContext> output, List<L2Property> properties) {
        UnrealPackage up = output.getContext().getUnrealPackage();

        for (L2Property property : properties) {
            Property template = property.getTemplate();

            for (int i = 0; i < property.getSize(); i++) {
                Object obj = property.getAt(i);
                if (obj == null) {
                    continue;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutput<UnrealRuntimeContext> objBuffer = new ObjectOutputStream<>(baos, output.getCharset(), 0, output.getSerializerFactory(), output.getContext());
                write(objBuffer, template, obj);
                byte[] bytes = baos.toByteArray();

                Type type = getType(template);
                int size = getPropertySize(bytes.length);
                boolean array = (i > 0) || (type == Type.BOOL && ((Boolean) obj));
                int info = (array ? 1 << 7 : 0) | (size << 4) | type.ordinal();

                output.writeCompactInt(nameReference(up, template.entry.getObjectName().getName()));
                output.writeByte(info);
                if (type == Type.STRUCT) {
                    output.writeCompactInt(nameReference(up, ((StructProperty) template).struct.entry.getObjectName().getName()));
                }
                switch (size) {
                    case 5:
                        output.writeByte(bytes.length);
                        break;
                    case 6:
                        output.writeShort(bytes.length);
                        break;
                    case 7:
                        output.writeInt(bytes.length);
                        break;
                }
                if (i > 0) {
                    output.writeByte(i);
                }
                output.writeBytes(bytes);
            }
        }
        output.writeCompactInt(up.nameReference("None"));
    }

    public static void write(ObjectOutput<UnrealRuntimeContext> objBuffer, Property template, Object obj) throws UncheckedIOException {
        if (template instanceof ByteProperty) {
            objBuffer.writeByte((Integer) obj);
        } else if (template instanceof IntProperty) {
            objBuffer.writeInt((Integer) obj);
        } else if (template instanceof BoolProperty) {
            //nothing
        } else if (template instanceof FloatProperty) {
            objBuffer.writeFloat((Float) obj);
        } else if (template instanceof ObjectProperty) {
            objBuffer.writeCompactInt((Integer) obj);
        } else if (template instanceof NameProperty) {
            objBuffer.writeCompactInt((Integer) obj);
        } else if (template instanceof ArrayProperty) {
            ArrayProperty arrayProperty = (ArrayProperty) template;

            List<Object> arrayList = (List<Object>) obj;
            objBuffer.writeCompactInt(arrayList.size());

            Property arrayInner = arrayProperty.inner;
            for (Object arrayObj : arrayList) {
                write(objBuffer, arrayInner, arrayObj);
            }
        } else if (template instanceof StructProperty) {
            StructProperty structProperty = (StructProperty) template;
            writeStruct(objBuffer, structProperty.struct.getFullName(), (List<L2Property>) obj);
        } else if (template instanceof StrProperty) {
            objBuffer.writeLine((String) obj);
        } else {
            throw new UnsupportedOperationException(template.getClass().getSimpleName() + " serialization not implemented");
        }
    }

    public static void writeStruct(ObjectOutput<UnrealRuntimeContext> objBuffer, String structName, List<L2Property> struct) throws UncheckedIOException {
        switch (structName) {
            case "Core.Object.Color":
            case "Core.Object.Vector":
            case "Core.Object.Rotator":
                writeStructBin(objBuffer, struct, structName);
                break;
            default:
                writeProperties(objBuffer, struct);
        }
    }

    public static void writeStructBin(DataOutput objBuffer, List<L2Property> struct, String structName) throws UncheckedIOException {
        switch (structName) {
            case "Core.Object.Color":
                objBuffer.writeByte(struct.stream().filter(p -> p.getName().equalsIgnoreCase("R")).mapToInt(p -> (Integer) p.getAt(0)).findAny().orElse(0));
                objBuffer.writeByte(struct.stream().filter(p -> p.getName().equalsIgnoreCase("G")).mapToInt(p -> (Integer) p.getAt(0)).findAny().orElse(0));
                objBuffer.writeByte(struct.stream().filter(p -> p.getName().equalsIgnoreCase("B")).mapToInt(p -> (Integer) p.getAt(0)).findAny().orElse(0));
                objBuffer.writeByte(struct.stream().filter(p -> p.getName().equalsIgnoreCase("A")).mapToInt(p -> (Integer) p.getAt(0)).findAny().orElse(0));
                break;
            case "Core.Object.Vector":
                objBuffer.writeFloat(struct.stream().filter(p -> p.getName().equalsIgnoreCase("X")).map(p -> (Float) p.getAt(0)).findAny().orElse(0f));
                objBuffer.writeFloat(struct.stream().filter(p -> p.getName().equalsIgnoreCase("Y")).map(p -> (Float) p.getAt(0)).findAny().orElse(0f));
                objBuffer.writeFloat(struct.stream().filter(p -> p.getName().equalsIgnoreCase("Z")).map(p -> (Float) p.getAt(0)).findAny().orElse(0f));
                break;
            case "Core.Object.Rotator":
                objBuffer.writeInt(struct.stream().filter(p -> p.getName().equalsIgnoreCase("Pitch")).mapToInt(p -> (Integer) p.getAt(0)).findAny().orElse(0));
                objBuffer.writeInt(struct.stream().filter(p -> p.getName().equalsIgnoreCase("Yaw")).mapToInt(p -> (Integer) p.getAt(0)).findAny().orElse(0));
                objBuffer.writeInt(struct.stream().filter(p -> p.getName().equalsIgnoreCase("Roll")).mapToInt(p -> (Integer) p.getAt(0)).findAny().orElse(0));
                break;
            default:
                throw new UnsupportedOperationException("not implemented");
        }
    }

    private static int nameReference(UnrealPackage up, String name) {
        int nameRef = up.nameReference(name);
        if (nameRef == -1) {
            try {
                up.addNameEntries(name);
            } catch (UncheckedIOException e) {
                throw new UnrealException("Couldn't add name entry: " + name, e);
            }
        }
        return up.nameReference(name);
    }

    @RequiredArgsConstructor
    public enum Type {
        NONE(null),
        BYTE(ByteProperty.class),
        INT(IntProperty.class),
        BOOL(BoolProperty.class),
        FLOAT(FloatProperty.class),
        OBJECT(ObjectProperty.class),
        NAME(NameProperty.class),
        _DELEGATE(DelegateProperty.class),
        _CLASS(ClassProperty.class),
        ARRAY(ArrayProperty.class),
        STRUCT(StructProperty.class),
        _VECTOR(StructProperty.class),
        _ROTATOR(StructProperty.class),
        STR(StrProperty.class),
        _MAP(null),
        _FIXED_ARRAY(null);

        private final Class<? extends Property> clazz;
    }

    public static Type getPropertyType(int info) {
        return Type.values()[info & 0b1111];
    }

    public static int getPropertySizeType(int info) {
        return (info >> 4) & 0b111;
    }

    public static int getPropertySize(int size) {
        switch (size) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 4:
                return 2;
            case 12:
                return 3;
            case 16:
                return 4;
            default:
                if (size < 0x100) {
                    return 5;
                } else if (size < 0x10000) {
                    return 6;
                } else {
                    return 7;
                }
        }
    }

    public static boolean isArray(int info) {
        return info >> 7 != 0;
    }

    public static int readPropertySize(int sizeType, DataInput dataInput) throws UncheckedIOException {
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
                throw new IllegalArgumentException(String.valueOf(sizeType));
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
        return type.clazz != null && type.clazz.isAssignableFrom(clazz);
    }

    public static Type getType(Property property) {
        for (Type type : Type.values()) {
            if (match(property.getClass(), type)) {
                return type;
            }
        }
        throw new IllegalStateException();
    }

    public static boolean isPrimitive(Property property) {
        return property instanceof ByteProperty ||
                property instanceof IntProperty ||
                property instanceof BoolProperty ||
                property instanceof FloatProperty ||
                property instanceof StrProperty;
    }

    public static List<L2Property> cloneStruct(List<L2Property> struct) {
        return struct.stream()
                .map(L2Property::copy)
                .collect(Collectors.toList());
    }

    public static L2Property create(Property property, String structName, UnrealSerializerFactory serializer, UnrealPackage up) {
        L2Property l2property = new L2Property(property);
        for (int i = 0; i < l2property.getSize(); i++) {
            l2property.putAt(i, defaultValue(property, structName, serializer, up));
        }
        return l2property;
    }

    public static Object defaultValue(Property property, String structName, UnrealSerializerFactory serializer, UnrealPackage unrealPackage) {
        Optional<acmi.l2.clientmod.unreal.core.Class> classOpt = serializer.getStruct(structName)
                .filter(struct -> struct instanceof acmi.l2.clientmod.unreal.core.Class)
                .map(struct -> (acmi.l2.clientmod.unreal.core.Class) struct);

        if (classOpt.isPresent()) {
            Object[] defaultValue = new Object[1];
            serializer.getStructTree(classOpt.get()).forEach(superClass -> superClass.properties
                    .parallelStream()
                    .filter(l2Property -> l2Property.getTemplate().equals(property))
                    .findAny()
                    .ifPresent(l2Property -> defaultValue[0] = l2Property.getAt(0)));
            if (defaultValue[0] != null) {
                if (PropertiesUtil.isPrimitive(property)) {
                    return defaultValue[0];
                } else if (property instanceof StructProperty) {
                    return PropertiesUtil.cloneStruct((List<L2Property>) defaultValue[0]);
                }
            }
        }

        return defaultValue(property, serializer, unrealPackage);
    }

    public static Object defaultValue(Property property, UnrealSerializerFactory serializer, UnrealPackage unrealPackage) {
        if (property instanceof ByteProperty) {
            return 0;
        } else if (property instanceof IntProperty) {
            return 0;
        } else if (property instanceof BoolProperty) {
            return false;
        } else if (property instanceof FloatProperty) {
            return 0f;
        } else if (property instanceof ObjectProperty) {
            return 0;
        } else if (property instanceof NameProperty) {
            return unrealPackage.nameReference("None");
        } else if (property instanceof StructProperty) {
            String structName = ((StructProperty) property).struct.getFullName();
            return getProperties(structName, serializer, true, true)
                    .map(p -> create(p, structName, serializer, unrealPackage))
                    .collect(Collectors.toList());
        } else if (property instanceof ArrayProperty) {
            return new ArrayList();
        } else if (property instanceof StrProperty) {
            return "";
        }
        throw new IllegalStateException(String.valueOf(property));
    }

    public static Stream<Property> getProperties(String structName, UnrealSerializerFactory serializer, boolean editOnly, boolean hideCategories) {
        Struct struct = serializer.getStruct(structName).orElse(new Struct());

        return PropertiesUtil.getPropertyFields(serializer, structName)
                .filter(p -> !editOnly || (p.propertyFlags & Property.CPF.Edit.getMask()) != 0)
                .filter(p -> !(struct instanceof acmi.l2.clientmod.unreal.core.Class) || (!hideCategories || !Arrays.asList(((acmi.l2.clientmod.unreal.core.Class) struct).hideCategories).contains(p.category)));
    }

    public static void removeDefaults(List<L2Property> properties, String structName, UnrealSerializerFactory serializer, UnrealPackage unrealPackage) {
        if (properties == null) {
            return;
        }

        for (Iterator<L2Property> it = properties.iterator(); it.hasNext(); ) {
            L2Property property = it.next();

            java.lang.Object def = defaultValue(property.getTemplate(), structName, serializer, unrealPackage);

            boolean del = true;
            for (int i = 0; i < property.getSize(); i++) {
                java.lang.Object obj = property.getAt(i);

                if (property.getTemplate() instanceof StructProperty) {
                    if (obj == null) {
                        continue;
                    }

                    List<L2Property> struct = (List<L2Property>) obj;
                    removeDefaults(struct, ((StructProperty) property.getTemplate()).struct.getFullName(), serializer, unrealPackage);
                    if (!struct.isEmpty()) {
                        del = false;
                    } else {
                        property.putAt(i, null);
                    }
                } else if (!def.equals(obj)) {
                    del = false;
                }
            }
            if (del) {
                it.remove();
            }
        }
    }
}
