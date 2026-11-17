# دليل التشغيل والنشر واختبار الجهاز — AI I.Z

يكمّل هذا الملف `SETUP_AND_DEPLOY_GUIDE.md` و`BACKEND_API_AND_POSTGRES.md` و`DELIVERY_AUDIT_FINAL_AR.md`.

## 1) بناء Debug قابل للتجربة

### بدون خادم (فحص الواجهة فقط)
- يمكن تثبيت APK Debug من CI وتصفح الشاشات بعد أي مسار ضيف محلي إن وُجد.
- **تسجيل الدخول السحابي لن ينجح** ما لم يُبنَ APK مع:
  - `AI_IZ_API_BASE_URL=https://…`
  - `GOOGLE_WEB_CLIENT_ID=….apps.googleusercontent.com`
- شاشة الدخول تعرض لافتة تحذير عند غياب أحد الإعدادين (بعد التحديث البرمجي لهذا الفرع).

### مع خادم محلي/منشور
```bat
set AI_IZ_API_BASE_URL=https://your-api.example
set GOOGLE_WEB_CLIENT_ID=your-id.apps.googleusercontent.com
scripts\windows\build-debug.bat
```
أو `./gradlew assembleDebug` بعد تصدير نفس المتغيرات.

قيود العميل: عنوان API يجب أن يبدأ بـ `https://` (وفي Debug فقط يُسمح بـ `http://10.0.2.2` لمحاكي Android).

## 2) Release APK/AAB إنتاج

Workflow: `.github/workflows/android-release.yml`

1. أضف GitHub **Secrets** (قيم حقيقية، ليست في Git):
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_STORE_PASSWORD`
   - `ANDROID_KEY_PASSWORD`
2. أضف GitHub **Variables**:
   - `AI_IZ_API_BASE_URL` (يجب HTTPS)
   - `GOOGLE_WEB_CLIENT_ID`
3. شغّل workflow يدوياً أو عبر الوسم المعتمد في الملف.
4. حمّل artifact `ai-iz-signed-release`.

**لا تستخدم** `ai-iz-release-ci-validation` أو keystore CI المؤقت كإصدار متجر.

## 3) Backend + PostgreSQL — نشر قابل للتكرار

### Migrations
1. أنشئ قاعدة فارغة.
2. طبّق المخطط الأساسي ثم الترحيلات بالترتيب:
   - `backend/db/schema.sql`
   - `backend/db/migrations/*.sql` (مرتبة حسب الاسم، مثل `002_studio_projects.sql`)
3. مثال:
```bash
export DATABASE_URL='postgresql://USER:PASS@HOST:5432/aiiz'
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f backend/db/schema.sql
for f in backend/db/migrations/*.sql; do psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$f"; done
```

### بيئة التشغيل
انسخ `backend/.env.example` إلى بيئة خاصة (systemd EnvironmentFile / منصة سحابية). الحد الأدنى:
- `DATABASE_URL`
- `GOOGLE_OIDC_CLIENT_ID`
- `ACCESS_TOKEN_HMAC_SECRET_HEX` (`openssl rand -hex 32`)
- `ACCESS_TOKEN_ISSUER` / `ACCESS_TOKEN_AUDIENCE`
- `CORS_ORIGIN` (أصول موثوقة فقط)

### فحوص الصحة
- `GET /api/health` — يجب أن يعيد حالة سليمة مع اتصال DB.
- `GET /api/me` مع Bearer صالح — بعد تسجيل الدخول.

```bash
curl -fsS "https://YOUR_API/api/health"
```

### نسخ احتياطي واستعادة
```bash
# نسخ
pg_dump --format=custom --file=aiiz-$(date -u +%Y%m%dT%H%M%SZ).dump "$DATABASE_URL"
# استعادة على قاعدة فارغة بنفس المخطط/الترحيلات أولاً إن لزم
pg_restore --clean --if-exists --no-owner --dbname="$DATABASE_URL" aiiz-YYYYMMDD.dump
```
اختبر الاستعادة على بيئة staging قبل الإنتاج. لا تضع ملفات dump في Git.

## 4) Runner — نشر معزول

- قاعدة بيانات **منفصلة** عن هوية API.
- `AI_IZ_API_BASE_URL` يشير إلى هوية HTTPS.
- `RUNNER_DATABASE_ISOLATED=true` فقط بعد التحقق اليدوي من العزل.
- `RUNNER_SCHEMA_USER_IDS` قائمة مشغّلين موثوقين (فارغة = رفض فحص المخطط).
- المسارات التالية تبقى **501** ولا تُفعَّل في هذا الإصدار:
  - `POST /api/tests/run`
  - `POST /api/db/query-readonly`
  - `POST /api/db/execute-approved`

صحة Runner:
```bash
curl -fsS "https://YOUR_RUNNER/api/health"
```

## 5) المراقبة والسجلات

- لا تسجّل Authorization أو refresh tokens أو `DATABASE_URL` أو محتوى PAT.
- راقب: معدل 401/403، فشل refresh reuse، زمن `/api/health`، أخطاء 5xx.
- أحداث التدقيق الأمنية في PostgreSQL (حذف حساب، قرارات موافقات) — راجع `PRIVACY_AND_DATA_DELETION_AR.md`.

## 6) قائمة اختبار جهاز Android حقيقي

- [ ] تثبيت Debug APK المبني بإعدادات API/OAuth الصحيحة
- [ ] ظهور شاشة الدخول؛ عند بناء بلا إعدادات تظهر لافتة نقص الإعداد
- [ ] تسجيل دخول Google ناجح ووصول للوحة
- [ ] تبديل اللغة ar/en وتغيّر RTL/LTR فوراً
- [ ] إنشاء مشروع Studio وحفظه وفتحه وتصدير ZIP
- [ ] تسجيل الخروج ثم الدخول مجدداً
- [ ] (اختياري) طلب حذف الحساب في بيئة غير إنتاجية والتحقق من إزالة البيانات
- [ ] عند قطع الشبكة تظهر رسالة فشل واضحة دون انهيار التطبيق
- [ ] رفض/تعطيل مسارات Runner الخطرة (501) إن وُصلت بالخادم

## 7) حواجز تحتاج مالك المشروع

- أسرار التوقيع ومتغيرات GitHub
- نطاق HTTPS وشهادات
- مشروع Google Cloud OAuth
- موافقة دمج PR #6 ونشر Production
