# Supabase setup for TUN-JOBS

This branch adds the database/security foundation for cross-device accounts, job posts, applications, applicant cards, and consent-controlled contact/CV access. The project migration has been run by the project owner. The Android app now has Supabase email sign-up/sign-in and securely stores the access token with Android Keystore. Remote profile sync, live job feeds/publishing, applications and private file exchange are not implemented yet.

## Create the project
1. Create a Supabase project at https://supabase.com/dashboard.
2. Open **SQL Editor** and run `supabase/migrations/20261110130000_job_matching.sql` (completed for the configured project on 2026-11-10).
3. In **Project Settings → API**, copy the Project URL and the publishable/anon key.
4. Never put the `service_role` / secret key in the Android app, GitHub, or chat.
5. Keep both Storage buckets private. The migration creates `candidate-photos` and `candidate-cvs` as private buckets and applies access policies.

## Security model
- Employers must be verified in `employer_accounts` before they can publish jobs. Verification is an admin action in Supabase, not a user-editable app switch.
- Candidate cards are visible to an employer after the candidate applies to one of that employer's jobs.
- Phone details and CV files are separate from the public candidate card. The candidate must explicitly enable phone/CV sharing for that application.
- CV downloads use authenticated access to the private bucket; do not make CVs public or store permanent public URLs.
- Job rows include optional latitude/longitude. The app must request location only after the user taps the nearby-sort control and must work when permission is denied.

## Android client configuration
The supplied Project URL and publishable key are configured in `SupabaseConfig.java`. A publishable key is intended for client-side use, but it is not an authorization bypass: keep Row Level Security enabled and use the policies in the migration. Never add a `service_role` or secret key to the Android app.

## Current implementation boundary
The GPS permission and local city-distance sorting are implemented in the app. The current built-in job examples remain clearly marked as demonstration data; the ANETI cards remain source-linked. Supabase credentials and email authentication are configured. Access tokens are encrypted with an Android Keystore AES-GCM key. Authentication has not yet been validated on a physical device. Remote profile sync, live job feeds/publication, applications, and consent-controlled candidate/CV delivery remain to be implemented and tested.
