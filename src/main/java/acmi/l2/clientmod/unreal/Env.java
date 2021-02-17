/*
 * Copyright 2016 acmi
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package acmi.l2.clientmod.unreal;

import acmi.l2.clientmod.io.UnrealPackage;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Env {
    File getStartDir();

    List<String> getPaths();

    default Stream<File> listFiles() {
        return getPaths().stream().flatMap(s -> {
            File file = new File(getStartDir(), s);
            File parent = file.getParentFile();
            if (!parent.exists()) {
                return Stream.empty();
            }
            return FileUtils.listFiles(file.getParentFile(), new WildcardFileFilter(file.getName()), null).stream();
        });
    }

    default Stream<File> getPackage(String name) {
        return listFiles()
                .filter(file -> FilenameUtils.removeExtension(file.getName()).equalsIgnoreCase(name));
    }

    default Optional<UnrealPackage> getPackage(File f) {
        try (UnrealPackage up = new UnrealPackage(f, true)) {
            return Optional.of(up);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    default Stream<UnrealPackage> listPackages(String name) {
        return getPackage(name)
                .map(this::getPackage)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    default Optional<UnrealPackage.ExportEntry> getExportEntry(@NonNull String fullName, @NonNull Predicate<String> fullClassName) throws UncheckedIOException {
        String[] path = fullName.split("\\.");
        Optional<UnrealPackage.ExportEntry> entryOptional = listPackages(path[0])
                .map(UnrealPackage::getExportTable)
                .flatMap(Collection::parallelStream)
                .filter(e -> e.getObjectFullName().equalsIgnoreCase(fullName))
                .filter(e -> fullClassName.test(e.getFullClassName()))
                .findAny();
        if (!entryOptional.isPresent()) {
            entryOptional = listPackages(path[0])
                    .map(UnrealPackage::getExportTable)
                    .flatMap(Collection::parallelStream)
                    .filter(e -> e.getObjectName().getName().equalsIgnoreCase(path[path.length - 1]))
                    .filter(e -> fullClassName.test(e.getFullClassName()))
                    .findAny();
        }
        return entryOptional;
    }

    void markInvalid(String pckg);
}
