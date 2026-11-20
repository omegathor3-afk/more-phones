package com.omega.morephones

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Ink = Color(0xFF050B14)
private val Panel = Color(0xFF101D2D)
private val Cyan = Color(0xFF00DDF5)
private val Gold = Color(0xFFFFC34D)
private val Muted = Color(0xFF91A8BD)

data class CloudPhone(val id: String, val name: String, val status: Int, val ip: String = "") {
    val isOn: Boolean get() = status == 4 || status == 3
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MorePhonesApp() }
    }
}

private object SecureStore {
    private const val PREFS = "more_phones_secure"
    private const val ALIAS = "more_phones_aes_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        gen.init(android.security.keystore.KeyGenParameterSpec.Builder(
            ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return gen.generateKey()
    }

    private fun enc(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
    }

    private fun dec(value: String): String {
        val parts = value.split(":")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)))
    }

    fun save(context: Context, id: String, secret: String) {
        context.getSharedPreferences(PREFS, 0).edit()
            .putString("id", enc(id)).putString("secret", enc(secret)).apply()
    }

    fun load(context: Context): Pair<String, String>? {
        val p = context.getSharedPreferences(PREFS, 0)
        val id = p.getString("id", null) ?: return null
        val secret = p.getString("secret", null) ?: return null
        return runCatching { dec(id) to dec(secret) }.getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit().clear().apply()
    }
}

private object MoreLoginApi {
    private const val BASE = "https://api.morelogin.com"

    private fun request(method: String, path: String, body: JSONObject? = null, bearer: String? = null): JSONObject {
        val c = URL(BASE + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 20000
            c.readTimeout = 30000
            c.setRequestProperty("Accept", "application/json")
            if (bearer != null) c.setRequestProperty("Authorization", "Bearer " + bearer)
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(raw) }.getOrElse {
                throw IllegalStateException("استجابة غير مفهومة من MoreLogin")
            }
            if (c.responseCode !in 200..299) throw IllegalStateException(json.optString("msg").ifBlank { "HTTP " + c.responseCode })
            val code = json.optInt("code", 0)
            if (code != 0) throw IllegalStateException(json.optString("msg").ifBlank { "API error " + code })
            return json
        } finally { c.disconnect() }
    }

    private fun token(credentials: Pair<String, String>): String {
        val b = JSONObject().put("client_id", credentials.first)
            .put("client_secret", credentials.second).put("grant_type", "client_credentials")
        return request("POST", "/oauth2/token", b).getJSONObject("data").getString("access_token")
    }

    suspend fun call(c: Pair<String, String>, method: String, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) { request(method, path, body, token(c)) }

    suspend fun list(c: Pair<String, String>): List<CloudPhone> {
        val response = call(c, "POST", "/cloudphone/page", JSONObject().put("pageNo", 1).put("pageSize", 100))
        val array = response.optJSONObject("data")?.optJSONArray("dataList") ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val proxy = o.optJSONObject("proxy") ?: JSONObject()
            val name = o.optString("envRemark").ifBlank { o.optString("envName").ifBlank { "Android " + (i + 1) } }
            CloudPhone(o.optString("id"), name, o.optInt("envStatus", -1), proxy.optString("exportIp"))
        }.filter { it.id.isNotBlank() }
    }

    suspend fun create(c: Pair<String, String>, name: String, host: String, port: Int, user: String, pass: String): String {
        val body = JSONObject().put("skuId", "10002").put("quantity", 1).put("envRemark", name)
            .put("automaticGeo", true).put("automaticLanguage", true).put("automaticLocation", true)
        val result = call(c, "POST", "/cloudphone/create", body)
        val ids = result.optJSONArray("data") ?: JSONArray()
        if (ids.length() == 0) throw IllegalStateException("المزود لم يرجع رقم الهاتف")
        val id = ids.getString(0)
        if (host.isNotBlank() && port in 1..65535) {
            val proxy = JSONObject().put("proxyIp", host).put("proxyPort", port)
                .put("proxyProvider", 2).put("proxyName", "MORE PHONES - " + name)
            if (user.isNotBlank()) proxy.put("username", user)
            if (pass.isNotBlank()) proxy.put("password", pass)
            call(c, "POST", "/cloudphone/setProxy", JSONObject().put("ids", JSONArray().put(id)).put("proxy", proxy))
        }
        return id
    }

    suspend fun power(c: Pair<String, String>, id: String, on: Boolean) {
        call(c, "POST", if (on) "/cloudphone/powerOn" else "/cloudphone/powerOff", JSONObject().put("id", id))
    }

    suspend fun link(c: Pair<String, String>, id: String): String {
        return call(c, "POST", "/cloudphone/link", JSONObject().put("id", id).put("lang", "en-US"))
            .getJSONObject("data").getString("url")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorePhonesApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var credentials by remember { mutableStateOf(SecureStore.load(context)) }
    var phones by remember { mutableStateOf<List<CloudPhone>>(emptyList()) }
    var showSettings by remember { mutableStateOf(credentials == null) }
    var showCreate by remember { mutableStateOf(false) }
    var apiId by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var phoneName by remember { mutableStateOf("") }
    var proxyHost by remember { mutableStateOf("") }
    var proxyPort by remember { mutableStateOf("1080") }
    var proxyUser by remember { mutableStateOf("") }
    var proxyPass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("اربط حساب MoreLogin لإنشاء هواتف Android سحابية حقيقية.") }

    fun task(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { block() } catch (e: Exception) { notice = e.message ?: "حدث خطأ" }
            finally { busy = false }
        }
    }

    fun refresh() {
        val c = credentials ?: return
        task {
            phones = MoreLoginApi.list(c)
            notice = "تم تحديث قائمة الهواتف من حساب MoreLogin."
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, onPrimary = Ink, background = Ink, surface = Panel, onSurface = Color.White, secondary = Gold)) {
        Scaffold(containerColor = Ink, topBar = {
            TopAppBar(title = {
                Column {
                    Text("MORE PHONES", fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                    Text("CLOUD ANDROID FLEET", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp)
                }
            }, actions = {
                IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "إعدادات", tint = Gold) }
                IconButton(onClick = { refresh() }, enabled = credentials != null && !busy) { Icon(Icons.Default.Refresh, "تحديث", tint = Cyan) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink))
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Card(shape = RoundedCornerShape(25.dp), border = BorderStroke(1.dp, Cyan.copy(alpha = .4f)), colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF123A57), Panel, Color(0xFF1E2333))))) {
                            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cloud, null, tint = Cyan, modifier = Modifier.size(42.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("كل مساحة = هاتف Android مستقل", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                                        Text("REMOTE CLOUD DEVICES", color = Gold, fontSize = 9.sp)
                                    }
                                }
                                Text("كل مساحة تنشئ جهاز Android سحابيًا منفصلًا ببياناته وتطبيقاته. افتح شاشة الجهاز وتحكم فيه من هاتفك.", color = Color(0xFFD1E0EC), fontSize = 12.sp, lineHeight = 19.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(onClick = { showCreate = true }, enabled = credentials != null && !busy, label = { Text("إنشاء هاتف Android") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                                    AssistChip(onClick = { showSettings = true }, label = { Text(if (credentials == null) "ربط الحساب" else "الحساب متصل") }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                                }
                            }
                        }
                    }
                }
                item { SectionTitle("هواتف Android السحابية", "01 / CLOUD INSTANCES") }
                if (credentials == null || phones.isEmpty()) {
                    item {
                        InfoCard(
                            if (credentials == null) "اربط حساب MoreLogin أولًا" else "لا توجد هواتف محمّلة",
                            if (credentials == null) "يحتاج التطبيق API ID وAPI Key من MoreLogin حتى ينشئ أنظمة Android فعلية على خوادمهم." else "اضغط تحديث لجلب أجهزتك، أو أنشئ هاتف Android جديدًا. كل جهاز مستقل عن الآخر."
                        )
                    }
                }
                items(phones, key = { it.id }) { phone ->
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Color(0xFF263D55)), shape = RoundedCornerShape(19.dp)) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Android, null, tint = Cyan, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(phone.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("ID: " + phone.id, color = Muted, fontSize = 9.sp)
                                }
                                StatusPill(phone.status)
                            }
                            Text(if (phone.ip.isNotBlank()) "IP خروج: " + phone.ip else "IP خروج: غير متحقق", color = if (phone.ip.isNotBlank()) Gold else Muted, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = {
                                    task {
                                        MoreLoginApi.power(credentials!!, phone.id, !phone.isOn)
                                        notice = if (phone.isOn) "أُرسل أمر إيقاف " + phone.name else "أُرسل أمر تشغيل " + phone.name + ". انتظر الإقلاع ثم حدّث القائمة."
                                        phones = MoreLoginApi.list(credentials!!)
                                    }
                                }, enabled = !busy, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.PowerSettingsNew, null)
                                    Spacer(Modifier.width(5.dp))
                                    Text(if (phone.isOn) "إيقاف" else "تشغيل")
                                }
                                OutlinedButton(onClick = {
                                    task {
                                        val url = MoreLoginApi.link(credentials!!, phone.id)
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        notice = "تم فتح رابط التحكم المؤقت. صلاحيته 5 دقائق."
                                    }
                                }, enabled = !busy && phone.status == 4, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.OpenInBrowser, null)
                                    Spacer(Modifier.width(5.dp))
                                    Text("فتح Android")
                                }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF211E19)), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Gold.copy(alpha = .3f))) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("الشبكة وVPN", color = Gold, fontWeight = FontWeight.Bold)
                            Text("يمكن تثبيت VPN داخل كل Android سحابي بشكل منفصل. للحصول على IP مختلف فعليًا، استخدم مخرج VPN أو Proxy مختلفًا لكل جهاز؛ تكرار نفس الخادم قد يعطي نفس IP.", color = Color(0xFFE4DFD4), fontSize = 11.sp, lineHeight = 17.sp)
                            Text("MoreLogin قد يتطلب Proxy SOCKS5 صالحًا قبل تشغيل الهاتف. إدخال Proxy هنا اختياري، لكن من دونه قد يرفض المزود بدء الجهاز.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                        }
                    }
                }
                item { Text(if (busy) "جارٍ تنفيذ الطلب..." else notice, color = if (busy) Cyan else Muted, fontSize = 11.sp, lineHeight = 16.sp) }
                item { Text("MORE PHONES • CLOUD BUILD 0.4.0", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp) }
            }
        }
    }

    if (showSettings) AlertDialog(
        onDismissRequest = { showSettings = false },
        title = { Text("ربط MoreLogin Open API") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("من لوحة MoreLogin افتح Settings → API & MCP، ثم انسخ API ID وAPI Key.", color = Muted, fontSize = 11.sp)
                OutlinedTextField(apiId, { apiId = it }, label = { Text("API ID") }, singleLine = true)
                OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key") }, singleLine = true)
                Text("تُحفظ المفاتيح مشفرة باستخدام Android Keystore على هذا الجهاز.", color = Muted, fontSize = 10.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (apiId.isBlank() || apiKey.isBlank()) notice = "أدخل API ID وAPI Key."
                else task {
                    val candidate = apiId.trim() to apiKey.trim()
                    val list = MoreLoginApi.list(candidate)
                    SecureStore.save(context, candidate.first, candidate.second)
                    credentials = candidate
                    phones = list
                    notice = "تم التحقق من الاتصال بحساب MoreLogin بنجاح."
                    showSettings = false
                }
            }) { Text("تحقق واحفظ") }
        },
        dismissButton = {
            Row {
                if (credentials != null) TextButton(onClick = {
                    SecureStore.clear(context); credentials = null; phones = emptyList()
                    apiId = ""; apiKey = ""; showSettings = false
                    notice = "تم فصل حساب MoreLogin."
                }) { Text("فصل الحساب") }
                TextButton(onClick = { showSettings = false }) { Text("إلغاء") }
            }
        }
    )

    if (showCreate) AlertDialog(
        onDismissRequest = { showCreate = false },
        title = { Text("إنشاء هاتف Android جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(phoneName, { phoneName = it }, label = { Text("اسم المساحة / الهاتف") }, singleLine = true)
                Text("النظام: Android 12 • جهاز مستقل جديد", color = Cyan, fontSize = 11.sp)
                Divider(color = Color(0xFF263D55))
                Text("Proxy SOCKS5 (اختياري، وقد يكون مطلوبًا لبدء الهاتف)", color = Gold, fontSize = 11.sp)
                OutlinedTextField(proxyHost, { proxyHost = it }, label = { Text("Proxy Host / IP") }, singleLine = true)
                OutlinedTextField(proxyPort, { proxyPort = it }, label = { Text("Port") }, singleLine = true)
                OutlinedTextField(proxyUser, { proxyUser = it }, label = { Text("Username (اختياري)") }, singleLine = true)
                OutlinedTextField(proxyPass, { proxyPass = it }, label = { Text("Password (اختياري)") }, singleLine = true)
                Text("استخدم Proxy مختلفًا لكل هاتف إذا أردت مخارج IP مختلفة. التكلفة حسب حساب MoreLogin.", color = Muted, fontSize = 10.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val port = proxyPort.toIntOrNull() ?: 0
                if (phoneName.isBlank()) notice = "اكتب اسمًا للهاتف."
                else if (proxyHost.isNotBlank() && port !in 1..65535) notice = "رقم منفذ Proxy غير صحيح."
                else {
                    val name = phoneName.trim()
                    val host = proxyHost.trim()
                    val user = proxyUser.trim()
                    val pass = proxyPass
                    task {
                        val id = MoreLoginApi.create(credentials!!, name, host, port, user, pass)
                        phones = MoreLoginApi.list(credentials!!)
                        notice = "أُرسل طلب إنشاء Android مستقل. رقم الجهاز: " + id + ". انتظر ظهوره بحالة متوقف قبل تشغيله."
                        phoneName = ""; proxyHost = ""; proxyPort = "1080"; proxyUser = ""; proxyPass = ""
                        showCreate = false
                    }
                }
            }) { Text("إنشاء Android") }
        },
        dismissButton = { TextButton(onClick = { showCreate = false }) { Text("إلغاء") } }
    )
}

@Composable
private fun SectionTitle(title: String, overline: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(overline, color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(body, color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun StatusPill(status: Int) {
    val label = when (status) {
        0, 7 -> "جارٍ الإنشاء"
        1 -> "فشل الإنشاء"
        2 -> "متوقف"
        3 -> "يقلع"
        4 -> "يعمل"
        5, 6 -> "جارٍ التنفيذ"
        else -> "غير معروف"
    }
    val tint = when (status) {
        4 -> Color(0xFF55D69E)
        3, 0, 5, 6, 7 -> Gold
        1 -> Color(0xFFFF6B6B)
        else -> Muted
    }
    Surface(color = tint.copy(alpha = .14f), shape = RoundedCornerShape(30.dp)) {
        Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}
