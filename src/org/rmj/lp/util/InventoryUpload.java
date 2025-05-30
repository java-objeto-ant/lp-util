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
import java.util.ArrayList;
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
import org.rmj.appdriver.constants.RecordStatus;

public class InventoryUpload extends Application {

    public static GRider oApp;
    Map<String, Integer> paStockID = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) throws Exception {
        String lsSQL = "";
        String lsCondition = "";

//        String lsAPI = "https://restgk.guanzongroup.com.ph/lospedritos/accounts/getInventory.php";
        String lsAPI = "http://localhost/lospedritos/accounts/getInventory.php";

        Map<String, String> headers = getAPIHeader();

        try {
            JSONObject param = new JSONObject();

            lsSQL = MiscUtil.addCondition(getSQ_Inventory(), "cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
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

                    }else {
                    System.out.println(response);
                    }
                }

            }
            loRSInventory.close();
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
