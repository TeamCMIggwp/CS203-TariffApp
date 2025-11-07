package wits.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WitsApiExceptionTest {

    @Test
    void constructor_withStatusCode_setsMessageAndStatusCode() {
        WitsApiException ex = new WitsApiException("WITS API communication error", 502);

        assertEquals("WITS API communication error", ex.getMessage());
        assertEquals(502, ex.getStatusCode());
        assertNull(ex.getCause());
    }

    @Test
    void constructor_withCause_setsMessageAndCause_leavesStatusCodeDefaultZero() {
        Throwable cause = new RuntimeException("boom");
        WitsApiException ex = new WitsApiException("Unexpected error: boom", cause);

        assertEquals("Unexpected error: boom", ex.getMessage());
        assertSame(cause, ex.getCause());
        // This ctor does not set statusCode, so it stays 0 by design
        assertEquals(0, ex.getStatusCode());
    }
}
