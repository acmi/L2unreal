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
import acmi.l2.clientmod.unreal.util.InvalidationListener;
import acmi.l2.clientmod.unreal.util.ObservableSet;
import acmi.l2.clientmod.unreal.util.ObservableSetWrapper;
import lombok.Getter;
import lombok.NonNull;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class UnrealSerializerFactory extends ReflectionSerializerFactory<UnrealRuntimeContext> {
    private static final Logger log = Logger.getLogger(UnrealSerializerFactory.class.getName());

    private static final String LOAD_THREAD_NAME = "Unreal loader";
    private static final int LOAD_THREAD_STACK_SIZE = loadThreadStackSize();

    private static int loadThreadStackSize() {
        try {
            return Integer.parseInt(System.getProperty("L2unreal.loadThreadStackSize", "8000000"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static final String unrealClassesPackage = "acmi.l2.clientmod.unreal";

    public static final Predicate<String> IS_STRUCT = c -> c.equalsIgnoreCase("Core.Struct") ||
            c.equalsIgnoreCase("Core.Function") ||
            c.equalsIgnoreCase("Core.State") ||
            c.equalsIgnoreCase("Core.Class");

    private final Map<String, Object> objects = new HashMap<>();
    private final Map<Integer, acmi.l2.clientmod.unreal.core.Function> nativeFunctions = new HashMap<>();
    private final ObservableSet<String> loaded = new ObservableSetWrapper<>(new HashSet<>());

    @Getter
    private final Env environment;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> new Thread(null, r, LOAD_THREAD_NAME, LOAD_THREAD_STACK_SIZE) {{
        setDaemon(true);
    }});

    public UnrealSerializerFactory(@NonNull Env environment) {
        this.environment = new EnvironmentWrapper(environment);
    }

    @Override
    protected Function<ObjectInput<UnrealRuntimeContext>, java.lang.Object> createInstantiator(Class<?> clazz) {
        if (Object.class.isAssignableFrom(clazz)) {
            return objectInput -> {
                UnrealRuntimeContext context = objectInput.getContext();
                int objRef = objectInput.readCompactInt();
                UnrealPackage.Entry entry = context.getUnrealPackage().objectReference(objRef);
                return getOrCreateObject(entry);
            };
        }
        return super.createInstantiator(clazz);
    }

    public Object getOrCreateObject(UnrealPackage.Entry packageLocalEntry) throws UncheckedIOException {
        if (packageLocalEntry == null) {
            return null;
        }

        String key = keyFor(packageLocalEntry);
        if (!objects.containsKey(key)) {
            log.finest(() -> String.format("Loading %s", packageLocalEntry));

            UnrealPackage.ExportEntry entry;
            try {
                entry = resolveExportEntry(packageLocalEntry).orElse(null);
                if (entry != null) {
                    Class<? extends Object> clazz = getClass(entry.getFullClassName());
                    Object obj = clazz.newInstance();
                    objects.put(key, obj);
                    Runnable load = () -> load(obj, entry);
                    if (LOAD_THREAD_NAME.equals(Thread.currentThread().getName())) {
                        load.run();
                    } else {
                        executorService.submit(load).get();
                    }
                } else {
                    create(packageLocalEntry.getObjectFullName(), packageLocalEntry.getFullClassName());
                }
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        return objects.get(key);
    }

    private String keyFor(UnrealPackage.Entry entry) {
        return (entry.getObjectFullName() + "_" + entry.getFullClassName()).toLowerCase();
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
        objects.put(keyFor(entry), new acmi.l2.clientmod.unreal.core.Class() {
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

    /**
     * @throws UnrealException if not found
     */
    public Object getOrCreateObject(@NonNull String objName, @NonNull Predicate<String> objClass) throws UncheckedIOException, UnrealException {
        return getOrCreateObject(getExportEntry(objName, objClass)
                .orElseThrow(() -> new UnrealException("Entry " + objName + " not found")));
    }

    public Optional<UnrealPackage.ExportEntry> resolveExportEntry(UnrealPackage.Entry entry) {
        if (entry == null) {
            return Optional.empty();
        }

        return getExportEntry(entry.getObjectFullName(), clazz -> clazz.equalsIgnoreCase(entry.getFullClassName()));
    }

    private Optional<UnrealPackage.ExportEntry> getExportEntry(String objName, Predicate<String> objClass) {
        return environment.getExportEntry(objName, objClass);
    }

    private Class<? extends Object> getClass(String className) throws UncheckedIOException {
        Class<?> clazz = null;
        try {
            String javaClassName = unrealClassesPackage + "." + unrealClassNameToJavaClassName(className);
            clazz = Class.forName(javaClassName);
            log.finest(() -> String.format("unreal[%s]->jvm[%s]", className, javaClassName));
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
                dataOutput.writeCompactInt(obj == null || obj.entry == null ? 0 : dataOutput.getContext().getUnrealPackage().objectReferenceByName(obj.entry.getObjectFullName(), c -> c.equalsIgnoreCase(obj.entry.getFullClassName())));
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
                setter.accept(object, () -> tokens.toArray(new Token[0]));
            });
            write.add((object, dataOutput) -> {
                BytecodeContext context = new BytecodeContext(dataOutput.getContext());
                SerializerFactory<BytecodeContext> serializerFactory = new TokenSerializerFactory();
                ObjectOutput<BytecodeContext> output = ObjectOutput.objectOutput(dataOutput, serializerFactory, context);
                Token[] array = (Token[]) getter.apply(object);
                output.writeInt(array.length);
                for (Token token: array) {
                    output.write(token);
                }
            });
        } else {
            super.serializer(type, getter, setter, getAnnotation, read, write);
        }
    }

    private void load(Object obj, UnrealPackage.ExportEntry entry) {
        ObjectInput<UnrealRuntimeContext> input = new ObjectInputStream<UnrealRuntimeContext>(
                new ByteArrayInputStream(entry.getObjectRawDataExternally()),
                entry.getUnrealPackage().getFile().getCharset(),
                entry.getOffset(),
                this,
                new UnrealRuntimeContext(entry, this)
        ) {
            @Override
            public java.lang.Object readObject(Class clazz) throws UncheckedIOException {
                if (getSerializerFactory() == null) {
                    throw new IllegalStateException("SerializerFactory is null");
                }

                Serializer serializer = getSerializerFactory().forClass(clazz);
                java.lang.Object obj = serializer.instantiate(this);
                if (!(obj instanceof acmi.l2.clientmod.unreal.core.Object)) {
                    throw new RuntimeException("USE input.getSerializerFactory().forClass(class).readObject(object, input);");
                }
                return obj;
            }
        };

        Serializer serializer = forClass(obj.getClass());
        serializer.readObject(obj, input);

        if (obj instanceof acmi.l2.clientmod.unreal.core.Function) {
            acmi.l2.clientmod.unreal.core.Function func = (acmi.l2.clientmod.unreal.core.Function) obj;
            if (func.nativeIndex > 0) {
                nativeFunctions.put(func.nativeIndex, func);
            }
        }

        if (entry.getFullClassName().equalsIgnoreCase("Core.Class")) {
            Runnable loadProps = () -> {
                obj.properties.addAll(PropertiesUtil.readProperties(input, obj.getFullName()));
                loaded.add(entry.getObjectFullName());
                log.finest(() -> entry.getObjectFullName() + " properties loaded");
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
                        public void invalidated(acmi.l2.clientmod.unreal.util.Observable observable) {
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
                //noinspection InfiniteLoopStatement
                while (true) {
                    baos.write(input.readUnsignedByte());
                }
            } catch (UncheckedIOException ignore) {
            }

            obj.unreadBytes = baos.toByteArray();
            if (obj.unreadBytes.length > 0) {
                log.warning(() -> obj + " " + obj.unreadBytes.length + " bytes ignored");
            }
        }

        log.finest(() -> entry.getObjectFullName() + " loaded");
    }

    public Optional<Struct> getStruct(String name) {
        try {
            return Optional.of((Struct) getOrCreateObject(name, IS_STRUCT));
        } catch (Exception e) {
            log.log(Level.WARNING, e, () -> "");
            return Optional.empty();
        }
    }

    /**
     * @return superClass or null
     */
    public String getSuperClass(@NonNull String clazz) {
        Optional<UnrealPackage.ExportEntry> opt = getExportEntry(clazz, IS_STRUCT);
        if (opt.isPresent()) {
            return opt.get().getObjectSuperClass() != null ?
                    opt.get().getObjectSuperClass().getObjectFullName() : null;
        } else {
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

    private final Map<String, Boolean> isSubclassCache = new HashMap<>();

    public boolean isSubclass(@NonNull String parent, @NonNull String child) {
        if (parent.equalsIgnoreCase(child)) {
            return true;
        }

        String k = parent + "@" + child;
        synchronized (isSubclassCache) {
            if (!isSubclassCache.containsKey(k)) {
                child = getSuperClass(child);
                isSubclassCache.put(k, child != null && isSubclass(parent, child));
            }
            return isSubclassCache.get(k);
        }
    }

    public <T extends Struct> List<T> getStructTree(@NonNull T struct) {
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

    private static class EnvironmentWrapper implements Env {
        private final Env environment;
        private final Map<String, UnrealPackage> adds = new HashMap<>();

        public EnvironmentWrapper(@NonNull Env environment) {
            this.environment = environment;
        }

        @Override
        public File getStartDir() {
            return environment.getStartDir();
        }

        @Override
        public List<String> getPaths() {
            return environment.getPaths();
        }

        @Override
        public Optional<UnrealPackage> getPackage(File f) {
            return environment.getPackage(f);
        }

        @Override
        public void markInvalid(String pckg) {
            environment.markInvalid(pckg);
        }

        @Override
        public Stream<UnrealPackage> listPackages(String name) {
            return appendCustomPackage(environment.listPackages(name), name);
        }

        @Override
        public Stream<File> listFiles() {
            return environment.listFiles();
        }

        @Override
        public Stream<File> getPackage(String name) {
            return environment.getPackage(name);
        }

        @Override
        public Optional<UnrealPackage.ExportEntry> getExportEntry(@NonNull String fullName, @NonNull Predicate<String> fullClassName) throws UncheckedIOException {
            String[] path = fullName.split("\\.");

            Optional<UnrealPackage.ExportEntry> entryOptional = appendCustomPackage(Stream.empty(), path[0])
                    .map(UnrealPackage::getExportTable)
                    .flatMap(Collection::stream)
                    .filter(e -> e.getObjectFullName().equalsIgnoreCase(fullName))
                    .filter(e -> fullClassName.test(e.getFullClassName()))
                    .findAny();
            if (entryOptional.isPresent()) {
                return entryOptional;
            }

            return environment.getExportEntry(fullName, fullClassName);
        }

        private Stream<UnrealPackage> appendCustomPackage(Stream<UnrealPackage> stream, String name) {
            if (!name.equalsIgnoreCase("Engine") && !name.equalsIgnoreCase("Core")) {
                return stream;
            }

            name = canonizeName(name);
            if (!adds.containsKey(name)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (InputStream is = getClass().getResourceAsStream("/" + name + ".u")) {
                    byte[] buf = new byte[1024];
                    int r;
                    while ((r = is.read(buf)) != -1) {
                        baos.write(buf, 0, r);
                    }
                } catch (IOException e) {
                    throw new UnrealException(e);
                }

                try (UnrealPackage up = new UnrealPackage(new RandomAccessMemory(name, baos.toByteArray(), UnrealPackage.getDefaultCharset()))) {
                    adds.put(name, up);
                }
            }

            return Stream.concat(Stream.of(adds.get(name)), stream);
        }

        private String canonizeName(String name) {
            return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
    }
}
