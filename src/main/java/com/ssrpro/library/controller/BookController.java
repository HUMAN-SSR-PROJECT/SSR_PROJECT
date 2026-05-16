package com.ssrpro.library.controller;


import org.springframework.ui.Model;
import com.ssrpro.library.dto.request.BookSearchReq;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.support.RegionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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
    public String executeSearch(@ModelAttribute("searchReq") BookSearchReq req, Model model) {
        if (req.getCity() <= 0) {
            req.setCity(11);
        }

        List<BookRes> searchResults = bookService.searchBooks(req);

        model.addAttribute("searchResults", searchResults);
        model.addAttribute("count", searchResults.size());
        model.addAttribute("isEmpty", searchResults.isEmpty());
        model.addAttribute("keyword", req.getKeyword());
        model.addAttribute("searched", true);
        model.addAttribute("searchReq", req);
        model.addAttribute("activeNav", "book-search");
        model.addAttribute("mainClass", "site-main--fluid");
        model.addAttribute("locationLabel", RegionCatalog.locationLabel(req.getCity(), req.getDistrict()));
        addRegionAttributes(model, req);

        return "book/search";
    }

    private void addRegionAttributes(Model model, BookSearchReq req) {
        model.addAttribute("cities", RegionCatalog.cities());
        model.addAttribute("districts", RegionCatalog.districts(req.getCity()));
        model.addAttribute("districtsCatalogJson", RegionCatalog.districtsCatalogJson());
        model.addAttribute("cityName", RegionCatalog.cityName(req.getCity()));
        model.addAttribute("districtName", RegionCatalog.districtName(req.getCity(), req.getDistrict()));
    }
}
