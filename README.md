TLV-KSP
========

[![CI](https://github.com/keiji/tlv-ksp/actions/workflows/ci.yml/badge.svg)](https://github.com/keiji/tlv-ksp/actions/workflows/ci.yml)

TLV-KSP is a Kotlin library for encoding and decoding TLV(Tag-Length-Value) data.
This library is now supported BER(Basic Encoding Rules) only.

### Setup

To add TLV-KSP to your project, include the following in your app module build.gradle file:

```kotlin
dependencies {
    implementation("dev.keiji.tlv:tlv:0.3.0")
    ksp("dev.keiji.tlv:tlv-ksp:0.3.0")
}
```

### Usage

#### BerTlv
```kotlin
@BerTlv
class PrimitiveDatum {

    @BerTlvItem(tag = [0x01])
    var data: ByteArray? = null
}
```

KSP will generate extend functions `writeTo(OutputStream)` and `readFrom(ByteArray)` to `PrimitiveDatum` class.

<details>
<summary>PrimitiveDatumBerTlvEncoder</summary>

```kotlin
fun PrimitiveDatum.writeTo(outputStream: OutputStream) {
    val dev_keiji_tlv_NopConverter = dev.keiji.tlv.NopConverter()

    data?.also {
        BerTlvEncoder.writeTo(byteArrayOf(0x01.toByte()), dev_keiji_tlv_NopConverter.convertToByteArray(it), outputStream)
    }

}
```

</details>

<details>
<summary>PrimitiveDatumBerTlvDecoder</summary>

```kotlin
fun PrimitiveDatum.readFrom(
    byteArray: ByteArray,
    postCallback: BerTlvDecoder.Callback? = null,
) {
    readFrom(ByteArrayInputStream(byteArray), postCallback)
}

fun PrimitiveDatum.readFrom(
    inputStream: InputStream,
    postCallback: BerTlvDecoder.Callback? = null,
) {

    BerTlvDecoder.readFrom(inputStream,
        object : BerTlvDecoder.Callback {
            override fun onLargeItemDetected(
                tag: ByteArray,
                length: BigInteger,
                inputStream: InputStream
            ) {
                postCallback?.onLargeItemDetected(tag, length, inputStream)
            }

            override fun onUnknownLengthItemDetected(
                tag: ByteArray,
                inputStream: InputStream
            ) {
                postCallback?.onUnknownLengthItemDetected(tag, inputStream)
            }

            val dev_keiji_tlv_NopConverter = dev.keiji.tlv.NopConverter()

            override fun onItemDetected(tag: ByteArray, value: ByteArray) {
                if (false) {
                    // Do nothing
                } else if (byteArrayOf(0x01.toByte()).contentEquals(tag)) {
                    this@readFrom.data = dev_keiji_tlv_NopConverter.convertFromByteArray(value)
                } else {
                    // Do nothing
                }
                postCallback?.onItemDetected(tag, value)
            }


        }
    )
}
```
</details>

#### CompactTlv
```kotlin
@CompactTlv
open class CompactPrimitiveDatum {

    @CompactTlvItem(tag = 0x01, order = 10)
    var data1: ByteArray? = null

    @CompactTlvItem(tag = 0x02, order = 5)
    var data2: ByteArray? = null
}
```

KSP will generate extend functions `writeTo(OutputStream)` and `readFrom(ByteArray)` to `CompactPrimitiveDatum` class.

<details>
<summary>CompactPrimitiveDatumCompactTlvEncoder</summary>

```kotlin
fun CompactPrimitiveDatum.writeTo(outputStream: OutputStream) {
    val dev_keiji_tlv_NopConverter = dev.keiji.tlv.NopConverter()

    data2?.also {
        CompactTlvEncoder.writeTo(0x02.toByte(), dev_keiji_tlv_NopConverter.convertToByteArray(it), outputStream)
    }
    data1?.also {
        CompactTlvEncoder.writeTo(0x01.toByte(), dev_keiji_tlv_NopConverter.convertToByteArray(it), outputStream)
    }

}
```

</details>

<details>
<summary>CompactPrimitiveDatumCompactTlvDecoder</summary>

```kotlin
fun CompactPrimitiveDatum.readFrom(
    byteArray: ByteArray,
    postCallback: CompactTlvDecoder.Callback? = null,
) {
    readFrom(ByteArrayInputStream(byteArray), postCallback)
}

fun CompactPrimitiveDatum.readFrom(
    inputStream: InputStream,
    postCallback: CompactTlvDecoder.Callback? = null,
) {

    CompactTlvDecoder.readFrom(inputStream,
        object : CompactTlvDecoder.Callback {

            val dev_keiji_tlv_NopConverter = dev.keiji.tlv.NopConverter()

            override fun onItemDetected(tag: Byte, value: ByteArray) {
                if (false) {
                    // Do nothing
                } else if (0x01.toByte() == tag) {
                    this@readFrom.data1 = dev_keiji_tlv_NopConverter.convertFromByteArray(value)
                } else if (0x02.toByte() == tag) {
                    this@readFrom.data2 = dev_keiji_tlv_NopConverter.convertFromByteArray(value)
                } else {
                    // Do nothing
                }
                postCallback?.onItemDetected(tag, value)
            }


        }
    )
}
```
</details>

#### TypeConverter

```kotlin
private val CHARSET_ASCII = Charset.forName("ASCII")

class AsciiStringTypeConverter : AbsTypeConverter<String>() {
    override fun convertFromByteArray(byteArray: ByteArray): String {
        return String(byteArray, charset = CHARSET_ASCII)
    }

    override fun convertToByteArray(data: String): ByteArray {
        return data.toByteArray(charset = CHARSET_ASCII)
    }
}
```

```kotlin
    @BerTlvItem(tag = [0x0F], typeConverter = AsciiStringTypeConverter::class)
    var stringData3: String? = null,
```

License
========

```
Copyright 2022 ARIYAMA Keiji

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
