package com.moyan.landlord.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import com.moyan.landlord.MoyanApp;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AudioManager {
    private static final String TAG = "AudioManager";

    // 音效类型
    public static final int SFX_PLAY_CARD = 0;
    public static final int SFX_PASS = 1;
    public static final int SFX_WIN = 2;
    public static final int SFX_LOSE = 3;
    public static final int SFX_BOMB = 4;
    public static final int SFX_KING_BOMB = 5;
    public static final int SFX_STRAIGHT = 6;
    public static final int SFX_PLANE = 7;
    public static final int SFX_PAIR = 8;
    public static final int SFX_TRIPLE = 9;
    public static final int SFX_SINGLE = 10;
    public static final int SFX_ALERT = 11;
    public static final int SFX_BUTTON = 12;
    public static final int SFX_START = 13;

    private final Context ctx;
    private SoundPool soundPool;
    private final Map<Integer, Integer> sfxMap = new HashMap<>();
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private float volume = 0.8f;

    public AudioManager(Context ctx) {
        this.ctx = ctx;
        initSoundPool();
        initTts();
        loadSettings();
    }

    private void initSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder b = new SoundPool.Builder();
            b.setMaxStreams(8);
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            int sr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            b.setAudioAttributes(new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_GAME)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            soundPool = b.build();
        } else {
            soundPool = new SoundPool(8, android.media.AudioManager.STREAM_MUSIC, 0);
        }
        // 用程序化音效（不依赖外部音频文件）
        // 实际项目中这里应 load(R.raw.xxx)
    }

    private void initTts() {
        tts = new TextToSpeech(ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(Locale.CHINESE);
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS Chinese not supported, fallback to default");
                    tts.setLanguage(Locale.getDefault());
                }
                ttsReady = true;
            }
        });
    }

    private void loadSettings() {
        volume = MoyanApp.getPrefs(ctx).getFloat(MoyanApp.KEY_VOLUME, 0.8f);
    }

    public void setVolume(float v) {
        this.volume = Math.max(0f, Math.min(1f, v));
        MoyanApp.getPrefs(ctx).edit().putFloat(MoyanApp.KEY_VOLUME, volume).apply();
    }

    public float getVolume() { return volume; }

    /** 播放音效（程序化生成简单提示音） */
    public void playSfx(int sfxType) {
        if (volume <= 0.01f) return;
        try {
            // 简化实现：用TTS播报简短音
            // 实际项目应加载res/raw下的音频文件
            String text = getSfxText(sfxType);
            if (ttsReady && text != null) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sfx_" + sfxType);
            }
        } catch (Exception e) {
            Log.w(TAG, "playSfx error: " + e.getMessage());
        }
    }

    private String getSfxText(int type) {
        switch (type) {
            case SFX_BOMB: return "炸弹";
            case SFX_KING_BOMB: return "王炸";
            case SFX_STRAIGHT: return "顺子";
            case SFX_PLANE: return "飞机";
            case SFX_WIN: return "胜利";
            case SFX_LOSE: return "失败";
            case SFX_PASS: return "过";
            default: return null;
        }
    }

    /** 语音播报出牌 */
    public void announcePlay(String playerName, String cardText) {
        if (!ttsReady || volume <= 0.01f) return;
        try {
            String text = playerName + "出" + cardText;
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "announce");
        } catch (Exception e) {
            Log.w(TAG, "announce error: " + e.getMessage());
        }
    }

    /** 播报牌型 */
    public void announceCardType(String typeName) {
        if (!ttsReady || volume <= 0.01f) return;
        tts.speak(typeName, TextToSpeech.QUEUE_FLUSH, null, "type");
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (soundPool != null) soundPool.release();
    }
}
