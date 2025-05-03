package org.rmj.lp.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import static org.rmj.lp.util.DSUpload.oApp;

public class ChargeInvoiceToAdvances {
    public static void main(String[] args) {
        String path;

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems";
        } else {
            path = "/srv/mac/GGC_Java_Systems";
        }

        System.setProperty("sys.default.path.config", path);

        oApp = new GRider("gRider");

        if (!oApp.loadEnv("gRider")) {
            System.exit(1);
        }
        if (!oApp.loadUser("gRider", "M001111122")) {
            System.exit(1);
        }
        
        //check open and unbilled charge invoice
        String lsSQL = "SELECT" +
                            "  a.sTransNox" +
                            ", a.sClientID" +
                            ", a.sChargeNo" +
                            ", a.nAmountxx" +
                            ", a.nVATSales" +
                            ", a.nVATAmtxx" +
                            ", a.nDiscount" +
                            ", a.nVatDiscx" +
                            ", a.nPWDDiscx" +
                            ", a.sClientNm" +
                            ", d.sCompnyNm" +
                            ", c.cSalTypex" +
                            ", a.nAmountxx - (a.nDiscount + a.nVatDiscx + a.nPWDDiscx) nTranTotl" +
                            ", b.dTransact" +
                        " FROM CASys_DBF_LP.Charge_Invoice a" +
                                " LEFT JOIN GGC_ISysDBF.Employee_Master001 c ON a.sClientID = c.sEmployID" +
                                " LEFT JOIN GGC_ISysDBF.Client_Master d ON c.sEmployID = d.sClientID" +
                            ", CASys_DBF_LP.SO_Master b" +
                        " WHERE a.sSourceNo = b.sTransNox" +
                            " AND a.cBilledxx = '0'" +
                            " AND b.cTranStat <> '3'" +
                            " AND b.dTransact BETWEEN '2025-04-01' AND '2025-04-30'" +
                        " ORDER BY sChargeNo";
        
        ResultSet loRS = oApp.executeQuery(lsSQL);
        
        if (MiscUtil.RecordCount(loRS) <= 0){
            System.out.println("No record found.");
            System.exit(0);
        }
        
        oApp.beginTrans();
        
        try {
            while (loRS.next()){
                lsSQL = "INSERT INTO Employee_Advances SET" +
                        "  sTransNox = " + SQLUtil.toSQL(MiscUtil.getNextCode("Employee_Advances", "sTransNox", true, oApp.getConnection(), oApp.getBranchCode())) +
                        ", dTransact = " + SQLUtil.toSQL(loRS.getString("dTransact")) +
                        ", sEmployID = " + SQLUtil.toSQL(loRS.getString("sClientID")) +
                        ", nAmountxx = " + loRS.getDouble("nTranTotl") +
                        ", sRemarksx = " + SQLUtil.toSQL("LP Cafeteria Charge Invoice: " + loRS.getString("dTransact")) +
                        ", sSourceCD = 'LPCI'" +
                        ", sSourceNo = " + SQLUtil.toSQL(loRS.getString("sTransNox")) +
                        ", sEntryByx = " + SQLUtil.toSQL(oApp.getUserID()) +
                        ", dEntryDte = " + SQLUtil.toSQL(oApp.getServerDate()) +
                        ", sApproved = " + SQLUtil.toSQL(oApp.getUserID()) +
                        ", dApproved = " + SQLUtil.toSQL("2025-04-30") +
                        ", cTranStat = '2'" + 
                        ", sModified = " + SQLUtil.toSQL(oApp.getUserID()) +
                        ", dModified = " + SQLUtil.toSQL(oApp.getServerDate());
                
                if (oApp.executeQuery(lsSQL, "Employee_Advances", oApp.getBranchCode(), "") <= 0){
                    oApp.rollbackTrans();
                    System.err.println(oApp.getErrMsg());
                    System.exit(1);
                }
                
                lsSQL = "UPDATE CASys_DBF_LP.Charge_Invoice SET" +
                            "  cBilledxx = '1'" +
                            ", dBilledxx = " + SQLUtil.toSQL(oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));
                
                if (oApp.executeQuery(lsSQL, "CASys_DBF_LP.Charge_Invoice", oApp.getBranchCode(), "") <= 0){
                    oApp.rollbackTrans();
                    System.err.println(oApp.getErrMsg());
                    System.exit(1);
                }
            }
        } catch (SQLException e) {
            oApp.rollbackTrans();
            System.err.println(e.getMessage());
            System.exit(1);
        }
        
        
        oApp.commitTrans();
        
        System.out.println("Charge invoices successfully captured.");
        System.exit(0);
    }
}
