package com.eleclib.util;

import com.eleclib.dto.ReaderTocEntryDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReaderTocTest {

    @Test
    void matchesTocLine_recognizesCommonHeadings() {
        assertTrue(ReaderToc.matchesTocLine("Глава 1"));
        assertTrue(ReaderToc.matchesTocLine("ГЛАВА IV. Тайна"));
        assertTrue(ReaderToc.matchesTocLine("Chapter 12: Begin"));
        assertTrue(ReaderToc.matchesTocLine("§ 3 Начало"));
        assertTrue(ReaderToc.matchesTocLine("IV. Пролог"));
        assertTrue(ReaderToc.matchesTocLine("1. Введение"));
        assertTrue(ReaderToc.matchesTocLine("### Оглавление"));
        assertTrue(ReaderToc.matchesTocLine("Введение"));
        assertFalse(ReaderToc.matchesTocLine("Обычный абзац без структуры."));
        assertFalse(ReaderToc.matchesTocLine("глава была давно"));
    }

    @Test
    void build_mapsHeadingsToPages() {
        StringBuilder sb = new StringBuilder();
        sb.append("Глава 1\n");
        sb.append("x".repeat(3000)).append("\n");
        sb.append("Глава 2\n");
        sb.append("y".repeat(500));
        List<ReaderTocEntryDto> toc = ReaderToc.build(sb.toString());
        assertEquals(2, toc.size());
        assertEquals("Глава 1", toc.get(0).getTitle());
        assertEquals(1, toc.get(0).getPageNumber());
        assertEquals("Глава 2", toc.get(1).getTitle());
        assertTrue(toc.get(1).getPageNumber() >= 2);
    }

    @Test
    void pageForCharOffset_firstPage() {
        List<String> pages = List.of("abc", "defgh");
        assertEquals(1, ReaderToc.pageForCharOffset(pages, 0));
        assertEquals(1, ReaderToc.pageForCharOffset(pages, 2));
        assertEquals(2, ReaderToc.pageForCharOffset(pages, 3));
    }
}
