package webclient

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.api.Response
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.JettyClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import webclient.SimpleWebClientConfigurationJetty.RequestLogEnhancer.enhance
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class SimpleWebClientConfigurationJetty {

    companion object {
        private const val BASE_URL = "https://gorest.co.in/public-api/users"
        private val logger: Logger = LoggerFactory.getLogger(SimpleWebClientConfigurationJetty::class.java)
    }

    @Bean
    fun defaultWebClientJetty(): WebClient {
        val httpClient: HttpClient = object : HttpClient(SslContextFactory()) {
            override fun newRequest(uri: URI?): Request? {
                val request: Request = super.newRequest(uri)
                return enhance(request)
            }
        }

        return WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeaders { header -> header.setBearerAuth("UYyeRzKAu1mLCDai8tdUDyu7wS6CdSC0Um3i") }
            .clientConnector(JettyClientHttpConnector(httpClient))
            .build()
    }

    object RequestLogEnhancer {
        fun enhance(request: Request): Request {
            val group = StringBuilder()
            request.onRequestBegin { theRequest: Request ->
                group
                    .append("Request ")
                    .append(theRequest.method)
                    .append(" ")
                    .append(theRequest.uri)
                    .append("\n")
            }
            request.onRequestHeaders { theRequest: Request ->
                for (header in theRequest.headers) group
                    .append(header)
                    .append("\n")
            }
            request.onRequestContent { theRequest: Request, content: ByteBuffer ->
                group.append(
                    content.toString(theRequest.headers.getCharset())
                )
            }
            request.onRequestSuccess { theRequest: Request? ->
                logger.debug("$group")
                group.delete(0, group.length)
            }
            group.append("\n")
            request.onResponseBegin { theResponse: Response ->
                group
                    .append("Response \n")
                    .append(theResponse.version)
                    .append(" ")
                    .append(theResponse.status)
                if (theResponse.reason != null) {
                    group
                        .append(" ")
                        .append(theResponse.reason)
                }
                group.append("\n")
            }
            request.onResponseHeaders { theResponse: Response ->
                for (header in theResponse.headers) group
                    .append(header)
                    .append("\n")
            }
            request.onResponseContent { theResponse: Response, content: ByteBuffer ->
                group.append(
                    content.toString(theResponse.headers.getCharset())
                )
            }
            request.onResponseSuccess { theResponse: Response? ->
                logger.debug("$group")
                group.delete(0, group.length)
            }
            return request
        }

        private fun ByteBuffer.toString(charset: Charset): String {
            val bytes: ByteArray
            if (hasArray()) {
                bytes = ByteArray(capacity())
                System.arraycopy(array(), 0, bytes, 0, capacity())
            } else {
                bytes = ByteArray(remaining())
                this[bytes, 0, bytes.size]
            }
            return String(bytes, charset)
        }

        private fun HttpFields.getCharset(): Charset {
            val contentType = this[HttpHeader.CONTENT_TYPE]
            if (contentType != null) {
                val tokens = contentType
                    .toLowerCase(Locale.US)
                    .split("charset=").toTypedArray()
                if (tokens.size == 2) {
                    val encoding = tokens[1].replace("[;\"]".toRegex(), "")
                    return Charset.forName(encoding)
                }
            }
            return StandardCharsets.UTF_8
        }
    }
}
