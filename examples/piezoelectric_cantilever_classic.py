"""Classic piezoelectric cantilever example for COMSOL/MPh.

The model is intentionally compact: a bonded substrate and piezoelectric layer,
top/bottom electrodes, a fixed root, a stationary solve, and visible result
plot groups. It creates a true ``PiezoelectricEffect`` coupling when available
and also defines an analytical displacement indicator for quick inspection.
"""

from __future__ import annotations

from datetime import datetime
from pathlib import Path

import mph


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "comsol_outputs" / "piezoelectric_cantilever_classic"
MODEL_NAME = "piezoelectric_cantilever_classic"


def block(geom, tag: str, size: list[str], pos: list[str], label: str):
    feature = geom.feature().create(tag, "Block")
    feature.set("size", size)
    feature.set("pos", pos)
    feature.label(label)
    return feature


def box_selection(comp, tag: str, label: str, entitydim: str, bounds: dict[str, str]):
    selection = comp.selection().create(tag, "Box")
    selection.label(label)
    selection.set("entitydim", entitydim)
    for key, value in bounds.items():
        selection.set(key, value)
    return selection


def volume_plot(group, tag: str, expression: str, description: str, unit: str | None = None):
    feature = group.feature().create(tag, "Volume")
    feature.set("expr", expression)
    feature.set("descr", description)
    if unit:
        try:
            feature.set("unit", unit)
        except Exception:
            pass
    return feature


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    client = mph.Client(host="localhost", port=2036)
    model = client.create(MODEL_NAME)
    java = model.java

    params = java.param()
    params.set("L", "40[mm]", "Cantilever length")
    params.set("W", "8[mm]", "Cantilever width")
    params.set("ts", "0.4[mm]", "Substrate thickness")
    params.set("tp", "0.6[mm]", "Piezoelectric layer thickness")
    params.set("V0", "100[V]", "Top electrode voltage")
    params.set("E_pzt", "63[GPa]", "Effective Young's modulus")
    params.set("nu_pzt", "0.31", "Poisson ratio")
    params.set("rho_pzt", "7800[kg/m^3]", "Density")
    params.set("epsr33", "1200", "Relative permittivity")
    params.set("d31", "-175e-12[m/V]", "Transverse piezoelectric coefficient")
    params.set("curv", "6*d31*V0/(tp*(ts+tp))", "Estimated curvature")
    params.set("w_tip_est", "0.5*curv*L^2", "Estimated tip deflection")

    comp = java.component().create("comp1", True)
    geom = comp.geom().create("geom1", 3)
    block(geom, "substrate", ["L", "W", "ts"], ["0", "0", "0"], "Elastic substrate")
    block(geom, "piezo", ["L", "W", "tp"], ["0", "0", "ts"], "Piezoelectric layer")
    union = geom.feature().create("uni1", "Union")
    union.selection("input").set(["substrate", "piezo"])
    union.set("intbnd", "on")
    geom.run()

    material = comp.material().create("mat1", "Common")
    material.label("PZT-like effective material")
    material.propertyGroup("def").set("youngsmodulus", "E_pzt")
    material.propertyGroup("def").set("poissonsratio", "nu_pzt")
    material.propertyGroup("def").set("density", "rho_pzt")
    material.propertyGroup("def").set("relpermittivity", "epsr33")

    top = box_selection(comp, "sel_top", "Top electrode", "2", {
        "xmin": "-1e-9", "xmax": "L+1e-9",
        "ymin": "-1e-9", "ymax": "W+1e-9",
        "zmin": "ts+tp-1e-8", "zmax": "ts+tp+1e-8",
    })
    bottom = box_selection(comp, "sel_bottom", "Bottom electrode", "2", {
        "xmin": "-1e-9", "xmax": "L+1e-9",
        "ymin": "-1e-9", "ymax": "W+1e-9",
        "zmin": "ts-1e-8", "zmax": "ts+1e-8",
    })
    root = box_selection(comp, "sel_root", "Fixed root", "2", {
        "xmin": "-1e-8", "xmax": "1e-8",
        "ymin": "-1e-9", "ymax": "W+1e-9",
        "zmin": "-1e-9", "zmax": "ts+tp+1e-9",
    })

    electrostatics = comp.physics().create("es", "Electrostatics", "geom1")
    potential = electrostatics.create("pot1", "ElectricPotential", 2)
    potential.selection().named(top.tag())
    potential.set("V0", "V0")
    ground = electrostatics.create("gnd1", "Ground", 2)
    ground.selection().named(bottom.tag())

    solid = comp.physics().create("solid", "SolidMechanics", "geom1")
    fixed = solid.create("fix1", "Fixed", 2)
    fixed.selection().named(root.tag())

    used_true_piezo = False
    try:
        coupling = java.multiphysics().create("pze1", "PiezoelectricEffect", "geom1", 3)
        coupling.label("Piezoelectric Effect")
        used_true_piezo = True
        print("Created true PiezoelectricEffect coupling.")
    except Exception as exc:
        print(f"PiezoelectricEffect unavailable, using indicator field only: {exc}")

    variables = comp.variable().create("var1")
    variables.label("Piezoelectric response indicators")
    variables.set("w_eq", "0.5*curv*x^2")
    variables.descr("w_eq", "Estimated voltage-induced out-of-plane displacement")
    variables.set("tip_ratio", "w_eq/w_tip_est")

    mesh = comp.mesh().create("mesh1", "geom1")
    mesh.autoMeshSize(4)
    mesh.run()

    study = java.study().create("std1")
    study.create("stat", "Stationary")
    study.run()

    result = java.result()
    pg_v = result.create("pg_v", "PlotGroup3D")
    pg_v.label("Electric potential across piezoelectric layer")
    volume_plot(pg_v, "volV", "V", "Electric potential", "V")
    pg_v.run()

    pg_e = result.create("pg_e", "PlotGroup3D")
    pg_e.label("Electric field norm")
    volume_plot(pg_e, "volE", "es.normE", "Electric field norm", "V/m")
    pg_e.run()

    pg_w = result.create("pg_w", "PlotGroup3D")
    pg_w.label("Equivalent piezoelectric bending shape")
    volume_plot(pg_w, "volW", "w_eq", "Equivalent piezo deflection", "um")
    pg_w.run()

    if used_true_piezo:
        pg_u = result.create("pg_u", "PlotGroup3D")
        pg_u.label("True solid displacement norm")
        volume_plot(pg_u, "volU", "solid.disp", "Displacement norm", "um")
        pg_u.run()

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    version_file = OUT_DIR / f"{MODEL_NAME}_{timestamp}.mph"
    latest_file = OUT_DIR / f"{MODEL_NAME}_latest.mph"
    model.save(str(version_file))
    model.save(str(latest_file))
    print(f"Saved: {version_file}")
    print(f"Saved: {latest_file}")


if __name__ == "__main__":
    main()
