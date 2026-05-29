package com.example.planetsimulate;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private SimulationState state;
    private SimulationView simulationView;
    private GameLoop gameLoop;

    private Spinner spinnerPlanet;
    private SeekBar seekDistance, seekVelocity, seekAngle;
    private TextView tvDistance, tvVelocity, tvAngle;
    private TextView tvInfoDistance, tvInfoSpeed, tvInfoTime, tvInfoEnergy;
    private TextView tvIndicatorCircular, tvIndicatorEscape;
    private Button btnStart, btnReset, btnOrbitalData, btnPreset;
    private Switch switchProMode, switchTrail;
    private LinearLayout rowPresetLock;
    private TextView tvPresetLockMsg;
    private boolean presetLocked = false;
    private EditText etSpeedMultiplier, etDistance, etVelocity;
    private TextView tvDistanceRange, tvVelocityRange;
    private LinearLayout rowDistanceSeek, rowDistanceInput, rowVelocitySeek, rowVelocityInput;

    private int timeMultiplier = 1;
    private boolean trailVisible = true;
    private boolean proMode = false;

    private static final double DIST_MIN_FACTOR = 1.5;
    private static final double DIST_MAX_FACTOR = 200.0;
    private static final double VEL_MAX_FACTOR  = 2.5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        state = new SimulationState();

        bindViews();
        simulationView.setState(state);
        gameLoop = new GameLoop(state, simulationView, this::updateInfoPanel);

        setupPlanetSpinner();
        setupDistanceSeekBar();
        setupVelocitySeekBar();
        setupAngleSeekBar();
        setupButtons();
        updateInfoPanel();

        if (TutorialOverlay.shouldShow(this)) {
            new TutorialOverlay(this).show();
        }
    }

    private void bindViews() {
        simulationView      = findViewById(R.id.simulationView);
        spinnerPlanet       = findViewById(R.id.spinnerPlanet);
        seekDistance        = findViewById(R.id.seekDistance);
        seekVelocity        = findViewById(R.id.seekVelocity);
        seekAngle           = findViewById(R.id.seekAngle);
        tvDistance          = findViewById(R.id.tvDistance);
        tvVelocity          = findViewById(R.id.tvVelocity);
        tvAngle             = findViewById(R.id.tvAngle);
        tvInfoDistance      = findViewById(R.id.tvInfoDistance);
        tvInfoSpeed         = findViewById(R.id.tvInfoSpeed);
        tvInfoTime          = findViewById(R.id.tvInfoTime);
        tvInfoEnergy        = findViewById(R.id.tvInfoEnergy);
        btnStart            = findViewById(R.id.btnStart);
        btnReset            = findViewById(R.id.btnReset);
        btnPreset           = findViewById(R.id.btnPreset);
        switchTrail         = findViewById(R.id.btnToggleTrail);
        btnOrbitalData      = findViewById(R.id.btnOrbitalData);
        switchProMode       = findViewById(R.id.switchProMode);
        etSpeedMultiplier   = findViewById(R.id.etSpeedMultiplier);
        etDistance          = findViewById(R.id.etDistance);
        etVelocity          = findViewById(R.id.etVelocity);
        rowPresetLock       = findViewById(R.id.rowPresetLock);
        tvPresetLockMsg     = findViewById(R.id.tvPresetLockMsg);
        tvDistanceRange     = findViewById(R.id.tvDistanceRange);
        tvVelocityRange     = findViewById(R.id.tvVelocityRange);
        rowDistanceSeek     = findViewById(R.id.rowDistanceSeek);
        rowDistanceInput    = findViewById(R.id.rowDistanceInput);
        rowVelocitySeek     = findViewById(R.id.rowVelocitySeek);
        rowVelocityInput    = findViewById(R.id.rowVelocityInput);
        tvIndicatorCircular = findViewById(R.id.tvIndicatorCircular);
        tvIndicatorEscape   = findViewById(R.id.tvIndicatorEscape);
    }

    private void setupPlanetSpinner() {
        String[] names = new String[PlanetData.ALL.length];
        for (int i = 0; i < PlanetData.ALL.length; i++) names[i] = PlanetData.ALL[i].name;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, names) {
            @Override
            public android.view.View getView(int position, android.view.View convertView,
                                             android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                ((android.widget.TextView) view).setTextColor(0xFFCCCCEE);
                ((android.widget.TextView) view).setTextSize(14f);
                return view;
            }
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView,
                                                     android.view.ViewGroup parent) {
                android.view.View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(0xFF1A1A2E);
                ((android.widget.TextView) view).setTextColor(0xFFCCCCEE);
                ((android.widget.TextView) view).setPadding(32, 24, 32, 24);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlanet.setAdapter(adapter);
        spinnerPlanet.setSelection(2);

        spinnerPlanet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                state.planet = PlanetData.ALL[pos];
                double factor = DIST_MIN_FACTOR + (seekDistance.getProgress() / 100.0)
                        * (DIST_MAX_FACTOR - DIST_MIN_FACTOR);
                state.initialDistance = state.planet.radius * factor;
                state.reset();
                simulationView.setState(state);
                updateDistanceLabel();
                updateInfoPanel();
                double vCirc = PhysicsEngine.circularOrbitVelocity(state.planet, state.initialDistance);
                updateIndicators(state.initialVelocity, vCirc);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDistanceSeekBar() {
        seekDistance.setProgress(10);
        updateDistanceFromSeek(10);

        seekDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateDistanceFromSeek(progress);
                    state.reset();
                    simulationView.setState(state);
                    updateInfoPanel();
                    double vCirc = PhysicsEngine.circularOrbitVelocity(state.planet, state.initialDistance);
                    updateIndicators(state.initialVelocity, vCirc);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                gameLoop.stop();
                state.running = false;
                btnStart.setText("開始");
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateDistanceFromSeek(int progress) {
        double factor = DIST_MIN_FACTOR + (progress / 100.0) * (DIST_MAX_FACTOR - DIST_MIN_FACTOR);
        state.initialDistance = state.planet.radius * factor;
        updateDistanceLabel();
    }

    private void updateDistanceLabel() {
        double km = state.initialDistance / 1000.0;
        if (km >= 1_000_000) tvDistance.setText(String.format("%.2f M km", km / 1_000_000));
        else                  tvDistance.setText(String.format("%.0f km", km));
    }

    private void setupVelocitySeekBar() {
        seekVelocity.setProgress(0);
        tvVelocity.setText("0 m/s");

        seekVelocity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    double vCircular = PhysicsEngine.circularOrbitVelocity(
                            state.planet, state.initialDistance);
                    state.initialVelocity = (progress / 100.0) * vCircular * VEL_MAX_FACTOR;
                    state.reset();
                    simulationView.invalidate();
                    updateVelocityLabel(vCircular);
                    updateInfoPanel();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                gameLoop.stop();
                state.running = false;
                btnStart.setText("開始");
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateVelocityLabel(double vCircular) {
        double v = state.initialVelocity;
        tvVelocity.setText(String.format("%.0f m/s", v));
        updateIndicators(v, vCircular);
    }

    private void setupAngleSeekBar() {
        seekAngle.setProgress(0);
        tvAngle.setText("0°  （切線）");

        seekAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    state.initialAngleDeg = progress;
                    state.reset();
                    simulationView.invalidate();
                    updateAngleLabel(progress);
                    updateInfoPanel();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                gameLoop.stop();
                state.running = false;
                btnStart.setText("開始");
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateAngleLabel(int deg) {
        String hint;
        if (deg == 0)        hint = "（切線）";
        else if (deg == 90)  hint = "（朝行星）";
        else if (deg == 180) hint = "（反切線）";
        else if (deg < 45)   hint = "（偏切線）";
        else if (deg < 135)  hint = "（偏徑向）";
        else                 hint = "（偏反切線）";
        tvAngle.setText(deg + "°  " + hint);
    }

    private void setupButtons() {
        btnStart.setOnClickListener(v -> {
            if (state.crashed) {
                state.reset();
                simulationView.invalidate();
                updateInfoPanel();
            }
            if (state.running) {
                state.running = false;
                gameLoop.stop();
                btnStart.setText("開始");
            } else {
                state.running = true;
                gameLoop.start();
                btnStart.setText("暫停");
            }
        });

        btnReset.setOnClickListener(v -> {
            gameLoop.stop();
            state.initialAngleDeg = 0;
            seekAngle.setProgress(0);
            updateAngleLabel(0);
            state.reset();
            btnStart.setText("開始");
            simulationView.invalidate();
            updateInfoPanel();
            setParamLocked(false, null);
        });

        // 數據頁面
        btnOrbitalData.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrbitalDataActivity.class);
            intent.putExtra("planetMass",   state.planet.mass);
            intent.putExtra("planetRadius", state.planet.radius);
            intent.putExtra("planetName",   state.planet.name);
            intent.putExtra("initDistance", state.initialDistance);
            intent.putExtra("initVelocity", state.initialVelocity);
            intent.putExtra("initAngle",    state.initialAngleDeg);
            intent.putExtra("simTime",      state.elapsedTime);
            intent.putExtra("currentSpeed", state.speed());
            intent.putExtra("currentDist",  state.distanceFromPlanet());
            startActivity(intent);
        });

        btnPreset.setOnClickListener(v -> {
            String[] items = {
                    "🌕  月球（27.3 天週期）",
                    "🛸  國際空間站（92 分鐘週期）",
                    "📡  地球同步衛星（24 小時週期）",
                    "🔭  哈伯太空望遠鏡（95 分鐘週期）",
                    "💫  橢圓軌道示範（離心率 0.7）",
                    "🚀  接近逃逸速度示範",
            };
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
                    .setTitle("選擇情境")
                    .setItems(items, (dialog, which) -> applyPreset(which))
                    .show();
        });

        switchTrail.setOnCheckedChangeListener((buttonView, isChecked) -> {
            trailVisible = isChecked;
            simulationView.setShowTrail(trailVisible);
        });

        switchProMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            proMode = isChecked;
            rowDistanceSeek.setVisibility(proMode ? View.GONE : View.VISIBLE);
            rowDistanceInput.setVisibility(proMode ? View.VISIBLE : View.GONE);
            rowVelocitySeek.setVisibility(proMode ? View.GONE : View.VISIBLE);
            rowVelocityInput.setVisibility(proMode ? View.VISIBLE : View.GONE);

            if (proMode) {
                double distKm = state.initialDistance / 1000.0;
                etDistance.setText(String.format("%.0f", distKm));
                etVelocity.setText(String.format("%.0f", state.initialVelocity));
                updateProModeRangeHints();
            }
        });

        etDistance.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                applyProDistance();
                return true;
            }
            return false;
        });

        etVelocity.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                applyProVelocity();
                return true;
            }
            return false;
        });

        etSpeedMultiplier.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                applySpeedMultiplier();
                return true;
            }
            return false;
        });

        simulationView.setOnClickListener(v -> applySpeedMultiplier());
    }

    private void applySpeedMultiplier() {
        String raw = etSpeedMultiplier.getText().toString().trim();
        int value = 1;
        if (!raw.isEmpty()) {
            try {
                value = Integer.parseInt(raw);
                if (value < 1) value = 1;
                if (value > 100000) value = 100000;
            } catch (NumberFormatException e) {
                value = 1;
            }
        }
        timeMultiplier = value;
        gameLoop.setTimeMultiplier(timeMultiplier);
        etSpeedMultiplier.setText(String.valueOf(timeMultiplier));

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSpeedMultiplier.getWindowToken(), 0);
    }

    private void applyProDistance() {
        double minKm = state.planet.radius * DIST_MIN_FACTOR / 1000.0;
        double maxKm = state.planet.radius * DIST_MAX_FACTOR / 1000.0;
        try {
            double km = Double.parseDouble(etDistance.getText().toString().trim());
            km = Math.max(minKm, Math.min(km, maxKm));
            state.initialDistance = km * 1000.0;
            etDistance.setText(String.format("%.0f", km));
        } catch (NumberFormatException e) {
            etDistance.setText(String.format("%.0f", state.initialDistance / 1000.0));
        }
        gameLoop.stop();
        state.running = false;
        btnStart.setText("開始");
        state.reset();
        simulationView.setState(state);
        updateProModeRangeHints();
        updateInfoPanel();
        double vCirc = PhysicsEngine.circularOrbitVelocity(state.planet, state.initialDistance);
        updateIndicators(state.initialVelocity, vCirc);
        hideKeyboard();
    }

    private void applyProVelocity() {
        double vCirc   = PhysicsEngine.circularOrbitVelocity(state.planet, state.initialDistance);
        double maxVel  = vCirc * VEL_MAX_FACTOR;
        try {
            double v = Double.parseDouble(etVelocity.getText().toString().trim());
            v = Math.max(0, Math.min(v, maxVel));
            state.initialVelocity = v;
            etVelocity.setText(String.format("%.0f", v));
        } catch (NumberFormatException e) {
            etVelocity.setText(String.format("%.0f", state.initialVelocity));
        }
        gameLoop.stop();
        state.running = false;
        btnStart.setText("開始");
        state.reset();
        simulationView.invalidate();
        updateInfoPanel();
        updateIndicators(state.initialVelocity, vCirc);
        hideKeyboard();
    }

    private void updateProModeRangeHints() {
        double minKm = state.planet.radius * DIST_MIN_FACTOR / 1000.0;
        double maxKm = state.planet.radius * DIST_MAX_FACTOR / 1000.0;
        tvDistanceRange.setText(String.format("%.0f~%.0f km", minKm, maxKm));

        double vCirc  = PhysicsEngine.circularOrbitVelocity(state.planet, state.initialDistance);
        double maxVel = vCirc * VEL_MAX_FACTOR;
        tvVelocityRange.setText(String.format("0~%.0f m/s", maxVel));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            View focus = getCurrentFocus();
            if (focus != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    public void updateInfoPanel() {
        double distKm = state.distanceFromPlanet() / 1000.0;
        if (distKm >= 1_000_000) tvInfoDistance.setText(String.format("%.3f M km", distKm / 1_000_000));
        else                      tvInfoDistance.setText(String.format("%.0f km", distKm));
        tvInfoSpeed.setText(String.format("%.1f m/s", state.speed()));
        tvInfoTime.setText(formatTime(state.elapsedTime));

        double e = state.specificEnergy() / 1e6;
        tvInfoEnergy.setText(String.format("%.2f MJ/kg", e));
    }

    private void updateIndicators(double v, double vCirc) {
        double vEscape  = vCirc * Math.sqrt(2.0);
        boolean nearCircular = v > 0 && Math.abs(v - vCirc) / vCirc < 0.05;
        boolean overEscape   = v > 0 && v >= vEscape * 0.95;
        tvIndicatorCircular.setTextColor(nearCircular ? 0xFF44FF44 : 0x3344FF44);
        tvIndicatorEscape.setTextColor(overEscape     ? 0xFFFF4444 : 0x33FF4444);
    }

    private String formatTime(double seconds) {
        if (seconds < 60)    return String.format("%.1f s", seconds);
        if (seconds < 3600)  return String.format("%.1f min", seconds / 60);
        if (seconds < 86400) return String.format("%.1f h", seconds / 3600);
        return String.format("%.1f 天", seconds / 86400);
    }

    private void applyPreset(int index) {
        gameLoop.stop();
        state.running = false;
        btnStart.setText("開始");

        // 情境資料：{行星索引, 距地心距離(km), 初速(m/s), 角度(°)}
        double[][] presets = {
                {2, 384400,  1022, 0},   // 月球
                {2,   6779,  7668, 0},   // 國際空間站（地球半徑 6371 + 408 km）
                {2,  35786,  3075, 0},   // 地球同步衛星
                {2,   6918,  7590, 0},   // 哈伯太空望遠鏡（6371 + 547 km）
                {2,  50000,  2000, 45},  // 橢圓軌道示範
                {2, 100000,  3900, 0},   // 接近逃逸速度
        };

        if (index < 0 || index >= presets.length) return;
        double[] p = presets[index];

        int planetIdx = (int) p[0];
        spinnerPlanet.setSelection(planetIdx);
        state.planet        = PlanetData.ALL[planetIdx];
        state.initialDistance = p[1] * 1000.0;
        state.initialVelocity = p[2];
        state.initialAngleDeg = p[3];

        double factor = state.initialDistance / state.planet.radius;
        boolean inSliderRange = factor >= DIST_MIN_FACTOR && factor <= DIST_MAX_FACTOR;

        if (inSliderRange) {
            double progress = (factor - DIST_MIN_FACTOR) / (DIST_MAX_FACTOR - DIST_MIN_FACTOR) * 100.0;
            seekDistance.setProgress((int) progress);

            if (proMode) {
                proMode = false;
                switchProMode.setChecked(false);
            }
        } else {

            if (!proMode) {
                proMode = true;
                switchProMode.setChecked(true);
            }
            etDistance.setText(String.format("%.0f", state.initialDistance / 1000.0));
        }

        double vCirc = PhysicsEngine.circularOrbitVelocity(state.planet, state.initialDistance);
        double vMax  = vCirc * VEL_MAX_FACTOR;
        int velProgress = (int) Math.max(0, Math.min(100, state.initialVelocity / vMax * 100));
        seekVelocity.setProgress(velProgress);
        if (proMode) etVelocity.setText(String.format("%.0f", state.initialVelocity));

        seekAngle.setProgress((int) state.initialAngleDeg);
        updateAngleLabel((int) state.initialAngleDeg);
        updateDistanceLabel();
        updateVelocityLabel(vCirc);
        if (proMode) updateProModeRangeHints();

        state.reset();
        simulationView.setState(state);
        updateInfoPanel();

        String[] presetNames = {
                "月球", "國際空間站 (ISS)", "地球同步衛星", "哈伯太空望遠鏡", "橢圓軌道示範", "逃逸速度示範"
        };
        setParamLocked(true, presetNames[index]);
    }

    private void setParamLocked(boolean locked, String presetName) {
        presetLocked = locked;

        seekDistance.setEnabled(!locked);
        seekVelocity.setEnabled(!locked);
        seekAngle.setEnabled(!locked);

        etDistance.setEnabled(!locked);
        etVelocity.setEnabled(!locked);

        spinnerPlanet.setEnabled(!locked);

        switchProMode.setEnabled(!locked);

        if (locked && presetName != null) {
            tvPresetLockMsg.setText("情境：" + presetName + "　　調整參數前請先重置");
            rowPresetLock.setVisibility(View.VISIBLE);
        } else {
            rowPresetLock.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameLoop.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state.running) gameLoop.start();
    }
}