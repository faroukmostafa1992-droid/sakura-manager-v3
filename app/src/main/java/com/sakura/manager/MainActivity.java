package com.sakura.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
  private static final String SUPABASE_URL="https://hoqsljlpswkvflxnywdp.supabase.co";
  private static final String SUPABASE_KEY="sb_publishable_OvtdtOQMjkqOttkjKO7hAw_VPV47Upi";
  private static final String ADMIN_UID="d0d99fb5-37ab-4daa-8049-198996b326e0";
  private final int cream=Color.rgb(250,246,239), pink=Color.rgb(232,190,197), ink=Color.rgb(62,52,53), muted=Color.rgb(130,116,113);
  private JSONArray products, sales, expenses, orders, cloudProducts=new JSONArray(), cloudVariants=new JSONArray();
  private TextView salesSummary;
  private long pendingImageProductId=-1;
  private LinearLayout page, list;
  private TextView summary, cloudStatus;
  private AlertDialog ordersDialog;
  private LinearLayout cloudList;
  private android.content.SharedPreferences prefs;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    prefs=getPreferences(0);
    getWindow().setStatusBarColor(cream);getWindow().setNavigationBarColor(cream);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    load();
    if(prefs.getString("access_token",null)==null) showLogin(); else render();
  }
  private void load(){try{products=new JSONArray(prefs.getString("products","[]"));}catch(Exception e){products=new JSONArray();}try{sales=new JSONArray(prefs.getString("sales","[]"));}catch(Exception e){sales=new JSONArray();}try{expenses=new JSONArray(prefs.getString("expenses","[]"));}catch(Exception e){expenses=new JSONArray();}try{orders=new JSONArray(prefs.getString("orders","[]"));}catch(Exception e){orders=new JSONArray();}sanitizeInventoryNumbers();migrateLegacyReservations();}
  private double safeNumber(JSONObject p,String key){double n=p==null?0:p.optDouble(key,0);return Double.isFinite(n)?n:0;}
  private void sanitizeInventoryNumbers(){try{for(int i=0;i<products.length();i++){JSONObject p=products.optJSONObject(i);if(p==null)continue;for(String key:new String[]{"stock","reserved_stock","cost","price"}){double n=safeNumber(p,key);if(!p.has(key)||!Double.isFinite(p.optDouble(key,0)))p.put(key,n);}}persist();}catch(Exception ignored){}}
  private void migrateLegacyReservations(){if(prefs.getBoolean("reservation_migration_v1",false))return;try{for(int i=0;i<orders.length();i++){JSONObject o=orders.optJSONObject(i);if(o==null||!o.optBoolean("inventory_reserved"))continue;String state=o.optString("local_state",o.optString("status"));if(!("new".equalsIgnoreCase(state)||"pending".equalsIgnoreCase(state)))continue;JSONArray its=o.optJSONArray("items");if(its==null)continue;for(int j=0;j<its.length();j++){JSONObject it=its.optJSONObject(j);if(it==null)continue;JSONObject p=findLocalProduct(it.optString("product_name"),it.optString("size"));if(p!=null){double q=it.optDouble("quantity");p.put("stock",p.optDouble("stock")+q);p.put("reserved_stock",p.optDouble("reserved_stock")+q);}}}prefs.edit().putBoolean("reservation_migration_v1",true).apply();persist();persistOrders();}catch(Exception ignored){}}
  private void persist(){prefs.edit().putString("products",products.toString()).apply();}
  private void persistSales(){prefs.edit().putString("sales",sales.toString()).apply();}
  private void persistExpenses(){prefs.edit().putString("expenses",expenses.toString()).apply();}
  private void persistOrders(){prefs.edit().putString("orders",orders.toString()).apply();}
  private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
  private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ink);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,-2);}
  private void showLogin(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(30),dp(24),dp(30));root.setBackgroundColor(cream);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    TextView brand=text("SAKURA",18,true);brand.setTextColor(Color.rgb(160,112,122));brand.setGravity(Gravity.CENTER);root.addView(brand,lp());
    TextView title=text("تسجيل الدخول للإدارة",23,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=lp();tp.topMargin=dp(12);root.addView(title,tp);
    TextView hint=text("استخدم حساب Sakura المسجل في Supabase",14,false);hint.setTextColor(muted);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=lp();hp.topMargin=dp(6);root.addView(hint,hp);
    EditText email=field("البريد الإلكتروني","");email.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);LinearLayout.LayoutParams ep=lp();ep.topMargin=dp(24);root.addView(email,ep);
    EditText password=field("كلمة المرور","");password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);LinearLayout.LayoutParams pp=lp();pp.topMargin=dp(10);root.addView(password,pp);
    Button login=button("دخول");LinearLayout.LayoutParams bp=lp();bp.topMargin=dp(18);root.addView(login,bp);
    TextView status=text("",13,false);status.setTextColor(Color.rgb(170,65,75));status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams st=lp();st.topMargin=dp(12);root.addView(status,st);
    login.setOnClickListener(v->{
      String em=email.getText().toString().trim(), pw=password.getText().toString();
      if(em.isEmpty()||pw.isEmpty()){status.setText("اكتب البريد الإلكتروني وكلمة المرور");return;}
      login.setEnabled(false);status.setText("جاري تسجيل الدخول...");
      ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(password.getWindowToken(),0);
      new Thread(()->{
        HttpURLConnection conn=null;
        try{
          URL url=new URL(SUPABASE_URL+"/auth/v1/token?grant_type=password");
          conn=(HttpURLConnection)url.openConnection();conn.setRequestMethod("POST");conn.setConnectTimeout(15000);conn.setReadTimeout(15000);conn.setDoOutput(true);
          conn.setRequestProperty("apikey",SUPABASE_KEY);conn.setRequestProperty("Content-Type","application/json");
          JSONObject body=new JSONObject();body.put("email",em);body.put("password",pw);
          try(OutputStream os=conn.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
          int code=conn.getResponseCode();InputStream is=code>=200&&code<300?conn.getInputStream():conn.getErrorStream();
          ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] data=new byte[4096];int n;while((n=is.read(data))!=-1)buffer.write(data,0,n);
          JSONObject response=new JSONObject(buffer.toString("UTF-8"));
          if(code>=200&&code<300&&response.has("access_token")){
            String token=response.getString("access_token"),refresh=response.optString("refresh_token","");
            String uid=response.optJSONObject("user")==null?"":response.getJSONObject("user").optString("id","");
            if(!ADMIN_UID.equals(uid)){runOnUiThread(()->{status.setText("هذا الحساب غير مصرح له بإدارة Sakura");login.setEnabled(true);});return;}
            prefs.edit().putString("access_token",token).putString("refresh_token",refresh).putString("auth_uid",uid).apply();
            runOnUiThread(()->{password.setText("");render();});
          }else{
            String msg=response.optString("msg",response.optString("message","تعذر تسجيل الدخول. راجع البيانات."));
            runOnUiThread(()->{status.setText(msg);login.setEnabled(true);});
          }
        }catch(Exception ex){runOnUiThread(()->{status.setText("تعذر الاتصال. تحقق من الإنترنت وحاول مجددًا.");login.setEnabled(true);});}
        finally{if(conn!=null)conn.disconnect();}
      }).start();
    });
    setContentView(root);
  }
  private void render(){
    ScrollView sc=new ScrollView(this);sc.setBackgroundColor(cream);page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(18),dp(24),dp(18),dp(28));page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sc.addView(page);
    LinearLayout top=new LinearLayout(this);top.setOrientation(0);top.setGravity(Gravity.CENTER_VERTICAL);page.addView(top,lp());
    TextView brand=text("SAKURA",14,true);brand.setTextColor(Color.rgb(160,112,122));top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
    Button logout=new Button(this);logout.setText("تسجيل خروج");logout.setAllCaps(false);logout.setTextSize(12);logout.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("تسجيل الخروج من حساب الإدارة؟").setNegativeButton("إلغاء",null).setPositiveButton("خروج",(d,w)->{prefs.edit().remove("access_token").remove("refresh_token").remove("auth_uid").apply();showLogin();}).show());top.addView(logout);
    page.addView(text("إدارة Sakura Cosmetics",25,true));
    TextView note=text("إدارة المنتجات والمخزون • إصدار 3.2",14,false);note.setTextColor(muted);page.addView(note);
    summary=text("",15,true);summary.setPadding(dp(16),dp(16),dp(16),dp(16));summary.setBackground(bg(Color.WHITE,18));LinearLayout.LayoutParams sp=lp();sp.topMargin=dp(20);page.addView(summary,sp);
    salesSummary=text("",15,true);salesSummary.setPadding(dp(16),dp(16),dp(16),dp(16));salesSummary.setBackground(bg(Color.WHITE,18));LinearLayout.LayoutParams ssp=lp();ssp.topMargin=dp(12);page.addView(salesSummary,ssp);refreshSales();
    Button addSale=button("＋ تسجيل عملية بيع");LinearLayout.LayoutParams salep=lp();salep.topMargin=dp(10);page.addView(addSale,salep);addSale.setOnClickListener(v->saleDialog());
    Button salesHistory=button("سجل المبيعات");LinearLayout.LayoutParams histp=lp();histp.topMargin=dp(7);page.addView(salesHistory,histp);salesHistory.setOnClickListener(v->salesHistoryDialog());
    Button orderButton=button("📦 طلبات المتجر");LinearLayout.LayoutParams orderP=lp();orderP.topMargin=dp(10);page.addView(orderButton,orderP);orderButton.setOnClickListener(v->fetchOrders());
    Button addExpense=button("＋ إضافة إعلان / مصروف");LinearLayout.LayoutParams expP=lp();expP.topMargin=dp(8);page.addView(addExpense,expP);addExpense.setOnClickListener(v->expenseDialog());
    Button expenseHistory=button("سجل الإعلانات والمصروفات");LinearLayout.LayoutParams expHistP=lp();expHistP.topMargin=dp(7);page.addView(expenseHistory,expHistP);expenseHistory.setOnClickListener(v->expenseHistoryDialog());
    Button add=button("＋ إضافة منتج جديد");LinearLayout.LayoutParams ap=lp();ap.topMargin=dp(14);page.addView(add,ap);add.setOnClickListener(v->productDialog(-1));
    Button importCatalog=button("⇄ ربط كتالوج المتجر بالمخزون");LinearLayout.LayoutParams ip=lp();ip.topMargin=dp(8);page.addView(importCatalog,ip);importCatalog.setOnClickListener(v->syncCatalogToInventory());
    TextView h=text("المنتجات",19,true);h.setPadding(0,dp(24),0,dp(10));page.addView(h);
    list=new LinearLayout(this);list.setOrientation(1);page.addView(list);refresh();
    TextView cloudTitle=text("كتالوج المتجر المباشر",19,true);cloudTitle.setPadding(0,dp(26),0,dp(8));page.addView(cloudTitle);
    Button sync=button("↻ تحديث منتجات المتجر");LinearLayout.LayoutParams syncp=lp();syncp.bottomMargin=dp(8);page.addView(sync,syncp);
    cloudStatus=text("لم يتم تحميل كتالوج المتجر بعد",13,false);cloudStatus.setTextColor(muted);page.addView(cloudStatus);
    cloudList=new LinearLayout(this);cloudList.setOrientation(1);page.addView(cloudList);
    sync.setOnClickListener(v->fetchCloudCatalog());
    fetchCloudCatalog();
    TextView foot=text("المخزون والتكلفة يظلان محليين على هذا الجهاز. كتالوج المتجر وأسعاره تُقرأ مباشرة من Supabase.",12,false);foot.setTextColor(muted);foot.setPadding(0,dp(24),0,0);page.addView(foot);
    setContentView(sc);
  }
  private void fetchCloudCatalog(){
    if(cloudStatus!=null)cloudStatus.setText("جاري تحميل منتجات المتجر...");
    new Thread(()->{
      try{
        JSONArray p=cloudGet("/rest/v1/products?select=id,name,description,image_url,active&active=eq.true&order=id.asc");
        JSONArray v=cloudGet("/rest/v1/product_variants?select=id,product_id,size,price,active&active=eq.true&order=id.asc");
        runOnUiThread(()->{cloudProducts=p;cloudVariants=v;renderCloudCatalog();});
      }catch(Exception e){
        runOnUiThread(()->{if(cloudStatus!=null)cloudStatus.setText("فشل تحميل الكتالوج: "+(e.getMessage()==null?"خطأ غير معروف":e.getMessage()));});
      }
    }).start();
  }
  private void chooseProductImage(long productId){
    if(productId<=0){Toast.makeText(this,"معرّف المنتج غير صالح",Toast.LENGTH_LONG).show();return;}
    pendingImageProductId=productId;
    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(intent,7101);
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode!=7101||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
    Uri uri=data.getData();
    try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
    final long productId=pendingImageProductId;pendingImageProductId=-1;
    if(productId<=0)return;
    if(cloudStatus!=null)cloudStatus.setText("جاري رفع صورة المنتج...");
    new Thread(()->{
      try{
        String mime=getContentResolver().getType(uri);
        if(mime==null||!mime.startsWith("image/"))throw new Exception("الملف المختار ليس صورة");
        String ext="jpg";
        if("image/png".equalsIgnoreCase(mime))ext="png";
        else if("image/webp".equalsIgnoreCase(mime))ext="webp";
        else if("image/jpeg".equalsIgnoreCase(mime))ext="jpg";
        else throw new Exception("اختر صورة JPG أو PNG أو WEBP");
        byte[] bytes;
        try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
          if(in==null)throw new Exception("تعذر قراءة الصورة");
          byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>12*1024*1024)throw new Exception("حجم الصورة أكبر من 12 ميجابايت");}
          bytes=out.toByteArray();
        }
        String path="products/"+productId+"_"+java.util.UUID.randomUUID().toString().replace("-","")+"."+ext;
        if(!refreshSession())throw new Exception("انتهت جلسة الدخول. سجّل الدخول مرة أخرى.");
        String token=prefs.getString("access_token","");
        if(token.isEmpty())throw new Exception("انتهت جلسة الدخول. سجّل الدخول مرة أخرى.");
        HttpURLConnection upload=(HttpURLConnection)new URL(SUPABASE_URL+"/storage/v1/object/sakura-products/"+path).openConnection();
        upload.setRequestMethod("POST");upload.setConnectTimeout(20000);upload.setReadTimeout(30000);upload.setDoOutput(true);
        upload.setRequestProperty("apikey",SUPABASE_KEY);upload.setRequestProperty("Authorization","Bearer "+token);
        upload.setRequestProperty("Content-Type",mime);upload.setRequestProperty("x-upsert","false");
        try(OutputStream os=upload.getOutputStream()){os.write(bytes);}
        int uploadCode=upload.getResponseCode();InputStream uploadStream=uploadCode>=200&&uploadCode<300?upload.getInputStream():upload.getErrorStream();
        String uploadBody=readBody(uploadStream);upload.disconnect();
        if(uploadCode<200||uploadCode>=300)throw new Exception("فشل رفع الصورة ("+uploadCode+"): "+uploadBody);
        String publicUrl=SUPABASE_URL+"/storage/v1/object/public/sakura-products/"+path;
        HttpURLConnection update=(HttpURLConnection)new URL(SUPABASE_URL+"/rest/v1/products?id=eq."+productId).openConnection();
        update.setRequestMethod("PATCH");update.setConnectTimeout(15000);update.setReadTimeout(15000);update.setDoOutput(true);
        update.setRequestProperty("apikey",SUPABASE_KEY);update.setRequestProperty("Authorization","Bearer "+token);
        update.setRequestProperty("Content-Type","application/json");update.setRequestProperty("Prefer","return=minimal");
        JSONObject body=new JSONObject();body.put("image_url",publicUrl);
        try(OutputStream os=update.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int updateCode=update.getResponseCode();InputStream updateStream=updateCode>=200&&updateCode<300?update.getInputStream():update.getErrorStream();
        String updateBody=readBody(updateStream);update.disconnect();
        if(updateCode<200||updateCode>=300)throw new Exception("تم رفع الصورة لكن تعذر تحديث المنتج ("+updateCode+"): "+updateBody);
        runOnUiThread(()->{for(int i=0;i<cloudProducts.length();i++){JSONObject p=cloudProducts.optJSONObject(i);if(p!=null&&p.optLong("id",-1)==productId)try{p.put("image_url",publicUrl);}catch(Exception ignored){}}renderCloudCatalog();Toast.makeText(this,"تم رفع الصورة وتحديث رابط المنتج في المتجر",Toast.LENGTH_LONG).show();});
      }catch(Exception e){String msg=e.getMessage()==null?"حدث خطأ أثناء رفع الصورة":e.getMessage();runOnUiThread(()->{if(cloudStatus!=null)cloudStatus.setText(msg);Toast.makeText(this,msg,Toast.LENGTH_LONG).show();});}
    }).start();
  }
  private String readBody(InputStream is)throws Exception{
    if(is==null)return "";
    try(InputStream in=is;ByteArrayOutputStream b=new ByteArrayOutputStream()){
      byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);
      return b.toString("UTF-8");
    }
  }
  private synchronized boolean refreshSession()throws Exception{
    String refresh=prefs.getString("refresh_token","");
    if(refresh.isEmpty())return false;
    HttpURLConnection c=(HttpURLConnection)new URL(SUPABASE_URL+"/auth/v1/token?grant_type=refresh_token").openConnection();
    c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setDoOutput(true);
    c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Content-Type","application/json");
    JSONObject req=new JSONObject();req.put("refresh_token",refresh);
    try(OutputStream os=c.getOutputStream()){os.write(req.toString().getBytes(StandardCharsets.UTF_8));}
    int code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();
    String body=readBody(is);c.disconnect();
    if(code<200||code>=300)throw new Exception("انتهت جلسة الدخول. سجّل الدخول مرة أخرى: "+body);
    JSONObject res=new JSONObject(body);String uid=res.optJSONObject("user")==null?"":res.getJSONObject("user").optString("id","");
    if(!ADMIN_UID.equals(uid))throw new Exception("حساب الإدارة غير مصرح له");
    prefs.edit().putString("access_token",res.getString("access_token")).putString("refresh_token",res.optString("refresh_token",refresh)).putString("auth_uid",uid).apply();
    return true;
  }
  private JSONArray cloudGet(String path)throws Exception{
    for(int attempt=0;attempt<2;attempt++){
      String token=prefs.getString("access_token",SUPABASE_KEY);
      HttpURLConnection c=(HttpURLConnection)new URL(SUPABASE_URL+path).openConnection();
      c.setRequestMethod("GET");c.setConnectTimeout(15000);c.setReadTimeout(15000);
      c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");
      int code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();
      String body=readBody(is);c.disconnect();
      if(code>=200&&code<300)return new JSONArray(body);
      if(code==401&&attempt==0&&refreshSession())continue;
      throw new Exception("Supabase HTTP "+code+": "+body);
    }
    throw new Exception("تعذر تجديد جلسة الدخول");
  }
  private void fetchOrders(){
    Toast.makeText(this,"جاري تحميل طلبات المتجر...",Toast.LENGTH_SHORT).show();
    new Thread(()->{
      try{
        JSONArray remote=cloudGet("/rest/v1/orders?select=id,customer_name,phone,address,total,status,created_at&order=created_at.desc");
        JSONArray merged=new JSONArray();String warning="";
        for(int i=0;i<remote.length();i++){
          JSONObject ro=remote.optJSONObject(i);if(ro==null)continue;
          long id=ro.optLong("id",-1);JSONObject local=findOrder(id);
          JSONArray items=cloudGet("/rest/v1/order_items?select=id,order_id,product_name,size,quantity,price&order_id=eq."+id);
          JSONObject o=local==null?new JSONObject():new JSONObject(local.toString());
          o.put("id",id);o.put("customer_name",ro.optString("customer_name"));o.put("phone",ro.optString("phone"));o.put("address",ro.optString("address"));o.put("total",ro.optDouble("total"));o.put("status",ro.optString("status","new"));o.put("created_at",ro.optString("created_at"));o.put("items",items);
          if(local==null && ("new".equalsIgnoreCase(o.optString("status"))||"pending".equalsIgnoreCase(o.optString("status")))){
            boolean ok=true;
            for(int j=0;j<items.length();j++){JSONObject it=items.optJSONObject(j);JSONObject p=findLocalProduct(it==null?"":it.optString("product_name"),it==null?"":it.optString("size"));if(p==null||p.optDouble("stock",0)-p.optDouble("reserved_stock",0)<(it==null?0:it.optDouble("quantity",0))){ok=false;break;}}
            if(ok){for(int j=0;j<items.length();j++){JSONObject it=items.optJSONObject(j);JSONObject p=findLocalProduct(it.optString("product_name"),it.optString("size"));p.put("reserved_stock",p.optDouble("reserved_stock")+it.optDouble("quantity"));}o.put("inventory_reserved",true);o.put("local_state","new");persist();}
            else {o.put("inventory_reserved",false);warning="\nتنبيه: بعض الطلبات لم يُخصم مخزونها لعدم كفاية الكمية أو عدم تطابق المنتج/الحجم.";}
          }else if(local!=null){o.put("inventory_reserved",local.optBoolean("inventory_reserved",false));o.put("local_state",local.optString("local_state",o.optString("status")));}
          merged.put(o);
        }
        orders=merged;persistOrders();final String warningMessage=warning;runOnUiThread(()->{showOrdersDialog(warningMessage);refresh();});
      }catch(Exception e){String msg=e.getMessage()==null?"تعذر تحميل الطلبات":e.getMessage();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("طلبات المتجر").setMessage(msg).setPositiveButton("حسنًا",null).show());}
    }).start();
  }
  private JSONObject findOrder(long id){for(int i=0;i<orders.length();i++){JSONObject o=orders.optJSONObject(i);if(o!=null&&o.optLong("id",-2)==id)return o;}return null;}
  private JSONObject findLocalProduct(String name,String size){for(int i=0;i<products.length();i++){JSONObject p=products.optJSONObject(i);if(p!=null&&p.optString("name").trim().equalsIgnoreCase(name.trim())&&p.optString("size").trim().equalsIgnoreCase(size.trim()))return p;}return null;}
  private void showOrdersDialog(String warning){
    LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(12),dp(8),dp(12),dp(8));box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    ScrollView sc=new ScrollView(this);LinearLayout cards=new LinearLayout(this);cards.setOrientation(1);sc.addView(cards);
    int pending=0;
    for(int i=0;i<orders.length();i++){
      JSONObject o=orders.optJSONObject(i);if(o==null)continue;
      String state=o.optString("local_state",o.optString("status"));
      if(!("new".equalsIgnoreCase(state)||"pending".equalsIgnoreCase(state)))continue;
      pending++;
      LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackground(bg(Color.WHITE,14));
      LinearLayout.LayoutParams cp=lp();cp.bottomMargin=dp(10);cards.addView(card,cp);
      card.addView(text("طلب #"+o.optLong("id")+"  •  "+money(o.optDouble("total")),16,true));
      card.addView(text(o.optString("customer_name")+"  •  "+o.optString("phone"),13,false));
      card.addView(text("العنوان: "+o.optString("address"),13,false));
      JSONArray its=o.optJSONArray("items");
      if(its!=null)for(int j=0;j<its.length();j++){JSONObject it=its.optJSONObject(j);if(it!=null)card.addView(text("• "+it.optString("product_name")+" / "+it.optString("size")+" × "+fmt(it.optDouble("quantity"))+" — "+money(it.optDouble("price")*it.optDouble("quantity")),13,false));}
      card.addView(text("تاريخ الطلب: "+o.optString("created_at"),12,false));
      LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
      Button ship=button("✓ تم الشحن"),cancel=button("✕ إلغاء");
      LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,-2,1);ap.topMargin=dp(8);ap.leftMargin=dp(4);actions.addView(ship,ap);
      LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.topMargin=dp(8);bp.rightMargin=dp(4);actions.addView(cancel,bp);card.addView(actions);
      ship.setOnClickListener(v->changeOrderState(o,"shipped",null));
      cancel.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("إلغاء الطلب؟ لن يتم تسجيل مبيعات أو خصم نهائي للمخزون.").setNegativeButton("رجوع",null).setPositiveButton("تأكيد الإلغاء",(d,w)->changeOrderState(o,"cancelled",null)).show());
    }
    if(pending==0)cards.addView(text("لا توجد طلبات معلقة حاليًا.",14,false));
    if(warning!=null&&!warning.isEmpty())cards.addView(text(warning,12,false));
    box.addView(sc,new LinearLayout.LayoutParams(-1,dp(450)));
    ordersDialog=new AlertDialog.Builder(this).setTitle("طلبات المتجر ("+pending+")").setView(box).setPositiveButton("تحديث",(d,w)->fetchOrders()).setNegativeButton("إغلاق",null).create();ordersDialog.show();
  }
  private String orderStatusLabel(String s){if("shipped".equalsIgnoreCase(s))return "تم الشحن";if("cancelled".equalsIgnoreCase(s))return "ملغي";return "طلب جديد";}
  private void changeOrderState(JSONObject o,String next,AlertDialog dlg){
    long id=o.optLong("id",-1);if(id<0)return;
    new Thread(()->{try{
      cloudPatch("/rest/v1/orders?id=eq."+id+"&status=eq."+o.optString("status"),new JSONObject().put("status",next));
      runOnUiThread(()->{try{
        JSONArray its=o.optJSONArray("items");double costTotal=0,qtyTotal=0;
        if(its!=null)for(int j=0;j<its.length();j++){
          JSONObject it=its.optJSONObject(j);if(it==null)continue;
          JSONObject p=findLocalProduct(it.optString("product_name"),it.optString("size"));
          double q=it.optDouble("quantity"),cost=p==null?0:p.optDouble("cost",0);
          costTotal+=q*cost;qtyTotal+=q;
          if(p!=null){
            double reserved=p.optDouble("reserved_stock",0);
            if("shipped".equals(next)){p.put("stock",Math.max(0,p.optDouble("stock")-q));p.put("reserved_stock",Math.max(0,reserved-q));}
            else if("cancelled".equals(next)&&o.optBoolean("inventory_reserved"))p.put("reserved_stock",Math.max(0,reserved-q));
          }
        }
        if("shipped".equals(next)){
          boolean exists=false;for(int k=0;k<sales.length();k++){JSONObject old=sales.optJSONObject(k);if(old!=null&&id==old.optLong("order_id",-2)){exists=true;break;}}
          if(!exists){JSONObject sale=new JSONObject();sale.put("order_id",id);sale.put("product","طلب متجر #"+id);sale.put("size","متعدد");sale.put("quantity",qtyTotal);sale.put("unit_price",0);sale.put("unit_cost",0);sale.put("cost_total",costTotal);sale.put("shipping",0);sale.put("discount",0);sale.put("total",o.optDouble("total"));sale.put("date",new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new java.util.Date()));sales.put(sale);persistSales();}
        }
        persist();o.put("status",next);o.put("local_state",next);o.put("inventory_reserved",false);persistOrders();refresh();refreshSales();
        Toast.makeText(this,"تم تحديث الطلب: "+orderStatusLabel(next),Toast.LENGTH_LONG).show();
        if(dlg!=null&&dlg.isShowing())dlg.dismiss();if(ordersDialog!=null&&ordersDialog.isShowing())ordersDialog.dismiss();showOrdersDialog("");
      }catch(Exception ex){Toast.makeText(this,"حدث خطأ أثناء تحديث بيانات الطلب",Toast.LENGTH_LONG).show();}});
    }catch(Exception ex){String m=ex.getMessage()==null?"تعذر تحديث حالة الطلب":ex.getMessage();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("تعذر تحديث الطلب").setMessage(m).setPositiveButton("حسنًا",null).show());}}).start();
  }
  private void cloudPatch(String path,JSONObject body)throws Exception{
    if(!refreshSession())throw new Exception("سجّل الدخول مرة أخرى");
    HttpURLConnection c=(HttpURLConnection)new URL(SUPABASE_URL+path).openConnection();c.setRequestMethod("PATCH");c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setDoOutput(true);
    c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Authorization","Bearer "+prefs.getString("access_token",""));c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Prefer","return=representation");
    try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
    int code=c.getResponseCode();String response=readBody(code>=200&&code<300?c.getInputStream():c.getErrorStream());c.disconnect();
    if(code<200||code>=300)throw new Exception("Supabase HTTP "+code+": "+response);
    if(response.trim().equals("[]"))throw new Exception("الطلب تغيرت حالته بالفعل أو لا توجد صلاحية لتحديثه. حدّث قائمة الطلبات.");
  }
  private void renderCloudCatalog(){
    if(cloudList==null||cloudStatus==null)return;
    cloudList.removeAllViews();
    cloudStatus.setText("تم تحميل "+cloudProducts.length()+" منتجات و"+cloudVariants.length()+" حجم من المتجر");
    for(int i=0;i<cloudProducts.length();i++){
      JSONObject p=cloudProducts.optJSONObject(i);if(p==null)continue;
      LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(bg(Color.WHITE,15));
      LinearLayout.LayoutParams cp=lp();cp.topMargin=dp(8);cloudList.addView(card,cp);
      card.addView(text(p.optString("name","منتج"),16,true));
      Button imageButton=button("اختيار / تغيير صورة المنتج");
      LinearLayout.LayoutParams imageButtonParams=lp();imageButtonParams.topMargin=dp(8);card.addView(imageButton,imageButtonParams);
      final long cloudId=p.optLong("id",-1);imageButton.setOnClickListener(v->chooseProductImage(cloudId));
      String desc=p.optString("description","");if(!desc.isEmpty()){TextView d=text(desc,12,false);d.setTextColor(muted);card.addView(d);}
      boolean found=false;
      for(int j=0;j<cloudVariants.length();j++){
        JSONObject v=cloudVariants.optJSONObject(j);if(v==null||v.optLong("product_id",-1)!=p.optLong("id",-2))continue;
        found=true;TextView line=text(v.optString("size","")+"  •  "+money(v.optDouble("price",0)),13,false);line.setTextColor(muted);line.setPadding(0,dp(5),0,0);card.addView(line);
      }
      if(!found){TextView none=text("لا توجد أحجام نشطة",12,false);none.setTextColor(muted);card.addView(none);}
    }
    if(cloudProducts.length()==0){TextView none=text("لا توجد منتجات نشطة في المتجر",13,false);cloudList.addView(none);}
  }
  private void syncCatalogToInventory(){
    if(cloudProducts.length()==0){Toast.makeText(this,"حمّل كتالوج المتجر أولاً",Toast.LENGTH_LONG).show();return;}
    int added=0,updated=0;
    try{
      for(int i=0;i<cloudProducts.length();i++){
        JSONObject cp=cloudProducts.optJSONObject(i);if(cp==null)continue;
        long productId=cp.optLong("id",-1);
        for(int j=0;j<cloudVariants.length();j++){
          JSONObject cv=cloudVariants.optJSONObject(j);if(cv==null||cv.optLong("product_id",-2)!=productId)continue;
          String size=cv.optString("size","").trim();
          if(size.equalsIgnoreCase("5ml")||size.equalsIgnoreCase("5 ml"))continue;
          long variantId=cv.optLong("id",-1);
          JSONObject local=null;
          for(int k=0;k<products.length();k++){
            JSONObject candidate=products.optJSONObject(k);if(candidate==null)continue;
            boolean sameVariant=variantId>0&&candidate.optLong("cloud_variant_id",-2)==variantId;
            boolean sameProductSize=candidate.optLong("cloud_product_id",-2)==productId&&candidate.optString("size","").equalsIgnoreCase(size);
            boolean sameNameSize=candidate.optString("name","").equalsIgnoreCase(cp.optString("name",""))&&candidate.optString("size","").equalsIgnoreCase(size);
            if(sameVariant||sameProductSize||sameNameSize){local=candidate;break;}
          }
          if(local==null){local=new JSONObject();local.put("stock",0);local.put("cost",0);products.put(local);added++;}
          else updated++;
          local.put("name",cp.optString("name","منتج"));
          local.put("category","Korean skincare");
          local.put("size",size);
          local.put("price",cv.optDouble("price",0));
          local.put("cloud_product_id",productId);
          local.put("cloud_variant_id",variantId);
          local.put("description",cp.optString("description",""));
          local.put("image_url",cp.optString("image_url",""));
        }
      }
      persist();refresh();
      Toast.makeText(this,"تم ربط الكتالوج بالمخزون. جديد: "+added+" • تم تحديث: "+updated+". الكميات والتكلفة المحلية محفوظة.",Toast.LENGTH_LONG).show();
    }catch(Exception e){Toast.makeText(this,"تعذر ربط الكتالوج: "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(ink);b.setBackground(bg(pink,20));return b;}
  private void refresh(){
    list.removeAllViews();double value=0,retail=0;int count=products.length();
    for(int i=0;i<count;i++){JSONObject p=products.optJSONObject(i);if(p==null)continue;double q=p.optDouble("stock",0),c=p.optDouble("cost",0),price=p.optDouble("price",0);value+=q*c;retail+=q*price;
      LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(15),dp(13),dp(15),dp(13));card.setBackground(bg(Color.WHITE,16));LinearLayout.LayoutParams cp=lp();cp.bottomMargin=dp(10);list.addView(card,cp);
      TextView name=text(p.optString("name","منتج"),17,true);card.addView(name);
      TextView details=text(p.optString("size","Full Size")+"  •  المخزون: "+fmt(q)+"  •  المحجوز: "+fmt(p.optDouble("reserved_stock"))+"  •  المتاح: "+fmt(Math.max(0,q-p.optDouble("reserved_stock")))+"  •  سعر البيع: "+money(price),13,false);details.setTextColor(muted);details.setPadding(0,dp(5),0,dp(2));card.addView(details);
      TextView cost=text("التكلفة للوحدة: "+money(c)+"  •  قيمة المخزون بسعر البيع: "+money(q*price),13,false);cost.setTextColor(muted);card.addView(cost);
      LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);Button edit=new Button(this);edit.setText("تعديل");edit.setAllCaps(false);edit.setOnClickListener(v->productDialog(indexOf(p)));Button del=new Button(this);del.setText("حذف");del.setAllCaps(false);del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("حذف المنتج من القائمة؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(d,w)->{remove(indexOf(p));}).show());actions.addView(edit);actions.addView(del);card.addView(actions);
    }
    summary.setText("عدد المنتجات: "+count+"\nقيمة المخزون بالتكلفة: "+money(value)+"\nقيمة البيع المتوقعة: "+money(retail));
    refreshSales();
    if(count==0){TextView empty=text("لا توجد منتجات بعد. اضغط إضافة منتج جديد للبدء.",15,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(12),dp(24),dp(12),dp(24));list.addView(empty);}
  }

  private void refreshSales(){
    if(salesSummary==null||sales==null)return;
    double revenue=0,costTotal=0,ads=0,other=0;int units=0;
    for(int i=0;i<sales.length();i++){JSONObject s=sales.optJSONObject(i);if(s==null)continue;revenue+=s.optDouble("total",0);costTotal+=s.optDouble("cost_total",0);units+=s.optInt("quantity",0);}
    for(int i=0;i<expenses.length();i++){JSONObject e=expenses.optJSONObject(i);if(e==null)continue;double amount=e.optDouble("amount",0);if("إعلانات".equals(e.optString("category")))ads+=amount;else other+=amount;}
    double allExpenses=ads+other;
    double inventoryRetail=0;for(int i=0;i<products.length();i++){JSONObject p=products.optJSONObject(i);if(p!=null)inventoryRetail+=p.optDouble("stock")*p.optDouble("price");}
    double totalCost=costTotal+allExpenses;
    salesSummary.setText("إجمالي التكلفة شامل المصروفات والإعلانات: "+money(totalCost)+"\nإجمالي المبيعات: "+money(revenue)+"\nقيمة المخزون بسعر البيع: "+money(inventoryRetail)+"\nصافي الأرباح: "+money(revenue-totalCost));
  }
  private void expenseDialog(){
    LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(18),dp(8),dp(18),dp(4));form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    Spinner category=new Spinner(this);String[] categories={"إعلانات","شحن","تغليف وعبوات","مواصلات","عمولات","مصاريف تشغيل","أخرى"};category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories));
    EditText amount=field("قيمة المصروف بالجنيه","");EditText note=field("بيان المصروف (اختياري)","");
    form.addView(label("نوع المصروف"));form.addView(category);form.addView(amount);form.addView(note);
    new AlertDialog.Builder(this).setTitle("إضافة إعلان / مصروف").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{
      try{double value=number(amount);if(value<=0)throw new Exception();JSONObject e=new JSONObject();e.put("category",category.getSelectedItem().toString());e.put("amount",value);e.put("note",note.getText().toString().trim());e.put("date",new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new java.util.Date()));expenses.put(e);persistExpenses();refreshSales();Toast.makeText(this,"تم تسجيل المصروف",Toast.LENGTH_SHORT).show();}
      catch(Exception ex){Toast.makeText(this,"أدخل قيمة صحيحة أكبر من صفر",Toast.LENGTH_LONG).show();}
    }).show();
  }
  private void expenseHistoryDialog(){
    StringBuilder b=new StringBuilder();double total=0;
    for(int i=expenses.length()-1;i>=0;i--){JSONObject e=expenses.optJSONObject(i);if(e==null)continue;double a=e.optDouble("amount",0);total+=a;b.append(e.optString("date")).append("\n").append(e.optString("category")).append(" • ").append(money(a)).append("\n");if(!e.optString("note","").isEmpty())b.append(e.optString("note")).append("\n");b.append("\n");}
    if(b.length()==0)b.append("لا توجد مصروفات مسجلة بعد");else b.insert(0,"إجمالي المصروفات: "+money(total)+"\n\n");
    new AlertDialog.Builder(this).setTitle("سجل الإعلانات والمصروفات").setMessage(b.toString()).setPositiveButton("إغلاق",null).show();
  }
  private void saleDialog(){
    if(products.length()==0){Toast.makeText(this,"أضف المنتجات أولاً",Toast.LENGTH_LONG).show();return;}
    java.util.ArrayList<Integer> available=new java.util.ArrayList<>();java.util.ArrayList<String> names=new java.util.ArrayList<>();
    for(int i=0;i<products.length();i++){JSONObject p=products.optJSONObject(i);if(p!=null){available.add(i);names.add(p.optString("name")+" - "+p.optString("size")+" (متاح "+fmt(p.optDouble("stock"))+")");}}
    if(names.isEmpty())return;
    LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(18),dp(8),dp(18),dp(4));form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    Spinner product=new Spinner(this);product.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
    EditText quantity=field("الكمية المباعة","1");EditText shipping=field("الشحن (اختياري)","0");EditText discount=field("الخصم (اختياري)","0");
    form.addView(label("المنتج"));form.addView(product);form.addView(quantity);form.addView(shipping);form.addView(discount);
    new AlertDialog.Builder(this).setTitle("تسجيل عملية بيع").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("تسجيل",(d,w)->{
      try{
        int idx=available.get(product.getSelectedItemPosition());JSONObject p=products.optJSONObject(idx);
        double q=number(quantity),ship=number(shipping),disc=number(discount),stock=p.optDouble("stock",0);
        if(q<=0||q>stock||ship<0||disc<0)throw new Exception();
        double unitPrice=p.optDouble("price",0),unitCost=p.optDouble("cost",0),subtotal=q*unitPrice,total=subtotal+ship-disc;
        if(total<0)throw new Exception();
        JSONObject sale=new JSONObject();sale.put("product",p.optString("name"));sale.put("size",p.optString("size"));sale.put("quantity",q);sale.put("unit_price",unitPrice);sale.put("unit_cost",unitCost);sale.put("cost_total",q*unitCost);sale.put("shipping",ship);sale.put("discount",disc);sale.put("total",total);sale.put("date",new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new java.util.Date()));
        sales.put(sale);p.put("stock",stock-q);persist();persistSales();refresh();refreshSales();
        Toast.makeText(this,"تم تسجيل البيع وخصم الكمية من المخزون",Toast.LENGTH_LONG).show();
      }catch(Exception ex){Toast.makeText(this,"راجع الكمية والمخزون والشحن والخصم",Toast.LENGTH_LONG).show();}
    }).show();
  }
  private void salesHistoryDialog(){
    StringBuilder b=new StringBuilder();
    for(int i=sales.length()-1;i>=0;i--){JSONObject s=sales.optJSONObject(i);if(s==null)continue;b.append(s.optString("date")).append("\n").append(s.optString("product")).append(" ").append(s.optString("size")).append(" × ").append(fmt(s.optDouble("quantity"))).append("\n").append("المبيعات: ").append(money(s.optDouble("total"))).append(" • التكلفة: ").append(money(s.optDouble("cost_total"))).append("\n\n");}
    if(b.length()==0)b.append("لا توجد عمليات بيع مسجلة بعد");
    new AlertDialog.Builder(this).setTitle("سجل المبيعات").setMessage(b.toString()).setPositiveButton("إغلاق",null).show();
  }
  private int indexOf(JSONObject obj){for(int i=0;i<products.length();i++)if(products.optJSONObject(i)==obj)return i;return -1;}
  private void remove(int i){if(i<0)return;JSONArray n=new JSONArray();for(int j=0;j<products.length();j++)if(j!=i)n.put(products.opt(j));products=n;persist();refresh();}
  private String fmt(double n){if(!Double.isFinite(n))n=0;return String.format(Locale.US,"%s",n==(long)n?String.valueOf((long)n):String.format(Locale.US,"%.2f",n));}
  private String money(double n){return fmt(n)+" ج.م";}
  private void productDialog(int index){
    JSONObject old=index>=0?products.optJSONObject(index):null;
    LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(18),dp(8),dp(18),dp(4));form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    EditText name=field("اسم المنتج",old==null?"":old.optString("name"));
    EditText category=field("التصنيف",old==null?"":old.optString("category"));
    EditText size=field("حجم المنتج (مثال: 10ml أو 30ml أو 50ml أو Full Size)",old==null?"10ml":old.optString("size","10ml"));
    EditText stock=field("الكمية المتاحة",old==null?"0":fmt(old.optDouble("stock")));
    EditText cost=field("تكلفة الوحدة بالجنيه",old==null?"0":fmt(old.optDouble("cost")));
    EditText price=field("سعر البيع بالجنيه",old==null?"0":fmt(old.optDouble("price")));
    form.addView(name);form.addView(category);form.addView(label("حجم المنتج"));form.addView(size);form.addView(stock);form.addView(cost);form.addView(price);
    new AlertDialog.Builder(this).setTitle(index<0?"إضافة منتج":"تعديل المنتج").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{
      String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"اكتب اسم المنتج",Toast.LENGTH_SHORT).show();return;}
      try{double q=number(stock),c=number(cost),pr=number(price);if(q<0||c<0||pr<0)throw new Exception();
        String productSize=size.getText().toString().trim();if(productSize.isEmpty())throw new Exception();
        JSONObject p=old==null?new JSONObject():old;p.put("name",n);p.put("category",category.getText().toString().trim());p.put("size",productSize);p.put("stock",q);p.put("cost",c);p.put("price",pr);
        if(index<0)products.put(p);persist();refresh();
      }catch(Exception e){Toast.makeText(this,"راجع الكمية والأسعار المدخلة",Toast.LENGTH_LONG).show();}
    }).show();
  }
  private EditText field(String hint,String value){EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setHint(hint);e.setTextSize(15);e.setPadding(dp(8),dp(7),dp(8),dp(7));return e;}
  private TextView label(String s){TextView t=text(s,14,true);t.setPadding(0,dp(6),0,dp(3));return t;}
  private double number(EditText e)throws Exception{String s=e.getText().toString().trim();return s.isEmpty()?0:Double.parseDouble(s);}
}