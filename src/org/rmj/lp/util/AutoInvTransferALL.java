package org.rmj.lp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Cell;
import static org.apache.poi.ss.usermodel.CellType.STRING;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.cas.inventory.base.InvTransfer;
import org.rmj.lib.net.LogWrapper;

public class AutoInvTransferALL {

    private static LogWrapper logwrapr = new LogWrapper("AutoInvTransferALL", "Utility.log");
    static GRider instance;
    static InvTransfer poInvTransfer;

    public void setGRider(GRider foApp) {
        instance = foApp;
    }

    public static boolean processInventory() throws SQLException {
        String lsSQL;

        if (instance == null) {
            System.exit(1);
        }
        //initialize the class 
        if (!poInvTransfer.newTransaction()) {
            return false;
        }
        lsSQL = getSQ_Stocks();
        System.out.println(getSQ_Stocks());
        ResultSet loRS = instance.executeQuery(getSQ_Stocks());

        loRS.beforeFirst();
        while (loRS.next()) {
            if (poInvTransfer.setUtilityDetail(poInvTransfer.ItemCount() - 1, loRS.getString("sBarCodex"), true)) {
                poInvTransfer.setDetail(poInvTransfer.ItemCount() - 1, "nQuantity", loRS.getObject("nQtyOnHnd"));
                poInvTransfer.addDetail();
            }
        }

        return true;
    }

    public static boolean saveTransaction() {

        //set Remarks as File Name
        poInvTransfer.setMaster("sRemarksx", "Utility generated " + CommonUtils.xsDateLong(instance.getServerDate()));
        //P0W1 General Warehouse
        //P0W2 Commisary
        //setDestination
        poInvTransfer.setMaster("sDestinat", instance.getBranchCode().equals("P0W1") ? instance.getBranchCode() : "P0W2");

        instance.beginTrans();
        if (!poInvTransfer.saveTransaction()) {
            return false;
        }

        if (!poInvTransfer.closeTransaction((String) poInvTransfer.getMaster("sTransNox"))) {
            return false;

        }
        instance.commitTrans();
        return true;
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
        instance = new GRider("gRider");

        poInvTransfer = new InvTransfer(instance, instance.getBranchCode(), true);

        try {
            if (processInventory()) {
                if (saveTransaction()) {
                    System.out.println("Transaction successfully saved ! ");
                    System.exit(0);
                }
                System.exit(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(AutoInvTransferALL.class.getName()).log(Level.SEVERE, null, ex);
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        if (!instance.logUser("gRider", "M001111122")) {
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        System.exit(0);
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

    private static String getSQ_Stocks() {
        String lsSQL = "SELECT "
                + "  a.sStockIDx"
                + ", a.sBarCodex"
                + ", a.sDescript"
                + ", a.sBriefDsc"
                + ", a.sAltBarCd"
                + ", a.sCategCd1"
                + ", a.sCategCd2"
                + ", a.sCategCd3"
                + ", a.sCategCd4"
                + ", a.sBrandCde"
                + ", a.sModelCde"
                + ", a.sColorCde"
                + ", a.sInvTypCd"
                + ", a.nUnitPrce"
                + ", a.nSelPrice"
                + ", a.nDiscLev1"
                + ", a.nDiscLev2"
                + ", a.nDiscLev3"
                + ", a.nDealrDsc"
                + ", a.cComboInv"
                + ", a.cWthPromo"
                + ", a.cSerialze"
                + ", a.cUnitType"
                + ", a.cInvStatx"
                + ", a.sSupersed"
                + ", a.cRecdStat"
                + ", b.sDescript xBrandNme"
                + ", c.sDescript xModelNme"
                + ", d.sDescript xInvTypNm"
                + ", e.nQtyOnHnd"
                + ", e.nResvOrdr"
                + ", e.nBackOrdr"
                + ", e.nFloatQty"
                + ", IFNULL(e.nLedgerNo, 0) nLedgerNo"
                + ", f.sMeasurNm"
                + " FROM Inventory a"
                + " LEFT JOIN Brand b"
                + " ON a.sBrandCde = b.sBrandCde"
                + " LEFT JOIN Model c"
                + " ON a.sModelCde = c.sModelCde"
                + " LEFT JOIN Inv_Type d"
                + " ON a.sInvTypCd = d.sInvTypCd"
                + " LEFT JOIN Measure f"
                + " ON a.sMeasurID = f.sMeasurID"
                + ", Inv_Master e"
                + " WHERE a.sStockIDx = e.sStockIDx"
                + " AND e.sBranchCd = " + SQLUtil.toSQL(instance.getBranchCode())
                + " AND e.nQtyOnHnd > 0";
        if (!System.getProperty("store.inventory.type").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        }

        return lsSQL;
    }

}
