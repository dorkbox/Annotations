/* ClassFileIterator.java
 *
 * Created: 2011-10-10 (Year-Month-Day)
 * Character encoding: UTF-8
 *
 ****************************************** LICENSE *******************************************
 *
 * Copyright (c) 2011 - 2013 XIAM Solutions B.V. (http://www.xiam.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.util.annotation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@code ClassFileIterator} is used to iterate over all Java ClassFile files available within
 * a specific context.
 * <p>
 * For every Java ClassFile ({@code .class}) an {@link InputStream} is returned.
 *
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @since annotation-detector 3.0.0
 */
public class ClassFileIterator implements ClassIterator {

    private FileIterator fileIter;
    protected final String[] pkgNameFilter;

    private ZipFileIterator zipIter;
    private boolean isFile;

    /**
     * Create a new {@code ClassFileIterator} returning all Java ClassFile files available
     * from the specified files and/or directories, including sub directories.
     * <p>
     * If the (optional) package filter is defined, only class files staring with one of the
     * defined package names are returned.
     * NOTE: package names must be defined in the native format (using '/' instead of '.').
     */
    protected ClassFileIterator(final String[] pkgNameFilter) {
        this.pkgNameFilter = pkgNameFilter;
    }

    /**
     * Create a new {@code ClassFileIterator} returning all Java ClassFile files available
     * from the specified files and/or directories, including sub directories.
     * <p>
     * If the (optional) package filter is defined, only class files staring with one of the
     * defined package names are returned.
     * NOTE: package names must be defined in the native format (using '/' instead of '.').
     */
    protected ClassFileIterator(final File[] filesOrDirectories, final String[] pkgNameFilter) {
        this.fileIter = new FileIterator(filesOrDirectories);
        this.pkgNameFilter = pkgNameFilter;
    }

    /**
     * Return the name of the Java ClassFile returned from the last call to {@link #next()}.
     * The name is either the path name of a file or the name of an ZIP/JAR file entry.
     */
    @Override
    public String getName() {
        // Both getPath() and getName() are very light weight method calls
        return this.zipIter == null ?
            this.fileIter.getFile().getPath() :
            this.zipIter.getEntry().getName();
    }

    /**
     * Return {@code true} if the current {@link InputStream} is reading from a plain
     * {@link File}.
     * Return {@code false} if the current {@link InputStream} is reading from a
     * ZIP File Entry.
     */
    @Override
    public boolean isFile() {
        return this.isFile;
    }

    /**
     * Return the next Java ClassFile as an {@code InputStream}.
     * <p>
     * NOTICE: Client code MUST close the returned {@code InputStream}!
     */
    @Override
    public InputStream next(final FilenameFilter filter) throws IOException {
        while (true) {
            if (this.zipIter == null) {
                final File file = this.fileIter.next();
                if (file == null) {
                    return null;
                } else {
                    final String path = file.getPath();
                    if (path.endsWith(".class") && (filter == null ||
                        filter.accept(this.fileIter.getRootFile(), this.fileIter.relativize(path)))) {
                        this.isFile = true;
                        return new FileInputStream(file);
                    } else if (this.fileIter.isRootFile() && endsWithIgnoreCase(path, ".jar")) {
                        this.zipIter = new ZipFileIterator(file, this.pkgNameFilter);
                    } // else just ignore
                }
            } else {
                final InputStream is = this.zipIter.next(filter);
                if (is == null) {
                    this.zipIter = null;
                } else {
                    this.isFile = false;
                    return is;
                }
            }
        }
    }

    // private

    private static boolean endsWithIgnoreCase(final String value, final String suffix) {
        final int n = suffix.length();
        return value.regionMatches(true, value.length() - n, suffix, 0, n);
    }
}
