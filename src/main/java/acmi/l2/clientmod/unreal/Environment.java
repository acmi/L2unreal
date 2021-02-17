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

import acmi.l2.clientmod.crypt.L2Crypt;
import acmi.l2.clientmod.crypt.rsa.L2Ver41x;
import acmi.l2.clientmod.crypt.rsa.L2Ver41xInputStream;
import acmi.l2.clientmod.io.BufferedRandomAccessFile;
import acmi.l2.clientmod.io.RandomAccess;
import acmi.l2.clientmod.io.RandomAccessFile;
import acmi.l2.clientmod.io.UnrealPackage;
import lombok.Getter;

import lombok.NonNull;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Environment implements Env {
    private static final Logger log = Logger.getLogger(Environment.class.getName());

    private static final Set<String> BUFFERED_PACKAGES = new HashSet<>(Arrays.asList(System.getProperty("L2unreal.bufferedExt", "").split(",")));

    private static final Pattern PATHS_PATTERN = Pattern.compile("\\s*Paths=(.*)");

    @Getter
    private final File startDir;
    @Getter
    private final List<String> paths;

    private final Map<String, List<File>> fileCache = new HashMap<>();
    private final Map<File, UnrealPackage> pckgCache = new HashMap<>();
    private final Map<UnrealPackage, Map<String, UnrealPackage.ExportEntry[]>> entriesCache = new HashMap<>();
    private final Map<UnrealPackage, Map<String, UnrealPackage.ExportEntry[]>> entriesCache2 = new HashMap<>();

    public Environment(@NonNull File startDir, @NonNull List<String> paths) {
        this.startDir = startDir;
        this.paths = paths;
    }

    public static Environment fromIni(File ini) throws UncheckedIOException {
        try (InputStream bis = new BufferedInputStream(new FileInputStream(ini))) {
            InputStream is = bis;
            bis.mark(28);
            if (L2Crypt.readHeader(bis) == 413) {
                BigInteger modulus = L2Ver41x.MODULUS_413;
                BigInteger exponent = L2Ver41x.PRIVATE_EXPONENT_413;

                bis.mark(128);
                try {
                    //noinspection ResultOfMethodCallIgnored
                    new L2Ver41xInputStream(bis, modulus, exponent).read();
                } catch (Exception e) {
                    modulus = L2Ver41x.MODULUS_L2ENCDEC;
                    exponent = L2Ver41x.PRIVATE_EXPONENT_L2ENCDEC;
                }

                bis.reset();
                is = new L2Ver41xInputStream(bis, modulus, exponent);
            } else {
                bis.reset();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            List<String> paths = br.lines()
                    .filter(s -> PATHS_PATTERN.matcher(s).matches())
                    .map(s -> s.substring(s.indexOf('=') + 1).trim())
                    .collect(Collectors.toList());
            if (paths.isEmpty()) {
                log.warning(() -> "Couldn't find any Path in file");
            }
            File startDir = ini.getParentFile();

            return new Environment(startDir, paths);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Stream<File> getPackage(String name) {
        if (!fileCache.containsKey(name)) {
            fileCache.put(name, listFiles()
                    .filter(file -> FilenameUtils.removeExtension(file.getName()).equalsIgnoreCase(name))
                    .collect(Collectors.toList()));
        }

        return fileCache.get(name).stream();
    }

    @Override
    public Optional<UnrealPackage> getPackage(File f) {
        if (!pckgCache.containsKey(f)) {
            log.fine("Loading " + f.getPath());

            try (RandomAccess ra = createRandomAccess(f)) {
                UnrealPackage up = new UnrealPackage(ra);
                pckgCache.put(f, up);

                BinaryOperator<UnrealPackage.ExportEntry[]> bo = (exportEntries, exportEntries2) -> {
                    UnrealPackage.ExportEntry[] res = new UnrealPackage.ExportEntry[exportEntries.length + exportEntries2.length];
                    System.arraycopy(exportEntries, 0, res, 0, exportEntries.length);
                    System.arraycopy(exportEntries2, 0, res, exportEntries.length, exportEntries2.length);
                    return res;
                };
                entriesCache.put(up, up.getExportTable().stream().collect(Collectors.toMap(e -> e.getObjectFullName().toLowerCase(), e -> new UnrealPackage.ExportEntry[]{e}, bo)));
                entriesCache2.put(up, up.getExportTable().stream().collect(Collectors.toMap(e -> e.getObjectName().getName().toLowerCase(), e -> new UnrealPackage.ExportEntry[]{e}, bo)));
            } catch (Exception e) {
                log.log(Level.WARNING, e, () -> String.format("Couldn't load %s", f.getPath()));
            }
        }

        return Optional.ofNullable(pckgCache.get(f));
    }

    @Override
    public Optional<UnrealPackage.ExportEntry> getExportEntry(@NonNull String fullName, @NonNull Predicate<String> fullClassName) throws UncheckedIOException {
        String[] path = fullName.split("\\.");
        Optional<UnrealPackage.ExportEntry> entryOptional = listPackages(path[0])
                .map(entriesCache::get)
                .map(map -> map.getOrDefault(fullName.toLowerCase(), new UnrealPackage.ExportEntry[0]))
                .flatMap(Arrays::stream)
                .filter(e -> fullClassName.test(e.getFullClassName()))
                .findAny();
        if (!entryOptional.isPresent()) {
            entryOptional = listPackages(path[0])
                    .map(entriesCache2::get)
                    .map(map -> map.getOrDefault(path[path.length - 1].toLowerCase(), new UnrealPackage.ExportEntry[0]))
                    .flatMap(Arrays::stream)
                    .filter(e -> fullClassName.test(e.getFullClassName()))
                    .findAny();
        }
        return entryOptional;
    }

    @Override
    public void markInvalid(String pckg) {
        getPackage(pckg).forEach(file -> {
            UnrealPackage toRemove = pckgCache.remove(file);
            entriesCache.remove(toRemove);
            entriesCache2.remove(toRemove);

            log.fine("Remove from cache " + file.getPath());
        });
    }

    protected RandomAccess createRandomAccess(File f) {
        if (BUFFERED_PACKAGES.contains(f.getName().substring(f.getName().lastIndexOf('.') + 1))) {
            log.fine("Using buffered random access for " + f.getPath());

            return new BufferedRandomAccessFile(f, true, UnrealPackage.getDefaultCharset());
        }

        return new RandomAccessFile(f, true, UnrealPackage.getDefaultCharset());
    }
}
