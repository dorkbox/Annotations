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
import java.net.URISyntaxException
import java.net.URL

/**
 * @author dorkbox, llc
 */
class CustomClassloaderIterator(fileNames: MutableList<URL>, packageNames: Array<String>) : ClassIterator {
    private val loaderFilesIterator: MutableIterator<URL>
    private var classFileIterator: ClassFileIterator? = null

    // have to support
    // 1 - scanning the classpath
    // 2 - scanning a specific package
    init {
        // if ANY of our filenames DO NOT start with "box", we have to add it as a file, so our iterator picks it up (and if dir, it's
        // children)

        val files: MutableSet<File> = mutableSetOf()
        val iterator = fileNames.iterator()
        while (iterator.hasNext()) {
            val url = iterator.next()
            if (url.protocol != "box") {
                try {
                    val file = File(url.toURI())
                    files.add(file)
                    iterator.remove()
                }
                catch (ex: URISyntaxException) {
                    throw IOException(ex.message)
                }
            }
        }

        if (files.isEmpty()) {
            this.classFileIterator = null
        }
        else {
            this.classFileIterator = ClassFileIterator(filesOrDirectories = files.toTypedArray(), pkgNameFilter = packageNames)
        }

        this.loaderFilesIterator = fileNames.iterator()
    }


    override val name = "CustomClassloaderIterator"

    override val isFile: Boolean
        get() {
            if (this.classFileIterator != null) {
                return this.classFileIterator!!.isFile
            }

            return false
        }

    @Throws(IOException::class)
    override fun next(filter: FilenameFilter?): InputStream? {
        if (this.classFileIterator != null) {
            while (true) {
                val next = this.classFileIterator!!.next(filter)
                if (next == null) {
                    this.classFileIterator = null
                }
                else {
                    val name = this.classFileIterator!!.name
                    if (name.endsWith(".class")) {
                        return next
                    }
                }
            }
        }

        if (this.loaderFilesIterator.hasNext()) {
            val next = this.loaderFilesIterator.next()
            return next.openStream()
        }

        return null
    }
}
