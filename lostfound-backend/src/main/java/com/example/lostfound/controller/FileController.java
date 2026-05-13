package com.example.lostfound.controller;

import com.example.lostfound.auth.LoginRequired;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf", ".doc", ".docx", ".txt");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final String uploadDir;

    public FileController(@Value("${lostfound.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @LoginRequired
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        String extension = validate(file);

        String dateFolder = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = Paths.get(uploadDir, dateFolder, fileName).toAbsolutePath().normalize();

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException(500, "File upload failed");
        }

        String publicPath = "/uploads/" + dateFolder + "/" + fileName;
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

        Map<String, String> result = new HashMap<>();
        result.put("url", baseUrl + publicPath);
        result.put("path", publicPath);
        result.put("name", file.getOriginalFilename() == null ? fileName : file.getOriginalFilename());
        result.put("isImage", String.valueOf(IMAGE_EXTENSIONS.contains(extension)));
        return ApiResponse.success(result);
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "File size must be <= 10MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "Only jpg, png, gif, webp, pdf, doc, docx and txt files are supported");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            return extension;
        }

        boolean imageType = contentType.startsWith("image/") && IMAGE_EXTENSIONS.contains(extension);
        boolean pdfType = "application/pdf".equals(contentType) && ".pdf".equals(extension);
        boolean wordType = (
                "application/msword".equals(contentType)
                        || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)
                        || "application/octet-stream".equals(contentType)
        ) && (".doc".equals(extension) || ".docx".equals(extension));
        boolean textType = contentType.startsWith("text/") && ".txt".equals(extension);

        if (!imageType && !pdfType && !wordType && !textType) {
            throw new BusinessException(400, "File type does not match file extension");
        }
        return extension;
    }

    private String getExtension(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return ".txt";
        }
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            return ".txt";
        }
        String extension = originalName.substring(dotIndex).toLowerCase();
        return extension.length() > 10 ? ".txt" : extension;
    }
}
