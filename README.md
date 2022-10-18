TLV-KSP
========

[![CI](https://github.com/keiji/tlv-ksp/actions/workflows/ci.yml/badge.svg)](https://github.com/keiji/tlv-ksp/actions/workflows/ci.yml)

TLV-KSP is a Kotlin library for encoding and decoding TLV(Tag-Length-Value) data. This library is
now supported BER(Basic Encoding Rules) and Compact format.

### Setup

To add TLV-KSP to your project, include the following in your app module build.gradle file:

```
dependencies {
    implementation("dev.keiji.tlv:tlv:0.3.0")
    ksp("dev.keiji.tlv:tlv-ksp:0.3.0")
}
```

### Usage

#### BER-TLV

```kotlin
@BerTlv
class PrimitiveDatum {

    @BerTlvItem(tag = [0x01])
    var data: ByteArray? = null
}
```

KSP will generate extend functions `writeTo(OutputStream)` and `readFrom(ByteArray)`
to `PrimitiveDatum` class.

<details>
<summary>PrimitiveDatumBerTlvEncoder</summary>

```
fun PrimitiveDatum.writeTo(outputStream: OutputStream) {
    val nopConverter = dev.keiji.tlv.NopConverter()

    data?.also {
        BerTlvEncoder.writeTo(byteArrayOf(0x01.toByte()), nopConverter.convertToByteArray(it), outputStream)
    }

}
```

</details>

<details>
<summary>PrimitiveDatumBerTlvDecoder</summary>

```
fun PrimitiveDatum.readFrom(data: ByteArray) {

    BerTlvDecoder.readFrom(ByteArrayInputStream(data),
        object : BerTlvDecoder.Callback {
            override fun onLargeItemDetected(
                tag: ByteArray,
                length: BigInteger,
                inputStream: InputStream
            ) {
                throw StreamCorruptedException("tag length is too large.")
            }

            private val dev_keiji_tlv_nopConverter = dev.keiji.tlv.NopConverter()

            override fun onItemDetected(tag: ByteArray, data: ByteArray) {
                if (false) {
                    // Do nothing
                } else if (byteArrayOf(0x01.toByte()).contentEquals(tag)) {
                    this@readFrom.data = nopConverter.convertFromByteArray(data)
                } else {
                    // Do nothing
                }
            }


        }
    )
}
```

</details>

#### Compact-TLV

```kotlin
@CompactTlv
class CompactPrimitiveDatum {

    @BerTlvItem(tag = 0x01)
    var data: ByteArray? = null
}
```

KSP will generate extend functions `writeTo(OutputStream)` and `readFrom(ByteArray)`
to `CompactPrimitiveDatum` class.

<details>
<summary>PrimitiveDatumCompactTlvEncoder</summary>

```
fun PrimitiveDatum.writeTo(outputStream: OutputStream) {
    val nopConverter = dev.keiji.tlv.NopConverter()

    data?.also {
        CompactTlvEncoder.writeTo(0x01.toByte(), nopConverter.convertToByteArray(it), outputStream)
    }

}
```

</details>

<details>
<summary>PrimitiveDatumCompactTlvDecoder</summary>

```
fun PrimitiveDatum.readFrom(data: ByteArray) {

    CompactTlvDecoder.readFrom(ByteArrayInputStream(data),
        object : CompactTlvDecoder.Callback {
            private val dev_keiji_tlv_nopConverter = dev.keiji.tlv.NopConverter()

            override fun onItemDetected(tag: ByteArray, data: ByteArray) {
                if (false) {
                    // Do nothing
                } else if (0x01.toByte() == tag)) {
                    this@readFrom.data = nopConverter.convertFromByteArray(data)
                } else {
                    // Do nothing
                }
            }
        }
    )
}
```

</details>

### TypeConverter

```
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

```
    @BerTlvItem(tag = [0x0F], typeConverter = AsciiStringTypeConverter::class)
    var stringData3: String? = null,
```

OR

```
    @CompactTlvItem(tag = 0x1, typeConverter = AsciiStringTypeConverter::class)
    var stringData: String? = null,
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
