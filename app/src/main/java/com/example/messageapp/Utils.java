package com.example.messageapp;

import android.graphics.drawable.GradientDrawable;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Các hàm tiện ích dùng chung. */
public final class Utils {

    private static final int[] AVATAR_COLORS = {
            0xFF0068FF, 0xFF00A86B, 0xFFFF7A00, 0xFF8E44AD,
            0xFFE91E63, 0xFF009688, 0xFF3F51B5, 0xFFF4511E
    };

    private Utils() {}

    /** Chữ cái đầu của tên để hiện trên avatar. */
    public static String initial(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        // Lấy chữ đầu của từ cuối (tên) như kiểu Zalo/Messenger
        return parts[parts.length - 1].substring(0, 1).toUpperCase(Locale.getDefault());
    }

    /** Tô màu avatar cố định theo tên (cùng tên -> cùng màu). */
    public static void tintAvatar(View view, String name) {
        int color = AVATAR_COLORS[Math.abs((name == null ? "" : name).hashCode()) % AVATAR_COLORS.length];
        GradientDrawable bg = (GradientDrawable) view.getBackground().mutate();
        bg.setColor(color);
    }

    /** Hôm nay -> HH:mm, ngày khác -> dd/MM. */
    public static String shortTime(long millis) {
        if (millis <= 0) return "";
        Calendar now = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(millis);
        boolean sameDay = now.get(Calendar.YEAR) == t.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == t.get(Calendar.DAY_OF_YEAR);
        String pattern = sameDay ? "HH:mm" : "dd/MM";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }

    /** Giờ hiển thị dưới mỗi tin nhắn: HH:mm dd/MM/yyyy. */
    public static String fullTime(long millis) {
        return new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }
}
