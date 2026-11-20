package com.omega.morephones

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF050B14)
private val Panel = Color(0xFF101D2D)
private val PanelLight = Color(0xFF172A40)
private val Cyan = Color(0xFF00DDF5)
private val Gold = Color(0xFFFFC34D)
private val Muted = Color(0xFF91A8BD)

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
    var message by remember { mutableStateOf("مساحة عمل محلية وآمنة على هاتفك") }

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedApk = uri
            selectedName = uri.lastPathSegment?.substringAfterLast('/') ?: "APK selected"
            message = "تم اختيار الملف. التثبيت النهائي بموافقتك عبر Android."
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, onPrimary = Ink, background = Ink, surface = Panel,
        onSurface = Color(0xFFF4F8FC), secondary = Gold,
        surfaceVariant = PanelLight, onSurfaceVariant = Muted
    )) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("MORE PHONES", fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                            Text("YOUR POCKET DEVICE LAB", color = Cyan, fontSize = 9.sp, letterSpacing = 2.2.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Cyan.copy(alpha = .35f))
                    ) {
                        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF123A57), Color(0xFF101D2D), Color(0xFF1E2333))))) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = Cyan.copy(alpha = .12f), shape = RoundedCornerShape(18.dp)) {
                                        Icon(Icons.Default.Devices, null, tint = Cyan, modifier = Modifier.padding(14.dp).size(34.dp))
                                    }
                                    Spacer(Modifier.width(13.dp))
                                    Column {
                                        Text("مختبرك. جهازك. تحكمك.", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                        Text("LOCAL ANDROID WORKSPACE", color = Gold, fontSize = 9.sp, letterSpacing = 1.4.sp)
                                    }
                                }
                                Text(
                                    "نظّم ملفات التطبيقات وجهّز تجاربك من مكان واحد، مع إبقاء التثبيت والبيانات تحت سيطرتك.",
                                    color = Color(0xFFD1E0EC), fontSize = 13.sp, lineHeight = 20.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    InfoChip("LOCAL", "على جهازك")
                                    InfoChip("PRIVATE", "بدون حساب")
                                }
                            }
                        }
                    }
                }
                item {
                    SectionTitle("أدوات المختبر", "01 / IMPORT")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263D55))
                    ) {
                        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Gold.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Default.CloudUpload, null, tint = Gold, modifier = Modifier.padding(11.dp).size(27.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("استيراد تطبيق", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("اختر ملف APK من مساحة تخزينك", color = Muted, fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = { apkPicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(15.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)
                            ) {
                                Icon(Icons.Default.CloudUpload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("اختيار ملف APK", fontWeight = FontWeight.ExtraBold)
                            }
                            if (selectedApk != null) {
                                HorizontalDivider(color = Color(0xFF29445D))
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
                                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Default.InstallMobile, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("فتح مثبت Android")
                                }
                            }
                        }
                    }
                }
                item {
                    SectionTitle("مساحات العمل", "02 / WORKSPACES")
                    LabProfileCard("ملف الاختبار الأساسي", "بيئة الهاتف الحالية", "ACTIVE", Icons.Default.Android)
                    Spacer(Modifier.height(9.dp))
                    LabProfileCard("ملف اختبار إضافي", "ملف تنظيمي — ليس هاتفًا افتراضيًا", "PLANNED", Icons.Default.Devices)
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF211E19)),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .28f))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Security, null, tint = Gold)
                            Spacer(Modifier.width(11.dp))
                            Column {
                                Text("شفافية وأمان", color = Gold, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "الإصدار الحالي لا ينشئ أنظمة Android افتراضية مستقلة ولا يغيّر IP أو VPN لكل ملف. التثبيت يتم بموافقتك، ولا نقرأ كلمات المرور أو بيانات التطبيقات الخاصة.",
                                    color = Color(0xFFE4DFD4), fontSize = 12.sp, lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(message, color = Muted, fontSize = 11.sp)
                    }
                }
                item {
                    Text("MORE PHONES  •  BUILD 0.2.0", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, overline: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(overline, color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun InfoChip(title: String, value: String) {
    Surface(color = Color(0xFF071522), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, Cyan.copy(alpha = .25f))) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.width(7.dp))
            Text(value, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LabProfileCard(title: String, subtitle: String, status: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263D55))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Cyan.copy(alpha = .10f), shape = RoundedCornerShape(13.dp)) {
                Icon(icon, null, tint = Cyan, modifier = Modifier.padding(10.dp).size(27.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Text(status, color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}
