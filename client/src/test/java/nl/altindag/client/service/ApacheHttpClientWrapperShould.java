package nl.altindag.client.service;

import static nl.altindag.client.ClientType.APACHE_HTTP_CLIENT;
import static nl.altindag.client.Constants.HEADER_KEY_CLIENT_TYPE;
import static nl.altindag.client.TestConstants.GET_METHOD;
import static nl.altindag.client.TestConstants.HTTP_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import nl.altindag.client.model.ClientResponse;

@RunWith(MockitoJUnitRunner.class)
public class ApacheHttpClientWrapperShould {

    @InjectMocks
    private ApacheHttpClientWrapper victim;
    @Mock
    private HttpClient httpClient;

    @Test
    public void executeRequest() throws Exception {
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);
        InputStream stream = new ByteArrayInputStream("Hello".getBytes());

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(stream);

        ArgumentCaptor<HttpGet> httpGetArgumentCaptor = ArgumentCaptor.forClass(HttpGet.class);
        ClientResponse clientResponse = victim.executeRequest(HTTP_URL);

        stream.close();

        assertThat(clientResponse.getStatusCode()).isEqualTo(200);
        assertThat(clientResponse.getResponseBody()).isEqualTo("Hello");

        verify(httpClient, times(1)).execute(httpGetArgumentCaptor.capture());
        assertThat(httpGetArgumentCaptor.getValue().getURI().toString()).isEqualTo(HTTP_URL);
        assertThat(httpGetArgumentCaptor.getValue().getMethod()).isEqualTo(GET_METHOD);
        assertThat(httpGetArgumentCaptor.getValue().getAllHeaders()).hasSize(1);
        assertThat(httpGetArgumentCaptor.getValue().getFirstHeader(HEADER_KEY_CLIENT_TYPE).getValue()).isEqualTo(APACHE_HTTP_CLIENT.getValue());
    }

}
