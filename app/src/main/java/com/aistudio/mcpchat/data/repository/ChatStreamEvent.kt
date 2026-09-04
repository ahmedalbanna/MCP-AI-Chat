package com.aistudio.mcpchat.data.repository

/**
 * Live token-stream events surfaced by [ChatRepository] while a generation is
 * in flight. Session-scoped so collectors can ignore streams belonging to a
 * chat the user has already navigated away from.
 */
sealed interface ChatStreamEvent {
    val sessionId: String

    /**
     * A generation round is starting. Emitted once per send and again before
     * each tool-call continuation hop — collectors should clear any
     * previously streamed preview text when a hop's narration is superseded.
     */
    data class StreamStarted(override val sessionId: String) : ChatStreamEvent

    /** A partial text token arrived over SSE. */
    data class TokenDelta(override val sessionId: String, val delta: String) : ChatStreamEvent

    /** The generation finished and the assistant message was persisted. */
    data class StreamFinished(override val sessionId: String) : ChatStreamEvent
}
