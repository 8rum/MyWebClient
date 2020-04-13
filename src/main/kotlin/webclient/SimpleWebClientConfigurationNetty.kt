package webclient

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOption
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import io.netty.util.internal.PlatformDependent.allocateUninitializedArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import reactor.netty.Connection
import reactor.netty.channel.BootstrapHandlers
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import java.nio.charset.Charset
import java.nio.charset.Charset.defaultCharset
import java.util.function.Consumer

@Component
class SimpleWebClientConfigurationNetty {

    companion object {
        private const val BASE_URL = "https://gorest.co.in/public-api/users"
        private val logger: Logger = LoggerFactory.getLogger(SimpleWebClientConfigurationNetty::class.java)
    }

    @Bean
    fun defaultWebClientNetty(): WebClient.Builder {
        val tcpClient = TcpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
            .doOnConnected { connection: Connection ->
                connection.addHandlerLast(ReadTimeoutHandler(15))
                    .addHandlerLast(WriteTimeoutHandler(15))
            }.bootstrap { bootstrap: Bootstrap ->
                BootstrapHandlers.updateLogSupport(bootstrap, CustomLogger(HttpClient::class.java))
            }

        val httpClient = HttpClient
            .from(tcpClient)
            .wiretap(true)

        return WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeaders { header -> header.setBearerAuth("UYyeRzKAu1mLCDai8tdUDyu7wS6CdSC0Um3i") }
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
//            .build()
    }

    class CustomLogger(clazz: Class<*>?) : LoggingHandler(clazz) {
        override fun format(ctx: ChannelHandlerContext, event: String, arg: Any): String {
            if (arg is ByteBuf) {
                return decode(
                    arg, arg.readerIndex(), arg.readableBytes(), defaultCharset()
                )
            }
            return super.format(ctx, event, arg)
        }

        private fun decode(src: ByteBuf, readerIndex: Int, len: Int, charset: Charset): String {
            if (len != 0) {
                val array: ByteArray
                val offset: Int
                if (src.hasArray()) {
                    array = src.array()
                    offset = src.arrayOffset() + readerIndex
                } else {
                    array = allocateUninitializedArray(len.coerceAtLeast(1024))
                    offset = 0
                    src.getBytes(readerIndex, array, 0, len)
                }
                return String(array, offset, len, charset)
            }
            return ""
        }
    }

    private fun logRequest(): ExchangeFilterFunction {
        return ExchangeFilterFunction { clientRequest: ClientRequest, next: ExchangeFunction ->
            logger.info("Request: ${clientRequest.method()} ${clientRequest.url()}")
            logger.info("--- Http Headers: ---")
            clientRequest.headers()
                .forEach { name: String, values: List<String> ->
                    logHeader(
                        name,
                        values
                    )
                }
            next.exchange(clientRequest)
        }
    }

    private fun logResponse(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofResponseProcessor { clientResponse: ClientResponse ->
            logger.info("Response: ${clientResponse.statusCode()}")
            clientResponse.headers().asHttpHeaders()
                .forEach { name: String?, values: List<String?> ->
                    values.forEach(
                        Consumer { value: String? ->
                            logger.info("$name=$value")
                        }
                    )
                }
            Mono.just(clientResponse)
        }
    }

    private fun logHeader(name: String, values: List<String>) {
        values.forEach(Consumer { value: String? ->
            logger.info("$name=$value")
        })
    }
}
