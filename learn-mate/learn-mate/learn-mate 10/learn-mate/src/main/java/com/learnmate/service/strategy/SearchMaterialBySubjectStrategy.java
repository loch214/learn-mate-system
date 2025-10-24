package com.learnmate.service.strategy;


import com.learnmate.model.Material;

import java.util.List;

public class SearchMaterialBySubjectStrategy implements MaterialSearchStrategy {

    @Override
    public List<Material> search(List<Material> materials, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return materials;
        }

        String lowerKeyword = keyword.toLowerCase();

        return materials.stream()
                .filter(material -> {
                    if (material.getSubject() == null) {
                        return false;
                    }

                    String subjectName = material.getSubject().getName();
                    String subjectId = material.getSubject().getId() != null
                            ? material.getSubject().getId().toString()
                            : null;

                    boolean matchesName = subjectName != null
                            && subjectName.toLowerCase().contains(lowerKeyword);

                    boolean matchesId = subjectId != null
                            && subjectId.equalsIgnoreCase(keyword);

                    return matchesName || matchesId;
                })
                .toList();
    }
}
