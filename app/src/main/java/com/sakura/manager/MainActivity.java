package com.sakura.manager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int cream = Color.rgb(250, 246, 239);
    private final int pink = Color.rgb(232, 190, 197);
    private final int ink = Color.rgb(62, 52, 53);

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(cream);
        getWindow().setNavigationBarColor(cream);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(cream);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(28));
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(page);

        TextView brand = label("SAKURA", 14, true);
        brand.setTextColor(Color.rgb(160, 112, 122));
        brand.setLetterSpacing(.2f);
        page.addView(brand);
        page.addView(label("إدارة Sakura Cosmetics", 25, true));
        TextView sub = label("لوحة التحكم • الإصدار 3.0", 14, false);
        sub.setTextColor(Color.rgb(130, 116, 113));
        page.addView(sub);

        LinearLayout notice = new LinearLayout(this);
        notice.setOrientation(LinearLayout.VERTICAL);
        notice.setPadding(dp(16), dp(14), dp(16), dp(14));
        notice.setBackground(panel(Color.WHITE, 18));
        LinearLayout.LayoutParams np = match();
        np.topMargin = dp(22);
        page.addView(notice, np);
        notice.addView(label("نسخة تأسيسية", 17, true));
        TextView detail = label("هذه واجهة أولية فقط. لم يتم ربط قاعدة البيانات أو تفعيل المبيعات والمخزون والطلبات بعد.", 14, false);
        detail.setPadding(0, dp(8), 0, 0);
        detail.setTextColor(Color.rgb(112, 99, 97));
        notice.addView(detail);

        section(page, "إدارة المتجر", new String[]{"المنتجات والأسعار", "المخزون والتكلفة", "الطلبات والمبيعات", "المصروفات والأرباح", "التقارير والطباعة"});
        Button open = new Button(this);
        open.setText("فتح متجر Sakura");
        open.setAllCaps(false);
        open.setTextColor(ink);
        open.setBackground(panel(pink, 24));
        LinearLayout.LayoutParams bp = match();
        bp.topMargin = dp(18);
        page.addView(open, bp);
        open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://sakurastore-xi.vercel.app"))));

        TextView footer = label("Sakura Manager V3", 12, false);
        footer.setGravity(Gravity.CENTER);
        footer.setTextColor(Color.GRAY);
        footer.setPadding(0, dp(26), 0, 0);
        page.addView(footer);
        setContentView(scroll);
    }

    private void section(LinearLayout page, String heading, String[] items) {
        TextView h = label(heading, 18, true);
        h.setPadding(0, dp(22), 0, dp(10));
        page.addView(h);
        for (String value : items) {
            TextView item = label(value + "   ›", 16, true);
            item.setPadding(dp(16), dp(17), dp(16), dp(17));
            item.setBackground(panel(Color.WHITE, 16));
            LinearLayout.LayoutParams p = match();
            p.bottomMargin = dp(9);
            page.addView(item, p);
        }
    }

    private TextView label(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(ink);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private android.graphics.drawable.GradientDrawable panel(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(-1, -2);
    }
    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + .5f);
    }
}
