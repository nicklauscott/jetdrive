package com.niclauscott.jetdrive.file_feature.file.service;

import java.util.*;

public class MimeTypeUtil {

    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("htm", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("csv", "text/csv");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("xml", "application/xml");

        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("webp", "image/webp");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");

        MIME_TYPES.put("mp3", "audio/mpeg");
        MIME_TYPES.put("wav", "audio/wav");
        MIME_TYPES.put("ogg", "audio/ogg");

        MIME_TYPES.put("mp4", "video/mp4");
        MIME_TYPES.put("webm", "video/webm");
        MIME_TYPES.put("mkv", "video/x-matroska");
        MIME_TYPES.put("avi", "video/x-msvideo");
        MIME_TYPES.put("mov", "video/quicktime");

        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("zip", "application/zip");
        MIME_TYPES.put("rar", "application/vnd.rar");
        MIME_TYPES.put("7z", "application/x-7z-compressed");
        MIME_TYPES.put("tar", "application/x-tar");
        MIME_TYPES.put("gz", "application/gzip");

        MIME_TYPES.put("doc", "application/msword");
        MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPES.put("xls", "application/vnd.ms-excel");
        MIME_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        MIME_TYPES.put("apk", "application/vnd.android.package-archive");
        MIME_TYPES.put("exe", "application/vnd.microsoft.portable-executable");
    }

    public static String getRandomMimeType() {
        List<Map.Entry<String, String>> entries = new ArrayList<>(MIME_TYPES.entrySet());
        Map.Entry<String, String> randomEntry = entries.get(new Random().nextInt(entries.size()));
        return randomEntry.getValue();
    }

    public static String getMimeTypeByExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream";
        }

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }
}
