package org.rmj.lp.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.constants.RecordStatus;

//Run Only in POS Data
public class InventoryTempStockID {

    static GRider instance;

    public static void main(String[] args) {
        String path;

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems";
        } else {
            path = "/srv/GGC_Java_Systems";
        }

        System.setProperty("sys.default.path.config", path);
        
        instance = new GRider("gRider");
        if (!instance.loadEnv("gRider")) {
            System.exit(1);
        }
        if (!instance.loadUser("gRider", "M001111122")) {
            System.exit(1);
        }

        //check Inventory List not including POS and Dummy sStockID
        String lsSQL = "SELECT "
                + " i.sStockIDx "
                + ", i.sBarcodex "
                + ", i.sDescript "
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
                + " WHERE i.sStockIDx NOT LIKE 'POS%' "
                + " AND i.sStockIDx NOT LIKE '______X%'"
                + " GROUP BY i.sStockIDx, i.sBarcodex, i.sDescript"
                + " ORDER BY i.sDescript";

        ResultSet loRS = instance.executeQuery(lsSQL);
        System.out.println(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            System.out.println("No record found.");
            System.exit(0);
        }

        instance.beginTrans();

        try {
            while (loRS.next()) {
                String lsTempStockIDx = loRS.getString("sStockIDx");
                String lsTempBarcode = loRS.getString("sBarcodex");

                if (lsTempStockIDx != null && lsTempStockIDx.length() >= 7) {
                    lsTempStockIDx = lsTempStockIDx.substring(0, 6) + "X" + lsTempStockIDx.substring(7);
                }
                if (lsTempBarcode != null && lsTempBarcode.length() >= 7) {
                    lsTempBarcode = lsTempBarcode.substring(0, 6) + "X" + lsTempBarcode.substring(7);
                }

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
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Combo_Meals", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Combo_Meals");
                    }
                }
                //Sales_Promo Table
                if (lbHasSalesPromo) {
                    lsSQL = "UPDATE Sales_Promo SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Sales_Promo", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Sales_Promo");
                    }
                }

                //Promo_Discount_Detail Table
                if (lbHasPromoDiscountDetail) {
                    lsSQL = "UPDATE Promo_Discount_Detail SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Promo_Discount_Detail", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Promo_Discount_Detail");
                    }
                }

                //Promo_Discount Table
                if (lbHasPromoDiscount) {
                    lsSQL = "UPDATE Promo_Discount SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Promo_Discount", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Promo_Discount");
                    }
                }

                //Promo_Add_On_Detail Table
                if (lbHasPromoAddOnDetail) {
                    lsSQL = "UPDATE Promo_Add_On_Detail SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Promo_Add_On_Detail", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Promo_Add_On_Detail");
                    }
                }

                //Promo_Add_On Table
                if (lbHasPromoAddOn) {
                    lsSQL = "UPDATE Promo_Add_On SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Promo_Add_On", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Promo_Add_On");
                    }
                }

                //Return_Detail Table
                if (lbHasReturnDetail) {
                    lsSQL = "UPDATE Return_Detail SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Return_Detail", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Return_Detail");
                    }
                }

                //Order_Split_Detail Table
                if (lbHasOrderSplitDetail) {
                    lsSQL = "UPDATE Order_Split_Detail SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Order_Split_Detail", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Order_Split_Detail");
                    }
                }
                //Price_History Table
                if (lbHasPriceHistory) {
                    lsSQL = "UPDATE Price_History SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Price_History", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Price_History");
                    }
                }

                //SO_Detail Table
                if (lbHasSODetail) {
                    lsSQL = "UPDATE SO_Detail SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "SO_Detail", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "SO_Detail");
                    }
                }

                //Inventory_Master Table
                if (lbHasInventoryMaster) {
                    lsSQL = "UPDATE Inventory_Master SET"
                            + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                    if (instance.executeQuery(lsSQL, "Inventory_Master", instance.getBranchCode(), "") <= 0) {
                        System.err.println(instance.getErrMsg() + "Inventory_Master");
                    }
                }

                //Inventory Table
                lsSQL = "UPDATE Inventory SET"
                        + "  sStockIDx = " + SQLUtil.toSQL(lsTempStockIDx)
                        + ",  sBarcodex = " + SQLUtil.toSQL(lsTempBarcode)
                        + ",  cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                        + " WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                if (instance.executeQuery(lsSQL, "Inventory", instance.getBranchCode(), "") <= 0) {
                    instance.rollbackTrans();
                    System.err.println(instance.getErrMsg() + "Inventory");
                    System.exit(1);
                }

            }
        } catch (SQLException e) {
            instance.rollbackTrans();
            System.err.println(e.getMessage());
            System.exit(1);
        }

        instance.commitTrans();

        System.out.println("Inventory Stock ID has successfully converted into dummy.");
        System.exit(0);
    }

    
}
