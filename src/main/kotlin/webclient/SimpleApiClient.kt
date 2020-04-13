package webclient

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SimpleApiClient(
    @Qualifier("defaultWebClientJetty") private val defaultWebClientJetty: WebClient
) {
    val getUsers: JsonNode?
        get() = defaultWebClientJetty.get()
            .uri("")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) {
                throw RuntimeException(it.statusCode().toString())
            }.onStatus(HttpStatus::is5xxServerError) {
                throw RuntimeException(it.statusCode().toString())
            }.bodyToMono(JsonNode::class.java).block()
}
