package com.learnmate.service.strategy;

import com.learnmate.model.Material;

import java.util.List;

public class SearchMaterialByTitleStrategy implements MaterialSearchStrategy {

    @Override
    public List<Material> search(List<Material> materials, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return materials;
        }

        String lowerKeyword = keyword.toLowerCase();
        return materials.stream()
                .filter(material -> material.getTitle() != null
                        && material.getTitle().toLowerCase().contains(lowerKeyword))
                .toList();
    }
}
