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

import java.io.FilenameFilter
import java.io.IOException
import java.lang.annotation.ElementType

/**
 * `Builder` offers a fluent API for using [AnnotationDetector].
 * Its only role is to offer a more clean API.
 * 
 * @author [Ronald K. Muller](mailto:rmuller@xiam.nl)
 * 
 * @author dorkbox, llc
 */
interface Builder {
    /**
     * Specify the annotation types to report.
     */
    fun forAnnotations(vararg annotations: Class<out Annotation>): Builder

    /**
     * Specify the annotation types to report.
     */
    fun forAnnotations(annotation: Class<out Annotation>): Builder

    /**
     * Specify the Element Types to scan. If this method is not called,
     * [ElementType.TYPE] is used as default.
     * 
     * 
     * Valid types are:
     * 
     *  * [ElementType.TYPE]
     *  * [ElementType.METHOD]
     *  * [ElementType.FIELD]
     * 
     * An `IllegalArgumentException` is thrown if another Element Type is specified or
     * no types are specified.
     */
    fun on(type: ElementType): Builder

    /**
     * Specify the Element Types to scan. If this method is not called,
     * [ElementType.TYPE] is used as default.
     * 
     * 
     * Valid types are:
     * 
     *  * [ElementType.TYPE]
     *  * [ElementType.METHOD]
     *  * [ElementType.FIELD]
     * 
     * An `IllegalArgumentException` is thrown if another Element Type is specified or
     * no types are specified.
     */
    fun on(vararg types: ElementType): Builder

    /**
     * Filter the scanned Class Files based on its name and the directory or jar file it is
     * stored.
     * 
     * 
     * If the Class File is stored as a single file in the file system the `File`
     * argument in [FilenameFilter.accept] is the
     * absolute path to the root directory scanned.
     * 
     * 
     * If the Class File is stored in a jar file the `File` argument in
     * [FilenameFilter.accept] is the absolute path of
     * the jar file.
     * 
     * 
     * The `String` argument is the full name of the ClassFile in native format,
     * including package name, like `eu/infomas/annotation/AnnotationDetector$1.class`.
     * 
     * 
     * Note that all non-Class Files are already filtered and not seen by the filter.
     * 
     * @param filter The filter, never `null`
     */
    fun filter(filter: FilenameFilter): Builder

    /**
     * Report the detected annotations to the specified `Reporter` instance.
     * 
     * @see Reporter.report
     * @see .collect
     */
    @Throws(IOException::class)
    fun report(reporter: Reporter)

    /**
     * Report the detected annotations to the specified `ReporterFunction` instance and
     * collect the returned values of
     * [ReporterFunction.report].
     * The collected values are returned as a `List`.
     * 
     * @see .report
     */
    @Throws(IOException::class)
    fun <T> collect(reporter: ReporterFunction<T>): MutableList<T>
}
