# GASS-JOBS

**GASS-JOBS** is a native Android employment platform starting with Kasserine and expanding across Tunisia.

## Current Android MVP
- Browse clearly labelled demonstration job opportunities.
- Switch between job-seeker and employer journeys.
- View job details.
- Open the official employment-office directory from the bottom navigation.
- Kasserine regional employment directorate contact, sourced from the Ministry of Employment's regional directory.
- Direct links to the Ministry's regional directory and the national open-data dataset.
- Source notes and dates, including a warning that the open-data CSV/XLSX records were last updated in March 2023.
- Build a debug APK automatically with GitHub Actions.

## Official directory data policy
The Ministry of Employment regional directory is the source for the Kasserine regional directorate contact displayed in the app:
https://www.emploi.gov.tn/fr/93/directions-regionales-de-formation-professionnelle-et-de-lemploi

The national open-data catalogue also publishes the employment-office network as CSV and XLSX under CC BY:
https://catalog.data.gov.tn/fr/dataset/reseau-de-bureaux-de-l-emploi-et-du-travail-independant-en-tunisie

The catalogue identifies annual updates, but the underlying resource records were last updated on 7 March 2023 and list a 2016–2019 coverage period. Do not treat those records as current without revalidation. Attribute the Ministry as producer, link to the dataset, and preserve update dates.

The current app displays the Kasserine regional directorate's published contact, not individual addresses for all three local employment offices. Those local office records must be verified separately before being shown as current.

## Important
The current job listings are demonstration data. No real applications, accounts, CV uploads, or personal contact details are collected yet. Those features require a secure backend, consent and privacy controls, and moderation.

Do not scrape or republish government vacancy listings unless an official API/feed or written reuse permission has been confirmed. Prefer direct links to official vacancy notices until then. Each imported vacancy must retain its original source, publication date, deadline, and last verification date.

## Build
Push to `main`, `feature/android-mvp`, or `feature/official-employment-directory`, or run the **GASS-JOBS Android APK** workflow manually. Download the `GASS-JOBS-Android-APK` artifact from the workflow run.
