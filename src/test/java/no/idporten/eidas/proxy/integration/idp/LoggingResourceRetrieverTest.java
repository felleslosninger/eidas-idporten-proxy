package no.idporten.eidas.proxy.integration.idp;

import com.nimbusds.jose.util.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Slf4j
class LoggingResourceRetrieverTest {

    @Test
    @DisplayName("Test resource retrieval")
    void testRetrieveResource() throws IOException {
        LoggingResourceRetriever retriever = new LoggingResourceRetriever(5000, 5000);

        URL mockUrl = new URL("https://example.com");

        Resource resource = retriever.retrieveResource(mockUrl);
        assertNotNull(resource, "Resource should not be null");
    }

    @Test
    @DisplayName("Test resource retrieval failure")
    void testRetrieveResourceFailure() {
        String wrongUrl = "https://nonexistent.example.com";
        LoggingResourceRetriever retriever = new LoggingResourceRetriever(5000, 5000);

        // Mock URL that does not exist
        final URL mockUrl;
        try {
            mockUrl = new URL(wrongUrl);
        } catch (IOException e) {
            log.info("Verifying that the resource does not exist {}", wrongUrl);
            return;
        }

        // Resource retrieval should throw IOException
        assertThrows(IOException.class, () -> retriever.retrieveResource(mockUrl));
    }

}
