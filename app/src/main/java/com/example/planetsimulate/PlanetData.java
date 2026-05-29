package com.example.planetsimulate;

public class PlanetData {

    public final String name;
    public final double mass;
    public final double radius;
    public final int color;

    public PlanetData(String name, double mass, double radius, int color) {
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.color = color;
    }

    public static final PlanetData[] ALL = {
            new PlanetData("水星", 3.301e23,  2.439e6,  0xFFAAAAAA),
            new PlanetData("金星", 4.867e24,  6.051e6,  0xFFE8C97A),
            new PlanetData("地球", 5.972e24,  6.371e6,  0xFF4B9CD3),
            new PlanetData("火星", 6.390e23,  3.389e6,  0xFFBC4A2E),
            new PlanetData("木星", 1.898e27,  6.991e7,  0xFFD4A96A),
            new PlanetData("土星", 5.683e26,  5.823e7,  0xFFE4C97E),
            new PlanetData("天王星", 8.681e25, 2.536e7, 0xFF7DE8E8),
            new PlanetData("海王星", 1.024e26, 2.462e7, 0xFF4B70DD),
    };
}
