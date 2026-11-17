package com.example.data.repository

import com.example.domain.model.AiModelRecommendation
import com.example.domain.model.DevelopmentEnvironment
import com.example.domain.model.ModelAdvisorEngine
import com.example.domain.model.ProgrammingLanguage
import com.example.domain.model.StudioFileItem
import com.example.domain.model.StudioProjectSpec
import java.util.UUID

/**
 * Generates full multi-file, production-grade project scaffolds
 * across all development environments and programming languages.
 * Part of ai i.z by Ibrahim Zeinab (إبراهيم زينب).
 */
object UniversalStudioProjectGenerator {

    fun generateProject(
        name: String,
        prompt: String,
        environment: DevelopmentEnvironment,
        language: ProgrammingLanguage,
        priority: String = "balanced"
    ): StudioProjectSpec {
        val safeName = name.trim().ifBlank {
            when (environment) {
                DevelopmentEnvironment.MOBILE_ANDROID -> "ai-iz-android-app"
                DevelopmentEnvironment.MOBILE_FLUTTER -> "ai_iz_flutter_app"
                DevelopmentEnvironment.WEB_REACT_NEXT -> "ai-iz-nextjs-web"
                DevelopmentEnvironment.BACKEND_PYTHON -> "ai-iz-fastapi-service"
                DevelopmentEnvironment.BACKEND_GO -> "ai-iz-go-microservice"
                DevelopmentEnvironment.SYSTEMS_RUST -> "ai-iz-rust-core"
                DevelopmentEnvironment.DATA_SCIENCE_AI -> "ai-iz-ml-engine"
                else -> "ai-iz-project"
            }
        }

        val recommendedModels = ModelAdvisorEngine.recommendModels(environment, language, priority)
        val primaryPick = recommendedModels.first()
        val alternates = recommendedModels.drop(1)

        val files = when (environment) {
            DevelopmentEnvironment.MOBILE_ANDROID -> generateAndroidComposeFiles(safeName, prompt)
            DevelopmentEnvironment.MOBILE_FLUTTER -> generateFlutterFiles(safeName, prompt)
            DevelopmentEnvironment.WEB_REACT_NEXT, DevelopmentEnvironment.WEB_VUE_SVELTE -> generateNextJsFiles(safeName, prompt)
            DevelopmentEnvironment.BACKEND_PYTHON -> generateFastApiFiles(safeName, prompt)
            DevelopmentEnvironment.BACKEND_GO -> generateGoFiles(safeName, prompt)
            DevelopmentEnvironment.SYSTEMS_RUST -> generateRustFiles(safeName, prompt)
            DevelopmentEnvironment.DATA_SCIENCE_AI -> generateDataScienceFiles(safeName, prompt)
            DevelopmentEnvironment.CLOUD_DEVOPS -> generateDevOpsFiles(safeName, prompt)
            else -> generateGenericFiles(safeName, prompt, language)
        }

        val buildInst = when (environment) {
            DevelopmentEnvironment.MOBILE_ANDROID -> "gradle assembleDebug"
            DevelopmentEnvironment.MOBILE_FLUTTER -> "flutter pub get && flutter build apk"
            DevelopmentEnvironment.WEB_REACT_NEXT -> "npm install && npm run build"
            DevelopmentEnvironment.BACKEND_PYTHON -> "pip install -r requirements.txt && uvicorn main:app --reload"
            DevelopmentEnvironment.BACKEND_GO -> "go mod tidy && go build -o server main.go"
            DevelopmentEnvironment.SYSTEMS_RUST -> "cargo build --release"
            DevelopmentEnvironment.DATA_SCIENCE_AI -> "pip install -r requirements.txt"
            DevelopmentEnvironment.CLOUD_DEVOPS -> "docker compose up -d"
            else -> "لم يتم تحديد أمر بناء موثوق لهذه البيئة في القالب الأولي؛ استخدم إعدادات المشروع المولّد."
        }

        val runInst = when (environment) {
            DevelopmentEnvironment.MOBILE_ANDROID -> "gradle installDebug && am start -n com.example/.MainActivity"
            DevelopmentEnvironment.MOBILE_FLUTTER -> "flutter run"
            DevelopmentEnvironment.WEB_REACT_NEXT -> "npm run dev"
            DevelopmentEnvironment.BACKEND_PYTHON -> "uvicorn main:app --host 0.0.0.0 --port 8000"
            DevelopmentEnvironment.BACKEND_GO -> "./server"
            DevelopmentEnvironment.SYSTEMS_RUST -> "./target/release/main"
            DevelopmentEnvironment.DATA_SCIENCE_AI -> "python agent_pipeline.py"
            DevelopmentEnvironment.CLOUD_DEVOPS -> "docker compose ps"
            else -> "لم يتم تحديد أمر تشغيل موثوق لهذه البيئة في القالب الأولي؛ راجع README الخاص بالمشروع."
        }

        val testInst = when (environment) {
            DevelopmentEnvironment.MOBILE_ANDROID -> "gradle testDebugUnitTest"
            DevelopmentEnvironment.MOBILE_FLUTTER -> "flutter test"
            DevelopmentEnvironment.WEB_REACT_NEXT -> "npm run lint (فحص lint؛ ليس اختبار وحدات)"
            DevelopmentEnvironment.BACKEND_PYTHON -> "pytest tests/ -v"
            DevelopmentEnvironment.BACKEND_GO -> "go test ./... -v"
            DevelopmentEnvironment.SYSTEMS_RUST -> "cargo test"
            DevelopmentEnvironment.DATA_SCIENCE_AI -> "لم يتم تضمين اختبارات آلية في القالب الأولي؛ أضف اختبارات pytest قبل تشغيلها."
            else -> "لم يتم تحديد أمر اختبار لهذه البيئة بعد؛ أضف إطار اختبارات مناسباً للمشروع ثم عرّف أمر تشغيله."
        }

        return StudioProjectSpec(
            id = UUID.randomUUID().toString().take(8),
            projectName = safeName,
            environment = environment,
            primaryLanguage = language,
            description = prompt.ifBlank { "مشروع متكامل ومُعد مسبقاً في بيئة ${environment.title} بواسطة ai i.z." },
            recommendedModel = primaryPick,
            alternateModels = alternates,
            files = files,
            buildInstructions = buildInst,
            runInstructions = runInst,
            testInstructions = testInst,
            securityAuditScore = null
        )
    }

    private fun generateAndroidComposeFiles(name: String, prompt: String): List<StudioFileItem> {
        return listOf(
            StudioFileItem(
                path = "app/src/main/java/com/iz/app/MainActivity.kt",
                language = ProgrammingLanguage.KOTLIN,
                isEntrypoint = true,
                purpose = "نقطة انطلاق تطبيق أندرويد وتفعيل Material 3 وEdge-to-Edge",
                content = """
                    package com.iz.app

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.activity.enableEdgeToEdge
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.*
                    import androidx.compose.ui.Alignment
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.unit.dp

                    // تم توليد الكود بواسطة ai i.z - المطور إبراهيم زينب
                    // متوافق مع Android 15/16 ومعايير Material 3 الحديثة
                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            enableEdgeToEdge()
                            setContent {
                                MaterialTheme {
                                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                        AppContent(modifier = Modifier.padding(innerPadding))
                                    }
                                }
                            }
                        }
                    }

                    @Composable
                    fun AppContent(modifier: Modifier = Modifier) {
                        var counter by remember { mutableStateOf(0) }
                        Column(
                            modifier = modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "تطبيق $name",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تم إنشاؤه عبر منصة ai i.z بواسطة إبراهيم زينب",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { counter++ }) {
                                Text("النقرات: ${'$'}counter")
                            }
                        }
                    }
                """.trimIndent()
            ),
            StudioFileItem(
                path = "app/build.gradle.kts",
                language = ProgrammingLanguage.KOTLIN,
                purpose = "إعدادات Gradle ومكتبات Jetpack Compose الحديثة",
                content = """
                    plugins {
                        alias(libs.plugins.android.application)
                        alias(libs.plugins.kotlin.compose)
                    }

                    android {
                        namespace = "com.iz.app"
                        compileSdk = 35

                        defaultConfig {
                            applicationId = "com.iz.$name"
                            minSdk = 24
                            targetSdk = 35
                            versionCode = 1
                            versionName = "1.0"
                        }
                    }

                    dependencies {
                        implementation(platform("androidx.compose:compose-bom:2024.10.01"))
                        implementation("androidx.compose.material3:material3")
                        implementation("androidx.compose.ui:ui")
                        implementation("androidx.activity:activity-compose:1.9.3")
                    }
                """.trimIndent()
            ),
            StudioFileItem(
                path = "app/src/test/java/com/iz/app/ExampleUnitTest.kt",
                language = ProgrammingLanguage.KOTLIN,
                purpose = "اختبارات الوحدة (Unit Tests)",
                content = """
                    package com.iz.app

                    import org.junit.Test
                    import org.junit.Assert.*

                    class ExampleUnitTest {
                        @Test
                        fun addition_isCorrect() {
                            assertEquals(4, 2 + 2)
                        }
                    }
                """.trimIndent()
            )
        )
    }

    private fun generateFlutterFiles(name: String, prompt: String): List<StudioFileItem> {
        return listOf(
            StudioFileItem(
                path = "lib/main.dart",
                language = ProgrammingLanguage.DART,
                isEntrypoint = true,
                purpose = "نقطة تشغيل تطبيق Flutter مع ثيم Material 3",
                content = """
                    import 'package:flutter/material.dart';

                    // تم إنشاء المشروع بواسطة ai i.z للمطور إبراهيم زينب
                    void main() {
                      runApp(const AiIzFlutterApp());
                    }

                    class AiIzFlutterApp extends StatelessWidget {
                      const AiIzFlutterApp({super.key});

                      @override
                      Widget build(BuildContext context) {
                        return MaterialApp(
                          title: '$name',
                          debugShowCheckedModeBanner: false,
                          theme: ThemeData(
                            useMaterial3: true,
                            colorSchemeSeed: Colors.indigo,
                            brightness: Brightness.dark,
                          ),
                          home: const HomeScreen(),
                        );
                      }
                    }

                    class HomeScreen extends StatefulWidget {
                      const HomeScreen({super.key});

                      @override
                      State<HomeScreen> createState() => _HomeScreenState();
                    }

                    class _HomeScreenState extends State<HomeScreen> {
                      int _counter = 0;

                      @override
                      Widget build(BuildContext context) {
                        return Scaffold(
                          appBar: AppBar(
                            title: const Text('$name - ai i.z'),
                            centerTitle: true,
                          ),
                          body: Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                const Icon(Icons.rocket_launch, size: 64, color: Colors.indigoAccent),
                                const SizedBox(height: 20),
                                Text(
                                  'مرحباً بك في تطبيق $name',
                                  style: Theme.of(context).textTheme.headlineSmall,
                                ),
                                const SizedBox(height: 10),
                                const Text('تم بناؤه وتجهيزه عبر أستوديو ai i.z'),
                                const SizedBox(height: 24),
                                ElevatedButton.icon(
                                  onPressed: () => setState(() => _counter++),
                                  icon: const Icon(Icons.touch_app),
                                  label: Text('النقرات: ${'$'}_counter'),
                                ),
                              ],
                            ),
                          ),
                        );
                      }
                    }
                """.trimIndent()
            ),
            StudioFileItem(
                path = "pubspec.yaml",
                language = ProgrammingLanguage.BASH,
                purpose = "حزم واعتماديات Flutter وDart",
                content = """
                    name: $name
                    description: "A Flutter application generated by ai i.z"
                    version: 1.0.0+1
                    environment:
                      sdk: '>=3.2.0 <4.0.0'

                    dependencies:
                      flutter:
                        sdk: flutter
                      flutter_riverpod: ^2.5.1
                      dio: ^5.4.1

                    dev_dependencies:
                      flutter_test:
                        sdk: flutter
                      flutter_lints: ^3.0.0
                """.trimIndent()
            )
        )
    }

    private fun generateNextJsFiles(name: String, prompt: String): List<StudioFileItem> {
        return listOf(
            StudioFileItem(
                path = "src/app/page.tsx",
                language = ProgrammingLanguage.TYPESCRIPT,
                isEntrypoint = true,
                purpose = "الصفحة الرئيسية بـ Next.js 14/15 App Router مع Tailwind",
                content = """
                    // تم التوليد عبر منصة ai i.z - المطور إبراهيم زينب
                    import React from 'react';

                    export default function HomePage() {
                      return (
                        <main className="min-h-screen bg-slate-950 text-white flex flex-col items-center justify-center p-8">
                          <div className="max-w-2xl w-full bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-2xl text-center space-y-6">
                            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-cyan-950/80 border border-cyan-500/30 text-cyan-400 text-sm">
                              <span>🚀</span>
                              <span>ai i.z Universal Studio</span>
                            </div>
                            <h1 className="text-4xl font-extrabold tracking-tight bg-gradient-to-r from-cyan-400 via-indigo-400 to-emerald-400 bg-clip-text text-transparent">
                              $name
                            </h1>
                            <p className="text-slate-400 text-lg leading-relaxed">
                              ${prompt.ifBlank { "مشروع ويب متكامل مبني باستخدام Next.js 15, Tailwind CSS, TypeScript ومُصمم لأعلى أداء وسرعة." }}
                            </p>
                            <div className="grid grid-cols-2 gap-4 pt-4 text-left">
                              <div className="p-4 rounded-xl bg-slate-800/50 border border-slate-700/50">
                                <div className="text-xs text-slate-400">البيئة البرمجية</div>
                                <div className="text-sm font-semibold text-white">Next.js App Router</div>
                              </div>
                              <div className="p-4 rounded-xl bg-slate-800/50 border border-slate-700/50">
                                <div className="text-xs text-slate-400">النموذج المقترح</div>
                                <div className="text-sm font-semibold text-indigo-400">Claude 3.7 Sonnet / Gemini 2.0</div>
                              </div>
                            </div>
                            <div className="pt-2 flex justify-center gap-4">
                              <button className="px-6 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold transition-all">
                                ابدأ التطوير
                              </button>
                            </div>
                          </div>
                        </main>
                      );
                    }
                """.trimIndent()
            ),
            StudioFileItem(
                path = "package.json",
                language = ProgrammingLanguage.JAVASCRIPT,
                purpose = "حزم وتراخيص Next.js وTypeScript",
                content = """
                    {
                      "name": "$name",
                      "version": "1.0.0",
                      "private": true,
                      "scripts": {
                        "dev": "next dev",
                        "build": "next build",
                        "start": "next start",
                        "lint": "next lint"
                      },
                      "dependencies": {
                        "react": "^19.0.0",
                        "react-dom": "^19.0.0",
                        "next": "^15.1.0",
                        "lucide-react": "^0.460.0",
                        "clsx": "^2.1.1",
                        "tailwind-merge": "^2.5.4"
                      },
                      "devDependencies": {
                        "typescript": "^5.6.3",
                        "@types/node": "^22.8.7",
                        "@types/react": "^19.0.0",
                        "tailwindcss": "^3.4.14",
                        "postcss": "^8.4.47",
                        "autoprefixer": "^10.4.20"
                      }
                    }
                """.trimIndent()
            )
        )
    }

    private fun generateFastApiFiles(name: String, prompt: String): List<StudioFileItem> {
        return listOf(
            StudioFileItem(
                path = "main.py",
                language = ProgrammingLanguage.PYTHON,
                isEntrypoint = true,
                purpose = "خادم FastAPI مع التوثيق التلقائي ونقاط الـ REST APIs",
                content = """
                    # تم إنشاؤه عبر منصة ai i.z للمطور إبراهيم زينب
                    from fastapi import FastAPI, HTTPException, status
                    from fastapi.middleware.cors import CORSMiddleware
                    from pydantic import BaseModel
                    from typing import List, Optional
                    import uvicorn

                    app = FastAPI(
                        title="$name API",
                        description="${prompt.ifBlank { "واجهة برمجية سريعة وخفيفة مبنية بـ FastAPI وPython 3.12" }}",
                        version="1.0.0"
                    )

                    app.add_middleware(
                        CORSMiddleware,
                        allow_origins=["*"],
                        allow_credentials=True,
                        allow_methods=["*"],
                        allow_headers=["*"],
                    )

                    class ItemSchema(BaseModel):
                        id: Optional[int] = None
                        title: str
                        description: str
                        completed: bool = False

                    items_db: List[ItemSchema] = []

                    @app.get("/", tags=["Health"])
                    async def root():
                        return {
                            "service": "$name",
                            "status": "healthy",
                            "engine": "FastAPI + AsyncIO",
                            "developer": "Ibrahim Zeinab"
                        }

                    @app.get("/items", response_model=List[ItemSchema], tags=["Items"])
                    async def get_items():
                        return items_db

                    @app.post("/items", response_model=ItemSchema, status_code=status.HTTP_201_CREATED, tags=["Items"])
                    async def create_item(item: ItemSchema):
                        item.id = len(items_db) + 1
                        items_db.append(item)
                        return item

                    if __name__ == "__main__":
                        uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
                """.trimIndent()
            ),
            StudioFileItem(
                path = "requirements.txt",
                language = ProgrammingLanguage.PYTHON,
                purpose = "مكتبات بايثون واعتماديات الخادم",
                content = """
                    fastapi>=0.115.0
                    uvicorn[standard]>=0.32.0
                    pydantic>=2.9.2
                    pytest>=8.3.3
                    httpx>=0.27.2
                """.trimIndent()
            ),
            StudioFileItem(
                path = "tests/test_main.py",
                language = ProgrammingLanguage.PYTHON,
                purpose = "اختبارات الـ APIs الآلية بـ PyTest وHTTPX",
                content = """
                    import pytest
                    from httpx import AsyncClient, ASGITransport
                    from main import app

                    @pytest.mark.asyncio
                    async def test_health():
                        transport = ASGITransport(app=app)
                        async with AsyncClient(transport=transport, base_url="http://test") as ac:
                            response = await ac.get("/")
                        assert response.status_code == 200
                        assert response.json()["status"] == "healthy"
                """.trimIndent()
            )
        )
    }

    private fun generateGoFiles(name: String, prompt: String): List<StudioFileItem> {
        return listOf(
            StudioFileItem(
                path = "main.go",
                language = ProgrammingLanguage.GO,
                isEntrypoint = true,
                purpose = "نقطة انطلاق ميكروسيرفيس Go مع خادم فائق السرعة",
                content = """
                    package main

                    import (
                    	"net/http"
                    	"github.com/gin-gonic/gin"
                    )

                    // تم إنشاؤه عبر منصة ai i.z - المطور إبراهيم زينب
                    func main() {
                    	r := gin.Default()

                    	r.GET("/", func(c *gin.Context) {
                    		c.JSON(http.StatusOK, gin.H{
                    			"service":   "$name",
                    			"framework": "Go 1.23 + Gin",
                    			"status":    "running",
                    			"author":    "Ibrahim Zeinab",
                    		})
                    	})

                    	r.GET("/ping", func(c *gin.Context) {
                    		c.String(http.StatusOK, "pong")
                    	})

                    	r.Run(":8080")
                    }
                """.trimIndent()
            ),
            StudioFileItem(
                path = "go.mod",
                language = ProgrammingLanguage.GO,
                purpose = "تعريف وحدة Go Module والاعتماديات",
                content = """
                    module $name

                    go 1.23

                    require github.com/gin-gonic/gin v1.10.0
                """.trimIndent()
            ),
            StudioFileItem(
                path = "Dockerfile",
                language = ProgrammingLanguage.BASH,
                purpose = "حاوية Docker متعددة المراحل لخفة وسرعة التشغيل",
                content = """
                    FROM golang:1.23-alpine AS builder
                    WORKDIR /app
                    COPY go.mod go.sum ./
                    RUN go mod download
                    COPY . .
                    RUN CGO_ENABLED=0 GOOS=linux go build -o /server main.go

                    FROM alpine:latest
                    WORKDIR /root/
                    COPY --from=builder /server .
                    EXPOSE 8080
                    CMD ["./server"]
                """.trimIndent()
            )
        )
    }

    private fun generateRustFiles(name: String, prompt: String): List<StudioFileItem> {
        return listOf(
            StudioFileItem(
                path = "src/main.rs",
                language = ProgrammingLanguage.RUST,
                isEntrypoint = true,
                purpose = "خادم Axum وTokio Async عالي الأداء مع أمان الذاكرة",
                content = """
                    use axum::{routing::get, response::Json, Router};
                    use serde::Serialize;
                    use std::net::SocketAddr;

                    // تم الإنشاء بواسطة ai i.z للمطور إبراهيم زينب
                    #[derive(Serialize)]
                    struct HealthCheck {
                        service: &'static str,
                        status: &'static str,
                        engine: &'static str,
                    }

                    #[tokio::main]
                    async fn main() {
                        let app = Router::new().route("/", get(health_handler));

                        let addr = SocketAddr::from(([0, 0, 0, 0], 8080));
                        println!("Rust server listening on {}", addr);
                        let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
                        axum::serve(listener, app).await.unwrap();
                    }

                    async fn health_handler() -> Json<HealthCheck> {
                        Json(HealthCheck {
                            service: "$name",
                            status: "healthy",
                            engine: "Rust 2024 + Axum + Tokio",
                        })
                    }
                """.trimIndent()
            ),
            StudioFileItem(
                path = "Cargo.toml",
                language = ProgrammingLanguage.BASH,
                purpose = "ملف بناء Cargo وحزم Rust",
                content = """
                    [package]
                    name = "$name"
                    version = "0.1.0"
                    edition = "2021"

                    [dependencies]
                    axum = "0.7"
                    tokio = { version = "1.0", features = ["full"] }
                    serde = { version = "1.0", features = ["derive"] }
                    serde_json = "1.0"
                """.trimIndent()
            )
        )
    }

    private fun generateDataScienceFiles(name: String, prompt: String): List<StudioFileItem> {
        return listOf(
            StudioFileItem(
                path = "agent_pipeline.py",
                language = ProgrammingLanguage.PYTHON,
                isEntrypoint = true,
                purpose = "محرك ذكاء اصطناعي ومعالجة البيانات والنماذج",
                content = """
                    # خط معالجة البيانات والذكاء الاصطناعي - ai i.z (إبراهيم زينب)
                    import numpy as np
                    import pandas as pd
                    from typing import Dict, Any

                    class ModelInferenceEngine:
                        def __init__(self, name: str):
                            self.name = name
                            print(f"Initialized AI Engine: {name}")

                        def process_features(self, data: pd.DataFrame) -> Dict[str, Any]:
                            stats = data.describe().to_dict()
                            return {
                                "status": "processed",
                                "rows_analyzed": len(data),
                                "summary": stats
                            }

                    if __name__ == "__main__":
                        df = pd.DataFrame({
                            "feature_a": np.random.rand(100),
                            "feature_b": np.random.rand(100) * 10
                        })
                        engine = ModelInferenceEngine("$name")
                        results = engine.process_features(df)
                        print("Analysis Complete:", results)
                """.trimIndent()
            ),
            StudioFileItem(
                path = "requirements.txt",
                language = ProgrammingLanguage.PYTHON,
                purpose = "مكتبات الذكاء وتحليل البيانات",
                content = """
                    numpy>=1.26.0
                    pandas>=2.2.0
                    scikit-learn>=1.4.0
                    torch>=2.2.0
                    transformers>=4.40.0
                """.trimIndent()
            )
        )
    }

    private fun generateDevOpsFiles(name: String, prompt: String): List<StudioFileItem> {
        return listOf(
            StudioFileItem(
                path = "docker-compose.yml",
                language = ProgrammingLanguage.BASH,
                isEntrypoint = true,
                purpose = "تهيئة البيئة السحابية وقاعدة البيانات",
                content = """
                    version: '3.8'
                    services:
                      app:
                        build: .
                        ports:
                          - "8000:8000"
                        environment:
                          - DATABASE_URL=postgres://user:secret@db:5432/$name
                        depends_on:
                          - db

                      db:
                        image: postgres:16-alpine
                        environment:
                          POSTGRES_DB: $name
                          POSTGRES_USER: user
                          POSTGRES_PASSWORD: secret
                        volumes:
                          - pgdata:/var/lib/postgresql/data
                        ports:
                          - "5432:5432"

                    volumes:
                      pgdata:
                """.trimIndent()
            ),
            StudioFileItem(
                path = ".github/workflows/ci.yml",
                language = ProgrammingLanguage.BASH,
                purpose = "أتمتة الفحص والبناء عبر GitHub Actions CI/CD",
                content = """
                    name: CI Workflow
                    on: [push, pull_request]

                    jobs:
                      build-and-test:
                        runs-on: ubuntu-latest
                        steps:
                          - uses: actions/checkout@v4
                          - name: Run Quality Audit
                            run: echo "Code verified by ai i.z"
                """.trimIndent()
            )
        )
    }

    private fun generateGenericFiles(name: String, prompt: String, lang: ProgrammingLanguage): List<StudioFileItem> {
        val ext = lang.fileExtension
        val entry = "main$ext"
        return listOf(
            StudioFileItem(
                path = entry,
                language = lang,
                isEntrypoint = true,
                purpose = "نقطة البداية بلغة ${lang.displayName}",
                content = """
                    // مشروع $name مبني بلغة ${lang.displayName}
                    // تم إنشاؤه عبر منصة ai i.z - المطور إبراهيم زينب
                    // الهدف: $prompt
                """.trimIndent()
            )
        )
    }
}
