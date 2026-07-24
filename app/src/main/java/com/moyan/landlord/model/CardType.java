package com.moyan.landlord.model;

public enum CardType {
    SINGLE,        // 单张
    PAIR,          // 对子
    TRIPLE,        // 三张
    TRIPLE_WITH_ONE,  // 三带一
    TRIPLE_WITH_PAIR, // 三带二
    STRAIGHT,      // 顺子（5+张连续）
    STRAIGHT_PAIR, // 连对（3+对连续）
    PLANE,         // 飞机（2+三张连续）
    PLANE_WITH_WINGS, // 飞机带翅膀
    FOUR_WITH_TWO, // 四带二
    BOMB,          // 炸弹（四张同点）
    KING_BOMB,     // 王炸
    PASS,          // 过牌
    INVALID        // 非法牌型
}
