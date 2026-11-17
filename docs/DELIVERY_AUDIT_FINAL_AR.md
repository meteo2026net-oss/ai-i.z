# حزمة التسليم النهائية — تدقيق مستقل AI-I.Z

**الفرع:** `feature/no-firebase-oidc-session-backend`  
**HEAD الموثّق:** `c8eac94`  
**آخر commit وظيفي ناجح:** `b35ee22`  
**CI الموثّق على HEAD:** [44077091710](https://github.com/meteo2026net-oss/ai-i.z/actions/runs/44077091710) (نجاح)  
**CI على b35ee22:** [44075694674](https://github.com/meteo2026net-oss/ai-i.z/actions/runs/44075694674) (نجاح)  
**تاريخ التدقيق:** 2026-11-17  

> لا يُعدّ هذا المستند موافقة على دمج PR #6 أو نشر Production.  
> مخرجات CI الموقّعة بمفتاح مؤقت **ليست** إصدار إنتاج.

---

## 1) التصنيف

### أ) مكتمل ومختبر (أدلة CI خضراء)

| البند | الدليل |
|--------|--------|
| بناء Debug + unit tests Android | CI Android على `c8eac94` / `b35ee22` |
| بناء minified Release للتحقق (keystore مؤقت) | artifact `ai-iz-release-ci-validation` |
| Backend sessions / refresh rotation / reuse revocation | `backend/test/api.integration.test.js` في CI Backend |
| Logout وإبطال عائلة refresh | نفس الاختبارات التكاملية |
| حذف الحساب + تأكيد + إلغاء تعريف سجلات التدقيق | نفس الاختبارات |
| عزل tenant لمشاريع Studio | نفس الاختبارات |
| Studio lifecycle: create/list/open-race/save-conflict/edit/export/secrets/restore | `StudioProjectLifecycleTest` ضمن unit tests |
| توطين واجهات Compose الأساسية + بيئات Studio عبر طبقة عرض | `AppUiStrings` + اختبارات عدم تسرّب العربية في الوضع الإنجليزي |
| Gradle Wrapper 9.3.1 + `gradlew` / `gradlew.bat` | موجود في المستودع |
| سكربتات Windows | `scripts/windows/*.bat` + README |
| مسار Release workflow جاهز هيكلياً | `.github/workflows/android-release.yml` (يتطلب أسراراً) |
| تعطيل SQL / تنفيذ الأوامر / اختبارات بعيدة على Runner | موثّق ويعيد HTTP **501** عمداً |

### ب) مكتمل وينتظر إعداداً خارجياً

| البند | المتطلب الخارجي |
|--------|------------------|
| APK/AAB إنتاج موقّع بمفتاح حقيقي | Secrets التوقيع أدناه + تشغيل workflow `Android signed release build` |
| تسجيل دخول Google حقيقي على جهاز | `GOOGLE_WEB_CLIENT_ID` + OAuth consent + SHA-1 إن لزم |
| تشغيل API على HTTPS | نشر Backend + PostgreSQL + متغيرات البيئة |
| ربط Runner بالهوية | نشر `runner/` مع التحقق عبر `GET /api/me` |
| بناء Debug محلي على Windows بمصادقة كاملة | نفس متغيرات البناء + JDK 21 + Android SDK |

### ج) غير مكتمل (عمداً أو متبقٍ)

| البند | الحالة |
|--------|--------|
| تفعيل `POST` SQL / تنفيذ أوامر / تشغيل اختبارات بعيدة على Runner | **غير مكتمل عمداً** حتى عزل عامل + سياسة SQL + أقل صلاحيات |
| اختبارات جهاز حقيقي (UI/UX يدوي) | لم تُنفَّذ في هذه البيئة |
| عناوين `DevelopmentEnvironment.title` داخل الـ enum | ما زالت عربية كقيم توافق؛ **العرض** محلّى عبر `AppUiStrings.environmentTitle` بمفتاح `enum.name` |
| بعض نصوص مولّدة/داخلية (محتوى مشاريع مولّدة، prompts) | ليست طبقة UI أساسية |

### د) يحتاج تدخلك

1. توفير أسرار/متغيرات GitHub للإنتاج (بدون وضعها في Git).  
2. نشر API على نطاق HTTPS حقيقي وقاعدة PostgreSQL.  
3. ضبط Google Cloud OAuth (Web client) ومطابقة `GOOGLE_OIDC_CLIENT_ID` / `GOOGLE_WEB_CLIENT_ID`.  
4. الموافقة الصريحة على دمج PR #6 ثم على نشر Production.  
5. (اختياري لاحقاً) تصميم عزل Runner قبل أي تفعيل لمسارات 501.

---

## 2) روابط التسليم الحالية

| المخرج | المعرّف / الرابط |
|---------|------------------|
| CI على HEAD `c8eac94` | https://github.com/meteo2026net-oss/ai-i.z/actions/runs/44077091710 |
| CI وظيفي `b35ee22` | https://github.com/meteo2026net-oss/ai-i.z/actions/runs/44075694674 |
| Debug APK (`b35ee22`) | artifact **14325611608** (`ai-iz-debug-apk`) من run 44075694674 |
| Release CI validation | artifact **14325022429** (`ai-iz-release-ci-validation`) — **مفتاح مؤقت** |
| المصدر | https://github.com/meteo2026net-oss/ai-i.z/tree/feature/no-firebase-oidc-session-backend |
| PR #6 | https://github.com/meteo2026net-oss/ai-i.z/pull/6 (**غير مدمج**) |

---

## 3) قائمة Secrets و Variables المطلوبة (أسماء فقط — بلا قيم)

### GitHub Actions — workflow `Android signed release build`

**Secrets**
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`
- (واسم المفتاح إن كان workflow يتوقعه كـ `ANDROID_KEY_ALIAS` — راجع الملف عند الضبط)

**Variables**
- `AI_IZ_API_BASE_URL` (يجب `https://…`)
- `GOOGLE_WEB_CLIENT_ID` (شكل `*.apps.googleusercontent.com`)

### Backend / PostgreSQL (بيئة التشغيل — ليس Git)

- `DATABASE_URL`
- `GOOGLE_OIDC_CLIENT_ID`
- `ACCESS_TOKEN_HMAC_SECRET_HEX` (32 بايت عشوائية / 64 hex)
- `ACCESS_TOKEN_ISSUER`
- `ACCESS_TOKEN_AUDIENCE`
- `ACCESS_TOKEN_TTL_SECONDS`
- `REFRESH_TOKEN_TTL_DAYS`
- `CORS_ORIGIN`
- `PORT` / `NODE_ENV`

راجع: `backend/.env.example` و `docs/SETUP_AND_DEPLOY_GUIDE.md`.

### بناء Android محلي / Windows

- `AI_IZ_API_BASE_URL`
- `GOOGLE_WEB_CLIENT_ID`

سكربتات: `scripts/windows/build-debug.bat`, `setup-env.example.bat`, `build-release-ci.bat` (الأخير للتحقق بمفتاح مؤقت فقط).

---

## 4) أدلة التشغيل والنشر (موجودة)

- `docs/SETUP_AND_DEPLOY_GUIDE.md` — إعداد OIDC وPostgreSQL وAndroid وWindows  
- `docs/BACKEND_API_AND_POSTGRES.md` — هوية API + Runner و501  
- `docs/PRIVACY_AND_DATA_DELETION_AR.md` — الخصوصية وحذف البيانات  
- `scripts/windows/README.md` — بناء Windows  
- `docs/IMPLEMENTATION_READINESS_AR.md` — سجل الجاهزية التراكمي  

---

## 5) خلاصة المدقق

التطبيق على هذا الفرع **جاهز للتحقق المستمر والتسليم الهندسي** مع Debug APK وCI أخضر ومسارات خطرة معطّلة.  
**ليس** جاهزاً لإصدار متجر أو Production حتى تُوفَّر أسرار التوقيع والنشر والموافقة على الدمج.
