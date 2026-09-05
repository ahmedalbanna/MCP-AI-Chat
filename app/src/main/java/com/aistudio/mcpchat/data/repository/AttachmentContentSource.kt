package com.aistudio.mcpchat.data.repository

import com.aistudio.mcpchat.data.model.ImageAttachment

/**
 * Base64 image payload ready for provider wire formats. The repository never
 * touches [android.content.ContentResolver] directly — encoding happens in
 * the Android-backed implementation so JVM tests can substitute a fake.
 */
data class LoadedAttachment(
    val mimeType: String,
    val base64Data: String,
)

fun interface AttachmentContentSource {
    /** Returns the encoded image, or null when the URI is no longer readable. */
    fun read(attachment: ImageAttachment): LoadedAttachment?
}
