# GitHub Secrets و Variables — AI I.Z (أسماء فقط)

لا تضع أي قيم سرية في Git أو في هذا الملف.

## Workflow: Android CI (تلقائي على الدفع)

لا يتطلب أسرار إنتاج. يستخدم keystore **مؤقتاً** للتحقق من minify فقط.

## Workflow: Android signed release build

### Secrets (Repository secrets)
| الاسم | الغرض |
|--------|--------|
| `ANDROID_KEYSTORE_BASE64` | محتوى ملف `.jks`/`.keystore` مرمّز base64 |
| `ANDROID_STORE_PASSWORD` | كلمة مرور مخزن المفاتيح |
| `ANDROID_KEY_PASSWORD` | كلمة مرور مفتاح التوقيع |

تحقق من `app/build.gradle.kts` لاسم الـ key alias إن كان مطلوباً كخاصية Gradle منفصلة.

### Variables (Repository variables)
| الاسم | القيود |
|--------|---------|
| `AI_IZ_API_BASE_URL` | يجب أن يبدأ بـ `https://` بدون شرطة مائلة ختامية |
| `GOOGLE_WEB_CLIENT_ID` | شكل `….apps.googleusercontent.com` ويطابق `GOOGLE_OIDC_CLIENT_ID` على الخادم |

## تطابق OIDC (Android ↔ Backend)

| Android (وقت البناء) | Backend (بيئة التشغيل) |
|----------------------|-------------------------|
| `GOOGLE_WEB_CLIENT_ID` | `GOOGLE_OIDC_CLIENT_ID` — **نفس** Web client ID |
| `AI_IZ_API_BASE_URL` | أصل النشر لـ `ACCESS_TOKEN_ISSUER` (HTTPS في الإنتاج) |
| — | `ACCESS_TOKEN_AUDIENCE` (مثال شائع: `ai-iz-android`) يجب أن يطابق ما يتحقق منه الخادم عند `jwtVerify` |

الخادم يتحقق من Google ID token عبر `audience: GOOGLE_OIDC_CLIENT_ID`، ويوقّع access tokens بـ HS256 مع `issuer` و`audience` من البيئة.

## Backend / Runner (ليست GitHub Secrets بالضرورة)

استخدم أسرار منصة الاستضافة:
- `DATABASE_URL`
- `ACCESS_TOKEN_HMAC_SECRET_HEX` (64 خانة hex / 32 بايت)
- `ACCESS_TOKEN_ISSUER`, `ACCESS_TOKEN_AUDIENCE`
- `CORS_ORIGIN`
- Runner: `AI_IZ_API_BASE_URL`, `DATABASE_URL` (قاعدة منفصلة), `RUNNER_SCHEMA_USER_IDS`, `RUNNER_DATABASE_ISOLATED`

## ملاحظات أمان السجلات

- لا تسجّل Authorization headers أو refresh/access tokens أو `DATABASE_URL`.
- ProGuard في Release يزيل معظم `Log.v/d/i`؛ أخطاء Auth تسجّل الرسالة العامة دون محتوى الرمز.
