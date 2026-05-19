package com.ssrpro.library.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LibraryServiceBookExistTest {

    @Autowired
    private LibraryService libraryService;

    @Test
    @DisplayName("bookExist JSON — result.hasBook / result.loanAvailable 파싱")
    void parseBookExistResponse_nestedResult() {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasBook", "Y");
        result.put("loanAvailable", "N");
        response.put("result", result);

        Boolean loan =
                ReflectionTestUtils.invokeMethod(
                        libraryService, "parseBookExistResponse", response, null);

        assertThat(loan).isFalse();
    }

    @Test
    @DisplayName("bookExist JSON — 대출 가능")
    void parseBookExistResponse_loanAvailableYes() {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasBook", "Y");
        result.put("loanAvailable", "Y");
        response.put("result", result);

        Boolean loan =
                ReflectionTestUtils.invokeMethod(
                        libraryService, "parseBookExistResponse", response, null);

        assertThat(loan).isTrue();
    }
}
