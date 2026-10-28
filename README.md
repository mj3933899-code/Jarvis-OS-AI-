# 🤖 JARVIS — Android AI Assistant (`com.aistudio.jarvisai.kxmpzq`)

**JARVIS** হলো একটি সম্পূর্ণ Kotlin ও Jetpack Compose (Material 3) ভিত্তিক Android AI Assistant অ্যাপ, যা **Google AI Studio** থেকে সরাসরি **GitHub Repository (`mj3933899-code/Jarvis-OS-AI-`) → GitHub Actions → Automatic Gradle APK Build → Direct Phone Download**-এর জন্য প্রস্তুত করা হয়েছে।

---

## 📊 প্রজেক্টের বর্তমান টেকনিক্যাল কনফিগারেশন ( অপরিবর্তিত ও যাচাইকৃত )

| কনফিগারেশন | মান (Value) |
|---|---|
| **App Name** | `JARVIS` |
| **Namespace (Package)** | `com.example` |
| **Application ID** | `com.aistudio.jarvisai.kxmpzq` |
| **Gradle Version** | `9.3.1` (`gradle-9.3.1-bin.zip`) |
| **Android Gradle Plugin (AGP)** | `9.1.1` |
| **Kotlin Version** | `2.0.21` (KSP `2.0.21-1.0.27`) |
| **Compile SDK / Target SDK** | `35` (Android 15) |
| **Min SDK** | `24` (Android 7.0+) |
| **Java / JDK Setup** | JDK `21` (Temurin) for Gradle 9.3.1 daemon; Bytecode Target `17` |
| **Default AI Model** | `gemini-3.8-flash` (Optional Advanced: `gemini-4-argon`) |
| **Gradle Debug APK Path** | `app/build/outputs/apk/debug/app-debug.apk` → রিনেম হয়ে `JARVIS-debug.apk` |
| **Gradle Release APK Path** | `app/build/outputs/apk/release/app-release.apk` → রিনেম হয়ে `JARVIS-release.apk` |

---

## 📱 ধাপে ধাপে গাইড (শুধুমাত্র মোবাইল ফোন ব্যবহার করেই সম্পূর্ণ কাজ করা যাবে)

### Step 1: GitHub-এ নতুন Repository তৈরি
1. আপনার ফোনের ব্রাউজারে (Chrome) [https://github.com/new](https://github.com/new)-এ যান এবং লগইন করুন।
2. **Repository name** দিন: `Jarvis-OS-AI-` (অথবা আপনার পছন্দের নাম, যেমন `mj3933899-code/Jarvis-OS-AI-`)।
3. Repository-টি **Public** (ফ্রি আনলিমিটেড GitHub Actions মিনিটের জন্য) অথবা **Private** সিলেক্ট করুন।
4. নিচে **Create repository** বাটনে ট্যাপ করুন।

---

### Step 2: Google AI Studio থেকে Android Project-টি GitHub-এ Publish / Push করা
আপনি পিসি ছাড়াই সরাসরি ফোন থেকে দুটি উপায়ে প্রজেক্টটি GitHub-এ পাঠাতে পারেন:

* **পদ্ধতি ক (সবচেয়ে সহজ — সরাসরি AI Studio থেকে Push to GitHub):**
  1. Google AI Studio-এর উপরের ডানদিকে **Settings / Actions (⚙️ বা ⋮)** মেনুতে ট্যাপ করুন।
  2. **Push to GitHub** (অথবা **Export to GitHub**) অপশনটি সিলেক্ট করুন।
  3. আপনার GitHub অ্যাকাউন্ট কানেক্ট করে টার্গেট রেপোজিটরি `mj3933899-code/Jarvis-OS-AI-` সিলেক্ট করে **Push** করুন।
  4. Push হওয়ার সাথে সাথেই `.github/workflows/build-apk.yml`সহ সব ফাইল GitHub-এ চলে যাবে!

* **পদ্ধতি খ (ZIP ডাউনলোড করে আপলোড):**
  1. AI Studio থেকে **Download ZIP** বা **Export Project** করুন।
  2. নিচের **প্রয়োজনীয় ফাইল তালিকা** অনুযায়ী ফাইলগুলো আপনার GitHub Repository-তে কমিট/আপলোড করুন।

---

### Step 3: GitHub Actions-এ গিয়ে Build Status দেখা
1. আপনার ফোনের ব্রাউজারে GitHub Repository (`https://github.com/mj3933899-code/Jarvis-OS-AI-`)-তে যান।
2. উপরের মেনু বার থেকে **Actions** ট্যাবে ট্যাপ করুন।
3. কোড Push করার সাথে সাথেই **"Build Android Debug APK"** নামের একটি Workflow রান হতে দেখবেন।
4. **(ঐচ্ছিক — ফোন থেকে নিজে ম্যানুয়ালি Build চালু করতে চাইলে):**
   - **Actions** ট্যাবে যান → বাম পাশ থেকে **Build Android Debug APK** (অথবা **Build & Publish Release APK**) সিলেক্ট করুন।
   - ডানপাশে **Run workflow** বাটনে ট্যাপ করে সবুজ **Run workflow** বাটন চাপুন।

---

### Step 4: Build সফল হয়েছে কিনা কীভাবে বুঝবেন
* **হলুদ ঘূর্ণায়মান গোলক (🟡 In progress):** বর্তমানে Gradle ডিপেন্ডেন্সি ডাউনলোড ও APK কম্পাইল হচ্ছে (সাধারণত ২ থেকে ৪ মিনিট সময় লাগে)।
* **সবুজ টিক চিহ্ন (✅ Success):** Build ১০০% সফল হয়েছে এবং `JARVIS-debug.apk` তৈরি হয়ে গেছে!
* **লাল ক্রস চিহ্ন (❌ Failure):** কোনো সমস্যা হয়েছে। ওই রানের ওপর ট্যাপ করে কোন ধাপে (`7. Build Debug APK with Gradle`) সমস্যা হয়েছে তার লগ দেখতে পাবেন।

---

### Step 5: APK কোথায় পাওয়া যাবে
Build সবুজ টিক (✅) পাওয়ার পর আপনি **দুই জায়গায়** APK পাবেন:
1. **GitHub Releases সেকশনে (ফোন ব্যবহারকারীদের জন্য সবচেয়ে সহজ — সরাসরি `.apk` ডাউনলোড, কোনো `.zip` আনজিপ করতে হয় না!)**
2. **GitHub Actions রান পেজের নিচে Artifacts সেকশনে (`JARVIS-debug-apk.zip`)**

---

### Step 6: GitHub Actions Artifact থেকে APK Download করার নিয়ম
1. GitHub-এর **Actions** ট্যাবে গিয়ে সর্বশেষ সবুজ টিক (✅) পাওয়া **Build Android Debug APK** রানটির ওপর ট্যাপ করুন।
2. পেজটির একদম নিচে স্ক্রল করে **Artifacts** সেকশনে যান।
3. **`JARVIS-debug-apk`** লেখার ওপর ট্যাপ করুন।
4. আপনার ফোনে `JARVIS-debug-apk.zip` ডাউনলোড হবে। ফোনের **Files by Google** বা ডিফল্ট File Manager দিয়ে `.zip` ফাইলটি Extract/Unzip করলেই ভেতরে **`JARVIS-debug.apk`** পেয়ে যাবেন।

---

### Step 7: GitHub-এর Releases Section থেকে সরাসরি APK Download করার নিয়ম (Unzip ছাড়াই!)
যেহেতু আপনি ফোন ব্যবহার করছেন, আপনার সুবিধার জন্য আমাদের Workflow স্বয়ংক্রিয়ভাবে **GitHub Releases**-এ সরাসরি `.apk` ফাইল আপলোড করে দেয়:
1. আপনার GitHub Repository-এর মূল পেজে (Code ট্যাবে) একটু নিচে স্ক্রল করে **Releases** সেকশনে ট্যাপ করুন (অথবা সরাসরি `https://github.com/mj3933899-code/Jarvis-OS-AI-/releases` লিংকে যান)।
2. সেখানে **"JARVIS Debug APK (Latest Auto-Build)"** (`latest-debug`) অথবা আপনার তৈরি করা রিলিজ ভার্সন (`v1.0.0`) দেখতে পাবেন।
3. **Assets** তালিকাটি খুলে সরাসরি **`JARVIS-debug.apk`** বা **`JARVIS-release.apk`**-এর ওপর ট্যাপ করুন।
4. কোনো `.zip` ছাড়াই সরাসরি `.apk` ফাইলটি আপনার ফোনে ডাউনলোড হবে!

---

### Step 8: Android ফোনে APK Install ও Test করার নিয়ম
1. ডাউনলোড হওয়া **`JARVIS-debug.apk`** বা **`JARVIS-release.apk`** ফাইলটির ওপর ট্যাপ করুন।
2. যদি ফোনে *"Install unknown apps"* বা *"Allow from this source"* পারমিশন চায়, তবে **Settings → Allow from this source** অন করে দিন।
3. **Install** বাটনে ট্যাপ করুন।
4. **Google Play Protect নোটিশ:** যেহেতু এটি Debug/Self-signed ডেভেলপার বিল্ড, তাই প্লে প্রোটেক্ট পপআপ দেখালে **"More details" → "Install anyway"** ট্যাপ করুন।
5. অ্যাপটি **Open** করুন, প্রয়োজনীয় পারমিশন দিন এবং **Settings → AI Provider & Gemini Model Configuration**-এ গিয়ে আপনার API Key বসিয়ে **Test Connection** বাটনে ট্যাপ করুন।

---

## 🔐 API Key ও GitHub Secrets নিরাপত্তা নির্দেশিকা (কোনো Hardcoded Key নেই)

### ১. অ্যাপের ভেতরে নিরাপদে API Key ব্যবহার (সবচেয়ে নিরাপদ পদ্ধতি)
* সোর্স কোডের ভেতরে কোনো Gemini, OpenAI, DeepSeek বা OpenRouter API Key হার্ডকোড করা নেই।
* ফোনে APK ইনস্টল করার পর অ্যাপের **Settings → AI Provider & Gemini Model Configuration** কার্ডে গিয়ে আপনার API Key ইনপুট দিন এবং **Save Config** ও **Test Connection** চাপুন।
* আপনার API Key শুধুমাত্র আপনার ফোনের লোকাল `DataStore`-এ সংরক্ষিত থাকবে এবং `x-goog-api-key` হেডারের মাধ্যমে সরাসরি অফিসিয়াল সার্ভারে যাবে—GitHub-এ কখনোই প্রকাশ পাবে না।

### ২. Release Signing-এর জন্য GitHub Secrets ব্যবহারের নিয়ম (ঐচ্ছিক)
ডিফল্টভাবে `release-apk.yml` প্রজেক্টের `debug.keystore.base64` ব্যবহার করে APK সাইন করে যাতে কোনো অতিরিক্ত সেটআপ ছাড়াই APK ইনস্টল করা যায়। আপনি যদি ভবিষ্যতে নিজস্ব কাস্টম Keystore ব্যবহার করতে চান:
1. GitHub Repository → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**-এ যান।
2. নিচের Secret নামগুলোতে আপনার Keystore তথ্য রাখতে পারেন (কখনোই সোর্স কোডে রাখবেন না):
   - `KEYSTORE_BASE64`: আপনার `.jks` বা `.keystore` ফাইলের Base64 স্ট্রিং।

---

## 📂 GitHub Repository-তে কোন ফাইলগুলো অবশ্যই রাখতে হবে এবং কোনগুলো রাখা যাবে না

### ✅ যে ফাইল ও ফোল্ডারগুলো অবশ্যই GitHub-এ থাকতে হবে:
* `.github/workflows/build-apk.yml` (Debug APK অটো-বিল্ড ওয়ার্কফ্লো)
* `.github/workflows/release-apk.yml` (Release ও Debug APK রিলিজ ওয়ার্কফ্লো)
* `app/build.gradle.kts` (অ্যাপ মডিউল কনফিগারেশন)
* `app/src/main/AndroidManifest.xml` (ম্যানিফেস্ট ও পারমিশন)
* `app/src/main/java/com/example/...` (সকল Kotlin সোর্স কোড)
* `app/src/main/res/...` (সকল আইকন, ক্যারেক্টার ইমেজ, থিম ও স্ট্রিং রিসোর্স)
* `app/src/test/java/com/example/ExampleRobolectricTest.kt` (ইউনিট ও রোবোলেকট্রিক টেস্ট)
* `gradle/libs.versions.toml` (ভার্সন ক্যাটালগ)
* `gradle/wrapper/gradle-wrapper.jar` এবং `gradle/wrapper/gradle-wrapper.properties` (Gradle 9.3.1 র‍্যাপার)
* `gradlew` এবং `gradlew.bat` (Gradle র‍্যাপার স্ক্রিপ্ট)
* `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` (রুট Gradle কনফিগারেশন)
* `debug.keystore.base64` (সাইনড APK তৈরির জন্য প্রয়োজনীয় বেস৬৪ কীস্টোর)
* `.gitignore`, `.env.example`, `metadata.json`, `README.md`

### ❌ যে ফাইল ও ফোল্ডারগুলো GitHub-এ আপলোড করা যাবে না (`.gitignore` দ্বারা স্বয়ংক্রিয়ভাবে বাদ দেওয়া হয়েছে):
* `.gradle/` এবং `.kotlin/` (Gradle ও Kotlin লোকাল ক্যাশ)
* `build/`, `app/build/`, `.build-outputs/` (কম্পাইলড বাইনারি ও লোকাল বিল্ড ক্যাশ)
* `local.properties` (লোকাল মেশিনের SDK পাথ)
* `.env` (আসল API Key বা গোপন টোকেন ফাইল)
* `.idea/`, `*.iml`, `.vscode/` (IDE ক্যাশ ফাইল)
* `*.jks`, `debug.keystore` (র বাইনারি কীস্টোর ফাইল)
