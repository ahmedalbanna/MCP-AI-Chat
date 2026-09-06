# 🏗️ دليل وكيل تطوير تطبيقات Android Kotlin

  

> **النسخة:** 2.0  

> **الغرض:** دليل شامل لوكلاء الذكاء الاصطناعي عند بناء تطبيقات Android بلغة Kotlin مع Jetpack Compose  

> **آخر تحديث:** يوليو 2026

  

---

  

## 📑 فهرس المحتويات

  

- [مقدمة عامة](#مقدمة-عامة)

- [🏗️ القسم 1: العمارة (Architecture)](#️-القسم-1-العمارة-architecture)

- [🎨 القسم 2: نظام السمات (Theme System)](#-القسم-2-نظام-السمات-theme-system)

- [🌍 القسم 3: التصميم العربي (RTL)](#-القسم-3-التصميم-العربي-rtl)

- [📊 القسم 4: إدارة الحالة (State Management)](#-القسم-4-إدارة-الحالة-state-management)

- [🔄 القسم 5: Coroutines & Flow](#-القسم-5-coroutines--flow)

- [🧭 القسم 6: الملاحة (Navigation)](#-القسم-6-الملاحة-navigation)

- [💾 القسم 7: طبقة البيانات (Data Layer)](#-القسم-7-طبقة-البيانات-data-layer)

- [🧪 القسم 8: الاختبارات (Testing)](#-القسم-8-الاختبارات-testing)

- [⚡ القسم 9: الأداء (Performance)](#-القسم-9-الأداء-performance)

- [🔒 القسم 10: الأمان (Security)](#-القسم-10-الأمان-security)

- [📋 ملاحق](#-ملاحق)

  

> **ملاحظة:** إذا كنت تستخدم محرراً لا يدعم التنقل عبر الروابط العربية، استخدم البحث عن `## القسم` للانتقال بين الأقسام.

  

---

  

## مقدمة عامة

  

### عن هذا الدليل

  

هذا الدليل يجمّع أفضل الممارسات والإرشادات اللازمة لبناء تطبيقات Android احترافية باستخدام **Kotlin** و **Jetpack Compose**. يغطي كل شيء من العمارة الأساسية إلى التفاصيل الدقيقة للتصميم العربي.

  

### المبادئ العامة

  

1. **فكر خطوة بخطوة** قبل التنفيذ. قسم المهام المعقدة إلى خطوات صغيرة قابلة للتحقق.

2. **استخدم أنماط المشروع الحالية** — لا تقدم أنماطاً جديدة دون داعٍ.

3. **التزم بمبدأ المسؤولية الواحدة** لكل دالة وملف.

4. **لا تترك TODO comments** أو كوداً ميتاً أو `console.log`.

5. **اكتب الاختبارات** مع الكود — ليس بعد الكود.

6. **استخدم أسماء ذات معنى** للمتغيرات والدوال.

7. **لا تبالغ في التحسين** — اكتب كوداً صحيحاً أولاً.

  

### القواعد الذهبية العشر

  

| # | القاعدة |

|---|---------|

| 1 | لا تستخدم ألواناً أو أحجام خطوط ثابتة — استخدم `MaterialTheme.colorScheme` و `MaterialTheme.typography` |

| 2 | لا تستخدم `Color.Gray` أو `Color(0xFF…)` أو `fontSize = X.sp` مباشرة |

| 3 | كل تطبيق عربي يحتاج: RTL + خط عربي + أرقام شرقية + أيقونات AutoMirrored |

| 4 | `ViewModel` لا يحتفظ بمرجع للـ `Context` أو الـ `View` |

| 5 | `StateFlow` للـ UI، `SharedFlow` للأحداث لمرة واحدة |

| 6 | استخدم `collectAsStateWithLifecycle()` وليس `collectAsState()` |

| 7 | `Modifier` يكون أول باراميتر في الكومبوزابل بعد الـ `content` |

| 8 | الـ `Repository` هو المصدر الوحيد للحقيقة |

| 9 | استخدم `Sealed Class/Interface` للأحداث والـ UI State |

| 10 | اختبر كل Use Case و ViewModel و Repository |

  

---

  

## 🏗️ القسم 1: العمارة (Architecture)

  

### 1.1 Clean Architecture Overview

  

الهيكل الموصى به للمشروع:

  

```

app/

├── domain/                          # الطبقة الأساسية - لا تعتمد على أي إطار عمل

│   ├── model/                       # Domain Models (كائنات الأعمال)

│   ├── repository/                  # واجهات الـ Repository (Contracts)

│   └── usecase/                     # Use Cases (سيناريوهات الاستخدام)

│

├── data/                            # طبقة البيانات

│   ├── remote/                      # API, DTOs, Retrofit services

│   ├── local/                       # Room DAOs, DataStore, Preferences

│   └── repository/                  # تطبيق واجهات الـ Repository

│

└── presentation/                    # طبقة العرض

    ├── screen/                      # شاشات Compose

    ├── viewmodel/                   # ViewModels

    ├── state/                       # UI State Classes

    ├── component/                   # مكونات UI قابلة لإعادة الاستخدام

    └── navigation/                  # إعدادات التنقل

```

  

### 1.2 MVVM Pattern

  

```kotlin

// ✅ صحيح: ViewModel يدير الحالة فقط ولا يعرف شيئاً عن Compose

  

class DashboardViewModel @Inject constructor(

    private val getTransactionsUseCase: GetTransactionsUseCase

) : ViewModel() {

  

    private val _uiState = MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

  

    fun onEvent(event: DashboardEvent) {

        when (event) {

            is DashboardEvent.Refresh -> loadData()

            is DashboardEvent.FilterChanged -> applyFilter(event.filter)

        }

    }

  

    private fun loadData() {

        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            getTransactionsUseCase()

                .onSuccess { transactions ->

                    _uiState.update {

                        it.copy(isLoading = false, transactions = transactions)

                    }

                }

                .onFailure { error ->

                    _uiState.update {

                        it.copy(isLoading = false, error = error.message)

                    }

                }

        }

    }

}

  

// ✅ UI State باستخدام Data Class

data class DashboardUiState(

    val isLoading: Boolean = false,

    val transactions: List<Transaction> = emptyList(),

    val error: String? = null,

    val filter: FilterType = FilterType.ALL

)

  

// ✅ Events باستخدام Sealed Interface

sealed interface DashboardEvent {

    data object Refresh : DashboardEvent

    data class FilterChanged(val filter: FilterType) : DashboardEvent

}

  

// ✅ Product Success Pattern بدلاً من Result (للمشاريع الكبيرة)

sealed class Product<T> {

    data class Success<T>(val data: T) : Product<T>()

    data class Error<T>(val message: String, val exception: Throwable? = null) : Product<T>()

    data class Loading<T>(val cachedData: T? = null) : Product<T>()

}

```

  

### 1.3 Use Case Pattern

  

```kotlin

// ✅ صحيح: Use Case واحد مسؤولية واحدة

  

class GetTransactionsUseCase @Inject constructor(

    private val transactionRepository: TransactionRepository

) {

    suspend operator fun invoke(

        filter: FilterType = FilterType.ALL

    ): Product<List<Transaction>> {

        return try {

            val transactions = transactionRepository.getTransactions(filter)

            Product.Success(transactions)

        } catch (e: Exception) {

            Product.Error(e.message ?: "حدث خطأ غير متوقع")

        }

    }

}

```

  

### 1.4 Dependency Injection (Hilt)

  

```kotlin

// ✅ Hilt Modules منظمة حسب الطبقة

  

@Module

@InstallIn(SingletonComponent::class)

object RepositoryModule {

  

    @Provides

    @Singleton

    fun provideTransactionRepository(

        remoteDataSource: TransactionRemoteDataSource,

        localDataSource: TransactionLocalDataSource

    ): TransactionRepository {

        return TransactionRepositoryImpl(remoteDataSource, localDataSource)

    }

}

  

@Module

@InstallIn(SingletonComponent::class)

object NetworkModule {

  

    @Provides

    @Singleton

    fun provideRetrofit(): Retrofit {

        return Retrofit.Builder()

            .baseUrl(BuildConfig.BASE_URL)

            .client(provideOkHttpClient())

            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))

            .build()

    }

  

    @Provides

    @Singleton

    fun provideOkHttpClient(): OkHttpClient {

        return OkHttpClient.Builder()

            .connectTimeout(30, TimeUnit.SECONDS)

            .readTimeout(30, TimeUnit.SECONDS)

            .addInterceptor(HttpLoggingInterceptor().apply {

                level = if (BuildConfig.DEBUG)

                    HttpLoggingInterceptor.Level.BODY

                else

                    HttpLoggingInterceptor.Level.NONE

            })

            .build()

    }

}

```

  

### 1.5 إرشادات كتابة Use Cases

  

| الإرشاد | ✅ صحيح | ❌ خطأ |

|---------|---------|--------|

| طريقة الاستدعاء | `operator fun invoke()` | `fun execute()` / `fun run()` |

| نوع الدالة | `suspend` أو ترجع `Flow` | خلط `suspend` و `Flow` بدون داعٍ |

| التسمية | `GetTransactionByIdUseCase` | `TransactionUseCase` (غير محدد) |

| المسؤولية | Use Case واحد = عملية واحدة | Use Case واحد = عمليات متعددة |

| الإرجاع | `Product<T>` أو `Flow<T>` | إرجاع `null` للدلالة على الخطأ |

  

### 1.6 قائمة التحقق للعمارة

  

- [ ] الفصل التام بين الـ Domain و Frameworks

- [ ] جميع الـ Use Cases تستخدم `operator fun invoke()`

- [ ] الـ ViewModel لا يحتفظ بمرجع Context

- [ ] الـ Repository هو المصدر الوحيد للحقيقة

- [ ] استخدام `sealed interface` للأحداث

- [ ] استخدام `data class` للـ UI State

- [ ] حقن التبعيات عبر Hilt/Koin — لا `new` مباشر

- [ ] لا منطق أعمال في الـ Composable

  

---

  

## 🎨 القسم 2: نظام السمات (Theme System)

  

### 2.1 القاعدة الذهبية

  

> **لا تستخدم أبداً `Color.Gray` أو `Color.Black` أو `Color.LightGray` أو `Color(0xFF…)` داخل الـ Composables.**  

> **لا تستخدم أبداً `fontSize = X.sp` أو `fontWeight = FontWeight.Bold` مباشرة على `Text`** — استخدم `style = MaterialTheme.typography.xxx` بدلاً من ذلك.

  

كل لون وحجم خط **يجب** أن يأتي من `MaterialTheme.colorScheme` أو `MaterialTheme.typography`.

  

### 2.2 نظام الألوان — Material 3 Roles

  

| الدور | الاستخدام |

|-------|-----------|

| `primary` | لون العلامة التجارية للأزرار النشطة والأيقونات |

| `onPrimary` | نص/أيقونات **فوق** خلفية `primary` |

| `primaryContainer` | خلفيات أساسية هادئة (بطاقات محددة، عناصر مميزة) |

| `onPrimaryContainer` | نص/أيقونات فوق `primaryContainer` |

| `secondary` | إجراءات أقل بروزاً، حدود الأزرار المخططة، أيقونات ثانوية |

| `onSecondary` | نص/أيقونات فوق `secondary` |

| `secondaryContainer` | خلفيات أقسام، بطاقات مع لمسة خفيفة |

| `onSecondaryContainer` | نص داخل `secondaryContainer` |

| `tertiary` | مؤشرات خطر متوسط / تحذير |

| `onTertiary` | نص/أيقونات فوق `tertiary` |

| `tertiaryContainer` | خلفية تحذير أو خطر متوسط |

| `background` | خلفية الشاشة (اللوحة الأساسية) |

| `onBackground` | النص الرئيسي مباشرة على `background` |

| `surface` | خلفية البطاقات، الحوارات، الأوراق السفلية |

| `onSurface` | النص الرئيسي داخل البطاقات والحوارات |

| `surfaceVariant` | خلفيات حاويات هادئة، فواصل أقسام |

| `onSurfaceVariant` | **النص الثانوي**، التسميات التوضيحية، البيانات الوصفية |

| `outline` | الحدود والفواصل (بارزة) |

| `outlineVariant` | حدود خفيفة، فواصل رفيعة |

| `error` | مؤشرات خطر عالي، إجراءات تدميرية |

| `onError` | نص/أيقونات فوق `error` |

| `errorContainer` | خلفية خطأ هادئة |

| `onErrorContainer` | نص فوق `errorContainer` |

  

#### شجرة القرار السريع للألوان

  

```

نص من أي نوع؟

├─ رئيسي / بارز → onSurface

├─ ثانوي / توضيحي → onSurfaceVariant

├─ على خلفية ملونة → on<Role> (مثال: onPrimary, onSecondary)

└─ خطأ / عاجل → error

  

خلفية من أي نوع؟

├─ مستوى الشاشة → background

├─ بطاقة / حوار → surface

├─ بطاقة مميزة → surfaceVariant أو primaryContainer.copy(alpha = 0.15f)

├─ صف خطأ → errorContainer

└─ فاصل / حد → outline أو outlineVariant

```

  

#### أمثلة: ❌ خطأ ✅ صحيح

  

```kotlin

// ❌ خطأ - ينكسر في الوضع الليلي

Text("Hello", color = Color.Gray)

Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)))

.border(1.dp, Color.LightGray)

  

// ✅ صحيح - يتكيف مع كل الأوضاع

Text("Hello", color = MaterialTheme.colorScheme.onSurfaceVariant)

Card(colors = CardDefaults.cardColors(

    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

))

.border(1.dp, MaterialTheme.colorScheme.outlineVariant)

```

  

### 2.3 الخطوط والطباعة (Typography)

  

#### Material 3 Type Scale

  

| النمط | الحجم | الوزن | الاستخدام النموذجي |

|-------|-------|-------|--------------------|

| `displayLarge` | 57sp | Normal | نصوص الشاشة الترحيبية |

| `displayMedium` | 45sp | Normal | عناوين كبيرة |

| `displaySmall` | 36sp | Normal | أرقام كبيرة |

| `headlineLarge` | 32sp | Normal | إجماليات لوحة المعلومات |

| `headlineMedium` | 28sp | Normal | عنوان الشاشة |

| `headlineSmall` | 24sp | Normal | عنوان فرعي |

| `titleLarge` | 22sp | Medium | عنوان شريط التطبيق |

| `titleMedium` | 16sp | Medium | عناوين الأقسام، عناوين الحوارات |

| `titleSmall` | 14sp | Medium | أسماء العملاء، عناوين عناصر القائمة |

| `bodyLarge` | 16sp | Normal | فقرات المحتوى الرئيسي |

| `bodyMedium` | 14sp | Normal | محتوى ثانوي، حالات فارغة |

| `bodySmall` | 12sp | Normal | تسميات توضيحية، بيانات وصفية |

| `labelLarge` | 14sp | Medium | أزرار، تسميات مؤكدة |

| `labelMedium` | 12sp | Medium | شرائح، أزرار صغيرة |

| `labelSmall` | 11sp | Medium | شارات، عدادات صغيرة |

  

#### أمثلة الاستخدام

  

```kotlin

// ❌ خطأ

Text("Client Name", fontWeight = FontWeight.Bold, fontSize = 14.sp)

Text("Section Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

  

// ✅ صحيح

Text("Client Name", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

Text("Section Title", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

Text("Subtitle text here", style = MaterialTheme.typography.bodyMedium)

```

  

#### خريطة تحويل الأحجام الشائعة

  

| القديم (Hardcoded) | البديل الصحيح |

|-------------------|---------------|

| `fontSize = 18.sp` | `MaterialTheme.typography.titleLarge` |

| `fontSize = 16.sp, Bold` | `titleMedium.copy(fontWeight = FontWeight.Bold)` |

| `fontSize = 14.sp, Bold` | `titleSmall.copy(fontWeight = FontWeight.Bold)` |

| `fontSize = 13.sp` | `bodyMedium` |

| `fontSize = 12.sp, Bold` | `labelLarge.copy(fontWeight = FontWeight.Bold)` |

| `fontSize = 12.sp` | `bodySmall` |

| `fontSize = 10.sp` | `labelSmall` |

  

### 2.4 أفضل ممارسات تعريف السمة

  

#### Colors.kt

  

```kotlin

// زوج واحد من الثوابت لكل دور: فاتح وداكن

val NaturalPrimary = Color(0xFF1A73E8)

val NaturalPrimaryDark = Color(0xFF8AB4F8)

  

val NaturalSecondary = Color(0xFF5F6368)

val NaturalSecondaryDark = Color(0xFF9AA0A6)

  

// ... إلخ لجميع الأدوار المستخدمة

```

  

#### Theme.kt

  

```kotlin

@Composable

fun MyApplicationTheme(

    darkTheme: Boolean = isSystemInDarkTheme(),

    content: @Composable () -> Unit

) {

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        MaterialTheme(

            colorScheme = colorScheme,

            typography = Typography,

            content = content

        )

    }

}

```

  

#### Type.kt

  

```kotlin

val Typography = Typography(

    displayLarge = TextStyle(

        fontFamily = FontFamily.Default,

        fontSize = 57.sp,

        lineHeight = 68.sp,

        letterSpacing = (-0.25).sp

    ),

    displayMedium = TextStyle(

        fontFamily = FontFamily.Default,

        fontSize = 45.sp,

        lineHeight = 56.sp

    ),

    headlineLarge = TextStyle(

        fontFamily = FontFamily.Default,

        fontSize = 32.sp,

        lineHeight = 48.sp,

        letterSpacing = 0.sp  // العربية لا تحتاج letter spacing كبير

    ),

    bodyLarge = TextStyle(

        fontFamily = FontFamily.Default,

        fontSize = 16.sp,

        lineHeight = 24.sp,

        letterSpacing = 0.5.sp

    ),

    // ... باقي الأنماط

)

```

  

### 2.5 قائمة التحقق لشاشة جديدة

  

- [ ] لا `Color.Gray` أو `Color.Black` أو `Color(0xFF…)`؟

- [ ] لا `fontSize = N.sp` مباشرة؟

- [ ] كل خلفية تستخدم `MaterialTheme.colorScheme.{role}`

- [ ] كل نص يستخدم `style = MaterialTheme.typography.{style}`

- [ ] كل حد يستخدم `outline` أو `outlineVariant`

- [ ] التجاوزات تستخدم `.copy()` — لا `fontWeight` منفصل

- [ ] الأيقونات تستخدم `tint` من السمة

- [ ] `Divider` يستخدم `outlineVariant.copy(alpha = 0.5f)`

  

---

  

## 🌍 القسم 3: التصميم العربي (RTL)

  

### 3.1 المبادئ الأساسية

  

عند تصميم تطبيق عربي، هناك **4 أركان أساسية** يجب ضبطها معاً:

  

| الركن | الوصف | الأولوية |

|-------|-------|----------|

| **RTL Layout** | اتجاه الصفحة من اليمين لليسار | 🔴 إجباري |

| **الخط العربي** | استخدام خط يدعم الحروف العربية بشكل صحيح | 🔴 إجباري |

| **الأرقام العربية الشرقية** | ٠١٢٣٤٥٦٧٨٩ بدلاً من 0123456789 | 🟡 ضروري للتطبيقات المالية |

| **الأيقونات الاتجاهية** | عكس الأيقونات التي تشير لاتجاه معين | 🟡 ضروري |

  

> **⚠️ تنبيه:** تجاهل أيٍّ من هذه الأركان يؤدي إلى تجربة مستخدم سيئة للمتحدثين بالعربية.

  

### 3.2 إعداد RTL

  

#### في Theme.kt (إجباري)

  

```kotlin

// ✅ صحيح

@Composable

fun MyApplicationTheme(content: @Composable () -> Unit) {

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        MaterialTheme(

            colorScheme = ...,

            typography = Typography,

            content = content

        )

    }

}

  

// ❌ خطأ - بدون CompositionLocalProvider

@Composable

fun MyApplicationTheme(content: @Composable () -> Unit) {

    MaterialTheme(...) { content() }

}

```

  

#### في AndroidManifest.xml (اختياري)

  

```xml

<application

    android:supportsRtl="true"

    ...>

```

  

#### في شاشة معينة (حالات خاصة)

  

```kotlin

CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

    // محتوى الشاشة فقط هنا

}

```

  

### 3.3 الخط العربي (Cairo Font)

  

#### إضافة ملف الخط

  

ضع ملف الخط في `res/font/`:

```

res/font/cairo_variable.ttf

```

  

#### تعريف FontFamily في Type.kt

  

```kotlin

val CairoFontFamily = FontFamily(

    Font(R.font.cairo_variable, FontWeight.Light),

    Font(R.font.cairo_variable, FontWeight.Normal),

    Font(R.font.cairo_variable, FontWeight.Medium),

    Font(R.font.cairo_variable, FontWeight.SemiBold),

    Font(R.font.cairo_variable, FontWeight.Bold),

)

  

val Typography = Typography(

    bodyLarge = TextStyle(fontFamily = CairoFontFamily, ...),

    bodyMedium = TextStyle(fontFamily = CairoFontFamily, ...),

    // ... جميع أنماط النصوص

)

```

  

#### ربط الخط في Theme.kt

  

```kotlin

MaterialTheme(

    typography = Typography,  // ✅ لا تنسى هذا

    ...

)

```

  

#### ملاحظات مهمة عن الخط

  

- **Cairo Variable Font**: إذا كان الخط من نوع Variable، قد لا يدعمه بعض إصدارات Android القديمة. جرب استخدام Cairo الثابت (non-variable) إذا ظهرت مشاكل.

- **Font Weight**: تأكد من أن ملف الخط يدعم جميع الأوزان التي تستخدمها (Light, Normal, Medium, SemiBold, Bold).

- **بدائل جيدة**: Tajawal, Almarai, Noto Naskh Arabic — كلها تدعم العربية والفارسية.

  

### 3.4 الأرقام العربية الشرقية

  

#### إنشاء أداة التحويل

  

```kotlin

// ArabicNumerals.kt - ملف مخصص للأرقام العربية

object ArabicNumerals {

    private val map = mapOf(

        '0' to '٠', '1' to '١', '2' to '٢', '3' to '٣',

        '4' to '٤', '5' to '٥', '6' to '٦', '7' to '٧',

        '8' to '٨', '9' to '٩'

    )

  

    fun convert(input: String): String =

        input.map { map[it] ?: it }.joinToString("")

  

    fun formatCurrency(amount: Double, symbol: String = "ر.ي"): String {

        val formatted = String.format("%,.0f", amount)

        return "${convert(formatted)} $symbol"

    }

  

    fun formatPercent(percent: Int): String =

        "${convert(percent.toString())}%"

  

    fun formatNumber(number: Long): String {

        val formatted = String.format("%,d", number)

        return convert(formatted)

    }

}

```

  

#### قواعد استخدام الأرقام

  

| النوع | أرقام غربية | أرقام شرق عربية | مثال |

|-------|-------------|-----------------|------|

| المبالغ النقدية | ❌ | ✅ | `١٬٥٠٠ ر.ي` |

| النسب المئوية | ❌ | ✅ | `١٥٪` |

| العدادات (عدد الحركات) | ❌ | ✅ | `٤٢ حركة` |

| أرقام الهواتف | ✅ | ❌ | `771111111` |

| التواريخ في واجهة API | ✅ | ❌ | `2024-01-15` |

| المدخلات النصية للأرقام | ✅ | ❌ | حقل إدخال المبلغ |

| الأرقام داخل جمل API/SDK | ✅ | ❌ | `userId = 42` |

| العملات الأجنبية ($, €) | ✅ | ❌ | `$1,500` |

  

#### أمثلة الاستخدام

  

```kotlin

// ✅ صحيح

Text(ArabicNumerals.formatCurrency(amount))

Text(ArabicNumerals.convert(stringResource(R.string.template, count)))

  

// ❌ خطأ

Text("${amount.toInt()} ر.ي")

Text("عدد العملاء: ${clientsCount}")

```

  

### 3.5 الأيقونات AutoMirrored

  

#### القاعدة الأساسية

  

في RTL، الأيقونات التي تشير إلى اتجاه معين يجب أن تنعكس تلقائياً:

  

```kotlin

// ✅ صحيح - أيقونة متجهة تنعكس تلقائياً في RTL

Icons.AutoMirrored.Filled.ArrowBack

Icons.AutoMirrored.Filled.ArrowForward

Icons.AutoMirrored.Filled.ExitToApp

Icons.AutoMirrored.Filled.ReceiptLong

Icons.AutoMirrored.Filled.List

Icons.AutoMirrored.Filled.Send

Icons.AutoMirrored.Filled.TrendingUp

Icons.AutoMirrored.Outlined.ListAlt

  

// ❌ خطأ - أيقونة لا تنعكس

Icons.Filled.ArrowBack

Icons.Default.ExitToApp

Icons.Default.ReceiptLong

```

  

#### الأيقونات التي لا تحتاج AutoMirrored

  

```

Icons.Default.Add (+)        Icons.Default.Settings (ترس)

Icons.Default.Search (بحث)   Icons.Default.Person (شخص)

Icons.Default.Store (متجر)   Icons.Default.Edit (قلم)

Icons.Default.Delete (🗑)    Icons.Default.Email (بريد)

Icons.Default.Phone (هاتف)   Icons.Default.Star (نجمة)

Icons.Default.Home (منزل)    Icons.Default.Favorite (قلب)

```

  

#### أيقونات يجب الحذر منها

  

| الخطأ | الصواب |

|-------|--------|

| `Icons.Default.ChevronRight` | `Icons.AutoMirrored.Filled.ChevronRight` |

| `Icons.Default.NavigateNext` | `Icons.AutoMirrored.Filled.NavigateNext` |

| `Icons.Default.KeyboardArrowRight` | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |

| `Icons.Default.ArrowDropDown` | `Icons.AutoMirrored.Filled.ArrowDropDown` |

  

### 3.6 محاذاة النصوص

  

```kotlin

// ✅ TextAlign يعمل بشكل صحيح مع RTL تلقائياً

Text(text = "نص عربي", textAlign = TextAlign.Center)  // محاذاة وسط

Text(text = "نص عربي")                                 // افتراضي = Start (يمين في RTL)

  

// ملاحظة: TextAlign.Start = يمين في RTL ويسار في LTR

// TextAlign.End = يسار في RTL ويمين في LTR

  

// ✅ للأرقام المخلوطة مع نص عربي

Text(

    text = "المجموع: ١٬٥٠٠ ر.ي",

    textDirection = TextDirection.Content

)

  

// ✅ عندما يكون النص رقماً إنجليزياً داخل سياق عربي

Row {

    Text("الرقم:", modifier = Modifier.weight(1f))

    Text(

        text = "771111111",

        modifier = Modifier.weight(1f),

        textAlign = TextAlign.End  // في RTL, End = يسار

    )

}

```

  

### 3.7 ترتيب المكونات (Layouts)

  

#### Row و Column

  

```kotlin

// ✅ صحيح - Arrangement.SpaceBetween يعمل بشكل صحيح في RTL

Row(

    modifier = Modifier.fillMaxWidth(),

    horizontalArrangement = Arrangement.SpaceBetween

) {

    Text("العنوان")     // يظهر في اليمين (Start)

    Text("القيمة")      // يظهر في اليسار (End)

}

  

// ✅ صحيح - Arrangement.spacedBy آمن في RTL

Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

    Button1   // يظهر في اليمين

    Button2   // بعده لليسار

}

  

// ❌ تجنب - Arrangement.Start/End قد يعطي نتائج غير متوقعة

```

  

#### RoundedCornerShape

  

```kotlin

// ✅ Start/End في RoundedCornerShape تنعكس تلقائياً

// في RTL: topStart = أعلى يمين, topEnd = أعلى يسار

RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)

RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)

```

  

#### Scaffold و TopAppBar

  

```kotlin

// ✅ في RTL: title في اليمين، actions في اليسار

TopAppBar(

    title = { Text("العنوان") },

    navigationIcon = { IconButton(...) },  // يظهر في اليمين

    actions = { IconButton(...) }           // تظهر في اليسار

)

```

  

#### AlertDialog

  

```kotlin

// ✅ في RTL: confirmButton في اليمين، dismissButton في اليسار

AlertDialog(

    confirmButton = { Text("تأكيد") },  // يمين

    dismissButton = { Text("إلغاء") }    // يسار

)

```

  

#### OutlinedTextField

  

```kotlin

// ✅ في RTL: leadingIcon يمين، trailingIcon يسار

OutlinedTextField(

    value = ...,

    leadingIcon = { Icon(Icons.Default.Search) },    // يمين

    trailingIcon = { IconButton(...) }                 // يسار

)

```

  

### 3.8 قائمة الأخطاء الشائعة

  

| # | الخطأ | ❌ | ✅ |

|---|-------|-----|------|

| 1 | نسيان تحويل الأرقام | `"${amount} ر.ي"` → `"1500 ر.ي"` | `ArabicNumerals.formatCurrency(amount)` → `"١٬٥٠٠ ر.ي"` |

| 2 | أيقونات بلا AutoMirrored | `Icons.Default.ArrowBack` | `Icons.AutoMirrored.Filled.ArrowBack` |

| 3 | افتراض LTR في التنسيق | `Row { Text("القيمة") Text("العنوان") }` | `Row { Text("القيمة") Text("العنوان") }` — القيمة يمين |

| 4 | RTL في شاشة دون أخرى | Screen A: RTL, Screen B: LTR | RTL في الـ Theme يغطي الجميع |

| 5 | تجاهل string resources | `Text("إجمالي الدخل")` | `Text(stringResource(R.string.total_income))` |

| 6 | أرقام عربية في API | `"٢٠٢٤-٠١-١٥"` | `"2024-01-15"` — أرقام غربية للـ API |

  

### 3.9 قائمة التحقق النهائية للتطبيقات العربية

  

#### 🔲 RTL Infrastructure

- [ ] `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` في Theme.kt

- [ ] `supportsRtl="true"` في AndroidManifest.xml

- [ ] جميع الشاشات ترث RTL تلقائياً

  

#### 🔲 الخط العربي

- [ ] ملف الخط مضاف في `res/font/`

- [ ] `FontFamily` معرف في Type.kt

- [ ] `Typography` مربوط في Theme.kt

- [ ] جميع الأوزان المستخدمة مدعومة

  

#### 🔲 الأرقام العربية الشرقية

- [ ] تم إنشاء أداة تحويل (مثل `ArabicNumerals.kt`)

- [ ] جميع المبالغ النقدية تستخدم `formatCurrency()`

- [ ] جميع النسب المئوية محولة

- [ ] جميع العدادات محولة

- [ ] أرقام الهواتف بقيت غربية

- [ ] التواريخ الداخلية بقيت غربية

- [ ] حقول الإدخال بقيت غربية

  

#### 🔲 الأيقونات

- [ ] `Icons.AutoMirrored.Filled.ExitToApp` بدلاً من `Icons.Default.ExitToApp`

- [ ] `Icons.AutoMirrored.Filled.ReceiptLong` بدلاً من `Icons.Default.ReceiptLong`

- [ ] `Icons.AutoMirrored.Filled.List` بدلاً من `Icons.Default.List`

- [ ] أي أيقونة اتجاهية أخرى تم تصحيحها

- [ ] الأيقونات المتناظرة (Settings, Add, Search) بقيت كما هي

  

#### 🔲 المحاذاة والتنسيق

- [ ] استخدام `Arrangement.SpaceBetween` في الصفوف

- [ ] `RoundedCornerShape` تستخدم `topStart/topEnd` بشكل صحيح

- [ ] `AlertDialog` أزرار التأكيد/الإلغاء في الموضع الصحيح

- [ ] `OutlinedTextField` الـ leading/trailing icons في الموضع الصحيح

- [ ] `BottomNavigation` ترتيب الأيقونات صحيح

  

### 3.10 مثال متكامل

  

```kotlin

@Composable

fun DashboardScreen(amount: Double, count: Int, percent: Int) {

    // ✅ RTL من Theme

  

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // عنوان

        Text(

            text = "لوحة المعلومات",

            style = MaterialTheme.typography.headlineMedium

        )

  

        // ✅ صف بمحاذاة SpaceBetween

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Text("إجمالي الدخل")

            // ✅ أرقام شرق عربية

            Text(ArabicNumerals.formatCurrency(amount))

        }

  

        // ✅ عداد

        Text(

            text = ArabicNumerals.convert(

                stringResource(R.string.transactions_count, count)

            )

        )

  

        // ✅ نسبة مئوية

        Text(ArabicNumerals.formatPercent(percent))

  

        // ✅ أيقونة AutoMirrored

        Icon(

            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,

            contentDescription = "الديون"

        )

    }

}

```

  

---

  

## 📊 القسم 4: إدارة الحالة (State Management)

  

### 4.1 State Hoisting

  

```kotlin

// ✅ صحيح: رفع الحالة للأعلى - الفصل بين المنطق والعرض

@Composable

fun TransactionScreen(

    viewModel: TransactionViewModel = hiltViewModel()

) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  

    TransactionContent(

        uiState = uiState,

        onEvent = viewModel::onEvent

    )

}

  

@Composable

private fun TransactionContent(

    uiState: TransactionUiState,

    onEvent: (TransactionEvent) -> Unit,

    modifier: Modifier = Modifier

) {

    // UI فقط - لا منطق هنا

    when {

        uiState.isLoading -> LoadingIndicator()

        uiState.error != null -> ErrorMessage(uiState.error!!, onRetry = { onEvent(TransactionEvent.Refresh) })

        else -> TransactionList(

            transactions = uiState.transactions,

            onTransactionClick = { ... },

            modifier = modifier

        )

    }

}

```

  

### 4.2 State Loading Pattern

  

```kotlin

// ✅ نمط موحد لحالات التحميل

@Composable

fun <T> StateHandler(

    state: Product<T>,

    onLoading: @Composable (cachedData: T?) -> Unit = { LoadingIndicator() },

    onError: @Composable (message: String, onRetry: () -> Unit) -> Unit,

    onSuccess: @Composable (data: T) -> Unit

) {

    when (state) {

        is Product.Loading -> onLoading(state.cachedData)

        is Product.Error -> onError(state.message, ...)

        is Product.Success -> onSuccess(state.data)

    }

}

```

  

### 4.3 Events/Actions Pattern

  

```kotlin

// ✅ استخدام Sealed Interface للأحداث

sealed interface TransactionEvent {

    data object LoadTransactions : TransactionEvent

    data class Search(val query: String) : TransactionEvent

    data class FilterByDate(val start: LocalDate, val end: LocalDate) : TransactionEvent

    data object Refresh : TransactionEvent

}

  

// ✅ استخدام One-Time Events مع Channel

sealed interface UiEvent {

    data class ShowSnackbar(val message: String) : UiEvent

    data class NavigateTo(val route: String) : UiEvent

    data object NavigateBack : UiEvent

}

  

@HiltViewModel

class TransactionViewModel @Inject constructor(

    private val getTransactionsUseCase: GetTransactionsUseCase

) : ViewModel() {

  

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)

    val uiEvent = _uiEvent.receiveAsFlow()

  

    private fun deleteTransaction(id: String) {

        viewModelScope.launch {

            deleteTransactionUseCase(id).onSuccess {

                _uiEvent.send(UiEvent.ShowSnackbar("تم الحذف بنجاح"))

                loadTransactions()

            }

        }

    }

}

  

// ✅ استقبال الحدث في الـ Composable

@Composable

fun TransactionScreen(viewModel: TransactionViewModel = hiltViewModel()) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

  

    LaunchedEffect(Unit) {

        viewModel.uiEvent.collect { event ->

            when (event) {

                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)

                is UiEvent.NavigateTo -> navController.navigate(event.route)

                is UiEvent.NavigateBack -> navController.popBackStack()

            }

        }

    }

    // ...

}

```

  

### 4.4 قواعد عامة لإدارة الحالة

  

| القاعدة | الشرح |

|---------|-------|

| **StateFlow للـ UI** | الحالة المستمرة — باستخدام `collectAsStateWithLifecycle()` |

| **SharedFlow للأحداث لمرة واحدة** | Snackbar, Navigation — عبر `Channel` |

| **State Hoisting** | لا تضع حالة قابلة للتغيير داخل الـ Composable |

| **Unidirectional Data Flow** | ViewModel ← State → Composable ← Events ← ViewModel |

| **Immutable State** | الـ UI State دائماً `data class` مع `val` فقط |

| **Screen is Stateful** | الـ Composable الرئيسي يتصل بـ ViewModel |

| **Content is Stateless** | الـ Composable الداخلي لا يعرف ViewModel |

  

---

  

## 🔄 القسم 5: Coroutines & Flow

  

### 5.1 Structured Concurrency

  

```kotlin

// ✅ استخدام viewModelScope للعمليات المرتبطة بدورة حياة الـ ViewModel

class TransactionViewModel @Inject constructor(

    private val repository: TransactionRepository

) : ViewModel() {

  

    // viewModelScope يلغى تلقائياً عند انتهاء ViewModel

    fun loadData() {

        viewModelScope.launch {

            // العمليات هنا

        }

    }

}

  

// ✅ استخدام lifecycleScope للعمليات المرتبطة بدورة حياة الـ Composable

@Composable

fun TransactionScreen() {

    val lifecycleOwner = LocalLifecycleOwner.current

  

    LaunchedEffect(Unit) {

        // يلغى تلقائياً عند مغادرة الشاشة

        viewModel.uiEvent.collect { event -> ... }

    }

}

```

  

### 5.2 Flow Patterns

  

```kotlin

// ✅ Repository يرجع Flow للمراقبة المستمرة

class TransactionRepositoryImpl @Inject constructor(

    private val api: TransactionApi,

    private val dao: TransactionDao

) : TransactionRepository {

  

    override fun getTransactions(): Flow<List<Transaction>> {

        return dao.getAllTransactions()

            .map { entities -> entities.map { it.toDomain() } }

            .catch { e ->

                Log.e("Repository", "Error fetching transactions", e)

                emit(emptyList())

            }

    }

  

    override fun refreshTransactions(): Flow<Product<List<Transaction>>> = flow {

        emit(Product.Loading())

        try {

            val remoteData = api.getTransactions()

            dao.clearAndInsert(remoteData.map { it.toEntity() })

            emit(Product.Success(dao.getAllTransactions().map { it.toDomain() }))

        } catch (e: Exception) {

            emit(Product.Error(e.message ?: "خطأ في الاتصال"))

        }

    }

}

  

// ✅ تحويل Flow إلى StateFlow في ViewModel

val uiState: StateFlow<DashboardUiState> = repository.getTransactions()

    .map { transactions ->

        DashboardUiState(transactions = transactions)

    }

    .stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5_000),

        initialValue = DashboardUiState()

    )

```

  

### 5.3 جمع الـ Flow في Compose

  

```kotlin

// ✅ صحيح - lifecycle-aware

@Composable

fun MyScreen() {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // يستمع فقط عندما يكون الـ Lifecycle في STATE_STARTED على الأقل

    // يتوقف تلقائياً عند الخلفية

}

  

// ❌ خطأ - غير lifecycle-aware

@Composable

fun MyScreen() {

    val uiState by viewModel.uiState.collectAsState()

    // يبقى يستمع حتى في الخلفية

}

```

  

### 5.4 معالجة الأخطاء

  

```kotlin

// ✅ نمط معالجة الأخطاء مع Flow

fun getData(): Flow<Product<List<Transaction>>> = flow {

    emit(Product.Loading())

    try {

        val data = api.getTransactions()

        emit(Product.Success(data.map { it.toDomain() }))

    } catch (e: IOException) {

        emit(Product.Error("تحقق من اتصال الإنترنت"))

    } catch (e: Exception) {

        emit(Product.Error("حدث خطأ غير متوقع"))

    }

}.flowOn(Dispatchers.IO)

  

// ✅ إعادة المحاولة مع Retry

fun getDataWithRetry(): Flow<Product<List<Transaction>>> = flow {

    emit(Product.Loading())

    val data = api.getTransactions()

    emit(Product.Success(data.map { it.toDomain() }))

}.retry(3) { cause ->

    cause is IOException && delay(1000) == Unit

}.flowOn(Dispatchers.IO)

```

  

### 5.5 Dispatchers

  

| المكان | الـ Dispatcher المناسب |

|--------|----------------------|

| استدعاء API | `Dispatchers.IO` |

| قاعدة بيانات (Room) | `Dispatchers.IO` (Room يتعامل معها تلقائياً في suspend functions) |

| عمليات كثيفة (CPU) | `Dispatchers.Default` |

| تحديث UI | لا حاجة — تلقائي في `viewModelScope` |

| ViewModel + Flow | `Dispatchers.Main` (أو `flowOn` للتحويل التلقائي) |

  

### 5.6 قواعد ذهبية لـ Coroutines & Flow

  

- **استخدم `flowOn(Dispatchers.IO)`** لعمليات الإدخال/الإخراج — في الـ Repository

- **لا تستخدم `GlobalScope` أبداً** — استخدم `viewModelScope` أو `lifecycleScope`

- **استخدم `Channel` للأحداث لمرة واحدة** و `StateFlow` للحالة المستمرة

- **استخدم `stateIn`** لتحويل Flow إلى StateFlow مع `WhileSubscribed`

- **تعامل مع الاستثناءات** بـ `.catch{}` — لا تترك Flow يموت بصمت

  

---

  

## 🧭 القسم 6: الملاحة (Navigation)

  

### 6.1 Compose Navigation

  

```kotlin

// ✅ تعريف المسارات باستخدام Sealed Class

sealed class Screen(val route: String) {

    data object Dashboard : Screen("dashboard")

    data object Transactions : Screen("transactions")

    data class TransactionDetail(val id: String) : Screen("transactions/{id}") {

        companion object {

            const val ROUTE = "transactions/{id}"

            fun createRoute(id: String) = "transactions/$id"

        }

    }

}

  

// ✅ إعداد NavHost

@Composable

fun AppNavHost(

    navController: NavHostController = rememberNavController(),

    modifier: Modifier = Modifier

) {

    NavHost(

        navController = navController,

        startDestination = Screen.Dashboard.route,

        modifier = modifier

    ) {

        composable(Screen.Dashboard.route) {

            DashboardScreen(

                onNavigateToTransactions = {

                    navController.navigate(Screen.Transactions.route)

                }

            )

        }

  

        composable(

            route = Screen.TransactionDetail.ROUTE,

            arguments = listOf(

                navArgument("id") { type = NavType.StringType }

            )

        ) { backStackEntry ->

            val id = backStackEntry.arguments?.getString("id") ?: return@composable

            TransactionDetailScreen(

                transactionId = id,

                onNavigateBack = { navController.popBackStack() }

            )

        }

  

        composable(Screen.Transactions.route) {

            TransactionsScreen(

                onTransactionClick = { id ->

                    navController.navigate(Screen.TransactionDetail.createRoute(id))

                }

            )

        }

    }

}

```

  

### 6.2 Type-Safe Navigation (مع Kotlin Serialization)

  

```kotlin

// ✅ النهج الحديث مع Kotlin Serialization

@Serializable

sealed class Route {

    @Serializable

    data object Dashboard : Route()

  

    @Serializable

    data object Transactions : Route()

  

    @Serializable

    data class TransactionDetail(val id: String) : Route()

}

  

// ✅ الاستخدام في NavHost

composable<Route.Dashboard> {

    DashboardScreen(

        onNavigateToTransactions = { navController.navigate(Route.Transactions) }

    )

}

  

composable<Route.TransactionDetail> { backStackEntry ->

    val detailRoute = backStackEntry.toRoute<Route.TransactionDetail>()

    TransactionDetailScreen(

        transactionId = detailRoute.id,

        onNavigateBack = { navController.popBackStack() }

    )

}

  

composable<Route.Transactions> {

    TransactionsScreen(

        onTransactionClick = { id ->

            navController.navigate(Route.TransactionDetail(id = id))

        }

    )

}

```

  

### 6.3 Navigation with Bottom Bar

  

```kotlin

// ✅ تعريف عناصر الشريط السفلي

sealed class BottomNavItem(

    val screen: Screen,

    val icon: ImageVector,

    val label: String

) {

    data object Dashboard : BottomNavItem(

        screen = Screen.Dashboard,

        icon = Icons.AutoMirrored.Filled.List,

        label = "الرئيسية"

    )

    data object Transactions : BottomNavItem(

        screen = Screen.Transactions,

        icon = Icons.AutoMirrored.Filled.ReceiptLong,

        label = "الحركات"

    )

}

  

// ✅ Scaffold + BottomBar

@Composable

fun AppScaffold() {

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

  

    Scaffold(

        bottomBar = {

            NavigationBar {

                val items = listOf(BottomNavItem.Dashboard, BottomNavItem.Transactions)

                items.forEach { item ->

                    NavigationBarItem(

                        icon = { Icon(item.icon, contentDescription = item.label) },

                        label = { Text(item.label) },

                        selected = currentRoute == item.screen.route,

                        onClick = {

                            navController.navigate(item.screen.route) {

                                popUpTo(navController.graph.findStartDestination().id) {

                                    saveState = true

                                }

                                launchSingleTop = true

                                restoreState = true

                            }

                        }

                    )

                }

            }

        }

    ) { innerPadding ->

        AppNavHost(

            navController = navController,

            modifier = Modifier.padding(innerPadding)

        )

    }

}

```

  

### 6.4 قواعد الملاحة

  

- **استخدم `popUpTo` لتجنب تكدس الـ Back Stack**

- **استخدم `launchSingleTop` للشاشات الرئيسية**

- **لا تمرر كائنات كبيرة في الـ Arguments** — استخدم IDs أو Shared ViewModel

- **اختبر التنقل** عبر `NavController` وهمي في اختبارات الـ UI

- **استخدم Type-Safe Navigation** مع Kotlin Serialization في المشاريع الجديدة

  

---

  

## 💾 القسم 7: طبقة البيانات (Data Layer)

  

### 7.1 Repository Pattern

  

```kotlin

// ✅ واجهة Repository في Domain

interface TransactionRepository {

    fun getTransactions(): Flow<List<Transaction>>

    suspend fun getTransactionById(id: String): Transaction?

    suspend fun refreshTransactions(): Product<Unit>

}

  

// ✅ تطبيق Repository في Data

class TransactionRepositoryImpl @Inject constructor(

    private val remoteDataSource: TransactionRemoteDataSource,

    private val localDataSource: TransactionLocalDataSource,

    private val networkMonitor: NetworkMonitor

) : TransactionRepository {

  

    override fun getTransactions(): Flow<List<Transaction>> {

        return localDataSource.getAllTransactions()

            .map { entities -> entities.map { it.toDomain() } }

    }

  

    override suspend fun refreshTransactions(): Product<Unit> {

        return try {

            if (!networkMonitor.isConnected) {

                return Product.Error("لا يوجد اتصال بالإنترنت")

            }

            val remoteData = remoteDataSource.getTransactions()

            localDataSource.clearAndInsert(remoteData.map { it.toEntity() })

            Product.Success(Unit)

        } catch (e: Exception) {

            Product.Error(e.message ?: "حدث خطأ")

        }

    }

}

```

  

### 7.2 Retrofit + Kotlin Serialization

  

```kotlin

// ✅ تعريف API Service

interface TransactionApi {

    @GET("api/v1/transactions")

    suspend fun getTransactions(

        @Query("page") page: Int = 1,

        @Query("limit") limit: Int = 20

    ): ApiResponse<List<TransactionDto>>

  

    @GET("api/v1/transactions/{id}")

    suspend fun getTransactionById(@Path("id") id: String): ApiResponse<TransactionDto>

  

    @POST("api/v1/transactions")

    suspend fun createTransaction(@Body body: CreateTransactionRequest): ApiResponse<TransactionDto>

}

  

// ✅ تعريف DTOs مع Kotlin Serialization

@Serializable

data class TransactionDto(

    @SerialName("id") val id: String,

    @SerialName("amount") val amount: Double,

    @SerialName("description") val description: String? = null,

    @SerialName("date") @Contextual val date: LocalDate,

    @SerialName("type") val type: TransactionTypeDto

)

  

@Serializable

data class ApiResponse<T>(

    @SerialName("success") val success: Boolean,

    @SerialName("data") val data: T? = null,

    @SerialName("message") val message: String? = null

)

```

  

### 7.3 Room Database

  

```kotlin

// ✅ Entity

@Entity(tableName = "transactions")

data class TransactionEntity(

    @PrimaryKey val id: String,

    val amount: Double,

    val description: String?,

    val date: Long,  // timestamp

    val type: String,

    val synced: Boolean = false

)

  

// ✅ DAO

@Dao

interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")

    fun getAllTransactions(): Flow<List<TransactionEntity>>

  

    @Query("SELECT * FROM transactions WHERE id = :id")

    suspend fun getTransactionById(id: String): TransactionEntity?

  

    @Insert(onConflict = OnConflictStrategy.REPLACE)

    suspend fun insertAll(transactions: List<TransactionEntity>)

  

    @Query("DELETE FROM transactions")

    suspend fun deleteAll()

  

    @Transaction

    suspend fun clearAndInsert(transactions: List<TransactionEntity>) {

        deleteAll()

        insertAll(transactions)

    }

}

  

// ✅ Database

@Database(

    entities = [TransactionEntity::class],

    version = 1,

    exportSchema = true

)

abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

}

```

  

### 7.4 DataStore — التفضيلات

  

```kotlin

// ✅ DataStore للتفضيلات

class UserPreferences @Inject constructor(

    private val dataStore: DataStore<Preferences>

) {

    companion object {

        private val LANGUAGE_KEY = stringPreferencesKey("language")

        private val THEME_KEY = stringPreferencesKey("theme_mode")

    }

  

    val language: Flow<String> = dataStore.data

        .map { preferences -> preferences[LANGUAGE_KEY] ?: "ar" }

  

    val themeMode: Flow<ThemeMode> = dataStore.data

        .map { preferences ->

            when (preferences[THEME_KEY]) {

                "dark" -> ThemeMode.DARK

                "light" -> ThemeMode.LIGHT

                else -> ThemeMode.SYSTEM

            }

        }

  

    suspend fun setLanguage(language: String) {

        dataStore.edit { preferences ->

            preferences[LANGUAGE_KEY] = language

        }

    }

  

    suspend fun setThemeMode(mode: ThemeMode) {

        dataStore.edit { preferences ->

            preferences[THEME_KEY] = mode.name.lowercase()

        }

    }

}

```

  

### 7.5 Mapping — DTO ↔ Entity ↔ Domain

  

```kotlin

// ✅ ممنوع وجود منطق تعيين في الـ Domain

// بيانات الخريطة تكون في نهاية اسم الملف

// TransactionMapper.kt - في Data Layer

  

fun TransactionDto.toEntity() = TransactionEntity(

    id = id,

    amount = amount,

    description = description.orEmpty(),

    date = date.toEpochDay(),

    type = type.name,

    synced = true

)

  

fun TransactionEntity.toDomain() = Transaction(

    id = id,

    amount = amount,

    description = description,

    date = LocalDate.ofEpochDay(date),

    type = TransactionType.valueOf(type)

)

  

fun TransactionDto.toDomain() = Transaction(

    id = id,

    amount = amount,

    description = description.orEmpty(),

    date = date,

    type = when (type) {

        TransactionTypeDto.INCOME -> TransactionType.INCOME

        TransactionTypeDto.EXPENSE -> TransactionType.EXPENSE

    }

)

```

  

---

  

## 🧪 القسم 8: الاختبارات (Testing)

  

### 8.1 Unit Testing — ViewModel

  

```kotlin

// ✅ استخدام MockK لاختبار ViewModel

class DashboardViewModelTest {

  

    private val getTransactionsUseCase: GetTransactionsUseCase = mockk()

    private lateinit var viewModel: DashboardViewModel

  

    @Before

    fun setup() {

        coEvery { getTransactionsUseCase() } returns Product.Success(sampleTransactions)

        viewModel = DashboardViewModel(getTransactionsUseCase)

    }

  

    @Test

    fun `when initialized then load transactions`() = runTest {

        // Act

        val state = viewModel.uiState.first()

  

        // Assert

        assertEquals(false, state.isLoading)

        assertEquals(sampleTransactions, state.transactions)

        assertEquals(null, state.error)

    }

  

    @Test

    fun `when refresh fails then show error`() = runTest {

        // Arrange

        coEvery { getTransactionsUseCase() } returns Product.Error("Network error")

  

        // Act

        viewModel.onEvent(DashboardEvent.Refresh)

        val state = viewModel.uiState.first { it.error != null }

  

        // Assert

        assertEquals("Network error", state.error)

    }

  

    @Test

    fun `when filter changed then update filter`() = runTest {

        // Arrange

        val filter = FilterType.EXPENSE

  

        // Act

        viewModel.onEvent(DashboardEvent.FilterChanged(filter))

        val state = viewModel.uiState.first()

  

        // Assert

        assertEquals(filter, state.filter)

    }

}

```

  

### 8.2 Unit Testing — Use Case

  

```kotlin

class GetTransactionsUseCaseTest {

  

    private val repository: TransactionRepository = mockk()

    private lateinit var useCase: GetTransactionsUseCase

  

    @Before

    fun setup() {

        useCase = GetTransactionsUseCase(repository)

    }

  

    @Test

    fun `returns success with transactions`() = runTest {

        // Arrange

        coEvery { repository.getTransactions(any()) } returns sampleTransactions

  

        // Act

        val result = useCase(FilterType.ALL)

  

        // Assert

        assertTrue(result is Product.Success)

        assertEquals(3, (result as Product.Success).data.size)

    }

  

    @Test

    fun `returns empty list when no transactions`() = runTest {

        // Arrange

        coEvery { repository.getTransactions(any()) } returns emptyList()

  

        // Act

        val result = useCase()

  

        // Assert

        assertTrue(result is Product.Success)

        assertTrue((result as Product.Success).data.isEmpty())

    }

  

    @Test

    fun `returns error when repository fails`() = runTest {

        // Arrange

        coEvery { repository.getTransactions(any()) } throws IOException("No internet")

  

        // Act

        val result = useCase()

  

        // Assert

        assertTrue(result is Product.Error)

        assertTrue((result as Product.Error).message.contains("internet"))

    }

}

```

  

### 8.3 Unit Testing — Repository

  

```kotlin

class TransactionRepositoryImplTest {

  

    private val remoteDataSource: TransactionRemoteDataSource = mockk()

    private val localDataSource: TransactionLocalDataSource = mockk()

    private val networkMonitor: NetworkMonitor = mockk()

    private lateinit var repository: TransactionRepositoryImpl

  

    @Before

    fun setup() {

        repository = TransactionRepositoryImpl(

            remoteDataSource, localDataSource, networkMonitor

        )

    }

  

    @Test

    fun `refreshTransactions fetches from remote and saves locally`() = runTest {

        // Arrange

        every { networkMonitor.isConnected } returns true

        coEvery { remoteDataSource.getTransactions() } returns sampleDtos

        coEvery { localDataSource.clearAndInsert(any()) } returns Unit

  

        // Act

        val result = repository.refreshTransactions()

  

        // Assert

        assertTrue(result is Product.Success)

        coVerify {

            remoteDataSource.getTransactions()

            localDataSource.clearAndInsert(any())

        }

    }

  

    @Test

    fun `refreshTransactions returns error when offline`() = runTest {

        // Arrange

        every { networkMonitor.isConnected } returns false

  

        // Act

        val result = repository.refreshTransactions()

  

        // Assert

        assertTrue(result is Product.Error)

        coVerify(exactly = 0) { remoteDataSource.getTransactions() }

        coVerify(exactly = 0) { localDataSource.clearAndInsert(any()) }

    }

}

```

  

### 8.4 UI Testing — Compose

  

```kotlin

// ✅ اختبار Composable

@RunWith(AndroidJUnit4::class)

class TransactionContentTest {

  

    @Test

    fun displays_transactions_when_loaded() {

        // Arrange

        val transactions = listOf(

            Transaction(id = "1", amount = 1000.0, description = "راتب", ...),

            Transaction(id = "2", amount = 500.0, description = "إيجار", ...)

        )

  

        // Act

        composeTestRule.setContent {

            MyApplicationTheme {

                TransactionContent(

                    uiState = DashboardUiState(transactions = transactions),

                    onEvent = {}

                )

            }

        }

  

        // Assert

        composeTestRule.onNodeWithText("راتب").assertIsDisplayed()

        composeTestRule.onNodeWithText("إيجار").assertIsDisplayed()

    }

  

    @Test

    fun shows_loading_indicator_when_loading() {

        // Act

        composeTestRule.setContent {

            MyApplicationTheme {

                TransactionContent(

                    uiState = DashboardUiState(isLoading = true),

                    onEvent = {}

                )

            }

        }

  

        // Assert

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()

    }

  

    @Test

    fun shows_error_message_when_error() {

        // Act

        composeTestRule.setContent {

            MyApplicationTheme {

                TransactionContent(

                    uiState = DashboardUiState(error = "حدث خطأ"),

                    onEvent = {}

                )

            }

        }

  

        // Assert

        composeTestRule.onNodeWithText("حدث خطأ").assertIsDisplayed()

    }

}

```

  

### 8.5 أدوات الاختبار

  

| الأداة | الاستخدام |

|--------|-----------|

| **JUnit 5** | إطار الاختبارات الأساسي |

| **MockK** | إنشاء Mock للـ Use Cases والـ Repository |

| **Turbine** | اختبار الـ Flow بسهولة |

| **Compose Test** | اختبار واجهات Compose |

| **Hilt Testing** | اختبار مع حقن التبعيات (يمكنك استخدام Koin للاختبار) |

| **Fakes** | إنشاء Fake Repository لاختبارات أسرع |

  

```kotlin

// ✅ استخدام Turbine مع Flow

@Test

fun `refresh emits loading then success`() = runTest {

    coEvery { repository.getTransactions() } returns sampleTransactions

  

    val flow = viewModel.refresh()

    flow.test {

        assertEquals(Product.Loading::class, awaitItem()::class)

        assertTrue(awaitItem() is Product.Success)

        awaitComplete()

    }

}

```

  

### 8.6 قواعد الاختبار

  

- **اختبر الـ ViewModel** — منطق الأعمال كله هنا

- **اختبر الـ Use Cases** — كل حالة استخدام على حدة

- **اختبر الـ Repository** — مع Fake أو Mock

- **اختبر الـ Composable** — الحالات الثلاث (تحميل، نجاح، خطأ)

- **لا تختبر الـ Framework** — لا تختبر Room أو Retrofit

- **تجنب معلمات الاختبار الديناميكية** — استخدم `@ParameterizedTest` عند الحاجة

  

---

  

## ⚡ القسم 9: الأداء (Performance)

  

### 9.1 منع إعادة التركيب (Recomposition)

  

```kotlin

// ✅ استخدام derivedStateOf للحسابات المشتقة

@Composable

fun TransactionList(transactions: List<Transaction>, searchQuery: String) {

    val filteredTransactions = remember(transactions, searchQuery) {

        derivedStateOf {

            if (searchQuery.isBlank()) transactions

            else transactions.filter { it.description.contains(searchQuery, ignoreCase = true) }

        }

    }

  

    LazyColumn {

        items(filteredTransactions.value, key = { it.id }) { transaction ->

            TransactionItem(transaction)

        }

    }

}

  

// ✅ استخدام remember مع key

@Composable

fun TransactionItem(transaction: Transaction) {

    val formattedDate = remember(transaction.date) {

        ArabicNumerals.convert(transaction.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))

    }

  

    Row(modifier = Modifier.fillMaxWidth()) {

        Text(formattedDate)

        Text(ArabicNumerals.formatCurrency(transaction.amount))

    }

}

  

// ✅ استخدام immutable lists

@Composable

fun TransactionList(transactions: List<Transaction>) {

    // ❌ هذا يسبب إعادة تركيب غير ضرورية

    // if (transactions.isNotEmpty()) { ... }

  

    // ✅ أفضل - استخدم الثبات

    val hasTransactions = remember(transactions) { transactions.isNotEmpty() }

    if (hasTransactions) { ... }

}

```

  

### 9.2 Lazy Lists

  

```kotlin

// ✅ تحسين LazyColumn

@Composable

fun TransactionList(

    transactions: List<Transaction>,

    onItemClick: (String) -> Unit

) {

    LazyColumn(

        modifier = Modifier.fillMaxSize(),

        contentPadding = PaddingValues(16.dp),

        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {

        // ✅ استخدم key لتحسين الأداء

        items(

            items = transactions,

            key = { it.id }

        ) { transaction ->

            TransactionItem(

                transaction = transaction,

                onClick = { onItemClick(transaction.id) }

            )

        }

  

        // ✅ footer للتحميل الإضافي

        if (hasMorePages) {

            item(key = "loading_footer") {

                LoadingIndicator(modifier = Modifier.fillMaxWidth())

            }

        }

  

        // ✅ Empty state

        if (transactions.isEmpty() && !isLoading) {

            item(key = "empty_state") {

                EmptyState(modifier = Modifier.fillMaxWidth())

            }

        }

    }

}

```

  

### 9.3 Image Loading (Coil)

  

```kotlin

@Composable

fun UserAvatar(

    imageUrl: String?,

    modifier: Modifier = Modifier

) {

    AsyncImage(

        model = ImageRequest.Builder(LocalContext.current)

            .data(imageUrl)

            .crossfade(true)

            .size(512)  // ✅ حدد الحجم المناسب

            .memoryCachePolicy(CachePolicy.ENABLED)

            .diskCachePolicy(CachePolicy.ENABLED)

            .build(),

        contentDescription = "صورة المستخدم",

        modifier = modifier.clip(CircleShape),

        contentScale = ContentScale.Crop,

        placeholder = painterResource(R.drawable.placeholder_user),

        error = painterResource(R.drawable.error_user)

    )

}

```

  

### 9.4 قواعد الأداء

  

| القاعدة | الشرح |

|---------|-------|

| **استخدم `key`** في LazyColumn/Row لمنع إعادة التركيب غير الضرورية |

| **استخدم `remember`** مع `key` للقيم المشتقة |

| **استخدم `derivedStateOf`** للحسابات المعتمدة على State |

| **تجنب `Modifier` غير الضرورية** — كل `Modifier` إضافي يكلف |

| **استخدم `snapshotFlow`** لتحويل State إلى Flow |

| **ضع الصور في أقرب حجم** لاحتياجاتك — لا تحمل صوراً أكبر |

| **استخدم `coil`** للصور — يدعم التخزين المؤقت |

  

---

  

## 🔒 القسم 10: الأمان (Security)

  

### 10.1 Encrypted SharedPreferences

  

```kotlin

// ✅ تخزين آمن للبيانات الحساسة

class SecurePreferences @Inject constructor(

    @ApplicationContext context: Context

) {

    private val masterKey = MasterKey.Builder(context)

        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)

        .build()

  

    private val securePreferences = EncryptedSharedPreferences.create(

        context,

        "secure_prefs",

        masterKey,

        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,

        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM

    )

  

    var authToken: String?

        get() = securePreferences.getString("auth_token", null)

        set(value) {

            securePreferences.edit().putString("auth_token", value).apply()

        }

  

    fun clear() {

        securePreferences.edit().clear().apply()

    }

}

```

  

### 10.2 Network Security

  

```kotlin

// ✅ OkHttp مع Interceptor للـ Auth

class AuthInterceptor @Inject constructor(

    private val securePreferences: SecurePreferences

) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val token = securePreferences.authToken

        val request = if (token != null) {

            chain.request().newBuilder()

                .addHeader("Authorization", "Bearer $token")

                .build()

        } else {

            chain.request()

        }

        return chain.proceed(request)

    }

}

  

// ✅ Certificate Pinning (للإنتاج)

class ProductionOkHttpClient @Inject constructor() {

    fun build(): OkHttpClient {

        val certificatePinner = CertificatePinner.Builder()

            .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")

            .build()

  

        return OkHttpClient.Builder()

            .certificatePinner(certificatePinner)

            .build()

    }

}

```

  

### 10.3 Biometric Authentication

  

```kotlin

// ✅ المصادقة البيومترية

class BiometricAuth @Inject constructor(

    @ApplicationContext private val context: Context

) {

    private val executor = ContextCompat.getMainExecutor(context)

    private val biometricManager = BiometricManager.from(context)

  

    fun canAuthenticate(): Boolean {

        return biometricManager.canAuthenticate(

            BiometricManager.Authenticators.BIOMETRIC_STRONG

        ) == BiometricManager.BIOMETRIC_SUCCESS

    }

  

    fun authenticate(

        title: String = "تأكيد الهوية",

        subtitle: String = "استخدم بصمتك أو وجهك",

        onSuccess: () -> Unit,

        onError: (String) -> Unit

    ) {

        val promptInfo = BiometricPrompt.PromptInfo.Builder()

            .setTitle(title)

            .setSubtitle(subtitle)

            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)

            .build()

  

        val biometricPrompt = BiometricPrompt(

            context as FragmentActivity,

            executor,

            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {

                    onSuccess()

                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {

                    onError(errString.toString())

                }

            }

        )

  

        biometricPrompt.authenticate(promptInfo)

    }

}

```

  

### 10.4 قواعد الأمان العامة

  

- **لا تخزن التوكن في SharedPreferences العادية** — استخدم EncryptedSharedPreferences

- **لا تظهر الـ API Key في الكود** — استخدم `BuildConfig` مع NDK أو الخادم

- **استخدم ProGuard/R8** في الإصدار النهائي

- **تحقق من صحة المدخلات** — لا تثق في User Input أبداً

- **استخدم HTTPS فقط** — لا تسمح باتصالات HTTP

- **عقم الـ SQL input** — (لكن Room يحمي من SQL Injection أصلاً)

- **لا تسجل البيانات الحساسة** — تأكد من `Log.d` لا يطبع التوكن

  

---

  

## 📋 ملاحق

  

### الملحق أ: Checklist الشاملة للمشروع

  

#### قبل بدء المشروع

- [ ] تحديد العمارة (Clean Architecture)

- [ ] اختيار أداة DI (Hilt / Koin)

- [ ] إعداد قاعدة البيانات (Room)

- [ ] إعداد الـ Networking (Retrofit + Kotlin Serialization)

- [ ] إعداد السمة (Light + Dark + RTL)

- [ ] إدارة الإصدارات (Semantic Versioning)

  

#### لكل شاشة جديدة

- [ ] العمارة: ViewModel + State + Events

- [ ] السمة: لا ألوان/خطوط مقاسة

- [ ] RTL: الأيقونات والمحاذاة صحيحة

- [ ] الحالات: تحميل + نجاح + خطأ + فارغ

- [ ] الأرقام: شرق عربية للعملات والعدادات

- [ ] الأداء: `key` + `remember` + `derivedStateOf`

- [ ] الاختبارات: ViewModel + UseCase + UI

  

#### قبل الإصدار

- [ ] `./gradlew lint` — بدون تحذيرات

- [ ] `./gradlew ktlintCheck`  — تنسيق الكود صحيح

- [ ] `./gradlew testDebugUnitTest` — كل الاختبارات تمر

- [ ] `./gradlew assembleDebug` — البناء ينجح

- [ ] اختبار يدوي: الوضع الفاتح + الليلي + RTL

- [ ] اختبار: أجهزة مختلفة الأحجام

- [ ] ProGuard/R8 مفعل

  

### الملحق ب: Quick Reference Cards

  

#### Theme Colors Quick Ref

  

| في الكود | الدور المناسب |

|----------|--------------|

| `MaterialTheme.colorScheme.primary` | أزرار، FABs، أيقونات نشطة |

| `MaterialTheme.colorScheme.onSurface` | نص رئيسي داخل بطاقة |

| `MaterialTheme.colorScheme.onSurfaceVariant` | نص ثانوي، Caption |

| `MaterialTheme.colorScheme.surfaceVariant` | خلفية حاوية هادئة |

| `MaterialTheme.colorScheme.outline` | حدود واضحة |

| `MaterialTheme.colorScheme.outlineVariant` | حدود خفيفة |

| `MaterialTheme.colorScheme.error` | أخطاء، مبالغ متأخرة |

| `MaterialTheme.colorScheme.errorContainer` | خلفية صف خطأ |

  

#### Typography Quick Ref

  

| الحجم القديم | النمط الصحيح |

|-------------|-------------|

| `fontSize = 16.sp, Bold` | `titleMedium.copy(fontWeight = Bold)` |

| `fontSize = 14.sp` | `bodyMedium` |

| `fontSize = 14.sp, Bold` | `titleSmall.copy(fontWeight = Bold)` |

| `fontSize = 12.sp` | `bodySmall` |

| `fontSize = 11.sp` | `labelMedium` |

| `fontSize = 10.sp` | `labelSmall` |

  

#### RTL Quick Ref

  

| المكون | سلوك RTL |

|--------|---------|

| `ArrowBack` | `Icons.AutoMirrored.Filled.ArrowBack` |

| `ExitToApp` | `Icons.AutoMirrored.Filled.ExitToApp` |

| `SpacedBy` | آمن — يحترم الاتجاه |

| `SpaceBetween` | آمن — يحترم الاتجاه |

| `topStart` | أعلى يمين |

| `topEnd` | أعلى يسار |

  

#### Architecture Quick Ref

  

| الطبقة | الملفات | يعتمد على |

|--------|---------|-----------|

| **Domain** | Model, Repository (interface), UseCase | لا شيء (Kotlin فقط) |

| **Data** | DTO, Entity, DAO, RepositoryImpl | Room, Retrofit, Domain |

| **Presentation** | Screen, ViewModel, State | Domain, Compose |

  

### الملحق ج: قائمة المصادر والمراجع

  

- [Material 3 Design Kit](https://m3.material.io/)

- [Jetpack Compose Docs](https://developer.android.com/compose)

- [Android Architecture Guide](https://developer.android.com/topic/architecture)

- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

- [Hilt DI](https://developer.android.com/training/dependency-injection/hilt-android)

- [Retrofit](https://square.github.io/retrofit/)

- [Kotlin Serialization](https://kotlinlang.org/docs/serialization.html)

- [Room Database](https://developer.android.com/training/data-storage/room)

- [Coil Image Loading](https://coil-kt.github.io/coil/)

- [MockK Testing](https://mockk.io/)

- [Arabic RTL Guidelines](https://material.io/design/usability/bidirectionality.html)

- Google's `material.io` for latest Material 3 updates

  

### الملحق د: Troubleshooting

  

| المشكلة | الحل المحتمل |

|---------|-------------|

| الخط العربي لا يظهر | تأكد من وجود ملف الخط في `res/font/` وأن الأوزان مدعومة |

| RTL لا يعمل | تأكد من `CompositionLocalProvider` في Theme.kt |

| الأرقام تظهر غربية | استخدم `ArabicNumerals.convert()` |

| الـ Icon لا ينعكس | استخدم `Icons.AutoMirrored.*` بدلاً من `Icons.Default.*` |

| الـ Navigation لا يعمل | تأكد من مطابقة route و arguments |

| `collectAsStateWithLifecycle` غير موجود | أضف `implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.x")` |

| `@HiltViewModel` لا يعمل | أضف `hiltViewModel()` من `androidx.hilt:hilt-navigation-compose` |

| الاختبارات لا تعمل مع Flow | استخدم `turbine` لاختبار Flow |

| Compose Test يجد الـ Text العربي | استخدم `onNodeWithText("النص")` بالضبط مع التشكيل |

  

---

  

> **تم بحمد الله**  

> هذا الدليل محدث باستمرار. إذا وجدت خطأ أو إضافة مفيدة، رجاء تحديث الملف.

  

---

  

*آخر تحديث: يوليو 2026*