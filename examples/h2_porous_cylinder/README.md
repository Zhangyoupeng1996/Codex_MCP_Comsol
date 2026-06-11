# Transient H2 Diffusion in a Porous Cylinder Suspended in Water

This example builds a 3D time-dependent COMSOL model for hydrogen generated on
the top face of a connected porous cylindrical sample suspended in a half-filled
sealed cubic container.

## Model Assumptions

- Only the water phase and connected porous sample are modeled.
- The upper headspace is initially vacuum and is not included as a gas-domain
  diffusion model in this first version.
- The water surface is treated as a no-flux boundary to observe the hydrogen
  concentration gradient in the water and porous sample.
- Container walls are no-flux boundaries.
- The sample side wall and bottom are continuous with the surrounding water
  because the same Transport of Diluted Species physics is solved across both
  domains with domain-specific diffusion coefficients.
- Hydrogen generation is represented with the requested top-face flux parameter
  `J_H2`. The Java script also adds a surface-flux-equivalent volumetric source
  `R_H2 = J_H2/h_sample` inside the connected porous sample, which preserves the
  same total generation rate `n_dot = J_H2*A_top` and avoids COMSOL Java API
  version differences in the Transport of Diluted Species flux-node internals.

Important: This model mainly describes the hydrogen concentration gradient in
the water phase and in the connected porous sample. Because the upper space is
initially vacuum, strict gas-phase transport cannot be represented by a simple
ordinary dilute-species diffusion model in air. To simulate hydrogen pressure
growth in the sealed headspace, extend the model with ideal-gas mass balance,
Henry-law water-gas interfacial coupling, and a gas pressure variable.

## Geometry

- Sealed cubic vessel internal side length: `L_box = 0.10 m`
- Water depth: `h_water = 0.05 m`
- Porous cylinder diameter: `d_sample = 0.022 m`
- Porous cylinder radius: `r_sample = 0.011 m`
- Porous cylinder height: `h_sample = 0.015 m`
- Cylinder top: `z_sample_top = 0.05 m`, flush with the water surface
- Cylinder bottom: `z_sample_bot = 0.035 m`
- Cylinder centerline: `x = 0.05 m`, `y = 0.05 m`

## Parameters

- Hydrogen diffusivity in water:
  `D_water = 4.5e-9 m^2/s`
- Connected porosity:
  `eps_p = 0.90`
- Tortuosity:
  `tau_p = 2.0`
- Effective diffusivity in porous sample:
  `D_eff = eps_p/tau_p*D_water = 2.025e-9 m^2/s`
- Initial hydrogen concentration:
  `c0 = 0 mol/m^3`
- Hydrogen generation flux:
  `J_H2 = 0.07 mol/(m^2*h) = 1.944444e-5 mol/(m^2*s)`
- Surface-flux-equivalent porous-sample source:
  `R_H2 = J_H2/h_sample`
- Cylinder top area:
  `A_top = pi*0.011^2 = 3.8013e-4 m^2`
- Total hydrogen generation rate:
  `n_dot = J_H2*A_top = approximately 7.39e-9 mol/s`
- Time range:
  `range(0,60,3600) s`

## Files

- `h2_porous_cylinder_model.java` builds and solves the COMSOL model.
- `run_comsol_model.ps1` runs the model and exports results on Windows.
- `export_results.java` exports images and CSV result tables from a solved MPH
  file.
- `results/` is created locally and should not be committed.

## How to Run

Open PowerShell in this directory and run:

```powershell
.\run_comsol_model.ps1
```

If COMSOL is installed somewhere else:

```powershell
.\run_comsol_model.ps1 -ComsolBin "C:\Program Files\COMSOL\COMSOL63\Multiphysics\bin\win64"
```

To only build and solve the MPH file:

```powershell
.\run_comsol_model.ps1 -SkipExport
```

To export results from an existing solved MPH file:

```powershell
.\run_comsol_model.ps1 -SkipSolve
```

## Outputs

The scripts create:

- `h2_porous_cylinder_model.mph`
- `h2_porous_cylinder_model_with_exports.mph`
- `results/h2_concentration_0060s.png`
- `results/h2_concentration_0600s.png`
- `results/h2_concentration_1800s.png`
- `results/h2_concentration_3600s.png`
- `results/vertical_z_center_profile.csv`
- `results/horizontal_x_center_profile.csv`
- `results/max_concentration_vs_time.csv`
- `results/sample_average_concentration_vs_time.csv`

## How to Modify Key Parameters

Edit `h2_porous_cylinder_model.java`:

- Hydrogen generation flux:
  change `model.param().set("J_H2", "0.07[mol/(m^2*h)]", ...)`
- Porosity:
  change `model.param().set("eps_p", "0.90", ...)`
- Tortuosity:
  change `model.param().set("tau_p", "2.0", ...)`
- Water diffusivity:
  change `model.param().set("D_water", "4.5e-9[m^2/s]", ...)`
- Simulation end time:
  change `model.param().set("t_end", "3600[s]", ...)`
- Output interval:
  change `model.param().set("dt_out", "60[s]", ...)`

The effective diffusion coefficient is computed in COMSOL as:

```text
D_eff = eps_p/tau_p*D_water
```

## Notes for Further Development

A physically richer sealed-vessel model should add:

- gas-domain hydrogen amount or pressure as a state variable,
- Henry-law partitioning at the water-gas interface,
- ideal-gas pressure growth in the headspace,
- optional bubble nucleation or degassing if supersaturation becomes relevant.
