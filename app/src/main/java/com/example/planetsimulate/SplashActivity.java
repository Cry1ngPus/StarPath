package com.example.planetsimulate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int TOTAL_DURATION = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View satellite   = findViewById(R.id.satellite);
        View orbitRing   = findViewById(R.id.orbitRing);
        View planet      = findViewById(R.id.planet);
        TextView tvTitle    = findViewById(R.id.tvTitle);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        ProgressBar progress = findViewById(R.id.progressBar);

        View orbitContainer = findViewById(R.id.orbitContainer);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(orbitContainer, "rotation", 0f, 360f);
        rotate.setDuration(2000);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.start();

        ObjectAnimator ringFadeIn = ObjectAnimator.ofFloat(orbitRing, "alpha", 0f, 1f);
        ringFadeIn.setDuration(600);
        ringFadeIn.setStartDelay(200);

        ObjectAnimator planetScaleX = ObjectAnimator.ofFloat(planet, "scaleX", 0f, 1f);
        ObjectAnimator planetScaleY = ObjectAnimator.ofFloat(planet, "scaleY", 0f, 1f);
        ObjectAnimator planetFade   = ObjectAnimator.ofFloat(planet, "alpha", 0f, 1f);
        AnimatorSet planetAnim = new AnimatorSet();
        planetAnim.playTogether(planetScaleX, planetScaleY, planetFade);
        planetAnim.setDuration(500);
        planetAnim.setStartDelay(300);
        planetAnim.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator satFade = ObjectAnimator.ofFloat(satellite, "alpha", 0f, 1f);
        satFade.setDuration(400);
        satFade.setStartDelay(600);

        tvTitle.setTranslationY(20f);
        ObjectAnimator titleFade = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f);
        ObjectAnimator titleMove = ObjectAnimator.ofFloat(tvTitle, "translationY", 20f, 0f);
        AnimatorSet titleAnim = new AnimatorSet();
        titleAnim.playTogether(titleFade, titleMove);
        titleAnim.setDuration(500);
        titleAnim.setStartDelay(700);
        titleAnim.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator subtitleFade = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f);
        subtitleFade.setDuration(500);
        subtitleFade.setStartDelay(900);

        ObjectAnimator progressFade = ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f);
        progressFade.setDuration(300);
        progressFade.setStartDelay(1000);

        ValueAnimator progressAnim = ValueAnimator.ofInt(0, 100);
        progressAnim.setDuration(1000);
        progressAnim.setStartDelay(1100);
        progressAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnim.addUpdateListener(a -> progress.setProgress((int) a.getAnimatedValue()));

        AnimatorSet all = new AnimatorSet();
        all.playTogether(ringFadeIn, planetAnim, satFade,
                titleAnim, subtitleFade, progressFade, progressAnim);
        all.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });

        all.setDuration(TOTAL_DURATION);
        all.start();
    }

    @Override
    public void onBackPressed() {
    }
}
