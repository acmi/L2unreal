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
package acmi.l2.clientmod.unreal;

import acmi.l2.clientmod.io.ObjectInput;
import acmi.l2.clientmod.io.ObjectInputStream;
import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.unreal.annotation.Bytecode;
import acmi.l2.clientmod.unreal.annotation.NameRef;
import acmi.l2.clientmod.unreal.annotation.ObjectRef;
import acmi.l2.clientmod.unreal.bytecode.BytecodeContext;
import acmi.l2.clientmod.unreal.bytecode.TokenSerializerFactory;
import acmi.l2.clientmod.unreal.bytecode.token.Token;
import acmi.l2.clientmod.unreal.core.Object;
import acmi.l2.clientmod.unreal.core.Struct;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class UnrealSerializerFactory extends ReflectionSerializerFactory<UnrealRuntimeContext> {
    private static final Logger log = Logger.getLogger(UnrealSerializerFactory.class.getName());

    public static String unrealClassesPackage = "acmi.l2.clientmod.unreal";

    public static final Predicate<String> IS_STRUCT = c -> c.equalsIgnoreCase("Core.Struct") ||
            c.equalsIgnoreCase("Core.Function") ||
            c.equalsIgnoreCase("Core.State") ||
            c.equalsIgnoreCase("Core.Class");

    private ForkJoinPool forkJoinPool = new ForkJoinPool();

    private Map<UnrealPackage.Entry, Object> objects = new HashMap<>();
    private Map<Integer, acmi.l2.clientmod.unreal.core.Function> nativeFunctions = new HashMap<>();
    private ObservableSet<String> loaded = FXCollections.observableSet(new HashSet<>());

    private Environment environment;

    public UnrealSerializerFactory(Environment environment) {
        this.environment = new EnvironmentWrapper(environment.getStartDir(), environment.getPaths());
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Override
    protected Function<ObjectInput<UnrealRuntimeContext>, java.lang.Object> createInstantiator(Class<?> clazz) {
        if (Object.class.isAssignableFrom(clazz))
            return objectInput -> {
                UnrealRuntimeContext context = objectInput.getContext();
                int objRef = objectInput.readCompactInt();
                UnrealPackage.Entry entry = context.getUnrealPackage().objectReference(objRef);
                return getOrCreateObject(entry);
            };
        return super.createInstantiator(clazz);
    }

    public Object getOrCreateObject(UnrealPackage.Entry packageLocalEntry) throws UncheckedIOException {
        if (packageLocalEntry == null)
            return null;

        if (!objects.containsKey(packageLocalEntry)) {
            log.fine(() -> String.format("Loading %s", packageLocalEntry));

            UnrealPackage.ExportEntry entry;
            try {
                entry = resolveExportEntry(packageLocalEntry).orElse(null);
                if (entry != null) {
                    Class<? extends Object> clazz = getClass(entry.getFullClassName());
                    Object obj = clazz.newInstance();
                    objects.put(entry, obj);
                    load(obj, entry);
                } else {
                    create(packageLocalEntry.getObjectFullName(), packageLocalEntry.getFullClassName());
                }
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        return objects.get(packageLocalEntry);
    }

    private void create(String objName, String objClass) {
        log.fine(() -> String.format("Create dummy %s[%s]", objName, objClass));

        UnrealPackage.Entry entry = new UnrealPackage.Entry(null, 0, 0, 0) {
            @Override
            public String getObjectFullName() {
                return objName;
            }

            @Override
            public String getFullClassName() {
                return objClass;
            }

            @Override
            public int getObjectReference() {
                return 0;
            }

            @Override
            public List getTable() {
                return null;
            }
        };
        objects.put(entry, new acmi.l2.clientmod.unreal.core.Class() {
            @Override
            public String getFullName() {
                return objName;
            }

            @Override
            public String getClassFullName() {
                return objClass;
            }
        });
    }

    public Object getOrCreateObject(String objName, Predicate<String> objClass) throws UncheckedIOException {
        return getOrCreateObject(getExportEntry(objName, objClass)
                .orElseThrow(() -> new UnrealException("Entry " + objName + " not found")));
    }

    public Optional<UnrealPackage.ExportEntry> resolveExportEntry(UnrealPackage.Entry entry) {
        if (entry == null)
            return Optional.empty();

        return entry instanceof UnrealPackage.ExportEntry ?
                Optional.of((UnrealPackage.ExportEntry) entry) :
                getExportEntry(entry.getObjectFullName(), clazz -> clazz.equalsIgnoreCase(entry.getFullClassName()));
    }

    private Optional<UnrealPackage.ExportEntry> getExportEntry(String objName, Predicate<String> objClass) {
        return environment.getExportEntry(objName, objClass);
    }

    private Class<? extends Object> getClass(String className) throws UncheckedIOException {
        Class<?> clazz = null;
        try {
            String javaClassName = unrealClassesPackage + "." + unrealClassNameToJavaClassName(className);
            log.finer(() -> String.format("unreal[%s]->jvm[%s]", className, javaClassName));
            clazz = Class.forName(javaClassName);
            return clazz.asSubclass(Object.class);
        } catch (ClassNotFoundException e) {
            log.finer(() -> String.format("Class %s not implemented in java", className));
        } catch (ClassCastException e) {
            Class<?> clazzLocal = clazz;
            log.warning(() -> String.format("%s is not subclass of %s", clazzLocal, Object.class));
        }

        return getClass(getExportEntry(className, c -> c.equalsIgnoreCase("Core.Class"))
                .map(e -> e.getObjectSuperClass().getObjectFullName())
                .orElse("Core.Object"));
//        Object object = getOrCreateObject(className, c -> c.equalsIgnoreCase("Core.Class"));
//        return getClass(object.entry.getObjectSuperClass().getObjectFullName());
    }

    private String unrealClassNameToJavaClassName(String className) {
        String[] path = className.split("\\.");
        return String.format("%s.%s", path[0].toLowerCase(), path[1]);
    }

    @Override
    protected <T> void serializer(Class type, Function<T, java.lang.Object> getter, BiConsumer<T, Supplier> setter, Function<Class<? extends Annotation>, Annotation> getAnnotation, List<BiConsumer<T, ObjectInput<UnrealRuntimeContext>>> read, List<BiConsumer<T, ObjectOutput<UnrealRuntimeContext>>> write) {
        if (type == String.class &&
                Objects.nonNull(getAnnotation.apply(NameRef.class))) {
            read.add((object, dataInput) ->
                    setter.accept(object, () ->
                            dataInput.getContext().getUnrealPackage().nameReference(dataInput.readCompactInt())));
            write.add((object, dataOutput) -> dataOutput.writeCompactInt(dataOutput.getContext().getUnrealPackage().nameReference((String) getter.apply(object))));
        } else if (Object.class.isAssignableFrom(type) &&
                Objects.nonNull(getAnnotation.apply(ObjectRef.class))) {
            read.add((object, dataInput) -> setter.accept(object, () ->
                    getOrCreateObject(dataInput.getContext().getUnrealPackage().objectReference(dataInput.readCompactInt()))));
            write.add((object, dataOutput) -> {
                Object obj = (Object) getter.apply(object);
                dataOutput.writeCompactInt(obj.entry.getObjectReference());
            });
        } else if (type.isArray() &&
                type.getComponentType() == Token.class &&
                Objects.nonNull(getAnnotation.apply(Bytecode.class))) {
            read.add((object, dataInput) -> {
                BytecodeContext context = new BytecodeContext(dataInput.getContext());
                SerializerFactory<BytecodeContext> serializerFactory = new TokenSerializerFactory();
                ObjectInput<BytecodeContext> input = ObjectInput.objectInput(dataInput, serializerFactory, context);
                int size = input.readInt();
                int readSize = 0;
                List<Token> tokens = new ArrayList<>();
                while (readSize < size) {
                    Token token = input.readObject(Token.class);
                    readSize += token.getSize(context);
                    tokens.add(token);
                }
                setter.accept(object, () -> tokens.toArray(new Token[tokens.size()]));
            });
            write.add((object, dataOutput) -> {
                BytecodeContext context = new BytecodeContext(dataOutput.getContext());
                SerializerFactory<BytecodeContext> serializerFactory = new TokenSerializerFactory();
                ObjectOutput<BytecodeContext> output = ObjectOutput.objectOutput(dataOutput, serializerFactory, context);
                Token[] array = (Token[]) getter.apply(object);
                output.writeInt(array.length);
                for (Token token : array)
                    output.write(token);
            });
        } else {
            super.serializer(type, getter, setter, getAnnotation, read, write);
        }
    }

    private void load(Object obj, UnrealPackage.ExportEntry entry) throws Throwable {
        try {
            ObjectInput<UnrealRuntimeContext> input = new ObjectInputStream<UnrealRuntimeContext>(
                    new ByteArrayInputStream(entry.getObjectRawDataExternally()),
                    entry.getUnrealPackage().getFile().getCharset(),
                    entry.getOffset(),
                    this,
                    new UnrealRuntimeContext(entry, this)
            ) {
                @Override
                public java.lang.Object readObject(Class clazz) throws UncheckedIOException {
                    if (getSerializerFactory() == null)
                        throw new IllegalStateException("SerializerFactory is null");

                    Serializer serializer = getSerializerFactory().forClass(clazz);
                    java.lang.Object obj = serializer.instantiate(this);
                    if (!(obj instanceof acmi.l2.clientmod.unreal.core.Object))
                        throw new RuntimeException("USE input.getSerializerFactory().forClass(class).readObject(object, input);");
                    return obj;
                }
            };

            CompletableFuture.runAsync(() -> {
                Serializer serializer = forClass(obj.getClass());
                serializer.readObject(obj, input);
            }, forkJoinPool).join();

            if (obj instanceof acmi.l2.clientmod.unreal.core.Function) {
                acmi.l2.clientmod.unreal.core.Function func = (acmi.l2.clientmod.unreal.core.Function) obj;
                if (func.nativeIndex > 0)
                    nativeFunctions.put(func.nativeIndex, func);
            }

            if (entry.getFullClassName().equalsIgnoreCase("Core.Class")) {
                Runnable loadProps = () -> {
                    obj.properties.addAll(PropertiesUtil.readProperties(input, obj.getFullName()));
                    loaded.add(entry.getObjectFullName());
                    log.fine(() -> entry.getObjectFullName() + " properties loaded");
                };
                if (entry.getObjectSuperClass() != null) {
                    String superClass = entry.getObjectSuperClass().getObjectFullName();

                    if (loaded.contains(superClass)) {
                        loadProps.run();
                    } else {
                        AtomicInteger ai = new AtomicInteger(0);
                        Consumer<InvalidationListener> c = il -> {
                            if (loaded.contains(superClass) && ai.getAndIncrement() == 0) {
                                loaded.removeListener(il);
                                loadProps.run();
                            }
                        };
                        InvalidationListener il = new InvalidationListener() {
                            @Override
                            public void invalidated(javafx.beans.Observable observable) {
                                c.accept(this);
                            }
                        };
                        loaded.addListener(il);
                        c.accept(il);
                    }
                } else {
                    loadProps.run();
                }
            }

            if (!(obj instanceof acmi.l2.clientmod.unreal.core.Class)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    while (true)
                        baos.write(input.readUnsignedByte());
                } catch (UncheckedIOException ignore) {
                }

                obj.unreadBytes = baos.toByteArray();
                if (obj.unreadBytes.length > 0)
                    log.warning(() -> obj + " " + obj.unreadBytes.length + " bytes ignored");
            }

            log.fine(() -> entry.getObjectFullName() + " loaded");
        } catch (CompletionException e) {
            throw e.getCause();
        }
    }

    public Optional<Struct> getStruct(String name) {
        try {
            return Optional.ofNullable((Struct) getOrCreateObject(name, IS_STRUCT));
        } catch (Exception e) {
            log.log(Level.WARNING, e, () -> "");
            return Optional.empty();
        }
    }

    public String getSuperClass(String clazz) {
        Optional<UnrealPackage.ExportEntry> opt = getExportEntry(clazz, IS_STRUCT);
        if (opt.isPresent())
            return opt.get().getObjectSuperClass() != null ?
                    opt.get().getObjectSuperClass().getObjectFullName() : null;
        else {
            try {
                Class<?> javaClass = Class.forName(unrealClassesPackage + "." + unrealClassNameToJavaClassName(clazz));
                Class<?> javaSuperClass = javaClass.getSuperclass();
                String javaSuperClassName = javaSuperClass.getName();
                if (javaSuperClassName.contains(unrealClassesPackage)) {
                    javaSuperClassName = javaSuperClassName.substring(unrealClassesPackage.length() + 1);
                    return javaSuperClassName.substring(0, 1).toUpperCase() + javaSuperClassName.substring(1);
                }
            } catch (ClassNotFoundException ignore) {
            }
        }
        return null;
    }

    private Map<String, Boolean> isSubclassCache = new HashMap<>();

    public boolean isSubclass(String parent, String child) {
        if (parent.equalsIgnoreCase(Objects.requireNonNull(child)))
            return true;

        String k = parent + "@" + child;
        if (!isSubclassCache.containsKey(k)) {
            child = getSuperClass(child);

            isSubclassCache.put(k, child != null && isSubclass(parent, child));
        }
        return isSubclassCache.get(k);
    }

    public <T extends Struct> List<T> getStructTree(T struct) {
        List<T> list = new ArrayList<>();

        for (UnrealPackage.ExportEntry entry = struct.entry; entry != null; entry = resolveExportEntry(entry.getObjectSuperClass()).orElse(null)) {
            list.add((T) getOrCreateObject(entry));
        }

        Collections.reverse(list);

        return list;
    }

    public Optional<acmi.l2.clientmod.unreal.core.Function> getNativeFunction(int index) {
        return Optional.ofNullable(nativeFunctions.get(index));
    }

    private static class EnvironmentWrapper extends Environment {
        private UnrealPackage engineAdds;

        public EnvironmentWrapper(File startDir, List<String> paths) {
            super(startDir, paths);
        }

        @Override
        public Stream<UnrealPackage> listPackages(String name) {
            Stream<UnrealPackage> stream = super.listPackages(name);

            if (name.equalsIgnoreCase("Engine")) {
                if (engineAdds == null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream is = getClass().getResourceAsStream("/Engine.u")) {
                        byte[] buf = new byte[1024];
                        int r;
                        while ((r = is.read(buf)) != -1)
                            baos.write(buf, 0, r);
                    } catch (IOException e) {
                        throw new UnrealException(e);
                    }

                    try (UnrealPackage up = new UnrealPackage(new RandomAccessMemory("Engine", baos.toByteArray(), UnrealPackage.getDefaultCharset()))) {
                        engineAdds = up;
                    }
                }

                stream = Stream.concat(Stream.of(engineAdds), stream);
            }

            return stream;
        }
    }
}
