package org.rmj.lp.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import static org.rmj.lp.util.DSUpload.oApp;

public class FixHistoryLedger {
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
            String lsSQL = "SELECT sStockIDx, sBranchCd, dBegInvxx" +
                            " FROM Inv_Master" +
                            " WHERE sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode()) +
                                " AND dBegInvxx IS NOT NULL";
        
            ResultSet loRS = oApp.executeQuery(lsSQL);
            
            if (MiscUtil.RecordCount(loRS) <= 0){
                System.out.println("No record to update.");
                System.exit(0);
            }
            
            while (loRS.next()){
                System.out.println(loRS.getString("sStockIDx") + "\t" + loRS.getString("sBranchCd"));
                lsSQL = "SELECT * FROM Inv_Ledger" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx")) +
                            " AND sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode()) +
                            " AND dTransact <= " + SQLUtil.toSQL(loRS.getString("dBegInvxx")) +
                        " ORDER BY dTransact, nLedgerNo";
            
                ResultSet loRSLedger = oApp.executeQuery(lsSQL);
                
                int lnLastHistNo = 0;
                while (loRSLedger.next()){
                    if (lnLastHistNo == 0){
                        //get last ledger no
                        lsSQL = "SELECT * FROM Inv_Ledger_Hist" +
                                " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx")) +
                                    " AND sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode()) +
                                " ORDER BY nLedgerNo DESC LIMIT 1";

                        ResultSet loRSx = oApp.executeQuery(lsSQL);
                        if (loRSx.next()) lnLastHistNo = loRSx.getInt("nLedgerNo");
                    }
                    
                    oApp.beginTrans();
                    lnLastHistNo++;
                    
                    lsSQL = "INSERT INTO Inv_Ledger_Hist SET" +
                            "  sStockIDx = " + SQLUtil.toSQL(loRSLedger.getObject("sStockIDx")) +
                            ", sBranchCd = " + SQLUtil.toSQL(loRSLedger.getObject("sBranchCd")) +
                            ", nLedgerNo = " + SQLUtil.toSQL(lnLastHistNo) +
                            ", dTransact = " + SQLUtil.toSQL(loRSLedger.getObject("dTransact")) +
                            ", sSourceCd = " + SQLUtil.toSQL(loRSLedger.getObject("sSourceCd")) +
                            ", sSourceNo = " + SQLUtil.toSQL(loRSLedger.getObject("sSourceNo")) +
                            ", nQtyInxxx = " + SQLUtil.toSQL(loRSLedger.getObject("nQtyInxxx")) +	
                            ", nQtyOutxx = " + SQLUtil.toSQL(loRSLedger.getObject("nQtyOutxx")) +	
                            ", nQtyOrder = " + SQLUtil.toSQL(loRSLedger.getObject("nQtyOrder")) +	
                            ", nQtyIssue = " + SQLUtil.toSQL(loRSLedger.getObject("nQtyIssue")) +	
                            ", nPurPrice = " + SQLUtil.toSQL(loRSLedger.getObject("nPurPrice")) +	
                            ", nUnitPrce = " + SQLUtil.toSQL(loRSLedger.getObject("nUnitPrce")) +	
                            ", nQtyOnHnd = " + SQLUtil.toSQL(loRSLedger.getObject("nQtyOnHnd")) +	
                            ", dExpiryxx = " + SQLUtil.toSQL(loRSLedger.getObject("dExpiryxx")) +
                            ", sModified = " + SQLUtil.toSQL(loRSLedger.getObject("sModified")) +
                            ", dModified = " + SQLUtil.toSQL(loRSLedger.getObject("dModified"));
                    
                    if (oApp.executeQuery(lsSQL, "Inv_Ledger_Hist", oApp.getBranchCode(), "") <= 0){
                        oApp.rollbackTrans();
                        System.err.println("Unable to execute statement: " + lsSQL);
                        System.exit(1);
                    }
                    
                    lsSQL = "DELETE FROM Inv_Ledger" + 
                            " WHERE sStockIDx = " + SQLUtil.toSQL(loRSLedger.getObject("sStockIDx")) +
                                " AND sBranchCd = " + SQLUtil.toSQL(loRSLedger.getObject("sBranchCd")) +
                                " AND dTransact = " + SQLUtil.toSQL(loRSLedger.getObject("dTransact")) +
                                " AND sSourceCd = " + SQLUtil.toSQL(loRSLedger.getObject("sSourceCd")) +
                                " AND sSourceNo = " + SQLUtil.toSQL(loRSLedger.getObject("sSourceNo"));
                            
                    if (oApp.executeQuery(lsSQL, "Inv_Ledger", oApp.getBranchCode(), "") <= 0){
                        oApp.rollbackTrans();
                        System.err.println("Unable to execute statement: " + lsSQL);
                        System.exit(1);
                    }
                    
                    oApp.commitTrans();
                }
            }
        } catch (SQLException e) {
            oApp.rollbackTrans();
            e.printStackTrace();
            System.exit(1);
        }
    }
}
