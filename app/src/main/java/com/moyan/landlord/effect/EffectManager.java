package com.moyan.landlord.effect;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import com.moyan.landlord.engine.SettingsManager;

public class EffectManager {
    private final Context ctx;
    private final SettingsManager sm;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 画质档位 0-6
    public static final int QUALITY_OFF = 0;
    public static final int QUALITY_LOW = 1;
    public static final int QUALITY_MED_LOW = 2;
    public static final int QUALITY_MED = 3;
    public static final int QUALITY_MED_HIGH = 4;
    public static final int QUALITY_HIGH = 5;
    public static final int QUALITY_ULTRA = 6;

    public EffectManager(Context ctx, SettingsManager sm) {
        this.ctx = ctx;
        this.sm = sm;
    }

    /** 根据画质档位决定是否播放特效 */
    public boolean shouldPlayEffect() {
        return sm.getQuality() >= QUALITY_MED;
    }

    public boolean shouldPlayParticle() {
        return sm.getQuality() >= QUALITY_HIGH;
    }

    public boolean shouldPlayShake() {
        return sm.getQuality() >= QUALITY_MED_LOW;
    }

    /** 顺子特效：流光扫过 */
    public void playStraightEffect(View target) {
        if (!shouldPlayEffect()) return;
        if (target == null) return;
        TranslateAnimation slide = new TranslateAnimation(
            -target.getWidth(), target.getWidth(), 0, 0);
        slide.setDuration(500);
        slide.setInterpolator(new AccelerateDecelerateInterpolator());
        target.startAnimation(slide);
    }

    /** 连对特效：链条闪烁 */
    public void playPairEffect(View target) {
        if (!shouldPlayEffect()) return;
        if (target == null) return;
        AlphaAnimation fade = new AlphaAnimation(1f, 0.3f);
        fade.setDuration(150);
        fade.setRepeatCount(3);
        fade.setRepeatMode(Animation.REVERSE);
        target.startAnimation(fade);
    }

    /** 飞机特效：粒子上升 */
    public void playPlaneEffect(View target) {
        if (!shouldPlayParticle()) return;
        if (target == null) return;
        // 简化：用位移动画模拟
        ObjectAnimator up = ObjectAnimator.ofFloat(target, "translationY", 0f, -30f, 0f);
        up.setDuration(600);
        up.start();
    }

    /** 炸弹特效：屏幕抖动 + 闪烁 */
    public void playBombEffect(View rootView) {
        if (!shouldPlayShake()) return;
        if (rootView == null) return;
        // 抖动
        rootView.animate()
            .translationX(10f).setDuration(50)
            .withEndAction(() -> rootView.animate().translationX(-10f).setDuration(50)
                .withEndAction(() -> rootView.animate().translationX(0f).setDuration(50).start())
                .start())
            .start();
        // 闪白
        if (rootView instanceof android.view.ViewGroup) {
            View flash = new View(ctx);
            flash.setBackgroundColor(Color.WHITE);
            flash.setAlpha(0.6f);
            ((android.view.ViewGroup) rootView).addView(flash);
            handler.postDelayed(() -> {
                if (flash.getParent() != null) {
                    ((android.view.ViewGroup) rootView).removeView(flash);
                }
            }, 150);
        }
    }

    /** 王炸特效：全屏闪电抖动 */
    public void playKingBombEffect(View rootView) {
        if (rootView == null) return;
        // 强制播放（王炸不受画质限制）
        rootView.animate()
            .translationX(20f).setDuration(40)
            .withEndAction(() -> rootView.animate().translationX(-20f).setDuration(40)
                .withEndAction(() -> rootView.animate().translationX(15f).setDuration(40)
                    .withEndAction(() -> rootView.animate().translationX(0f).setDuration(40).start())
                    .start())
                .start())
            .start();
    }

    /** 出牌动画 */
    public void playCardPlayAnimation(View card, int delayMs) {
        if (card == null) return;
        card.setAlpha(0f);
        card.setScaleX(0.5f);
        card.setScaleY(0.5f);
        handler.postDelayed(() -> {
            card.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }, delayMs);
    }

    /** 胜利特效 */
    public void playWinEffect(TextView textView) {
        if (textView == null) return;
        ObjectAnimator scale = ObjectAnimator.ofFloat(textView, "scaleX", 1f, 1.3f, 1f);
        scale.setDuration(800);
        scale.setRepeatCount(2);
        scale.start();
        ObjectAnimator.ofFloat(textView, "scaleY", 1f, 1.3f, 1f).setDuration(800).start();
    }

    /** 清理 */
    public void cleanup() {
        handler.removeCallbacksAndMessages(null);
    }
}
