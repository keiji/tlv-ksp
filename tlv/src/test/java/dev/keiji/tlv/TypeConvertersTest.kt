package dev.keiji.tlv

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeConvertersTest {

    @Test
    fun nopConverterTest() {
        val converter = NopConverter()
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val result = converter.convertFromByteArray(data)
        assertArrayEquals(data, result)
        val result2 = converter.convertToByteArray(result)
        assertArrayEquals(data, result2)
    }

    @Test
    fun byteTypeConverterTest() {
        val converter = ByteTypeConverter()
        val data = byteArrayOf(0x01)
        val result = converter.convertFromByteArray(data)
        assertEquals(0x01.toByte(), result)
        val result2 = converter.convertToByteArray(result)
        assertArrayEquals(data, result2)
    }

    @Test
    fun booleanTypeConverterTest() {
        val converter = BooleanTypeConverter()

        // Test True
        val dataTrue = byteArrayOf(0xFF.toByte())
        val resultTrue = converter.convertFromByteArray(dataTrue)
        assertTrue(resultTrue)
        val result2True = converter.convertToByteArray(resultTrue)
        assertArrayEquals(dataTrue, result2True)

        // Test False
        val dataFalse = byteArrayOf(0x00)
        val resultFalse = converter.convertFromByteArray(dataFalse)
        assertEquals(false, resultFalse)
        val result2False = converter.convertToByteArray(resultFalse)
        assertArrayEquals(dataFalse, result2False)

        // Test non-zero as True (if that's the expected behavior, let's verify)
        // Implementation says: byteArray[0] != 0x00.toByte()
        val dataNonZero = byteArrayOf(0x01)
        val resultNonZero = converter.convertFromByteArray(dataNonZero)
        assertTrue(resultNonZero)
    }

    @Test
    fun stringTypeConverterTest() {
        val converter = StringTypeConverter()
        val str = "Hello World"
        val data = str.toByteArray(Charsets.UTF_8)
        val result = converter.convertFromByteArray(data)
        assertEquals(str, result)
        val result2 = converter.convertToByteArray(result)
        assertArrayEquals(data, result2)
    }
}
