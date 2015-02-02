/* FileIterator.java
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
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * {@code FileIterator} enables iteration over all files in a directory and all its sub
 * directories.
 * <p>
 * Usage:
 * <pre>
 * FileIterator iter = new FileIterator(new File("./src"));
 * File f;
 * while ((f = iter.next()) != null) {
 *     // do something with f
 *     assert f == iter.getCurrent();
 * }
 * </pre>
 *
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @since annotation-detector 3.0.0
 */
final class FileIterator {

    private final Deque<File> stack = new LinkedList<File>();
    private int rootCount;
    private File currentRoot;
    private File current;

    /**
     * Create a new {@code FileIterator} using the specified 'filesOrDirectories' as root.
     * <p>
     * If 'filesOrDirectories' contains a file, the iterator just returns that single file.
     * If 'filesOrDirectories' contains a directory, all files in that directory
     * and its sub directories are returned (depth first).
     *
     * @param filesOrDirectories Zero or more {@link File} objects, which are iterated
     * in the specified order (depth first)
     */
    FileIterator(final File... filesOrDirectories) {
        addReverse(filesOrDirectories);
        this.rootCount = this.stack.size();
    }

    /**
     * Return the last returned file or {@code null} if no more files are available.
     *
     * @see #next()
     */
    File getFile() {
        return this.current;
    }

    File getRootFile() {
        return this.currentRoot;
    }

    /**
     * Relativize the absolute full (file) 'path' against the current root file.
     * <p>
     * Example:<br/>
     * Let current root be "/path/to/dir".
     * Then {@code relativize("/path/to/dir/with/file.ext")} equals "with/file.ext" (without
     * leading '/').
     * <p>
     * Note: the paths are not canonicalized!
     */
    String relativize(final String path) {
        assert path.startsWith(this.currentRoot.getPath());
        return path.substring(this.currentRoot.getPath().length() + 1);
    }

    /**
     * Return {@code true} if the current file is one of the files originally
     * specified as one of the constructor file parameters, i.e. is a root file
     * or directory.
     */
    boolean isRootFile() {
        if (this.current == null) {
            throw new NoSuchElementException();
        }
        return this.stack.size() < this.rootCount;
    }

    /**
     * Return the next {@link File} object or {@code null} if no more files are
     * available.
     *
     * @see #getFile()
     */
    File next() {
        if (this.stack.isEmpty()) {
            this.current = null;
            return null;
        } else {
            this.current = this.stack.removeLast();
            if (this.current.isDirectory()) {
                if (this.stack.size() < this.rootCount) {
                    this.rootCount = this.stack.size();
                    this.currentRoot = this.current;
                }
                addReverse(this.current.listFiles());
                return next();
            } else {
                return this.current;
            }
        }
    }

    // private

    /**
     * Add the specified files in reverse order.
     */
    private void addReverse(final File[] files) {
        for (int i = files.length - 1; i >= 0; --i) {
            this.stack.add(files[i]);
        }
    }

}
