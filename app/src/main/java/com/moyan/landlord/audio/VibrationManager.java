package com.moyan.landlord.audio;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import com.moyan.landlord.MoyanApp;

public class VibrationManager {
    private static final String TAG = "VibrationManager";
    private final Context ctx;
    private boolean enabled = true;

    public VibrationManager(Context ctx) {
        this.ctx = ctx;
        this.enabled = MoyanApp.getPrefs(ctx).getBoolean(MoyanApp.KEY_VIBRATE, true);
    }

    public void setEnabled(boolean on) {
        this.enabled = on;
        MoyanApp.getPrefs(ctx).edit().putBoolean(MoyanApp.KEY_VIBRATE, on).apply();
    }

    public boolean isEnabled() { return enabled; }

    /** 出牌振动（轻） */
    public void vibratePlay() {
        if (!enabled) return;
        vibrate(20, 50);
    }

    /** 顺子振动（中） */
    public void vibrateStraight() {
        if (!enabled) return;
        vibrate(40, 100);
    }

    /** 飞机振动（较强） */
    public void vibratePlane() {
        if (!enabled) return;
        long[] pattern = {0, 50, 30, 50, 30, 50};
        vibratePattern(pattern);
    }

    /** 炸弹振动（强） */
    public void vibrateBomb() {
        if (!enabled) return;
        long[] pattern = {0, 80, 20, 80, 20, 80, 20, 80};
        vibratePattern(pattern);
    }

    /** 王炸振动（超强） */
    public void vibrateKingBomb() {
        if (!enabled) return;
        long[] pattern = {0, 120, 30, 100, 30, 100, 30, 100, 30, 120};
        vibratePattern(pattern);
    }

    /** 胜利振动 */
    public void vibrateWin() {
        if (!enabled) return;
        long[] pattern = {0, 200, 100, 200, 100, 200};
        vibratePattern(pattern);
    }

    /** 失败振动 */
    public void vibrateLose() {
        if (!enabled) return;
        vibrate(300, 0);
    }

    /** 简单振动 */
    private void vibrate(long ms, int amplitude) {
        try {
            Vibrator v = MoyanApp.getVibrator(ctx);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, amplitude));
            } else {
                v.vibrate(ms);
            }
        } catch (Exception e) {
            Log.w(TAG, "vibrate error: " + e.getMessage());
        }
    }

    /** 模式振动 */
    private void vibratePattern(long[] pattern) {
        try {
            Vibrator v = MoyanApp.getVibrator(ctx);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        } catch (Exception e) {
            Log.w(TAG, "vibratePattern error: " + e.getMessage());
        }
    }
}
