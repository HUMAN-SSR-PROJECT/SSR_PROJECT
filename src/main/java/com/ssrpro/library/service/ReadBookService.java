package com.ssrpro.library.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssrpro.library.dao.ReadBookDao;
import com.ssrpro.library.dto.request.ReadBookReq;
import com.ssrpro.library.dto.response.ReadBookRes;
import com.ssrpro.library.dto.response.ReadingStatsRes;
import com.ssrpro.library.dto.response.ReadingStatsRes.GenreSlice;
import com.ssrpro.library.dto.response.ReadingStatsRes.MonthlyBar;
import com.ssrpro.library.dto.response.ReadingStatsRes.RatingBar;
import com.ssrpro.library.dto.response.ReadSoonRes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadBookService {
  private static final String[] GENRE_COLORS = {
      "#111111", "#555555", "#888880", "#aaaaaa", "#bbbbbb", "#cccccc", "#d4cfc8", "#e8e3dc"
  };
  private static final DateTimeFormatter MONTH_LABEL =
      DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN);

  private final ReadBookDao readBookDao;

  /** 1: 없음, 2: 읽는 중, 3: 완독 */
  public int readingState(Long memberId, Long bookId) {
    return readBookDao.isReadBookDuplicate(memberId, bookId);
  }

  public boolean isReadSoon(Long memberId, Long bookId) {
    return readBookDao.isReadSoonDuplicate(memberId, bookId);
  }

  // 읽을 책 추가 / 삭제
  @Transactional
  public boolean addToReadSoon(Long memberId, Long bookId) {
    if (readBookDao.isReadSoonDuplicate(memberId, bookId)) {
      return readBookDao.deleteReadSoon(memberId, bookId);
    }
    return readBookDao.addToReadSoon(memberId, bookId);
  }

  // 내 서재 - 읽을 책 전체 조회
  public List<ReadSoonRes> readSoonList(Long memberId) {
    List<ReadSoonRes> readSoonRes = readBookDao.readSoonList(memberId);
    return (readSoonRes != null) ? readSoonRes : Collections.emptyList();
  }

  /**
   * 읽을 책(즐겨찾기) → 읽는 중 추가. read_soon 행은 유지한다.
   * 도서 상세 «독서 기록» 토글과 달리 읽는 중이어도 삭제하지 않는다.
   */
  @Transactional
  public boolean addToReadingFromWishlist(Long memberId, Long bookId) {
    int state = readBookDao.isReadBookDuplicate(memberId, bookId);
    return switch (state) {
      case 1 -> readBookDao.addToReading(memberId, bookId);
      case 2 -> true;
      case 3 -> throw new IllegalStateException("완독한 책은 읽는 중으로 이동할 수 없습니다.");
      default -> throw new IllegalStateException("알 수 없는 상태입니다.");
    };
  }

  // 내 서재·도서 상세 — 읽는 중 추가 / 삭제 토글
  // 1 : 데이터 없음, 2 : 읽는 중, 3 : 완독
  @Transactional
  public boolean addToReading(Long memberId, Long bookId) {
    int state = readBookDao.isReadBookDuplicate(memberId, bookId);
    switch (state) {
      case 1:
        return readBookDao.addToReading(memberId, bookId);
      case 2:
        return readBookDao.deleteReading(memberId, bookId);
      case 3:
        throw new IllegalStateException("완독한 책은 삭제할 수 없습니다.");
      default:
        throw new IllegalStateException("알 수 없는 상태입니다.");
    }
  }

  // 내 서재 - 읽는 중 전체 조회
  public List<ReadBookRes> readingList(Long memberId) {
    List<ReadBookRes> readBookRes = readBookDao.readingList(memberId);
    return (readBookRes != null) ? readBookRes : Collections.emptyList();
  }

  // 내 서재 - 읽는 중 → 완독, 완독 기록 수정
  @Transactional
  public boolean changeToReaded(Long memberId, ReadBookReq readBookReq) {
    int state = readBookDao.isReadBookDuplicate(memberId, readBookReq.getBookId());
    if (state == 1) {
      throw new IllegalStateException("존재하지 않는 정보입니다.");
    }
    return readBookDao.changeToReaded(readBookReq, memberId);
  }

  // 내 서재 - 완독 전체 조회
  public List<ReadBookRes> readedList(Long memberId) {
    List<ReadBookRes> readBookRes = readBookDao.readedList(memberId);
    return (readBookRes != null) ? readBookRes : Collections.emptyList();
  }

  // 내 서재 - 완독 상세 조회
  public ReadBookRes readedInfo(Long memberId, Long bookId) {
    if (readBookDao.isReadBookDuplicate(memberId, bookId) == 1) {
      throw new IllegalStateException("존재하지 않는 정보입니다.");
    }
    ReadBookRes readBookRes = readBookDao.readedInfo(memberId, bookId);
    return readBookRes;
  }

  // 독서 분석 리포트 - 통계 계산용 리스트 (완독만)
  public List<ReadBookRes> getRawData(Long memberId) {
    List<ReadBookRes> readBookRes = readBookDao.getRawData(memberId);
    return (readBookRes != null) ? readBookRes : Collections.emptyList();
  }

  /** 완독 기준 독서 통계 (Figma 1:1200) */
  public ReadingStatsRes buildReadingStats(Long memberId) {
    List<ReadBookRes> finished = getRawData(memberId);
    if (finished.isEmpty()) {
      return ReadingStatsRes.empty();
    }

    long recordedDays = 0;
    double ratingSum = 0;
    int ratedCount = 0;
    NavigableMap<YearMonth, Long> monthlyDays = new TreeMap<>();
    Map<String, Integer> genreCounts = new LinkedHashMap<>();
    int[] ratingCounts = new int[6];

    for (ReadBookRes book : finished) {
      long days = readingDays(book);
      recordedDays += days;

      if (book.getReadBookEnd() != null) {
        YearMonth month = YearMonth.from(book.getReadBookEnd());
        monthlyDays.merge(month, days, Long::sum);
      }

      if (book.getBookGenre() != null && !book.getBookGenre().isBlank()) {
        genreCounts.merge(book.getBookGenre(), 1, Integer::sum);
      }

      Double rating = book.getReadBookRating();
      if (rating != null && rating > 0) {
        ratingSum += rating;
        ratedCount++;
        int star = Math.max(1, Math.min(5, (int) Math.round(rating)));
        ratingCounts[star]++;
      }
    }

    int bookCount = finished.size();
    double averageRating = ratedCount == 0 ? 0 : ratingSum / ratedCount;
    String averageRatingDisplay = String.format(Locale.KOREAN, "%.1f", averageRating);

    long maxMonthly = monthlyDays.values().stream().mapToLong(Long::longValue).max().orElse(0);
    int chartMax = (int) Math.max(10, ((maxMonthly + 9) / 10) * 10);
    List<Integer> yAxisTicks = buildYAxisTicks(chartMax);
    List<MonthlyBar> monthlyBars = buildMonthlyBars(monthlyDays, chartMax);

    List<GenreSlice> genres = buildGenreSlices(genreCounts);
    String donutGradient = buildDonutGradient(genres, bookCount);
    List<RatingBar> ratingBars = buildRatingBars(ratingCounts);

    List<ReadBookRes> recentFinished =
        finished.stream()
            .sorted(
                Comparator.comparing(
                    ReadBookRes::getReadBookEnd, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(10)
            .collect(Collectors.toList());

    return ReadingStatsRes.builder()
        .bookCount(bookCount)
        .recordedDays(recordedDays)
        .averageRating(averageRating)
        .averageRatingDisplay(averageRatingDisplay)
        .chartMax(chartMax)
        .yAxisTicks(yAxisTicks)
        .monthlyBars(monthlyBars)
        .donutGradient(donutGradient)
        .genres(genres)
        .ratingBars(ratingBars)
        .recentFinished(recentFinished)
        .build();
  }

  private static long readingDays(ReadBookRes book) {
    if (book.getReadBookEnd() == null) {
      return 0;
    }
    LocalDate end = book.getReadBookEnd().toLocalDate();
    LocalDate start =
        book.getReadBookStart() != null ? book.getReadBookStart().toLocalDate() : end;
    if (end.isBefore(start)) {
      return 0;
    }
    return ChronoUnit.DAYS.between(start, end) + 1;
  }

  private static List<Integer> buildYAxisTicks(int chartMax) {
    List<Integer> ticks = new ArrayList<>();
    for (int value = chartMax; value >= 0; value -= 10) {
      ticks.add(value);
    }
    return ticks;
  }

  private static List<MonthlyBar> buildMonthlyBars(NavigableMap<YearMonth, Long> monthlyDays, int chartMax) {
    if (monthlyDays.isEmpty()) {
      return Collections.emptyList();
    }
    YearMonth cursor = monthlyDays.keySet().iterator().next();
    YearMonth last = monthlyDays.lastKey();
    List<MonthlyBar> bars = new ArrayList<>();
    while (!cursor.isAfter(last)) {
      long value = monthlyDays.getOrDefault(cursor, 0L);
      int heightPercent = chartMax == 0 ? 0 : (int) Math.round(value * 100.0 / chartMax);
      String label = cursor.atDay(1).format(MONTH_LABEL);
      bars.add(new MonthlyBar(label, value, heightPercent));
      cursor = cursor.plusMonths(1);
    }
    return bars;
  }

  private static List<GenreSlice> buildGenreSlices(Map<String, Integer> genreCounts) {
    if (genreCounts.isEmpty()) {
      return Collections.emptyList();
    }
    int total = genreCounts.values().stream().mapToInt(Integer::intValue).sum();
    List<Map.Entry<String, Integer>> sorted =
        genreCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());

    List<GenreSlice> slices = new ArrayList<>();
    for (int i = 0; i < sorted.size(); i++) {
      Map.Entry<String, Integer> entry = sorted.get(i);
      int barPercent = (int) Math.round(entry.getValue() * 100.0 / total);
      String color = GENRE_COLORS[i % GENRE_COLORS.length];
      slices.add(new GenreSlice(entry.getKey(), entry.getValue(), barPercent, color));
    }
    return slices;
  }

  private static String buildDonutGradient(List<GenreSlice> genres, int bookCount) {
    if (bookCount <= 0 || genres.isEmpty()) {
      return "conic-gradient(#e8e3dc 0 100%)";
    }
    int total = genres.stream().mapToInt(GenreSlice::getCount).sum();
    StringBuilder gradient = new StringBuilder("conic-gradient(");
    double accumulated = 0;
    for (int i = 0; i < genres.size(); i++) {
      GenreSlice slice = genres.get(i);
      double end = accumulated + slice.getCount() * 100.0 / total;
      if (i > 0) {
        gradient.append(", ");
      }
      gradient
          .append(slice.getColor())
          .append(" ")
          .append(accumulated)
          .append("% ")
          .append(end)
          .append("%");
      accumulated = end;
    }
    gradient.append(")");
    return gradient.toString();
  }

  private static List<RatingBar> buildRatingBars(int[] ratingCounts) {
    int maxCount = 0;
    for (int star = 1; star <= 5; star++) {
      maxCount = Math.max(maxCount, ratingCounts[star]);
    }
    List<RatingBar> bars = new ArrayList<>();
    for (int star = 5; star >= 1; star--) {
      int count = ratingCounts[star];
      int barPercent = maxCount == 0 ? 0 : (int) Math.round(count * 100.0 / maxCount);
      bars.add(new RatingBar(star, count, barPercent));
    }
    return bars;
  }
}
