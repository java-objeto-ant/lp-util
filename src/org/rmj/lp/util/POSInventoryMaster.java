package org.rmj.lp.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.constants.Logical;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.cas.inventory.base.InvMaster;
import org.rmj.lib.net.LogWrapper;

public class POSInventoryMaster {

    private static LogWrapper logwrapr = new LogWrapper("POSBOMInventory", "Utility.log");
    static GRider instance;

    public void setGRider(GRider foApp) {
        instance = foApp;
    }

    public static void main(String[] args) {
        logwrapr.info("Process started.");

        String path;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems";
        } else {
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);
        if (!loadProperties()) {
            System.err.println("Unable to load config.");
            System.exit(1);
        } else {
            System.out.println("Config file loaded successfully.");
        }
        instance = new GRider("General");

        if (!instance.logUser("General", "M001111122")) {
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        if (!processInventory()) {
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);

        }
    }

    private static boolean processInventory() {
        String lsSQL;
        ResultSet loRSBranch;
        ResultSet loRSInventory;
        ResultSet loRS;
        ResultSet loRSBOM;
        String loBranch;
        InvMaster loInvMaster;
        try {
            //fetch uncaptured

            lsSQL = "SELECT sBranchCD FROM Branch WHERE sBranchCD LIKE 'P0%' AND cRecdStat = " + SQLUtil.toSQL(Logical.YES);
            loRSBranch = instance.executeQuery(lsSQL);

            if (MiscUtil.RecordCount(loRSBranch) <= 0) {
                System.err.println("No Record Found!");
                return false;
            }

            loRSBranch.beforeFirst();
            while (loRSBranch.next()) {
                instance.beginTrans();

                loBranch = loRSBranch.getString("sBranchCD");
                lsSQL = getSQ_Inventory(loBranch);
                loRSInventory = instance.executeQuery(lsSQL);

                if (MiscUtil.RecordCount(loRSInventory) <= 0) {
                    System.err.println("No Inventory Record Found! for" + loRSBranch.getString("sBranchCD"));
                    continue;
                }

                loRSInventory.beforeFirst();
                while (loRSInventory.next()) {

                    //inventory master checking
                    String lsStockID = loRSInventory.getString("sStockIDx");
                    boolean lbisWithBOMx = loRSInventory.getString("cWithBOMx").equals("1");
                    loInvMaster = new InvMaster(instance, loBranch, true);
                    loInvMaster.NewRecord();

                    if (loInvMaster.SearchInventory(lsStockID, true, true)) {
                        loInvMaster.setMaster("sBranchCd", loBranch);
                        loInvMaster.setMaster("dAcquired", instance.getServerDate());
                        loInvMaster.setMaster("dBegInvxx", instance.getServerDate());

                        if (!loInvMaster.SaveRecord()) {
                            System.err.println(loInvMaster.getErrMsg().isEmpty() ? loInvMaster.getMessage() : loInvMaster.getErrMsg());
                            return false;

                        }

                        loInvMaster = null;
                    }
                    if (lbisWithBOMx) {
                        //check if has  sub unit entry BOM
                        lsSQL = "SELECT sStockIDx, nEntryNox, nQuantity"
                                + " FROM Inventory_Sub_Unit"
                                + " WHERE sStockIDX = " + SQLUtil.toSQL(lsStockID)
                                + " ORDER BY nEntryNox";
                        loRS = instance.executeQuery(lsSQL);

                        if (MiscUtil.RecordCount(loRS) <= 0) {
                            System.err.println("No-BOM Detail Record Found!" + lsStockID);
                            continue;
                        } else {
                            loRS.beforeFirst();
                            while (loRS.next()) {
                                String lsSubStockID = loRS.getString("sStockIDx");

                                //check if has master inventory of BOM for saving 
                                lsSQL = "SELECT sStockIDx, sBranchCd, dBegInvxx"
                                        + " FROM Inv_Master"
                                        + " WHERE sBranchCd = " + SQLUtil.toSQL(loBranch)
                                        + " AND sStockIDX = " + SQLUtil.toSQL(lsSubStockID);
                                loRSBOM = instance.executeQuery(lsSQL);

                                if (MiscUtil.RecordCount(loRS) <= 0) {
                                    loInvMaster = new InvMaster(instance, loBranch, true);
                                    loInvMaster.NewRecord();

                                    if (loInvMaster.SearchInventory(loRSBOM.getString("sStockIDx"), true, true)) {
                                        loInvMaster.setMaster("sBranchCd", loBranch);
                                        loInvMaster.setMaster("dAcquired", instance.getServerDate());
                                        loInvMaster.setMaster("dBegInvxx", instance.getServerDate());

                                        if (!loInvMaster.SaveRecord()) {
                                            System.err.println(loInvMaster.getErrMsg().isEmpty() ? loInvMaster.getMessage() : loInvMaster.getErrMsg());
                                            return false;
                                        }
                                    }
                                    loInvMaster = null;
                                }
                            }
                            loRS.close();
                        }
                    }
                }

                loRSInventory.close();
                instance.commitTrans();
            }

            loRSBranch.close();
        } catch (SQLException ex) {
            Logger.getLogger(POSInventoryMaster.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public static boolean loadProperties() {
        try {
            Properties po_props = new Properties();
            po_props.load(new FileInputStream("D:\\GGC_Java_Systems\\config\\rmj.properties"));

            System.setProperty("app.debug.mode", po_props.getProperty("app.debug.mode"));
            System.setProperty("user.id", po_props.getProperty("user.id"));
            System.setProperty("app.product.id", po_props.getProperty("app.product.id"));

            if (System.getProperty("app.product.id").equalsIgnoreCase("integsys")) {
                System.setProperty("pos.clt.nm", po_props.getProperty("pos.clt.nm.integsys"));
            } else {
                System.setProperty("pos.clt.nm", po_props.getProperty("pos.clt.nm.telecom"));
            }

            System.setProperty("pos.clt.tin", po_props.getProperty("pos.clt.tin"));
            System.setProperty("pos.clt.crm.no", po_props.getProperty("pos.clt.crm.no"));
            System.setProperty("pos.clt.dir.ejournal", po_props.getProperty("pos.clt.dir.ejournal"));

            //store info
            System.setProperty("store.commissary", po_props.getProperty("store.commissary"));
            System.setProperty("store.inventory.type", po_props.getProperty("store.inventory.type"));
            System.setProperty("store.inventory.strict.type", po_props.getProperty("store.inventory.strict.type"));
            System.setProperty("store.inventory.type.stock", po_props.getProperty("store.inventory.type.stock"));
            System.setProperty("store.inventory.type.product", po_props.getProperty("store.inventory.type.product"));

            //UI
            System.setProperty("app.product.id.grider", po_props.getProperty("app.product.id.grider"));
            System.setProperty("app.product.id.general", po_props.getProperty("app.product.id.general"));
            System.setProperty("app.product.id.integsys", po_props.getProperty("app.product.id.integsys"));
            System.setProperty("app.product.id.telecom", po_props.getProperty("app.product.id.telecom"));

            return true;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static String getSQ_Sales() {
        String lsSQL = "SELECT"
                + ", a.sTransNox"
                + ", a.dTransact"
                + ", b.sStockIDx"
                + ", b.nUnitPrce"
                + ", c.sBarCodex"
                + ", c.sDescript"
                + ", c.sCategCd4"
                + ", c.sInvTypCd"
                + ", SUM(b.nQuantity) nQuantity"
                + ", IFNULL(c.cWithBOMx,0) cWithBOMx"
                + " FROM SO_Master a"
                + " LEFT JOIN SO_Detail b on a.sTransNox = b.sTransNox"
                + "     AND b.cReversex = '+'"
                + " LEFT JOIN Inventory c on b.sStockIDx = c.sStockIDx"
                + "  GROUP BY a.sTransNox,b.sStockIDx ORDER BY a.sTransNox,a.dTransact ASC";

        return lsSQL;

    }

    private static String getSQ_Inventory(String fsBranch) {
        String lsSQL = "SELECT"
                + " sStockIDx"
                + ", sBarCodex"
                + ", sDescript"
                + ", cWithBOMx"
                + " FROM Inventory a"
                + " WHERE sStockIDx NOT IN(SELECT sStockIDx FROM"
                + " Inv_Master WHERE sBranchCd =" + SQLUtil.toSQL(fsBranch)
                + " ) AND sStockIDx LIKE 'POS%'"
                + " ORDER BY sDescript ASC";

        return lsSQL;

    }
}
