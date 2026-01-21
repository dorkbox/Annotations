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

import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.zip.*

/**
 * `ZipFileIterator` is used to iterate over all entries in a given `zip` or
 * `jar` file and returning the [InputStream] of these entries.
 * 
 * 
 * It is possible to specify an (optional) entry name filter.
 * 
 * 
 * The most efficient way of iterating is used, see benchmark in test classes.
 * @author [Ronald K. Muller](mailto:rmuller@xiam.nl)
 * @author dorkbox, llc
 *
 *
 * @param file         The ZIP file used to iterate over all entries
 * @param entryNameFilter (optional) file name filter. Only entry names starting with one of the specified names in the filter are returned
 */
internal class ZipFileIterator(private val file: File, private val entryNameFilter: Array<String>?) {
    private val zipFile: ZipFile = ZipFile(file)
    private val entries: Enumeration<out ZipEntry>

    private var current: ZipEntry? = null


    init {
        this.entries = this.zipFile.entries()
    }

    val entry: ZipEntry
        get() = this.current!!

    @Throws(IOException::class)
    fun next(filter: FilenameFilter?): InputStream? {
        while (this.entries.hasMoreElements()) {
            val current = this.entries.nextElement()
            this.current = current

            if (filter == null || accept(current, filter)) {
                return this.zipFile.getInputStream(current)
            }
        }
        // no more entries in this ZipFile, so close ZipFile
        try {
            // zipFile is never null here
            this.zipFile.close()
        }
        catch (_: IOException) {
            // suppress IOException, otherwise close() is called twice
        }

        return null
    }

    private fun accept(entry: ZipEntry, filter: FilenameFilter): Boolean {
        if (entry.isDirectory) {
            return false
        }
        val name = entry.getName()
        if (name.endsWith(".class") && filter.accept(this.file, name)) {
            if (this.entryNameFilter == null) {
                return true
            }
            for (entryName in this.entryNameFilter) {
                if (name.startsWith(entryName)) {
                    return true
                }
            }
        }
        return false
    }
}
