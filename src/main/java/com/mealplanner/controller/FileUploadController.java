package com.mealplanner.controller;

import com.mealplanner.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new RuntimeException("文件为空");

        Path dirPath = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(dirPath);

        String ext = Optional.ofNullable(StringUtils.getFilenameExtension(file.getOriginalFilename())).orElse("jpg");
        String filename = UUID.randomUUID() + "." + ext;
        file.transferTo(dirPath.resolve(filename));

        return ApiResponse.success(Map.of("url", "/uploads/" + filename));
    }
}
