package dev.keiji.tlv

import junit.framework.Assert
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

    @Test
    fun validateAnnotation1() {
        validateAnnotation(byteArrayOf(0x6E))
    }

    @Test
    fun validateAnnotation2() {
        validateAnnotation(byteArrayOf(0x5F, 0x6E))
    }

    @Test
    fun validateAnnotation3() {
        validateAnnotation(byteArrayOf(0x5F, 0xFF.toByte(), 0x81.toByte(), 0x01))
    }

    @Test
    fun validateAnnotation_exception1() {
        try {
            validateAnnotation(byteArrayOf(0x4F, 0x01))
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception2() {
        try {
            validateAnnotation(byteArrayOf(0x5F))
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception3() {
        try {
            validateAnnotation(byteArrayOf(0x5F, 0x80.toByte()))
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception4() {
        try {
            validateAnnotation(byteArrayOf(0x5F, 0x71, 0x01))
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }
}
