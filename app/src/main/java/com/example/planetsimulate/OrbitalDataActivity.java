package com.example.planetsimulate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrbitalDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orbital_data);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnRestartTutorial).setOnClickListener(v -> {
            getSharedPreferences("tutorial", MODE_PRIVATE)
                    .edit().remove("done").apply();
            android.widget.Toast.makeText(this, "下次進入主頁時會顯示教學", android.widget.Toast.LENGTH_SHORT).show();
        });

        Bundle b = getIntent().getExtras();
        if (b == null) return;

        double planetMass   = b.getDouble("planetMass",   0);
        double planetRadius = b.getDouble("planetRadius", 0);
        String planetName   = b.getString("planetName",   "未知");
        double initDistance = b.getDouble("initDistance", 0);
        double initVelocity = b.getDouble("initVelocity", 0);
        double initAngle    = b.getDouble("initAngle",    0);
        double simTime      = b.getDouble("simTime",      0);
        double currentSpeed = b.getDouble("currentSpeed", 0);
        double currentDist  = b.getDouble("currentDist",  0);

        populateData(planetName, planetMass, planetRadius,
                initDistance, initVelocity, initAngle, simTime, currentSpeed, currentDist);
    }

    private void populateData(String planetName, double M, double planetR,
                              double r0, double v0, double angleDeg, double simTime,
                              double currentSpeed, double currentDist) {
        final double G = SimulationState.G;

        set(R.id.tvPlanetName,   planetName);
        set(R.id.tvPlanetMass,   String.format("%.4e kg", M));
        set(R.id.tvPlanetRadius, String.format("%.0f km", planetR / 1000.0));
        set(R.id.tvSurfaceG,     String.format("%.2f m/s²", G * M / (planetR * planetR)));

        double epsilon = 0.5 * v0 * v0 - G * M / r0;

        double h = r0 * v0 * Math.cos(Math.toRadians(angleDeg));

        double ecc;
        if (v0 == 0 || Math.abs(h) < 1e3) {

            set(R.id.tvEccentricity,  "N/A  （純徑向，無軌道）");
            set(R.id.tvSemiMajorAxis, "N/A");
            set(R.id.tvPeriod,        "N/A");
            set(R.id.tvPeriapsis,     "N/A");
            set(R.id.tvApoapsis,      "N/A");
            ecc = -1;
        } else {
            double eccSq = 1.0 + (2.0 * epsilon * h * h) / (G * M * G * M);
            ecc = eccSq < 0 ? 0 : Math.sqrt(eccSq);
        }

        if (ecc >= 0) {
            boolean isEscape = epsilon >= 0;
            String eccLabel = isEscape ? "  （雙曲線，逃逸）"
                    : ecc < 0.01 ? "  （近似正圓）" : "  （橢圓）";
            set(R.id.tvEccentricity, String.format("%.4f", ecc) + eccLabel);

            if (isEscape) {
                set(R.id.tvSemiMajorAxis, "N/A");
                set(R.id.tvPeriod,        "N/A");
                set(R.id.tvPeriapsis,     "N/A");
                set(R.id.tvApoapsis,      "N/A");
            } else {
                double a  = -G * M / (2.0 * epsilon);
                double rp = a * (1.0 - ecc);
                double ra = a * (1.0 + ecc);
                double T  = 2.0 * Math.PI * Math.sqrt(a * a * a / (G * M));
                set(R.id.tvSemiMajorAxis, fmtDist(a));
                set(R.id.tvPeriod,        fmtTime(T));
                set(R.id.tvPeriapsis,     fmtDist(rp));
                set(R.id.tvApoapsis,      fmtDist(ra));
            }
        }

        double vCirc   = Math.sqrt(G * M / r0);
        double vEscape = Math.sqrt(2.0 * G * M / r0);
        set(R.id.tvInitVelocity, String.format("%.1f m/s", v0));
        set(R.id.tvCircularV,    String.format("%.1f m/s", vCirc));
        set(R.id.tvEscapeV,      String.format("%.1f m/s", vEscape));
        set(R.id.tvVRatio,       String.format("%.3f  （v / v圓軌）", v0 / vCirc));

        double accel = G * M / (currentDist * currentDist);
        set(R.id.tvCurrentDist,  fmtDist(currentDist));
        set(R.id.tvCurrentSpeed, String.format("%.2f m/s", currentSpeed));
        set(R.id.tvCurrentAccel, String.format("%.4f m/s²", accel));
        set(R.id.tvSimTime,      fmtTime(simTime));

        double ke     = 0.5 * currentSpeed * currentSpeed;
        double pe     = -G * M / currentDist;
        double totalE = ke + pe;
        double vcNow  = Math.sqrt(G * M / currentDist);
        double veNow  = Math.sqrt(2.0 * G * M / currentDist);
        set(R.id.tvKineticE,     String.format("%.4f MJ/kg", ke / 1e6));
        set(R.id.tvPotentialE,   String.format("%.4f MJ/kg", pe / 1e6));
        set(R.id.tvTotalE,       String.format("%.4f MJ/kg", totalE / 1e6));
        set(R.id.tvCircularVNow, String.format("%.1f m/s", vcNow));
        set(R.id.tvEscapeVNow,   String.format("%.1f m/s", veNow));
    }

    private void set(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text != null ? text : "--");
    }

    private String fmtDist(double meters) {
        double km = meters / 1000.0;
        if (km >= 1_000_000) return String.format("%.4f M km", km / 1_000_000);
        if (km >= 1_000)     return String.format("%.1f km", km);
        return String.format("%.2f m", meters);
    }

    private String fmtTime(double seconds) {
        if (seconds < 60)          return String.format("%.2f s", seconds);
        if (seconds < 3600)        return String.format("%.2f min", seconds / 60);
        if (seconds < 86400)       return String.format("%.4f h", seconds / 3600);
        if (seconds < 86400 * 365) return String.format("%.4f 天", seconds / 86400);
        return String.format("%.4f 年", seconds / (86400 * 365.25));
    }
}