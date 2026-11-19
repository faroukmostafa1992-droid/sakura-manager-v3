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
  private final int cream=Color.rgb(250,246,239), pink=Color.rgb(232,190,197), ink=Color.rgb(62,52,53), muted=Color.rgb(130,116,113);
  private JSONArray products;
  private LinearLayout page, list;
  private TextView summary;
  private android.content.SharedPreferences prefs;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    prefs=getPreferences(0);
    getWindow().setStatusBarColor(cream);getWindow().setNavigationBarColor(cream);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    load();
    if(prefs.getString("access_token",null)==null) showLogin(); else render();
  }
  private void load(){try{products=new JSONArray(prefs.getString("products","[]"));}catch(Exception e){products=new JSONArray();}}
  private void persist(){prefs.edit().putString("products",products.toString()).apply();}
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
    Button add=button("＋ إضافة منتج جديد");LinearLayout.LayoutParams ap=lp();ap.topMargin=dp(14);page.addView(add,ap);add.setOnClickListener(v->productDialog(-1));
    TextView h=text("المنتجات",19,true);h.setPadding(0,dp(24),0,dp(10));page.addView(h);
    list=new LinearLayout(this);list.setOrientation(1);page.addView(list);refresh();
    TextView foot=text("تسجيل الدخول متصل بـ Supabase. بيانات المنتجات ما زالت محفوظة محليًا؛ المزامنة السحابية لم تُفعّل بعد.",12,false);foot.setTextColor(muted);foot.setPadding(0,dp(24),0,0);page.addView(foot);
    setContentView(sc);
  }
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(ink);b.setBackground(bg(pink,20));return b;}
  private void refresh(){
    list.removeAllViews();double value=0,retail=0;int count=products.length();
    for(int i=0;i<count;i++){JSONObject p=products.optJSONObject(i);if(p==null)continue;double q=p.optDouble("stock",0),c=p.optDouble("cost",0),price=p.optDouble("price",0);value+=q*c;retail+=q*price;
      LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(15),dp(13),dp(15),dp(13));card.setBackground(bg(Color.WHITE,16));LinearLayout.LayoutParams cp=lp();cp.bottomMargin=dp(10);list.addView(card,cp);
      TextView name=text(p.optString("name","منتج"),17,true);card.addView(name);
      TextView details=text(p.optString("size","Full Size")+"  •  المخزون: "+fmt(q)+"  •  سعر البيع: "+money(price),13,false);details.setTextColor(muted);details.setPadding(0,dp(5),0,dp(2));card.addView(details);
      TextView cost=text("التكلفة للوحدة: "+money(c)+"  •  قيمة المخزون: "+money(q*c),13,false);cost.setTextColor(muted);card.addView(cost);
      LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);Button edit=new Button(this);edit.setText("تعديل");edit.setAllCaps(false);edit.setOnClickListener(v->productDialog(indexOf(p)));Button del=new Button(this);del.setText("حذف");del.setAllCaps(false);del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("حذف المنتج من القائمة؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(d,w)->{remove(indexOf(p));}).show());actions.addView(edit);actions.addView(del);card.addView(actions);
    }
    summary.setText("عدد المنتجات: "+count+"\nقيمة المخزون بالتكلفة: "+money(value)+"\nقيمة البيع المتوقعة: "+money(retail));
    if(count==0){TextView empty=text("لا توجد منتجات بعد. اضغط إضافة منتج جديد للبدء.",15,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(12),dp(24),dp(12),dp(24));list.addView(empty);}
  }
  private int indexOf(JSONObject obj){for(int i=0;i<products.length();i++)if(products.optJSONObject(i)==obj)return i;return -1;}
  private void remove(int i){if(i<0)return;JSONArray n=new JSONArray();for(int j=0;j<products.length();j++)if(j!=i)n.put(products.opt(j));products=n;persist();refresh();}
  private String fmt(double n){return String.format(Locale.US,"%s",n==(long)n?String.valueOf((long)n):String.format(Locale.US,"%.2f",n));}
  private String money(double n){return fmt(n)+" ج.م";}
  private void productDialog(int index){
    JSONObject old=index>=0?products.optJSONObject(index):null;
    LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(18),dp(8),dp(18),dp(4));form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    EditText name=field("اسم المنتج",old==null?"":old.optString("name"));
    EditText category=field("التصنيف",old==null?"":old.optString("category"));
    Spinner size=new Spinner(this);String[] sizes={"10ml","Full Size"};size.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,sizes));size.setSelection(old!=null&&old.optString("size").equals("10ml")?0:1);
    EditText stock=field("الكمية المتاحة",old==null?"0":fmt(old.optDouble("stock")));
    EditText cost=field("تكلفة الوحدة بالجنيه",old==null?"0":fmt(old.optDouble("cost")));
    EditText price=field("سعر البيع بالجنيه",old==null?"0":fmt(old.optDouble("price")));
    form.addView(name);form.addView(category);form.addView(label("حجم المنتج"));form.addView(size);form.addView(stock);form.addView(cost);form.addView(price);
    new AlertDialog.Builder(this).setTitle(index<0?"إضافة منتج":"تعديل المنتج").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{
      String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"اكتب اسم المنتج",Toast.LENGTH_SHORT).show();return;}
      try{double q=number(stock),c=number(cost),pr=number(price);if(q<0||c<0||pr<0)throw new Exception();
        JSONObject p=old==null?new JSONObject():old;p.put("name",n);p.put("category",category.getText().toString().trim());p.put("size",size.getSelectedItem().toString());p.put("stock",q);p.put("cost",c);p.put("price",pr);
        if(index<0)products.put(p);persist();refresh();
      }catch(Exception e){Toast.makeText(this,"راجع الكمية والأسعار المدخلة",Toast.LENGTH_LONG).show();}
    }).show();
  }
  private EditText field(String hint,String value){EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setHint(hint);e.setTextSize(15);e.setPadding(dp(8),dp(7),dp(8),dp(7));return e;}
  private TextView label(String s){TextView t=text(s,14,true);t.setPadding(0,dp(6),0,dp(3));return t;}
  private double number(EditText e)throws Exception{String s=e.getText().toString().trim();return s.isEmpty()?0:Double.parseDouble(s);}
}