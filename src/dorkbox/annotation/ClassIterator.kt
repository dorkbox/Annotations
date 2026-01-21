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
 *
 * Modified 2014, dorkbox, llc
 */
package dorkbox.annotation

import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream

/**
 * `ClassFileIterator` is used to iterate over all Java ClassFile files available within
 * a specific context.
 * 
 * 
 * For every Java ClassFile (`.class`) an [InputStream] is returned.
 * 
 * @author [Ronald K. Muller](mailto:rmuller@xiam.nl)
 */
interface ClassIterator {
    /**
     * Return the name of the Java ClassFile returned from the last call to [.next].
     * The name is either the path name of a file or the name of an ZIP/JAR file entry.
     */
    val name: String

    /**
     * Return `true` if the current [InputStream] is reading from a plain [java.io.File].
     * Return `false` if the current [InputStream] is reading from a ZIP File Entry.
     */
    val isFile: Boolean

    /**
     * Return the next Java ClassFile as an `InputStream`.
     * 
     * 
     * NOTICE: Client code MUST close the returned `InputStream`!
     */
    @Throws(IOException::class)
    fun next(filter: FilenameFilter?): InputStream?
}
