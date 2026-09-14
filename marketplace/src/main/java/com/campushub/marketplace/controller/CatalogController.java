package com.campushub.marketplace.controller;

import com.campushub.marketplace.service.CatalogService;
import com.campushub.marketplace.vo.MarketplaceCatalogView;
import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplace/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<ResponseResult<MarketplaceCatalogView>> getCatalog(HttpServletRequest request) {
        String requestId = RequestUtils.getOrCreateRequestId(request);

        MarketplaceCatalogView catalog =  catalogService.getCatalog();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(catalog, requestId));
    }
}
