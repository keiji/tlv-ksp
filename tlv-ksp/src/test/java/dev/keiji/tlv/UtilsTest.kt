package dev.keiji.tlv

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class UtilsTest {

    private val byteArrayComparator = Comparator<ByteArray> { obj1, obj2 -> compare(obj1, obj2) }

    @Test
    fun byteArrayComparatorTest1() {
        val byteArray1 = byteArrayOf()
        val byteArray2 = byteArrayOf()
        val data = arrayOf(byteArray1, byteArray2)

        data.sortWith(byteArrayComparator)

        assertArrayEquals(byteArray1, data[0])
        assertArrayEquals(byteArray2, data[1])
    }

    @Test
    fun byteArrayComparatorTest2() {
        val byteArray1 = byteArrayOf(0x01)
        val byteArray2 = byteArrayOf()
        val data = arrayOf(byteArray1, byteArray2)

        data.sortWith(byteArrayComparator)

        assertArrayEquals(byteArray2, data[0])
        assertArrayEquals(byteArray1, data[1])
    }

    @Test
    fun byteArrayComparatorTest3() {
        val byteArray1 = byteArrayOf(0x01)
        val byteArray2 = byteArrayOf(0x02)
        val data = arrayOf(byteArray1, byteArray2)

        data.sortWith(byteArrayComparator)

        assertArrayEquals(byteArray1, data[0])
        assertArrayEquals(byteArray2, data[1])
    }

    @Test
    fun byteArrayComparatorTest4() {
        val byteArray1 = byteArrayOf(0x01)
        val byteArray2 = byteArrayOf(0x00)
        val data = arrayOf(byteArray1, byteArray2)

        data.sortWith(byteArrayComparator)

        assertArrayEquals(byteArray2, data[0])
        assertArrayEquals(byteArray1, data[1])
    }

    @Test
    fun byteArrayComparatorTest5() {
        val byteArray1 = byteArrayOf(0xFF.toByte())
        val byteArray2 = byteArrayOf(0x00)
        val data = arrayOf(byteArray1, byteArray2)

        data.sortWith(byteArrayComparator)

        assertArrayEquals(byteArray2, data[0])
        assertArrayEquals(byteArray1, data[1])
    }

    @Test
    fun byteArrayComparatorTest6() {
        val byteArray1 = byteArrayOf(0x00, 0x02)
        val byteArray2 = byteArrayOf(0x00, 0x01)
        val data = arrayOf(byteArray1, byteArray2)

        data.sortWith(byteArrayComparator)

        assertArrayEquals(byteArray2, data[0])
        assertArrayEquals(byteArray1, data[1])
    }

    @Test
    fun byteArrayComparatorTest7() {
        val byteArray1 = byteArrayOf(0x00, 0x02)
        val byteArray2 = byteArrayOf(0x00)
        val data = arrayOf(byteArray1, byteArray2)

        data.sortWith(byteArrayComparator)

        assertArrayEquals(byteArray2, data[0])
        assertArrayEquals(byteArray1, data[1])
    }
}
