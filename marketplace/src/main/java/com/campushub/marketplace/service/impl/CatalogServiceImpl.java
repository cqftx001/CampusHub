package com.campushub.marketplace.service.impl;

import com.campushub.marketplace.domain.Brand;
import com.campushub.marketplace.domain.Category;
import com.campushub.marketplace.mapper.CatalogMapper;
import com.campushub.marketplace.repository.BrandRepository;
import com.campushub.marketplace.repository.CategoryRepository;
import com.campushub.marketplace.service.CatalogService;
import com.campushub.marketplace.vo.MarketplaceCatalogView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CatalogMapper catalogMapper;

    public CatalogServiceImpl(CategoryRepository categoryRepository, BrandRepository brandRepository, CatalogMapper catalogMapper) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.catalogMapper = catalogMapper;
    }

    @Override
    public MarketplaceCatalogView getCatalog() {
        List<Category> categories = categoryRepository.findAvailableCatalogCategories();

        List<Brand> brands = brandRepository.findAllByActiveTrueOrderByDisplayOrderAscDisplayNameAsc();

        return catalogMapper.toView(categories, brands);
    }
}
