package com.example.onlyoffice.controller;

import com.example.onlyoffice.config.OnlyOfficeProperties;
import com.example.onlyoffice.model.CallbackRequest;
import com.example.onlyoffice.service.DocumentKeyRegistry;
import com.example.onlyoffice.service.FileUtility;
import com.example.onlyoffice.service.JwtService;
import com.example.onlyoffice.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Receives status callbacks from the Document Server. On save (status 2 / 6) it
 * downloads the edited document and writes it back to storage.
 */
@Slf4j
@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class CallbackController {

    private static final int STATUS_MUST_SAVE = 2;
    private static final int STATUS_FORCE_SAVE = 6;

    private final StorageService storage;
    private final JwtService jwtService;
    private final FileUtility fileUtility;
    private final DocumentKeyRegistry keyRegistry;
    private final OnlyOfficeProperties onlyOffice;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newHttpClient();

    @PostMapping("/{fileName}")
    public ResponseEntity<Map<String, Object>> callback(@PathVariable String fileName,
                                                         @RequestBody CallbackRequest body,
                                                         HttpServletRequest request) {
        try {
            CallbackRequest verified = verify(body, request);
            log.debug("Callback for '{}' status={}", fileName, verified.getStatus());

            if (verified.getStatus() == STATUS_MUST_SAVE || verified.getStatus() == STATUS_FORCE_SAVE) {
                saveEditedDocument(fileName, verified.getUrl());
            }
            // OnlyOffice expects {"error":0} on success.
            return ResponseEntity.ok(Map.of("error", 0));
        } catch (SecurityException se) {
            log.warn("Rejected callback for '{}': {}", fileName, se.getMessage());
            return ResponseEntity.ok(Map.of("error", 1, "message", se.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process callback for '{}'", fileName, e);
            return ResponseEntity.ok(Map.of("error", 1, "message", e.getMessage()));
        }
    }

    /**
     * Validates the JWT. OnlyOffice may send it either in the request body ("token")
     * or in the configured header as "Bearer &lt;jwt&gt;". When present, the body-in-token
     * "payload" claim is the authoritative request data.
     */
    @SuppressWarnings("unchecked")
    private CallbackRequest verify(CallbackRequest body, HttpServletRequest request) {
        if (!jwtService.enabled()) {
            return body;
        }
        String token = body.getToken();
        if (token == null) {
            String header = request.getHeader(onlyOffice.getJwtHeader());
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }
        }
        if (token == null) {
            throw new SecurityException("Missing JWT on callback");
        }
        Claims claims = jwtService.verify(token);
        // When the token is header-based, claims ARE the callback payload;
        // when body-based, the payload lives under the "payload" claim.
        Map<String, Object> payload = claims.get("payload", Map.class);
        Map<String, Object> data = payload != null ? payload : claims;
        return objectMapper.convertValue(data, CallbackRequest.class);
    }

    private void saveEditedDocument(String fileName, String downloadUrl) throws Exception {
        HttpResponse<InputStream> resp = http.send(
                HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Download from Document Server failed: HTTP " + resp.statusCode());
        }
        long len = resp.headers().firstValueAsLong("content-length").orElse(-1);
        try (InputStream in = resp.body()) {
            byte[] bytes = in.readAllBytes();
            storage.upload(fileName, new java.io.ByteArrayInputStream(bytes), bytes.length,
                    fileUtility.getMimeType(fileName));
        }
        keyRegistry.nextKey(fileName);
        log.info("Saved edited document '{}' ({} bytes)", fileName, len);
    }
}
