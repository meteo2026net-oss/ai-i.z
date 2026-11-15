# Backend Runner API Contract & PostgreSQL Isolated Migrations

## 1. عقد واجهات خادم التنفيذ (Backend Runner API)
يتصل تطبيق الهاتف بعنوان Backend عبر HTTPS مع ترويسة `Authorization: Bearer <BACKEND_TOKEN>`.

### نقاط النهاية المطلوبة:
1. `GET /api/health`
   - الاستجابة: `{ "status": "ok", "version": "1.0.0", "dotnetAvailable": true, "flutterAvailable": true, "postgresConnected": true, "activeDatabaseName": "metra_2027_v2", "lastCheckUtc": "..." }`
2. `POST /api/tests/run`
   - الطلب: `{ "repo": "gfa437926-tech/metra2027", "branch": "main", "target": "all" }`
   - الاستجابة: `{ "runId": "...", "passedCount": 42, "failedCount": 0, "skippedCount": 1, "durationMs": 5200, "summary": "...", "cases": [...] }`
3. `GET /api/db/schema`
   - يرجع بنية الجداول والأعمدة والعلاقات والفهارس وحالة RLS من قاعدة METRA الجديدة المعزولة فقط.
4. `POST /api/db/query-readonly`
   - ينفذ استعلامات `SELECT` / `EXPLAIN` للقراءة فقط عبر مستخدم PostgreSQL بصلاحيات قراءة منفصلة (`metra_readonly_role`).
5. `POST /api/db/execute-approved`
   - يتطلب `approvalId` ومعاينة أثر صريحة لتنفيذ تغييرات المستوى الثالث المعتمدة من المستخدم.

## 2. مقترح Migration مبدئي لقاعدة METRA 2027 الجديدة (بيئة معزولة)
> تحذير: لا تقم أبداً بتشغيل أي ملف migration على قاعدة METRA القديمة.

```sql
-- Role separation: Read-Only vs Controlled Migration Role
CREATE ROLE metra_readonly_role NOINHERIT LOGIN PASSWORD 'CHANGE_ME_STRONG';
GRANT USAGE ON SCHEMA public TO metra_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO metra_readonly_role;

-- Example isolated audit & task telemetry table for METRA 2027
CREATE TABLE IF NOT EXISTS public.agent_execution_checkpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_slug TEXT NOT NULL,
    branch_name TEXT NOT NULL,
    task_title TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('queued', 'running', 'awaiting_approval', 'completed', 'failed')),
    progress_percent INTEGER NOT NULL DEFAULT 0 CHECK (progress_percent BETWEEN 0 AND 100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.agent_execution_checkpoints ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow authenticated service read"
ON public.agent_execution_checkpoints
FOR SELECT
TO authenticated
USING (true);
```
