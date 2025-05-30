/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.lp.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.application.Application;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.WebClient;
import org.rmj.appdriver.constants.Logical;
import org.rmj.appdriver.constants.RecordStatus;

public class POSSalesUpload extends Application {

    public static GRider oApp;
    Map<String, Integer> paStockID = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) throws Exception {
        String lsSQL = "";
//        String lsAPI = "https://restgk.guanzongroup.com.ph/lospedritos/accounts/getSalesPOS.php";
        String lsAPI = "http://localhost/lospedritos/accounts/getSalesPOS.php";

        Map<String, String> headers = getAPIHeader();

        try {
            JSONObject param = new JSONObject();
            String dPOSCptrd = oApp.Config(oApp.getBranchCode(), "dPOSCptrd");

            if (dPOSCptrd == null || dPOSCptrd.trim().isEmpty()) {
                dPOSCptrd = "CURDATE()";
            } else {
                dPOSCptrd = SQLUtil.toSQL(dPOSCptrd);
            }
            lsSQL = MiscUtil.addCondition(getSQ_SaleMaster(), " cCaptured = " + Logical.NO
                    + " AND dTransact >= " + dPOSCptrd);
            System.out.println(lsSQL);
            ResultSet loRSSOMaster = oApp.executeQuery(lsSQL);

            if (MiscUtil.RecordCount(loRSSOMaster) <= 0) {
                System.out.println("No Record to upload");
                System.exit(0);
            }
            JSONObject jsonObject = new JSONObject();
            JSONArray somasterArray = new JSONArray();
            JSONArray sodetailArray = new JSONArray();
            JSONObject somasterObject = new JSONObject();
            JSONObject sodetailObject = new JSONObject();

            loRSSOMaster.beforeFirst();
            while (loRSSOMaster.next()) {
                //clear first 
                jsonObject.clear();
                somasterArray.clear();
                sodetailArray.clear();
                somasterObject.clear();
                sodetailObject.clear();
                param.clear();

                somasterObject.put("sTransNox", loRSSOMaster.getString("sTransNox"));
                somasterObject.put("dTransact", loRSSOMaster.getString("dTransact"));
                somasterObject.put("sReceiptx", loRSSOMaster.getString("sReceiptx"));
                somasterObject.put("nContrlNo", loRSSOMaster.getObject("nContrlNo"));
                somasterObject.put("nTranTotl", loRSSOMaster.getDouble("nTranTotl"));
                somasterObject.put("sCashierx", loRSSOMaster.getString("sCashierx"));
                somasterObject.put("sTableNox", loRSSOMaster.getString("sTableNox"));
                somasterObject.put("sWaiterID", loRSSOMaster.getString("sWaiterID"));
                somasterObject.put("sMergeIDx", loRSSOMaster.getString("sMergeIDx"));
                somasterObject.put("nOccupnts", loRSSOMaster.getObject("nOccupnts"));
                somasterObject.put("sOrderNox", loRSSOMaster.getString("sOrderNox"));
                somasterObject.put("sBillNmbr", loRSSOMaster.getString("sBillNmbr"));
                somasterObject.put("nPrntBill", loRSSOMaster.getObject("nPrntBill"));
                somasterObject.put("dPrntBill", loRSSOMaster.getString("dPrntBill"));
                somasterObject.put("cTranStat", loRSSOMaster.getObject("cTranStat"));
                somasterObject.put("cSChargex", loRSSOMaster.getObject("cSChargex"));
                somasterObject.put("cTranType", loRSSOMaster.getObject("cTranType"));
                somasterObject.put("cCaptured", loRSSOMaster.getObject("cCaptured"));
                somasterObject.put("dCaptured", loRSSOMaster.getString("dCaptured"));
                somasterObject.put("sCustName", loRSSOMaster.getString("sCustName"));
                somasterObject.put("sModified", loRSSOMaster.getString("sModified"));
                somasterObject.put("dModified", loRSSOMaster.getString("dModified"));

                somasterArray.add(somasterObject);

                String lsSQLDetail = MiscUtil.addCondition(getSQ_SaleDetail(), "sTransNox = " + SQLUtil.toSQL(loRSSOMaster.getString("sTransNox")));
                ResultSet loRSSODetail = oApp.executeQuery(lsSQLDetail);
                loRSSODetail.beforeFirst();
                while (loRSSODetail.next()) {

                    sodetailObject = new JSONObject();
                    sodetailObject.put("sTransNox", loRSSODetail.getString("sTransNox"));
                    sodetailObject.put("nEntryNox", loRSSODetail.getObject("nEntryNox"));
                    sodetailObject.put("sStockIDx", loRSSODetail.getString("sStockIDx"));
                    sodetailObject.put("cReversex", loRSSODetail.getString("cReversex"));
                    sodetailObject.put("nQuantity", loRSSODetail.getDouble("nQuantity"));
                    sodetailObject.put("nUnitPrce", loRSSODetail.getDouble("nUnitPrce"));
                    sodetailObject.put("nDiscount", loRSSODetail.getDouble("nDiscount"));
                    sodetailObject.put("nAddDiscx", loRSSODetail.getDouble("nAddDiscx"));
                    sodetailObject.put("nComplmnt", loRSSODetail.getDouble("nComplmnt"));
                    sodetailObject.put("cPrintedx", loRSSODetail.getString("cPrintedx"));
                    sodetailObject.put("cServedxx", loRSSODetail.getString("cServedxx"));
                    sodetailObject.put("cDetailxx", loRSSODetail.getString("cDetailxx"));
                    sodetailObject.put("sReplItem", loRSSODetail.getString("sReplItem"));
                    sodetailObject.put("cReversed", loRSSODetail.getString("cReversed"));
                    sodetailObject.put("cComboMlx", loRSSODetail.getString("cComboMlx"));
                    sodetailObject.put("cWthPromo", loRSSODetail.getString("cWthPromo"));
                    sodetailObject.put("dModified", loRSSODetail.getString("dModified"));

                    sodetailArray.add(sodetailObject);
                    paStockID.putIfAbsent(loRSSODetail.getString("sStockIDx"), 0);
                }
                jsonObject.put("SO_Detail", sodetailArray);
                jsonObject.put("SO_Master", somasterArray);

                param.put("payload", jsonObject);
                System.out.println("json object :" + param);
                String response = WebClient.sendHTTP(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);

                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(response);

                if (json == null) {
                    System.out.println("No Response");
                    System.exit(1);
                } else {
                    if (json.get("result").equals("error")) {
                        System.out.println(json.toJSONString());
                        System.exit(1);
                    } else {
                        lsSQL = "UPDATE SO_Master SET"
                                + " cCaptured = " + SQLUtil.toSQL(Logical.YES)
                                + ", dCaptured = " + SQLUtil.toSQL(oApp.getServerDate())
                                + " WHERE sTransNox =" + SQLUtil.toSQL(json.get("transno").toString());
                        if (oApp.executeQuery(lsSQL, "", "", "") <= 0) {
                            System.out.println(response);
                        }
                    }
                    if (!paStockID.isEmpty()) {
                        UploadInventory();
                    }
                }
            }
            loRSSOMaster.close();
            System.exit(0);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private void UploadInventory() {
        String lsSQL = "";
        String lsCondition = "";

//        String lsAPI = "https://restgk.guanzongroup.com.ph/lospedritos/accounts/getInventory.php";
        String lsAPI = "http://localhost/lospedritos/accounts/getInventory.php";

        Map<String, String> headers = getAPIHeader();

        try {
            JSONObject param = new JSONObject();

            if (!paStockID.isEmpty()) {
                for (Map.Entry<String, Integer> entry : paStockID.entrySet()) {
                    if (entry.getValue() == 0) {
                        lsCondition += ", " + SQLUtil.toSQL(entry.getKey());
                        paStockID.put(entry.getKey(), 1);
                    }
                }
                if (!lsCondition.isEmpty()) {
                    lsCondition = "AND sStockIDx IN (" + lsCondition.substring(2) + ") GROUP BY sStockIDx";
                }
            }

            if (lsCondition.isEmpty()) {
                return;
            }

            lsSQL = MiscUtil.addCondition(getSQ_Inventory(), "cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE) + lsCondition);
            System.out.println(lsSQL);
            ResultSet loRSInventory = oApp.executeQuery(lsSQL);

            JSONObject jsonObject = new JSONObject();
            JSONArray inventoryArray = new JSONArray();
            JSONObject inventoryObject = new JSONObject();

            while (loRSInventory.next()) {
                //clear first 
                inventoryObject.clear();
                jsonObject.clear();
                param.clear();
                inventoryArray.clear();

                inventoryObject.put("sStockIDx", loRSInventory.getString("sStockIDx"));
                inventoryObject.put("sBarCodex", loRSInventory.getString("sBarCodex"));
                inventoryObject.put("sDescript", loRSInventory.getString("sDescript"));
                inventoryObject.put("sBriefDsc", loRSInventory.getString("sBriefDsc"));
                inventoryObject.put("sCategCd4", loRSInventory.getString("sCategCd4"));
                inventoryObject.put("sMeasurID", loRSInventory.getString("sMeasurID"));
                inventoryObject.put("sInvTypCd", loRSInventory.getString("sInvTypCd"));
                inventoryObject.put("nUnitPrce", loRSInventory.getString("nUnitPrce"));
                inventoryObject.put("nSelPrice", loRSInventory.getString("nSelPrice"));
                inventoryObject.put("nDiscLev1", loRSInventory.getString("nDiscLev1"));
                inventoryObject.put("nDiscLev2", loRSInventory.getString("nDiscLev2"));
                inventoryObject.put("nDiscLev3", loRSInventory.getString("nDiscLev3"));
                inventoryObject.put("nDealrDsc", loRSInventory.getString("nDealrDsc"));
                inventoryObject.put("cComboInv", loRSInventory.getString("cComboInv"));
                inventoryObject.put("cWthPromo", loRSInventory.getString("cWthPromo"));
                inventoryObject.put("cRecdStat", loRSInventory.getString("cRecdStat"));

                inventoryArray.add(inventoryObject);

                jsonObject.put("Inventory", inventoryArray);
                param.put("payload", jsonObject);
                System.out.println("json object :" + param);
                String response = WebClient.sendHTTP(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);

                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(response);

                if (json == null) {
                    System.out.println("No Response");
                    System.exit(1);
                } else {
                    if (json.get("result").equals("error")) {
                        System.out.println(json.toJSONString());
                        System.exit(1);

                    } else {
                        System.out.println(response);
                    }
                }
            }
            loRSInventory.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        String path;

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems";
        } else {
            path = "/srv/GGC_Java_Systems";
        }

        System.setProperty("sys.default.path.config", path);

        oApp = new GRider("gRider");

        if (!oApp.loadEnv("gRider")) {
            System.exit(1);
        }
        if (!oApp.loadUser("gRider", "M001111122")) {
            System.exit(1);
        }

        launch(args);
    }

    public static HashMap getAPIHeader() {
        try {
            String clientid = "GGC_BM001";
            String productid = "gRider";
            String imei = InetAddress.getLocalHost().getHostName();
            String user = "M001111122";
            String log = "";
            System.out.println(imei);
            Calendar calendar = Calendar.getInstance();
            Map<String, String> headers
                    = new HashMap<String, String>();
            headers.put("Accept", "application/json");
            headers.put("Content-Type", "application/json");
            headers.put("g-api-id", productid);
            headers.put("g-api-imei", imei);

            headers.put("g-api-key", SQLUtil.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));
            headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String) headers.get("g-api-imei") + (String) headers.get("g-api-key")));
            headers.put("g-api-client", clientid);
            headers.put("g-api-user", user);
            headers.put("g-api-log", log);
            headers.put("g-char-request", "UTF-8");
            headers.put("g-api-token", "");

            return (HashMap) headers;
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private String getSQ_SaleMaster() {
        String lsSQL;
        lsSQL = " SELECT "
                + " sTransNox "
                + " , dTransact "
                + " , sReceiptx "
                + " , nContrlNo "
                + " , nTranTotl "
                + " , sCashierx "
                + " , sTableNox "
                + " , sWaiterID "
                + " , sMergeIDx "
                + " , nOccupnts "
                + " , sOrderNox "
                + " , sBillNmbr "
                + " , nPrntBill "
                + " , dPrntBill "
                + " , cTranStat "
                + " , cSChargex "
                + " , cTranType "
                + " , cCaptured "
                + " , dCaptured "
                + " , sCustName "
                + " , sModified "
                + " , dModified "
                + " FROM SO_Master "
                + " ORDER BY sTransNox";

        return lsSQL;
    }

    private String getSQ_SaleDetail() {
        String lsSQL;
        lsSQL = " SELECT "
                + " sTransNox "
                + " , nEntryNox "
                + " , sStockIDx "
                + " , cReversex "
                + " , nQuantity "
                + " , nUnitPrce "
                + " , nDiscount "
                + " , nAddDiscx "
                + " , nComplmnt "
                + " , cPrintedx "
                + " , cServedxx "
                + " , cDetailxx "
                + " , sReplItem "
                + " , cReversed "
                + " , cComboMlx "
                + " , cWthPromo "
                + " , dModified "
                + " FROM SO_Detail ";

        return lsSQL;

    }

    private String getSQ_Inventory() {
        String lsSQL = "SELECT"
                + " sStockIDx"
                + ", sBarCodex"
                + ", sDescript"
                + ", sBriefDsc"
                + ", sCategrID `sCategCd4`"
                + ", sMeasurID"
                + ", sInvTypID `sInvTypCd`"
                + ", nUnitPrce"
                + ", nSelPrice"
                + ", nDiscLev1"
                + ", nDiscLev2"
                + ", nDiscLev3"
                + ", nDealrDsc"
                + ", nDiscLev3"
                + ", cComboMlx `cComboInv`"
                + ", cWthPromo"
                + ", cRecdStat"
                + " FROM Inventory";

        return lsSQL;

    }
}
