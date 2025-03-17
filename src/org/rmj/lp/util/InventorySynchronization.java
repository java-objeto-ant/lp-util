package org.rmj.lp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Cell;
import static org.apache.poi.ss.usermodel.CellType.FORMULA;
import static org.apache.poi.ss.usermodel.CellType.NUMERIC;
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
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.lib.net.LogWrapper;

public class InventorySynchronization extends Application {

    private static LogWrapper logwrapr = new LogWrapper("InventorySynchronization", "Utility.log");

    static GRider instance;

    public void setGRider(GRider finstance) {
        instance = finstance;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = new GRider("gRider");
        if (getSynchronizationFile(primaryStage)) {

        }

        System.exit(0);
    }

    public boolean getSynchronizationFile(Stage fsStage) {
        // Initialize FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("d:\\"));
        fileChooser.setTitle("Inventory Synchronization File");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel files (*.xlsx)", "*.xlsx");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show the save dialog
        File file = fileChooser.showOpenDialog(fsStage);

        return processSynchronization(file);

    }

    public boolean processSynchronization(File fsFile) {
        String lsSQL;
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

            List<List<Object>> laExcelData = new ArrayList<>();

            try (FileInputStream fis = new FileInputStream(fsFile); Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet loWBSheet = workbook.getSheetAt(0);

                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

                //Read & Parse into Arraylist
                for (Row lnRow : loWBSheet) {
                    List<Object> laRowData = new ArrayList<>();
                    for (Cell loCell : lnRow) {
                        Object loValue = getCellValue(loCell, formulaEvaluator);
                        if (loValue != null && !loValue.toString().trim().isEmpty()) {
                            laRowData.add(loValue);
                        }
                    }
                    laExcelData.add(laRowData);
                }

                System.out.println(laExcelData.size());
                if (laExcelData.size() > 0) {

                    instance.beginTrans();
                    for (int lnCtr = 1; lnCtr < laExcelData.size(); lnCtr++) {

                        String lsOldBarcode = laExcelData.get(lnCtr).get(0).toString();

                        if (lsOldBarcode.contains("deletion") || lsOldBarcode.length() < 12 || lsOldBarcode.length() > 12) {
                            System.out.println("invalid barcode input" + lsOldBarcode);
                            continue;
                        }
                        String lsOldDescription = laExcelData.get(lnCtr).get(1).toString();
                        String lsNewStockID = laExcelData.get(lnCtr).get(2).toString();
                        String lsNewBarcode = laExcelData.get(lnCtr).get(3).toString();
                        String lsNewDescription = laExcelData.get(lnCtr).get(4).toString();
                        String lsBriefDsc = lsNewDescription.length() >= 14
                                ? lsNewDescription.substring(0, 14)
                                : lsNewDescription; // Use the full string if it's shorter

                        String lsCategory = laExcelData.get(lnCtr).get(5).toString();
                        if (lsCategory.length() == 3) {
                            lsCategory = "0" + lsCategory;
                        }
                        String lsSellingPrice = laExcelData.get(lnCtr).get(7).toString();
                        double lnSellingPrice;
                        try {
                            lnSellingPrice = Double.parseDouble(lsSellingPrice);
                        } catch (NumberFormatException e) {
                            lnSellingPrice = 0.0;
                        }

                        lsSQL = "SELECT  "
                                + " i.sStockIDx "
                                + ", i.sBarcodex "
                                + ", i.sDescript "
                                + ", i.sBriefDsc "
                                + ", i.sCategrID "
                                + ", i.sSizeIDxx "
                                + ", i.sMeasurID "
                                + ", i.sInvTypID "
                                + ", i.nUnitPrce "
                                + ", i.nSelPrice "
                                + ", i.nDiscLev1 "
                                + ", i.nDiscLev2 "
                                + ", i.nDiscLev3 "
                                + ", i.nDealrDsc "
                                + ", i.cComboMlx "
                                + ", i.cWthPromo "
                                + ", i.sImgePath "
                                + ", i.dPricexxx "
                                + ", i.cRecdStat "
                                + ", COUNT(cm.sStockIDx)  ComboMeal"
                                + ", COUNT(sp.sStockIDx)  SalesPromo"
                                + ", COUNT(pdd.sStockIDx) PromoDiscountDetail"
                                + ", COUNT(pd.sStockIDx)  PromoDiscount"
                                + ", COUNT(pad.sStockIDx) PromoAddOnDetail"
                                + ", COUNT(pa.sStockIDx)  PromoAddOn"
                                + ", COUNT(rd.sStockIDx)  ReturnDetail"
                                + ", COUNT(osd.sStockIDx) OrderSplitDetail"
                                + ", COUNT(ph.sStockIDx)  PriceHistory"
                                + ", COUNT(sod.sStockIDx) SODetail"
                                + ", COUNT(im.sStockIDx)  InventoryMaster"
                                + " FROM Inventory i "
                                + " LEFT JOIN Combo_Meals cm "
                                + " ON i.sStockIDx = cm.sStockIDx"
                                + " LEFT JOIN Sales_Promo sp "
                                + "  ON i.sStockIDx = sp.sStockIDx"
                                + " LEFT JOIN Promo_Discount_Detail pdd "
                                + " ON i.sStockIDx = pdd.sStockIDx"
                                + " LEFT JOIN Promo_Discount pd "
                                + " ON i.sStockIDx = pd.sStockIDx"
                                + " LEFT JOIN Promo_Add_On_Detail pad "
                                + " ON i.sStockIDx = pad.sStockIDx"
                                + " LEFT JOIN Promo_Add_On pa "
                                + " ON i.sStockIDx = pa.sStockIDx"
                                + " LEFT JOIN Return_Detail rd "
                                + " ON i.sStockIDx = rd.sStockIDx"
                                + " LEFT JOIN Order_Split_Detail osd "
                                + " ON i.sStockIDx = osd.sStockIDx"
                                + " LEFT JOIN Price_History ph "
                                + " ON i.sStockIDx = ph.sStockIDx"
                                + " LEFT JOIN SO_Detail sod "
                                + " ON i.sStockIDx = sod.sStockIDx"
                                + " LEFT JOIN Inventory_Master im "
                                + " ON i.sStockIDx = im.sStockIDx "
                                + " WHERE i.sStockIDx LIKE '______X_____' "
                                + " AND i.sDescript = " + SQLUtil.toSQL(lsOldDescription)
                                + " AND i.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                + " GROUP BY i.sStockIDx, i.sBarcodex, i.sDescript"
                                + " ORDER BY i.sDescript, COUNT(sod.sStockIDx)";

                        ResultSet loRS = instance.executeQuery(lsSQL);
                        System.out.println(lsSQL);

                        if (MiscUtil.RecordCount(loRS) <= 0) {
                            System.out.println("No record found. " + lsOldDescription);

                            //check if inventory already has the new StockID inserted// do nothing
                            lsSQL = "SELECT * "
                                    + " FROM Inventory "
                                    + " WHERE sStockIDx = " + SQLUtil.toSQL(lsNewStockID);
                            ResultSet loRSNewInventoryControl = instance.executeQuery(lsSQL);

                            if (MiscUtil.RecordCount(loRSNewInventoryControl) <= 0) {
                                lsSQL = "INSERT INTO Inventory SET "
                                        + " sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + ", sBarcodex = " + SQLUtil.toSQL(lsNewBarcode)
                                        + ", sDescript = " + SQLUtil.toSQL(lsNewDescription)
                                        + ", sBriefDsc = " + SQLUtil.toSQL(lsBriefDsc)
                                        + ", sCategrID = " + SQLUtil.toSQL(lsCategory)
                                        + ", sSizeIDxx = NULL"
                                        + ", sMeasurID = NULL"
                                        + ", sInvTypID = " + SQLUtil.toSQL("FsGd")
                                        + ", nUnitPrce = " + SQLUtil.toSQL(lnSellingPrice)
                                        + ", nSelPrice = " + SQLUtil.toSQL(lnSellingPrice)
                                        + ", nDiscLev1 = 0.00"
                                        + ", nDiscLev2 = 0.00"
                                        + ", nDiscLev3 = 0.00"
                                        + ", nDealrDsc = 0.00"
                                        + ", cComboMlx = 0"
                                        + ", cWthPromo = 0"
                                        + ", sImgePath = NULL"
                                        + ", dPricexxx = " + SQLUtil.toSQL(instance.getServerDate())
                                        + ", cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                        + ", sModified = " + SQLUtil.toSQL("JavaUTILSync")
                                        + ", dModified = " + SQLUtil.toSQL(instance.getServerDate());

                                if (instance.executeQuery(lsSQL, "Inventory", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Inventory");
                                }
                            }
                            loRSNewInventoryControl.close();
                        } else {

                            //check if inventory already has the new StockID inserted // case second run
                            lsSQL = "SELECT * "
                                    + " FROM Inventory "
                                    + " WHERE sStockIDx = " + SQLUtil.toSQL(lsNewStockID);
                            ResultSet loRSNewInventory = instance.executeQuery(lsSQL);

//                            System.out.println(lsSQL);
                            //check barcode at if already existing
                            //disable other barcode not equal to old 
                            while (loRS.next()) {

                                String lsOldStockID = loRS.getString("sStockIDx");
                                if (!loRS.getString("sBarcodex").equals(lsOldBarcode) && MiscUtil.RecordCount(loRSNewInventory) > 1) {
                                    lsSQL = "UPDATE Inventory SET "
                                            + " cRecdStat = " + SQLUtil.toSQL(RecordStatus.INACTIVE)
                                            + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                    if (instance.executeQuery(lsSQL, "Inventory", instance.getBranchCode(), "") <= 0) {
                                        System.err.println(instance.getErrMsg() + "Inventory");
                                    }

                                    continue;
                                }
                            }
                            //update the first record & affecting tables first record has most Detail
                            loRS.first();
                            String lsOldStockID = loRS.getString("sStockIDx");
                            boolean lbHasComboMeal = loRS.getDouble("ComboMeal") > 0;
                            boolean lbHasSalesPromo = loRS.getDouble("SalesPromo") > 0;
                            boolean lbHasPromoDiscountDetail = loRS.getDouble("PromoDiscountDetail") > 0;
                            boolean lbHasPromoDiscount = loRS.getDouble("PromoDiscount") > 0;
                            boolean lbHasPromoAddOnDetail = loRS.getDouble("PromoAddOnDetail") > 0;
                            boolean lbHasPromoAddOn = loRS.getDouble("PromoAddOn") > 0;
                            boolean lbHasReturnDetail = loRS.getDouble("ReturnDetail") > 0;
                            boolean lbHasOrderSplitDetail = loRS.getDouble("OrderSplitDetail") > 0;
                            boolean lbHasPriceHistory = loRS.getDouble("PriceHistory") > 0;
                            boolean lbHasSODetail = loRS.getDouble("SODetail") > 0;
                            boolean lbHasInventoryMaster = loRS.getDouble("InventoryMaster") > 0;

                            //Combo_Meal Table
                            if (lbHasComboMeal) {
                                lsSQL = "UPDATE Combo_Meals SET"
                                        + "  sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Combo_Meals", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Combo_Meals");
                                }
                            }
                            //Sales_Promo Table
                            if (lbHasSalesPromo) {
                                lsSQL = "UPDATE Sales_Promo SET"
                                        + "  sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Sales_Promo", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Sales_Promo");
                                }
                            }

                            //Promo_Discount_Detail Table
                            if (lbHasPromoDiscountDetail) {
                                lsSQL = "UPDATE Promo_Discount_Detail SET"
                                        + "  sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Promo_Discount_Detail", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Promo_Discount_Detail");
                                }
                            }

                            //Promo_Discount Table
                            if (lbHasPromoDiscount) {
                                lsSQL = "UPDATE Promo_Discount SET"
                                        + "  sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Promo_Discount", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Promo_Discount");
                                }
                            }

                            //Promo_Add_On_Detail Table
                            if (lbHasPromoAddOnDetail) {
                                lsSQL = "UPDATE Promo_Add_On_Detail SET"
                                        + "  sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Promo_Add_On_Detail", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Promo_Add_On_Detail");
                                }
                            }

                            //Promo_Add_On Table
                            if (lbHasPromoAddOn) {
                                lsSQL = "UPDATE Promo_Add_On SET"
                                        + " sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Promo_Add_On", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Promo_Add_On");
                                }
                            }

                            //Return_Detail Table
                            if (lbHasReturnDetail) {
                                lsSQL = "UPDATE Return_Detail SET"
                                        + " sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Return_Detail", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Return_Detail");
                                }
                            }

                            //Order_Split_Detail Table
                            if (lbHasOrderSplitDetail) {
                                lsSQL = "UPDATE Order_Split_Detail SET"
                                        + " sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Order_Split_Detail", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Order_Split_Detail");
                                }
                            }
                            //Price_History Table
                            if (lbHasPriceHistory) {
                                lsSQL = "UPDATE Price_History SET"
                                        + " sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Price_History", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Price_History");
                                }
                            }

                            //SO_Detail Table
                            if (lbHasSODetail) {
                                lsSQL = "UPDATE SO_Detail SET"
                                        + " sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "SO_Detail", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "SO_Detail");
                                }
                            }

                            if (MiscUtil.RecordCount(loRSNewInventory) <= 0) {
                                //Inventory_Master Table
                                if (lbHasInventoryMaster) {
                                    lsSQL = "UPDATE Inventory_Master SET"
                                            + " sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                            + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                    if (instance.executeQuery(lsSQL, "Inventory_Master", instance.getBranchCode(), "") <= 0) {
                                        System.err.println(instance.getErrMsg() + "Inventory_Master");
                                    }
                                }
                                //updating if no duplicate lsNewStockID
                                lsSQL = "UPDATE Inventory SET "
                                        + " sStockIDx = " + SQLUtil.toSQL(lsNewStockID)
                                        + ", sBarcodex = " + SQLUtil.toSQL(lsNewBarcode)
                                        + ", sDescript = " + SQLUtil.toSQL(lsNewDescription)
                                        + ", sCategrID = " + SQLUtil.toSQL(lsCategory)
                                        + ", nUnitPrce = " + SQLUtil.toSQL(lnSellingPrice)
                                        + ", nSelPrice = " + SQLUtil.toSQL(lnSellingPrice)
                                        + ", dPricexxx = " + SQLUtil.toSQL(instance.getServerDate())
                                        + ", sModified = " + SQLUtil.toSQL("JavaUTILSync")
                                        + ", dModified = " + SQLUtil.toSQL(instance.getServerDate())
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Inventory", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Inventory");
                                }

                            } else {
                                //check the barcode possible duplicate description
                                if (!loRS.getString("sBarcodex").equals(lsOldBarcode)) {
                                    loRSNewInventory.next();
                                    System.out.println("Duplicate record found. StockID =" + lsNewStockID);
                                    //updating if has duplicate NewStockID,Copy converted to New Stock ID  of other field of Dummy Data and  disable dummy

                                    lsSQL = "UPDATE Inventory SET "
                                            + " sBarcodex = " + SQLUtil.toSQL(lsNewBarcode)
                                            + ", sDescript = " + SQLUtil.toSQL(lsNewDescription)
                                            + ", sBriefDsc = " + SQLUtil.toSQL(loRS.getString("sBriefDsc"))
                                            + ", sCategrID = " + SQLUtil.toSQL(lsCategory)
                                            + ", sSizeIDxx = " + SQLUtil.toSQL(loRS.getString("sSizeIDxx") == null ? "" : loRS.getString("sSizeIDxx"))
                                            + ", sMeasurID = " + SQLUtil.toSQL(loRS.getString("sMeasurID") == null ? "" : loRS.getString("sMeasurID"))
                                            + ", sInvTypID = " + SQLUtil.toSQL(loRS.getString("sInvTypID"))
                                            + ", nUnitPrce = " + SQLUtil.toSQL(lnSellingPrice)
                                            + ", nSelPrice = " + SQLUtil.toSQL(lnSellingPrice)
                                            + ", nDiscLev1 = " + SQLUtil.toSQL(loRS.getString("nDiscLev1"))
                                            + ", nDiscLev2 = " + SQLUtil.toSQL(loRS.getString("nDiscLev2"))
                                            + ", nDiscLev3 = " + SQLUtil.toSQL(loRS.getString("nDiscLev3"))
                                            + ", nDealrDsc = " + SQLUtil.toSQL(loRS.getString("nDealrDsc"))
                                            + ", cComboMlx = " + SQLUtil.toSQL(loRS.getString("cComboMlx"))
                                            + ", cWthPromo = " + SQLUtil.toSQL(loRS.getString("cWthPromo"))
                                            + ", sImgePath = " + SQLUtil.toSQL(loRS.getString("sImgePath") == null ? "" : loRS.getString("sImgePath"))
                                            + ", dPricexxx = " + SQLUtil.toSQL(instance.getServerDate())
                                            + ", cRecdStat = " + SQLUtil.toSQL(loRS.getString("cRecdStat"))
                                            + ", sModified = " + SQLUtil.toSQL("JavaUTILSync")
                                            + ", dModified = " + SQLUtil.toSQL(instance.getServerDate())
                                            + " WHERE sStockIDx = " + SQLUtil.toSQL(lsNewStockID);

                                    if (instance.executeQuery(lsSQL, "Inventory", instance.getBranchCode(), "") <= 0) {
                                        System.err.println(instance.getErrMsg() + "Inventory");
                                        instance.rollbackTrans();
                                        System.exit(1);
                                    }

                                }
                                lsSQL = "UPDATE Inventory SET "
                                        + " cRecdStat = " + SQLUtil.toSQL(RecordStatus.INACTIVE)
                                        + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                                if (instance.executeQuery(lsSQL, "Inventory", instance.getBranchCode(), "") <= 0) {
                                    System.err.println(instance.getErrMsg() + "Inventory");
                                }
                            }
                            loRSNewInventory.close();
                        }

                    }
                }

                instance.commitTrans();
            } catch (IOException | SQLException e) {
                Logger.getLogger(InventorySynchronization.class.getName()).log(Level.SEVERE, null, e);
                return false;
            }

        }

        return true;
    }

    private static Object getCellValue(Cell foCell, FormulaEvaluator foEvaluator) {
        switch (foCell.getCellType()) {
            case STRING:
                return foCell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(foCell)) {
                    return foCell.getDateCellValue();
                } else {
                    BigDecimal bigDecimal = BigDecimal.valueOf(foCell.getNumericCellValue());
                    return bigDecimal.toPlainString();
                }
            case BOOLEAN:
                return foCell.getBooleanCellValue();
            case FORMULA:
                CellValue lfCellValue = foEvaluator.evaluate(foCell);
                return lfCellValue.getNumberValue();
            case BLANK:
            case ERROR:
            default:
                return null;
        }
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

        instance = new GRider("gRider");

        if (!instance.logUser("gRider", "M001111122")) {
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        launch(args);
    }

}
