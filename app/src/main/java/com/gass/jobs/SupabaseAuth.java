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
        final String refreshToken;
        final String userId;

        Result(boolean ok, String message, String accessToken, String refreshToken, String userId) {
            this.ok = ok;
            this.message = message;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.userId = userId;
        }
    }

    static Result signIn(String email, String password) {
        return authRequest("/auth/v1/token?grant_type=password", email, password, null);
    }

    static Result signUp(String email, String password) {
        return authRequest("/auth/v1/signup", email, password, null);
    }

    static Result refresh(String refreshToken) {
        return authRequest("/auth/v1/token?grant_type=refresh_token", null, null, refreshToken);
    }

    private static Result authRequest(String path, String email, String password, String refreshToken) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(SupabaseConfig.PROJECT_URL + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY);

            JSONObject body = new JSONObject();
            if (refreshToken != null) {
                body.put("refresh_token", refreshToken);
            } else {
                body.put("email", email);
                body.put("password", password);
            }
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = readAll(stream);
            JSONObject json = new JSONObject(response.isEmpty() ? "{}" : response);
            if (status >= 200 && status < 300) {
                JSONObject user = json.optJSONObject("user");
                if (user == null) user = json;
                String access = json.optString("access_token", "");
                String refresh = json.optString("refresh_token", "");
                String id = user.optString("id", "");
                if (access.isEmpty()) {
                    return new Result(true,
                            "Compte créé. Vérifiez votre e-mail pour confirmer le compte, puis connectez-vous.",
                            "", "", id);
                }
                return new Result(true, "Connexion réussie.", access, refresh, id);
            }

            String message = json.optString("msg", json.optString("message",
                    json.optString("error_description", "Erreur d'authentification (" + status + ").")));
            return new Result(false, message, "", "", "");
        } catch (Exception e) {
            return new Result(false, "Connexion impossible : " + e.getMessage(), "", "", "");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private SupabaseAuth() {}
}
