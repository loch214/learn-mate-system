package com.learnmate.service;

import com.learnmate.model.Material;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import com.learnmate.repository.MaterialRepository;
import com.learnmate.service.strategy.MaterialSearchContext;
import com.learnmate.service.strategy.MaterialSearchStrategy;
import com.learnmate.service.strategy.SearchMaterialBySubjectStrategy;
import com.learnmate.service.strategy.SearchMaterialByTitleStrategy;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final FileStorageService fileStorageService;

    public MaterialService(MaterialRepository materialRepository, FileStorageService fileStorageService) {
        this.materialRepository = materialRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Material> getAllMaterials() {
        return materialRepository.findByActiveTrueOrderByUploadedAtDesc();
    }

    public List<Material> getMaterialsByTeacher(User teacher) {
        return materialRepository.findByTeacherAndActiveTrue(teacher);
    }

    public List<Material> getMaterialsBySubjectAndClass(Subject subject, SchoolClass schoolClass) {
        return materialRepository.findBySubjectAndSchoolClassAndActiveTrue(subject, schoolClass);
    }

    public List<Material> getMaterialsBySubject(Subject subject) {
        return materialRepository.findBySubjectAndActiveTrue(subject);
    }

    public List<Material> getMaterialsByClass(SchoolClass schoolClass) {
        return materialRepository.findBySchoolClassAndActiveTrue(schoolClass);
    }

    public List<Material> getMaterialsByClassAndSubjects(SchoolClass schoolClass, Set<Subject> subjects) {
        return materialRepository.findBySchoolClassAndSubjectInAndActiveTrue(schoolClass, subjects);
    }

    public Optional<Material> getMaterialById(Long id) {
        return materialRepository.findById(id);
    }

    public Material saveMaterial(Material material, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file, FileStorageService.FileType.MATERIAL);
            material.setFileName(fileName);
            material.setOriginalFileName(file.getOriginalFilename());
            material.setFileType(file.getContentType());
            material.setFileSize(file.getSize());
            material.setFilePath("uploads/materials/" + fileName);
        }
        return materialRepository.save(material);
    }

    public void deleteMaterial(Long id) {
        Optional<Material> material = materialRepository.findById(id);
        if (material.isPresent()) {
            material.get().setActive(false);
            materialRepository.save(material.get());
        }
    }

    
    public List<Material> searchMaterials(String keyword, String searchType) {
        List<Material> allMaterials = materialRepository.findByActiveTrueOrderByUploadedAtDesc();
        return applyStrategy(allMaterials, keyword, searchType);
    }

    
    public List<Material> filterMaterials(List<Material> materials, String keyword, String searchType) {
        return applyStrategy(materials, keyword, searchType);
    }

    private List<Material> applyStrategy(List<Material> materials, String keyword, String searchType) {
        MaterialSearchStrategy strategy = resolveStrategy(searchType);
        MaterialSearchContext context = new MaterialSearchContext();
        context.setStrategy(strategy);
        return context.executeSearch(materials, keyword);
    }

    private MaterialSearchStrategy resolveStrategy(String searchType) {
        if ("subject".equalsIgnoreCase(searchType)) {
            return new SearchMaterialBySubjectStrategy();
        }
        return new SearchMaterialByTitleStrategy();
    }
}


