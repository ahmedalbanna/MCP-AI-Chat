# دليل تصميم واجهات المحادثة بالعربية (RTL) لتطبيقات Android Jetpack Compose

  

> هذا الدليل خاص بوكلاء الذكاء الاصطناعي عند تصميم أو تعديل تطبيقات Android باللغة العربية.

> اقرأه بالكامل قبل البدء في أي مشروع عربي.

  

---

  

## جدول المحتويات

1. [مبادئ أساسية](#1-مبادئ-أساسية)

2. [إعداد الاتجاه RTL](#2-إعداد-الاتجاه-rtl)

3. [الخط العربي (Cairo Font)](#3-الخط-العربي-cairo-font)

4. [الأرقام الشرق عربية](#4-الأرقام-الشرق-عربية)

5. [الأيقونات AutoMirrored](#5-الأيقونات-automirrored)

6. [محاذاة النصوص](#6-محاذاة-النصوص)

7. [ترتيب المكونات Layouts](#7-ترتيب-المكونات-layouts)

8. [قائمة الأخطاء الشائعة](#8-قائمة-الأخطاء-الشائعة)

9. [قائمة التحقق النهائية](#9-قائمة-التحقق-النهائية)

10. [مثال متكامل](#10-مثال-متكامل)

  

---

  

## 1. مبادئ أساسية

  

عند تصميم تطبيق عربي، هناك 4 أركان أساسية يجب ضبطها معاً:

  

| الركن | الوصف | الأولوية |

|-------|-------|----------|

| **RTL Layout** | اتجاه الصفحة من اليمين لليسار | 🔴 إجباري |

| **الخط العربي** | استخدام خط يدعم الحروف العربية بشكل صحيح | 🔴 إجباري |

| **الأرقام العربية الشرقية** | ٠١٢٣٤٥٦٧٨٩ بدلاً من 0123456789 | 🟡 ضروري للتطبيقات المالية |

| **الأيقونات الاتجاهية** | عكس الأيقونات التي تشير لاتجاه معين | 🟡 ضروري |

  

> **⚠️ تنبيه:** تجاهل أيٍّ من هذه الأركان يؤدي إلى تجربة مستخدم سيئة للمتحدثين بالعربية.

  

---

  

## 2. إعداد الاتجاه RTL

  

### 2.1. في AndroidManifest.xml (اختياري)

```xml

<application

    android:supportsRtl="true"

    ...>

```

هذا يخبر النظام أن تطبيقك يدعم RTL. لكن في Compose، نتحكم بالاتجاه برمجياً.

  

### 2.2. في Theme.kt (إجباري)

لفّ جميع محتويات التطبيق بـ `CompositionLocalProvider`:

  

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

```

  

```kotlin

// ❌ خطأ - بدون CompositionLocalProvider

@Composable

fun MyApplicationTheme(content: @Composable () -> Unit) {

    MaterialTheme(...) { content() }

}

```

  

### 2.3. في شاشة معينة (حالات خاصة)

إذا أردت تطبيق RTL على شاشة محددة فقط:

  

```kotlin

CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

    // محتوى الشاشة فقط هنا

}

```

  

---

  

## 3. الخط العربي (Cairo Font)

  

### 3.1. إضافة ملف الخط

ضع ملف الخط في `res/font/`:

```

res/font/cairo_variable.ttf

```

  

### 3.2. تعريف FontFamily (إجباري)

في ملف `Type.kt`:

  

```kotlin

// ✅ صحيح

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

  

### 3.3. ربط الخط في Theme.kt

```kotlin

MaterialTheme(

    typography = Typography,  // ✅ لا تنسى هذا

    ...

)

```

  

### 3.4. ملاحظات مهمة عن الخط

- **Cairo Variable Font**: إذا كان الخط من نوع Variable، قد لا يدعمه بعض إصدارات Android القديمة. جرب استخدام Cairo الثابت (non-variable) إذا ظهرت مشاكل.

- **Font Weight**: تأكد من أن ملف الخط يدعم جميع الأوزان التي تستخدمها (Light, Normal, Medium, SemiBold, Bold).

- **Arabic vs Persian**: بعض الخطوط العربية لا تدعم الحروف الفارسية (ﮒ ﭻ ﭖ). Cairo يدعمها.

  

---

  

## 4. الأرقام الشرق عربية

  

### 4.1. لماذا الأرقام الشرق عربية؟

المستخدم العربي في اليمن والخليج يفضل رؤية:

- `١٬٥٠٠ ر.ي` بدلاً من `1,500 ر.ي`

- `نسبة ١٥%` بدلاً من `نسبة 15%`

  

### 4.2. إنشاء أداة التحويل

  

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

}

```

  

### 4.3. قواعد استخدام الأرقام

  

| النوع | أرقام غربية | أرقام شرق عربية | مثال |

|-------|-------------|-----------------|------|

| المبالغ النقدية | ❌ | ✅ | `١٬٥٠٠ ر.ي` |

| النسب المئوية | ❌ | ✅ | `١٥%` |

| العدادات (عدد الحركات) | ❌ | ✅ | `٤٢ حركة` |

| أرقام الهواتف | ✅ | ❌ | `771111111` |

| التواريخ في واجهة API | ✅ | ❌ | `2024-01-15` |

| المدخلات النصية للأرقام | ✅ | ❌ | حقل إدخال المبلغ |

| الأرقام داخل جمل API/SDK | ✅ | ❌ | `userId = 42` |

  

### 4.4. التطبيق في الكود

  

```kotlin

// ✅ صحيح - استخدام دالة التحويل

Text(ArabicNumerals.formatCurrency(amount))

  

// ✅ صحيح - تحويل النصوص المنسقة

Text(ArabicNumerals.convert(stringResource(R.string.template, count)))

  

// ❌ خطأ - أرقام غربية مباشرة

Text("${amount.toInt()} ر.ي")

```

  

---

  

## 5. الأيقونات AutoMirrored

  

### 5.1. المبدأ

في RTL، الأيقونات التي تشير إلى اتجاه معين (يمين/يسار، ترتيب، رجوع، خروج) يجب أن تنعكس تلقائياً.

  

### 5.2. القاعدة الأساسية

```kotlin

// ✅ صحيح - أيقونة متجهة تنعكس تلقائياً في RTL

Icons.AutoMirrored.Filled.ArrowBack

Icons.AutoMirrored.Filled.ArrowForward

Icons.AutoMirrored.Filled.ExitToApp

Icons.AutoMirrored.Filled.ReceiptLong

Icons.AutoMirrored.Outlined.ListAlt

Icons.AutoMirrored.Filled.List

Icons.AutoMirrored.Filled.TrendingUp

Icons.AutoMirrored.Filled.EventNote

Icons.AutoMirrored.Filled.Send

  

// ❌ خطأ - أيقونة لا تنعكس في RTL

Icons.Filled.ArrowBack

Icons.Default.ExitToApp

Icons.Default.ReceiptLong

```

  

### 5.3. الأيقونات التي لا تحتاج AutoMirrored

بعض الأيقونات متناظرة أو غير اتجاهية ولا تحتاج لعكس:

```

Icons.Default.Add (+)

Icons.Default.Settings (ترس)

Icons.Default.Search (بحث)

Icons.Default.Person (شخص)

Icons.Default.Store (متجر)

Icons.Default.Edit (قلم)

Icons.Default.Delete (سلة مهملات)

Icons.Default.Email (بريد)

Icons.Default.Phone (هاتف)

Icons.Default.Star (نجمة)

Icons.Default.Home (منزل)

Icons.Default.Favorite (قلب)

```

  

### 5.4. أيقونات يجب الحذر منها

بعض الأيقونات لها دلالة اتجاهية خفية:

- `Icons.Default.ChevronRight` / `ChevronLeft` → استخدم `Icons.AutoMirrored.Filled.ChevronRight`

- `Icons.Default.NavigateNext` / `NavigateBefore` → استخدم `Icons.AutoMirrored.Filled.NavigateNext`

- `Icons.Default.KeyboardArrowRight` / `KeyboardArrowLeft` → استخدم `Icons.AutoMirrored.Filled.KeyboardArrowRight`

  

---

  

## 6. محاذاة النصوص

  

### 6.1. TextAlign

```kotlin

// ✅ صحيح - TextAlign يعمل بشكل صحيح مع RTL تلقائياً

Text(text = "نص عربي", textAlign = TextAlign.Center)  // محاذاة وسط

Text(text = "نص عربي")                                 // افتراضي = Start (يمين في RTL)

  

// ملاحظة: TextAlign.Start يعني يمين في RTL ويسار في LTR

// TextAlign.End يعني يسار في RTL ويمين في LTR

```

  

### 6.2. SoftWrap و Text Direction

```kotlin

// ✅ صحيح - للأرقام المخلوطة مع نص عربي

Text(

    text = "المجموع: ١٬٥٠٠ ر.ي",

    textDirection = TextDirection.Content  // يكتشف تلقائياً

)

```

  

### 6.3. عندما يكون النص رقماً إنجليزياً داخل سياق عربي

```kotlin

// ✅ صحيح

Row {

    Text("الرقم:", modifier = Modifier.weight(1f))

    Text(

        text = "771111111",

        modifier = Modifier.weight(1f),

        textAlign = TextAlign.End  // في RTL, End = يسار (اتجاه كتابة الأرقام)

    )

}

```

  

---

  

## 7. ترتيب المكونات Layouts

  

### 7.1. Row و Column

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

  

// ❌ تجنب - Arrangement.Start/Arrangement.End قد يعطي نتائج غير متوقعة

// Row(horizontalArrangement = Arrangement.Start) // غير آمن

```

  

### 7.2. Alignment في Row و Column

```kotlin

// ✅ صحيح - Alignment.CenterHorizontally محايد

Column(horizontalAlignment = Alignment.CenterHorizontally)

  

// ✅ صحيح - Alignment.Start/End تنعكس تلقائياً

Column(horizontalAlignment = Alignment.Start)   // يمين في RTL

Column(horizontalAlignment = Alignment.End)     // يسار في RTL

```

  

### 7.3. RoundedCornerShape

```kotlin

// ✅ صحيح - Start/End في RoundedCornerShape تنعكس تلقائياً

// في RTL: topStart = أعلى يمين, topEnd = أعلى يسار

RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)

RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)

  

// هذا يتطابق مع ترتيب الأزرار:

// [الزر الأول (يمين - topStart)] [الزر الأوسط] [الزر الأخير (يسار - topEnd)]

```

  

### 7.4. Scaffold و TopAppBar

```kotlin

// ✅ صحيح - TopAppBar في RTL:

// - title يظهر في يمين الـ TopAppBar

// - actions تظهر في يسار الـ TopAppBar

// - navigationIcon يظهر في يمين الـ TopAppBar

TopAppBar(

    title = { Text("العنوان") },

    navigationIcon = { IconButton(...) },  // يظهر في اليمين

    actions = { IconButton(...) }           // تظهر في اليسار

)

```

  

### 7.5. AlertDialog

```kotlin

// ✅ صحيح - Material3 AlertDialog في RTL:

// - زر التأكيد (confirmButton) يظهر في اليمين

// - زر الإلغاء (dismissButton) يظهر في اليسار

AlertDialog(

    confirmButton = { Text("تأكيد") },  // يمين

    dismissButton = { Text("إلغاء") }    // يسار

)

```

  

### 7.6. OutlinedTextField

```kotlin

// ✅ صحيح - في RTL:

// - leadingIcon يظهر في اليمين

// - trailingIcon يظهر في اليسار

// - النص يبدأ من اليمين

OutlinedTextField(

    value = ...,

    leadingIcon = { Icon(Icons.Default.Search) },  // يمين

    trailingIcon = { IconButton(...) }               // يسار

)

```

  

---

  

## 8. قائمة الأخطاء الشائعة

  

### ❌ خطأ 1: نسيان تحويل الأرقام

```kotlin

// خطأ

Text("${amount} ر.ي")  // "1500 ر.ي"

  

// صواب

Text(ArabicNumerals.formatCurrency(amount))  // "١٬٥٠٠ ر.ي"

```

  

### ❌ خطأ 2: استخدام أيقونات بدون AutoMirrored

```kotlin

// خطأ

Icons.Default.ArrowBack

Icons.Default.ExitToApp

  

// صواب

Icons.AutoMirrored.Filled.ArrowBack

Icons.AutoMirrored.Filled.ExitToApp

```

  

### ❌ خطأ 3: افتراض LTR في التنسيق

```kotlin

// خطأ - في اليسار

Row {

    Text("القيمة")

    Text("العنوان")

}

  

// صواب - العنوان أولاً (يمين)

Row {

    Text("القيمة")  

    Text("العنوان")  // اليسار

}

// أو استخدم Modifier.weight (الأفضل)

```

  

### ❌ خطأ 4: إضافة RTL في شاشة واحدة فقط

```kotlin

// خطأ - التطبيق غير متناسق

// Screen A: لها RTL

// Screen B: لا يوجد RTL

  

// صواب - RTL في الـ Theme يغطي جميع الشاشات

```

  

### ❌ خطأ 5: تجاهل @string resources للغة العربية

```kotlin

// خطأ - نص مقسى (hardcoded)

Text("إجمالي الدخل")

  

// ✅ صواب - استخدام string resources

Text(stringResource(R.string.overview_total_income))

// ولكن الأفضل استخدامها للترجمة، النص العربي المقسى مقبول في تطبيق عربي بحت

```

  

### ❌ خطأ 6: استخدام الأرقام العربية في التواريخ API

```kotlin

// خطأ - API يتوقع yyyy-MM-dd

"٢٠٢٤-٠١-١٥"

  

// صواب

"2024-01-15"  // Western digits للتواريخ API

```

  

---

  

## 9. قائمة التحقق النهائية

  

استخدم هذه القائمة قبل اعتبار أي شاشة "مكتملة" في التطبيق العربي:

  

### 🔲 RTL Infrastructure

- [ ] `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` في Theme.kt

- [ ] `supportsRtl="true"` في AndroidManifest.xml

- [ ] جميع الشاشات ترث RTL تلقائياً

  

### 🔲 الخط العربي

- [ ] ملف الخط مضاف في `res/font/`

- [ ] `FontFamily` معرف في Type.kt

- [ ] `Typography` مربوط في Theme.kt

- [ ] جميع الأوزان المستخدمة مدعومة في ملف الخط

  

### 🔲 الأرقام العربية الشرقية

- [ ] تم إنشاء أداة تحويل (مثل `ArabicNumerals.kt`)

- [ ] جميع المبالغ النقدية تستخدم `formatCurrency()`

- [ ] جميع النسب المئوية محولة

- [ ] جميع العدادات محولة (عدد الحركات، عدد العملاء...)

- [ ] أرقام الهواتف بقيت غربية

- [ ] التواريخ الداخلية بقيت غربية

- [ ] حقول الإدخال بقيت غربية (المستخدم يكتب بالأرقام الغربية)

  

### 🔲 الأيقونات

- [ ] `Icons.AutoMirrored.Filled.ExitToApp` بدلاً من `Icons.Default.ExitToApp`

- [ ] `Icons.AutoMirrored.Filled.ReceiptLong` بدلاً من `Icons.Default.ReceiptLong`

- [ ] `Icons.AutoMirrored.Filled.List` بدلاً من `Icons.Default.List`

- [ ] أي أيقونة اتجاهية أخرى تم تصحيحها

- [ ] الأيقونات المتناظرة (Settings, Add, Search) بقيت كما هي

  

### 🔲 المحاذاة والتنسيق

- [ ] استخدام `Arrangement.SpaceBetween` في الصفوف

- [ ] استخدام `Arrangement.spacedBy()` بدلاً من `Arrangement.Start/End`

- [ ] `RoundedCornerShape` تستخدم `topStart/topEnd` بشكل صحيح مع ترتيب RTL

- [ ] `Scaffold`, `TopAppBar`, `NavigationBar` تعمل بشكل صحيح

- [ ] `AlertDialog` أزرار التأكيد/الإلغاء في الموضع الصحيح

- [ ] `OutlinedTextField` الـ leading/trailing icons في الموضع الصحيح

  

### 🔲 اختبار

- [ ] التطبيق يُبنى بدون أخطاء (`./gradlew assembleDebug`)

- [ ] اختبار يدوي على جهاز أو محاكي بلغة عربية

- [ ] التحقق من جميع الشاشات

- [ ] التحقق من الحوارات المنبثقة (Dialogs)

- [ ] التحقق من الـ Navigation (Bottom Bar, Top Bar)

  

---

  

## 10. مثال متكامل

  

### ملف Theme.kt كامل

```kotlin

package com.example.ui.theme

  

import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.Composable

import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.ui.platform.LocalLayoutDirection

import androidx.compose.ui.unit.LayoutDirection

  

@Composable

fun MyApplicationTheme(content: @Composable () -> Unit) {

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        MaterialTheme(

            colorScheme = LightColorScheme,

            typography = Typography,  // Cairo Font

            content = content

        )

    }

}

```

  

### شاشة مثال كاملة

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

  
