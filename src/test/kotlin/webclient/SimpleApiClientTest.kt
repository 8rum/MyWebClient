package webclient

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SimpleApiClientTest {
    @Autowired
    private val webTestClient: WebTestClient? = null

    private val webClientDefaultConfiguration = WebClientDefaultConfiguration()
    private var webClientGoRestConfiguration: WebClientGoRestConfiguration? = null

    @Value("\${go_rest_users_host}") private val goRestUsersHost: String = ""
    @Value("\${go_rest_get_users}") private val goRestGetUsers: String = ""

//    @InjectMocks
    private var simpleApiClient: SimpleApiClient? = null

    @BeforeEach
    fun init() {
        MockitoAnnotations.initMocks(this)
        val webClientGoRestConfiguration = WebClientGoRestConfiguration(webClientDefaultConfiguration, goRestUsersHost)
        simpleApiClient = SimpleApiClient(webClientGoRestConfiguration.webClientGoRest(), goRestGetUsers)
    }

    @Test
    fun testGetTodosAPICall() {
        val responseSpec = webTestClient?.let {
            it.get()
                .uri("https://jsonplaceholder.typicode.com/todos/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
        } ?: throw NoSuchElementException()

        responseSpec.expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .expectBody()
            .jsonPath("$.title").isNotEmpty
            .jsonPath("$.userId").isNotEmpty
            .jsonPath("$.completed").isNotEmpty
    }

    @Test
    fun webClientGetTest() {
        simpleApiClient?.getUsers
//        println("#### RESPONSE: ${simpleApiClient?.getUsers}")
    }
}