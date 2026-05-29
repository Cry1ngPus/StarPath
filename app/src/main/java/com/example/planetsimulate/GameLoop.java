package com.example.planetsimulate;

import android.view.Choreographer;

public class GameLoop implements Choreographer.FrameCallback {

    public interface OnFrameListener {
        void onFrame();
    }

    private final PhysicsEngine engine = new PhysicsEngine();
    private final SimulationState state;
    private final SimulationView view;
    private final OnFrameListener listener;

    private boolean running = false;

    public GameLoop(SimulationState state, SimulationView view, OnFrameListener listener) {
        this.state    = state;
        this.view     = view;
        this.listener = listener;
    }

    public void start() {
        if (running) return;
        running = true;
        Choreographer.getInstance().postFrameCallback(this);
    }

    public void stop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;

        engine.step(state, timeMultiplier);

        listener.onFrame();

        view.invalidate();

        if (state.running) {
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            running = false;
        }
    }
    private int timeMultiplier = 1;

    public void setTimeMultiplier(int multiplier) {
        this.timeMultiplier = multiplier;
    }
}
