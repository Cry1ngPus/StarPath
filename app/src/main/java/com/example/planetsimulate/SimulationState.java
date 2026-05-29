package com.example.planetsimulate;

import java.util.ArrayList;
import java.util.List;

public class SimulationState {
    public static final double G = 6.674e-11;
    private static final int MAX_TRAIL_POINTS = 600;
    public PlanetData planet;
    public double initialDistance;
    public double initialVelocity;
    public double initialAngleDeg = 0.0;
    public double posX;
    public double posY;
    public double velX;
    public double velY;
    public double elapsedTime;
    public boolean running;
    public boolean crashed;
    public final List<double[]> trail = new ArrayList<>();

    public SimulationState() {
        planet = PlanetData.ALL[2];
        initialDistance = planet.radius * 3;
        initialVelocity = 0;
        reset();
    }

    public void reset() {
        posX = initialDistance;
        posY = 0;

        double rad = Math.toRadians(initialAngleDeg);
        double tangential = Math.cos(rad);
        double radial     = Math.sin(rad);

        velX = -initialVelocity * radial;
        velY = -initialVelocity * tangential;

        elapsedTime = 0;
        running = false;
        crashed = false;
        trail.clear();
    }

    public void recordTrail() {
        trail.add(new double[]{posX, posY, speed()});
        if (trail.size() > MAX_TRAIL_POINTS) {
            trail.remove(0);
        }
    }

    public double distanceFromPlanet() {
        return Math.sqrt(posX * posX + posY * posY);
    }

    public double speed() {
        return Math.sqrt(velX * velX + velY * velY);
    }

    public double specificEnergy() {
        double v = speed();
        double r = distanceFromPlanet();
        return 0.5 * v * v - G * planet.mass / r;
    }

    public double escapeVelocity() {
        return Math.sqrt(2.0 * G * planet.mass / distanceFromPlanet());
    }
}
