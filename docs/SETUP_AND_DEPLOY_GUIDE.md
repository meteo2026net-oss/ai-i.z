# دليل إعداد وتشغيل AI I.Z

هذا الدليل يخص مستودع `meteo2026net-oss/ai-i.z` وتطبيق Android الموجود فيه. لا تستخدم إعدادات مستودعات METRA القديمة أو رموز وصولها لهذا المشروع.

## 1. متطلبات التطوير

- JDK 21، وهو الإصدار المستخدم في GitHub Actions.
- Android SDK المتوافق مع compileSdk 36.
- Gradle 9.3.1 أو أحدث.
- Node.js 22 وFirebase CLI عند تشغيل اختبارات Firebase المحلية.
- Android Studio حديث يدعم Kotlin وJetpack Compose.

ملاحظة: المستودع لا يحتوي حالياً على Gradle Wrapper؛ لذلك استخدم Gradle مثبتاً محلياً أو بيئة CI إلى أن تتم إضافة Wrapper موثوق.

## 2. إعداد Firebase

1. أنشئ مشروع Firebase مخصصاً لتطبيق AI I.Z.
2. سجّل تطبيق Android بالمعرّف `com.metra.agent` (هذا هو applicationId الحالي في Gradle).
3. نزّل `google-services.json` المطابق وضعه في `app/google-services.json`.
4. فعّل طرق المصادقة التي تريد إتاحتها، ثم أنشئ Firestore.
5. راجع `firestore.rules` واختبر القواعد في Emulator قبل نشرها.
6. أعد إعداد App Check لمعرّف التطبيق وبيئة الإصدار الفعلية.

لا ترفع ملفات الخدمة الخاصة أو مفاتيح حسابات الخدمة أو رموز App Check أو ملفات التوقيع إلى Git.

## 3. ربط GitHub

1. أنشئ Fine-grained Personal Access Token مخصصاً وبأقل صلاحيات لازمة.
2. امنح الرمز وصولاً إلى المستودعات التي تريد إدارتها فقط.
3. استخدم صلاحيات Contents وPull requests بحسب الوظائف التي تحتاجها، وActions بصلاحية قراءة عند الحاجة لمشاهدة نتائج CI.
4. أدخل الرمز من شاشة ربط GitHub داخل التطبيق، ولا تضعه في الكود أو ملفات المستودع.
5. ألغِ الرمز فوراً إذا ظهر في سجل أو مشاركة غير مقصودة.

لا تمنح صلاحية Admin أو Secrets أو إدارة إعدادات المستودع إلا إذا كانت هناك حاجة موثقة وموافقة صريحة.

## 4. حالة Backend Runner

تطبيق Android يحتوي على عميل Retrofit وعقد API لخادم Backend Runner، لكنه لا يتضمن في هذا المستودع خادماً منشوراً يثبت جاهزية نقاط النهاية التالية:

- `GET /api/health`
- `POST /api/tests/run`
- `GET /api/db/schema`
- `POST /api/db/query-readonly`
- `POST /api/db/execute-approved`

لذلك لا تعتبر وظائف تشغيل الاختبارات عن بعد أو إدارة قواعد البيانات جاهزة للإنتاج قبل تنفيذ الخادم ونشره عبر HTTPS والتحقق من مصادقة المستخدم وصلاحياته وموافقاته على الخادم نفسه.

لا تستخدم عنواناً وهمياً أو رمزاً افتراضياً. لا تتصل بقاعدة إنتاج قبل اختبار بيئة معزولة ونسخة احتياطية.

## 5. تشغيل التطبيق والاختبارات

على Windows PowerShell:

```powershell
gradle testDebugUnitTest
gradle assembleDebug
```

على macOS/Linux:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

تتطلب الاختبارات التي تستخدم Firebase Auth وFirestore تشغيل المحاكيات:

```bash
firebase emulators:start --only auth,firestore --project demo-no-project
```

ويشغّل GitHub Actions الاختبارات باستخدام `firebase emulators:exec` ثم ينشئ APK تجريبياً ويرفعه كـ artifact عند نجاح المهمة.

## 6. قبل اعتبار التطبيق جاهزاً للإطلاق

- نجاح Android CI واختبارات الوحدة.
- تثبيت APK على جهاز فعلي واختبار تسجيل الدخول والتنقل.
- اختبار إنشاء مشروع وحفظه واستعادته.
- اختبار ربط GitHub مع رمز محدود الصلاحيات.
- إعداد Firebase الإنتاجي واختبار قواعد Firestore وApp Check.
- نشر Backend Runner واختبار المصادقة والموافقات من جهة الخادم.
- مراجعة سياسة الخصوصية وحذف الحساب والبيانات والنسخ الاحتياطية.
- إنشاء إصدار Release موقّع بمفتاح محفوظ خارج GitHub، ثم اختبار التحديث من إصدار سابق.

نجاح بناء APK وحده لا يعني أن التكاملات الخارجية أو جميع وظائف المنتج جاهزة للإنتاج.
