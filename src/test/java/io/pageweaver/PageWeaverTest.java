package io.pageweaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PageWeaverTest {

    @Test
    void requiresApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new PageWeaver(""));
    }

    @Test
    void exceptionCarriesStatusAndBody() {
        PageWeaverException e = new PageWeaverException("boom", 402, "quota");
        assertEquals(Integer.valueOf(402), e.getStatusCode());
        assertEquals("quota", e.getBody());
    }
}
