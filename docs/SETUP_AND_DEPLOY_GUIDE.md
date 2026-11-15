# دليل إعداد وتشغيل AI I.Z

هذا الدليل يخص مستودع `meteo2026net-oss/ai-i.z` وتطبيق Android الحالي. يعتمد تسجيل الدخول وبيانات المستخدم على خدمة AI I.Z وPostgreSQL، ولا يستخدم Firebase Authentication أو Firestore لهذا الغرض.

## 1. متطلبات التطوير

- JDK 21.
- Android SDK المتوافق مع compileSdk 36.
- Gradle 9.3.1 أو أحدث.
- Node.js 22 أو أحدث لاختبارات وخادم `backend/`.
- Android Studio حديث يدعم Kotlin وJetpack Compose.

لا يحتوي المستودع حالياً على Gradle Wrapper؛ استخدم Gradle مثبتاً محلياً أو بيئة CI إلى أن تتم إضافة Wrapper موثوق.

## 2. إعداد Google OIDC وخادم AI I.Z

1. أنشئ OAuth 2.0 Client من نوع Web application في Google Cloud Console.
2. استخدم Client ID نفسه في إعداد الخادم وفي بناء Android.
3. جهّز PostgreSQL 15 أو أحدث، وأنشئ قاعدة بيانات مخصصة للتطبيق.
4. طبّق المخطط:
   `psql "$DATABASE_URL" -f backend/db/schema.sql`
5. انسخ `backend/.env.example` إلى ملف بيئة محلي غير مرفوع إلى Git، واضبط القيم المطلوبة وفق `backend/README.md`.
6. أنشئ `ACCESS_TOKEN_HMAC_SECRET_HEX` عشوائياً بطول 32 بايت (64 خانة hex)، ولا تعِد استخدام قيمة المثال.
7. شغّل اختبارات الخادم قبل تشغيله: `npm install --prefix backend` ثم `npm test --prefix backend`.
8. انشر API على استضافة HTTPS موثوقة، ثم اضبط `ACCESS_TOKEN_ISSUER` و`ACCESS_TOKEN_AUDIENCE` بما يطابق إعدادات العميل والخادم.

لا تضع بيانات PostgreSQL أو مفاتيح التوقيع أو OAuth client secrets داخل APK أو Git.

## 3. إعداد Android

مرّر القيمتين وقت البناء كمتغيرات بيئة أو Gradle properties:

- `AI_IZ_API_BASE_URL`: عنوان HTTPS لخادم AI I.Z المنشور.
- `GOOGLE_WEB_CLIENT_ID`: Web OAuth Client ID المطابق لإعداد الخادم.

مثال PowerShell:

```powershell
$env:AI_IZ_API_BASE_URL = "https://api.example.invalid"
$env:GOOGLE_WEB_CLIENT_ID = "your-client-id.apps.googleusercontent.com"
gradle testDebugUnitTest
gradle assembleDebug
```

العنوان أعلاه مثال توضيحي وليس خدمة منشورة. لا تستخدمه في تشغيل حقيقي.

## 4. ربط GitHub ومفاتيح مزودي الذكاء الاصطناعي

1. أنشئ Fine-grained Personal Access Token مخصصاً وبأقل صلاحيات لازمة.
2. امنحه الوصول إلى المستودعات والوظائف المطلوبة فقط.
3. أدخل الرمز من شاشة ربط GitHub داخل التطبيق؛ لا تضعه في الكود أو ملفات المستودع.
4. ألغِ الرمز فوراً إذا ظهر في سجل أو مشاركة غير مقصودة.
5. أدخل مفاتيح مزودي الذكاء الاصطناعي عبر آليات الإعداد الآمنة المعتمدة في التطبيق، ولا ترفعها إلى Git.

لا تمنح صلاحية Admin أو Secrets أو إدارة إعدادات المستودع إلا لحاجة موثقة وموافقة صريحة.

## 5. حالة Backend Runner

أضيفت نواة خادم مستقلة في `runner/` (Node.js 22+) وتتحقق من كل طلب عبر `GET /api/me` في خدمة هوية AI I.Z؛ لا تفك JWT محلياً ولا تقبل رمزاً ثابتاً. توفر حالياً `GET /api/health` وفحص مخطط PostgreSQL في `GET /api/db/schema` ضمن مخطط `public` فقط، مع عرض metadata دون صفوف البيانات. اتصال Runner بقاعدة البيانات منفصل عن قاعدة الهوية ويجب أن يستخدم قاعدة مخصصة مع صلاحيات قراءة metadata فقط عند النشر.

تبقى `POST /api/tests/run` و`POST /api/db/query-readonly` و`POST /api/db/execute-approved` مغلقة وتعيد HTTP 501 عمداً. لا يوجد بعد عامل تنفيذ معزول أو محلل SQL موثوق أو سجل موافقات خادمي غير قابل للتلاعب؛ لذلك لا يجوز تفعيلها أو اعتبارها وظائف منجزة.

إعداد Runner:

1. انشر مجلد `runner/` كخدمة Node.js 22 مستقلة على HTTPS.
2. اضبط `DATABASE_URL` لقاعدة بيانات Runner المعزولة، و`AI_IZ_API_BASE_URL` لعنوان خدمة هوية AI I.Z المنشورة.
3. اضبط `RUNNER_DATABASE_ISOLATED=true` فقط بعد التحقق فعلياً من أن قاعدة البيانات معزولة عن أي قواعد محمية أو إنتاجية.
4. اجعل `RUNNER_SCHEMA_USER_IDS` قائمة بمعرفات حسابات المشغلين المصرح لهم فقط؛ اتركها فارغة لمنع استعراض المخطط عن الجميع.
5. استخدم مستخدم PostgreSQL محدود الصلاحيات، وTLS موثقاً، وسجلات لا تحتوي على رموز الجلسات أو بيانات الاتصال.
6. نفذ `npm install --prefix runner` ثم `npm test --prefix runner`.

راجع `docs/BACKEND_API_AND_POSTGRES.md` قبل أي نشر. لا تستخدم بيانات اتصال الإنتاج في الاختبارات.

## 6. تشغيل الاختبارات

```powershell
npm test --prefix backend
gradle testDebugUnitTest
gradle assembleDebug
```

ينفذ GitHub Actions اختبارات Backend مع PostgreSQL مؤقت، ويختبر Android ويبني APK تجريبياً. نجاح CI لا يثبت صحة إعداد OAuth الإنتاجي أو نشر API أو جاهزية Backend Runner.

## 7. بوابة ما قبل الإطلاق

- نجاح Android CI وBackend CI ورفع APK كـ artifact.
- اختبار تسجيل الدخول والاستعادة وتدوير refresh token والخروج على جهاز Android فعلي.
- نشر API على HTTPS وربطه بقاعدة PostgreSQL مؤمنة مع نسخ احتياطية واستعادة مجربة.
- اختبار عزل الحسابات وعدم الوصول إلى سجلات مستخدم آخر.
- اختبار إنشاء مشروع وحفظه واستعادته وتدفقات العمل الرئيسية.
- تنفيذ Backend Runner واختبار المصادقة والموافقات والعزل وحدود التنفيذ من جهة الخادم.
- مراجعة سياسة الخصوصية وحذف الحساب والبيانات والنسخ الاحتياطية.
- إنشاء إصدار Release موقّع بمفتاح محفوظ خارج المستودع، ثم اختبار التثبيت والتحديث.

نجاح بناء APK وحده لا يعني أن التكاملات الخارجية أو جميع وظائف المنتج جاهزة للإنتاج.
