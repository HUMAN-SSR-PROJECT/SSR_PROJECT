package com.ssrpro.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ssrpro.library.dao.ReadBookDao;
import com.ssrpro.library.dto.response.ReadBookRes;
import com.ssrpro.library.dto.response.ReadingStatsRes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReadBookServiceTest {

  @Mock private ReadBookDao readBookDao;

  @InjectMocks private ReadBookService readBookService;

  @Test
  @DisplayName("readingDays — 시작·완독 같은 날이면 1일")
  void readingDays_sameDay_returnsOne() {
    LocalDateTime day = LocalDate.of(2026, 5, 1).atStartOfDay();
    ReadBookRes book =
        ReadBookRes.builder().readBookStart(day).readBookEnd(day).build();

    long days = ReflectionTestUtils.invokeMethod(ReadBookService.class, "readingDays", book);

    assertThat(days).isEqualTo(1);
  }

  @Test
  @DisplayName("readingDays — 5/1~5/3 구간이면 3일(포함)")
  void readingDays_threeDaySpan_returnsThree() {
    ReadBookRes book =
        ReadBookRes.builder()
            .readBookStart(LocalDate.of(2026, 5, 1).atStartOfDay())
            .readBookEnd(LocalDate.of(2026, 5, 3).atStartOfDay())
            .build();

    long days = ReflectionTestUtils.invokeMethod(ReadBookService.class, "readingDays", book);

    assertThat(days).isEqualTo(3);
  }

  @Test
  @DisplayName("readingDays — 완독일이 시작일보다 이전이면 0일")
  void readingDays_endBeforeStart_returnsZero() {
    ReadBookRes book =
        ReadBookRes.builder()
            .readBookStart(LocalDate.of(2026, 5, 3).atStartOfDay())
            .readBookEnd(LocalDate.of(2026, 5, 1).atStartOfDay())
            .build();

    long days = ReflectionTestUtils.invokeMethod(ReadBookService.class, "readingDays", book);

    assertThat(days).isZero();
  }

  @Test
  @DisplayName("buildReadingStats — 당일 완독 1권 기록 일수 1")
  void buildReadingStats_sameDayFinished_recordedDaysIsOne() {
    LocalDateTime day = LocalDate.of(2026, 5, 1).atStartOfDay();
    ReadBookRes book =
        ReadBookRes.builder()
            .readBookStart(day)
            .readBookEnd(day)
            .readBookState("3")
            .build();
    when(readBookDao.getRawData(1L)).thenReturn(List.of(book));

    ReadingStatsRes stats = readBookService.buildReadingStats(1L);

    assertThat(stats.getRecordedDays()).isEqualTo(1);
  }
}
