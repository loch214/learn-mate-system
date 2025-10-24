package com.learnmate.service.strategy;

import com.learnmate.model.Material;

import java.util.List;

public class MaterialSearchContext {
    private MaterialSearchStrategy strategy;

    public void setStrategy(MaterialSearchStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Material> executeSearch(List<Material> materials, String keyword) {
        if (strategy == null) {
            return materials;
        }
        return strategy.search(materials, keyword);
    }
}
