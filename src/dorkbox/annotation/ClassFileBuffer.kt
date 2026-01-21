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

/* ClassFileBuffer.java
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

import java.io.*

/**
 * `ClassFileBuffer` is used by [AnnotationDetector] to efficiently read Java
 * ClassFile files from an [InputStream] and parse the content via the [DataInput]
 * interface.
 * 
 * 
 * Note that Java ClassFile files can grow really big,
 * `com.sun.corba.se.impl.logging.ORBUtilSystemException` is 128.2 kb!
 * 
 * @author [Ronald K. Muller](mailto:rmuller@xiam.nl)
 */
internal class ClassFileBuffer : DataInput {

    constructor(initialCapacity: Int = 8 * 1024) {
        require(initialCapacity >= 1) { "initialCapacity < 1: $initialCapacity" }
        this.buffer = ByteArray(initialCapacity)
    }

    private var buffer: ByteArray
    private var size = 0 // the number of significant bytes read
    private var pointer = 0 // the "read pointer"

    /**
     * Create a new, empty `ClassFileBuffer` with the specified initial capacity.
     * The initial capacity must be greater than zero. The internal buffer will grow
     * automatically when a higher capacity is required. However, buffer resizing occurs
     * extra overhead. So in good initial capacity is important in performance critical
     * situations.
     */

    /**
     * Clear and fill the buffer of this `ClassFileBuffer` with the
     * supplied byte stream.
     * The read pointer is reset to the start of the byte array.
     */
    @Throws(IOException::class)
    fun readFrom(`in`: InputStream) {
        this.pointer = 0
        this.size = 0
        var n: Int
        do {
            n = `in`.read(this.buffer, this.size, this.buffer.size - this.size)
            if (n > 0) {
                this.size += n
            }
            resizeIfNeeded()
        } while (n >= 0)
    }

    /**
     * Sets the file-pointer offset, measured from the beginning of this file,
     * at which the next read or write occurs.
     */
    @Throws(IOException::class)
    fun seek(position: Int) {
        require(position >= 0) { "position < 0: $position" }
        if (position > this.size) {
            throw EOFException()
        }
        this.pointer = position
    }

    /**
     * Return the size (in bytes) of this Java ClassFile file.
     */
    fun size(): Int {
        return this.size
    }

    // DataInput
    @Throws(IOException::class)
    override fun readFully(bytes: ByteArray) {
        readFully(bytes, 0, bytes.size)
    }

    @Throws(IOException::class)
    override fun readFully(bytes: ByteArray, offset: Int, length: Int) {
        if (length < 0 || offset < 0 || offset + length > bytes.size) {
            throw IndexOutOfBoundsException()
        }
        if (this.pointer + length > this.size) {
            throw EOFException()
        }
        System.arraycopy(this.buffer, this.pointer, bytes, offset, length)
        this.pointer += length
    }

    @Throws(IOException::class)
    override fun skipBytes(n: Int): Int {
        seek(this.pointer + n)
        return n
    }

    @Throws(IOException::class)
    override fun readByte(): Byte {
        if (this.pointer >= this.size) {
            throw EOFException()
        }
        return this.buffer[this.pointer++]
    }

    @Throws(IOException::class)
    override fun readBoolean(): Boolean {
        return readByte().toInt() != 0
    }

    @Throws(IOException::class)
    override fun readUnsignedByte(): Int {
        if (this.pointer >= this.size) {
            throw EOFException()
        }
        return read()
    }

    @Throws(IOException::class)
    override fun readUnsignedShort(): Int {
        if (this.pointer + 2 > this.size) {
            throw EOFException()
        }
        return (read() shl 8) + read()
    }

    @Throws(IOException::class)
    override fun readShort(): Short {
        return readUnsignedShort().toShort()
    }

    @Throws(IOException::class)
    override fun readChar(): Char {
        return readUnsignedShort().toChar()
    }

    @Throws(IOException::class)
    override fun readInt(): Int {
        if (this.pointer + 4 > this.size) {
            throw EOFException()
        }
        return (read() shl 24) + (read() shl 16) + (read() shl 8) + read()
    }

    @Throws(IOException::class)
    override fun readLong(): Long {
        if (this.pointer + 8 > this.size) {
            throw EOFException()
        }
        return (read().toLong() shl 56) + (read().toLong() shl 48) + (read().toLong() shl 40) + (read().toLong() shl 32) + (read() shl 24) + (read() shl 16) + (read() shl 8) + read()
    }

    @Throws(IOException::class)
    override fun readFloat(): Float {
        return java.lang.Float.intBitsToFloat(readInt())
    }

    @Throws(IOException::class)
    override fun readDouble(): Double {
        return java.lang.Double.longBitsToDouble(readLong())
    }

    /**
     * This method throws an [UnsupportedOperationException] because the method
     * is deprecated and not used in the context of this implementation.
     * 
     */
    @Deprecated("Does not support UTF-8, use readUTF() instead")
    @Throws(IOException::class)
    override fun readLine(): String? {
        throw UnsupportedOperationException("readLine() is deprecated and not supported")
    }

    @Throws(IOException::class)
    override fun readUTF(): String {
        return DataInputStream.readUTF(this)
    }

    // private
    private fun read(): Int {
        return this.buffer[this.pointer++].toInt() and 0xff
    }

    private fun resizeIfNeeded() {
        if (this.size >= this.buffer.size) {
            val newBuffer = ByteArray(this.buffer.size * 2)
            System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.size)
            this.buffer = newBuffer
        }
    }
}
