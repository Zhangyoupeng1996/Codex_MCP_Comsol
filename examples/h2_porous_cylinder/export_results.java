import com.comsol.model.Model;
import com.comsol.model.util.ModelUtil;

public class export_results {

    private static void trySetPlotTime(Model model, int solnum) {
        try {
            model.result("pg3d_c").set("looplevel", new int[]{solnum});
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) throws Exception {
        String modelPath = (args != null && args.length > 0) ? args[0] : "h2_porous_cylinder_model.mph";
        Model model = ModelUtil.load("h2_porous_cylinder_export", modelPath);
        model.study("std1").run();
        model.result().dataset("dset1").set("solution", "sol1");
        model.result("pg3d_c").set("data", "dset1");

        model.result().export().create("img_c_0060", "Image3D");
        model.result().export("img_c_0060").label("3D concentration image at 60 s");
        model.result().export("img_c_0060").set("plotgroup", "pg3d_c");
        model.result().export("img_c_0060").set("pngfilename", "results/h2_concentration_0060s.png");

        model.result().export().create("img_c_0600", "Image3D");
        model.result().export("img_c_0600").label("3D concentration image at 600 s");
        model.result().export("img_c_0600").set("plotgroup", "pg3d_c");
        model.result().export("img_c_0600").set("pngfilename", "results/h2_concentration_0600s.png");

        model.result().export().create("img_c_1800", "Image3D");
        model.result().export("img_c_1800").label("3D concentration image at 1800 s");
        model.result().export("img_c_1800").set("plotgroup", "pg3d_c");
        model.result().export("img_c_1800").set("pngfilename", "results/h2_concentration_1800s.png");

        model.result().export().create("img_c_3600", "Image3D");
        model.result().export("img_c_3600").label("3D concentration image at 3600 s");
        model.result().export("img_c_3600").set("plotgroup", "pg3d_c");
        model.result().export("img_c_3600").set("pngfilename", "results/h2_concentration_3600s.png");

        model.result().export().create("tbl_z", "Data");
        model.result().export("tbl_z").label("Vertical z-axis concentration profile");
        model.result().export("tbl_z").set("data", "cln_z");
        model.result().export("tbl_z").set("expr", new String[]{"c"});
        model.result().export("tbl_z").set("unit", new String[]{"mol/m^3"});
        model.result().export("tbl_z").set("filename", "results/vertical_z_center_profile.csv");

        model.result().export().create("tbl_x", "Data");
        model.result().export("tbl_x").label("Horizontal centerline concentration profile");
        model.result().export("tbl_x").set("data", "cln_x");
        model.result().export("tbl_x").set("expr", new String[]{"c"});
        model.result().export("tbl_x").set("unit", new String[]{"mol/m^3"});
        model.result().export("tbl_x").set("filename", "results/horizontal_x_center_profile.csv");

        model.result().table().create("tbl_max", "Table");
        model.result().table("tbl_max").label("Maximum concentration versus time");
        model.result().numerical("max_c").set("table", "tbl_max");
        model.result().numerical("max_c").setResult();
        model.result().export().create("csv_max", "Table");
        model.result().export("csv_max").set("table", "tbl_max");
        model.result().export("csv_max").set("filename", "results/max_concentration_vs_time.csv");

        model.result().table().create("tbl_avg", "Table");
        model.result().table("tbl_avg").label("Average sample concentration versus time");
        model.result().numerical("avg_c_sample").set("table", "tbl_avg");
        model.result().numerical("avg_c_sample").setResult();
        model.result().export().create("csv_avg", "Table");
        model.result().export("csv_avg").set("table", "tbl_avg");
        model.result().export("csv_avg").set("filename", "results/sample_average_concentration_vs_time.csv");

        trySetPlotTime(model, 2);
        model.result().export("img_c_0060").run();
        trySetPlotTime(model, 11);
        model.result().export("img_c_0600").run();
        trySetPlotTime(model, 31);
        model.result().export("img_c_1800").run();
        trySetPlotTime(model, 61);
        model.result().export("img_c_3600").run();
        model.result().export("tbl_z").run();
        model.result().export("tbl_x").run();
        model.result().export("csv_max").run();
        model.result().export("csv_avg").run();

        model.save("h2_porous_cylinder_model_with_exports.mph");
    }
}
