package com.example.onlyoffice.controller;

import com.example.onlyoffice.config.OnlyOfficeProperties;
import com.example.onlyoffice.service.DocumentKeyRegistry;
import com.example.onlyoffice.service.DocumentManager;
import com.example.onlyoffice.service.FileUtility;
import com.example.onlyoffice.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DocumentController {

    private final StorageService storage;
    private final DocumentManager documentManager;
    private final DocumentKeyRegistry keyRegistry;
    private final FileUtility fileUtility;
    private final OnlyOfficeProperties onlyOffice;
    private final ObjectMapper objectMapper;

    /** Home page: list of documents in the bucket. */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("files", storage.list());
        return "index";
    }

    /** Upload a document into storage. */
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes ra) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank() || !fileUtility.isSupported(name)) {
            ra.addFlashAttribute("error", "Unsupported or missing file: " + name);
            return "redirect:/";
        }
        storage.upload(name, file.getInputStream(), file.getSize(), fileUtility.getMimeType(name));
        ra.addFlashAttribute("message", "Uploaded " + name);
        return "redirect:/";
    }

    /** Editor page for a document. */
    @GetMapping("/editor")
    public String editor(@RequestParam String fileName,
                         @RequestParam(defaultValue = "true") boolean edit,
                         Model model) throws IOException {
        if (!storage.exists(fileName)) {
            model.addAttribute("error", "File not found: " + fileName);
            return "index";
        }
        var config = documentManager.buildConfig(
                fileName, keyRegistry.currentKey(fileName), "1", "Demo User", edit);

        model.addAttribute("fileName", fileName);
        model.addAttribute("docserverUrl", onlyOffice.getDocserverUrl());
        model.addAttribute("configJson", objectMapper.writeValueAsString(config));
        return "editor";
    }

    /**
     * Raw file download endpoint. The Document Server fetches the original document
     * from here (document.url in the editor config).
     */
    @GetMapping("/api/files/{fileName}/download")
    @ResponseBody
    public ResponseEntity<InputStreamResource> download(@org.springframework.web.bind.annotation.PathVariable String fileName) {
        ResponseInputStream<GetObjectResponse> stream = storage.download(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(fileUtility.getMimeType(fileName)))
                .body(new InputStreamResource(stream));
    }
}
