package com.moyan.landlord.engine;

import android.content.Context;
import android.content.SharedPreferences;
import com.moyan.landlord.MoyanApp;

public class CoinRankManager {
    private final SharedPreferences sp;
    private int coins;
    private int rank;

    // 段位定义
    public static final String[] RANK_NAMES = {
        "新手", "学徒", "熟手", "高手", "精英",
        "专家", "大师", "宗师", "王者", "至尊斗帝"
    };
    public static final int MAX_RANK = 9;

    // 各段位入场费
    public static final int[] ENTRY_FEE = {
        0, 50, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000
    };

    // 各段位AI难度
    public static final int[] AI_DIFFICULTY = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9
    };

    public CoinRankManager(Context ctx) {
        this.sp = MoyanApp.getPrefs(ctx);
        this.coins = sp.getInt(MoyanApp.KEY_COINS, 1000);
        this.rank = sp.getInt(MoyanApp.KEY_RANK, 0);
    }

    public int getCoins() { return coins; }
    public int getRank() { return rank; }
    public String getRankName() { return RANK_NAMES[Math.min(rank, MAX_RANK)]; }

    /** 检查能否进入某段位 */
    public boolean canEnter(int targetRank) {
        if (targetRank < 0 || targetRank > MAX_RANK) return false;
        return coins >= ENTRY_FEE[targetRank];
    }

    /** 当前可进入的最高段位 */
    public int getMaxAffordableRank() {
        int r = rank;
        while (r < MAX_RANK && coins >= ENTRY_FEE[r + 1]) r++;
        return r;
    }

    /** 赢得一局 */
    public void winGame(int baseScore, int multiple) {
        int earn = baseScore * multiple;
        // 高段位赢更多
        earn = (int) (earn * (1 + rank * 0.1));
        coins += earn;
        // 升段
        if (rank < MAX_RANK && coins >= ENTRY_FEE[rank + 1]) {
            rank++;
        }
        save();
    }

    /** 输掉一局 */
    public void loseGame(int baseScore, int multiple) {
        int lose = baseScore * multiple;
        coins = Math.max(0, coins - lose);
        // 降段
        if (rank > 0 && coins < ENTRY_FEE[rank]) {
            rank--;
        }
        save();
    }

    /** 获取当前段位对应的AI难度 */
    public int getCurrentAiDifficulty() {
        return AI_DIFFICULTY[rank];
    }

    /** 获取当前段位入场费 */
    public int getCurrentEntryFee() {
        return ENTRY_FEE[rank];
    }

    private void save() {
        sp.edit()
            .putInt(MoyanApp.KEY_COINS, coins)
            .putInt(MoyanApp.KEY_RANK, rank)
            .apply();
    }

    /** 重置（开发者用） */
    public void reset() {
        coins = 1000;
        rank = 0;
        save();
    }
}
