package com.moyan.landlord.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.moyan.landlord.MoyanApp;
import com.moyan.landlord.audio.AudioManager;
import com.moyan.landlord.audio.VibrationManager;
import com.moyan.landlord.engine.AIEngine;
import com.moyan.landlord.engine.CardCounter;
import com.moyan.landlord.engine.CardEngine;
import com.moyan.landlord.engine.CoinRankManager;
import com.moyan.landlord.engine.GameEngine;
import com.moyan.landlord.engine.SettingsManager;
import com.moyan.landlord.effect.EffectManager;
import com.moyan.landlord.model.Card;
import com.moyan.landlord.model.CardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    private GameEngine engine;
    private AudioManager audioMgr;
    private VibrationManager vibMgr;
    private EffectManager effectMgr;
    private SettingsManager settingsMgr;
    private CoinRankManager coinMgr;

    private LinearLayout handContainer;
    private LinearLayout playAreaLeft, playAreaRight, playAreaBottom;
    private TextView tvInfo, tvCoins, tvMultiple, tvLandlord;
    private Button btnPlay, btnPass;

    private List<CardView> handCardViews = new ArrayList<>();
    private CardTouchHandler touchHandler;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int difficulty = 5;
    private int mode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        difficulty = getIntent().getIntExtra("difficulty", 5);
        mode = getIntent().getIntExtra("mode", 0);

        initManagers();
        initViews();
        startNewRound();
    }

    private void initManagers() {
        audioMgr = new AudioManager(this);
        vibMgr = new VibrationManager(this);
        settingsMgr = new SettingsManager(this);
        coinMgr = new CoinRankManager(this);

        // 设置帧率
        int fps = settingsMgr.getFps();
        // 简单设置：只设亮度/刷新提示
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        effectMgr = new EffectManager(this, settingsMgr);
    }

    private void initViews() {
        handContainer = findViewById(R.id.ll_hand_cards);
        playAreaLeft = findViewById(R.id.ll_play_left);
        playAreaRight = findViewById(R.id.ll_play_right);
        playAreaBottom = findViewById(R.id.ll_play_bottom);
        tvInfo = findViewById(R.id.tv_info);
        tvCoins = findViewById(R.id.tv_coins);
        tvMultiple = findViewById(R.id.tv_multiple);
        tvLandlord = findViewById(R.id.tv_landlord);
        btnPlay = findViewById(R.id.btn_play);
        btnPass = findViewById(R.id.btn_pass);

        btnPlay.setOnClickListener(v -> onPlayClicked());
        btnPass.setOnClickListener(v -> onPassClicked());

        // 返回按钮
        Button btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("退出对局")
                    .setMessage("确定要退出当前对局吗？")
                    .setPositiveButton("退出", (d, w) -> finish())
                    .setNegativeButton("继续", null)
                    .show();
            });
        }
    }

    private void startNewRound() {
        engine = new GameEngine();
        engine.startNewGame(difficulty);

        // 叫地主
        runBiddingPhase();

        updateInfo();
        renderHand();
    }

    private void runBiddingPhase() {
        int[] scores = engine.biddingPhase();
        // 简化：自动选最高分的人当地主
        StringBuilder sb = new StringBuilder("叫地主：\n");
        for (int i = 0; i < 3; i++) {
            sb.append("玩家").append(i + 1).append("：").append(scores[i]).append("分\n");
        }
        sb.append("地主是：玩家").append(engine.getLandlord() + 1);
        tvInfo.setText(sb.toString());

        // 显示底牌
        List<Card> bottom = engine.getBottomCards();
        StringBuilder bottomSb = new StringBuilder("底牌：");
        for (Card c : bottom) bottomSb.append(c.getDisplayName()).append(" ");
        Toast.makeText(this, bottomSb.toString(), Toast.LENGTH_LONG).show();

        audioMgr.playSfx(AudioManager.SFX_START);
    }

    private void renderHand() {
        handContainer.removeAllViews();
        handCardViews.clear();

        List<Card> myHand = engine.getHand(GameEngine.PLAYER_HUMAN);
        CardEngine.sortCards(myHand);

        for (Card c : myHand) {
            CardView cv = new CardView(this);
            cv.setCard(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(-8, 0, -8, 0); // 牌叠在一起
            cv.setLayoutParams(lp);
            handContainer.addView(cv);
            handCardViews.add(cv);
        }

        touchHandler = new CardTouchHandler(handContainer);
        touchHandler.setCardViews(handCardViews);
        touchHandler.setOnSelectionChangedListener(selected -> {
            // 可以显示已选牌提示
        });
    }

    private void onPlayClicked() {
        List<Card> selected = touchHandler.getSelectedCards();
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先选择要出的牌", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证合法性
        List<Card> myHand = engine.getHand(GameEngine.PLAYER_HUMAN);
        List<Card> toPlay = new ArrayList<>(selected);
        boolean ok = engine.playCards(GameEngine.PLAYER_HUMAN, toPlay);

        if (!ok) {
            Toast.makeText(this, "出牌不合法！", Toast.LENGTH_SHORT).show();
            return;
        }

        // 播放音效
        CardType type = CardEngine.recognizeType(toPlay);
        playCardSfx(type);
        vibMgr.vibratePlay();

        // 显示出的牌
        showPlayedCards(GameEngine.PLAYER_HUMAN, toPlay);

        // 清除选中
        touchHandler.clearSelection();
        renderHand();

        // 检查胜负
        int winner = engine.checkWinner();
        if (winner >= 0) {
            onGameEnd(winner);
            return;
        }

        updateInfo();
        // 轮到AI
        scheduleAiTurn();
    }

    private void onPassClicked() {
        List<Card> lastPlay = engine.getLastPlay();
        if (lastPlay.isEmpty()) {
            Toast.makeText(this, "你是先手，必须出牌", Toast.LENGTH_SHORT).show();
            return;
        }
        engine.pass(GameEngine.PLAYER_HUMAN);
        Toast.makeText(this, "过牌", Toast.LENGTH_SHORT).show();
        audioMgr.playSfx(AudioManager.SFX_PASS);
        vibMgr.vibratePlay();
        updateInfo();
        scheduleAiTurn();
    }

    private void scheduleAiTurn() {
        handler.postDelayed(() -> runAiTurn(), 1500);
    }

    private void runAiTurn() {
        int current = engine.getCurrentPlayer();
        if (current == GameEngine.PLAYER_HUMAN) {
            // 回到人类
            updateInfo();
            return;
        }

        AIEngine ai = new AIEngine(difficulty);
        ai.setCounter(engine.getCounter());
        List<Card> myHand = engine.getHand(current);
        List<Card> lastPlay = engine.getLastPlay();
        boolean isFirst = lastPlay.isEmpty();
        List<Card> play = ai.think(myHand, lastPlay, isFirst);

        if (play.isEmpty()) {
            Toast.makeText(this, "AI" + current + "过牌", Toast.LENGTH_SHORT).show();
            engine.pass(current);
            audioMgr.playSfx(AudioManager.SFX_PASS);
        } else {
            engine.playCards(current, play);
            CardType type = CardEngine.recognizeType(play);
            playCardSfx(type);
            showPlayedCards(current, play);
            Toast.makeText(this, "AI" + current + "出牌", Toast.LENGTH_SHORT).show();
        }

        // 检查胜负
        int winner = engine.checkWinner();
        if (winner >= 0) {
            onGameEnd(winner);
            return;
        }

        updateInfo();

        // 如果下家还是AI，继续
        if (engine.getCurrentPlayer() != GameEngine.PLAYER_HUMAN) {
            scheduleAiTurn();
        }
    }

    private void showPlayedCards(int player, List<Card> cards) {
        LinearLayout target = null;
        switch (player) {
            case GameEngine.PLAYER_HUMAN: target = playAreaBottom; break;
            case 1: target = playAreaLeft; break;
            case 2: target = playAreaRight; break;
        }
        if (target == null) return;

        target.removeAllViews();
        StringBuilder sb = new StringBuilder();
        for (Card c : cards) {
            TextView tv = new TextView(this);
            tv.setText(c.getDisplayName());
            tv.setTextSize(18);
            tv.setTextColor(c.isJoker() ? Color.YELLOW : Color.WHITE);
            target.addView(tv);
            sb.append(c.getDisplayName()).append(" ");
        }

        // 语音播报
        audioMgr.announcePlay("玩家" + (player + 1), sb.toString().trim());
    }

    private void playCardSfx(CardType type) {
        if (type == null) return;
        switch (type) {
            case BOMB:
            case KING_BOMB:
                audioMgr.playSfx(AudioManager.SFX_BOMB);
                vibMgr.vibrateBomb();
                if (type == CardType.KING_BOMB) {
                    effectMgr.playKingBombEffect(findViewById(R.id.root_layout));
                } else {
                    effectMgr.playBombEffect(findViewById(R.id.root_layout));
                }
                break;
            case STRAIGHT:
                audioMgr.playSfx(AudioManager.SFX_STRAIGHT);
                vibMgr.vibrateStraight();
                break;
            case PLANE:
            case PLANE_WITH_WINGS:
                audioMgr.playSfx(AudioManager.SFX_PLANE);
                vibMgr.vibratePlane();
                break;
            default:
                audioMgr.playSfx(AudioManager.SFX_PLAY_CARD);
                vibMgr.vibratePlay();
        }
    }

    private void updateInfo() {
        int multiple = engine.getMultiple();
        tvMultiple.setText("倍数：" + multiple);
        tvCoins.setText("斗币：" + coinMgr.getCoins());
        tvLandlord.setText("地主：玩家" + (engine.getLandlord() + 1));

        int cur = engine.getCurrentPlayer();
        if (cur == GameEngine.PLAYER_HUMAN) {
            tvInfo.setText("轮到你出牌了！");
        } else {
            tvInfo.setText("等待AI" + cur + "出牌...");
        }
    }

    private void onGameEnd(int winner) {
        int[] result = engine.calculateResult(winner);
        boolean humanWin = (winner == GameEngine.PLAYER_HUMAN);

        // 更新斗币
        int baseScore = engine.getBaseScore();
        int multiple = engine.getMultiple();
        if (humanWin) {
            coinMgr.winGame(baseScore, multiple);
            audioMgr.playSfx(AudioManager.SFX_WIN);
            vibMgr.vibrateWin();
        } else {
            coinMgr.loseGame(baseScore, multiple);
            audioMgr.playSfx(AudioManager.SFX_LOSE);
            vibMgr.vibrateLose();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(humanWin ? "🎉 你赢了！" : "😢 你输了！\n");
        sb.append("倍数：").append(multiple).append("\n");
        sb.append("得分变化：").append(result[GameEngine.PLAYER_HUMAN]).append("\n");
        sb.append("当前斗币：").append(coinMgr.getCoins()).append("\n");
        sb.append("段位：").append(coinMgr.getRankName());

        new AlertDialog.Builder(this)
            .setTitle(humanWin ? "胜利！" : "失败")
            .setMessage(sb.toString())
            .setPositiveButton("再来一局", (d, w) -> startNewRound())
            .setNegativeButton("返回首页", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioMgr != null) audioMgr.shutdown();
        if (effectMgr != null) effectMgr.cleanup();
    }
}
