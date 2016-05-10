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

import acmi.l2.clientmod.crypt.CryptoException;
import acmi.l2.clientmod.crypt.L2Crypt;
import acmi.l2.clientmod.io.UnrealPackage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Environment {
    private static final Logger log = Logger.getLogger(Environment.class.getName());

    private static final Pattern PATHS_PATTERN = Pattern.compile("\\s*Paths=(.*)");

    private File startDir;
    private List<String> paths;

    private Map<File, UnrealPackage> pckgCache = new HashMap<>();

    public Environment(File startDir, List<String> paths) {
        this.startDir = startDir;
        this.paths = paths;
    }

    public static Environment fromIni(File ini) throws UncheckedIOException {
        try (InputStream fis = new FileInputStream(ini)) {
            InputStream is = fis;
            try {
                is = L2Crypt.decrypt(is, ini.getName());
            } catch (CryptoException e) {
                log.fine(e.getMessage());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            List<String> paths = br.lines()
                    .filter(s -> PATHS_PATTERN.matcher(s).matches())
                    .map(s -> s.substring(s.indexOf('=') + 1).trim())
                    .collect(Collectors.toList());
            if (paths.isEmpty())
                log.warning(() -> "Couldn't find any Path in file");
            File startDir = ini.getParentFile();

            return new Environment(startDir, paths);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public File getStartDir() {
        return startDir;
    }

    public List<String> getPaths() {
        return paths;
    }

    public Stream<File> listFiles() {
        return paths.stream().flatMap(s -> {
            File file = new File(startDir, s);
            File parent = file.getParentFile();
            if (!parent.exists()) return Stream.empty();
            return FileUtils.listFiles(file.getParentFile(), new WildcardFileFilter(file.getName()), null).stream();
        });
    }

    public Stream<File> getPackage(String name) {
        return listFiles()
                .filter(file -> FilenameUtils.removeExtension(file.getName()).equalsIgnoreCase(name));
    }

    public Stream<UnrealPackage> listPackages() {
        return listFiles()
                .map(this::getPackage)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Optional<UnrealPackage.ExportEntry> getExportEntry(String fullName, Predicate<String> fullClassName) throws UncheckedIOException {
        String pckgName = fullName.substring(0, fullName.indexOf('.'));
        return listPackages()
                .filter(p -> p.getPackageName().equalsIgnoreCase(pckgName))
                .map(UnrealPackage::getExportTable)
                .flatMap(Collection::stream)
                .filter(e -> e.getObjectFullName().equalsIgnoreCase(fullName))
                .filter(e -> fullClassName.test(e.getFullClassName()))
                .findAny();
    }

    private Optional<UnrealPackage> getPackage(File f) {
        if (!pckgCache.containsKey(f)) {
            try (UnrealPackage up = new UnrealPackage(f, true)) {
                pckgCache.put(f, up);
            } catch (Exception e) {
                log.log(Level.WARNING, e, () -> String.format("Couldn't load %s", f));
            }
        }

        return Optional.ofNullable(pckgCache.get(f));
    }
}
