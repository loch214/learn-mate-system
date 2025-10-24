package com.learnmate.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir.exams}")
    private String examsUploadDir;

    @Value("${file.upload-dir.timetables}")
    private String timetablesUploadDir;

    @Value("${file.upload-dir.materials}")
    private String materialsUploadDir;

    @Value("${file.upload-dir.answer-sheets}")
    private String answerSheetsUploadDir;

    @Value("${file.upload-dir.payment-slips:./uploads/payment-slips}")
    private String paymentSlipsUploadDir;

    @Value("${file.upload-dir.notifications:./uploads/notifications}")
    private String notificationsUploadDir;

    public enum FileType {
        EXAM, TIMETABLE, MATERIAL, ANSWER_SHEET, PAYMENT_SLIP, NOTIFICATION
    }

    private Path getFileStorageLocation(FileType fileType) {
        String uploadDir = switch (fileType) {
            case EXAM -> examsUploadDir;
            case TIMETABLE -> timetablesUploadDir;
            case MATERIAL -> materialsUploadDir;
            case ANSWER_SHEET -> answerSheetsUploadDir;
            case PAYMENT_SLIP -> paymentSlipsUploadDir;
            case NOTIFICATION -> notificationsUploadDir;
        };
        
        Path fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
        
        return fileStorageLocation;
    }

    public String storeFile(MultipartFile file) {
        return storeFile(file, FileType.EXAM); // Default to EXAM for backward compatibility
    }

    public String storeFile(MultipartFile file, FileType fileType) {
        // Check if file is empty or has no name
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new RuntimeException("Failed to store empty file or file with no name");
        }

        // Normalize file name (we already checked for null above)
        String originalFileName = file.getOriginalFilename();
        String fileName = StringUtils.cleanPath(originalFileName == null ? "unknown" : originalFileName);

        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Generate unique filename to avoid conflicts
            String fileExtension = "";
            if (fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Get the appropriate storage location for this file type
            Path fileStorageLocation = getFileStorageLocation(fileType);
            
            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Path getFileStorageLocationPublic(FileType fileType) {
        return getFileStorageLocation(fileType);
    }

    public boolean deleteFile(String fileName, FileType fileType) {
        try {
            Path fileStorageLocation = getFileStorageLocation(fileType);
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + fileName, ex);
        }
    }

    // Backward compatibility method
    public boolean deleteFile(String fileName) {
        return deleteFile(fileName, FileType.EXAM);
    }
}
