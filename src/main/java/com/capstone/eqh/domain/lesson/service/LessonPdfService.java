package com.capstone.eqh.domain.lesson.service;

import com.capstone.eqh.domain.lesson.dto.response.LessonPdfResponseDto;
import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.entity.LessonMaterial;
import com.capstone.eqh.domain.lesson.repository.LessonMaterialRepository;
import com.capstone.eqh.domain.lesson.repository.LessonRepository;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import com.capstone.eqh.global.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonPdfService {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    private static final String PDF_EXTENSION = ".pdf";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final LessonMaterialRepository lessonMaterialRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final SupabaseStorageService supabaseStorageService;

    @Transactional
    public LessonPdfResponseDto uploadPdf(Long lessonId, MultipartFile file, Long userId, String title) {
        Lesson lesson = findLessonById(lessonId);
        User uploader = findUserById(userId);
        validatePdfFile(file);

        String originalFileName = sanitizeOriginalFileName(file.getOriginalFilename());
        String savedFileName = UUID.randomUUID() + PDF_EXTENSION;
        String storagePath = supabaseStorageService.buildStoragePath(lessonId, savedFileName);

        byte[] content = readFileBytes(file);
        supabaseStorageService.upload(storagePath, content, PDF_CONTENT_TYPE);
        String fileUrl = supabaseStorageService.getPublicUrl(storagePath);

        LessonMaterial material = LessonMaterial.builder()
                .lesson(lesson)
                .title(title != null && !title.isBlank() ? title : originalFileName)
                .fileUrl(fileUrl)
                .createdBy(uploader)
                .build();
        return LessonPdfResponseDto.from(lessonMaterialRepository.save(material));
    }

    @Transactional
    public void deletePdf(Long pdfId) {
        LessonMaterial material = findPdfById(pdfId);
        lessonMaterialRepository.delete(material);
    }

    public List<LessonPdfResponseDto> getPdfList(Long lessonId) {
        findLessonById(lessonId);
        return lessonMaterialRepository.findAllByLessonId(lessonId)
                .stream()
                .map(LessonPdfResponseDto::from)
                .toList();
    }

    public boolean isUploader(Long pdfId, Long userId) {
        return lessonMaterialRepository.findById(pdfId)
                .map(m -> m.getCreatedBy() != null && m.getCreatedBy().getId().equals(userId))
                .orElse(false);
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PDF_TYPE);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !hasPdfExtension(originalFileName)) {
            throw new CustomException(ErrorCode.INVALID_PDF_TYPE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            throw new CustomException(ErrorCode.INVALID_PDF_TYPE);
        }
    }

    private boolean hasPdfExtension(String fileName) {
        String normalized = sanitizeOriginalFileName(fileName).toLowerCase(Locale.ROOT);
        return normalized.endsWith(PDF_EXTENSION);
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PDF_TYPE);
        }
        String fileName = Paths.get(originalFileName).getFileName().toString();
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new CustomException(ErrorCode.INVALID_PDF_TYPE);
        }
        return fileName;
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }
    }

    private Lesson findLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException(ErrorCode.LESSON_NOT_FOUND));
    }

    private LessonMaterial findPdfById(Long pdfId) {
        return lessonMaterialRepository.findById(pdfId)
                .orElseThrow(() -> new CustomException(ErrorCode.PDF_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}