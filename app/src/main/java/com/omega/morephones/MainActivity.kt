package com.omega.morephones

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101318)
private val Panel = Color(0xFF1B222B)
private val Gold = Color(0xFFFFC247)
private val Muted = Color(0xFF9AA6B2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MorePhonesApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorePhonesApp() {
    val context = LocalContext.current
    var selectedApk by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("مساحة عمل محلية على هذا الهاتف") }

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedApk = uri
            selectedName = uri.lastPathSegment?.substringAfterLast('/') ?: "APK selected"
            message = "تم اختيار ملف. التثبيت يحتاج موافقتك من مثبت Android."
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Gold, onPrimary = Ink, background = Ink, surface = Panel,
        onSurface = Color(0xFFF4F6F8), secondary = Color(0xFF7FC8B5)
    )) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("MORE PHONES", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                            Text("LOCAL DEVICE LAB", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Gold.copy(alpha = .14f), shape = RoundedCornerShape(16.dp)) {
                                    Icon(Icons.Default.Devices, null, tint = Gold, modifier = Modifier.padding(13.dp).size(30.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("مختبرك على جهاز واحد", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("إدارة ملفات وتجارب التطبيقات محليًا", color = Muted, fontSize = 12.sp)
                                }
                            }
                            Text("أنشئ ملفات اختبار لتنظيم التطبيقات والإصدارات. هذه الملفات تنظيمية وليست أنظمة Android افتراضية مستقلة.", color = Color(0xFFD0D6DC), fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoChip("محلي", "لا سيرفر")
                                InfoChip("خصوصية", "لا حساب مطلوب")
                            }
                        }
                    }
                }
                item {
                    Text("أدوات المختبر", color = Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, null, tint = Gold, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("استيراد ملف APK", fontWeight = FontWeight.Bold)
                                    Text("اختر ملف تثبيت محفوظًا على هاتفك", color = Muted, fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick = { apkPicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
                            ) { Text("اختيار ملف APK", fontWeight = FontWeight.Bold) }
                            if (selectedApk != null) {
                                HorizontalDivider(color = Color(0xFF36404B))
                                Text(selectedName ?: "ملف APK محدد", color = Color.White, fontWeight = FontWeight.Medium)
                                OutlinedButton(onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(selectedApk, "application/vnd.android.package-archive")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        message = "تعذر فتح مثبت الحزم لهذا الملف."
                                    }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.InstallMobile, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("فتح مثبت Android")
                                }
                            }
                        }
                    }
                }
                item {
                    Text("ملفات الاختبار", color = Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LabProfileCard("ملف الاختبار الأساسي", "بيئة الهاتف الحالية", "نشط", Icons.Default.Android)
                    Spacer(Modifier.height(8.dp))
                    LabProfileCard("ملف اختبار جديد", "مساحة تنظيمية — ليست هاتفًا افتراضيًا", "قريبًا", Icons.Default.Devices)
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF29251B)), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Security, null, tint = Gold)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("حدود تقنية وخصوصية", color = Gold, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("التطبيق لا ينشئ حاليًا أجهزة Android مستقلة، ولا يغيّر IP أو VPN لكل ملف. تثبيت أي APK يتم بموافقتك عبر Android. لا نقرأ كلمات المرور أو بيانات التطبيقات.", color = Color(0xFFE0D8C7), fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(message, color = Muted, fontSize = 11.sp)
                    }
                }
                item { Text("MORE PHONES • الإصدار 0.1.0", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun InfoChip(title: String, value: String) {
    Surface(color = Color(0xFF29313B), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(5.dp))
            Text(value, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LabProfileCard(title: String, subtitle: String, status: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Text(status, color = Gold, fontSize = 11.sp)
        }
    }
}
