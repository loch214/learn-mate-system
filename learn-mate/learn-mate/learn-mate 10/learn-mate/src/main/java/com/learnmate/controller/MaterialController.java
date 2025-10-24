package com.learnmate.controller;

import com.learnmate.model.Material;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import com.learnmate.service.MaterialService;
import com.learnmate.service.NotificationService;
import com.learnmate.service.SchoolClassService;
import com.learnmate.service.SubjectService;
import com.learnmate.service.UserService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/materials")
public class MaterialController extends BaseController {
    private final MaterialService materialService;
    private final SubjectService subjectService;
    private final SchoolClassService schoolClassService;
    private final UserService userService;
    private final NotificationService notificationService;

    public MaterialController(MaterialService materialService, SubjectService subjectService,
                             SchoolClassService schoolClassService, UserService userService,
                             NotificationService notificationService) {
        this.materialService = materialService;
        this.subjectService = subjectService;
        this.schoolClassService = schoolClassService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'ADMIN')")
    public String listMaterials(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("=== Materials Controller Called ===");
            User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            System.out.println("User: " + user.getUsername() + ", Role: " + user.getRole());
            
            List<Material> materials;
            if (user.getRole().name().equals("STUDENT")) {
                // Students see materials for their class AND subjects they're enrolled in
                if (user.getSchoolClass() != null && user.getSubjects() != null && !user.getSubjects().isEmpty()) {
                    System.out.println("Student class: " + user.getSchoolClass().getName());
                    System.out.println("Student subjects: " + user.getSubjects().stream().map(Subject::getName).toList());
                    try {
                        materials = materialService.getMaterialsByClassAndSubjects(user.getSchoolClass(), user.getSubjects());
                    } catch (Exception e) {
                        System.out.println("Error filtering by subjects, falling back to class only: " + e.getMessage());
                        materials = materialService.getMaterialsByClass(user.getSchoolClass());
                    }
                } else {
                    // If student has no class or subjects assigned, show empty list with message
                    System.out.println("Student has no class or subjects assigned");
                    materials = java.util.Collections.emptyList();
                    if (user.getSchoolClass() == null) {
                        model.addAttribute("error", "No class assigned. Please contact your administrator.");
                    } else {
                        model.addAttribute("error", "No subjects enrolled. Please contact your administrator.");
                    }
                }
            } else {
                // Teachers and admins see all materials
                System.out.println("Loading all materials for teacher/admin");
                materials = materialService.getAllMaterials();
            }
            
            System.out.println("Materials found: " + (materials != null ? materials.size() : "null"));
            System.out.println("Subjects count: " + subjectService.getAllSubjects().size());
            System.out.println("Classes count: " + schoolClassService.getAllSchoolClasses().size());
            
            model.addAttribute("materials", materials);
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            model.addAttribute("user", user); // Add user to model for template access
            return "materials/list";
        } catch (Exception e) {
            System.out.println("Error in materials controller: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading materials: " + e.getMessage());
            model.addAttribute("materials", java.util.Collections.emptyList());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            model.addAttribute("user", userService.getUserByUsername(userDetails.getUsername()).orElse(new User()));
            return "materials/list";
        }
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public String createMaterialForm(Model model) {
        model.addAttribute("material", new Material());
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "materials/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public String createMaterial(@ModelAttribute Material material, 
                                BindingResult result,
                                @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model, RedirectAttributes redirectAttributes) {
        
        // Manual validation
        if (material.getTitle() == null || material.getTitle().trim().isEmpty()) {
            result.rejectValue("title", "error.title", "Title is required");
        }
        if (material.getSubject() == null) {
            result.rejectValue("subject", "error.subject", "Subject is required");
        }
        if (material.getSchoolClass() == null) {
            result.rejectValue("schoolClass", "error.schoolClass", "Class is required");
        }
        if (file == null || file.isEmpty()) {
            result.rejectValue("file", "error.file", "File is required");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "materials/create";
        }

        try {
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            material.setTeacher(teacher);
            Material savedMaterial = materialService.saveMaterial(material, file);
            notificationService.notifyMaterialUploaded(savedMaterial);
            
            redirectAttributes.addFlashAttribute("success", "Material uploaded successfully!");
            return "redirect:/materials/list";
        } catch (IOException e) {
            model.addAttribute("error", "Failed to upload file: " + e.getMessage());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "materials/create";
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String editMaterialForm(@PathVariable Long id, Model model) {
        Material material = materialService.getMaterialById(id).orElseThrow();
        model.addAttribute("material", material);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "materials/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String updateMaterial(@PathVariable Long id, 
                                @ModelAttribute Material material, 
                                BindingResult result,
                                @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
                                Model model, RedirectAttributes redirectAttributes) {
        
        // Manual validation
        if (material.getTitle() == null || material.getTitle().trim().isEmpty()) {
            result.rejectValue("title", "error.title", "Title is required");
        }
        if (material.getSubject() == null) {
            result.rejectValue("subject", "error.subject", "Subject is required");
        }
        if (material.getSchoolClass() == null) {
            result.rejectValue("schoolClass", "error.schoolClass", "Class is required");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "materials/edit";
        }

        try {
            Material existingMaterial = materialService.getMaterialById(id).orElseThrow();
            existingMaterial.setTitle(material.getTitle());
            existingMaterial.setDescription(material.getDescription());
            existingMaterial.setSubject(material.getSubject());
            existingMaterial.setSchoolClass(material.getSchoolClass());
            
            if (file != null && !file.isEmpty()) {
                materialService.saveMaterial(existingMaterial, file);
            } else {
                materialService.saveMaterial(existingMaterial, null);
            }
            
            redirectAttributes.addFlashAttribute("success", "Material updated successfully!");
            return "redirect:/materials/list";
        } catch (IOException e) {
            model.addAttribute("error", "Failed to update file: " + e.getMessage());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "materials/edit";
        }
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String deleteMaterial(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        materialService.deleteMaterial(id);
        redirectAttributes.addFlashAttribute("success", "Material deleted successfully!");
        return "redirect:/materials/list";
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'ADMIN')")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long id) {
        try {
            Material material = materialService.getMaterialById(id).orElseThrow();
            Path filePath = Paths.get(material.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(material.getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + material.getOriginalFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // === Strategy Pattern Snippet 5: Controller entry point ===
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'ADMIN')")
    public String searchMaterials(@RequestParam(required = false) String title,
                                 @RequestParam(required = false) Long subjectId,
                                 @RequestParam(required = false) Long classId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        try {
            System.out.println("=== Search Materials Called ===");
            User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            System.out.println("User: " + user.getUsername() + ", Role: " + user.getRole());
            
            Subject subject = subjectId != null ? subjectService.getSubjectById(subjectId).orElse(null) : null;
            SchoolClass schoolClass = classId != null ? schoolClassService.getSchoolClassById(classId).orElse(null) : null;
            
            List<Material> materials;
            if (user.getRole().name().equals("STUDENT")) {
                // Students see materials for their class AND subjects they're enrolled in
                if (user.getSchoolClass() != null && user.getSubjects() != null && !user.getSubjects().isEmpty()) {
                    System.out.println("Student class: " + user.getSchoolClass().getName());
                    System.out.println("Student subjects: " + user.getSubjects().stream().map(Subject::getName).toList());
                    try {
                        materials = materialService.getMaterialsByClassAndSubjects(user.getSchoolClass(), user.getSubjects());
                    } catch (Exception e) {
                        System.out.println("Error filtering by subjects, falling back to class only: " + e.getMessage());
                        materials = materialService.getMaterialsByClass(user.getSchoolClass());
                    }
                } else {
                    // If student has no class or subjects assigned, show empty list with message
                    System.out.println("Student has no class or subjects assigned");
                    materials = java.util.Collections.emptyList();
                    if (user.getSchoolClass() == null) {
                        model.addAttribute("error", "No class assigned. Please contact your administrator.");
                    } else {
                        model.addAttribute("error", "No subjects enrolled. Please contact your administrator.");
                    }
                }
                
                // Apply search filters to student materials using strategy-based service logic
                if (title != null && !title.trim().isEmpty()) {
                    materials = materialService.filterMaterials(materials, title, "title");
                }
                if (subject != null) {
                    String subjectKeyword = subject.getId() != null
                            ? subject.getId().toString()
                            : subject.getName();
                    materials = materialService.filterMaterials(materials, subjectKeyword, "subject");
                }

                if (schoolClass != null) {
                    materials = materials.stream()
                            .filter(m -> m.getSchoolClass() != null && m.getSchoolClass().equals(schoolClass))
                            .toList();
                }
            } else {
                // Teachers and admins see all materials with search filters
                System.out.println("Loading all materials for teacher/admin with search filters");
                materials = materialService.getAllMaterials();

                if (title != null && !title.trim().isEmpty()) {
                    materials = materialService.filterMaterials(materials, title, "title");
                }

                if (subject != null) {
                    String subjectKeyword = subject.getId() != null
                            ? subject.getId().toString()
                            : subject.getName();
                    materials = materialService.filterMaterials(materials, subjectKeyword, "subject");
                }

                if (schoolClass != null) {
                    materials = materials.stream()
                            .filter(m -> m.getSchoolClass() != null && m.getSchoolClass().equals(schoolClass))
                            .toList();
                }
            }
            
            System.out.println("Materials found: " + (materials != null ? materials.size() : "null"));
            
            model.addAttribute("materials", materials);
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            model.addAttribute("searchTitle", title);
            model.addAttribute("searchSubjectId", subjectId);
            model.addAttribute("searchClassId", classId);
            model.addAttribute("user", user); // Add user to model for template access
            
            return "materials/list";
        } catch (Exception e) {
            System.out.println("Error in search materials controller: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error searching materials: " + e.getMessage());
            model.addAttribute("materials", java.util.Collections.emptyList());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            model.addAttribute("user", userService.getUserByUsername(userDetails.getUsername()).orElse(new User()));
            return "materials/list";
        }
    }
}


