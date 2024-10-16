package org.rmj.lp.util;

import java.sql.ResultSet;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.net.LogWrapper;

public class update_transfer_cost {
    public static void main (String [] args){
        LogWrapper logwrapr = new LogWrapper("update_transfer_cost", "Utility.log");
        logwrapr.info("Process started.");
        
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);
        
        GRiderX instance = new GRiderX("gRider");
        
        if (!instance.logUser("gRider", "M001111122")){
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        String lsSQL = "SELECT" +
                            "  a.sTransNox" +
                            ", a.dTransact" +
                            ", a.dModified" +
                            ", c.sBranchNm" +
                            ", b.sBranchNm" +
                            ", a.nTranTotl" +
                            ", a.sBranchCd" +
                            ", a.sDestinat" +
                        " FROM Inv_Transfer_Master a" +
                            " LEFT JOIN Branch c ON LEFT(a.sTransNox,4) = c.sBranchCd" +
                            ", Branch b" +
                        " WHERE a.sDestinat = b.sBranchCd" +
                        " AND a.dTransact >= '2024-06-01'" +
                        " AND a.cTranStat <> 3" +
                        " AND LEFT(a.sTransNox,4) IN('P0W1','PR01')" +
                        " ORDER BY c.sBranchNm, a.dTransact";
        
        ResultSet loMaster = instance.executeQuery(lsSQL);
        
        instance.beginTrans();
        try {
            while(loMaster.next()){
                lsSQL = "SELECT" +
                            "  a.sStockIDx" +
                            ", a.nEntryNox" +
                            ", a.nQuantity" +
                            ", a.nEntryNox" +
                            ", a.nInvCostx" +
                            ", b.nUnitPrce" +
                        " FROM Inv_Transfer_Detail a" +
                            ", Inventory b" +
                        " WHERE a.sStockIDx = b.sStockIDx" +
                            " AND a.sTransNox = " + SQLUtil.toSQL(loMaster.getString("sTransNox"));
                
                ResultSet loDetail = instance.executeQuery(lsSQL);
                
                double lnTotal = 0.00;
                double lnAmntx = 0.00;
                
                while(loDetail.next()){
                    lnAmntx = loDetail.getDouble("nUnitPrce");
                    
                    lsSQL = "UPDATE Inv_Transfer_Detail SET" +
                                "  nInvCostx = " + lnAmntx +
                                ", dModified = " + SQLUtil.toSQL(instance.getServerDate()) +
                            " WHERE sTransNox = " + SQLUtil.toSQL(loMaster.getString("sTransNox")) +
                                " AND sStockIDx = " + SQLUtil.toSQL(loDetail.getString("sStockIDx")) +
                                " AND nEntryNox = " + loDetail.getInt("nEntryNox");
                    
                    if (instance.executeQuery(lsSQL, "Inv_Transfer_Detail", loMaster.getString("sBranchCd"), loMaster.getString("sDestinat")) <= 0){
                        System.err.println("Unable to update detail...");
                        instance.rollbackTrans();
                        System.exit(1);
                    }
                    
                    lnTotal += lnAmntx * loDetail.getDouble("nQuantity");
                }
                
                lsSQL = "UPDATE Inv_Transfer_Master SET " +
                            "  nTranTotl = " + lnTotal +
                            ", dModified = " + SQLUtil.toSQL(instance.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(loMaster.getString("sTransNox"));
                
                if (instance.executeQuery(lsSQL, "Inv_Transfer_Master", loMaster.getString("sBranchCd"), loMaster.getString("sDestinat")) <= 0){
                    System.err.println("Unable to update master...");
                    instance.rollbackTrans();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            instance.rollbackTrans();
            System.exit(1);
        }
        
        instance.commitTrans();
        System.exit(0);
    }    
}
