package com.gass.jobs;

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

    static Result upsert(String userId, String token, String fullName, String phone,
                         String city, String profession, String education,
                         String skills, String availability) {
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
            card.put("discoverable", true);
            request("/rest/v1/candidate_cards?on_conflict=user_id", token, card);

            JSONObject privateDetails = new JSONObject();
            privateDetails.put("user_id", userId);
            privateDetails.put("phone", phone);
            request("/rest/v1/candidate_private?on_conflict=user_id", token, privateDetails);
            return new Result(true, "Vos informations professionnelles sont maintenant synchronisées avec votre compte. Le téléphone reste dans la table privée, protégé par les règles d'accès Supabase. La photo et le CV ne sont pas encore envoyés.");
        } catch (Exception e) {
            return new Result(false, "Le profil reste enregistré sur ce téléphone, mais la synchronisation Supabase a échoué : " + e.getMessage());
        }
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
                InputStream stream = c.getErrorStream();
                String error = readAll(stream);
                throw new IllegalStateException("Supabase (" + status + "): " + error);
            }
            InputStream stream = c.getInputStream();
            if (stream != null) stream.close();
        } finally {
            if (c != null) c.disconnect();
        }
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
