package com.moyan.landlord.engine;

import com.moyan.landlord.model.Card;
import com.moyan.landlord.model.CardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AIEngine {

    private final int difficulty; // 0=菜鸟, 9=至尊
    private final Random random = new Random();
    private CardCounter counter;

    public AIEngine(int difficulty) {
        this.difficulty = Math.max(0, Math.min(9, difficulty));
    }

    public void setCounter(CardCounter counter) {
        this.counter = counter;
    }

    /** AI主入口：根据当前手牌和场上最后出的牌，决定出什么 */
    public List<Card> think(List<Card> myHand, List<Card> lastPlay, boolean isFirstPlay) {
        List<Card> hand = new ArrayList<>(myHand);
        CardEngine.sortCards(hand);

        // 如果没人出牌（自己先手），主动出击
        if (isFirstPlay || lastPlay == null || lastPlay.isEmpty()) {
            return playFirst(hand);
        }

        CardType lastType = CardEngine.recognizeType(lastPlay);
        int myRank = calcHandRank(hand);

        // 菜鸟：随机过牌或乱出
        if (difficulty <= 1) {
            if (random.nextDouble() < 0.3) return new ArrayList<>(); // 30%过牌
            return playFirst(hand); // 随便出
        }

        // 中等：能压就压，不能就过
        if (difficulty <= 4) {
            List<Card> candidate = findBeat(hand, lastPlay, lastType);
            if (candidate != null && !willBeNaked(candidate, hand)) {
                return candidate;
            }
            return new ArrayList<>(); // 过牌
        }

        // 高手+：策略性出牌
        // 1. 如果有炸弹且不划算，留着
        // 2. 如果能一波带走，果断出
        // 3. 记牌判断对方是否可能炸

        List<Card> beat = findBeat(hand, lastPlay, lastType);
        if (beat == null) return new ArrayList<>();

        // 至尊AI：深度策略
        if (difficulty >= 7) {
            // 如果出完这手牌就能赢，直接出
            List<Card> temp = new ArrayList<>(hand);
            temp.removeAll(beat);
            if (temp.isEmpty()) return beat;

            // 如果对方只剩很少牌，考虑用炸弹
            // 如果自己手牌还多，留大牌
            if (myRank > 5 && beat.size() <= 2) {
                // 还有大牌在手，可以压
                return beat;
            }
            if (myRank <= 3) {
                // 手牌少了，赶紧走
                return beat;
            }
            return beat;
        }

        return beat;
    }

    /** 主动出牌（先手） */
    private List<Card> playFirst(List<Card> hand) {
        // 菜鸟：随便出一张最小的
        if (difficulty == 0) {
            return Collections.singletonList(hand.get(hand.size() - 1));
        }

        // 找最小的单张/对子/三张出
        Map<Integer, List<Card>> groups = groupByRank(hand);

        // 优先出三张带（如果有）
        for (Map.Entry<Integer, List<Card>> e : groups.entrySet()) {
            if (e.getValue().size() >= 3) {
                List<Card> result = new ArrayList<>(e.getValue().subList(0, 3));
                // 带一张最小的单牌
                for (Card c : hand) {
                    if (!result.contains(c)) {
                        result.add(c);
                        break;
                    }
                }
                return result;
            }
        }

        // 出对子
        for (Map.Entry<Integer, List<Card>> e : groups.entrySet()) {
            if (e.getValue().size() >= 2 && e.getKey() < 15) { // 不先出2
                return new ArrayList<>(e.getValue().subList(0, 2));
            }
        }

        // 出单张（最小的，不送王）
        for (int i = hand.size() - 1; i >= 0; i--) {
            Card c = hand.get(i);
            if (!c.isJoker()) return Collections.singletonList(c);
        }
        return Collections.singletonList(hand.get(hand.size() - 1));
    }

    /** 寻找能压过lastPlay的牌组合 */
    private List<Card> findBeat(List<Card> hand, List<Card> lastPlay, CardType type) {
        Map<Integer, List<Card>> groups = groupByRank(hand);
        List<Integer> ranks = new ArrayList<>(groups.keySet());
        Collections.sort(ranks);

        switch (type) {
            case SINGLE: {
                int target = getMaxRank(lastPlay);
                for (int r : ranks) {
                    if (r > target && !isJokerRank(r)) {
                        return Collections.singletonList(groups.get(r).get(0));
                    }
                }
                // 尝试用王
                if (hasJoker(hand, 16) && target < 16) return Collections.singletonList(findJoker(hand, 16));
                if (hasJoker(hand, 17) && target < 17) return Collections.singletonList(findJoker(hand, 17));
                // 炸弹
                return findAnyBomb(hand);
            }
            case PAIR: {
                int target = getMaxRank(lastPlay);
                for (int r : ranks) {
                    if (r > target && groups.get(r).size() >= 2) {
                        return new ArrayList<>(groups.get(r).subList(0, 2));
                    }
                }
                return findAnyBomb(hand);
            }
            case TRIPLE:
            case TRIPLE_WITH_ONE:
            case TRIPLE_WITH_PAIR: {
                int target = CardEngine.getMainRank(lastPlay);
                for (int r : ranks) {
                    if (r > target && groups.get(r).size() >= 3) {
                        List<Card> result = new ArrayList<>(groups.get(r).subList(0, 3));
                        // 补带牌
                        for (Card c : hand) {
                            if (!result.contains(c) && result.size() < lastPlay.size()) {
                                result.add(c);
                            }
                        }
                        return result;
                    }
                }
                return findAnyBomb(hand);
            }
            case STRAIGHT: {
                int len = lastPlay.size();
                return findStraightBeat(hand, lastPlay, len);
            }
            case BOMB: {
                int target = getMaxRank(lastPlay);
                for (int r : ranks) {
                    if (r > target && groups.get(r).size() >= 4) {
                        return new ArrayList<>(groups.get(r).subList(0, 4));
                    }
                }
                // 王炸
                if (hasJoker(hand, 16) && hasJoker(hand, 17)) {
                    List<Card> r = new ArrayList<>();
                    r.add(findJoker(hand, 16));
                    r.add(findJoker(hand, 17));
                    return r;
                }
                return null;
            }
            case KING_BOMB:
                return null; // 王炸无敌
            default:
                return null;
        }
    }

    private List<Card> findStraightBeat(List<Card> hand, List<Card> lastPlay, int len) {
        // 简化：找比lastPlay最大rank大1的顺子
        int maxRank = getMaxRank(lastPlay);
        // 从maxRank+1开始尝试组顺子
        for (int start = maxRank + 1; start <= 14 - len + 1; start++) {
            List<Card> straight = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < len; i++) {
                int r = start + i;
                if (r > 14) { ok = false; break; }
                // 找hand中rank==r的牌
                Card found = null;
                for (Card c : hand) {
                    if (c.rank == r) { found = c; break; }
                }
                if (found == null) { ok = false; break; }
                straight.add(found);
            }
            if (ok && straight.size() == len) return straight;
        }
        return findAnyBomb(hand);
    }

    private List<Card> findAnyBomb(List<Card> hand) {
        Map<Integer, List<Card>> groups = groupByRank(hand);
        for (List<Card> g : groups.values()) {
            if (g.size() >= 4) return new ArrayList<>(g.subList(0, 4));
        }
        return null;
    }

    private boolean willBeNaked(List<Card> play, List<Card> hand) {
        // 简化判断：出完之后手牌剩很少且有大牌在外面
        List<Card> remain = new ArrayList<>(hand);
        remain.removeAll(play);
        return remain.size() <= 1;
    }

    private Map<Integer, List<Card>> groupByRank(List<Card> cards) {
        Map<Integer, List<Card>> m = new HashMap<>();
        for (Card c : cards) {
            m.computeIfAbsent(c.rank, k -> new ArrayList<>()).add(c);
        }
        return m;
    }

    private int getMaxRank(List<Card> cards) {
        int max = 0;
        for (Card c : cards) if (c.rank > max) max = c.rank;
        return max;
    }

    private boolean isJokerRank(int r) { return r == 16 || r == 17; }
    private boolean hasJoker(List<Card> hand, int jokerRank) {
        for (Card c : hand) if (c.rank == jokerRank) return true;
        return false;
    }
    private Card findJoker(List<Card> hand, int jokerRank) {
        for (Card c : hand) if (c.rank == jokerRank) return c;
        return null;
    }

    /** 估算手牌强度（越大越强） */
    private int calcHandRank(List<Card> hand) {
        int score = 0;
        for (Card c : hand) {
            if (c.isBigJoker()) score += 10;
            else if (c.isSmallJoker()) score += 8;
            else if (c.rank == 15) score += 6; // 2
            else if (c.rank == 14) score += 5; // A
            else if (c.rank >= 11) score += 3;
            else score += 1;
        }
        return score;
    }

    /** 叫地主评分（0-100） */
    public int bidScore(List<Card> hand) {
        int score = 0;
        Map<Integer, Integer> cm = new HashMap<>();
        for (Card c : hand) cm.put(c.rank, cm.getOrDefault(c.rank, 0) + 1);

        // 王
        if (cm.getOrDefault(17, 0) > 0) score += 25;
        if (cm.getOrDefault(16, 0) > 0) score += 15;
        // 2的数量
        score += cm.getOrDefault(15, 0) * 8;
        // A的数量
        score += cm.getOrDefault(14, 0) * 5;
        // 炸弹
        for (int v : cm.values()) if (v >= 4) score += 20;
        // 三张
        for (int v : cm.values()) if (v >= 3) score += 8;

        // 难度修正
        score = (int) (score * (0.5 + difficulty * 0.05));

        return Math.max(0, Math.min(100, score));
    }

    /** 根据斗币返回对应难度档位 */
    public static int getDifficultyForCoins(int coins) {
        if (coins < 500) return 0;       // 新手场
        if (coins < 2000) return 2;      // 初级场
        if (coins < 5000) return 4;      // 中级场
        if (coins < 15000) return 6;     // 高级场
        if (coins < 50000) return 8;     // 大师场
        return 9;                          // 至尊场
    }
}
