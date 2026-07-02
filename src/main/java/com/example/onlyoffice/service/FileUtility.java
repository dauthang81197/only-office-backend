package com.example.onlyoffice.service;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Maps file extensions to OnlyOffice document types and MIME types.
 */
@Component
public class FileUtility {

    private static final Set<String> WORD = Set.of(
            "doc", "docx", "docm", "dot", "dotx", "dotm", "odt", "fodt", "ott", "rtf", "txt",
            "html", "htm", "mht", "xml", "pdf", "djvu", "fb2", "epub", "xps");

    private static final Set<String> CELL = Set.of(
            "xls", "xlsx", "xlsm", "xlt", "xltx", "xltm", "ods", "fods", "ots", "csv");

    private static final Set<String> SLIDE = Set.of(
            "pps", "ppsx", "ppsm", "ppt", "pptx", "pptm", "pot", "potx", "potm", "odp", "fodp", "otp");

    public String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    /** OnlyOffice documentType: "word" | "cell" | "slide". */
    public String getDocumentType(String fileName) {
        String ext = getExtension(fileName);
        if (CELL.contains(ext)) return "cell";
        if (SLIDE.contains(ext)) return "slide";
        return "word";
    }

    public boolean isSupported(String fileName) {
        String ext = getExtension(fileName);
        return WORD.contains(ext) || CELL.contains(ext) || SLIDE.contains(ext);
    }

    public String getMimeType(String fileName) {
        return switch (getExtension(fileName)) {
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }
}
