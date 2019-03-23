package io.stardog.stardao.kotlin.partial

import io.stardog.stardao.annotations.Id
import io.stardog.stardao.annotations.Updatable
import io.stardog.stardao.kotlin.partial.annotations.PartialDataObjects
import org.junit.Test
import kotlin.test.assertEquals

class DtoGenerateProcessorTest {
    @Test
    fun testWhatever() {
        val user = ExampleUser("Foo", "Test")
        assertEquals("Foo", user.id)
    }
}

@PartialDataObjects(["Test"])
data class ExampleUser(
    @Id
    val id: String,

    @Updatable
    val name: String
)