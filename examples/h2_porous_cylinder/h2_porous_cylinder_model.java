import com.comsol.model.Model;
import com.comsol.model.ModelNode;
import com.comsol.model.physics.Physics;
import com.comsol.model.util.ModelUtil;

public class h2_porous_cylinder_model {

    private static void trySet(Object feature, String key, String value) {
        try {
            feature.getClass().getMethod("set", String.class, String.class).invoke(feature, key, value);
        } catch (Exception ignored) {
        }
    }

    private static com.comsol.model.physics.PhysicsFeature createFeature(
            Physics physics, String tag, String[] typeCandidates, int entityDim) {
        RuntimeException last = null;
        for (String type : typeCandidates) {
            try {
                return physics.feature().create(tag, type, entityDim);
            } catch (RuntimeException ex) {
                last = ex;
            }
        }
        throw last;
    }

    public static Model run() {
        ModelUtil.clear();
        Model model = ModelUtil.create("Model");
        model.label("h2_porous_cylinder_model.mph");

        model.param().set("L_box", "0.10[m]", "Internal side length of sealed square container");
        model.param().set("h_water", "0.05[m]", "Water depth, half of the 10 cm vessel");
        model.param().set("r_sample", "0.011[m]", "Porous cylinder radius");
        model.param().set("d_sample", "0.022[m]", "Porous cylinder diameter");
        model.param().set("h_sample", "0.015[m]", "Porous cylinder height");
        model.param().set("z_sample_bot", "0.035[m]", "Bottom elevation of suspended cylinder");
        model.param().set("z_sample_top", "0.05[m]", "Top elevation, flush with water surface");
        model.param().set("eps_p", "0.90", "Connected porosity of sample");
        model.param().set("tau_p", "2.0", "Tortuosity of sample");
        model.param().set("D_water", "4.5e-9[m^2/s]", "Hydrogen diffusion coefficient in water");
        model.param().set("D_eff", "eps_p/tau_p*D_water", "Effective diffusivity in porous sample");
        model.param().set("c0", "0[mol/m^3]", "Initial hydrogen concentration");
        model.param().set("J_H2", "0.07[mol/(m^2*h)]", "Hydrogen generation flux");
        model.param().set("J_H2_SI", "J_H2", "Hydrogen generation flux in SI units");
        model.param().set("A_top", "pi*r_sample^2", "Cylinder top area");
        model.param().set("n_dot", "J_H2_SI*A_top", "Total hydrogen generation rate");
        model.param().set("t_end", "3600[s]", "Simulation end time");
        model.param().set("dt_out", "60[s]", "Output time step");

        ModelNode comp = model.component().create("comp1", true);
        comp.label("Water and porous cylinder");
        comp.geom().create("geom1", 3);
        comp.geom("geom1").lengthUnit("m");

        comp.geom("geom1").feature().create("waterblk", "Block");
        comp.geom("geom1").feature("waterblk").label("Water volume");
        comp.geom("geom1").feature("waterblk").set("size", new String[]{"L_box", "L_box", "h_water"});
        comp.geom("geom1").feature("waterblk").set("pos", new String[]{"0", "0", "0"});

        comp.geom("geom1").feature().create("samplecyl", "Cylinder");
        comp.geom("geom1").feature("samplecyl").label("Connected porous cylinder");
        comp.geom("geom1").feature("samplecyl").set("r", "r_sample");
        comp.geom("geom1").feature("samplecyl").set("h", "h_sample");
        comp.geom("geom1").feature("samplecyl").set("pos", new String[]{"L_box/2", "L_box/2", "z_sample_bot"});
        comp.geom("geom1").feature("samplecyl").set("axis", new String[]{"0", "0", "1"});

        comp.geom("geom1").feature().create("wateronly", "Difference");
        comp.geom("geom1").feature("wateronly").label("Water domain excluding porous sample");
        comp.geom("geom1").feature("wateronly").selection("input").set(new String[]{"waterblk"});
        comp.geom("geom1").feature("wateronly").selection("input2").set(new String[]{"samplecyl"});

        comp.geom("geom1").feature().create("uni1", "Union");
        comp.geom("geom1").feature("uni1").label("Water plus porous sample with internal boundaries");
        comp.geom("geom1").feature("uni1").selection("input").set(new String[]{"wateronly", "samplecyl"});
        comp.geom("geom1").feature("uni1").set("intbnd", "on");
        comp.geom("geom1").run();

        comp.selection().create("sel_sample_dom", "Box");
        comp.selection("sel_sample_dom").label("Porous sample domain");
        comp.selection("sel_sample_dom").set("entitydim", "3");
        comp.selection("sel_sample_dom").set("xmin", "L_box/2-r_sample-1e-5[m]");
        comp.selection("sel_sample_dom").set("xmax", "L_box/2+r_sample+1e-5[m]");
        comp.selection("sel_sample_dom").set("ymin", "L_box/2-r_sample-1e-5[m]");
        comp.selection("sel_sample_dom").set("ymax", "L_box/2+r_sample+1e-5[m]");
        comp.selection("sel_sample_dom").set("zmin", "z_sample_bot-1e-5[m]");
        comp.selection("sel_sample_dom").set("zmax", "z_sample_top+1e-5[m]");

        comp.selection().create("sel_sample_top", "Box");
        comp.selection("sel_sample_top").label("Hydrogen generation surface");
        comp.selection("sel_sample_top").set("entitydim", "2");
        comp.selection("sel_sample_top").set("xmin", "L_box/2-r_sample-1e-5[m]");
        comp.selection("sel_sample_top").set("xmax", "L_box/2+r_sample+1e-5[m]");
        comp.selection("sel_sample_top").set("ymin", "L_box/2-r_sample-1e-5[m]");
        comp.selection("sel_sample_top").set("ymax", "L_box/2+r_sample+1e-5[m]");
        comp.selection("sel_sample_top").set("zmin", "z_sample_top-1e-5[m]");
        comp.selection("sel_sample_top").set("zmax", "z_sample_top+1e-5[m]");

        comp.selection().create("sel_mid_vertical", "Cylinder");
        comp.selection("sel_mid_vertical").label("Cylinder-center vertical probe region");
        comp.selection("sel_mid_vertical").set("entitydim", "3");
        comp.selection("sel_mid_vertical").set("r", "0.001[m]");
        comp.selection("sel_mid_vertical").set("pos", new String[]{"L_box/2", "L_box/2", "h_water/2"});
        comp.selection("sel_mid_vertical").set("axis", new String[]{"0", "0", "1"});

        comp.material().create("mat_water", "Common");
        comp.material("mat_water").label("Water, H2 diffusion");
        comp.material("mat_water").propertyGroup("def").set("density", "1000[kg/m^3]");
        comp.material("mat_water").propertyGroup("def").set("dynamicviscosity", "1e-3[Pa*s]");

        comp.variable().create("var1");
        comp.variable("var1").label("Hydrogen transport variables");
        comp.variable("var1").set(
                "D_H2",
                "if(((x-L_box/2)^2+(y-L_box/2)^2<=r_sample^2)&&(z>=z_sample_bot)&&(z<=z_sample_top),D_eff,D_water)",
                "Hydrogen diffusivity: porous sample effective value, water value elsewhere");
        comp.variable("var1").set(
                "R_H2",
                "if(((x-L_box/2)^2+(y-L_box/2)^2<=r_sample^2)&&(z>=z_sample_bot)&&(z<=z_sample_top),J_H2_SI/h_sample,0[mol/(m^3*s)])",
                "Surface-flux-equivalent hydrogen source in the connected porous sample");

        Physics tds = comp.physics().create("tds", "DilutedSpecies", "geom1");
        tds.label("Transport of Diluted Species - H2 in water and porous sample");
        tds.field("concentration").field("c");
        tds.field("concentration").component(new String[]{"c"});
        tds.feature("cdm1").label("Hydrogen diffusion with sample effective diffusivity");
        tds.feature("cdm1").set("D_c", "D_H2");

        tds.feature("init1").set("initc", "c0");

        com.comsol.model.physics.PhysicsFeature flux = createFeature(
                tds, "flux_h2_top", new String[]{"FluxBoundary"}, 2);
        flux.label("Hydrogen generation flux on sample top");
        flux.selection().named("sel_sample_top");
        trySet(flux, "FluxType", "GeneralInwardFlux");
        trySet(flux, "species", "1");
        try {
            flux.set("J0", new String[]{"J_H2_SI"});
        } catch (Exception ex) {
            trySet(flux, "J0", "J_H2_SI");
        }

        com.comsol.model.physics.PhysicsFeature source = createFeature(
                tds, "src_h2_sample", new String[]{"Reactions", "Reaction"}, 3);
        source.label("Equivalent H2 production in porous sample");
        source.selection().all();
        trySet(source, "R_c_src", "userdef");
        try {
            source.set("R_c", "R_H2");
        } catch (Exception ex) {
            trySet(source, "R_c", "R_H2");
        }

        model.mesh().create("mesh1", "geom1");
        model.mesh("mesh1").label("Automatic tetrahedral mesh");
        model.mesh("mesh1").autoMeshSize(3);
        model.mesh("mesh1").run();

        model.study().create("std1");
        model.study("std1").label("Transient hydrogen diffusion");
        model.study("std1").feature().create("time", "Transient");
        model.study("std1").feature("time").set("tlist", "range(0,dt_out,t_end)");
        model.study("std1").feature("time").set("plot", "off");

        model.result().dataset().create("dset1", "Solution");
        model.result().dataset("dset1").label("Time-dependent solution");

        model.result().create("pg3d_c", "PlotGroup3D");
        model.result("pg3d_c").label("3D H2 concentration");
        model.result("pg3d_c").set("data", "dset1");
        model.result("pg3d_c").feature().create("vol1", "Volume");
        model.result("pg3d_c").feature("vol1").set("expr", "c");
        model.result("pg3d_c").feature("vol1").set("unit", "mol/m^3");
        model.result("pg3d_c").feature("vol1").set("descr", "Hydrogen concentration");

        model.result().dataset().create("cln_z", "CutLine3D");
        model.result().dataset("cln_z").label("Vertical z-axis through cylinder center");
        model.result().dataset("cln_z").set("genpoints", new String[][]{
                {"L_box/2", "L_box/2", "0"},
                {"L_box/2", "L_box/2", "h_water"}
        });

        model.result().dataset().create("cln_x", "CutLine3D");
        model.result().dataset("cln_x").label("Horizontal x-axis through cylinder center");
        model.result().dataset("cln_x").set("genpoints", new String[][]{
                {"0", "L_box/2", "z_sample_top"},
                {"L_box", "L_box/2", "z_sample_top"}
        });

        model.result().numerical().create("max_c", "MaxVolume");
        model.result().numerical("max_c").label("Maximum H2 concentration versus time");
        model.result().numerical("max_c").selection().all();
        model.result().numerical("max_c").set("expr", "c");
        model.result().numerical("max_c").set("unit", "mol/m^3");
        model.result().numerical("max_c").set("data", "dset1");

        model.result().numerical().create("avg_c_sample", "AvVolume");
        model.result().numerical("avg_c_sample").label("Average H2 concentration inside porous sample");
        model.result().numerical("avg_c_sample").selection().named("sel_sample_dom");
        model.result().numerical("avg_c_sample").set("expr", "c");
        model.result().numerical("avg_c_sample").set("unit", "mol/m^3");
        model.result().numerical("avg_c_sample").set("data", "dset1");

        return model;
    }

    public static void main(String[] args) throws Exception {
        Model model = run();
        model.study("std1").run();
        model.result().dataset("dset1").set("solution", "sol1");
        model.result("pg3d_c").set("data", "dset1");
        model.save("h2_porous_cylinder_model.mph");
    }
}
