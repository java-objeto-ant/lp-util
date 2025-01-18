package org.rmj.lp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.rmj.cas.inventory.base.InvAdjustment;
import org.rmj.lib.net.LogWrapper;

public class AutoInvAdjustment extends Application {

    private static LogWrapper logwrapr = new LogWrapper("Auto_Inventory_Adjustment", "Utility.log");
    static GRider instance;
    InvAdjustment poInvAdjustment;

    public void setGRider(GRider foApp) {
        instance = foApp;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = new GRider("gRider");
        if (getFileAdjustment(primaryStage)) {
            if (!(poInvAdjustment.ItemCount() <= 0)) {
                if (!saveTransaction()) {
                    System.out.println("Unable to Save Transaction!! "
                            + poInvAdjustment.getMessage());
                    System.exit(1);
                }
            }
        }

        System.exit(0);
    }

    public InvAdjustment getInvAdjustment() {
        return poInvAdjustment;
    }

    public boolean getFileAdjustment(Stage fsStage) {
        // Initialize FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("d:\\"));
        fileChooser.setTitle("Inventory Adjustment File");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel files (*.xlsx)", "*.xlsx");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show the save dialog
        File file = fileChooser.showOpenDialog(fsStage);

        return processAdjustment(file);

    }

    public boolean processAdjustment(File fsFile) {

        poInvAdjustment = new InvAdjustment(instance, instance.getBranchCode(), true);
        if (fsFile != null) {
            if (!fsFile.getPath().endsWith(".xlsx")) {
                fsFile = new File(fsFile.getPath() + ".xlsx");
            }

            if (fsFile == null) {
                logwrapr.severe("No File found!" + instance.getErrMsg());
                return false;
            }

            // Check if file has the correct extension
            if (!fsFile.getPath().endsWith(".xlsx")) {
                System.err.println("Invalid file format. Please select an .xlsx file.");
                return false;
            }
            //initialize the class 
            if (!poInvAdjustment.newTransaction()) {
                return false;
            }
            //set Remarks as File Name
            poInvAdjustment.setMaster("sRemarksx", fsFile.getName());

            //Set Detail
            try (FileInputStream fis = new FileInputStream(fsFile); Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet loWBSheet = workbook.getSheetAt(0);
                Object loValue;
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                String loCredit;
                String loDebit;
                String loBarcode;

                for (Row lnRow : loWBSheet) {

                    Cell loIndexCell01 = lnRow.getCell(1);
                    //update kung alin tlga ung kkunin haha
                    Cell loIndexCell02 = lnRow.getCell(9);

                    if (loIndexCell02 != null) {
                        switch (loIndexCell02.getCellType()) {
                            case STRING:
                                loValue = loIndexCell02.getStringCellValue();
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(loIndexCell02)) {
                                    loValue = loIndexCell02.getDateCellValue();
                                } else {
                                    loValue = loIndexCell02.getNumericCellValue();
                                }
                                break;
                            case FORMULA:
                                // Evaluate the formula and get the result
                                CellValue loCellValue = formulaEvaluator.evaluate(loIndexCell02);
                                loValue = loCellValue.getNumberValue();
                                break;
                            default:
                                loValue = 0.0;
                        }

                        if (loValue != null || loValue == "") {
                            double lnValue;
                            try {
                                lnValue = Double.valueOf(loValue.toString());
                            } catch (NumberFormatException e) {
                                lnValue = 0;
                            }

                            if (lnValue != 0.0) {
                                //get Detail
                                if (loIndexCell01 != null) {
                                    switch (loIndexCell01.getCellType()) {
                                        case STRING:
                                            loValue = loIndexCell01.getStringCellValue();
                                            break;
                                        default:
                                            loValue = "";
                                    }

                                    if (loValue != null || loValue == "") {
                                        if (!poInvAdjustment.setUtilityDetail(poInvAdjustment.ItemCount() - 1,
                                                loValue.toString(), true)) {
                                            //skip the current row
                                            continue;
                                        }
                                        if (lnValue > 0) {
                                            poInvAdjustment.setDetail(poInvAdjustment.ItemCount() - 1, "nCredtQty", lnValue);
                                        } else {
                                            // Ensure the value is positive for Debit Quantity
                                            lnValue = Math.abs(lnValue);
                                            System.out.println("lnValue after Math.abs: " + lnValue);
                                            poInvAdjustment.setDetail(poInvAdjustment.ItemCount() - 1, "nDebitQty", lnValue);
                                        }

//                                        //-2 because it has autoadd
//                                        loCredit = poInvAdjustment.getDetail(poInvAdjustment.ItemCount() - 2, "nCredtQty").toString();
//                                        loDebit = poInvAdjustment.getDetail(poInvAdjustment.ItemCount() - 2, "nDebitQty").toString();
//                                        loBarcode = poInvAdjustment.getDetailOthers(poInvAdjustment.ItemCount() - 2, "sBarCodex").toString();
//
//                                        System.out.println(poInvAdjustment.ItemCount() - 1 + " Barcode  = " + loBarcode + ", Credit = " + loCredit + ", Debit =" + loDebit);
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (SQLException ex) {
                Logger.getLogger(AutoInvAdjustment.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

    public boolean saveTransaction() {
        instance.beginTrans();
        if (!poInvAdjustment.saveTransaction()) {
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

        if (!instance.logUser("gRider", "M001111122")) {
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        launch(args);
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
}
