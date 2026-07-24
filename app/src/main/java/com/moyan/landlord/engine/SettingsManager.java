package com.moyan.landlord.engine;

import android.content.Context;
import android.content.SharedPreferences;
import com.moyan.landlord.MoyanApp;

public class SettingsManager {
    private final SharedPreferences sp;
    private final Context ctx;

    // 帧率选项
    public static final int[] FPS_OPTIONS = {30, 60, 90, 120};
    public static final int FPS_DEFAULT = 60;

    // 画质档位 0-6
    public static final int QUALITY_MIN = 0;
    public static final int QUALITY_MAX = 6;
    public static final int QUALITY_DEFAULT = 3;

    // 运气值 0-100
    public static final int LUCK_DEFAULT = 50;

    public SettingsManager(Context ctx) {
        this.ctx = ctx;
        this.sp = MoyanApp.getPrefs(ctx);
    }

    // ===== 帧率 =====
    public int getFps() {
        return sp.getInt(MoyanApp.KEY_FPS, FPS_DEFAULT);
    }
    public void setFps(int fps) {
        sp.edit().putInt(MoyanApp.KEY_FPS, fps).apply();
    }

    // ===== 画质 =====
    public int getQuality() {
        return sp.getInt(MoyanApp.KEY_QUALITY, QUALITY_DEFAULT);
    }
    public void setQuality(int q) {
        q = Math.max(QUALITY_MIN, Math.min(QUALITY_MAX, q));
        sp.edit().putInt(MoyanApp.KEY_QUALITY, q).apply();
    }

    /** 画质档位名称 */
    public String getQualityName(int q) {
        switch (q) {
            case 0: return "省电";
            case 1: return "低";
            case 2: return "中低";
            case 3: return "中";
            case 4: return "中高";
            case 5: return "高";
            case 6: return "极致";
            default: return "中";
        }
    }

    // ===== 音量 =====
    public float getVolume() {
        return sp.getFloat(MoyanApp.KEY_VOLUME, 0.8f);
    }
    public void setVolume(float v) {
        v = Math.max(0f, Math.min(1f, v));
        sp.edit().putFloat(MoyanApp.KEY_VOLUME, v).apply();
    }

    // ===== 振动 =====
    public boolean isVibrateEnabled() {
        return sp.getBoolean(MoyanApp.KEY_VIBRATE, true);
    }
    public void setVibrateEnabled(boolean on) {
        sp.edit().putBoolean(MoyanApp.KEY_VIBRATE, on).apply();
    }

    // ===== 运气值（开发者选项） =====
    public int getLuckValue() {
        return sp.getInt(MoyanApp.KEY_LUCK, LUCK_DEFAULT);
    }
    public void setLuckValue(int luck) {
        luck = Math.max(0, Math.min(100, luck));
        sp.edit().putInt(MoyanApp.KEY_LUCK, luck).apply();
    }

    /** 根据运气值调整发牌（高运气=更多大牌） */
    public void applyLuckToDeck(java.util.List<Card> deck) {
        int luck = getLuckValue();
        if (luck <= 50) return; // 50以下不改变
        // 简化：运气>50时，把大牌往前移
        int boost = (luck - 50) / 5; // 0-10
        if (boost <= 0) return;
        // 把rank>=14的牌移到前面
        java.util.List<Card> bigCards = new java.util.ArrayList<>();
        java.util.List<Card> smallCards = new java.util.ArrayList<>();
        for (Card c : deck) {
            if (c.rank >= 14 - boost) bigCards.add(c);
            else smallCards.add(c);
        }
        deck.clear();
        deck.addAll(bigCards);
        deck.addAll(smallCards);
    }
}
