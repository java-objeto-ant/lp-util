package org.rmj.lp.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.constants.Logical;
import static org.rmj.lp.util.DSUpload.oApp;

public class AutoTagSubItem {

    public static void main(String[] args) {
        String path;

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems";
        } else {
            path = "/srv/mac/GGC_Java_Systems";
        }

        System.setProperty("sys.default.path.config", path);

        oApp = new GRider("General");

        if (!oApp.loadEnv("General")) {
            System.exit(1);
        }

        if (!oApp.loadUser("General", "M001111122")) {
            System.exit(1);
        }

        try {
            String lsSQL = "SELECT a.sStockIDx FROM Inventory_Sub_Unit a "
                    + " LEFT JOIN Inventory b ON a.sStockIDx = b.sStockIDx"
                    + "     WHERE (b.cWSubUnit = " + SQLUtil.toSQL(Logical.NO)
                    + "         AND b.cWithBOMx = " + SQLUtil.toSQL(Logical.NO) + ")"
                    + " GROUP BY a.sStockIDx";
            ResultSet loRS = oApp.executeQuery(lsSQL);

            if (MiscUtil.RecordCount(loRS) <= 0) {
                System.out.println("No record to update.");
                System.exit(0);
            }

            while (loRS.next()) {
                System.out.println(loRS.getString("sStockIDx"));
                lsSQL = "SELECT * FROM Inventory"
                        + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                ResultSet loRSInventory = oApp.executeQuery(lsSQL);

                while (loRSInventory.next()) {
                    if (!loRSInventory.getString("cWSubUnit").equals("1")
                            && !loRSInventory.getString("cWithBOMx").equals("1")) {
                        oApp.beginTrans();

                        lsSQL = "UPDATE Inventory SET"
                                + "  cWSubUnit = " + SQLUtil.toSQL(Logical.YES)
                                + " WHERE sStockIDx = " + SQLUtil.toSQL(loRSInventory.getObject("sStockIDx"));
                        if (oApp.executeQuery(lsSQL, "Inventory", oApp.getBranchCode(), "") <= 0) {
                            oApp.rollbackTrans();
                            System.err.println("Unable to execute statement: " + lsSQL);
                            System.exit(1);
                        }

                        oApp.commitTrans();
                    }
                }
            }
        } catch (SQLException e) {
            oApp.rollbackTrans();
            e.printStackTrace();
            System.exit(1);
        }
    }
}
