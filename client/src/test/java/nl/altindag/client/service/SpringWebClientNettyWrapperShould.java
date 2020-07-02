package nl.altindag.client.service;

import static nl.altindag.client.ClientType.SPRING_WEB_CLIENT_NETTY;
import static nl.altindag.client.TestConstants.HEADER_KEY_CLIENT_TYPE;
import static nl.altindag.client.TestConstants.HTTP_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import nl.altindag.client.model.ClientResponse;
import reactor.core.publisher.Mono;

@RunWith(MockitoJUnitRunner.class)
public class SpringWebClientNettyWrapperShould {

    @InjectMocks
    private SpringWebClientNettyWrapper victim;
    @Mock
    private WebClient webClient;

    @Test
    public void getClientType() {
        assertThat(victim.getClientType()).isEqualTo(SPRING_WEB_CLIENT_NETTY);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executeRequest() throws Exception {
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        org.springframework.web.reactive.function.client.ClientResponse response = mock(org.springframework.web.reactive.function.client.ClientResponse.class);
        Mono<org.springframework.web.reactive.function.client.ClientResponse> clientResponseMono = Mono.just(response);
        ResponseEntity<String> responseEntity = mock(ResponseEntity.class);
        Mono<ResponseEntity<String>> responseEntityMono = Mono.just(responseEntity);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(HTTP_URL)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.header(HEADER_KEY_CLIENT_TYPE, SPRING_WEB_CLIENT_NETTY.getValue())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.exchange()).thenReturn(clientResponseMono);
        when(response.toEntity(String.class)).thenReturn(responseEntityMono);
        when(responseEntity.getBody()).thenReturn("Hello");
        when(responseEntity.getStatusCodeValue()).thenReturn(200);

        ClientResponse clientResponse = victim.executeRequest(HTTP_URL);

        assertThat(clientResponse.getStatusCode()).isEqualTo(200);
        assertThat(clientResponse.getResponseBody()).isEqualTo("Hello");
    }

}
