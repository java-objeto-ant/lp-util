package org.rmj.lp.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.constants.TransactionStatus;

/**
 *
 * @author Maynard
 */
public class AutoCancelPO {

    private int pnCancelDays = 60;
    private final String MASTER_TABLE = "PO_Master";

    private GRider instance;
    private String message;

    public AutoCancelPO(GRider foGrider) {
        instance = foGrider;
    }

    public AutoCancelPO(GRider foGrider, int fiCancelDays) {
        instance = foGrider;
        pnCancelDays = fiCancelDays;
    }

    public String getMessage() {
        return message;
    }

    public boolean processAutoCancel() {
        try {
            message = "";

            if (pnCancelDays <= 0) {
                message = "Machine is not configured.";
                return false;
            }

            String lsSQL = getSQ_Master();
            ResultSet loRS = instance.executeQuery(lsSQL);
            System.out.println(lsSQL);
            if (MiscUtil.RecordCount(loRS) > 0) {
                loRS.beforeFirst();
                while (loRS.next()) {
                    String sTransNox = loRS.getString("sTransNox");
                    lsSQL = "UPDATE CASys_DBF_LP." + MASTER_TABLE
                            + " set cTranStat = 5 ,"
                            + " sRemarksx = CONCAT(IFNULL(sRemarksx, ''), ' NOT USED')"
                            + " WHERE sTransNox =" + SQLUtil.toSQL(sTransNox);

                    if (instance.executeQuery(lsSQL, "", "", "") <= 0) {
                        System.out.println(lsSQL);
                    }
                    lsSQL = "";

                }
                message = "PO Auto Cancel Utility successfully ";
                loRS.close();

            } else {
                message = "No Record Found !";
                loRS.close();

            }
        } catch (SQLException e) {
            e.printStackTrace();
            message = e.getMessage();
            return false;
        }

        return true;
    }

    private String getSQ_Master() {
        return "SELECT sTransNox"
                + ", dTransact"
                + ", cTranStat"
                + " FROM CASys_DBF_LP." + MASTER_TABLE
                + " WHERE dTransact <= DATE_SUB(CURDATE(), INTERVAL " + pnCancelDays + " DAY) "
                + " AND cTranStat NOT IN ( " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED)
                + ", " + SQLUtil.toSQL(TransactionStatus.STATE_POSTED)
                + ", " + SQLUtil.toSQL("5") + ")"
                + " AND sTransNox NOT IN (SELECT sOrderNox FROM PO_Receiving_Detail)"
                + " ORDER BY dTransact DESC";

    }

    public static void main(String[] args) {
        String path;

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems";
        } else {
            path = "/srv/GGC_Java_Systems";
        }

        System.setProperty("sys.default.path.config", path);

        GRider oApp = new GRider("General");

        if (!oApp.loadEnv("General")) {
            System.exit(1);
        }
        if (!oApp.loadUser("General", "M001111122")) {
            System.exit(1);
        }

        if (args.length > 1) {
            int lnCancelDays = 60;
            try {
                lnCancelDays = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number of days. Using default: 60 days.");
            }

            AutoCancelPO poAutoCancel = new AutoCancelPO(oApp, lnCancelDays);
            if (poAutoCancel.processAutoCancel()) {
                System.out.println("Process completed: " + poAutoCancel.getMessage());
            } else {
                System.out.println("Process failed: " + poAutoCancel.getMessage());
            }
        } else {
            AutoCancelPO poAutoCancel = new AutoCancelPO(oApp);
            if (poAutoCancel.processAutoCancel()) {
                System.out.println("Process completed: " + poAutoCancel.getMessage());
            } else {
                System.out.println("Process failed: " + poAutoCancel.getMessage());
            }

        }

    }
}
