package com.example.onlyoffice.controller;

import com.example.onlyoffice.config.OnlyOfficeProperties;
import com.example.onlyoffice.service.DocumentKeyRegistry;
import com.example.onlyoffice.service.DocumentManager;
import com.example.onlyoffice.service.FileUtility;
import com.example.onlyoffice.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON API consumed by the Angular test client (list / upload / editor config).
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentApiController {

    private final StorageService storage;
    private final DocumentManager documentManager;
    private final DocumentKeyRegistry keyRegistry;
    private final FileUtility fileUtility;
    private final OnlyOfficeProperties onlyOffice;

    /** List document names in storage. */
    @GetMapping
    public List<String> list() {
        return storage.list();
    }

    /** Upload a document. */
    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank() || !fileUtility.isSupported(name)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unsupported or missing file: " + name));
        }
        storage.upload(name, file.getInputStream(), file.getSize(), fileUtility.getMimeType(name));
        return ResponseEntity.ok(Map.of("fileName", name));
    }

    /**
     * Editor config for a document. The browser uses {@code documentServerApiUrl} to
     * load api.js, then initialises {@code DocsAPI.DocEditor} with {@code config}.
     */
    @GetMapping("/{fileName}/config")
    public ResponseEntity<?> config(@PathVariable String fileName,
                                    @RequestParam(defaultValue = "true") boolean edit,
                                    @RequestParam(defaultValue = "1") String userId,
                                    @RequestParam(defaultValue = "Demo User") String userName) {
        if (!storage.exists(fileName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found: " + fileName));
        }
        Map<String, Object> config = documentManager.buildConfig(
                fileName, keyRegistry.currentKey(fileName), userId, userName, edit);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("documentServerApiUrl",
                onlyOffice.getDocserverUrl() + "/web-apps/apps/api/documents/api.js");
        body.put("config", config);
        return ResponseEntity.ok(body);
    }
}
