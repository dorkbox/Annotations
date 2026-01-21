/*
 * Copyright 2026 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
package dorkbox.annotation

import java.io.File
import java.util.*

/**
 * `FileIterator` enables iteration over all files in a directory and all its sub directories.
 * 
 * 
 * Usage:
 * <pre>
 * FileIterator iter = new FileIterator(new File("./src"));
 * File f;
 * while ((f = iter.next()) != null) {
 * // do something with f
 * assert f == iter.getCurrent();
 * }
</pre> * 
 * 
 * @author [Ronald K. Muller](mailto:rmuller@xiam.nl)
 */
internal class FileIterator(filesOrDirectories: Array<File>) {
    private val stack: Deque<File> = LinkedList<File>()

    private var rootCount: Int
    private var currentRoot: File? = null
    private var current: File? = null

    /**
     * Create a new `FileIterator` using the specified 'filesOrDirectories' as root.
     * 
     * 
     * If 'filesOrDirectories' contains a file, the iterator just returns that single file.
     * If 'filesOrDirectories' contains a directory, all files in that directory
     * and its sub directories are returned (depth first).
     * 
     * @param filesOrDirectories Zero or more [File] objects, which are iterated
     * in the specified order (depth first)
     */
    init {
        addReverse(filesOrDirectories)
        this.rootCount = this.stack.size
    }

    val file: File
        /**
         * Return the last returned file or `null` if no more files are available.
         * 
         * @see .next
         */
        get() = this.current!!


    val rootFile: File
        get() = this.currentRoot!!

    /**
     * Relativize the absolute full (file) 'path' against the current root file.
     * 
     * 
     * Example:<br></br>
     * Let current root be "/path/to/dir".
     * Then `relativize("/path/to/dir/with/file.ext")` equals "with/file.ext" (without
     * leading '/').
     * 
     * 
     * Note: the paths are not canonicalized!
     */
    fun relativize(path: String): String {
        assert(path.startsWith(this.currentRoot!!.path))
        return path.substring(this.currentRoot!!.path.length + 1)
    }

    /**
     * Return `true` if the current file is one of the files originally
     * specified as one of the constructor file parameters, i.e. is a root file
     * or directory.
     */
    fun isRootFile(): Boolean {
        if (this.current == null) {
            throw NoSuchElementException()
        }
        return this.stack.size < this.rootCount
    }

    /**
     * Return the next [File] object or `null` if no more files are
     * available.
     * 
     * @see .getFile
     */
    operator fun next(): File? {
        if (this.stack.isEmpty()) {
            this.current = null
            return null
        }
        else {
            val current = this.stack.removeLast()
            this.current = current

            if (current.isDirectory) {
                if (this.stack.size < this.rootCount) {
                    this.rootCount = this.stack.size
                    this.currentRoot = current
                }
                val files = current.listFiles()
                if (files != null) {
                    addReverse(files)
                }
                return next()
            }
            else {
                return this.current
            }
        }
    }

    /**
     * Add the specified files in reverse order.
     */
    private fun addReverse(files: Array<File>) {
        for (i in files.indices.reversed()) {
            this.stack.add(files[i])
        }
    }
}
