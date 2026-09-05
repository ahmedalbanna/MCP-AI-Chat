package com.aistudio.mcpchat.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * An image the user attached to a chat message. Only the content URI is
 * persisted (never the bytes) — providers receive fresh base64 data read
 * from the URI at request time.
 */
data class ImageAttachment(
    val uri: String,
    val mimeType: String,
)

fun List<ImageAttachment>.toJsonString(): String? {
    if (isEmpty()) return null
    val array = JSONArray()
    for (attachment in this) {
        array.put(
            JSONObject()
                .put("uri", attachment.uri)
                .put("mimeType", attachment.mimeType),
        )
    }
    return array.toString()
}

fun parseImageAttachments(json: String?): List<ImageAttachment> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            val uri = obj.optString("uri")
            if (uri.isBlank()) {
                null
            } else {
                ImageAttachment(uri = uri, mimeType = obj.optString("mimeType", "image/*"))
            }
        }
    }.getOrDefault(emptyList())
}
