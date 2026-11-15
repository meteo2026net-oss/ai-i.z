package com.example.data.repository

object FlutterSpecExporter {

    fun buildFlutterBlueprintMarkdown(): String = """
        # METRA AI Developer Agent — مواصفات Flutter & Backend للتصدير

        ## 1. البنية المعمارية (Clean Architecture)
        - Presentation: RTL Arabic Material 3 (16 شاشة: تسجيل الدخول، ربط GitHub، إعداد AI، لوحة التحكم، المحادثة، المهام، مستكشف الملفات، محرر الأكواد والمقارنة، الفروع والـ commits، Pull Requests، GitHub Actions، قاعدة البيانات، مراجعة SQL والأمان، الاختبارات، سجل العمليات، إعدادات الصلاحيات).
        - Domain: محرك Agent Loop ذاتي مع حارس صلاحيات ثلاثي المستويات (T1 تلقائي، T2 موافقة قبل التنفيذ، T3 موافقة صريحة مستقلة) وعزل إلزامي لقاعدة METRA القديمة.
        - Data:
          * GitHub REST API v3 (gfa437926-tech/metra2027 و gfa437926-tech/metra-weather-intelligence)
          * AI Orchestrator (Firebase AI Logic Gemini 3.8 Flash / 4 Argon + OpenAI اختياري)
          * Firestore Cloud Sync + SQLite/Room Local Checkpoints + WorkManager Background Resume
          * Backend Runner API لتشغيل اختبارات .NET و Flutter وفحص PostgreSQL المعزول.

        ## 2. حزم pubspec.yaml المقترحة لنسخة Flutter
        - flutter_riverpod / flutter_bloc
        - go_router
        - dio + retrofit
        - flutter_secure_storage
        - drift (SQLite) + workmanager
        - firebase_core, firebase_auth, cloud_firestore, firebase_ai

        ## 3. نقاط نهاية Backend Runner المطلوبة
        - GET /api/health
        - POST /api/tests/run
        - GET /api/db/schema
        - POST /api/db/query-readonly
        - POST /api/db/execute-approved

        ## 4. مقترح PostgreSQL Migration للبيئة المعزولة الجديدة
        CREATE ROLE metra_readonly_role NOINHERIT LOGIN PASSWORD 'CHANGE_ME';
        GRANT USAGE ON SCHEMA public TO metra_readonly_role;
        GRANT SELECT ON ALL TABLES IN SCHEMA public TO metra_readonly_role;

        CREATE TABLE IF NOT EXISTS public.agent_execution_checkpoints (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            repo_slug TEXT NOT NULL,
            branch_name TEXT NOT NULL,
            task_title TEXT NOT NULL,
            status TEXT NOT NULL,
            progress_percent INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );
        ALTER TABLE public.agent_execution_checkpoints ENABLE ROW LEVEL SECURITY;
    """.trimIndent()
}
