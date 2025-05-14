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
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.Logical;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.cas.inventory.base.InvAdjustment;
import org.rmj.cas.inventory.base.InventoryTrans;
import org.rmj.cas.inventory.constants.basefx.InvConstants;
import org.rmj.lib.net.LogWrapper;

public class POSBOMInventory {

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
        if (!processSalesInventory()) {
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);

        }
    }

    private static boolean processSalesInventory() {
        String lsSQL;
        ResultSet loRS;
        ResultSet loRSSales;
        ResultSet loRSBOM;
        ResultSet loRSMaster;
        String loBranch;
        String loTransaction;
        String loDateTransaction;
        String loBOMAdjTrans;
        InvAdjustment poInvAdjustment;

        try {
            //fetch uncaptured
            lsSQL = MiscUtil.addCondition(getSQ_Master(), " a.cCaptured = " + SQLUtil.toSQL(Logical.NO)
                    + "  AND a.cTranStat <> " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED));
            loRSMaster = instance.executeQuery(lsSQL);

            if (MiscUtil.RecordCount(loRSMaster) <= 0) {
                System.err.println("No Record Found!");
                return false;
            }

            loRSMaster.beforeFirst();
            while (loRSMaster.next()) {
                int lnCtr = 0;
                loBranch = loRSMaster.getString("sBranchCd");
                loTransaction = loRSMaster.getString("sTransNox");
                loDateTransaction = loRSMaster.getString("dTransact");
                //fetch detail of sales
                lsSQL = MiscUtil.addCondition(getSQ_Sales(), " a.sTransNox = " + SQLUtil.toSQL(loTransaction));
                loRSSales = instance.executeQuery(lsSQL);
                if (MiscUtil.RecordCount(loRSSales) <= 0) {
                    System.err.println("No Detail Record Found!" + loTransaction);
                    return false;
                }
                
                /*---------------------------------------------------------------------------------
                 *   Initialize required classes
                 *---------------------------------------------------------------------------------*/
                InventoryTrans loInvTrans = new InventoryTrans(instance, loBranch);
                loInvTrans.InitTransaction();
                poInvAdjustment = new InvAdjustment(instance, loBranch, true);
                if (!poInvAdjustment.newTransaction()) {
                    continue;
                }

                /*---------------------------------------------------------------------------------
                 *   Save inventory trans of the items
                 *---------------------------------------------------------------------------------*/
                poInvAdjustment.setMaster("sRemarksx", loTransaction + " BOM Utility");
                poInvAdjustment.setMaster("dTransact", CommonUtils.toDate(loDateTransaction));
                poInvAdjustment.setMaster("sSourceNo", loTransaction);
                poInvAdjustment.setMaster("sSourceCd", InvConstants.SALES);

                instance.beginTrans();
                loRSSales.beforeFirst();
                loBOMAdjTrans = "";
                while (loRSSales.next()) {
                    //inventory master checking
                    String lsStockID = loRSSales.getString("sStockIDx");
                    double lnSalesQty = loRSSales.getDouble("nQuantity");
                    boolean lbisWithBOMx = loRSSales.getString("sStockIDx").equals("1");

                    if (lbisWithBOMx) {
                        lsSQL = "SELECT sStockIDx, nEntryNox, nQuantity"
                                + " FROM Inventory_Sub_Unit"
                                + " WHERE sStockIDX = " + SQLUtil.toSQL(lsStockID)
                                + " ORDER BY nEntryNox";
                        loRSBOM = instance.executeQuery(lsSQL);

                        if (MiscUtil.RecordCount(loRSBOM) <= 0) {
                            System.err.println("No-BOM Detail Record Found!" + lsStockID);
                            continue;
                        } else {
                            loRSBOM.beforeFirst();
                            while (loRSBOM.next()) {
                                double lnSubQty = loRSBOM.getDouble("nQuantity");
                                String lsSubStockID = loRSBOM.getString("sStockIDx");
                                if (!poInvAdjustment.setUtilityDetail(poInvAdjustment.ItemCount() - 1,
                                        lsSubStockID, true)) {
                                    //skip the current row
                                    continue;
                                }
                                poInvAdjustment.setDetail(poInvAdjustment.ItemCount() - 1, "nCredtQty", lnSalesQty * lnSubQty);

                            }

                        }
                    } else {
                        if (lsStockID.equals("")) {
                            break;
                        }

                        lsSQL = "SELECT"
                                + "  nQtyOnHnd"
                                + ", nResvOrdr"
                                + ", nBackOrdr"
                                + ", nLedgerNo"
                                + " FROM Inv_Master"
                                + " WHERE sStockIDx = " + SQLUtil.toSQL(lsStockID)
                                + " AND sBranchCd = " + SQLUtil.toSQL(loBranch);

                        loRS = instance.executeQuery(lsSQL);

                        loInvTrans.setDetail(lnCtr, "sStockIDx", lsStockID);
                        loInvTrans.setDetail(lnCtr, "nQuantity", lnSalesQty);

                        if (MiscUtil.RecordCount(loRS) == 0) {
                            loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                            loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                            loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
                        } else {
                            try {
                                loRS.first();
                                loInvTrans.setDetail(lnCtr, "nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                                loInvTrans.setDetail(lnCtr, "nResvOrdr", loRS.getDouble("nResvOrdr"));
                                loInvTrans.setDetail(lnCtr, "nBackOrdr", loRS.getDouble("nBackOrdr"));
                                loInvTrans.setDetail(lnCtr, "nLedgerNo", loRS.getInt("nLedgerNo"));
                            } catch (SQLException e) {
                                System.out.print("Please inform MIS Department.");
                                return false;
                            }
                        }
                        lnCtr++;
                    }
                }
                if (poInvAdjustment.ItemCount() > 0) {
                    poInvAdjustment.saveTransaction();
                    loBOMAdjTrans = poInvAdjustment.getMaster("sTransNox").toString();
                    if (poInvAdjustment.openTransaction(loBOMAdjTrans)) {
                        poInvAdjustment.closeTransaction(loBOMAdjTrans);
                        System.out.println("BOM Utility has Adjustment with Transaction No. =" + loBOMAdjTrans);
                    }
                }
                if (!loInvTrans.Sales(loTransaction, CommonUtils.toDate(loDateTransaction), EditMode.ADDNEW)) {
                    System.err.println(loInvTrans.getMessage());
                    System.err.println(loInvTrans.getErrMsg());
                    return false;
                }

                instance.commitTrans();
            }
        } catch (SQLException ex) {
            Logger.getLogger(POSBOMInventory.class.getName()).log(Level.SEVERE, null, ex);
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

    private static String getSQ_Master() {
        String lsSQL = "SELECT"
                + " b.sBranchCd"
                + " b.sBranchNm"
                + ", a.sTransNox"
                + ", a.dTransact"
                + " FROM SO_Master a"
                + " LEFT JOIN Branch b ON LEFT(a.sTransNox, 4) = b.sBranchCd"
                + " ORDER BY a.sTransNox,a.dTransact ASC";

        return lsSQL;

    }
}
