package eu.eidas.auth.commons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests for EidasStringUtil")
class EidasStringUtilTest {

    @Test
    @DisplayName("Decode bytes from Base64")
    void testDecodeBytesFromBase64() {
        String base64String = "SGVsbG8gV29ybGQh"; // Base64 encoded "Hello World!"
        byte[] decodedBytes = EidasStringUtil.decodeBytesFromBase64(base64String);
        String decodedString = EidasStringUtil.toString(decodedBytes);
        assertEquals("Hello World!", decodedString);
    }

    @Test
    @DisplayName("Decode string from Base64")
    void testDecodeStringFromBase64() {
        String base64String = "SGVsbG8gV29ybGQh"; // Base64 encoded "Hello World!"
        String decodedString = EidasStringUtil.decodeStringFromBase64(base64String);
        assertEquals("Hello World!", decodedString);
    }

    @Test
    @DisplayName("Encode byte array to Base64")
    void testEncodeToBase64ByteArray() {
        String input = "Hello World!";
        byte[] bytes = EidasStringUtil.getBytes(input);
        String base64String = EidasStringUtil.encodeToBase64(bytes);
        assertEquals("SGVsbG8gV29ybGQh", base64String);
    }

    @Test
    @DisplayName("Encode string to Base64")
    void testEncodeToBase64String() {
        String input = "Hello World!";
        String base64String = EidasStringUtil.encodeToBase64(input);
        assertEquals("SGVsbG8gV29ybGQh", base64String);
    }

    @Test
    @DisplayName("Get bytes from string")
    void testGetBytes() {
        String input = "Hello World!";
        byte[] bytes = EidasStringUtil.getBytes(input);
        assertArrayEquals("Hello World!".getBytes(), bytes);
    }

    @Test
    @DisplayName("Convert bytes to string")
    void testToString() {
        byte[] bytes = "Hello World!".getBytes();
        String string = EidasStringUtil.toString(bytes);
        assertEquals("Hello World!", string);
    }

    @Test
    @DisplayName("Get tokens from string")
    void testGetTokens() {
        String tokens = "token1,token2;token3";
        List<String> tokenList = EidasStringUtil.getTokens(tokens);
        assertEquals(3, tokenList.size());
        assertTrue(tokenList.contains("token1"));
        assertTrue(tokenList.contains("token2"));
        assertTrue(tokenList.contains("token3"));
    }
}
