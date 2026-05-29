package com.example.planetsimulate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.List;

public class SimulationView extends View {

    private SimulationState state;
    private boolean showTrail = true;

    private final Paint paintPlanet  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintObject  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTrail   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrid    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintOrbit   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintApsides = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float scale   = 1e-6f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private int crashFlashCounter = 0;

    public SimulationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        initGestureDetectors(context);
    }

    public void setState(SimulationState state) {
        this.state = state;
        crashFlashCounter = 0;
        if (getWidth() > 0) autoFitScale();
    }

    public void setShowTrail(boolean show) {
        showTrail = show;
        invalidate();
    }

    private void initPaints() {
        paintPlanet.setStyle(Paint.Style.FILL);

        paintObject.setStyle(Paint.Style.FILL);
        paintObject.setColor(Color.WHITE);

        paintTrail.setStyle(Paint.Style.STROKE);
        paintTrail.setStrokeWidth(2f);

        paintText.setColor(Color.WHITE);
        paintText.setTextSize(28f);

        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(0.5f);
        paintGrid.setColor(0x22FFFFFF);

        paintOrbit.setStyle(Paint.Style.STROKE);
        paintOrbit.setStrokeWidth(1.5f);
        paintOrbit.setColor(0x55FFFFFF);
        paintOrbit.setPathEffect(new DashPathEffect(new float[]{12f, 8f}, 0f));

        paintApsides.setStyle(Paint.Style.FILL);
        paintApsides.setTextSize(24f);
    }

    private void initGestureDetectors(Context context) {
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        scale *= detector.getScaleFactor();
                        scale = Math.max(1e-10f, Math.min(scale, 1e-2f));
                        invalidate();
                        return true;
                    }
                });

        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                            float distanceX, float distanceY) {
                        offsetX -= distanceX;
                        offsetY -= distanceY;
                        invalidate();
                        return true;
                    }
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        offsetX = 0f;
                        offsetY = 0f;
                        autoFitScale();
                        invalidate();
                        return true;
                    }
                });
    }

    private void autoFitScale() {
        if (state == null || getWidth() == 0 || getHeight() == 0) return;
        double viewRadius = Math.max(state.planet.radius * 2.0, state.initialDistance * 1.2);
        float viewMin = Math.min(getWidth(), getHeight());
        scale = (float)(viewMin / (viewRadius * 2.0));
        offsetX = 0f;
        offsetY = 0f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        autoFitScale();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private float worldToScreenX(double worldX) {
        return getWidth() / 2f + offsetX + (float)(worldX * scale);
    }

    private float worldToScreenY(double worldY) {
        return getHeight() / 2f + offsetY + (float)(worldY * scale);
    }

    private float worldToScreenLen(double worldLen) {
        return (float)(worldLen * scale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (state == null) return;

        canvas.drawColor(Color.BLACK);

        drawGrid(canvas);
        drawOrbitPreview(canvas);
        drawApsides(canvas);
        drawPlanet(canvas);
        drawTrail(canvas);
        drawObject(canvas);
        drawCrashedMessage(canvas);
    }

    private void drawGrid(Canvas canvas) {
        float cx = worldToScreenX(0);
        float cy = worldToScreenY(0);
        canvas.drawLine(cx, 0, cx, getHeight(), paintGrid);
        canvas.drawLine(0, cy, getWidth(), cy, paintGrid);
    }

    private void drawApsides(Canvas canvas) {
        if (!state.running && state.trail.isEmpty()) return;

        double G  = SimulationState.G;
        double M  = state.planet.mass;
        double r0 = state.initialDistance;
        double v0 = state.initialVelocity;

        if (v0 <= 0) return;

        double epsilon = 0.5 * v0 * v0 - G * M / r0;
        if (epsilon >= 0) return;

        double angleDeg   = state.initialAngleDeg;
        double tangential = Math.cos(Math.toRadians(angleDeg));
        double h = r0 * v0 * tangential;
        if (Math.abs(h) < 1e3) return;

        double eccSq = 1.0 + (2.0 * epsilon * h * h) / (G * M * G * M);
        if (eccSq < 0) return;
        double ecc = Math.sqrt(eccSq);

        double a  = -G * M / (2.0 * epsilon);
        double rp = a * (1.0 - ecc);
        double ra = a * (1.0 + ecc);

        if (rp <= state.planet.radius) return;

        double rad = Math.toRadians(angleDeg);
        double vx  = -v0 * Math.sin(rad);
        double vy  = -v0 * Math.cos(rad);

        double hSigned = r0 * vy;

        double ex = (vy * hSigned) / (G * M) - 1.0;
        double ey = (-vx * hSigned) / (G * M);

        double periapsisAngle = Math.atan2(ey, ex);

        double pWorldX = rp * Math.cos(periapsisAngle);
        double pWorldY = rp * Math.sin(periapsisAngle);

        double aWorldX = ra * Math.cos(periapsisAngle + Math.PI);
        double aWorldY = ra * Math.sin(periapsisAngle + Math.PI);

        float pSX = worldToScreenX(pWorldX);
        float pSY = worldToScreenY(pWorldY);
        float aSX = worldToScreenX(aWorldX);
        float aSY = worldToScreenY(aWorldY);

        drawApside(canvas, pSX, pSY, "近", 0xFFFFCC00);

        drawApside(canvas, aSX, aSY, "遠", 0xFF00CCFF);

        paintText.setColor(Color.WHITE);
        paintText.setTextSize(28f);
    }

    private void drawApside(Canvas canvas, float sx, float sy, String label, int color) {
        int w = getWidth();
        int h = getHeight();
        int margin = 32;

        boolean inScreen = sx >= 0 && sx <= w && sy >= 0 && sy <= h;

        paintApsides.setColor(color);
        paintText.setColor(color);
        paintText.setTextSize(22f);

        if (inScreen) {
            canvas.drawCircle(sx, sy, 7f, paintApsides);
            canvas.drawText(label, sx + 10, sy - 10, paintText);
        } else {
            float cx = w / 2f;
            float cy = h / 2f;
            float dx = sx - cx;
            float dy = sy - cy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float ndx = dx / len;
            float ndy = dy / len;

            float t = Float.MAX_VALUE;
            if (ndx > 0) t = Math.min(t, (w - margin - cx) / ndx);
            else if (ndx < 0) t = Math.min(t, (margin - cx) / ndx);
            if (ndy > 0) t = Math.min(t, (h - margin - cy) / ndy);
            else if (ndy < 0) t = Math.min(t, (margin - cy) / ndy);

            float arrowX = cx + ndx * t;
            float arrowY = cy + ndy * t;

            float size = 18f;
            float perpX = -ndy;
            float perpY =  ndx;
            android.graphics.Path arrow = new android.graphics.Path();
            arrow.moveTo(arrowX + ndx * size, arrowY + ndy * size);
            arrow.lineTo(arrowX - ndx * size * 0.5f + perpX * size * 0.7f,
                    arrowY - ndy * size * 0.5f + perpY * size * 0.7f);
            arrow.lineTo(arrowX - ndx * size * 0.5f - perpX * size * 0.7f,
                    arrowY - ndy * size * 0.5f - perpY * size * 0.7f);
            arrow.close();
            canvas.drawPath(arrow, paintApsides);

            canvas.drawText(label, arrowX + ndx * size + 6, arrowY + ndy * size + 6, paintText);
        }
    }
    private void drawOrbitPreview(Canvas canvas) {

        if (state.running || state.crashed || !state.trail.isEmpty() || state.elapsedTime > 0) return;

        double G  = SimulationState.G;
        double M  = state.planet.mass;
        double r0 = state.initialDistance;
        double v0 = state.initialVelocity;
        double angleDeg = state.initialAngleDeg;

        if (v0 <= 0 || Math.abs(Math.cos(Math.toRadians(angleDeg))) < 0.01) {
            float cx = worldToScreenX(0);
            float cy = worldToScreenY(0);
            float r  = worldToScreenLen(r0);
            if (r >= 5f) canvas.drawCircle(cx, cy, r, paintOrbit);
            return;
        }

        double epsilon = 0.5 * v0 * v0 - G * M / r0;

        if (epsilon >= 0) {
            float cx = worldToScreenX(0);
            float cy = worldToScreenY(0);
            float r  = worldToScreenLen(r0);
            if (r >= 5f) canvas.drawCircle(cx, cy, r, paintOrbit);
            return;
        }

        double rad = Math.toRadians(angleDeg);
        double vx  = -v0 * Math.sin(rad);
        double vy  = -v0 * Math.cos(rad);
        double hSigned = r0 * vy;
        double h   = Math.abs(hSigned);
        if (h < 1e3) return;

        double eccSq = 1.0 + (2.0 * epsilon * h * h) / (G * M * G * M);
        if (eccSq < 0) return;
        double ecc = Math.sqrt(eccSq);
        double a   = -G * M / (2.0 * epsilon);
        double b   = a * Math.sqrt(Math.max(0, 1.0 - ecc * ecc));

        double ex = (vy * hSigned) / (G * M) - 1.0;
        double ey = (-vx * hSigned) / (G * M);
        double periapsisAngle = Math.atan2(ey, ex);

        double centerX = -a * ecc * Math.cos(periapsisAngle);
        double centerY = -a * ecc * Math.sin(periapsisAngle);

        int steps = 200;
        float[] pts = new float[steps * 4];
        for (int i = 0; i < steps; i++) {
            double t1 = 2.0 * Math.PI * i / steps;
            double t2 = 2.0 * Math.PI * (i + 1) / steps;

            double x1 = a * Math.cos(t1);
            double y1 = b * Math.sin(t1);
            double x2 = a * Math.cos(t2);
            double y2 = b * Math.sin(t2);

            double cos = Math.cos(periapsisAngle);
            double sin = Math.sin(periapsisAngle);
            double rx1 = x1 * cos - y1 * sin + centerX;
            double ry1 = x1 * sin + y1 * cos + centerY;
            double rx2 = x2 * cos - y2 * sin + centerX;
            double ry2 = x2 * sin + y2 * cos + centerY;

            pts[i * 4]     = worldToScreenX(rx1);
            pts[i * 4 + 1] = worldToScreenY(ry1);
            pts[i * 4 + 2] = worldToScreenX(rx2);
            pts[i * 4 + 3] = worldToScreenY(ry2);
        }
        canvas.drawLines(pts, paintOrbit);
    }

    private void drawPlanet(Canvas canvas) {
        float cx = worldToScreenX(0);
        float cy = worldToScreenY(0);
        float r  = Math.max(12f, worldToScreenLen(state.planet.radius));

        paintPlanet.setColor(state.planet.color);
        canvas.drawCircle(cx, cy, r, paintPlanet);

        paintText.setTextSize(28f);
        paintText.setColor(Color.WHITE);
        canvas.drawText(state.planet.name, cx + r + 8, cy + 10, paintText);
    }

    private void drawTrail(Canvas canvas) {
        if (!showTrail) return;
        List<double[]> trail = state.trail;
        int size = trail.size();
        if (size < 2) return;

        double maxSpeed = 1.0;
        for (double[] pt : trail) {
            if (pt[2] > maxSpeed) maxSpeed = pt[2];
        }

        for (int i = 1; i < size; i++) {
            double[] prev = trail.get(i - 1);
            double[] curr = trail.get(i);

            float alpha = (float) i / size;
            int a = (int)(alpha * 220);

            float t = (float)(curr[2] / maxSpeed);
            int color = speedColor(t, a);
            paintTrail.setColor(color);

            canvas.drawLine(
                    worldToScreenX(prev[0]), worldToScreenY(prev[1]),
                    worldToScreenX(curr[0]), worldToScreenY(curr[1]),
                    paintTrail
            );
        }
        paintTrail.setColor(0x99FFFFFF); // 還原
    }

    private int speedColor(float t, int alpha) {
        int r, g, b;
        if (t < 0.5f) {
            float s = t * 2f;
            r = (int)(0x44 + s * (0xFF - 0x44));
            g = (int)(0x88 + s * (0xFF - 0x88));
            b = 0xFF;
        } else {
            float s = (t - 0.5f) * 2f;
            r = 0xFF;
            g = (int)(0xFF - s * (0xFF - 0x88));
            b = (int)(0xFF - s * 0xFF);
        }
        return Color.argb(alpha, r, g, b);
    }

    private void drawObject(Canvas canvas) {
        float sx = worldToScreenX(state.posX);
        float sy = worldToScreenY(state.posY);

        if (state.crashed) {
            crashFlashCounter++;
            if ((crashFlashCounter / 6) % 2 == 0) {
                paintObject.setColor(0xFFFF4444);
                canvas.drawCircle(sx, sy, 10f, paintObject);
            }

            postInvalidateDelayed(16);
            return;
        }

        paintObject.setColor(Color.WHITE);
        canvas.drawCircle(sx, sy, 8f, paintObject);
    }

    private void drawCrashedMessage(Canvas canvas) {
        if (!state.crashed) return;
        paintText.setTextSize(36f);
        paintText.setColor(0xFFFF4444);
        String msg = "已墜入 " + state.planet.name;
        float tw = paintText.measureText(msg);

        float sx = worldToScreenX(state.posX);
        float sy = worldToScreenY(state.posY);
        canvas.drawText(msg, sx - tw / 2f, sy + 148f,paintText);
        paintText.setColor(Color.WHITE);
    }
}