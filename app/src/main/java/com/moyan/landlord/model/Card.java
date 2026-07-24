package com.moyan.landlord.model;

public class Card {
    public final int suit; // 0-3: 黑桃桃草花，4:小王，5:大王
    public final int rank; // 3-15 (J=11,Q=12,K=13,A=14,2=15,小王=16,大王=17)
    public final int value; // 用于排序和比较的数值

    public Card(int suit, int rank) {
        this.suit = suit;
        this.rank = rank;
        if (rank == 16) this.value = 16;       // 小王
        else if (rank == 17) this.value = 17;  // 大王
        else this.value = rank;
    }

    public String getSuitSymbol() {
        switch (suit) {
            case 0: return "♠";
            case 1: return "♥";
            case 2: return "♣";
            case 3: return "♦";
            case 4: return "🃏"; // 小王
            case 5: return "🃏"; // 大王
            default: return "?";
        }
    }

    public String getRankText() {
        switch (rank) {
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            case 14: return "A";
            case 15: return "2";
            case 16: return "w";
            case 17: return "W";
            default: return String.valueOf(rank);
        }
    }

    public String getDisplayName() {
        return getSuitSymbol() + getRankText();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public boolean isJoker() {
        return rank >= 16;
    }

    public boolean isBigJoker() {
        return rank == 17;
    }

    public boolean isSmallJoker() {
        return rank == 16;
    }
}
