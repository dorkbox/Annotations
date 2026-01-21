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
package dorkbox.annotation

import java.io.*

/**
 * `ClassFileIterator` is used to iterate over all Java ClassFile files available within
 * a specific context.
 * 
 * 
 * For every Java ClassFile (`.class`) an [InputStream] is returned.
 * 
 * @author [Ronald K. Muller](mailto:rmuller@xiam.nl)
 * 
 * @author dorkbox, llc
 */
class ClassFileIterator : ClassIterator {
    private val fileIter: FileIterator
    private val pkgNameFilter: Array<String>

    private var zipIter: ZipFileIterator? = null
    private var _isFile: Boolean = false

    override val isFile: Boolean
        get() = _isFile

    /**
     * Create a new `ClassFileIterator` returning all Java ClassFile files available
     * from the specified files and/or directories, including sub directories.
     * 
     * 
     * If the (optional) package filter is defined, only class files staring with one of the
     * defined package names are returned.
     * NOTE: package names must be defined in the native format (using '/' instead of '.').
     */
    constructor(filesOrDirectories: Array<File>, pkgNameFilter: Array<String>) {
        this.fileIter = FileIterator(filesOrDirectories)
        this.pkgNameFilter = pkgNameFilter
    }

    /**
     * Return the name of the Java ClassFile returned from the last call to [.next].
     * The name is either the path name of a file or the name of an ZIP/JAR file entry.
     */
    override val name: String
        get() {
            // Both getPath() and getName() are very light weight method calls
            return if (this.zipIter == null) this.fileIter.file.path else this.zipIter!!.entry.getName()
        }

    /**
     * Return the next Java ClassFile as an `InputStream`.
     * 
     * 
     * NOTICE: Client code MUST close the returned `InputStream`!
     */
    @Throws(IOException::class)
    override fun next(filter: FilenameFilter?): InputStream? {
        while (true) {
            if (this.zipIter == null) {
                val file = this.fileIter.next()
                if (file == null) {
                    return null
                }
                else {
                    val path = file.path
                    if (path.endsWith(".class") && (filter == null || filter.accept(
                            this.fileIter.rootFile, this.fileIter.relativize(
                                path
                            )
                        ))
                    ) {
                        this._isFile = true
                        return FileInputStream(file)
                    }
                    else if (this.fileIter.isRootFile() && endsWithIgnoreCase(path, ".jar")) {
                        this.zipIter = ZipFileIterator(file, this.pkgNameFilter)
                    } // else just ignore
                }
            }
            else {
                val `is` = this.zipIter!!.next(filter)
                if (`is` == null) {
                    this.zipIter = null
                }
                else {
                    this._isFile = false
                    return `is`
                }
            }
        }
    }

    companion object {
        private fun endsWithIgnoreCase(value: String, @Suppress("SameParameterValue") suffix: String): Boolean {
            val n = suffix.length
            return value.regionMatches(value.length - n, suffix, 0, n, ignoreCase = true)
        }
    }
}
