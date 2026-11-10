package com.gass.jobs;

/**
 * Public Supabase client configuration.
 *
 * The publishable key is designed for client-side use. Database access must
 * remain protected by the Row Level Security policies in the SQL migration.
 * Never add a Supabase service_role/secret key to this application.
 */
final class SupabaseConfig {
    static final String PROJECT_URL = "https://rwmldlqjwgtjckspqkfl.supabase.co";
    static final String PUBLISHABLE_KEY = "sb_publishable_aYD3mQuWRYLMbqjLCDoBLA_RVdRxu4_";

    private SupabaseConfig() {}

    static boolean isConfigured() {
        return PROJECT_URL.startsWith("https://")
                && PROJECT_URL.endsWith(".supabase.co")
                && PUBLISHABLE_KEY.startsWith("sb_publishable_");
    }
}
