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

public class CIUpload extends Application {

    public static GRider oApp;

    @Override
    public void start(Stage stage) throws Exception {
        String lsAPI = "http://192.168.10.70/LosPedritos/getChargeInvoice.php";

        Map<String, String> headers = getAPIHeader();

        try {
            JSONObject param = new JSONObject();
            System.out.println(getSQ_ChargeInv());
            ResultSet loRS = oApp.executeQuery(getSQ_ChargeInv());

            JSONArray ciArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            JSONObject chargeinvObject = new JSONObject();
            JSONArray somasterArray = new JSONArray();
            JSONObject somasterObject = new JSONObject();
            while (loRS.next()) {
                 //clear first
                jsonObject.clear();
                chargeinvObject.clear();
                somasterArray.clear();
                somasterObject.clear();
                param.clear();
                
                System.out.println(loRS.getString("sTransNox"));
                chargeinvObject.put("sTransNox", loRS.getString("sTransNox"));
                chargeinvObject.put("sClientID", loRS.getString("sClientID"));
                chargeinvObject.put("sChargeNo", loRS.getString("sChargeNo"));
                chargeinvObject.put("sSourceCd", loRS.getString("sSourceCd"));
                chargeinvObject.put("sSourceNo", loRS.getString("sSourceNo"));
                chargeinvObject.put("cCollectd", loRS.getObject("cCollectd"));
                chargeinvObject.put("cBilledxx", loRS.getObject("cBilledxx"));
                chargeinvObject.put("dBilledxx", loRS.getString("dBilledxx"));
                chargeinvObject.put("cPaidxxxx", loRS.getObject("cPaidxxxx"));
                chargeinvObject.put("dPaidxxxx", loRS.getString("dPaidxxxx"));
                chargeinvObject.put("cWaivexxx", loRS.getObject("cWaivexxx"));
                chargeinvObject.put("dWaivexxx", loRS.getString("dWaivexxx"));
                chargeinvObject.put("sWaivexxx", loRS.getString("sWaivexxx"));
                chargeinvObject.put("nAmountxx", loRS.getBigDecimal("nAmountxx"));
                chargeinvObject.put("nVATSales", loRS.getBigDecimal("nVATSales"));
                chargeinvObject.put("nVATAmtxx", loRS.getBigDecimal("nVATAmtxx"));
                chargeinvObject.put("nDiscount", loRS.getBigDecimal("nDiscount"));
                chargeinvObject.put("nVatDiscx", loRS.getBigDecimal("nVatDiscx"));
                chargeinvObject.put("nPWDDiscx", loRS.getBigDecimal("nPWDDiscx"));
                chargeinvObject.put("nAmtPaidx", loRS.getBigDecimal("nAmtPaidx"));
                chargeinvObject.put("cORPrintx", loRS.getObject("cORPrintx"));
                chargeinvObject.put("cTranStat", loRS.getObject("cTranStat"));
                chargeinvObject.put("cSendStat", loRS.getObject("cSendStat"));
                chargeinvObject.put("sClientNm", loRS.getString("sClientNm"));
                chargeinvObject.put("sAddressx", loRS.getString("sAddressx"));
                chargeinvObject.put("sModified", loRS.getString("sModified"));
                chargeinvObject.put("dModified", loRS.getString("dModified"));
                ciArray.add(chargeinvObject);
                jsonObject.put("Charge_Invoice", ciArray);

                String lsSQLDetail = MiscUtil.addCondition(getSQ_SaleMaster(), "sTransNox = " + SQLUtil.toSQL(loRS.getString("sSourceNo")));
                ResultSet loRSSOMaster = oApp.executeQuery(lsSQLDetail);

                while (loRSSOMaster.next()) {

                    somasterObject.put("sTransNox", loRSSOMaster.getString("sTransNox"));
                    somasterObject.put("dTransact", loRSSOMaster.getString("dTransact"));
                    somasterObject.put("sReceiptx", loRSSOMaster.getString("sReceiptx"));
                    somasterObject.put("nContrlNo", loRSSOMaster.getObject("nContrlNo"));
                    somasterObject.put("nTranTotl", loRSSOMaster.getBigDecimal("nTranTotl"));
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
                    somasterObject.put("sCustName", loRSSOMaster.getString("sCustName"));
                    somasterObject.put("sModified", loRSSOMaster.getString("sModified"));
                    somasterObject.put("dModified", loRSSOMaster.getString("dModified"));

                    somasterArray.add(somasterObject);
                }
                jsonObject.put("SO_Master", somasterArray);

                loRSSOMaster.close();

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

                        String lsSQL = "UPDATE Charge_Invoice set cSendStat = 1 WHERE sTransNox =" + SQLUtil.toSQL(json.get("transno").toString());
                        if (oApp.executeQuery(lsSQL, "", "", "") <= 0) {
                            System.out.println(response);
                        }
                    }
                }
            }
            loRS.close();
            System.exit(0);
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

    private String getSQ_ChargeInv() {
        String lsSQL;

        lsSQL = " SELECT "
                + " sTransNox "
                + " , sClientID "
                + " , sChargeNo "
                + " , sSourceCd "
                + " , sSourceNo "
                + " , cCollectd "
                + " , cBilledxx "
                + " , dBilledxx "
                + " , cPaidxxxx "
                + " , dPaidxxxx "
                + " , cWaivexxx "
                + " , dWaivexxx "
                + " , sWaivexxx "
                + " , nAmountxx "
                + " , nVATSales "
                + " , nVATAmtxx "
                + " , nDiscount "
                + " , nVatDiscx "
                + " , nPWDDiscx "
                + " , nAmtPaidx "
                + " , cORPrintx "
                + " , cTranStat "
                + " , cSendStat "
                + " , sClientNm "
                + " , sAddressx "
                + " , sModified "
                + " , dModified "
                + " FROM Charge_Invoice "
                + " WHERE cSendStat = 0 "
                + " ORDER BY dModified DESC";

        return lsSQL;
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
                + " , sCustName "
                + " , sModified "
                + " , dModified "
                + " FROM SO_Master ";

        return lsSQL;
    }
}
