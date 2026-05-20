package com.eleclib.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaderPagingTest {

    @Test
    void totalPages_emptyText_isOne() {
        assertEquals(1, ReaderPaging.totalPages(""));
        assertEquals(1, ReaderPaging.totalPages("   "));
    }

    @Test
    void totalPages_shortText_isOne() {
        assertEquals(1, ReaderPaging.totalPages("hello"));
    }

    @Test
    void totalPages_longText_isAtLeastTwo() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            sb.append("word").append(i).append(' ');
            sb.append("x".repeat(120));
            sb.append(' ');
        }
        assertTrue(ReaderPaging.totalPages(sb.toString()) >= 2);
    }
}
