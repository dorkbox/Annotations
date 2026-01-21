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

/*
 * Copyright 2014 XIAM Solutions B.V. The Netherlands (www.xiam.nl)
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

import java.lang.annotation.ElementType
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * for specifying the default report methods, without constantly creating new objects
 */
object AnnotationDefaults {
    val getTypeName: ReporterFunction<String> = object : ReporterFunction<String> {
        override fun report(cursor: Cursor): String {
            return cursor.typeName
        }
    }
    val getAnnotationType: ReporterFunction<Class<out Annotation>> = object : ReporterFunction<Class<out Annotation>> {
        override fun report(cursor: Cursor): Class<out Annotation> {
            return cursor.annotationType
        }
    }
    val getElementType: ReporterFunction<ElementType> = object : ReporterFunction<ElementType> {
        override fun report(cursor: Cursor): ElementType {
            return cursor.elementType
        }
    }
    val getMemberName: ReporterFunction<String> = object : ReporterFunction<String> {
        override fun report(cursor: Cursor): String {
            return cursor.memberName
        }
    }
    val getType: ReporterFunction<Class<*>> = object : ReporterFunction<Class<*>> {
        override fun report(cursor: Cursor): Class<*> {
            return cursor.type
        }
    }
    val getConstructor: ReporterFunction<Constructor<*>> = object : ReporterFunction<Constructor<*>> {
        override fun report(cursor: Cursor): Constructor<*> {
            return cursor.constructor
        }
    }
    val getField: ReporterFunction<Field> = object : ReporterFunction<Field> {
        override fun report(cursor: Cursor): Field {
            return cursor.field
        }
    }
    val getMethod: ReporterFunction<Method> = object : ReporterFunction<Method> {
        override fun report(cursor: Cursor): Method {
            return cursor.method
        }
    }
}
