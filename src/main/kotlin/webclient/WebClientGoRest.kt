package webclient

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class WebClientGoRestConfiguration(
    private val webClientDefaultConfiguration: WebClientDefaultConfiguration,
    @Value("\${go_rest_users_host}") private val goRestUsersHost: String
) {
        private val logger: Logger = LoggerFactory.getLogger(WebClientGoRestConfiguration::class.java)

    @Bean
    fun webClientGoRest(): WebClient =
        webClientDefaultConfiguration
            .defaultWebClientJettyBuilder()
            .baseUrl(goRestUsersHost)
            .defaultHeaders { header -> header.setBearerAuth("UYyeRzKAu1mLCDai8tdUDyu7wS6CdSC0Um3i") }
            .build()
}
