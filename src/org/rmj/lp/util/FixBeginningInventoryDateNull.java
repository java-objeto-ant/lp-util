package org.rmj.lp.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import static org.rmj.lp.util.DSUpload.oApp;

public class FixBeginningInventoryDateNull {
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
        
        String lsSQL = "SELECT sStockIDx, dBegInvxx, nBegQtyxx" +
                        " FROM Inv_Master" +
                        " WHERE sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode()) +
                            " AND sStockIDxx <> ''" +
                            " AND dBegInvxx IS NULL";
        
        ResultSet loRS = oApp.executeQuery(lsSQL);
        
        try {
            if (MiscUtil.RecordCount(loRS) <= 0){
                System.out.println("No record to update.");
                System.exit(0);
            }
            
            oApp.beginTrans();
            
            ResultSet loRS1;
            while (loRS.next()){
                //check if it has a ledger on or after 2024-05-31
                lsSQL = "SELECT sSourceCd, dTransact FROM Inv_Ledger" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx")) +
                            " AND sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode()) +
                        " ORDER BY dTransact, nLedgerNo LIMIT 1";
                
                
                
                lsSQL = "SELECT sSourceCd, dTransact FROM Inv_Ledger" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx")) +
                            " AND sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode()) +
                        " ORDER BY dTransact, nLedgerNo LIMIT 1";
                
                loRS1 = oApp.executeQuery(lsSQL);
                
                if (loRS1.next()){
                    if (loRS1.getString("sSourceCd").equalsIgnoreCase("pcrc")){
                        System.out.println(loRS.getDate("dBegInvxx") + "-->>" + loRS1.getDate("dTransact"));
                        long lnDiff = CommonUtils.dateDiff(loRS.getDate("dBegInvxx"), loRS1.getDate("dTransact"));
                        System.out.println(lnDiff);

                        if (lnDiff >= 0) {
                            lsSQL = "UPDATE Inv_Master SET" +
                                        "  dAcquired  = " + SQLUtil.toSQL(CommonUtils.dateAdd(loRS1.getDate("dTransact"), -1)) +
                                        ", dBegInvxx  = " + SQLUtil.toSQL(CommonUtils.dateAdd(loRS1.getDate("dTransact"), -1)) +
                                    " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx")) +
                                    " AND sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode());

                            if (oApp.executeQuery(lsSQL, "Inv_Master", oApp.getBranchCode(), "") <= 0){
                                System.err.println("Unable to update inventory beginning date.");
                                oApp.rollbackTrans();
                                System.exit(1);
                            }
                        }
                    }
                }
            }
            oApp.commitTrans();
        } catch (SQLException e) {
            oApp.rollbackTrans();
            e.printStackTrace();
        }
    }
}
