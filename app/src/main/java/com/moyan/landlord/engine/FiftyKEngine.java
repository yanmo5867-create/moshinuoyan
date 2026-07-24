package com.moyan.landlord.engine;

import com.moyan.landlord.model.Card;
import com.moyan.landlord.model.CardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FiftyKEngine {

    // 五十K特殊牌型：5+10+K同花色 = 炸弹
    public static final int FIFTY_K_VALUE = 50;

    /** 检查手中是否有五十K（5+10+K同花色） */
    public static List<Card> findFiftyK(List<Card> hand) {
        Map<Integer, List<Card>> bySuit = new HashMap<>();
        for (Card c : hand) {
            if (c.rank >= 3 && c.rank <= 15) {
                bySuit.computeIfAbsent(c.suit, k -> new ArrayList<>()).add(c);
            }
        }
        for (Map.Entry<Integer, List<Card>> e : bySuit.entrySet()) {
            boolean has5 = false, has10 = false, hasK = false;
            for (Card c : e.getValue()) {
                if (c.rank == 5) has5 = true;
                if (c.rank == 10) has10 = true;
                if (c.rank == 13) hasK = true;
            }
            if (has5 && has10 && hasK) {
                List<Card> result = new ArrayList<>();
                for (Card c : e.getValue()) {
                    if (c.rank == 5 || c.rank == 10 || c.rank == 13) {
                        result.add(c);
                    }
                }
                return result;
            }
        }
        return null;
    }

    /** 创建五十K专用牌组（不含大小王，两副牌去掉部分） */
    public static List<Card> createFiftyKDeck() {
        List<Card> deck = new ArrayList<>();
        // 用两副牌的前半部分（去王），确保5/10/K齐全
        for (int i = 0; i < 2; i++) {
            for (int s = 0; s < 4; s++) {
                for (int r = 3; r <= 15; r++) {
                    deck.add(new Card(s, r));
                }
            }
        }
        // 加大小王
        deck.add(new Card(4, 16));
        deck.add(new Card(5, 17));
        return deck;
    }

    /** 五十K牌型识别（在普通识别基础上增加五十K检测） */
    public static CardType recognizeWithFiftyK(List<Card> cards) {
        CardType normal = CardEngine.recognizeType(cards);
        if (normal != CardType.INVALID) return normal;

        // 检查是否是五十K
        if (cards.size() == 3) {
            boolean has5 = false, has10 = false, hasK = false;
            int suit = -1;
            for (Card c : cards) {
                if (suit == -1) suit = c.suit;
                else if (c.suit != suit) return CardType.INVALID;
                if (c.rank == 5) has5 = true;
                else if (c.rank == 10) has10 = true;
                else if (c.rank == 13) hasK = true;
            }
            if (has5 && has10 && hasK) return CardType.BOMB; // 五十K当炸弹
        }
        return CardType.INVALID;
    }
}
