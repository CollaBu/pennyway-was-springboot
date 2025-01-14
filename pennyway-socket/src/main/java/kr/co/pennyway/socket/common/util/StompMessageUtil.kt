package kr.co.pennyway.socket.common.util;

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.pennyway.socket.common.dto.ServerSideMessage
import org.springframework.messaging.Message
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder

/**
 * STOMP 메시지 처리를 위한 유틸리티 클래스.
 * 이 클래스는 STOMP 헤더 액세서 생성 및 메시지 생성과 관련된 공통 기능을 제공합니다.
 */
object StompMessageUtil {
    private val log = logger()
    private val EMPTY_PAYLOAD = ByteArray(0)

    /**
     * StompHeaderAccessor와 페이로드를 사용하여 STOMP 메시지를 생성합니다.
     *
     * @param accessor     StompHeaderAccessor
     * @param payload      ServerSideMessage 메시지 페이로드 (null일 수 있음)
     * @param objectMapper Jackson ObjectMapper
     * @return 생성된 STOMP 메시지
     */
    @JvmStatic
    fun createMessage(
        accessor: StompHeaderAccessor,
        payload: ServerSideMessage?,
        objectMapper: ObjectMapper
    ): Message<ByteArray> = payload?.let { nonNullPayload ->
        runCatching {
            objectMapper.writeValueAsBytes(nonNullPayload)
        }.fold(
            onSuccess = { bytes ->
                MessageBuilder.createMessage(bytes, accessor.messageHeaders)
            },
            onFailure = { e ->
                log.error("Error serializing payload", e)
                createEmptyMessage(accessor)
            }
        )
    } ?: createEmptyMessage(accessor)

    private fun createEmptyMessage(accessor: StompHeaderAccessor): Message<ByteArray> =
        MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.messageHeaders)
}
