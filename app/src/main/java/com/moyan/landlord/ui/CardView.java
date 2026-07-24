package com.moyan.landlord.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import com.moyan.landlord.model.Card;

public class CardView extends View {
    private Card card;
    private boolean selected = false;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cardRect = new RectF();
    private static final float CORNER_RADIUS = 12f;
    private static final float BORDER_WIDTH = 2f;

    public CardView(Context context) {
        super(context);
        setWillNotDraw(false);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setCard(Card card) {
        this.card = card;
        invalidate();
    }

    public Card getCard() { return card; }

    public void setSelected(boolean s) {
        this.selected = s;
        invalidate();
    }

    public boolean isSelected() { return selected; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (card == null) return;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        cardRect.set(2, 2, w - 2, h - 2);

        // 背景
        paint.setColor(selected ? Color.parseColor("#E8F5E9") : Color.WHITE);
        canvas.drawRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, paint);

        // 边框
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(BORDER_WIDTH);
        if (selected) {
            borderPaint.setColor(Color.parseColor("#4CAF50"));
        } else if (card.isBigJoker()) {
            borderPaint.setColor(Color.parseColor("#FFD700"));
        } else if (card.isSmallJoker()) {
            borderPaint.setColor(Color.parseColor("#C0C0C0"));
        } else {
            borderPaint.setColor(Color.parseColor("#BDBDBD"));
        }
        canvas.drawRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint);

        // 文字颜色
        int textColor;
        if (card.isJoker()) {
            textColor = card.isBigJoker() ? Color.parseColor("#E53935") : Color.BLACK;
        } else if (card.suit == 0 || card.suit == 3) {
            textColor = Color.parseColor("#D32F2F"); // 黑桃/方块红色
        } else if (card.suit == 1) {
            textColor = Color.parseColor("#E53935"); // 红桃红色
        } else {
            textColor = Color.BLACK; // 梅花黑色
        }
        // 方块用红色
        if (card.suit == 3) textColor = Color.parseColor("#D32F2F");

        textPaint.setColor(textColor);
        textPaint.setTextSize(Math.min(w, h) * 0.35f);

        String center = card.getRankText();
        float cx = w / 2f;
        float cy = h / 2f + textPaint.getTextSize() * 0.35f;
        canvas.drawText(center, cx, cy, textPaint);

        // 左上角小标记
        textPaint.setTextSize(Math.min(w, h) * 0.18f);
        String small = card.getSuitSymbol();
        canvas.drawText(small, w * 0.18f, h * 0.22f, textPaint);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = (int) (w * 1.45f);
        setMeasuredDimension(w, h);
    }
}
