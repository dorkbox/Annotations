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

import dorkbox.updates.Updates.add
import java.io.*
import java.lang.annotation.ElementType
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.net.JarURLConnection
import java.net.URISyntaxException
import java.net.URL
import java.util.*

/**
 * `AnnotationDetector` reads Java Class Files ("*.class") and reports the
 * found annotations via a simple, developer friendly API.
 * 
 * 
 * A Java Class File consists of a stream of 8-bit bytes. All 16-bit, 32-bit, and 64-bit
 * quantities are constructed by reading in two, four, and eight consecutive 8-bit
 * bytes, respectively. Multi byte data items are always stored in big-endian order,
 * where the high bytes come first. In the Java platforms, this format is
 * supported by interfaces [DataInput] and [java.io.DataOutput].
 * 
 * 
 * A class file consists of a single ClassFile structure:
 * <pre>
 * ClassFile {
 * u4 magic;
 * u2 minor_version;
 * u2 major_version;
 * u2 constant_pool_count;
 * cp_info constant_pool[constant_pool_count-1];
 * u2 access_flags;
 * u2 this_class;
 * u2 super_class;
 * u2 interfaces_count;
 * u2 interfaces[interfaces_count];
 * u2 fields_count;
 * field_info fields[fields_count];
 * u2 methods_count;
 * method_info methods[methods_count];
 * u2 attributes_count;
 * attribute_info attributes[attributes_count];
 * }
 * 
 * Where:
 * u1 unsigned byte [DataInput.readUnsignedByte]
 * u2 unsigned short [DataInput.readUnsignedShort]
 * u4 unsigned int [DataInput.readInt]
 * 
 * Annotations are stored as Attributes, named "RuntimeVisibleAnnotations" for
 * [java.lang.annotation.RetentionPolicy.RUNTIME] and "RuntimeInvisibleAnnotations" for
 * [java.lang.annotation.RetentionPolicy.CLASS].
</pre> * 
 * References:
 * 
 *  * [Java class file (Wikipedia)](http://en.wikipedia.org/wiki/Java_class_file)
 * (Gentle Introduction);
 *  * [Java
 * VM Specification, Java SE 8 Edition (Chapter 4)](http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html) for the real work.
 *  * [scanning java annotations at
 * runtime](http://stackoverflow.com/questions/259140).
 * 
 * 
 * 
 * Similar projects / libraries:
 * 
 *  * [JBoss MC Scanning lib](http://community.jboss.org/wiki/MCScanninglib);
 *  * [Google Reflections](http://code.google.com/p/reflections/), in fact an
 * improved version of [scannotation](http://scannotation.sourceforge.net/);
 *  * [annovention](https://github.com/ngocdaothanh/annovention), improved version
 * of the [original Annovention](http://code.google.com/p/annovention) project.
 * Available from maven: `tv.cntt:annovention:1.2`;
 *  * If using the Spring Framework, use `ClassPathScanningCandidateComponentProvider`
 * 
 * 
 * 
 * All above mentioned projects make use of a byte code manipulation library (like BCEL,
 * ASM or Javassist).
 * 
 * @author [Ronald K. Muller](mailto:rmuller@xiam.nl)
 * 
 * @author dorkbox, llc
 */
@Suppress("unused")
class AnnotationDetector : Builder, Cursor {
    private val loader: ClassLoader

    // The buffer is reused during the life cycle of this AnnotationDetector instance
    private val cpBuffer: ClassFileBuffer
    private val cfIterator: ClassIterator?

    // The Element Types to detect
    private val elementTypes: MutableSet<ElementType?>

    // Reusing the constantPool is not needed for better performance
    private var constantPool: Array<Any>? = null

    // The cached annotation types to report, maps raw Annotation type name to Class object
    private var annotations: Map<String, Class<out Annotation>>? = null
    private var filter: FilenameFilter? = null
    private var reporter: Reporter? = null

    // The current annotation reported
    private var _annotationType: Class<out Annotation>? = null

    // The 'raw' name of the current interface or class being scanned and reported
    // (using '/' instead of '.' in package name)
    private var _typeName: String? = null

    // The current method or field (if any) being scanned
    private var _memberName: String? = null

    // The current Element Type beinig reported
    private var _elementType: ElementType? = null

    // The method descriptor of the currently reported annotated method in "raw"
    // format (as it appears in the Java Class File). Example method descriptors:
    // "()V"                      no arguments, return type void
    // "(Ljava/lang/String;II)I"  String, int, int as arguments, return type int
    private var methodDescriptor: String? = null


    private constructor(loader: ClassLoader, filesOrDirectories: Array<File>?, iterator: ClassIterator?, pkgNameFilter: Array<String>?) {
        this.loader = loader
        this.cpBuffer = ClassFileBuffer()
        this.elementTypes = EnumSet.of<ElementType?>(ElementType.TYPE)

        if (iterator == null) {
            val fod = filesOrDirectories as Array<File>
            this.cfIterator = ClassFileIterator(filesOrDirectories = fod, pkgNameFilter = pkgNameFilter ?: emptyArray<String>())

            if (filesOrDirectories.isEmpty()) {
                if (DEBUG) {
                    println("No files or directories to scan!")
                }
            }
            else if (DEBUG) {
                println("Files and root directories scanned:")
                println(filesOrDirectories.contentToString().replace(", ", "\n"))
            }
        }
        else {
            this.cfIterator = iterator

            if (DEBUG) {
                println("Class Files from the custom classfileiterator scanned.")
            }
        }
    }




    private fun Class<out Annotation>.toTypeName(): String {
        return "L${this.name.replace('.', '/')};"
    }

    /**
     * See [Builder.forAnnotations].
     */
    override fun forAnnotations(annotation: Class<out Annotation>): Builder {
        // map "raw" type names to Class object
        this.annotations = mapOf(annotation.toTypeName() to annotation)
        return this
    }

    /**
     * See [Builder.forAnnotations].
     */
    @SafeVarargs
    override fun forAnnotations(vararg annotations: Class<out Annotation>): Builder {
        val newAnnotations = HashMap<String, Class<out Annotation>>(annotations.size)

        // map "raw" type names to Class object
        for (annotation in annotations) {
            newAnnotations[annotation.toTypeName()] = annotation
        }

        this.annotations = newAnnotations
        return this
    }

    /**
     * See [Builder.on].
     */
    override fun on(type: ElementType): Builder {
        this.elementTypes.clear()
        when (type) {
            ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD -> this.elementTypes.add(type)
            else                                                                             -> throw IllegalArgumentException("Unsupported: $type")
        }

        return this
    }

    /**
     * See [Builder.on].
     */
    override fun on(vararg types: ElementType): Builder {
        require(types.isNotEmpty()) { "At least one Element Type must be specified" }

        this.elementTypes.clear()
        for (t in types) {
            when (t) {
                ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD -> this.elementTypes.add(t)
                else                                                                             -> throw IllegalArgumentException("Unsupported: $t")
            }
        }
        return this
    }

    /**
     * See [Builder.filter].
     */
    override fun filter(filter: FilenameFilter): Builder {
        this.filter = filter
        return this
    }

    /**
     * See [Builder.report].
     */
    @Throws(IOException::class)
    override fun report(reporter: Reporter) {
        this.reporter = reporter
        detect(this.cfIterator!!)
    }

    /**
     * See [Builder.collect].
     */
    @Throws(IOException::class)
    override fun <T> collect(reporter: ReporterFunction<T>): MutableList<T> {
        val list: MutableList<T> = mutableListOf()

        this.reporter = object : Reporter {
            override fun report(cursor: Cursor) {
                list.add(reporter.report(cursor))
            }
        }

        detect(this.cfIterator!!)
        return list
    }

    /**
     * See [Cursor.typeName].
     */
    override val typeName: String
        get() = _typeName!!.replace('/', '.')

    /**
     * See [Cursor.annotationType].
     */
    override val annotationType: Class<out Annotation>
        get() = _annotationType!!


    /**
     * See [Cursor.elementType].
     */
    override val elementType: ElementType
        get() = _elementType!!

    /**
     * See [Cursor.memberName].
     */
    override val memberName: String
        get() = _memberName!!

    /**
     * See [Cursor.type].
     */
    override val type: Class<*>
        get() = loadClass(this.loader, typeName)

    /**
     * See [Cursor.field].
     */
    override val field: Field
        get() {
            check(this.elementType == ElementType.FIELD) { "Illegal to call getField() when ${this.elementType} is reported" }

        try {
            return type.getDeclaredField(this.memberName)
        }
        catch (ex: NoSuchFieldException) {
            throw assertionError("Cannot find Field '%s' for type %s", this.memberName, typeName)
        }
    }

    /**
     * See [Cursor.constructor].
     */
    override val constructor: Constructor<*>
        get() {
            check(this.elementType == ElementType.CONSTRUCTOR) { "Illegal to call getMethod() when ${this.elementType} is reported" }

            try {
                val parameterTypes = parseArguments(this.methodDescriptor!!)
                return type.getConstructor(*parameterTypes)
            }
            catch (ex: NoSuchMethodException) {
                throw assertionError("Cannot find Constructor '%s(...)' for type %s", this.memberName, typeName)
            }
        }

    /**
     * See [Cursor.method].
     */
    override val method: Method
        get() {
            check(this.elementType == ElementType.METHOD) { "Illegal to call getMethod() when ${this.elementType} is reported" }
        try {
            val parameterTypes = parseArguments(this.methodDescriptor!!)
            return type.getDeclaredMethod(this.memberName, *parameterTypes)
        }
        catch (ex: NoSuchMethodException) {
            throw assertionError("Cannot find Method '%s(...)' for type %s", this.memberName, typeName)
        }
    }

    /**
     * See [Cursor.getAnnotation].
     */
    override fun <T : Annotation> getAnnotation(annotationClass: Class<T>): T {
        check(annotationClass == this.annotationType) { "Illegal to call getAnnotation() when ${this.annotationType.name} is reported" }
        val ae = when (this.elementType) {
            ElementType.TYPE   -> type
            ElementType.FIELD  -> field
            ElementType.METHOD -> method
            else               -> throw AssertionError(this.elementType)
        }
        return ae.getAnnotation<T>(annotationClass)
    }

    @Throws(IOException::class)
    private fun detect(iterator: ClassIterator) {
        var stream: InputStream?
        val mustEndInClass = iterator is ClassFileIterator
        while ((iterator.next(this.filter).also { stream = it }) != null) {
            try {
                this.cpBuffer.readFrom(stream!!)
                val name = iterator.name

                // SOME files can actually have CAFEBABE (binary files), but are NOT CLASSFILES! Explicitly define this!
                if (this.cpBuffer.hasCafebabe()) {
                    if (mustEndInClass && !name.endsWith(".class")) {
                        continue
                    }
                    if (DEBUG) {
                        println("Class File: $name")
                    }

                    read(this.cpBuffer)
                } // else ignore
            }
            finally {
                // closing InputStream from ZIP Entry is handled by ZipFileIterator
                if (iterator.isFile) {
                    stream!!.close()
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun ClassFileBuffer.hasCafebabe(): Boolean {
        return size() > 4 && readInt() == -0x35014542
    }

    /**
     * Inspect the given (Java) class file in streaming mode.
     */
    @Throws(IOException::class)
    private fun read(di: DataInput) {
        readVersion(di)
        readConstantPoolEntries(di)
        readAccessFlags(di)
        readThisClass(di)
        readSuperClass(di)
        readInterfaces(di)
        readFields(di)
        readMethods(di)
        readAttributes(di, ElementType.TYPE)
    }

    @Throws(IOException::class)
    private fun readVersion(di: DataInput) {
        // sequence: minor version, major version (argument_index is 1-based)

        if (DEBUG) {
            val minor = di.readUnsignedShort()
            val maj = di.readUnsignedShort()

            println("Java Class version $maj.$minor")
        }
        else {
            di.skipBytes(4)
        }
    }

    @Throws(IOException::class)
    private fun readConstantPoolEntries(di: DataInput) {
        val count = di.readUnsignedShort()
        @Suppress("UNCHECKED_CAST")
        this.constantPool = arrayOfNulls<Any>(count) as Array<Any>

        var i = 1
        while (i < count) {
            if (readConstantPoolEntry(di, i)) {
                // double slot
                ++i
            }
            ++i
        }
    }

    /**
     * Return `true` if a double slot is read (in case of Double or Long constant).
     */
    @Throws(IOException::class)
    private fun readConstantPoolEntry(di: DataInput, index: Int): Boolean {
        when (val tag = di.readUnsignedByte()) {
            CP_METHOD_TYPE, CP_MODULE_ID, CP_MODULE_PACKAGE_ID     -> {
                di.skipBytes(2) // readUnsignedShort()
                return false
            }

            CP_METHOD_HANDLE                                       -> {
                di.skipBytes(3)
                return false
            }

            CP_INTEGER, CP_FLOAT, CP_REF_FIELD,
            CP_REF_METHOD, CP_REF_INTERFACE,
            CP_NAME_AND_TYPE, CP_DYNAMIC, CP_INVOKE_DYNAMIC       -> {
                di.skipBytes(4) // readInt() / readFloat() / readUnsignedShort() * 2
                return false
            }

            CP_LONG, CP_DOUBLE                                    -> {
                di.skipBytes(8) // readLong() / readDouble()
                return true
            }

            CP_UTF8                                               -> {
                this.constantPool!![index] = di.readUTF()
                return false
            }

            CP_CLASS, CP_STRING                                   -> {
                // reference to CP_UTF8 entry. The referenced index can have a higher number!
                this.constantPool!![index] = di.readUnsignedShort()
                return false
            }

            else                                                  -> throw ClassFormatError("Unknown tag value for constant pool entry: $tag")
        }
    }

    @Throws(IOException::class)
    private fun readAccessFlags(di: DataInput) {
        di.skipBytes(2) // u2
    }

    @Throws(IOException::class)
    private fun readThisClass(di: DataInput) {
        this._typeName = resolveUtf8(di)
    }

    @Throws(IOException::class)
    private fun readSuperClass(di: DataInput) {
        di.skipBytes(2) // u2
    }

    @Throws(IOException::class)
    private fun readInterfaces(di: DataInput) {
        val count = di.readUnsignedShort()
        di.skipBytes(count * 2) // count * u2
    }

    @Throws(IOException::class)
    private fun readFields(di: DataInput) {
        val count = di.readUnsignedShort()
        for (i in 0..<count) {
            readAccessFlags(di)
            this._memberName = resolveUtf8(di)

            // descriptor is Field type in raw format, we do not need it, so skip
            //final String descriptor = resolveUtf8(di);
            di.skipBytes(2)

            if (DEBUG) {
                println("Field: ${this.memberName}")
            }
            readAttributes(di, ElementType.FIELD)
        }
    }

    @Throws(IOException::class)
    private fun readMethods(di: DataInput) {
        val count = di.readUnsignedShort()
        for (i in 0..<count) {
            readAccessFlags(di)

            this._memberName = resolveUtf8(di)
            this.methodDescriptor = resolveUtf8(di)

            if (DEBUG) {
                println("Method: ${this.memberName}")
            }

            readAttributes(di, if ("<init>" == this.memberName) ElementType.CONSTRUCTOR else ElementType.METHOD)
        }
    }

    @Throws(IOException::class)
    private fun readAttributes(di: DataInput, reporterType: ElementType) {
        val count = di.readUnsignedShort()
        for (i in 0..<count) {
            val name = resolveUtf8(di)
            // in bytes, use this to skip the attribute info block
            val length = di.readInt()
            if (this.elementTypes.contains(reporterType) && ("RuntimeVisibleAnnotations" == name || "RuntimeInvisibleAnnotations" == name)) {
                if (DEBUG) {
                    println("Attribute: $name")
                }
                readAnnotations(di, reporterType)
            }
            else {
                if (DEBUG) {
                    println("Attribute: $name (ignored)")
                }
                di.skipBytes(length)
            }
        }
    }

    @Throws(IOException::class)
    private fun readAnnotations(di: DataInput, elementType: ElementType) {
        // the number of Runtime(In)VisibleAnnotations

        val count = di.readUnsignedShort()
        for (i in 0..<count) {
            val rawTypeName = readAnnotation(di)
            _annotationType = this.annotations!![rawTypeName]

            if (_annotationType == null) {
                if (DEBUG) {
                    println("Annotation: $rawTypeName (ignored)")
                }
                continue
            }

            if (DEBUG) {
                println("Annotation: '${_annotationType!!.name}' on type '$typeName', member '$memberName' (reported)")
            }

            this._elementType = elementType
            this.reporter!!.report(this)
        }
    }

    @Throws(IOException::class)
    private fun readAnnotation(di: DataInput): String {
        val rawTypeName = resolveUtf8(di)
        // num_element_value_pairs
        val count = di.readUnsignedShort()
        for (i in 0..<count) {
            if (DEBUG) {
                println("Annotation Element: ${resolveUtf8(di)}")
            }
            else {
                di.skipBytes(2)
            }
            readAnnotationElementValue(di)
        }
        return rawTypeName
    }

    @Throws(IOException::class)
    private fun readAnnotationElementValue(di: DataInput) {
        val tag = di.readUnsignedByte()
        when (tag) {
            BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT, BOOLEAN, STRING, CLASS -> di.skipBytes(2)
            ENUM                                                                -> di.skipBytes(4) // 2 * u2
            ANNOTATION                                                          -> readAnnotation(di)
            ARRAY                                                               -> {
                val count = di.readUnsignedShort()
                var i = 0
                while (i < count) {
                    readAnnotationElementValue(di)
                    ++i
                }
            }

            else                                                                -> {
                throw ClassFormatError("Not a valid annotation element type tag: 0x${Integer.toHexString(tag)}")
            }
        }
    }

    /**
     * Look up the String value, identified by the u2 index value from constant pool
     * (direct or indirect).
     */
    @Throws(IOException::class)
    private fun resolveUtf8(di: DataInput): String {
        val index = di.readUnsignedShort()
        val value = this.constantPool!![index]
        val s = if (value is Int) {
            this.constantPool!![value] as String
        }
        else {
            value as String
        }
        return s
    }

    /**
     * Return the method arguments of the currently reported annotated method as a
     * `Class` array.
     */
    // incorrect detection of dereferencing possible null pointer
    // TODO: https://github.com/checkstyle/checkstyle/issues/14 fixed in 5.8?
    private fun parseArguments(descriptor: String): Array<Class<*>?> {
        val n = descriptor.length
        // "minimal" descriptor: no arguments: "()V", the first character is always '('
        if (n < 3 || descriptor[0] != '(') {
            throw unparseable(descriptor, "Wrong format")
        }
        var args: MutableList<Class<*>?>? = null
        var i = 1
        while (i < n) {
            var c = descriptor[i]
            if (i == 1) {
                if (c == ')') {
                    return arrayOfNulls(0)
                }
                else {
                    args = LinkedList()
                }
            }

            var j: Int
            when (c) {
                'V'  -> args!!.add(Void.TYPE)
                'Z'  -> args!!.add(Boolean::class.javaPrimitiveType)
                'C'  -> args!!.add(Char::class.javaPrimitiveType)
                'B'  -> args!!.add(Byte::class.javaPrimitiveType)
                'S'  -> args!!.add(Short::class.javaPrimitiveType)
                'I'  -> args!!.add(Int::class.javaPrimitiveType)
                'F'  -> args!!.add(Float::class.javaPrimitiveType)
                'J'  -> args!!.add(Long::class.javaPrimitiveType)
                'D'  -> args!!.add(Double::class.javaPrimitiveType)
                '['  -> {
                    j = i
                    do {
                        c = descriptor[++j]
                    } while (c == '[') // multi dimensional array
                    if (c == 'L') {
                        j = descriptor.indexOf(';', i + 1)
                    }
                    args!!.add(loadClass(this.loader, descriptor.substring(i, j + 1)))
                    i = j
                }

                'L'  -> {
                    j = descriptor.indexOf(';', i + 1)
                    args!!.add(loadClass(this.loader, descriptor.substring(i + 1, j)))
                    i = j
                }

                ')'  ->                     // end of argument type list, stop parsing
                    return args!!.toTypedArray<Class<*>?>()

                else -> throw unparseable(descriptor, "Not a recognized type: $c")
            }
            ++i
        }
        throw unparseable(descriptor, "No closing parenthesis")
    }

    companion object {
        private const val DEBUG = false

        // https://en.wikipedia.org/wiki/Java_class_file
        // Constant Pool type ta gs
        private const val CP_UTF8 = 1
        private const val CP_INTEGER = 3
        private const val CP_FLOAT = 4
        private const val CP_LONG = 5
        private const val CP_DOUBLE = 6
        private const val CP_CLASS = 7
        private const val CP_STRING = 8
        private const val CP_REF_FIELD = 9
        private const val CP_REF_METHOD = 10
        private const val CP_REF_INTERFACE = 11
        private const val CP_NAME_AND_TYPE = 12
        private const val CP_METHOD_HANDLE = 15 // Java VM SE 7
        private const val CP_METHOD_TYPE = 16 // Java VM SE 7
        private const val CP_DYNAMIC = 17 // Java VM SE 11
        private const val CP_INVOKE_DYNAMIC = 18 // Java VM SE 7
        private const val CP_MODULE_ID = 19 // Java VM SE 9
        private const val CP_MODULE_PACKAGE_ID = 20 // Java VM SE 9

        // AnnotationElementValue / Java raw types
        private val BYTE = 'B'.code
        private val CHAR = 'C'.code
        private val DOUBLE = 'D'.code
        private val FLOAT = 'F'.code
        private val INT = 'I'.code
        private val LONG = 'J'.code
        private val SHORT = 'S'.code
        private val BOOLEAN = 'Z'.code
        private val ARRAY = '['.code

        // used for AnnotationElement only
        private val STRING = 's'.code
        private val ENUM = 'e'.code
        private val CLASS = 'c'.code
        private val ANNOTATION = '@'.code

        init {
            // Add this project to the updates system, which verifies this class + UUID + version information
            add(
                AnnotationDetector::class.java, "27e311d499c94b1aa0a879710429964d", version
            )
        }

        /**
         * Gets the version number.
         */
        const val version = "3.3"

        /**
         * Factory method, starting point for the fluent interface.
         * Only scan Class Files in the specified packages. If nothing is specified, all classes
         * on the class path are scanned.
         */
        @Throws(IOException::class)
        fun scanClassPath(vararg packageNames: String): Builder {
            val loader = Thread.currentThread().contextClassLoader
            return scanClassPath(loader, *packageNames)
        }

        /**
         * Factory method, starting point for the fluent interface.
         * Only scan Class Files in the specified packages. If nothing is specified, all classes
         * on the class path are scanned.
         */
        @Throws(IOException::class)
        fun scanClassPath(loader: ClassLoader, vararg packageNames: String): Builder {
            val pkgNameFilter: Array<String>?

            // DORKBOX added
            val isCustomLoader = "dorkbox.classloader.ClassLoader" == loader.javaClass.name

            val sysProperties = System.getProperty("java.class.path")
                .split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            if (isCustomLoader) {
                val fileNames: MutableList<URL>

                // scanning the classpath
                if (packageNames.isEmpty()) {
                    pkgNameFilter = null

                    fileNames = sysProperties.map {
                        File(it).toURI().toURL()
                    }.toMutableList()
                }
                else {
                    pkgNameFilter = packageNames.map { name ->
                        name.replace('.', '/').let {
                            if (!it.endsWith("/")) {
                                "$it/"
                            } else {
                                it
                            }
                        }
                    }.toTypedArray()


                    fileNames = pkgNameFilter.flatMap { packageName ->
                        loader.getResources(packageName).toList()
                    }.toMutableList()
                }



                return AnnotationDetector(
                    loader = loader,
                    filesOrDirectories = null,
                    iterator = CustomClassloaderIterator(fileNames = fileNames, packageNames = packageNames.toList().toTypedArray()),
                    pkgNameFilter = pkgNameFilter!!)
            }
            else {
                val files: MutableSet<File>

                if (packageNames.isEmpty()) {
                    pkgNameFilter = null
                    files = sysProperties.map {
                        File(it)
                    }.toMutableSet()
                }
                else {
                    pkgNameFilter = packageNames.map { name ->
                        name.replace('.', '/').let {
                            if (!it.endsWith("/")) {
                                "$it/"
                            } else {
                                it
                            }
                        }
                    }.toTypedArray()

                    files = mutableSetOf()

                    for (packageName in pkgNameFilter) {
                        addFiles(loader, packageName, files)
                    }
                }
                return AnnotationDetector(
                    loader = loader,
                    filesOrDirectories = files.toTypedArray(),
                    iterator = null,
                    pkgNameFilter = pkgNameFilter!!)
            }
        }

        /**
         * Factory method, starting point for the fluent interface.
         * Scan all files specified by the classFileIterator.
         */
        fun scan(loader: ClassLoader, iterator: ClassIterator?): Builder {
            return AnnotationDetector(loader, null, iterator, null)
        }

        /**
         * Factory method, starting point for the fluent interface.
         * Scan all files in the specified jar files and directories.
         */
        fun scanFiles(loader: ClassLoader, vararg filesOrDirectories: File): Builder {
            return AnnotationDetector(loader, filesOrDirectories.toList().toTypedArray(), null, null)
        }

        /**
         * Factory method, starting point for the fluent interface.
         * Scan all files in the specified jar files and directories.
         */
        fun scanFiles(vararg filesOrDirectories: File): Builder {
            return AnnotationDetector(Thread.currentThread().contextClassLoader, filesOrDirectories.toList().toTypedArray(), null, null)
        }

        // private
        @Throws(IOException::class)
        private fun addFiles(loader: ClassLoader, resourceName: String, files: MutableSet<File>) {
            val resourceEnum = loader.getResources(resourceName)
            while (resourceEnum.hasMoreElements()) {
                val url = resourceEnum.nextElement()
                if (DEBUG) {
                    println("Resource URL: $url")
                }

                // Handle JBoss VFS URL's which look like (example package 'nl.dvelop'):
                // vfs:/foo/bar/website.war/WEB-INF/classes/nl/dvelop/
                // vfs:/foo/bar/website.war/WEB-INF/lib/dwebcore-0.0.1.jar/nl/dvelop/
                val protocol = url.protocol
                val isVfs = "vfs" == protocol
                if ("file" == protocol || isVfs) {
                    val dir: File = toFile(url)
                    if (dir.isDirectory) {
                        files.add(dir)
                    }
                    else if (isVfs) {
                        //Jar file via JBoss VFS protocol - strip package name
                        var jarPath = dir.path
                        val idx = jarPath.indexOf(".jar")
                        if (idx > -1) {
                            jarPath = jarPath.substring(0, idx + 4)
                            val jarFile = File(jarPath)
                            if (jarFile.isFile) {
                                files.add(jarFile)
                            }
                        }
                    }
                    else {
                        throw assertionError("Not a recognized file URL: %s", url)
                    }
                }
                else {
                    // Resource in Jar File
                    val jarFile: File = toFile((url.openConnection() as JarURLConnection).jarFileURL)
                    if (jarFile.isFile) {
                        files.add(jarFile)
                    }
                    else {
                        throw assertionError("Not a File: %s", jarFile)
                    }
                }
            }
        }

        @Throws(IOException::class)
        private fun toFile(url: URL): File {
            // the only correct way to convert the URL to a File object, also see issue #16
            // Do not use URLDecoder
            try {
                return File(url.toURI())
            }
            catch (ex: URISyntaxException) {
                throw IOException(ex.message)
            }
        }

        /**
         * Load the class, but do not initialize it.
         */
        private fun loadClass(loader: ClassLoader, rawClassName: String): Class<*> {
            val typeName = rawClassName.replace('/', '.')
            try {
                return Class.forName(typeName, false, loader)
            }
            catch (ex: ClassNotFoundException) {
                throw assertionError("Cannot load type '%s', scanned file not on class path? (%s)", typeName, ex)
            }
        }

        /**
         * The method descriptor must always be parseable, so if not, then an AssertionError is thrown.
         */
        private fun unparseable(descriptor: String?, cause: String?): AssertionError {
            return assertionError("Unparseable method descriptor: '%s' (cause: %s)", descriptor, cause)
        }

        private fun assertionError(message: String, vararg args: Any?): AssertionError {
            return AssertionError(String.format(message, *args))
        }
    }
}
