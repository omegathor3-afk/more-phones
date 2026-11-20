package com.omega.morephones

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private val Ink = Color(0xFF050B14)
private val Panel = Color(0xFF101D2D)
private val Cyan = Color(0xFF00DDF5)
private val Gold = Color(0xFFFFC34D)
private val Muted = Color(0xFF91A8BD)

data class LabPhone(val id: String, val name: String, val kind: String, val endpoint: String = "", val apkCount: Int = 0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MorePhonesApp() }
    }
}

private fun loadPhones(context: android.content.Context): List<LabPhone> {
    val raw = context.getSharedPreferences("more_phones", 0).getString("phones", "[]") ?: "[]"
    val arr = JSONArray(raw)
    return (0 until arr.length()).mapNotNull { i ->
        runCatching {
            val o = arr.getJSONObject(i)
            val id = o.getString("id")
            val dir = File(context.filesDir, "phone-lab/$id/apks")
            LabPhone(id, o.getString("name"), o.getString("kind"), o.optString("endpoint"), dir.listFiles()?.count { it.isFile } ?: 0)
        }.getOrNull()
    }
}

private fun savePhones(context: android.content.Context, phones: List<LabPhone>) {
    val arr = JSONArray()
    phones.forEach { p ->
        arr.put(JSONObject().put("id", p.id).put("name", p.name).put("kind", p.kind).put("endpoint", p.endpoint))
    }
    context.getSharedPreferences("more_phones", 0).edit().putString("phones", arr.toString()).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorePhonesApp() {
    val context = LocalContext.current
    var phones by remember { mutableStateOf(loadPhones(context)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showCloud by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var cloudName by remember { mutableStateOf("") }
    var cloudUrl by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("أنشئ مساحة محلية أو اربط جهاز Android سحابيًا") }
    val selected = phones.firstOrNull { it.id == selectedId }

    fun refresh() { phones = loadPhones(context) }
    fun addPhone(name: String, kind: String, endpoint: String = "") {
        val p = LabPhone(UUID.randomUUID().toString(), name.trim(), kind, endpoint)
        phones = phones + p
        savePhones(context, phones)
        selectedId = p.id
    }

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val p = selected
        if (uri != null && p != null && p.kind == "local") {
            runCatching {
                val dir = File(context.filesDir, "phone-lab/${p.id}/apks").apply { mkdirs() }
                val display = (uri.lastPathSegment ?: "application.apk").substringAfterLast('/')
                    .replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "application.apk" }
                val target = File(dir, "${System.currentTimeMillis()}-$display")
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Cannot open selected file" }.copyTo(target.outputStream())
                }
                notice = "تم حفظ نسخة من ملف APK داخل مساحة ${p.name}. لم يتم تثبيته داخل نظام Android مستقل."
                refresh()
            }.onFailure { notice = "تعذر نسخ الملف: ${it.message}" }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, onPrimary = Ink, background = Ink, surface = Panel, onSurface = Color.White, secondary = Gold)) {
        Scaffold(containerColor = Ink, topBar = {
            TopAppBar(title = {
                Column {
                    Text("MORE PHONES", fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                    Text("LOCAL + CLOUD DEVICE LAB", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp)
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink))
        }) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(shape = RoundedCornerShape(25.dp), border = BorderStroke(1.dp, Cyan.copy(alpha = .35f)), colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF123A57), Panel, Color(0xFF1E2333))))) {
                            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Devices, null, tint = Cyan, modifier = Modifier.size(42.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("مساحات مستقلة للإدارة", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                        Text("LOCAL WORKSPACES • CLOUD LINKS", color = Gold, fontSize = 9.sp)
                                    }
                                }
                                Text("أنشئ ملفات محلية منفصلة لحفظ ملفات APK، أو أضف رابط جلسة Android سحابية يدعمها مزودك.", color = Color(0xFFD1E0EC), fontSize = 12.sp, lineHeight = 19.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(onClick = { showCreate = true }, label = { Text("مساحة محلية") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                                    AssistChip(onClick = { showCloud = true }, label = { Text("ربط سحابي") }, leadingIcon = { Icon(Icons.Default.Cloud, null) })
                                }
                            }
                        }
                    }
                }
                item { SectionTitle("أجهزتك ومساحاتك", "01 / INSTANCES") }
                if (phones.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Android, null, tint = Muted, modifier = Modifier.size(34.dp))
                                Text("لا توجد مساحات بعد", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("ابدأ بإنشاء مساحة محلية أو ربط جلسة سحابية", color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
                items(phones, key = { it.id }) { p ->
                    val active = selectedId == p.id
                    Card(
                        onClick = { selectedId = p.id },
                        colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFF142D40) else Panel),
                        border = BorderStroke(1.dp, if (active) Cyan.copy(alpha = .65f) else Color(0xFF263D55)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (p.kind == "cloud") Icons.Default.Cloud else Icons.Default.Folder, null, tint = if (p.kind == "cloud") Gold else Cyan, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Text(if (p.kind == "cloud") "جلسة سحابية • رابط خارجي" else "مساحة ملفات محلية • ${p.apkCount} APK", color = Muted, fontSize = 10.sp)
                            }
                            if (p.kind == "cloud") {
                                IconButton(onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.endpoint))) }
                                        .onFailure { notice = "تعذر فتح رابط الجلسة" }
                                }) { Icon(Icons.Default.OpenInNew, "فتح", tint = Cyan) }
                            } else {
                                Text("LOCAL", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            IconButton(onClick = {
                                phones = phones.filterNot { it.id == p.id }
                                File(context.filesDir, "phone-lab/${p.id}").deleteRecursively()
                                savePhones(context, phones)
                                if (selectedId == p.id) selectedId = null
                                notice = "تم حذف مساحة العمل وملفات APK التابعة لها"
                            }) { Icon(Icons.Default.Delete, "حذف", tint = Muted) }
                        }
                    }
                }
                if (selected != null) {
                    item { SectionTitle("إدارة المساحة المحددة", "02 / SELECTED") }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(selected.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (selected.kind == "local") {
                                    Text("يمكنك حفظ ملفات APK هنا بشكل منفصل. هذا لا ينشئ نظام Android ضيفًا ولا يثبت التطبيقات داخل بيئة معزولة.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
                                    Button(onClick = { apkPicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text("استيراد APK إلى هذه المساحة")
                                    }
                                } else {
                                    Text("الرابط محفوظ كاختصار جلسة. فتحه يعتمد على خدمة Android سحابية فعالة وصلاحية وصول من مزود الخدمة.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
                                    OutlinedButton(onClick = {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(selected.endpoint))) }
                                            .onFailure { notice = "تعذر فتح الرابط" }
                                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.OpenInNew, null); Text("فتح جلسة Android") }
                                }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF211E19)), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Gold.copy(alpha = .3f))) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Security, null, tint = Gold)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("حدود التشغيل بوضوح", color = Gold, fontWeight = FontWeight.Bold)
                                Text("المساحات المحلية هنا خزائن ملفات منفصلة وليست هواتف Android افتراضية. الربط السحابي يفتح رابط مزود خارجي ولا ينشئ خادمًا تلقائيًا. هاتفك Android 12 لا يتيح لتطبيق عادي إنشاء عدة أنظمة Android كاملة محليًا.", color = Color(0xFFE4DFD4), fontSize = 11.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }
                item { Text(notice, color = Muted, fontSize = 11.sp) }
                item { Text("MORE PHONES • BUILD 0.3.0", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp) }
            }
        }
    }

    if (showCreate) AlertDialog(
        onDismissRequest = { showCreate = false },
        title = { Text("إنشاء مساحة محلية") },
        text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("اسم المساحة") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            if (newName.isNotBlank()) { addPhone(newName, "local"); newName = ""; showCreate = false; notice = "تم إنشاء مساحة ملفات محلية مستقلة." }
        }) { Text("إنشاء") } },
        dismissButton = { TextButton(onClick = { showCreate = false }) { Text("إلغاء") } }
    )
    if (showCloud) AlertDialog(
        onDismissRequest = { showCloud = false },
        title = { Text("ربط جلسة Android سحابية") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cloudName, onValueChange = { cloudName = it }, label = { Text("اسم الجهاز") }, singleLine = true)
                OutlinedTextField(value = cloudUrl, onValueChange = { cloudUrl = it }, label = { Text("رابط HTTPS للجلسة") }, singleLine = true)
                Text("أدخل رابط جلسة يوفره مزود خدمة Android سحابية. لا تدخل كلمة مرور أو رمزًا سريًا في الرابط.", color = Muted, fontSize = 10.sp)
            }
        },
        confirmButton = { TextButton(onClick = {
            val valid = cloudUrl.trim().startsWith("https://", true)
            if (cloudName.isNotBlank() && valid) {
                addPhone(cloudName, "cloud", cloudUrl.trim())
                cloudName = ""; cloudUrl = ""; showCloud = false
                notice = "تم حفظ رابط الجلسة. لم يتم التحقق من عمل الخدمة بعد."
            } else notice = "أدخل اسمًا ورابط HTTPS صحيحًا."
        }) { Text("حفظ الرابط") } },
        dismissButton = { TextButton(onClick = { showCloud = false }) { Text("إلغاء") } }
    )
}

@Composable
private fun SectionTitle(title: String, overline: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(overline, color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}
