package com.gass.jobs;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class SupabaseAuth {
    static final class Result {
        final boolean ok;
        final String message;
        final String accessToken;
        final String userId;
        Result(boolean ok, String message, String accessToken, String userId) {
            this.ok = ok; this.message = message;
            this.accessToken = accessToken; this.userId = userId;
        }
    }

    static Result signIn(String email, String password) {
        return authRequest("/auth/v1/token?grant_type=password", email, password);
    }

    static Result signUp(String email, String password) {
        return authRequest("/auth/v1/signup", email, password);
    }

    private static Result authRequest(String path, String email, String password) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(SupabaseConfig.PROJECT_URL + path);
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            int status = c.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
            String response = readAll(stream);
            JSONObject json = new JSONObject(response.isEmpty() ? "{}" : response);
            if (status >= 200 && status < 300) {
                JSONObject user = json.optJSONObject("user");
                if (user == null) user = json;
                String token = json.optString("access_token", "");
                String id = user.optString("id", "");
                if (token.isEmpty()) {
                    return new Result(true, "Compte créé. Vérifiez votre e-mail pour confirmer le compte, puis connectez-vous.", "", id);
                }
                return new Result(true, "Connexion réussie.", token, id);
            }
            String message = json.optString("msg", json.optString("message",
                    json.optString("error_description", "Erreur d'authentification (" + status + ").")));
            return new Result(false, message, "", "");
        } catch (Exception e) {
            return new Result(false, "Connexion impossible : " + e.getMessage(), "", "");
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private SupabaseAuth() {}
}
