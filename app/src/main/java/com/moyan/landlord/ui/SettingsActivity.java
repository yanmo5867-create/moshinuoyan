package com.moyan.landlord.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.moyan.landlord.MoyanApp;
import com.moyan.landlord.audio.AudioManager;
import com.moyan.landlord.audio.VibrationManager;
import com.moyan.landlord.engine.SettingsManager;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager sm;
    private AudioManager audioMgr;
    private VibrationManager vibMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sm = new SettingsManager(this);
        audioMgr = new AudioManager(this);
        vibMgr = new VibrationManager(this);

        setupFpsSection();
        setupQualitySection();
        setupVolumeSection();
        setupVibrateSection();
        setupLuckSection();
        setupDeveloperSection();
    }

    private void setupFpsSection() {
        TextView tvFps = findViewById(R.id.tv_fps_value);
        tvFps.setText(sm.getFps() + "fps");

        SeekBar sb = findViewById(R.id.seek_fps);
        sb.setMax(3);
        int[] options = SettingsManager.FPS_OPTIONS;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == sm.getFps()) { sb.setProgress(i); break; }
        }
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int fps = SettingsManager.FPS_OPTIONS[Math.min(p, 3)];
                tvFps.setText(fps + "fps");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                int fps = SettingsManager.FPS_OPTIONS[Math.min(s.getProgress(), 3)];
                sm.setFps(fps);
                audioMgr.playSfx(AudioManager.SFX_BUTTON);
            }
        });
    }

    private void setupQualitySection() {
        TextView tvQ = findViewById(R.id.tv_quality_value);
        tvQ.setText(sm.getQualityName(sm.getQuality()));

        SeekBar sb = findViewById(R.id.seek_quality);
        sb.setMax(SettingsManager.QUALITY_MAX);
        sb.setProgress(sm.getQuality());
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                tvQ.setText(sm.getQualityName(p));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                sm.setQuality(s.getProgress());
                audioMgr.playSfx(AudioManager.SFX_BUTTON);
            }
        });
    }

    private void setupVolumeSection() {
        SeekBar sb = findViewById(R.id.seek_volume);
        sb.setMax(100);
        sb.setProgress((int) (sm.getVolume() * 100));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                float v = p / 100f;
                sm.setVolume(v);
                if (fromUser && p % 10 == 0) audioMgr.playSfx(AudioManager.SFX_BUTTON);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void setupVibrateSection() {
        Switch sw = findViewById(R.id.switch_vibrate);
        sw.setChecked(sm.isVibrateEnabled());
        sw.setOnCheckedChangeListener((v, checked) -> {
            sm.setVibrateEnabled(checked);
            vibMgr.setEnabled(checked);
        });
    }

    private void setupLuckSection() {
        SeekBar sb = findViewById(R.id.seek_luck);
        sb.setMax(100);
        sb.setProgress(sm.getLuckValue());
        TextView tvLuck = findViewById(R.id.tv_luck_value);
        tvLuck.setText(sm.getLuckValue() + "%");

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                tvLuck.setText(p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                sm.setLuckValue(s.getProgress());
            }
        });
    }

    private void setupDeveloperSection() {
        findViewById(R.id.btn_dev_reset).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("开发者选项")
                .setMessage("确认重置所有数据（斗币/段位/设置）？")
                .setPositiveButton("重置", (d, w) -> {
                    getSharedPreferences(MoyanApp.PREFS, MODE_PRIVATE).edit().clear().apply();
                    sm.setVolume(0.8f);
                    sm.setQuality(3);
                    sm.setFps(60);
                    sm.setLuckValue(50);
                    sm.setVibrateEnabled(true);
                    // 重置斗币
                    getSharedPreferences(MoyanApp.PREFS, MODE_PRIVATE)
                        .edit().putInt(MoyanApp.KEY_COINS, 1000).apply();
                    android.widget.Toast.makeText(this, "已重置", android.widget.Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioMgr != null) audioMgr.shutdown();
    }
}
