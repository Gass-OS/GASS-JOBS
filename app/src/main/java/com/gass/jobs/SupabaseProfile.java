package com.gass.jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class SupabaseProfile {
    static final class Result {
        final boolean ok;
        final String message;
        Result(boolean ok, String message) { this.ok = ok; this.message = message; }
    }
    static final class ProfileData {
        final String fullName, phone, email, city, profession, education, skills, availability, photoPath, photoUrl;
        ProfileData(String fullName, String phone, String email, String city, String profession,
                    String education, String skills, String availability, String photoPath, String photoUrl) {
            this.fullName=fullName; this.phone=phone; this.email=email; this.city=city; this.profession=profession;
            this.education=education; this.skills=skills; this.availability=availability;
            this.photoPath=photoPath == null ? "" : photoPath; this.photoUrl=photoUrl == null ? "" : photoUrl;
        }
    }

    static Result upsert(String userId, String token, String fullName, String phone, String email, String city,
                         String profession, String education, String skills, String availability) {
        return upsert(userId, token, fullName, phone, email, city, profession, education, skills, availability, "");
    }

    static Result upsert(String userId, String token, String fullName, String phone, String email, String city,
                         String profession, String education, String skills, String availability, String photoPath) {
        try {
            JSONObject card = new JSONObject();
            card.put("user_id", userId); card.put("full_name", fullName); card.put("city", city);
            card.put("governorate", city); card.put("profession", profession); card.put("experience", profession);
            card.put("education", education); card.put("skills", skills); card.put("availability", availability);
            card.put("discoverable", true);
            if (photoPath != null && !photoPath.isEmpty()) card.put("photo_path", photoPath);
            request("/rest/v1/candidate_cards?on_conflict=user_id", token, card);

            JSONObject publicProfile = new JSONObject();
            publicProfile.put("user_id", userId); publicProfile.put("full_name", fullName);
            publicProfile.put("city", city); publicProfile.put("governorate", city); publicProfile.put("profession", profession);
            if (photoPath != null && !photoPath.isEmpty()) publicProfile.put("photo_path", photoPath);
            request("/rest/v1/candidate_public_profiles?on_conflict=user_id", token, publicProfile);

            JSONObject privateDetails = new JSONObject();
            privateDetails.put("user_id", userId); privateDetails.put("phone", phone); privateDetails.put("email", email);
            request("/rest/v1/candidate_private?on_conflict=user_id", token, privateDetails);
            if (photoPath != null && !photoPath.isEmpty()) updateAuthPhotoMetadata(token, photoPath);
            return new Result(true, photoPath == null || photoPath.isEmpty()
                    ? "Profil synchronisé. Ajoutez une photo puis enregistrez pour la synchroniser."
                    : "Profil et photo synchronisés sur votre compte TUN-JOBS.");
        } catch (Exception e) {
            return new Result(false, "Le profil reste enregistré localement, mais la synchronisation a échoué : " + e.getMessage());
        }
    }

    static ProfileData fetch(String userId, String token) throws Exception {
        JSONArray cards = new JSONArray(get("/rest/v1/candidate_cards?user_id=eq." + userId
                + "&select=full_name,city,profession,education,skills,availability,photo_path", token));
        JSONObject card = cards.length() == 0 ? new JSONObject() : cards.getJSONObject(0);
        JSONArray privateRows = new JSONArray(get("/rest/v1/candidate_private?user_id=eq." + userId + "&select=phone,email", token));
        JSONObject privateRow = privateRows.length() == 0 ? new JSONObject() : privateRows.getJSONObject(0);
        JSONObject authUser = getAuthUser(token);
        JSONObject metadata = authUser.optJSONObject("user_metadata");
        if (metadata == null) metadata = new JSONObject();

        String fullName = first(card.optString("full_name", ""), metadata.optString("full_name", ""));
        String city = first(card.optString("city", ""), metadata.optString("city", ""));
        String profession = first(card.optString("profession", ""), metadata.optString("profession", ""));
        String education = first(card.optString("education", ""), metadata.optString("education", ""));
        String skills = card.optString("skills", "");
        if (skills.isEmpty()) skills = jsonStringOrArray(metadata.opt("skills"));
        String availability = first(card.optString("availability", ""), metadata.optString("availability", ""));
        String photoPath = first(card.optString("photo_path", ""), metadata.optString("photo_path", ""));
        String photoUrl = metadata.optString("photo_url", "");
        String phone = first(privateRow.optString("phone", ""), metadata.optString("phone", ""));
        String email = first(privateRow.optString("email", ""), authUser.optString("email", ""));
        if (cards.length() == 0 && metadata.length() == 0) return null;
        return new ProfileData(fullName, phone, email, city, profession, education, skills, availability, photoPath, photoUrl);
    }

    static String uploadPhoto(String userId, String token, File photoFile) throws Exception {
        if (photoFile == null || !photoFile.isFile() || photoFile.length() == 0)
            throw new IllegalArgumentException("Aucune photo de profil valide n'a été trouvée.");
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IllegalArgumentException("Format d'image non reconnu.");
        int sample = 1;
        while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
        if (bitmap == null) throw new IllegalArgumentException("Impossible de lire la photo.");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, bytes)) throw new IllegalStateException("Impossible de préparer la photo.");
        } finally { bitmap.recycle(); }

        String path = userId + "/" + System.currentTimeMillis() + "-profile.jpg";
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SupabaseConfig.PROJECT_URL + "/storage/v1/object/candidate-photos/" + path).openConnection();
            c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setDoOutput(true);
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            c.setRequestProperty("Content-Type", "image/jpeg"); c.setRequestProperty("x-upsert", "false");
            byte[] data = bytes.toByteArray();
            c.setFixedLengthStreamingMode(data.length);
            try (OutputStream out = c.getOutputStream()) { out.write(data); }
            int status = c.getResponseCode();
            if (status < 200 || status >= 300)
                throw new IllegalStateException("Stockage photo Supabase (" + status + ") : " + readAll(c.getErrorStream()));
            return path;
        } finally { if (c != null) c.disconnect(); }
    }

    static boolean downloadPhoto(String photoUrl, String photoPath, String token, File destination) throws Exception {
        String urlText = photoUrl == null ? "" : photoUrl.trim();
        if (urlText.isEmpty() && photoPath != null && !photoPath.isEmpty())
            urlText = SupabaseConfig.PROJECT_URL + "/storage/v1/object/public/candidate-photos/" + Uri.encode(photoPath, "/");
        if (!urlText.startsWith(SupabaseConfig.PROJECT_URL + "/storage/v1/")) return false;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(urlText).openConnection();
            c.setRequestMethod("GET"); c.setConnectTimeout(15000); c.setReadTimeout(30000);
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) return false;
            File parent = destination.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(destination, false)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                out.flush();
            }
            return destination.isFile() && destination.length() > 0;
        } finally { if (c != null) c.disconnect(); }
    }

    private static void updateAuthPhotoMetadata(String token, String photoPath) throws Exception {
        JSONObject data = new JSONObject();
        data.put("photo_path", photoPath);
        data.put("photo_url", SupabaseConfig.PROJECT_URL + "/storage/v1/object/public/candidate-photos/" + photoPath);
        JSONObject body = new JSONObject(); body.put("data", data);
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SupabaseConfig.PROJECT_URL + "/auth/v1/user").openConnection();
            c.setRequestMethod("PUT"); c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            int status = c.getResponseCode();
            if (status < 200 || status >= 300)
                throw new IllegalStateException("Mise à jour de la photo (" + status + ") : " + readAll(c.getErrorStream()));
        } finally { if (c != null) c.disconnect(); }
    }

    private static JSONObject getAuthUser(String token) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SupabaseConfig.PROJECT_URL + "/auth/v1/user").openConnection();
            c.setRequestMethod("GET"); c.setConnectTimeout(15000); c.setReadTimeout(20000);
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Compte Supabase (" + status + ").");
            return new JSONObject(readAll(c.getInputStream()));
        } finally { if (c != null) c.disconnect(); }
    }

    private static String jsonStringOrArray(Object value) {
        if (value == null || value == JSONObject.NULL) return "";
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value; StringBuilder out = new StringBuilder();
            for (int i=0;i<array.length();i++) { if (i>0) out.append(", "); out.append(array.optString(i, "")); }
            return out.toString();
        }
        return String.valueOf(value);
    }
    private static String first(String a, String b) { return a == null || a.trim().isEmpty() ? (b == null ? "" : b) : a; }

    private static void request(String path, String token, JSONObject body) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SupabaseConfig.PROJECT_URL + path).openConnection();
            c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            c.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Supabase (" + status + "): " + readAll(c.getErrorStream()));
            InputStream stream = c.getInputStream(); if (stream != null) stream.close();
        } finally { if (c != null) c.disconnect(); }
    }

    private static String get(String path, String token) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SupabaseConfig.PROJECT_URL + path).openConnection();
            c.setRequestMethod("GET"); c.setConnectTimeout(15000); c.setReadTimeout(20000);
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Supabase (" + status + "): " + readAll(c.getErrorStream()));
            return readAll(c.getInputStream());
        } finally { if (c != null) c.disconnect(); }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private SupabaseProfile() {}
}
