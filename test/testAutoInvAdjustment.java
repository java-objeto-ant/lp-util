
import java.io.File;
import java.io.FileInputStream;
import org.rmj.appdriver.GRider;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rmj.cas.inventory.base.InvAdjustment;
import org.rmj.lp.util.AutoInvAdjustment;

public class testAutoInvAdjustment {

    static GRider instance;
    static AutoInvAdjustment loAutoInvAdjustment;
    String psFilePath = "C:\\Users\\Maynard\\Downloads\\Consolidation of Gen and Comm warehouse template.xlsx";

    @BeforeClass
    public static void setUpClass() {
        String path;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems";
        } else {
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);

        instance = new GRider("gRider");
        loAutoInvAdjustment = new AutoInvAdjustment();
        loAutoInvAdjustment.setGRider(instance);
        loAutoInvAdjustment.loadProperties();
    }

    @Test
    public void testProcess() throws IOException {
        boolean result;

        File loFile = new File(psFilePath);
        if (!loFile.exists()) {
            Assert.fail("Test file not found: " + psFilePath);
        }
        result = loAutoInvAdjustment.processAdjustment(loFile);
        if (!result) {
            Assert.fail("Unable to Process Adjustment!");
        }
        assertTrue(result);
    }

    @Test
    public void testProcessedEntry() {
        File fsFile = new File(psFilePath);
        if (!fsFile.exists()) {
            Assert.fail("Test file not found: " + psFilePath);
        }
        if (fsFile != null) {
            if (!fsFile.getPath().endsWith(".xlsx")) {
                fsFile = new File(fsFile.getPath() + ".xlsx");
            }

            if (fsFile == null) {
                Assert.fail("Unable to No file Found!");
            }

            // Check if file has the correct extension
            if (!fsFile.getPath().endsWith(".xlsx")) {
                Assert.fail("Invalid file format. Please select an .xlsx file.");
            }
//            loAutoInvAdjustment.getInvAdjustment().setMaster("sRemarksx", fsFile.getName());

            //Set Detail
            try (FileInputStream fis = new FileInputStream(fsFile); Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet loWBSheet = workbook.getSheetAt(0);
                Object loValue;
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                int lnCtr;
                for (Row lnRow : loWBSheet) {
                    lnCtr = 0;
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
                                        // Skip the last index
                                        if (lnCtr == loAutoInvAdjustment.getInvAdjustment().ItemCount() - 1) {
                                            break; // Exit the loop for the last index
                                        }

                                        Object detailValue = loAutoInvAdjustment.getInvAdjustment().getDetail(lnCtr, "sBarCodex");
                                        if (detailValue != null && !detailValue.equals(loValue)) {
                                            continue; // Skip mismatched rows
                                        }
                                        if (lnValue > 0) {
                                            //this validation depndent on stock available 
                                            if (!loAutoInvAdjustment.getInvAdjustment().getDetail(lnCtr, "nCredtQty").equals(lnValue)) {
                                                Assert.fail("Invalid Excel Amount to Setted Data!"
                                                        + loAutoInvAdjustment.getInvAdjustment().getDetail(lnCtr, "sBarCodex")
                                                        + ",Value of Excel = " + loValue
                                                        + ", Setted Value of  = " + loAutoInvAdjustment.getInvAdjustment().getDetail(lnCtr, "nCredtQty"));
                                            }
                                        } else {
                                            // Ensure the value is positive for Debit Quantity
                                            lnValue = Math.abs(lnValue);
                                            if (!loAutoInvAdjustment.getInvAdjustment().getDetail(lnCtr, "nDebitQty").equals(lnValue)) {
                                                Assert.fail("Invalid Excel Amount to Setted Data!"
                                                        + loAutoInvAdjustment.getInvAdjustment().getDetail(lnCtr, "sBarCodex")
                                                        + ",Value of Excel = " + loValue
                                                        + ", Setted Value of  = " + loAutoInvAdjustment.getInvAdjustment().getDetail(lnCtr, "nDebitQty"));

                                            }
                                        }

                                        lnCtr++;
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
            }
        }
    }

    @Test
    public void testSaveTransaction() throws IOException {
        boolean result;

        result = loAutoInvAdjustment.saveTransaction();
        if (!result) {
            Assert.fail("Unable to Save Adjustment!");
        }
        assertTrue(result);
    }
}
