package io.stardog.stardao.auto.kotlin

import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class DataPartialProcessorTest {
    @Test
    fun generatePartialTestUser() {
        val processor = DataPartialProcessor()
//        processor.generateClass("PartialKotlinUser", "io.stardog.stardao.test")
    }
}

data class KotlinUser(val name: String)