package com.ssrpro.library.dto.response;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingStatsRes {

  private int bookCount;
  private long recordedDays;
  private double averageRating;
  private String averageRatingDisplay;
  private int chartMax;
  private List<Integer> yAxisTicks;
  private List<MonthlyBar> monthlyBars;
  private String donutGradient;
  private List<GenreSlice> genres;
  private List<RatingBar> ratingBars;
  private List<ReadBookRes> recentFinished;

  public boolean isEmpty() {
    return bookCount <= 0;
  }

  public static ReadingStatsRes empty() {
    return ReadingStatsRes.builder()
        .bookCount(0)
        .recordedDays(0L)
        .averageRating(0)
        .averageRatingDisplay("0.0")
        .chartMax(10)
        .yAxisTicks(List.of(10, 0))
        .monthlyBars(Collections.emptyList())
        .donutGradient("conic-gradient(#e8e3dc 0 100%)")
        .genres(Collections.emptyList())
        .ratingBars(defaultRatingBars())
        .recentFinished(Collections.emptyList())
        .build();
  }

  private static List<RatingBar> defaultRatingBars() {
    return List.of(
        new RatingBar(5, 0, 0),
        new RatingBar(4, 0, 0),
        new RatingBar(3, 0, 0),
        new RatingBar(2, 0, 0),
        new RatingBar(1, 0, 0));
  }

  @Getter
  @AllArgsConstructor
  public static class MonthlyBar {
    private final String label;
    private final long value;
    private final int heightPercent;
  }

  @Getter
  @AllArgsConstructor
  public static class GenreSlice {
    private final String genre;
    private final int count;
    private final int barPercent;
    private final String color;
  }

  @Getter
  @AllArgsConstructor
  public static class RatingBar {
    private final int stars;
    private final int count;
    private final int barPercent;
  }
}
