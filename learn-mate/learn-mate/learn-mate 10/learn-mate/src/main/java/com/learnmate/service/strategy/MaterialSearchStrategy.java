package com.learnmate.service.strategy;


import com.learnmate.model.Material;

import java.util.List;

public interface MaterialSearchStrategy {
    List<Material> search(List<Material> materials, String keyword);
}
