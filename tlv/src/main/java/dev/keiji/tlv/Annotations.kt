/*
 * Copyright (C) 2022 ARIYAMA Keiji
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

package dev.keiji.tlv

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class BerTlv

@Target(AnnotationTarget.FIELD)
annotation class BerTlvItem(
    val tag: ByteArray,
    val typeConverter: KClass<out AbsTypeConverter<*>> = NopConverter::class,
    val order: Int = 0
)

@Target(AnnotationTarget.CLASS)
annotation class CompactTlv

@Target(AnnotationTarget.FIELD)
annotation class CompactTlvItem(
    val tag: Byte,
    val typeConverter: KClass<out AbsTypeConverter<*>> = NopConverter::class,
    val order: Int = 0
)
