# 💼 TUN-JOBS (GASS-JOBS)

### Jobs in Tunisia, as easy as a swipe

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Gass-OS/GASS-JOBS/actions)
[![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](https://supabase.com)
[![WebRTC](https://img.shields.io/badge/WebRTC-calls-333333?style=for-the-badge&logo=webrtc&logoColor=white)](https://webrtc.org)
[![Build](https://github.com/Gass-OS/GASS-JOBS/actions/workflows/android.yml/badge.svg)](https://github.com/Gass-OS/GASS-JOBS/actions)

**TUN-JOBS** is the public name of the GASS-JOBS Android employment platform — starting with Kasserine, built for all of Tunisia. Job seekers swipe through offers, employers swipe through candidates, and when both sides say yes, they can talk — by chat, voice or video call.

---

## 📖 What it is

- 👆 **Swipe to discover** — job offers and candidate profiles in a Tinder-style deck: swipe right to like, left to pass
- 🤝 **Mutual match, then talk** — only a mutual match opens a private chat, with **voice and video calls** (WebRTC) right inside it
- 📻 **Radio & TV** — live radio and TV inside the app, plus an **Inbox** for your conversations
- 🏛️ **ANETI / Real Jobs Hub** — public employment offers alongside community listings
- 🗄️ **Supabase backend** — accounts, profiles, matching and call signalling run on Supabase (PostgREST + Auth)
- 📡 **Calls that survive bad networks** — call setup retries the TURN service and the signalling channel, and stops ringing after 30 seconds with a "No answer" message instead of hanging forever

One branch, one APK: everything (calls, Radio, TV, Inbox) lives on `main`.

---

## 🌐 Website

The multilingual landing website is `index.html` at the repo root, published with GitHub Pages ([live site](https://gass-os.github.io/GASS-JOBS/)).

- French, English and Arabic (RTL) interface
- Mobile-responsive landing page
- Candidate and employer feature introductions
- Links to the Android app releases, source repository and support issues

---

## 📦 Get the app

Every push to `main` builds an APK with GitHub Actions — download **`GASS-JOBS-Android-APK`** from the latest successful run's artifacts ([Actions page](https://github.com/Gass-OS/GASS-JOBS/actions)). Current APK is about **28 MB**.

> The app is not on Google Play yet — the CI artifact is the way to install it for now. The app is under active development: demo profiles shown in the app are illustrative, and the real-jobs catalogue is being built out.

---

## 🛠️ Build it yourself

```bash
git clone https://github.com/Gass-OS/GASS-JOBS.git
cd GASS-JOBS
# Open in Android Studio (Gradle project) — app/ is the Android app,
# index.html is the website, supabase/ holds the database migrations.
```

Pushing to `main` triggers the same build in CI — the APK is built by GitHub Actions.

### Project layout

```
├── app/                  # Android app (Java)
│   └── src/main/java/com/gass/jobs/
│       ├── MainActivity.java   # swipe deck, matches, chat, Radio/TV/Inbox
│       ├── CallActivity.java   # WebRTC voice/video calls + TURN
│       ├── RadioActivity.java  # radio
│       └── TvActivity.java     # TV
├── supabase/migrations/  # database schema (profiles, matches, call signals)
├── index.html            # landing website (GitHub Pages)
├── docs/                 # website docs
└── .github/workflows/
    ├── android.yml       # builds the APK on push to main
    ├── keep-alive.yml    # daily ping so the Supabase backend never sleeps
    └── deploy-pages.yml  # publishes the website
```

### How calls work

1. Two people match (both swiped right).
2. The match opens a private chat — voice and video call buttons live there.
3. The app fetches TURN credentials from a Cloudflare Worker, then signals through Supabase (`job_call_signals`) to ring the other side.
4. The other side taps **"Check incoming calls"** in the chat to answer.

---

## 🔒 Data integrity and privacy

Any sample profile or opportunity shown in website illustrations is illustrative, not a live job listing. Real job listings must retain their source and publication date. Personal profile details, photos and CVs require secure backend storage, consent and clear visibility controls.

---

## 📜 Status

| Area | State |
|:---|---|
| Swipe + matching | Working |
| Private chat | Working |
| Voice / video calls | Working after a match (WebRTC + TURN) |
| Radio, TV, Inbox | Working |
| ANETI / Real Jobs Hub | In the app |
| Real job listings catalogue | In progress — demo profiles for now |

---

<div align="center">

**Made in Tunisia 🇹🇳**

*A Gass-OS project — jobs for Kasserine, jobs for Tunisia.*

</div>
