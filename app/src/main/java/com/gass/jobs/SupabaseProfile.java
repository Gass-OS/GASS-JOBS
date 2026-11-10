package com.gass.jobs;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
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
        final String fullName, phone, email, city, profession, education, skills, availability, photoPath, cvPath;
        ProfileData(String fullName, String phone, String email, String city, String profession,
                    String education, String skills, String availability, String photoPath, String cvPath) {
            this.fullName=fullName; this.phone=phone; this.email=email; this.city=city;
            this.profession=profession; this.education=education;
            this.skills=skills; this.availability=availability; this.photoPath=photoPath; this.cvPath=cvPath;
        }
    }

    static Result upsert(String userId, String token, String fullName, String phone, String email,
                         String city, String profession, String education,
                         String skills, String availability, String photoPath, String cvPath) {
        try {
            JSONObject card = new JSONObject();
            card.put("user_id", userId);
            card.put("full_name", fullName);
            card.put("city", city);
            card.put("governorate", city);
            card.put("profession", profession);
            card.put("experience", profession);
            card.put("education", education);
            card.put("skills", skills);
            card.put("availability", availability);
            card.put("photo_path", photoPath == null || photoPath.isEmpty() ? JSONObject.NULL : photoPath);
            card.put("discoverable", true);
            request("/rest/v1/candidate_cards?on_conflict=user_id", token, card);

            JSONObject publicProfile = new JSONObject();
            publicProfile.put("user_id", userId);
            publicProfile.put("full_name", fullName);
            publicProfile.put("city", city);
            publicProfile.put("governorate", city);
            publicProfile.put("profession", profession);
            publicProfile.put("photo_path", photoPath == null || photoPath.isEmpty() ? JSONObject.NULL : photoPath);
            request("/rest/v1/candidate_public_profiles?on_conflict=user_id", token, publicProfile);
            if (cvPath != null && !cvPath.isEmpty()) {
                JSONObject document = new JSONObject();
                document.put("user_id", userId);
                document.put("cv_path", cvPath);
                request("/rest/v1/candidate_documents?on_conflict=user_id", token, document);
            }

            JSONObject privateDetails = new JSONObject();
            privateDetails.put("user_id", userId);
            privateDetails.put("phone", phone);
            privateDetails.put("email", email);
            request("/rest/v1/candidate_private?on_conflict=user_id", token, privateDetails);
            return new Result(true, "Profil synchronisé. Nom, ville et photo sont publics ; e-mail, téléphone et CV restent privés.");
        } catch (Exception e) {
            return new Result(false, "Le profil reste enregistré localement, mais la synchronisation a échoué : " + e.getMessage());
        }
    }

    static ProfileData fetch(String userId, String token) throws Exception {
        String filter = "?user_id=eq." + userId + "&select=full_name,city,profession,education,skills,availability,photo_path";
        JSONArray cards = new JSONArray(get("/rest/v1/candidate_cards" + filter, token));
        if (cards.length() == 0) return null;
        JSONObject card = cards.getJSONObject(0);
        String documentFilter = "?user_id=eq." + userId + "&select=cv_path";
        JSONArray documentRows = new JSONArray(get("/rest/v1/candidate_documents" + documentFilter, token));
        String cvPath = documentRows.length() == 0 ? "" : documentRows.getJSONObject(0).optString("cv_path", "");
        String privateFilter = "?user_id=eq." + userId + "&select=phone,email";
        JSONArray privateRows = new JSONArray(get("/rest/v1/candidate_private" + privateFilter, token));
        String phone = privateRows.length() == 0 ? "" : privateRows.getJSONObject(0).optString("phone", "");
        String email = privateRows.length() == 0 ? "" : privateRows.getJSONObject(0).optString("email", "");
        return new ProfileData(
                card.optString("full_name", ""), phone, email, card.optString("city", ""),
                card.optString("profession", ""), card.optString("education", ""),
                card.optString("skills", ""), card.optString("availability", ""),
                card.optString("photo_path", ""), cvPath);
    }

    private static void request(String path, String token, JSONObject body) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SupabaseConfig.PROJECT_URL + path).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            c.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Supabase (" + status + "): " + readAll(c.getErrorStream()));
            }
            InputStream stream = c.getInputStream();
            if (stream != null) stream.close();
        } finally { if (c != null) c.disconnect(); }
    }

    private static String get(String path, String token) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SupabaseConfig.PROJECT_URL + path).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Supabase (" + status + "): " + readAll(c.getErrorStream()));
            }
            return readAll(c.getInputStream());
        } finally { if (c != null) c.disconnect(); }
    }

    static void updateAssetPaths(String userId, String token, String photoPath, String cvPath) throws Exception {
        if (photoPath != null && !photoPath.isEmpty()) {
            JSONObject photo = new JSONObject();
            photo.put("photo_path", photoPath);
            patch("/rest/v1/candidate_cards?user_id=eq." + userId, token, photo);
            patch("/rest/v1/candidate_public_profiles?user_id=eq." + userId, token, photo);
        }
        if (cvPath != null && !cvPath.isEmpty()) {
            JSONObject document = new JSONObject();
            document.put("user_id", userId);
            document.put("cv_path", cvPath);
            request("/rest/v1/candidate_documents?on_conflict=user_id", token, document);
        }
    }

    private static void patch(String path, String token, JSONObject body) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SupabaseConfig.PROJECT_URL + path).openConnection();
            c.setRequestMethod("PATCH");
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            c.setRequestProperty("Authorization", "Bearer " + token);
            c.setRequestProperty("Prefer", "return=minimal");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Supabase (" + status + "): " + readAll(c.getErrorStream()));
            InputStream stream = c.getInputStream();
            if (stream != null) stream.close();
        } finally { if (c != null) c.disconnect(); }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private SupabaseProfile() {}
}
