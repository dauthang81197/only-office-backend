package com.example.onlyoffice.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the current version key for each document. OnlyOffice caches a document
 * by its key, so the key MUST change whenever the stored content changes, otherwise
 * clients keep editing a stale cached copy.
 *
 * <p>In-memory for demo purposes; back it with the database in production.
 */
@Component
public class DocumentKeyRegistry {

    private final ConcurrentHashMap<String, AtomicInteger> versions = new ConcurrentHashMap<>();

    public String currentKey(String fileName) {
        int v = versions.computeIfAbsent(fileName, k -> new AtomicInteger()).get();
        return sanitize(fileName) + "_" + v;
    }

    /** Bump the version after a successful save; returns the new key. */
    public String nextKey(String fileName) {
        int v = versions.computeIfAbsent(fileName, k -> new AtomicInteger()).incrementAndGet();
        return sanitize(fileName) + "_" + v;
    }

    private String sanitize(String fileName) {
        // Key allows only [0-9a-zA-Z.=_-], max 128 chars.
        String s = fileName.replaceAll("[^0-9a-zA-Z._-]", "_");
        return s.length() > 100 ? s.substring(s.length() - 100) : s;
    }
}
