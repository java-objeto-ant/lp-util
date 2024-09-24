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

public class DSUpload extends Application {

    public static GRider oApp;

    @Override
    public void start(Stage stage) throws Exception {
        String lsAPI = "http://192.168.10.70/LosPedritos/getDeliveryService.php";

        Map<String, String> headers = getAPIHeader();

        try {
            JSONObject param = new JSONObject();
            System.out.println(getSQ_DeliveryTrans());
            ResultSet loRS = oApp.executeQuery(getSQ_DeliveryTrans());

            JSONArray dtArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            JSONObject deliverytransObject = new JSONObject();
            JSONArray somasterArray = new JSONArray();
            JSONObject somasterObject = new JSONObject();
            JSONArray arledgerArray = new JSONArray();
            JSONObject arledgerObject = new JSONObject();
            while (loRS.next()) {

                //clear first
                jsonObject.clear();
                deliverytransObject.clear();
                somasterArray.clear();
                somasterObject.clear();
                arledgerArray.clear();
                arledgerObject.clear();
                param.clear();
                

                deliverytransObject.put("sTransNox", loRS.getString("sTransNox"));
                deliverytransObject.put("sRiderIDx", loRS.getString("sRiderIDx"));
                deliverytransObject.put("sRemarksx", loRS.getString("sRemarksx"));
                deliverytransObject.put("nAmountxx", loRS.getBigDecimal("nAmountxx"));
                deliverytransObject.put("sSourceCd", loRS.getString("sSourceCd"));
                deliverytransObject.put("sSourceNo", loRS.getString("sSourceNo"));
                deliverytransObject.put("cCollectd", loRS.getObject("cCollectd"));
                deliverytransObject.put("cBilledxx", loRS.getObject("cBilledxx"));
                deliverytransObject.put("dBilledxx", loRS.getString("dBilledxx"));
                deliverytransObject.put("cPaidxxxx", loRS.getObject("cPaidxxxx"));
                deliverytransObject.put("dPaidxxxx", loRS.getString("dPaidxxxx"));
                deliverytransObject.put("cWaivexxx", loRS.getObject("cWaivexxx"));
                deliverytransObject.put("dWaivexxx", loRS.getString("dWaivexxx"));
                deliverytransObject.put("sWaivexxx", loRS.getString("sWaivexxx"));
                deliverytransObject.put("cTranStat", loRS.getObject("cTranStat"));
                deliverytransObject.put("cSendStat", loRS.getObject("cSendStat"));
                deliverytransObject.put("dModified", loRS.getString("dModified"));
                deliverytransObject.put("dTimeStmp", loRS.getString("dTimeStmp"));
                dtArray.add(deliverytransObject);
                jsonObject.put("Delivery_Service_Trans", dtArray);

                String lsSQLDetail = MiscUtil.addCondition(getSQ_SaleMaster(), "sTransNox = " + SQLUtil.toSQL(loRS.getString("sSourceNo")));
                ResultSet loRSSOMaster = oApp.executeQuery(lsSQLDetail);

                while (loRSSOMaster.next()) {
                //fetching the Ledger sourcing deliveryservice
                    System.out.println(loRS.getString("sTransNox"));
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
                
                
                //fetching the Ledger sourcing deliveryservice
                //branchcd+posno+yeear sample P0060124000000001
                String lsSQLLedger = MiscUtil.addCondition(getSQ_ARLedger(), "sSourceNo = " + SQLUtil.toSQL(loRS.getString("sTransNox")));
                ResultSet loRSLedger = oApp.executeQuery(lsSQLLedger);

                while (loRSLedger.next()) {

                    System.out.println(loRSLedger.getString("sClientID"));
                    arledgerObject.put("sClientID", loRSLedger.getString("sClientID"));
                    arledgerObject.put("sBranchCd", loRSLedger.getString("sBranchCd"));
                    arledgerObject.put("nEntryNox", loRSLedger.getObject("nEntryNox"));
                    arledgerObject.put("dTransact", loRSLedger.getString("dTransact"));
                    arledgerObject.put("sSourceCd", loRSLedger.getString("sSourceCd"));
                    arledgerObject.put("sSourceNo", loRSLedger.getString("sSourceNo"));
                    arledgerObject.put("cReversex", loRSLedger.getString("cReversex"));
                    arledgerObject.put("nCreditxx", loRSLedger.getBigDecimal("nCreditxx"));
                    arledgerObject.put("nDebitxxx", loRSLedger.getBigDecimal("nDebitxxx"));
                    arledgerObject.put("dPostedxx", loRSLedger.getString("dPostedxx"));

                    arledgerArray.add(arledgerObject);
                }
                jsonObject.put("AR_Ledger", arledgerArray);

                loRSLedger.close();

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

                        String lsSQL = "UPDATE Delivery_Service_Trans set cSendStat = 1 WHERE sTransNox =" + SQLUtil.toSQL(json.get("transno").toString());
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

    private String getSQ_DeliveryTrans() {
        String lsSQL;

        lsSQL = " SELECT "
                + " sTransNox "
                + " , sRiderIDx "
                + " , sRemarksx "
                + " , nAmountxx "
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
                + " , cTranStat "
                + " , cSendStat "
                + " , dModified "
                + " , dTimeStmp  "
                + " FROM Delivery_Service_Trans "
                + " WHERE cSendStat = 0 "
                + " ORDER BY dTimeStmp DESC";

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
    
        private String getSQ_ARLedger() {
        String lsSQL;
        lsSQL = " SELECT "
                + " sClientID "
                + " , sBranchCd "
                + " , nEntryNox "
                + " , dTransact "
                + " , sSourceCd "
                + " , sSourceNo "
                + " , cReversex "
                + " , nCreditxx "
                + " , nDebitxxx "
                + " , dPostedxx "
                + " FROM AR_Ledger ";

        return lsSQL;
    }
}
