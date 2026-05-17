package com.ssrpro.library.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssrpro.library.dto.request.BookDetailReq;
import com.ssrpro.library.dto.request.ReviewReq;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.dto.response.LibraryRes;
import com.ssrpro.library.dto.response.ReviewRes;
import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.service.LibraryService;
import com.ssrpro.library.service.ReadBookService;
import com.ssrpro.library.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/book/detail")
@RequiredArgsConstructor
public class BookDetailController {
    private final BookService bookService;
    private final ReviewService reviewService;
    private final LibraryService libraryService;
    private final ReadBookService readBookService;
    private final ObjectMapper objectMapper;

    /**
     * 검색 결과에서 책 클릭 시 — libraryCodes는 POST body로 받고, URL에는 bookId만 남김.
     */
    @PostMapping("/{bookId}")
    public String bookDetailFromSearch(
            @PathVariable Long bookId,
            @ModelAttribute("bookDetailReq") BookDetailReq req,
            RedirectAttributes redirectAttributes) {
        req.setBookId(bookId);
        redirectAttributes.addFlashAttribute("bookDetailReq", req);
        return "redirect:/book/detail/" + bookId;
    }

    /** 상세 — 책·도서관·리뷰 일괄 조회, 탭은 클라이언트 전환 */
    @GetMapping("/{bookId}")
    public String bookDetail(
            Model model,
            @PathVariable Long bookId,
            @ModelAttribute("bookDetailReq") BookDetailReq req,
            @AuthenticationPrincipal CustomUser customUser) {
        return renderBookDetail(model, bookId, req, customUser);
    }

    private String renderBookDetail(
            Model model,
            Long bookId,
            BookDetailReq req,
            CustomUser customUser) {
        Long memberId = customUser != null ? customUser.getMemberId() : null;
        req.setBookId(bookId);
        model.addAttribute("bookDetailReq", req);
        model.addAttribute("activeNav", "book-search");

        BookRes book = null;
        try {
            book = bookService.getBookDetail(bookId);
            model.addAttribute("book", book);
        } catch (Exception e) {
            model.addAttribute("bookError", e.getMessage());
        }

        List<ReviewRes> reviews = List.of();
        try {
            reviews = reviewService.findByBookId(bookId, memberId);
            model.addAttribute("reviews", reviews);
        } catch (Exception e) {
            model.addAttribute("reviewError", e.getMessage());
            model.addAttribute("reviews", List.of());
        }

        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("reviewAvg", computeReviewAvg(reviews, book));
        model.addAttribute("ratingBars", buildRatingBars(reviews));
        model.addAttribute("myReview", findMyReview(reviews, memberId));
        model.addAttribute("currentMemberId", memberId);
        model.addAttribute("isAdmin", isAdmin(customUser));
        model.addAttribute("reviewsEditJson", toReviewsEditJson(reviews));

        int readingState = 1;
        boolean readSoonSaved = false;
        if (memberId != null && book != null) {
            readingState = readBookService.readingState(memberId, bookId);
            readSoonSaved = readBookService.isReadSoon(memberId, bookId);
        }
        model.addAttribute("readingState", readingState);
        model.addAttribute("readSoonSaved", readSoonSaved);

        List<LibraryRes> libraries = List.of();
        String libraryApiWarning = null;
        String distanceHint = null;
        try {
            String isbn = book != null ? book.getBookIsbn() : null;
            LibraryService.BookDetailLibrariesResult libraryResult =
                    libraryService.findLibrariesForBookDetail(req, isbn, memberId);
            libraries = libraryResult.libraries();
            libraryApiWarning = libraryResult.libraryApiWarning();
            distanceHint = libraryResult.distanceHint();
            model.addAttribute("libraries", libraries);
        } catch (Exception e) {
            model.addAttribute("libraryError", e.getMessage());
            model.addAttribute("libraries", List.of());
        }
        model.addAttribute("libraryApiWarning", libraryApiWarning);
        model.addAttribute("distanceHint", distanceHint);
        model.addAttribute("libraryCount", libraries.size());
        model.addAttribute("librariesJson", toLibrariesJson(libraries));
        model.addAttribute("mainClass", "site-main--fluid");

        return "book/detail";
    }

    @PostMapping("/{bookId}/readbook")
    public String insertReadBook(
            @PathVariable Long bookId,
            @ModelAttribute("bookDetailReq") BookDetailReq bookDetailReq,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) {
            return loginRequiredRedirect();
        }

        Long memberId = customUser.getMemberId();

        try {
            int state = readBookService.readingState(memberId, bookId);
            if (state == 3) {
                redirectAttributes.addFlashAttribute("readBookError", "완독한 도서는 독서 기록을 변경할 수 없습니다.");
                return redirectToDetail(bookId, bookDetailReq, redirectAttributes, null);
            }
            boolean rst = readBookService.addToReading(memberId, bookId);
            if (!rst) {
                redirectAttributes.addFlashAttribute("readBookError", "처리 중 오류가 발생했습니다.");
                return redirectToDetail(bookId, bookDetailReq, redirectAttributes, null);
            }
            if (state == 2) {
                redirectAttributes.addFlashAttribute("success", "읽는 중 목록에서 제거했습니다.");
            } else {
                redirectAttributes.addFlashAttribute("success", "읽는 중 목록에 추가했습니다.");
            }
            return redirectToDetail(bookId, bookDetailReq, redirectAttributes, null);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("readBookError", e.getMessage());
            return redirectToDetail(bookId, bookDetailReq, redirectAttributes, null);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToDetail(bookId, bookDetailReq, redirectAttributes, null);
        }
    }

    @PostMapping("/{bookId}/readSoon")
    public String insertReadSoon(
            @PathVariable Long bookId,
            @ModelAttribute("bookDetailReq") BookDetailReq bookDetailReq,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) {
            return loginRequiredRedirect();
        }

        Long memberId = customUser.getMemberId();

        try {
            boolean wasSaved = readBookService.isReadSoon(memberId, bookId);
            boolean rst = readBookService.addToReadSoon(memberId, bookId);
            if (!rst) {
                redirectAttributes.addFlashAttribute("readBookError", "처리 중 오류가 발생했습니다.");
                return redirectToDetail(bookId, bookDetailReq, redirectAttributes, null);
            }
            if (wasSaved) {
                redirectAttributes.addFlashAttribute("success", "읽을 책 목록에서 제거했습니다.");
            } else {
                redirectAttributes.addFlashAttribute("success", "읽을 책 목록에 추가했습니다.");
            }
            return redirectToDetail(bookId, bookDetailReq, redirectAttributes, null);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToDetail(bookId, bookDetailReq, redirectAttributes, null);
        }
    }

    @PostMapping("/insert")
    public String insert(
            @ModelAttribute ReviewReq req,
            @ModelAttribute("bookDetailReq") BookDetailReq bookDetailReq,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes rttr) {
        if (customUser == null) {
            return loginRequiredRedirect();
        }

        if (req.getBookId() == null || req.getRating() < 1 || req.getRating() > 5) {
            rttr.addFlashAttribute("error", "별점을 선택해 주세요.");
            return redirectToDetail(req.getBookId(), bookDetailReq, rttr, "reviews");
        }
        if (req.getComment() == null || req.getComment().trim().length() < 10) {
            rttr.addFlashAttribute("error", "리뷰는 10자 이상 입력해 주세요.");
            return redirectToDetail(req.getBookId(), bookDetailReq, rttr, "reviews");
        }

        int result = reviewService.insert(req, customUser.getMemberId());

        if (result > 0) {
            rttr.addFlashAttribute("success", "리뷰가 등록되었습니다.");
        } else {
            rttr.addFlashAttribute("error", "이미 리뷰를 작성하셨습니다.");
        }

        return redirectToDetail(req.getBookId(), bookDetailReq, rttr, "reviews");
    }

    @PostMapping("/update")
    public String update(
            @ModelAttribute ReviewReq req,
            @ModelAttribute("bookDetailReq") BookDetailReq bookDetailReq,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes rttr) {
        if (customUser == null) {
            return loginRequiredRedirect();
        }

        if (req.getReviewId() == null || req.getRating() < 1 || req.getRating() > 5) {
            rttr.addFlashAttribute("error", "별점을 선택해 주세요.");
            return redirectToDetail(req.getBookId(), bookDetailReq, rttr, "reviews");
        }
        if (req.getComment() == null || req.getComment().trim().length() < 10) {
            rttr.addFlashAttribute("error", "리뷰는 10자 이상 입력해 주세요.");
            return redirectToDetail(req.getBookId(), bookDetailReq, rttr, "reviews");
        }

        boolean admin = isAdmin(customUser);
        int result = admin
                ? reviewService.adminUpdate(req)
                : reviewService.update(req, customUser.getMemberId());

        if (result > 0) {
            rttr.addFlashAttribute("success", "리뷰가 수정되었습니다.");
        } else {
            rttr.addFlashAttribute("error", "수정 권한이 없습니다.");
        }

        return redirectToDetail(req.getBookId(), bookDetailReq, rttr, "reviews");
    }

    @PostMapping("/delete/{reviewId}")
    public String delete(
            @PathVariable Long reviewId,
            @RequestParam Long bookId,
            @ModelAttribute("bookDetailReq") BookDetailReq bookDetailReq,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes rttr) {
        if (customUser == null) {
            return loginRequiredRedirect();
        }

        int result = reviewService.delete(reviewId, customUser.getMemberId());

        if (result > 0) {
            rttr.addFlashAttribute("success", "리뷰가 삭제되었습니다.");
        } else {
            rttr.addFlashAttribute("error", "삭제 권한이 없습니다.");
        }

        return redirectToDetail(bookId, bookDetailReq, rttr, "reviews");
    }

    @PostMapping("/admin/delete/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDelete(
            @PathVariable Long reviewId,
            @RequestParam Long bookId,
            @ModelAttribute("bookDetailReq") BookDetailReq bookDetailReq,
            RedirectAttributes rttr) {
        reviewService.adminDelete(reviewId);
        rttr.addFlashAttribute("success", "관리자 권한으로 삭제되었습니다.");
        return redirectToDetail(bookId, bookDetailReq, rttr, "reviews");
    }

    @PostMapping("/like/{reviewId}")
    public String toggleLike(
            @PathVariable Long reviewId,
            @RequestParam Long bookId,
            @ModelAttribute("bookDetailReq") BookDetailReq bookDetailReq,
            @AuthenticationPrincipal CustomUser customUser,
            RedirectAttributes rttr) {
        if (customUser == null) {
            return loginRequiredRedirect();
        }

        reviewService.toggleLike(reviewId, customUser.getMemberId());
        return redirectToDetail(bookId, bookDetailReq, rttr, "reviews");
    }

    private static boolean isAdmin(CustomUser customUser) {
        if (customUser == null) {
            return false;
        }
        return customUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private static double computeReviewAvg(List<ReviewRes> reviews, BookRes book) {
        if (!reviews.isEmpty()) {
            return reviews.stream()
                    .mapToDouble(ReviewRes::getReviewRating)
                    .average()
                    .orElse(0);
        }
        return book != null ? book.getBookRating() : 0;
    }

    private static Optional<ReviewRes> findMyReview(List<ReviewRes> reviews, Long memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        return reviews.stream()
                .filter(r -> memberId.equals(r.getMemberId()))
                .findFirst();
    }

    private List<Map<String, Object>> buildRatingBars(List<ReviewRes> reviews) {
        int total = reviews.size();
        int[] counts = new int[6];
        for (ReviewRes review : reviews) {
            int star = (int) Math.round(review.getReviewRating());
            if (star >= 1 && star <= 5) {
                counts[star]++;
            }
        }
        List<Map<String, Object>> bars = new ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            Map<String, Object> bar = new HashMap<>();
            bar.put("star", star);
            bar.put("count", counts[star]);
            bar.put("percent", total == 0 ? 0 : Math.round(counts[star] * 100.0 / total));
            bars.add(bar);
        }
        return bars;
    }

    private String toLibrariesJson(List<LibraryRes> libraries) {
        List<Map<String, Object>> payload = libraries.stream()
                .map(lib -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", lib.getLibraryName());
                    item.put("addr", lib.getLibraryAddr());
                    item.put("lat", lib.getLibraryLat());
                    item.put("lon", lib.getLibraryLon());
                    return item;
                })
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String toReviewsEditJson(List<ReviewRes> reviews) {
        List<Map<String, Object>> payload = reviews.stream()
                .map(review -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("reviewId", review.getReviewId());
                    item.put("rating", review.getReviewRating());
                    item.put("comment", review.getReviewComment());
                    return item;
                })
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private static void preserveBookDetailReq(BookDetailReq bookDetailReq, RedirectAttributes redirectAttributes) {
        if (bookDetailReq != null
                && bookDetailReq.getLibraryCodes() != null
                && !bookDetailReq.getLibraryCodes().isEmpty()) {
            redirectAttributes.addFlashAttribute("bookDetailReq", bookDetailReq);
        }
    }

    private static String redirectToDetail(
            Long bookId,
            BookDetailReq bookDetailReq,
            RedirectAttributes redirectAttributes,
            String detailTab) {
        preserveBookDetailReq(bookDetailReq, redirectAttributes);
        if (detailTab != null && !detailTab.isBlank()) {
            redirectAttributes.addFlashAttribute("detailTab", detailTab);
        }
        if (bookId == null) {
            return "redirect:/book/search";
        }
        return "redirect:/book/detail/" + bookId;
    }

    private static String loginRequiredRedirect() {
        return "redirect:/member/login?needLogin=true";
    }
}
