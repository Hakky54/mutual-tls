package nl.altindag.client;

import static nl.altindag.client.util.AssertJCustomConditions.GSON_CONVERTER_FACTORY;
import static nl.altindag.client.util.AssertJCustomConditions.SUBSTRING_OF_HTTP_OR_HTTPS_SERVER_URL;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.client.Client;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.api.client.http.HttpTransport;
import com.twitter.finagle.Service;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import kong.unirest.Unirest;
import nl.altindag.sslcontext.SSLFactory;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(MockitoJUnitRunner.class)
public class ClientConfigShould {

    private ClientConfig victim = new ClientConfig();

    @Test
    public void createSslTrustManagerHelper() {
        SSLFactory sslFactory = victim.sslFactory(false, false,
                                                              EMPTY, EMPTY.toCharArray(), EMPTY, EMPTY.toCharArray());

        assertThat(sslFactory).isNotNull();
        assertThat(sslFactory.isSecurityEnabled()).isFalse();
        assertThat(sslFactory.isOneWayAuthenticationEnabled()).isFalse();
        assertThat(sslFactory.isTwoWayAuthenticationEnabled()).isFalse();
    }

    @Test
    public void createSslTrustManagerHelperWithOneWayAuthentication() {
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        SSLFactory sslFactory = victim.sslFactory(true, false,
                                                              EMPTY, EMPTY.toCharArray(), trustStorePath, trustStorePassword.toCharArray());

        assertThat(sslFactory).isNotNull();
        assertThat(sslFactory.isSecurityEnabled()).isTrue();
        assertThat(sslFactory.isOneWayAuthenticationEnabled()).isTrue();
        assertThat(sslFactory.isTwoWayAuthenticationEnabled()).isFalse();
    }

    @Test
    public void createSslTrustManagerHelperWithTwoWayAuthentication() {
        String keyStorePath = "keystores-for-unit-tests/identity.jks";
        String keyStorePassword = "secret";
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        SSLFactory sslFactory = victim.sslFactory(false, true,
                                                              keyStorePath, keyStorePassword.toCharArray(), trustStorePath, trustStorePassword.toCharArray());

        assertThat(sslFactory).isNotNull();
        assertThat(sslFactory.isSecurityEnabled()).isTrue();
        assertThat(sslFactory.isOneWayAuthenticationEnabled()).isFalse();
        assertThat(sslFactory.isTwoWayAuthenticationEnabled()).isTrue();
    }

    @Test
    public void createApacheHttpClientWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        HttpClient httpClient = victim.apacheHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();
        verify(sslFactory, times(0)).getHostnameVerifier();
    }

    @Test
    public void createApacheHttpClientWithSecurity() throws NoSuchAlgorithmException {
        SSLFactory sslFactory = createSSLFactory(false, true);

        HttpClient httpClient = victim.apacheHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();
    }

    @Test
    public void createJdkHttpClientWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        java.net.http.HttpClient httpClient = victim.jdkHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();
    }

    @Test
    public void createJdkHttpClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        java.net.http.HttpClient httpClient = victim.jdkHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).getSslContext();
    }

    @Test
    public void createRestTemplate() {
        HttpClient httpClient = mock(HttpClient.class);

        RestTemplate restTemplate = victim.restTemplate(httpClient);

        assertThat(restTemplate).isNotNull();
    }

    @Test
    public void createOkHttpClientWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        OkHttpClient okHttpClient = victim.okHttpClient(sslFactory);

        assertThat(okHttpClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();
        verify(sslFactory, times(0)).getTrustManager();
        verify(sslFactory, times(0)).getHostnameVerifier();
    }

    @Test
    public void createOkHttpClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        OkHttpClient okHttpClient = victim.okHttpClient(sslFactory);

        assertThat(okHttpClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getTrustManager();
        verify(sslFactory, times(1)).getHostnameVerifier();

        assertThat(okHttpClient.hostnameVerifier()).isEqualTo(sslFactory.getHostnameVerifier());
        assertThat(okHttpClient.x509TrustManager()).isEqualTo(sslFactory.getTrustManager());
    }

    @Test
    public void createWebClientWithNettyWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        WebClient webClient = victim.webClientWithNetty(sslFactory);

        assertThat(webClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).isOneWayAuthenticationEnabled();
        verify(sslFactory, times(0)).isTwoWayAuthenticationEnabled();
        verify(sslFactory, times(0)).getSslContext();
        verify(sslFactory, times(0)).getKeyManagerFactory();
        verify(sslFactory, times(0)).getTrustManagerFactory();
    }

    @Test
    public void createWebClientWithNettyWithTwoWayAuthentication() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        WebClient webClient = victim.webClientWithNetty(sslFactory);

        assertThat(webClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).isOneWayAuthenticationEnabled();
        verify(sslFactory, times(1)).isTwoWayAuthenticationEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getKeyManagerFactory();
        verify(sslFactory, times(1)).getTrustManagerFactory();
    }

    @Test
    public void createWebClientWithNettyWithOneWayAuthentication() {
        SSLFactory sslFactory = createSSLFactory(true, false);

        WebClient webClient = victim.webClientWithNetty(sslFactory);

        assertThat(webClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).isOneWayAuthenticationEnabled();
        verify(sslFactory, times(1)).isTwoWayAuthenticationEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(0)).getKeyManagerFactory();
        verify(sslFactory, times(1)).getTrustManagerFactory();
    }

    @Test
    public void createWebClientWithJettyWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        WebClient webClient = victim.webClientWithJetty(sslFactory);

        assertThat(webClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).isOneWayAuthenticationEnabled();
        verify(sslFactory, times(0)).isTwoWayAuthenticationEnabled();
        verify(sslFactory, times(0)).getSslContext();
        verify(sslFactory, times(0)).getHostnameVerifier();
        verify(sslFactory, times(0)).getTrustStores();
        verify(sslFactory, times(0)).getIdentities();
    }

    @Test
    public void createWebClientWithJettyWithTwoWayAuthentication() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        WebClient webClient = victim.webClientWithJetty(sslFactory);

        assertThat(webClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).isOneWayAuthenticationEnabled();
        verify(sslFactory, times(0)).isTwoWayAuthenticationEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();
        verify(sslFactory, times(0)).getTrustStores();
        verify(sslFactory, times(0)).getIdentities();
    }

    @Test
    public void createWebClientWithJettyWithOneWayAuthentication() {
        SSLFactory sslFactory = createSSLFactory(true, false);

        WebClient webClient = victim.webClientWithJetty(sslFactory);

        assertThat(webClient).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).isOneWayAuthenticationEnabled();
        verify(sslFactory, times(0)).isTwoWayAuthenticationEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();
        verify(sslFactory, times(0)).getTrustStores();
        verify(sslFactory, times(0)).getIdentities();
    }

    @Test
    public void createJerseyClientWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        Client client = victim.jerseyClient(sslFactory);

        assertThat(client).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();
        verify(sslFactory, times(0)).getHostnameVerifier();

        client.close();
    }

    @Test
    public void createJerseyClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        Client client = victim.jerseyClient(sslFactory);

        assertThat(client).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();

        client.close();
    }

    @Test
    public void createOldJerseyClientWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        com.sun.jersey.api.client.Client client = victim.oldJerseyClient(sslFactory);

        assertThat(client).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();
        verify(sslFactory, times(0)).getHostnameVerifier();

        client.destroy();
    }

    @Test
    public void createOldJerseyClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        com.sun.jersey.api.client.Client client = victim.oldJerseyClient(sslFactory);

        assertThat(client).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(2)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();

        client.destroy();
    }

    @Test
    public void createGoogleHttpClientWithoutSecurity() throws IOException {
        SSLFactory sslFactory = createSSLFactory(false, false);

        HttpTransport httpTransport = victim.googleHttpClient(sslFactory);

        assertThat(httpTransport).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();
        verify(sslFactory, times(0)).getHostnameVerifier();

        httpTransport.shutdown();
    }

    @Test
    public void createGoogleHttpClientWithSecurity() throws IOException {
        SSLFactory sslFactory = createSSLFactory(false, true);

        HttpTransport httpTransport = victim.googleHttpClient(sslFactory);

        assertThat(httpTransport).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();

        httpTransport.shutdown();
    }

    @Test
    public void createUnirestWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        victim.unirest(sslFactory);

        assertThat(Unirest.primaryInstance().config().getSslContext()).isNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();
        verify(sslFactory, times(0)).getHostnameVerifier();

        Unirest.shutDown();
    }

    @Test
    public void createUnirestWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        victim.unirest(sslFactory);

        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();

        assertThat(Unirest.primaryInstance().config().getSslContext()).isEqualTo(sslFactory.getSslContext());

        Unirest.shutDown();
    }

    @Test
    public void createRetrofitWithProvidedOkHttpClient() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Retrofit retrofit = victim.retrofit(okHttpClient);

        assertThat(retrofit).isNotNull();
        assertThat(retrofit.baseUrl().toString()).has(SUBSTRING_OF_HTTP_OR_HTTPS_SERVER_URL);
        assertThat(retrofit.converterFactories()).has(GSON_CONVERTER_FACTORY);
    }

    @Test
    public void createFinagleClientWithoutSecurity() throws URISyntaxException {
        SSLFactory sslFactory = createSSLFactory(false, false);

        Service<Request, Response> service = victim.finagle(sslFactory);

        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();

        assertThat(service.isAvailable()).isTrue();
        assertThat(service.status().toString()).isEqualTo("Open");

        service.close();
    }

    @Test
    public void createFinagleClientWithSecurity() throws URISyntaxException {
        SSLFactory sslFactory = createSSLFactory(false, true);

        Service<Request, Response> service = victim.finagle(sslFactory);

        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).getSslContext();

        assertThat(service.isAvailable()).isTrue();
        assertThat(service.status().toString()).isEqualTo("Open");

        service.close();
    }

    @Test
    public void createAkkaHttpClientWithoutSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, false);

        Http http = victim.akkaHttpClient(sslFactory, ActorSystem.create());

        assertThat(http).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(0)).getSslContext();
    }

    @Test
    public void createAkkaHttpClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        Http http = victim.akkaHttpClient(sslFactory, ActorSystem.create());

        assertThat(http).isNotNull();
        verify(sslFactory, times(1)).isSecurityEnabled();
        verify(sslFactory, times(1)).getSslContext();
    }

    @Test
    public void createActorSystem() {
        ActorSystem actorSystem = victim.actorSystem();

        assertThat(actorSystem).isNotNull();
        assertThat(actorSystem.name()).isEqualTo("ClientConfig");
    }

    private SSLFactory createSSLFactory(boolean oneWayAuthenticationEnabled, boolean twoWayAuthenticationEnabled) {
        String keyStorePath = "keystores-for-unit-tests/identity.jks";
        String keyStorePassword = "secret";
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
        if (oneWayAuthenticationEnabled) {
            sslFactoryBuilder.withTrustStore(trustStorePath, trustStorePassword.toCharArray())
                             .withHostnameVerifierEnabled(true);
        }

        if (twoWayAuthenticationEnabled) {
            sslFactoryBuilder.withIdentity(keyStorePath, keyStorePassword.toCharArray())
                             .withTrustStore(trustStorePath, trustStorePassword.toCharArray())
                             .withHostnameVerifierEnabled(true);
        }
        return Mockito.spy(sslFactoryBuilder.build());
    }

}
