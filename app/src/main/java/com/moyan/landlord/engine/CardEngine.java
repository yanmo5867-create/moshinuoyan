package com.moyan.landlord.engine;

import com.moyan.landlord.model.Card;
import com.moyan.landlord.model.CardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardEngine {

    /** 生成一副54张扑克牌 */
    public static List<Card> createDeck() {
        List<Card> deck = new ArrayList<>();
        for (int s = 0; s < 4; s++) {
            for (int r = 3; r <= 15; r++) {
                deck.add(new Card(s, r));
            }
        }
        deck.add(new Card(4, 16)); // 小王
        deck.add(new Card(5, 17)); // 大王
        return deck;
    }

    /** 洗牌 */
    public static void shuffle(List<Card> deck) {
        Collections.shuffle(deck);
    }

    /** 发牌：返回3个手牌列表 + 底牌（3张） */
    public static DealResult dealCards(List<Card> deck) {
        List<Card>[] hands = new List[3];
        for (int i = 0; i < 3; i++) {
            hands[i] = new ArrayList<>();
        }
        for (int i = 0; i < 51; i++) {
            hands[i % 3].add(deck.get(i));
        }
        List<Card> bottom = new ArrayList<>();
        for (int i = 51; i < 54; i++) {
            bottom.add(deck.get(i));
        }
        // 排序所有手牌
        for (List<Card> h : hands) {
            sortCards(h);
        }
        return new DealResult(hands[0], hands[1], hands[2], bottom);
    }

    /** 按value降序排序 */
    public static void sortCards(List<Card> cards) {
        cards.sort(Comparator.comparingInt((Card c) -> c.value).reversed());
    }

    /** 识别牌型 */
    public static CardType recognizeType(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return CardType.PASS;
        int n = cards.size();
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : cards) {
            countMap.put(c.rank, countMap.getOrDefault(c.rank, 0) + 1);
        }
        List<Integer> counts = new ArrayList<>(countMap.values());
        Collections.sort(counts, Collections.reverseOrder());

        // 王炸
        if (n == 2) {
            boolean hasW = false, hasw = false;
            for (Card c : cards) {
                if (c.isBigJoker()) hasW = true;
                if (c.isSmallJoker()) hasw = true;
            }
            if (hasW && hasw) return CardType.KING_BOMB;
        }

        // 单张
        if (n == 1) return CardType.SINGLE;
        // 对子
        if (n == 2 && counts.get(0) == 2) return CardType.PAIR;
        // 三张
        if (n == 3 && counts.get(0) == 3) return CardType.TRIPLE;
        // 炸弹
        if (n == 4 && counts.get(0) == 4) return CardType.BOMB;

        // 三带一
        if (n == 4 && counts.get(0) == 3) return CardType.TRIPLE_WITH_ONE;
        // 三带二
        if (n == 5 && counts.get(0) == 3 && counts.get(1) == 2) return CardType.TRIPLE_WITH_PAIR;

        // 顺子 5+
        if (n >= 5 && isStraight(countMap, n)) return CardType.STRAIGHT;
        // 连对 3对+
        if (n >= 6 && n % 2 == 0 && isStraightPair(countMap, n / 2)) return CardType.STRAIGHT_PAIR;
        // 飞机 2+三张连续
        if (n >= 6 && isPlane(countMap, counts, n)) return CardType.PLANE;

        // 四带二
        if (n == 6 && counts.get(0) == 4) return CardType.FOUR_WITH_TWO;

        return CardType.INVALID;
    }

    private static boolean isStraight(Map<Integer, Integer> cm, int n) {
        // 不能有2和王
        if (cm.containsKey(15) || cm.containsKey(16) || cm.containsKey(17)) return false;
        List<Integer> keys = new ArrayList<>(cm.keySet());
        Collections.sort(keys);
        if (keys.size() != n) return false;
        for (int i = 1; i < keys.size(); i++) {
            if (keys.get(i) - keys.get(i - 1) != 1) return false;
        }
        return true;
    }

    private static boolean isStraightPair(Map<Integer, Integer> cm, int pairs) {
        if (cm.containsKey(15) || cm.containsKey(16) || cm.containsKey(17)) return false;
        List<Integer> keys = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cm.entrySet()) {
            if (e.getValue() == 2) keys.add(e.getKey());
        }
        if (keys.size() != pairs) return false;
        Collections.sort(keys);
        for (int i = 1; i < keys.size(); i++) {
            if (keys.get(i) - keys.get(i - 1) != 1) return false;
        }
        return true;
    }

    private static boolean isPlane(Map<Integer, Integer> cm, List<Integer> counts, int n) {
        int tripleCount = 0;
        for (int c : counts) if (c == 3) tripleCount++;
        // 至少2组三张
        if (tripleCount < 2) return false;
        // 检查连续性
        List<Integer> tripleRanks = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cm.entrySet()) {
            if (e.getValue() == 3) tripleRanks.add(e.getKey());
        }
        Collections.sort(tripleRanks);
        for (int i = 1; i < tripleRanks.size(); i++) {
            if (tripleRanks.get(i) - tripleRanks.get(i - 1) != 1) return false;
        }
        // 飞机带翅膀：剩余牌数 = 0, 2, 4...
        int remaining = n - tripleCount * 3;
        return remaining == 0 || remaining == tripleCount * 2;
    }

    /** 比较两手牌，返回正数表示c1>c2 */
    public static int compareCards(List<Card> c1, List<Card> c2) {
        CardType t1 = recognizeType(c1);
        CardType t2 = recognizeType(c2);
        if (t1 == CardType.INVALID || t2 == CardType.INVALID) return 0;

        // 炸弹和王炸特殊
        if (t1 == CardType.KING_BOMB) return 1;
        if (t2 == CardType.KING_BOMB) return -1;
        if (t1 == CardType.BOMB && t2 != CardType.BOMB) return 1;
        if (t2 == CardType.BOMB && t1 != CardType.BOMB) return -1;

        if (t1 != t2) return 0; // 牌型不同不能压

        int max1 = getMaxRank(c1);
        int max2 = getMaxRank(c2);
        return Integer.compare(max1, max2);
    }

    private static int getMaxRank(List<Card> cards) {
        int max = 0;
        for (Card c : cards) if (c.rank > max) max = c.rank;
        return max;
    }

    /** 判断cards1是否能压住cards2 */
    public static boolean canBeat(List<Card> cards1, List<Card> cards2) {
        if (cards2 == null || cards2.isEmpty()) return true;
        if (cards1 == null || cards1.isEmpty()) return false;
        CardType t1 = recognizeType(cards1);
        CardType t2 = recognizeType(cards2);
        if (t1 == CardType.INVALID) return false;

        // 王炸最大
        if (t1 == CardType.KING_BOMB) return true;
        // 炸弹能压非炸弹
        if (t1 == CardType.BOMB && t2 != CardType.BOMB && t2 != CardType.KING_BOMB) return true;
        if (t2 == CardType.BOMB && t1 != CardType.BOMB && t1 != CardType.KING_BOMB) return false;

        if (t1 != t2) return false;
        return compareCards(cards1, cards2) > 0;
    }

    /** 获取牌型的主rank（用于比较大小） */
    public static int getMainRank(List<Card> cards) {
        Map<Integer, Integer> cm = new HashMap<>();
        for (Card c : cards) cm.put(c.rank, cm.getOrDefault(c.rank, 0) + 1);
        int max = 0;
        for (Map.Entry<Integer, Integer> e : cm.entrySet()) {
            if (e.getValue() >= 3 && e.getKey() > max) max = e.getKey();
        }
        if (max == 0) {
            for (Card c : cards) if (c.rank > max) max = c.rank;
        }
        return max;
    }

    /** 发牌结果封装 */
    public static class DealResult {
        public final List<Card> hand0, hand1, hand2, bottom;
        public DealResult(List<Card> h0, List<Card> h1, List<Card> h2, List<Card> b) {
            this.hand0 = h0; this.hand1 = h1; this.hand2 = h2; this.bottom = b;
        }
    }
}
