package com.moyan.landlord.engine;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public class RefreshRateManager {
    private static final String TAG = "RefreshRate";

    /** 设置目标帧率 */
    public static void setTargetFps(Activity activity, int fps) {
        if (activity == null) return;
        try {
            Window window = activity.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 优先使用 preferredRefreshRate (API 23+)
                Display display = activity.getWindowManager().getDefaultDisplay();
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);
                params.preferredRefreshRate = fps;
                window.setAttributes(params);
                Log.d(TAG, "Set preferredRefreshRate=" + fps);
            }

            // 也设置屏幕亮度模式等保活
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            Log.w(TAG, "setTargetFps failed: " + e.getMessage());
        }
    }

    /** 获取设备支持的最高刷新率 */
    public static int getMaxRefreshRate(Activity activity) {
        if (activity == null) return 60;
        try {
            Display display = activity.getWindowManager().getDefaultDisplay();
            float[] rates = display.getSupportedRefreshRates();
            float max = 60f;
            for (float r : rates) if (r > max) max = r;
            return (int) max;
        } catch (Exception e) {
            return 60;
        }
    }

    /** 获取最合适的帧率选项 */
    public static int getBestFpsOption(Activity activity) {
        int max = getMaxRefreshRate(activity);
        if (max >= 120) return 120;
        if (max >= 90) return 90;
        return 60;
    }

    /** 进入游戏时调用：全屏+高刷 */
    public static void enterGameMode(Activity activity, int targetFps) {
        if (activity == null) return;
        // 全屏
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 设置帧率
        setTargetFps(activity, targetFps);
    }

    /** 退出游戏时调用：恢复 */
    public static void exitGameMode(Activity activity) {
        if (activity == null) return;
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
