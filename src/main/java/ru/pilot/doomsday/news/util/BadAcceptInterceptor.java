package ru.pilot.doomsday.news.util;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * В RSS зачастую есть ошибки с Content-Type. Этот перехватчик исправляет
 */
public class BadAcceptInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        return new ClientHttpResponseWrapper(response);
    }

    static class ClientHttpResponseWrapper implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final HttpHeaders headers;

        public ClientHttpResponseWrapper(ClientHttpResponse delegate) {
            this.delegate = delegate;
            HttpHeaders originalHeaders = new HttpHeaders(delegate.getHeaders());

            String first = originalHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
            if (first != null && first.contains("application/rss+xml")) {
                originalHeaders.setContentType(MediaType.APPLICATION_RSS_XML);
            }
            this.headers = HttpHeaders.readOnlyHttpHeaders(originalHeaders);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return this.delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return this.delegate.getStatusText();
        }

        @Override
        public void close() {
            this.delegate.close();
        }

        @Override
        public InputStream getBody() throws IOException {
            return this.delegate.getBody();
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }
    }
}