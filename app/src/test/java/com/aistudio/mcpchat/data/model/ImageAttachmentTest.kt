package com.aistudio.mcpchat.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageAttachmentTest {

    @Test
    fun `toJsonString serializes uri and mimeType`() {
        val json = listOf(
            ImageAttachment("content://media/1", "image/png"),
            ImageAttachment("content://media/2", "image/jpeg"),
        ).toJsonString()

        val parsed = parseImageAttachments(json)
        assertEquals(2, parsed.size)
        assertEquals("content://media/1", parsed[0].uri)
        assertEquals("image/png", parsed[0].mimeType)
        assertEquals("content://media/2", parsed[1].uri)
        assertEquals("image/jpeg", parsed[1].mimeType)
    }

    @Test
    fun `toJsonString returns null for empty list`() {
        assertNull(emptyList<ImageAttachment>().toJsonString())
    }

    @Test
    fun `parseImageAttachments handles null and blank json`() {
        assertEquals(emptyList<ImageAttachment>(), parseImageAttachments(null))
        assertEquals(emptyList<ImageAttachment>(), parseImageAttachments(""))
        assertEquals(emptyList<ImageAttachment>(), parseImageAttachments("   "))
    }

    @Test
    fun `parseImageAttachments returns empty list for malformed json`() {
        assertEquals(emptyList<ImageAttachment>(), parseImageAttachments("not json at all"))
    }

    @Test
    fun `parseImageAttachments skips entries without uri and defaults mimeType`() {
        val json = """[{"uri":"content://media/3"},{"mimeType":"image/png"}]"""

        val parsed = parseImageAttachments(json)
        assertEquals(1, parsed.size)
        assertEquals("content://media/3", parsed[0].uri)
        assertEquals("image/*", parsed[0].mimeType)
    }
}
