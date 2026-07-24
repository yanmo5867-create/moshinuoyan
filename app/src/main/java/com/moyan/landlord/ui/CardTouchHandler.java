package com.moyan.landlord.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.moyan.landlord.model.Card;
import java.util.ArrayList;
import java.util.List;

public class CardTouchHandler implements View.OnTouchListener {
    private final ViewGroup cardContainer;
    private final List<CardView> cardViews = new ArrayList<>();
    private float startX = -1;
    private float startY = -1;
    private static final float SLIDE_THRESHOLD = 20f; // 滑动触发阈值(dp)
    private boolean isSliding = false;
    private float density = 1f;

    // 回调
    private OnCardSelectionChangedListener listener;

    public interface OnCardSelectionChangedListener {
        void onSelectionChanged(List<Card> selectedCards);
    }

    public CardTouchHandler(ViewGroup container) {
        this.cardContainer = container;
        this.density = container.getResources().getDisplayMetrics().density;
    }

    public void setOnSelectionChangedListener(OnCardSelectionChangedListener l) {
        this.listener = l;
    }

    /** 设置卡牌视图列表 */
    public void setCardViews(List<CardView> views) {
        cardViews.clear();
        cardViews.addAll(views);
        for (CardView cv : cardViews) {
            cv.setOnTouchListener(this);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!(v instanceof CardView)) return false;
        CardView touched = (CardView) v;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                isSliding = false;
                return true; // 必须返回true才能接收后续事件

            case MotionEvent.ACTION_MOVE:
                if (startX < 0) return false;
                float dx = event.getX() - startX;
                float dy = event.getY() - startY;
                float threshold = SLIDE_THRESHOLD * density;

                if (!isSliding && Math.abs(dx) > threshold) {
                    isSliding = true;
                }

                if (isSliding) {
                    // 右滑 = 选中，左滑 = 取消选中
                    if (dx > 0) {
                        // 选中touched及右侧所有卡牌
                        selectFromTo(touched, true);
                    } else {
                        // 取消选中touched及右侧所有卡牌
                        selectFromTo(touched, false);
                    }
                    startX = event.getX(); // 重置起点
                    notifyListener();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isSliding && startX >= 0) {
                    // 单击：切换选中状态
                    toggleCard(touched);
                    notifyListener();
                }
                startX = -1;
                startY = -1;
                isSliding = false;
                return true;
        }
        return false;
    }

    private void toggleCard(CardView cv) {
        boolean newState = !cv.isSelected();
        cv.setSelected(newState);
        animateCard(cv, newState);
    }

    private void selectFromTo(CardView target, boolean select) {
        int targetIdx = cardViews.indexOf(target);
        if (targetIdx < 0) return;
        // 选中target及其右侧所有
        for (int i = targetIdx; i < cardViews.size(); i++) {
            CardView cv = cardViews.get(i);
            if (cv.isSelected() != select) {
                cv.setSelected(select);
                animateCard(cv, select);
            }
        }
    }

    private void animateCard(CardView cv, boolean selected) {
        float ty = selected ? -30f : 0f;
        cv.animate().translationY(ty).setDuration(120).start();
    }

    private void notifyListener() {
        if (listener == null) return;
        List<Card> selected = new ArrayList<>();
        for (CardView cv : cardViews) {
            if (cv.isSelected() && cv.getCard() != null) {
                selected.add(cv.getCard());
            }
        }
        listener.onSelectionChanged(selected);
    }

    /** 取消所有选中 */
    public void clearSelection() {
        for (CardView cv : cardViews) {
            if (cv.isSelected()) {
                cv.setSelected(false);
                cv.animate().translationY(0f).setDuration(100).start();
            }
        }
        notifyListener();
    }

    /** 获取当前选中的牌 */
    public List<Card> getSelectedCards() {
        List<Card> result = new ArrayList<>();
        for (CardView cv : cardViews) {
            if (cv.isSelected() && cv.getCard() != null) {
                result.add(cv.getCard());
            }
        }
        return result;
    }
}
