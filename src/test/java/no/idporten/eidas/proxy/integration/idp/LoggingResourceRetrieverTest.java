package no.idporten.eidas.proxy.integration.idp;

import com.nimbusds.jose.util.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class LoggingResourceRetrieverTest {

    @Test
    @DisplayName("Test resource retrieval")
    void testRetrieveResource() throws IOException, URISyntaxException {
        LoggingResourceRetriever retriever = spy(new LoggingResourceRetriever(5000, 5000));
        URL mockUrl = new URI("https://example.com").toURL();
        Resource expectedResource = new Resource("content", "text/html");

        doReturn(expectedResource).when((com.nimbusds.jose.util.DefaultResourceRetriever) retriever).retrieveResource(mockUrl);

        Resource resource = retriever.retrieveResource(mockUrl);
        assertNotNull(resource, "Resource should not be null");
    }

    @Test
    @DisplayName("Test resource retrieval failure")
    void testRetrieveResourceFailure() throws IOException, URISyntaxException {
        LoggingResourceRetriever retriever = spy(new LoggingResourceRetriever(5000, 5000));
        URL mockUrl =  new URI("https://nonexistent.example.com").toURL();

        doThrow(new IOException("Connection failed")).when((com.nimbusds.jose.util.DefaultResourceRetriever) retriever).retrieveResource(mockUrl);

        assertThrows(IOException.class, () -> retriever.retrieveResource(mockUrl));
    }

}
