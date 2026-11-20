package com.omega.morephones

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val API = "https://more-phones-api-lampoo.vercel.app"
private val Ink = Color(0xFF050B14)
private val Panel = Color(0xFF101D2D)
private val Cyan = Color(0xFF00DDF5)
private val Gold = Color(0xFFFFC34D)
private val Muted = Color(0xFF91A8BD)

data class CloudPhone(val id:String,val name:String,val status:Int,val ip:String="") {
    val isOn:Boolean get()=status==4||status==3
}
class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MorePhonesApp()}}
}
private object InstallIdentity {
    fun get(context:Context):String {
        val p=context.getSharedPreferences("more_phones_install",0)
        p.getString("install_id",null)?.let{return it}
        val id=UUID.randomUUID().toString()
        p.edit().putString("install_id",id).apply()
        return id
    }
}
private object MorePhonesService {
    private fun request(method:String,path:String,body:JSONObject?=null):JSONObject {
        val c=URL(API+path).openConnection() as HttpURLConnection
        try {
            c.requestMethod=method;c.connectTimeout=15000;c.readTimeout=30000
            c.setRequestProperty("Accept","application/json")
            if(body!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toString().toByteArray())}}
            val code=c.responseCode
            val raw=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}?:"{}"
            val j=JSONObject(raw)
            if(code !in 200..299||!j.optBoolean("ok",false))throw IllegalStateException(j.optString("error").ifBlank{"HTTP $code"})
            return j
        } finally {c.disconnect()}
    }
    private fun body(context:Context)=JSONObject().put("installId",InstallIdentity.get(context))
    suspend fun list(context:Context):List<CloudPhone> = withContext(Dispatchers.IO){
        val j=request("GET","/api/phones?installId="+Uri.encode(InstallIdentity.get(context)))
        val a=j.optJSONArray("phones")?:org.json.JSONArray()
        (0 until a.length()).mapNotNull{i->a.optJSONObject(i)?.let{o->CloudPhone(o.optString("id"),o.optString("name"),o.optInt("status",-1),o.optString("ip"))}}
    }
    suspend fun create(context:Context):CloudPhone=withContext(Dispatchers.IO){
        val j=request("POST","/api/phones/create",body(context))
        val p=j.getJSONObject("phone")
        CloudPhone(p.getString("id"),p.getString("name"),p.optInt("status",0))
    }
    suspend fun power(context:Context,id:String,on:Boolean)=withContext(Dispatchers.IO){
        request("POST","/api/phones/"+Uri.encode(id)+"/power",body(context).put("on",on))
    }
    suspend fun link(context:Context,id:String):String=withContext(Dispatchers.IO){
        request("POST","/api/phones/"+Uri.encode(id)+"/link",body(context)).getString("url")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MorePhonesApp(){
    val context=LocalContext.current
    val scope= rememberCoroutineScope()
    var phones by remember{mutableStateOf<List<CloudPhone>>(emptyList())}
    var busy by remember{mutableStateOf(false)}
    var notice by remember{mutableStateOf("اضغط «＋ هاتف جديد» لإنشاء Android مستقل على السحابة.")}
    fun task(block:suspend()->Unit){if(busy)return;busy=true;scope.launch{try{block()}catch(e:Exception){notice=e.message?:"حدث خطأ"}finally{busy=false}}}
    fun refresh(){task{phones=MorePhonesService.list(context);notice="تم تحديث الهواتف المرتبطة بهذا التطبيق."}}
    fun createPhone(){task{
        notice="جارٍ إرسال طلب إنشاء هاتف Android جديد..."
        val created=MorePhonesService.create(context)
        notice="تم إنشاء الطلب لـ ${created.name}. جارٍ انتظار تجهيز الجهاز..."
        var ready:CloudPhone?=null
        repeat(40){
            delay(3000)
            val found=MorePhonesService.list(context)
            phones=found
            val p=found.firstOrNull{it.id==created.id}
            if(p!=null&&(p.status==2||p.status==3||p.status==4))ready=p
            if(p?.status==1)throw IllegalStateException("مزود الخدمة أبلغ عن فشل إنشاء الهاتف.")
            if(ready!=null)return@repeat
        }
        val p=ready?:throw IllegalStateException("تم قبول طلب الإنشاء، لكن التجهيز لم يكتمل بعد. اضغط تحديث بعد قليل.")
        if(p.status!=4){MorePhonesService.power(context,p.id,true);notice="الهاتف جاهز ويجري تشغيله. حدّث القائمة بعد قليل."}
        else {val url=MorePhonesService.link(context,p.id);context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)));notice="${p.name} جاهز وتم فتح شاشة التحكم."}
    }}
    MaterialTheme(colorScheme=darkColorScheme(primary=Cyan,onPrimary=Ink,background=Ink,surface=Panel,onSurface=Color.White,secondary=Gold)){
        Scaffold(containerColor=Ink,topBar={TopAppBar(title={Column{Text("MORE PHONES",fontWeight=FontWeight.Black,letterSpacing=2.sp,color=Color.White);Text("CLOUD ANDROID FLEET",color=Cyan,fontSize=9.sp,letterSpacing=2.sp)}},actions={IconButton(onClick={refresh()},enabled=!busy){Icon(Icons.Default.Refresh,"تحديث",tint=Cyan)}},colors=TopAppBarDefaults.topAppBarColors(containerColor=Ink))}){padding->
            LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                item{Card(shape=RoundedCornerShape(25.dp),border=BorderStroke(1.dp,Cyan.copy(alpha=.4f)),colors=CardDefaults.cardColors(containerColor=Panel)){
                    Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF123A57),Panel,Color(0xFF1E2333))))){
                        Column(Modifier.padding(19.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Cloud,null,tint=Cyan,modifier=Modifier.size(42.dp));Spacer(Modifier.width(12.dp));Column{Text("كل مساحة = هاتف Android مستقل",fontSize=17.sp,fontWeight=FontWeight.ExtraBold);Text("REMOTE CLOUD DEVICES",color=Gold,fontSize=9.sp)}}
                            Text("أنشئ هواتف Android منفصلة، لكل هاتف تطبيقاته وبياناته. لا توجد خانات API أو Proxy داخل التطبيق.",color=Color(0xFFD1E0EC),fontSize=12.sp,lineHeight=19.sp)
                            Button(onClick={createPhone()},enabled=!busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("＋ هاتف جديد بضغطة واحدة")}
                        }
                    }
                }}
                item{SectionTitle("هواتف Android السحابية","01 / CLOUD INSTANCES")}
                if(phones.isEmpty())item{InfoCard("لا توجد هواتف ظاهرة بعد","أنشئ أول هاتف من الزر أعلاه. يجب أن تكون خدمة MoreLogin الخلفية مهيأة بحساب مزود فعلي.")}
                items(phones,key={it.id}){phone->
                    Card(colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Color(0xFF263D55)),shape=RoundedCornerShape(19.dp)){
                        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Android,null,tint=Cyan,modifier=Modifier.size(32.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(phone.name,fontWeight=FontWeight.Bold,fontSize=15.sp);Text("ID: "+phone.id,color=Muted,fontSize=9.sp)};StatusPill(phone.status)}
                            Text(if(phone.ip.isNotBlank())"IP خروج: "+phone.ip else "IP خروج: غير متحقق",color=if(phone.ip.isNotBlank())Gold else Muted,fontSize=10.sp)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
                                Button(onClick={task{MorePhonesService.power(context,phone.id,!phone.isOn);phones=MorePhonesService.list(context);notice="تم إرسال أمر الطاقة لـ ${phone.name}."}},enabled=!busy,modifier=Modifier.weight(1f)){Icon(Icons.Default.PowerSettingsNew,null);Spacer(Modifier.width(5.dp));Text(if(phone.isOn)"إيقاف" else "تشغيل")}
                                OutlinedButton(onClick={task{val url=MorePhonesService.link(context,phone.id);context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)));notice="تم فتح جلسة التحكم."}},enabled=!busy&&phone.status==4,modifier=Modifier.weight(1f)){Icon(Icons.Default.OpenInBrowser,null);Spacer(Modifier.width(5.dp));Text("فتح Android")}
                            }
                        }
                    }
                }
                item{Text(if(busy)"جارٍ تنفيذ الطلب..." else notice,color=if(busy)Cyan else Muted,fontSize=11.sp,lineHeight=16.sp)}
                item{Text("MORE PHONES • ONE-TAP CLOUD ANDROID",color=Muted,fontSize=10.sp,letterSpacing=1.5.sp)}
            }
        }
    }
}
@Composable private fun SectionTitle(title:String,overline:String){Column(verticalArrangement=Arrangement.spacedBy(3.dp)){Text(overline,color=Cyan,fontSize=9.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold);Text(title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=17.sp)}}
@Composable private fun InfoCard(title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(title,color=Color.White,fontWeight=FontWeight.Bold);Text(body,color=Muted,fontSize=11.sp,lineHeight=17.sp)}}}
@Composable private fun StatusPill(status:Int){
    val label=when(status){0,7->"جارٍ الإنشاء";1->"فشل الإنشاء";2->"متوقف";3->"يقلع";4->"يعمل";5,6->"جارٍ التنفيذ";else->"غير معروف"}
    val tint=when(status){4->Color(0xFF55D69E);3,0,5,6,7->Gold;1->Color(0xFFFF6B6B);else->Muted}
    Surface(color=tint.copy(alpha=.14f),shape=RoundedCornerShape(30.dp)){Text(label,color=tint,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp))}
}
