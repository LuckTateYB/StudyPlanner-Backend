package com.studyplan.studyplan.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class AiClientConfig {

    /** Timeout for connecting to the AI API, in milliseconds. */
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    /** Timeout for reading a response from the AI API, in milliseconds. */
    private static final int READ_TIMEOUT_MS = 30_000;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    @Value("${ai.api.key}")
    private String aiApiKey;

    @Value("${ai.api.model}")
    private String aiModel;

    @Value("${ai.api.max-tokens:256}")
    private int maxTokens;

    /**
     * Creates the {@link RestTemplate} bean used for AI API calls.
     * Configured with reasonable timeouts to avoid hanging requests.
     *
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    // -------------------------------------------------------------------------
    // Getters for injection into other components
    // -------------------------------------------------------------------------

    public String getAiApiUrl() { return aiApiUrl; }
    public String getAiApiKey() { return aiApiKey; }
    public String getAiModel()  { return aiModel;  }
    public int    getMaxTokens(){ return maxTokens; }
}
