# TUN-JOBS

**TUN-JOBS** is a native Android employment platform starting with Kasserine and expanding across Tunisia.

## Current Android MVP
- TUN-JOBS app display name (Android package remains `com.gass.jobs` to preserve update compatibility).
- Top-left navigation menu with Profile, Settings, About, official sources and regional offices.
- Larger employment-office action buttons and candidate/employer form controls.
- First-launch welcome screen with job-seeker and employer entry choices.
- Candidate profile editor for name, phone, email, governorate, experience, education, skills, availability, profile photo and CV document selection. Profile data and selected file references stay on-device; nothing is uploaded.
- Larger employment-office contact cards, buttons and tap targets.
- Employer offer form with required-field validation and private on-device draft save/load/delete; drafts are not uploaded or published.
- Browse clearly labelled demonstration job opportunities.
- Open official Tunisian employment portals (ANETI, public-service competitions, Ministry of Employment and Interior Ministry recruitment) without scraping or republishing their notices.
- Save and remove favourite listings locally, and review saved listings from the Profile tab.
- Filter by the actual governorate rather than the town name, so Sbeitla and Fériana listings are included under Kasserine.
- Filter by governorate using the listing's actual governorate (including Sbeitla and Fériana under Kasserine).
- Switch between job-seeker and employer journeys.
- View job details.
- Open the official employment-office directory from the bottom navigation.
- Regional employment directorate contacts for all 24 governorates, with Kasserine displayed first, sourced from the Ministry of Employment's official directory.
- Tap-to-call and tap-to-email actions for published regional contact details.
- Direct links to the Ministry's regional directory and the national open-data dataset.
- Source notes and dates, including a warning that the open-data CSV/XLSX records were last updated in March 2023.
- Build a debug APK automatically with GitHub Actions.

## Official directory data policy
The Ministry of Employment regional directory is the source for the Kasserine regional directorate contact displayed in the app:
https://www.emploi.gov.tn/fr/93/directions-regionales-de-formation-professionnelle-et-de-lemploi

The national open-data catalogue also publishes the employment-office network as CSV and XLSX under CC BY:
https://catalog.data.gov.tn/fr/dataset/reseau-de-bureaux-de-l-emploi-et-du-travail-independant-en-tunisie

The catalogue identifies annual updates, but the underlying resource records were last updated on 7 March 2023 and list a 2016–2019 coverage period. Do not treat those records as current without revalidation. Attribute the Ministry as producer, link to the dataset, and preserve update dates.

The app displays regional directorate contacts, not the individual addresses of every local employment office. For Kasserine, the Ministry says there are three employment offices, but their separate addresses and phone numbers must be verified before being shown.

## Important
The current job listings are demonstration data. No real applications, online accounts, or server-side CV uploads are available yet. Candidate profile fields and document references are stored locally on the device only. Those features require a secure backend, consent and privacy controls, and moderation.

Do not scrape or republish government vacancy listings unless an official API/feed or written reuse permission has been confirmed. Prefer direct links to official vacancy notices until then. Each imported vacancy must retain its original source, publication date, deadline, and last verification date.

## Build
Push to `main`, `feature/android-mvp`, or `feature/official-employment-directory`, or run the **GASS-JOBS Android APK** workflow manually. Download the `GASS-JOBS-Android-APK` artifact from the workflow run.
