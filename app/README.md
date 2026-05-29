# StarPath

A high-precision two-body orbital mechanics simulation platform for Android.

Simulate the motion of a test particle under gravitational influence from solar system planets, with real-time trajectory visualization and analytical orbit data.

---

## Features

### Physics Engine

- Velocity Verlet integration algorithm with 20 substeps per frame
- Ensures numerical stability and bounded energy error near periapsis
- Supports dynamic time multiplier for accelerated simulation

### Orbital Visualization

- Velocity-colored trajectory: blue (slow) to orange (fast) linear interpolation
- Laplace-Runge-Lenz vector computation for accurate periapsis and apoapsis markers
- Edge arrows when apsidal markers are off-screen
- Elliptical orbit preview before simulation starts, computed from current parameters

### Parameter Control

- Slider mode for intuitive adjustment
- Professional mode for precise numerical input with valid range hints
- Initial velocity angle control: 0 degrees (tangential) to 180 degrees (reverse tangential)
- Six preset scenarios: Moon, ISS, GEO satellite, Hubble Space Telescope, elliptical orbit demo, escape velocity demo

### Analytical Data

- Real-time computation of eccentricity, semi-major axis, orbital period, periapsis and apoapsis distance
- Angular momentum calculated with tangential velocity component v times cos(theta) for oblique entry accuracy
- Specific mechanical energy monitoring (MJ/kg)

### User Experience

- Dark space-themed UI
- Splash screen with satellite orbit animation on launch
- Six-step overlay tutorial on first launch, never shown again after completion
- Auto-fit scale based on planet radius and initial distance
- Pinch-to-zoom and pan gestures, double-tap to reset view

---

## Preset Scenarios

| Scenario | Distance | Velocity | Notes |
|---|---|---|---|
| Moon | 384,400 km | 1,022 m/s | Period error < 0.5% |
| ISS | 6,779 km | 7,668 m/s | 408 km altitude |
| GEO Satellite | 35,786 km | 3,075 m/s | 24-hour period |
| Hubble Space Telescope | 6,918 km | 7,590 m/s | 547 km altitude |
| Elliptical Demo | 50,000 km | 2,000 m/s | 45 degrees, eccentricity ~0.7 |
| Escape Velocity Demo | 100,000 km | 3,900 m/s | Near escape threshold |

---

## Architecture

The system uses a modular layered architecture with full separation between physics computation, data management, and rendering.

```
MainActivity          (UI controller, lifecycle management)
├── SplashActivity    (launch animation)
├── TutorialOverlay   (first-launch onboarding)
├── GameLoop          (Choreographer-based frame driver)
│   ├── PhysicsEngine (Velocity Verlet integration, no state ownership)
│   └── SimulationState (position, velocity, trail history)
│       └── PlanetData (solar system physical constants)
├── SimulationView    (Canvas rendering, gesture detection)
└── OrbitalDataActivity (analytical solution computation)
```

---

## Tech Stack

- Platform: Android OS (API 24+)
- Language: Java
- Rendering: Custom View with Canvas API
- Frame sync: Android Choreographer
- Build: Gradle

---

## Supported Planets

Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune

All planetary data sourced from real physical constants (mass, radius, surface gravity).

---

## Build Instructions

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on a device or emulator with API 24 or above

---

## Competition

2026 National Ilan University Programming, Information and AI Application Contest
Category: Creative Implementation Professional Group
Advisor: Chao-Hsi Huang