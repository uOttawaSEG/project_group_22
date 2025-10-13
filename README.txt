# OTAMS – Android App (SEG 2105 • Project 1)

_A simple tutoring platform demo app built for **SEG 2105 – Introduction to Software Engineering**._

**Professor:** Hussein Al Osman  
**Course:** Fall 2025

---

## 👥 Team

| Name | Student # |
|---|---|
| Owen Simpson | 300250777 |
| Ruike Zhang | 300378620 |
| Joy Yeung | 300440192 |
| Zackary Hamwi | 300337212 |
| Youssouf Salah | 8229696 |
| Maryam (Marie) Sindhu | 300350212 |

---

## 📱 Overview

OTAMS lets users **register** and **sign in** as one of three roles:

- **Student** – create an account with basic info and program details  
- **Tutor** – register with degrees and courses they can tutor  
- **Administrator** – protected creation via 1 invite code (demo flow)

After authentication, users see a **Welcome screen** showing their **role** and profile summary. Users can **log out** any time.

---

## ✨ Features (Deliverable 1)

- Email/password **registration** and **login** (Firebase Auth)
- Role-aware UI: **Student / Tutor / Admin**
- **Field validation** with clear error messages
- **Welcome screen** displays user role and profile details
- Optional bonus: **Firebase Firestore** used to store user profiles
## 🔐 Admin (Demo)

Use these **demo credentials** to create and sign in as the Administrator:

- **Invite code:** `SEG2105-ADMIN-ONLY`
- **Admin email:** `admin@otams22.com`
- **Admin password:** `AdminPass_Group_22`

**How to use:**
1. Open **Admin Registration** in the app.
2. Enter the email and password above.
3. Enter the invite code **SEG2105-ADMIN-ONLY**.
4. Submit to create the admin, then log in with the same email/password.

> ⚠️ For assignment demo only. In a real app, the invite code and admin bootstrap flow would be secured (remote config/Cloud Functions, server checks, etc.).

---

## 🏗️ Tech Stack

- **Language:** Java (Android)
- **Min SDK:** 24
- **Target / Compile SDK:** 36
- **Libraries:** Material Components, Firebase Auth, Firebase Firestore

---

## 🚀 Getting Started

1. **Clone** the repo in Android Studio (Giraffe+ recommended).
2. In **`app/`**, place your **`google-services.json`** that matches:
   - `package_name`: `com.example.seg2105_project_1_tutor_registration_form`
   - Firebase project: `otams-prod-db`
3. Verify Gradle plugins in `app/build.gradle.kts`:
   ```kotlin
   plugins {
       alias(libs.plugins.android.application)
       id("com.google.gms.google-services")
   }
