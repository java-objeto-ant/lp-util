package org.rmj.lp.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.cas.inventory.base.InvTransfer;
import static org.rmj.lp.util.DSUpload.oApp;

public class AutoPostTransfers {
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
        
        loadProperties();
        
        try {
            String lsSQL = "SELECT * FROM Inv_Transfer_Master" +
                            " WHERE sDestinat = " + SQLUtil.toSQL(oApp.getBranchCode()) +
                                " AND cTranStat NOT IN ('2', '3')" +
                            " ORDER BY dTransact, sTransNox";
        
            ResultSet loRS = oApp.executeQuery(lsSQL);
            
            if (MiscUtil.RecordCount(loRS) <= 0){
                System.out.println("No record to update.");
                System.exit(0);
            }
            
            InvTransfer loTrans;
            
            while (loRS.next()){
                loTrans = new InvTransfer(oApp, oApp.getBranchCode(), false);
                
                if (loTrans.openTransaction(loRS.getString("sTransNox"))){
                    if (loTrans.postTransaction(loRS.getString("sTransNox"), loRS.getDate("dTransact"))){
                        System.out.println(loTrans.getMaster("sTransNox") + " posted successfully.");
                    } else {
                        System.err.println("Unable to post " + loTrans.getMaster("sTransNox") + ".");
                        System.err.println(loTrans.getMessage() + "; " + loTrans.getErrMsg());
                        System.exit(1);
                    }
                }
            }
            System.exit(0);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static boolean loadProperties() {
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
}
