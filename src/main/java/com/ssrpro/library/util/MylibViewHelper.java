package com.ssrpro.library.util;

/**
 * Thymeleaf에서 SpEL 제한(Math.round·cast 등)을 피하기 위한 내 서재 뷰 헬퍼.
 */
public final class MylibViewHelper {

    private MylibViewHelper() {
    }

    /** 0~5 별점 — rating이 null이거나 숫자가 아니면 0 */
    public static int filledStarCount(Number rating) {
        if (rating == null) {
            return 0;
        }
        return Math.round(rating.floatValue());
    }
}
