package webclient

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SimpleApiClient(
    @Qualifier("webClientGoRest") private val webClientGoRest: WebClient,
    @Value("\${go_rest_get_users}") private val goRestGetUsers: String
) {

    val getUsers: JsonNode?
        get() = webClientGoRest.get()
            .uri(goRestGetUsers)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) {
                throw RuntimeException(it.statusCode().toString())
            }.onStatus(HttpStatus::is5xxServerError) {
                throw RuntimeException(it.statusCode().toString())
            }.bodyToMono(JsonNode::class.java).block()
}
