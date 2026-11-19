package com.sakura.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class MainActivity extends Activity {
  private final int cream=Color.rgb(250,246,239), pink=Color.rgb(232,190,197), ink=Color.rgb(62,52,53), muted=Color.rgb(130,116,113);
  private JSONArray products;
  private LinearLayout page, list;
  private TextView summary;
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(cream);getWindow().setNavigationBarColor(cream);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);load();render();}
  private void load(){try{products=new JSONArray(getPreferences(0).getString("products","[]"));}catch(Exception e){products=new JSONArray();}}
  private void persist(){getPreferences(0).edit().putString("products",products.toString()).apply();}
  private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
  private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ink);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,-2);}
  private void render(){
    ScrollView sc=new ScrollView(this);sc.setBackgroundColor(cream);page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(18),dp(24),dp(18),dp(28));page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sc.addView(page);
    TextView brand=text("SAKURA",14,true);brand.setTextColor(Color.rgb(160,112,122));page.addView(brand);
    page.addView(text("إدارة Sakura Cosmetics",25,true));
    TextView note=text("إدارة المنتجات والمخزون • إصدار 3.1",14,false);note.setTextColor(muted);page.addView(note);
    summary=text("",15,true);summary.setPadding(dp(16),dp(16),dp(16),dp(16));summary.setBackground(bg(Color.WHITE,18));LinearLayout.LayoutParams sp=lp();sp.topMargin=dp(20);page.addView(summary,sp);
    Button add=button("＋ إضافة منتج جديد");LinearLayout.LayoutParams ap=lp();ap.topMargin=dp(14);page.addView(add,ap);add.setOnClickListener(v->productDialog(-1));
    TextView h=text("المنتجات",19,true);h.setPadding(0,dp(24),0,dp(10));page.addView(h);
    list=new LinearLayout(this);list.setOrientation(1);page.addView(list);refresh();
    TextView foot=text("البيانات محفوظة على هذا الجهاز مؤقتًا. المزامنة السحابية لم تُفعّل بعد.",12,false);foot.setTextColor(muted);foot.setPadding(0,dp(24),0,0);page.addView(foot);
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