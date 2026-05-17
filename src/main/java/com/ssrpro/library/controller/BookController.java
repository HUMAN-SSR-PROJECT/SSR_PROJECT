package com.ssrpro.library.controller;


import org.springframework.ui.Model;
import com.ssrpro.library.dto.request.BookSearchReq;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.dto.response.PageResult;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.support.RegionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/search")
    public String searchPage(Model model) {
        BookSearchReq searchReq = new BookSearchReq();
        searchReq.setCity(11);
        searchReq.setDistrict(0);

        model.addAttribute("searchReq", searchReq);
        model.addAttribute("searched", false);
        model.addAttribute("activeNav", "book-search");
        model.addAttribute("mainClass", "site-main--fluid");
        addRegionAttributes(model, searchReq);
        return "book/search";
    }

    @GetMapping("/search/execute")
    public String executeSearch(
            @ModelAttribute("searchReq") BookSearchReq req,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        if (req.getCity() <= 0) {
            req.setCity(11);
        }

        String keyword = req.getKeyword() == null ? "" : req.getKeyword().trim();
        if (keyword.isEmpty()) {
            model.addAttribute("searchResults", java.util.List.<BookRes>of());
            model.addAttribute("count", 0);
            model.addAttribute("countAtLeast", false);
            model.addAttribute("isEmpty", true);
            model.addAttribute("keyword", "");
            model.addAttribute("searched", true);
            model.addAttribute("searchReq", req);
            model.addAttribute("pageResult", PageResult.empty(1, PageResult.DEFAULT_SIZE));
            addCommonSearchModel(model, req);
            return "book/search";
        }

        int safePage = Math.max(page, 1);
        req.setPage(safePage);

        BookService.SearchPageResult searchPage = bookService.searchBooksPaged(req, safePage);
        PageResult<BookRes> pageResult = searchPage.page();
        if (pageResult.getTotalPages() > 0 && safePage > pageResult.getTotalPages()) {
            safePage = pageResult.getTotalPages();
            req.setPage(safePage);
            searchPage = bookService.searchBooksPaged(req, safePage);
            pageResult = searchPage.page();
        }

        boolean isEmpty = pageResult.getContent().isEmpty();
        model.addAttribute("searchResults", pageResult.getContent());
        model.addAttribute("count", pageResult.getTotalItems());
        model.addAttribute("countAtLeast", pageResult.hasNext());
        model.addAttribute("libraryApiWarning", searchPage.libraryApiWarning());
        model.addAttribute("isEmpty", isEmpty);
        model.addAttribute("keyword", keyword);
        model.addAttribute("searched", true);
        model.addAttribute("searchReq", req);
        model.addAttribute("pageResult", pageResult);
        addCommonSearchModel(model, req);

        return "book/search";
    }

    private void addCommonSearchModel(Model model, BookSearchReq req) {
        model.addAttribute("activeNav", "book-search");
        model.addAttribute("mainClass", "site-main--fluid");
        model.addAttribute("locationLabel", RegionCatalog.locationLabel(req.getCity(), req.getDistrict()));
        addRegionAttributes(model, req);
    }

    private void addRegionAttributes(Model model, BookSearchReq req) {
        model.addAttribute("cities", RegionCatalog.cities());
        model.addAttribute("districts", RegionCatalog.districts(req.getCity()));
        model.addAttribute("districtsCatalogJson", RegionCatalog.districtsCatalogJson());
        model.addAttribute("cityName", RegionCatalog.cityName(req.getCity()));
        model.addAttribute("districtName", RegionCatalog.districtName(req.getCity(), req.getDistrict()));
    }
}
