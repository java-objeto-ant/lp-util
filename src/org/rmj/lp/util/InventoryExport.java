package org.rmj.lp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;
import javax.sql.CommonDataSource;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.lib.net.LogWrapper;

public class InventoryExport extends Application {

    static String filePath = "D:/GGC_Java_Systems/excel export/";
    private static LogWrapper logwrapr = new LogWrapper("InventoryExport", "Utility.log");
    static String[] headers = {"Old Barcode", "Old Description", "New Stock ID", "New Barcode", "New Description", "New Category", "Date Modified", "Selling Price",
        "SO Detail", "Combo Meal", "Sales Promo", "Promo Discount Detail ", "Promo Discount", "Promo Add On Detail", "Promo Add On", "Return Detail", "Order Split Detail"};
    static GRider instance;
    static String excelName = "";
    ResultSet loRS;

    public void setGRider(GRider finstance) {
        instance = finstance;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = new GRider("gRider");

        if (processActiveTempStock(primaryStage, ShowMessageFX.YesNo(null, "Inventory Stock Report", "Do you want to disable loaded inventory?"))) {

        }

        System.exit(0);
    }

    public boolean processActiveTempStock(Stage fsStage, Boolean fbDisable) {
        String lsSQL;

        try {
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
                    + ", i.dModified "
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
                    + " AND i.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                    + " GROUP BY i.sStockIDx, i.sBarcodex, i.sDescript"
                    + " ORDER BY i.sDescript, COUNT(sod.sStockIDx)";

            ResultSet loRS = instance.executeQuery(lsSQL);
            System.out.println(lsSQL);

            if (MiscUtil.RecordCount(loRS) <= 0) {
                System.out.println("No record found.");
            } else {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Inventory Data");

                // Create header row
                Row headerRow = sheet.createRow(0);

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(getHeaderCellStyle(workbook));
                }

                headerRow.setHeightInPoints(20);

                // Create a CellStyle with double format (e.g., two decimal places)
                CellStyle doubleStyle = workbook.createCellStyle();
                DataFormat format = workbook.createDataFormat();
                doubleStyle.setDataFormat(format.getFormat("#,##0.00")); // Adjust format as needed
                // Populate data rows
                int rowIndex = 1;
                if (fbDisable) {
                    instance.beginTrans();
                }
                loRS.beforeFirst();
                while (loRS.next()) {

                    String lsTempStockIDx = loRS.getString("sStockIDx");
                    String lsTempBarcode = loRS.getString("sBarcodex");

                    if (lsTempBarcode != null && lsTempBarcode.length() >= 7) {
                        lsTempBarcode = lsTempBarcode.substring(0, 6) + "0" + lsTempBarcode.substring(7);
                    }

                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(lsTempBarcode);
                    row.createCell(1).setCellValue(loRS.getString("sDescript"));
                    row.createCell(2).setCellValue("");
                    row.createCell(3).setCellValue("");
                    row.createCell(4).setCellValue("");
                    row.createCell(5).setCellValue(loRS.getString("sCategrID"));
                    row.createCell(6).setCellValue(loRS.getString("dModified"));
                    row.createCell(7).setCellValue(loRS.getString("nSelPrice"));
                    row.createCell(8).setCellValue(loRS.getString("SODetail"));
                    row.createCell(9).setCellValue(loRS.getString("ComboMeal"));
                    row.createCell(10).setCellValue(loRS.getString("SalesPromo"));
                    row.createCell(11).setCellValue(loRS.getString("PromoDiscountDetail"));
                    row.createCell(12).setCellValue(loRS.getString("PromoDiscount"));
                    row.createCell(13).setCellValue(loRS.getString("PromoAddOnDetail"));
                    row.createCell(14).setCellValue(loRS.getString("PromoAddOn"));
                    row.createCell(15).setCellValue(loRS.getString("ReturnDetail"));
                    row.createCell(16).setCellValue(loRS.getString("OrderSplitDetail"));

                    if (fbDisable) {
                        String lsOldStockID = (loRS.getString("sStockIDx"));
                        lsSQL = "UPDATE Inventory SET "
                                + " cRecdStat = " + SQLUtil.toSQL(RecordStatus.INACTIVE)
                                + " WHERE sStockIDx = " + SQLUtil.toSQL(lsOldStockID);

                        if (instance.executeQuery(lsSQL, "Inventory", instance.getBranchCode(), "") <= 0) {
                            System.err.println(instance.getErrMsg() + "Inventory");
                        }
                    }
                }

                if (fbDisable) {
                    instance.commitTrans();
                }

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                    int currentWidth = sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i, currentWidth + 1000);
//            System.out.println("sheet width = " + sheet.getColumnWidth(i));
                }

                // Ensure the directory exists
                File directory = new File(filePath);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // Generate a unique file name if the file already exists
                String fileFullPath = filePath + excelName;
                File file = new File(fileFullPath);
                int count = 1;

                excelName = "Inventory Temporary Stock - " + CommonUtils.xsDateLong(instance.getServerDate()) + ".xlsx";
                while (file.exists()) {
                    String baseName = excelName.contains(".")
                            ? excelName.substring(0, excelName.lastIndexOf("."))
                            : excelName;
                    String extension = excelName.contains(".")
                            ? excelName.substring(excelName.lastIndexOf("."))
                            : "";
                    fileFullPath = filePath + baseName + "-" + count + extension;
                    file = new File(fileFullPath);
                    count++;
                }
                try (FileOutputStream fileOut = new FileOutputStream(fileFullPath)) {
                    workbook.write(fileOut);
                    System.out.println("Exported to Excel successfully.");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        workbook.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Write to Excel file
            }

        } catch (SQLException e) {
            Logger.getLogger(InventoryExport.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }

        return true;
    }

    private static CellStyle getHeaderCellStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();

        // Set background color
        headerStyle.setFillForegroundColor(IndexedColors.OLIVE_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 12);
        headerStyle.setFont(font);

        // Set center alignment
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // Set borders for the header cells
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setTopBorderColor(IndexedColors.WHITE.getIndex()); // Set top border color to black
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBottomBorderColor(IndexedColors.WHITE.getIndex()); // Set bottom border color to black
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setLeftBorderColor(IndexedColors.WHITE.getIndex()); // Set left border color to black
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setRightBorderColor(IndexedColors.WHITE.getIndex()); // Set right border color to black

        return headerStyle;
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
