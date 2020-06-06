package org.springframework.cloud.openfeign.cloud;

/**
 * 依韵 2020/3/19
 */
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Default implementation of {@link ApacheHttpClientFactory}.
 * @author Ryan Baxter
 */
public class DefaultApacheHttpClientFactory implements ApacheHttpClientFactory {

    private HttpClientBuilder builder;

    public DefaultApacheHttpClientFactory(HttpClientBuilder builder) {
        this.builder = builder;
    }

    /**
     * A default {@link HttpClientBuilder}. The {@link HttpClientBuilder} returned will
     * have content compression disabled, have cookie management disabled, and use system
     * properties.
     */
    @Override
    public HttpClientBuilder createBuilder() {
        return this.builder.disableContentCompression()
                .disableCookieManagement().useSystemProperties();
    }
}

