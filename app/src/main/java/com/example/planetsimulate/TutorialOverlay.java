package com.example.planetsimulate;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TutorialOverlay {

    private static final String PREF_NAME = "tutorial";
    private static final String PREF_DONE = "done";

    private final AppCompatActivity activity;
    private OverlayView overlayView;
    private View cardView;
    private int currentStep = 0;

    private static final Object[][] STEPS = {
            {"選擇行星",     "從這裡選擇要模擬的星球\n共有 8 顆行星可以選擇",              R.id.rowPlanet},
            {"設定距離",     "調整物體距行星中心的距離\n往右拉距離越遠",                    R.id.rowDistanceSeek},
            {"設定初速",     "調整物體的切線初速度\n接近 40% 時會顯示「≈圓軌」提示",        R.id.rowVelocitySeek},
            {"設定角度",     "調整初速方向\n0° = 切線，90° = 朝行星，180° = 反切線",        R.id.rowAngle},
            {"快速載入情境", "點擊情境可一鍵載入\n月球、ISS、同步衛星等真實數據",           R.id.btnPreset},
            {"開始模擬",     "設定好後按開始\n雙擊畫面可以重置視角\n雙指捏合可以縮放",      R.id.btnStart},
    };

    public TutorialOverlay(AppCompatActivity activity) {
        this.activity = activity;
    }

    public static boolean shouldShow(Context context) {
        return !context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_DONE, false);
    }

    private static void markDone(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(PREF_DONE, true).apply();
    }

    public void show() {
        ViewGroup root = activity.findViewById(android.R.id.content);

        overlayView = new OverlayView(activity);
        root.addView(overlayView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        cardView = LayoutInflater.from(activity).inflate(R.layout.tutorial_card, root, false);
        root.addView(cardView);

        cardView.findViewById(R.id.tutBtnNext).setOnClickListener(v ->
                activity.getWindow().getDecorView().post(() -> showStep(currentStep + 1)));
        cardView.findViewById(R.id.tutBtnSkip).setOnClickListener(v ->
                activity.getWindow().getDecorView().post(() -> dismiss()));

        showStep(0);
    }

    private void dismiss() {
        markDone(activity);
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (overlayView != null && overlayView.getParent() == root) root.removeView(overlayView);
        if (cardView != null && cardView.getParent() == root) root.removeView(cardView);
        overlayView = null;
        cardView = null;
    }

    private void showStep(int step) {
        currentStep = step;
        if (step >= STEPS.length) {
            dismiss();
            return;
        }
        if (overlayView == null || cardView == null) return;

        String title = (String) STEPS[step][0];
        String desc  = (String) STEPS[step][1];
        int targetId = (int)   STEPS[step][2];

        ((TextView) cardView.findViewById(R.id.tutTitle)).setText(title);
        ((TextView) cardView.findViewById(R.id.tutDesc)).setText(desc);
        ((TextView) cardView.findViewById(R.id.tutStep)).setText((step + 1) + " / " + STEPS.length);
        ((Button)   cardView.findViewById(R.id.tutBtnNext))
                .setText(step == STEPS.length - 1 ? "完成" : "下一步");

        View target = activity.findViewById(targetId);
        overlayView.setTarget(target, step, STEPS.length);
    }

    class OverlayView extends View {

        private final Paint paintDim  = new Paint();
        private final Paint paintHole = new Paint();
        private final Paint paintDot  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);

        private RectF targetRect = new RectF();
        private int step = 0;
        private int total = 0;

        OverlayView(Context context) {
            super(context);
            setClickable(true);

            paintDim.setColor(0xD0000000);
            paintDim.setStyle(Paint.Style.FILL);

            paintHole.setColor(Color.TRANSPARENT);
            paintHole.setStyle(Paint.Style.FILL);
            paintHole.setXfermode(new android.graphics.PorterDuffXfermode(
                    android.graphics.PorterDuff.Mode.CLEAR));

            paintBorder.setColor(0xFF7C6FFF);
            paintBorder.setStyle(Paint.Style.STROKE);
            paintBorder.setStrokeWidth(3f);

            paintDot.setStyle(Paint.Style.FILL);
        }

        void setTarget(View target, int step, int total) {
            this.step  = step;
            this.total = total;
            if (target != null) {
                target.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                target.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                int[] loc = new int[2];
                                target.getLocationOnScreen(loc);
                                int pad = 14;
                                targetRect.set(
                                        loc[0] - pad, loc[1] - pad,
                                        loc[0] + target.getWidth() + pad,
                                        loc[1] + target.getHeight() + pad);
                                invalidate();
                            }
                        });
            } else {
                targetRect.setEmpty();
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paintDim);
            if (!targetRect.isEmpty()) {
                canvas.drawRoundRect(targetRect, 14f, 14f, paintHole);
                canvas.drawRoundRect(targetRect, 14f, 14f, paintBorder);
            }
        }
    }
}
