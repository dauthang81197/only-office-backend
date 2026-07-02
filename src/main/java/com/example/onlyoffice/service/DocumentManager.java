package com.example.onlyoffice.service;

import com.example.onlyoffice.config.AppProperties;
import com.example.onlyoffice.config.OnlyOfficeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the JSON config the OnlyOffice editor (api.js) needs, and signs it.
 */
@Service
@RequiredArgsConstructor
public class DocumentManager {

    private final OnlyOfficeProperties onlyOffice;
    private final AppProperties app;
    private final FileUtility fileUtility;
    private final JwtService jwtService;

    /**
     * @param fileName    document key in storage
     * @param docKey      unique version key; must change whenever the document content changes
     * @param userId      current user id
     * @param userName    current user display name
     * @param canEdit     whether the editor opens in edit mode
     */
    public Map<String, Object> buildConfig(String fileName, String docKey,
                                           String userId, String userName, boolean canEdit) {
        String encoded = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        String downloadUrl = app.getPublicUrl() + "/api/files/" + encoded + "/download";
        String callbackUrl = app.getPublicUrl() + "/api/callback/" + encoded;

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", fileUtility.getExtension(fileName));
        document.put("key", docKey);
        document.put("title", fileName);
        document.put("url", downloadUrl);

        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("edit", canEdit);
        permissions.put("download", true);
        document.put("permissions", permissions);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", userId);
        user.put("name", userName);

        Map<String, Object> customization = new LinkedHashMap<>();
        customization.put("autosave", true);
        customization.put("forcesave", true);

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("mode", canEdit ? "edit" : "view");
        editorConfig.put("callbackUrl", callbackUrl);
        editorConfig.put("lang", "vi");
        editorConfig.put("user", user);
        editorConfig.put("customization", customization);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("documentType", fileUtility.getDocumentType(fileName));
        config.put("document", document);
        config.put("editorConfig", editorConfig);
        config.put("height", "100%");
        config.put("width", "100%");

        if (jwtService.enabled()) {
            config.put("token", jwtService.sign(new LinkedHashMap<>(config)));
        }
        return config;
    }
}
