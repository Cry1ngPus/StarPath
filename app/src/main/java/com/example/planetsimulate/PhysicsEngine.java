package com.example.planetsimulate;

public class PhysicsEngine {

    private static final double BASE_DT = 1.0;
    private static final int SUBSTEPS = 20;

    public boolean step(SimulationState state, int timeMultiplier) {
        if (!state.running || state.crashed) return false;

        double dt = BASE_DT * timeMultiplier;
        double subDt = dt / SUBSTEPS;

        for (int i = 0; i < SUBSTEPS; i++) {
            double[] acc = computeAcceleration(state);

            state.posX += state.velX * subDt + 0.5 * acc[0] * subDt * subDt;
            state.posY += state.velY * subDt + 0.5 * acc[1] * subDt * subDt;

            double[] accNew = computeAcceleration(state);

            state.velX += 0.5 * (acc[0] + accNew[0]) * subDt;
            state.velY += 0.5 * (acc[1] + accNew[1]) * subDt;

            state.elapsedTime += subDt;

            if (state.distanceFromPlanet() <= state.planet.radius) {
                state.crashed = true;
                state.running = false;
                return false;
            }
        }

        state.recordTrail();
        return true;
    }

    private double[] computeAcceleration(SimulationState state) {
        double r = state.distanceFromPlanet();

        if (r < 1.0) r = 1.0;

        double a = SimulationState.G * state.planet.mass / (r * r);

        double ax = -a * state.posX / r;
        double ay = -a * state.posY / r;

        return new double[]{ax, ay};
    }

    public static double circularOrbitVelocity(PlanetData planet, double distance) {
        return Math.sqrt(SimulationState.G * planet.mass / distance);
    }
}
