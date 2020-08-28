package nl.altindag.client.service

import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import nl.altindag.client.ClientType.KTOR_ANDROID_HTTP_CLIENT
import nl.altindag.client.TestConstants.HTTPS_URL
import nl.altindag.client.TestConstants.HTTP_URL
import nl.altindag.client.util.MockServerTestHelper
import nl.altindag.client.util.SSLFactoryTestHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class KtorAndroidHttpClientShould {

    @Test
    fun executeRequest() {
        val mockServerTestHelper = MockServerTestHelper(KTOR_ANDROID_HTTP_CLIENT)
        val client = KtorAndroidHttpClient(null)

        val clientResponse = client.executeRequest(HTTP_URL)

        assertThat(clientResponse.statusCode).isEqualTo(200)
        assertThat(clientResponse.responseBody).isEqualTo("Hello")

        mockServerTestHelper.stop()
    }

    @Test
    fun createClientWithSslMaterial() {
        val sslFactory = SSLFactoryTestHelper.createSSLFactory(true, true)
        val client = KtorAndroidHttpClient(sslFactory)

        assertThatThrownBy { client.executeRequest(HTTPS_URL) }

        verify(sslFactory, times(1)).sslContext
        verify(sslFactory, times(1)).hostnameVerifier
    }

}
