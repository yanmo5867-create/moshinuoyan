package com.moyan.landlord.ui;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.moyan.landlord.MoyanApp;
import com.moyan.landlord.audio.AudioManager;
import com.moyan.landlord.audio.VibrationManager;
import com.moyan.landlord.engine.CoinRankManager;
import com.moyan.landlord.engine.SettingsManager;

public class MainActivity extends AppCompatActivity {

    private AudioManager audioMgr;
    private VibrationManager vibMgr;
    private CoinRankManager coinMgr;
    private SettingsManager settingsMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化全局管理器
        audioMgr = new AudioManager(this);
        vibMgr = new VibrationManager(this);
        coinMgr = new CoinRankManager(this);
        settingsMgr = new SettingsManager(this);

        // 标题动画
        TextView title = findViewById(R.id.tv_title);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.start();

        // 斗币显示
        updateCoinDisplay();

        // 开始游戏
        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> {
            audioMgr.playSfx(AudioManager.SFX_BUTTON);
            vibMgr.vibratePlay();
            startGame();
        });

        // 设置
        Button btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            audioMgr.playSfx(AudioManager.SFX_BUTTON);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // 模式选择
        Button btnMode = findViewById(R.id.btn_mode);
        btnMode.setOnClickListener(v -> showModeDialog());

        // 战绩
        Button btnStats = findViewById(R.id.btn_stats);
        btnStats.setOnClickListener(v -> showStatsDialog());
    }

    private void updateCoinDisplay() {
        TextView tvCoins = findViewById(R.id.tv_coins);
        if (tvCoins != null) {
            tvCoins.setText("💰 " + coinMgr.getCoins() + " | " + coinMgr.getRankName());
        }
    }

    private void startGame() {
        // 检查斗币
        if (coinMgr.getCoins() < coinMgr.getCurrentEntryFee() / 2) {
            Toast.makeText(this, "斗币不足，请先完成任务获取斗币", Toast.LENGTH_SHORT).show();
            return;
        }

        // 传入难度
        int difficulty = coinMgr.getCurrentAiDifficulty();
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("mode", 0); // 经典模式
        startActivity(intent);
    }

    private void showModeDialog() {
        String[] modes = {"经典场", "不洗牌场", "癞子场", "明牌场", "快速场", "五十K场", "双副牌场"};
        new AlertDialog.Builder(this)
            .setTitle("选择模式")
            .setItems(modes, (d, which) -> {
                int difficulty = coinMgr.getCurrentAiDifficulty();
                Intent intent = new Intent(this, GameActivity.class);
                intent.putExtra("difficulty", difficulty);
                intent.putExtra("mode", which);
                startActivity(intent);
            })
            .show();
    }

    private void showStatsDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append("段位：").append(coinMgr.getRankName()).append("\n");
        sb.append("斗币：").append(coinMgr.getCoins()).append("\n");
        sb.append("AI难度：").append(coinMgr.getCurrentAiDifficulty() + 1).append("/10\n");
        sb.append("画质：").append(settingsMgr.getQualityName(settingsMgr.getQuality())).append("\n");
        sb.append("帧率：").append(settingsMgr.getFps()).append("fps");

        new AlertDialog.Builder(this)
            .setTitle("我的信息")
            .setMessage(sb.toString())
            .setPositiveButton("确定", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCoinDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioMgr != null) audioMgr.shutdown();
    }
}
