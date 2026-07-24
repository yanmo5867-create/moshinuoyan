package com.moyan.landlord.engine;

import com.moyan.landlord.model.Card;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardCounter {
    private final Map<Integer, Integer> remaining = new HashMap<>();

    public CardCounter() {
        // 初始化：每种rank（3-15）各4张，王各1张
        for (int r = 3; r <= 15; r++) remaining.put(r, 4);
        remaining.put(16, 1); // 小王
        remaining.put(17, 1); // 大王
    }

    /** 记录已出的牌 */
    public void recordPlayed(List<Card> played) {
        if (played == null) return;
        for (Card c : played) {
            int r = c.rank;
            int left = remaining.getOrDefault(r, 0);
            if (left > 0) remaining.put(r, left - 1);
        }
    }

    /** 某rank还剩几张 */
    public int getRemaining(int rank) {
        return remaining.getOrDefault(rank, 0);
    }

    /** 是否还有炸弹可能（某rank剩>=4） */
    public boolean bombPossible() {
        for (int v : remaining.values()) if (v >= 4) return true;
        return false;
    }

    /** 获取所有剩余牌信息（用于UI显示） */
    public Map<Integer, Integer> getRemainingMap() {
        return new HashMap<>(remaining);
    }

    /** 重置 */
    public void reset() {
        remaining.clear();
        for (int r = 3; r <= 15; r++) remaining.put(r, 4);
        remaining.put(16, 1);
        remaining.put(17, 1);
    }
}
