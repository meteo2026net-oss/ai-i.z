# METRA AI Developer Agent — Flutter Cross-Platform Specification

هذا المستند يوثق مواصفات ومعمارية تطبيق **METRA AI Developer Agent** بصيغة جاهزة للتحويل أو التطوير المتوازي على **Flutter & Dart** بنفس التصميم والشاشات الـ 16 وبنية Clean Architecture المطبقة في النسخة الأصلية من تطبيق Android.

## 1. الهيكل المعماري المقترح (Clean Architecture + Riverpod/BLoC)
- `lib/core/security/`: تشفير المفاتيح عبر `flutter_secure_storage`، وبوابة الصلاحيات الثلاثية (`ApprovalGuard`) لمنع تجاوز مستويات الموافقة ومنع الاتصال بقاعدة METRA القديمة.
- `lib/domain/`: كيانات `AgentTask`، `ApprovalRequest`، `ChatMessage`، `OperationLog`، `UserSettings`، و`AgentLoopEngine`.
- `lib/data/remote/`: عميل Dio لـ GitHub REST API، وعميل مزود الذكاء الاصطناعي (`GeminiProvider` / `OpenAiProvider`)، وعميل `BackendRunnerClient`.
- `lib/data/local/`: قاعدة `sqflite` / `drift` لحفظ نقاط التوقف المرحلية (`task_checkpoints`) وجدولة الخلفية عبر `workmanager`.
- `lib/presentation/`: 16 شاشة عربية كاملة بدعم `Directionality(textDirection: TextDirection.rtl)` وMaterial 3:
  1. شاشة تسجيل الدخول (Google Sign-In).
  2. شاشة ربط GitHub واختيار المستودع (`gfa437926-tech/metra2027` و`gfa437926-tech/metra-weather-intelligence`).
  3. شاشة إعداد مزود الذكاء الاصطناعي ورابط Backend.
  4. لوحة التحكم الرئيسية مع رسوم بيانية ومؤشرات الحالة.
  5. شاشة المحادثة مع الوكيل (Streaming + أوامر سريعة).
  6. شاشة المهام وخطة التنفيذ.
  7. مستكشف ملفات المشروع.
  8. محرر ومراجع الأكواد ومقارنة التغييرات.
  9. شاشة GitHub والفروع والـ commits.
  10. شاشة Pull Requests ومراجعتها.
  11. شاشة GitHub Actions.
  12. شاشة قاعدة البيانات (PostgreSQL/Supabase المعزولة).
  13. شاشة SQL والمراجعة الأمنية.
  14. شاشة الاختبارات.
  15. سجل العمليات وتصدير التقارير.
  16. إعدادات الأمان والصلاحيات والوضع الداكن/الفاتح.

## 2. حزم `pubspec.yaml` الموصى بها
- `flutter_riverpod` أو `flutter_bloc` لإدارة الحالة.
- `go_router` للتنقل بين الشاشات الـ 16.
- `dio` + `retrofit` للاتصال بـ GitHub وBackend.
- `flutter_secure_storage` لتشفير GitHub PAT ومفاتيح API.
- `drift` (SQLite) + `workmanager` لاستئناف المهام في الخلفية.
- `firebase_core` + `firebase_auth` + `cloud_firestore` + `firebase_ai`.
