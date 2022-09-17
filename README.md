TLV-KSP
========

TLV-KSP is a Kotlin library for encoding and decoding TLV(Tag-Length-Value) data.
This library is now supported BER(Basic Encoding Rules) only.

### Setup
TBD

### Usage

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

```
fun PrimitiveDatum.writeTo(outputStream: OutputStream) {
data?.also {
BerTlvEncoder.writeTo(byteArrayOf(0x01.toByte()), it, outputStream)
}

}
```

</details>

<details>
<summary>PrimitiveDatumBerTlvDecoder</summary>

```
fun PrimitiveDatum.readFrom(data: ByteArray) {

    BerTlvDecoder.readFrom(ByteArrayInputStream(data),
        object : BerTlvDecoder.Companion.Callback {
            override fun onLargeItemDetected(
                tag: ByteArray,
                length: BigInteger,
                inputStream: InputStream
            ) {
                throw StreamCorruptedException("tag length is too large.")
            }

            override fun onItemDetected(tag: ByteArray, data: ByteArray) {
                if (false) {
                    // Do nothing
                } else if (byteArrayOf(0x01.toByte()).contentEquals(tag)) {
                    this@readFrom.data = data
                } else {
                    // Do nothing
                }
            }


        }
    )
}
```
</details>

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
