package com.moyan.landlord;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

public class MoyanApp extends Application {

    private static final String TAG = "MoyanApp";
    private static final String PREFS = "moyan_settings";

    // 设置项 key
    public static final String KEY_FPS = "fps";
    public static final String KEY_QUALITY = "quality";
    public static final String KEY_VOLUME = "volume";
    public static final String KEY_VIBRATE = "vibrate";
    public static final String KEY_LUCK = "luck_value";
    public static final String KEY_COINS = "coins";
    public static final String KEY_RANK = "rank";

    // 默认值
    public static final int DEFAULT_FPS = 60;
    public static final int DEFAULT_QUALITY = 3;
    public static final float DEFAULT_VOLUME = 0.8f;
    public static final boolean DEFAULT_VIBRATE = true;
    public static final int DEFAULT_LUCK = 50;
    public static final int DEFAULT_COINS = 1000;
    public static final int DEFAULT_RANK = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        initDefaults();
        Log.d(TAG, "MoyanApp initialized");
    }

    private void initDefaults() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        boolean changed = false;
        if (!sp.contains(KEY_FPS)) { editor.putInt(KEY_FPS, DEFAULT_FPS); changed = true; }
        if (!sp.contains(KEY_QUALITY)) { editor.putInt(KEY_QUALITY, DEFAULT_QUALITY); changed = true; }
        if (!sp.contains(KEY_VOLUME)) { editor.putFloat(KEY_VOLUME, DEFAULT_VOLUME); changed = true; }
        if (!sp.contains(KEY_VIBRATE)) { editor.putBoolean(KEY_VIBRATE, DEFAULT_VIBRATE); changed = true; }
        if (!sp.contains(KEY_LUCK)) { editor.putInt(KEY_LUCK, DEFAULT_LUCK); changed = true; }
        if (!sp.contains(KEY_COINS)) { editor.putInt(KEY_COINS, DEFAULT_COINS); changed = true; }
        if (!sp.contains(KEY_RANK)) { editor.putInt(KEY_RANK, DEFAULT_RANK); changed = true; }
        if (changed) editor.apply();
    }

    public static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    @SuppressWarnings("deprecation")
    public static Vibrator getVibrator(Context ctx) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) return vm.getDefaultVibrator();
        }
        return (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
