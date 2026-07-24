package com.moyan.landlord.engine;

import com.moyan.landlord.model.Card;
import com.moyan.landlord.model.CardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameEngine {

    public static final int PLAYER_HUMAN = 0;
    public static final int PLAYER_AI_LEFT = 1;
    public static final int PLAYER_AI_RIGHT = 2;

    private List<Card>[] hands;
    private List<Card> bottomCards;
    private int landlord = -1; // 地主是谁
    private int currentPlayer;
    private List<Card> lastPlay; // 上一手牌
    private int lastPlayer; // 上一手是谁出的
    private int baseScore = 1; // 底分
    private int multiple = 1; // 倍数
    private boolean spring = true; // 是否可能春天
    private CardCounter counter;

    private final Random random = new Random();

    public GameEngine() {
        this.counter = new CardCounter();
    }

    /** 开始新一局 */
    public void startNewGame(int difficulty) {
        List<Card> deck = CardEngine.createDeck();
        CardEngine.shuffle(deck);
        CardEngine.DealResult dr = CardEngine.dealCards(deck);
        hands = new List[]{dr.hand0, dr.hand1, dr.hand2};
        bottomCards = dr.bottom;
        counter.reset();
        landlord = -1;
        lastPlay = new ArrayList<>();
        lastPlayer = -1;
        multiple = 1;
        spring = true;
    }

    /** 叫地主阶段：返回叫分结果（0/1/2/3） */
    public int[] biddingPhase() {
        int[] scores = new int[3];
        for (int i = 0; i < 3; i++) {
            AIEngine ai = new AIEngine(5); // 叫分阶段用中等AI
            ai.setCounter(counter);
            scores[i] = ai.bidScore(hands[i]) / 25; // 转成0-4分
            scores[i] = Math.min(3, Math.max(0, scores[i]));
        }
        // 确定地主
        int maxScore = -1;
        for (int i = 0; i < 3; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                landlord = i;
            }
        }
        if (maxScore <= 0) {
            // 全不叫，重新发牌
            startNewGame(5);
            return biddingPhase();
        }
        // 地主拿底牌
        hands[landlord].addAll(bottomCards);
        CardEngine.sortCards(hands[landlord]);
        counter.recordPlayed(bottomCards); // 底牌不算已出
        baseScore = maxScore;
        multiple = maxScore;
        currentPlayer = landlord; // 地主先出
        lastPlay = new ArrayList<>();
        lastPlayer = -1;
        return scores;
    }

    /** 出牌：返回是否合法 */
    public boolean playCards(int player, List<Card> cards) {
        if (player != currentPlayer) return false;
        if (cards == null || cards.isEmpty()) {
            // 过牌
            return true;
        }
        // 验证牌型
        CardType type = CardEngine.recognizeType(cards);
        if (type == CardType.INVALID) return false;

        // 如果不是首发，需要压过上家
        if (lastPlay != null && !lastPlay.isEmpty()) {
            if (!CardEngine.canBeat(cards, lastPlay)) return false;
        }

        // 合法，从手牌移除
        hands[player].removeAll(cards);
        counter.recordPlayed(cards);

        // 更新倍数
        if (type == CardType.BOMB || type == CardType.KING_BOMB) {
            multiple *= 2;
        }

        lastPlay = new ArrayList<>(cards);
        lastPlayer = player;
        spring = false; // 有人接牌了就不是春天

        // 检查是否结束
        if (hands[player].isEmpty()) {
            return true; // 赢了
        }

        // 下一个玩家
        currentPlayer = (player + 1) % 3;
        return true;
    }

    /** 过牌 */
    public void pass(int player) {
        if (player != currentPlayer) return;
        // 如果连续两人过牌，重置lastPlay
        int next = (player + 1) % 3;
        // 简化处理：直接到下家
        currentPlayer = next;
        // 如果回到地主且地主没牌了，说明下家赢了
    }

    /** 检查游戏是否结束，返回赢家（0/1/2），-1表示未结束 */
    public int checkWinner() {
        for (int i = 0; i < 3; i++) {
            if (hands[i].isEmpty()) return i;
        }
        return -1;
    }

    /** 计算结算：返回每个人的得分变化 */
    public int[] calculateResult(int winner) {
        int[] result = new int[3];
        boolean isSpring = (winner == landlord) && spring;
        boolean antiSpring = (winner != landlord) && spring;

        int finalMultiple = multiple;
        if (isSpring || antiSpring) finalMultiple *= 2;

        int score = baseScore * finalMultiple;
        if (winner == landlord) {
            result[landlord] = score * 2;
            result[(landlord + 1) % 3] = -score;
            result[(landlord + 2) % 3] = -score;
        } else {
            result[winner] = score;
            result[(winner + 1) % 3] = score; // 队友也赢
            result[landlord] = -score * 2;
        }
        return result;
    }

    // ===== Getters =====
    public List<Card> getHand(int player) { return new ArrayList<>(hands[player]); }
    public int getLandlord() { return landlord; }
    public int getCurrentPlayer() { return currentPlayer; }
    public List<Card> getLastPlay() { return new ArrayList<>(lastPlay); }
    public int getMultiple() { return multiple; }
    public int getBaseScore() { return baseScore; }
    public CardCounter getCounter() { return counter; }
    public List<Card> getBottomCards() { return new ArrayList<>(bottomCards); }

    /** 获取玩家角色名 */
    public String getRoleName(int player) {
        if (player == landlord) return "地主";
        return "农民";
    }

    /** 是否轮到人类 */
    public boolean isHumanTurn() {
        return currentPlayer == PLAYER_HUMAN;
    }

    /** 简单AI自动出牌 */
    public List<Card> aiAutoPlay(int aiDifficulty) {
        int player = currentPlayer;
        AIEngine ai = new AIEngine(aiDifficulty);
        ai.setCounter(counter);
        List<Card> play = ai.think(hands[player], lastPlay, lastPlay.isEmpty());
        return play;
    }
}
