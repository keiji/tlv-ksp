TLV-KSP
========

[![CI](https://github.com/keiji/tlv-ksp/actions/workflows/ci.yml/badge.svg)](https://github.com/keiji/tlv-ksp/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/dev.keiji.tlv/tlv-ksp.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/dev.keiji.tlv/tlv-ksp)

TLV-KSP is a Kotlin library for encoding and decoding TLV(Tag-Length-Value) data. This library is
now supported BER(Basic Encoding Rules) and Compact format.

### Setup

To add TLV-KSP to your project, include the following in your app module build.gradle file:

```
dependencies {
    implementation("dev.keiji.tlv:tlv:0.4.1")
    ksp("dev.keiji.tlv:tlv-ksp:0.4.1")
}
```

### Verify Coverage

To verify code coverage, run the following command:

```bash
./gradlew tlv:jacocoTestReport
```

The coverage report will be generated at `tlv/build/reports/jacoco/test/html/index.html`.

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

### How to publish

To publish the package to Maven Central, you need to configure your GPG key and Central Portal credentials.

**1. Configure GPG Signing**

This project is configured to use the GPG command-line tool (`useGpgCmd()`), which allows you to specify the signing key by its User ID (e.g., email address). You have two options to configure this, with project-specific settings taking precedence over global ones.

**Option 1: Project-specific setting (Recommended)**

For project-specific configuration, which avoids interfering with other projects, create a `local.properties` file in the project's root directory. This file is automatically ignored by Git in Android projects, so your credentials won't be committed.

Add the following line to `local.properties`:

```properties
signing.gnupg.keyName=YOUR_KEY_ID_OR_EMAIL
```

**Option 2: Global setting**

Alternatively, you can configure the key globally for all your projects by editing your user-level `gradle.properties` file, which is usually located at `~/.gradle/gradle.properties`.

Add the following line to that file:

```properties
signing.gnupg.keyName=YOUR_KEY_ID_OR_EMAIL
```

---

For both options, replace `YOUR_KEY_ID_OR_EMAIL` with the email address associated with your GPG key, or its hexadecimal ID.

To be prompted for your passphrase in the terminal, ensure that the `GPG_PASSPHRASE` environment variable is **not** set.

**2. Configure Central Portal Credentials**

Set the following environment variables with your Sonatype Central Portal credentials:

*   `CENTRAL_PORTAL_USERNAME`: Your username or token.
*   `CENTRAL_PORTAL_PASSWORD`: Your password or token.

**3. Publish the Package**

Once the configuration is complete, run the following command:

```bash
./gradlew publishAggregationToCentralPortal
```

This command will build the project, sign the artifacts using your specified GPG key, and upload them to the Sonatype Central Portal. You will be prompted to enter your GPG key passphrase in the terminal during the signing process.

License
========

```
Copyright 2022-2026 ARIYAMA Keiji

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
