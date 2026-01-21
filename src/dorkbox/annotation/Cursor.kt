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

/* Cursor.java
 *
 * Created: 2014-06-15 (Year-Month-Day)
 * Character encoding: UTF-8
 *
 ****************************************** LICENSE *******************************************
 *
 * Copyright (c) 2014 XIAM Solutions B.V. (http://www.xiam.nl)
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

import java.lang.annotation.ElementType
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * `Cursor` offers a "cursor interface" for working with [AnnotationDetector].
 * 
 * @author [Ronald K. Muller](mailto:rmuller@xiam.nl)
 */
interface Cursor {
    /**
     * Return the type name of the currently reported Java Class File.
     */
    val typeName: String

    /**
     * Return the Annotation Type currently reported.
     */
    val annotationType: Class<out Annotation>

    /**
     * Return the `ElementType` of the currently reported `Annotation`.
     */
    val elementType: ElementType

    /**
     * Return the member name of the currently reported `Annotation`.
     * In case of an annotation on type level, "&lt;clinit&gt;" is reported.
     */
    val memberName: String

    /**
     * Return the [type][Class] of the currently reported Java Class File.
     */
    val type: Class<*>

    /**
     * Return the [Constructor] instance of the currently reported annotated Constructor.
     */
    val constructor: Constructor<*>

    /**
     * Return the [Field] instance of the currently reported annotated Field.
     */
    val field: Field

    /**
     * Return the [Method] instance of the currently reported annotated Method.
     */
    val method: Method

    /**
     * Return the `Annotation` of the reported Annotated Element.
     */
    fun <T : Annotation> getAnnotation(annotationClass: Class<T>): T
}
