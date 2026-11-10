package com.gass.jobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class SupabaseStorage {
    static String upload(File file, String bucket, String userId, String token, String contentType) throws Exception {
        if (file == null || !file.isFile() || file.length() == 0) throw new IllegalArgumentException("Fichier vide ou introuvable.");
        if (file.length() > 15L * 1024L * 1024L) throw new IllegalArgumentException("Fichier trop volumineux (maximum 15 Mo).");
        String objectName = userId + "/profile-" + System.currentTimeMillis() + extensionForMime(contentType);
        HttpURLConnection c = null;
        try {
            URL url = new URL(SupabaseConfig.PROJECT_URL + "/storage/v1/object/" + bucket + "/" + encodePath(objectName));
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(20000);
            c.setReadTimeout(60000);
            c.setDoOutput(true);
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            c.setRequestProperty("Content-Type", contentType);
            c.setRequestProperty("Cache-Control", "3600");
            c.setFixedLengthStreamingMode(file.length());
            try (InputStream in = new FileInputStream(file); OutputStream out = c.getOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                out.flush();
            }
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Stockage Supabase (" + status + "): " + readAll(c.getErrorStream()));
            return objectName;
        } finally { if (c != null) c.disconnect(); }
    }

    static boolean downloadPublicPhoto(String objectPath, File destination) {
        return download("/storage/v1/object/public/candidate-photos/" + encodePath(objectPath), destination, null);
    }

    static boolean downloadPrivateCv(String objectPath, File destination, String token) {
        return download("/storage/v1/object/authenticated/candidate-cvs/" + encodePath(objectPath), destination, token);
    }

    private static boolean download(String path, File destination, String token) {
        HttpURLConnection c = null;
        File temporary = new File(destination.getParentFile(), destination.getName() + ".tmp");
        try {
            URL url = new URL(SupabaseConfig.PROJECT_URL + path);
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(15000);
            c.setReadTimeout(45000);
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) return false;
            try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(temporary, false)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                out.flush();
            }
            if (destination.exists() && !destination.delete()) return false;
            return temporary.renameTo(destination);
        } catch (Exception ignored) {
            if (temporary.exists()) temporary.delete();
            return false;
        } finally { if (c != null) c.disconnect(); }
    }

    static String extensionForPath(String path) {
        int dot = path == null ? -1 : path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot) : ".bin";
    }

    private static String extensionForMime(String mime) {
        String value = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if (value.contains("jpeg") || value.contains("jpg")) return ".jpg";
        if (value.contains("png")) return ".png";
        if (value.contains("webp")) return ".webp";
        if (value.contains("pdf")) return ".pdf";
        if (value.contains("wordprocessingml")) return ".docx";
        if (value.contains("msword")) return ".doc";
        return ".bin";
    }

    private static String encodePath(String path) throws Exception {
        StringBuilder out = new StringBuilder();
        for (String part : path.split("/")) {
            if (out.length() > 0) out.append('/');
            out.append(URLEncoder.encode(part, "UTF-8").replace("+", "%20"));
        }
        return out.toString();
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        try (InputStream input = in; java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048]; int count;
            while ((count = input.read(buffer)) != -1) out.write(buffer, 0, count);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private SupabaseStorage() {}
}
