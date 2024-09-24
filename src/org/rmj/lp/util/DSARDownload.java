/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.lp.util;

import java.math.BigDecimal;
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

public class DSARDownload extends Application {

    public static GRider oApp;
    private String p_sBranchCd;

    @Override
    public void start(Stage stage) throws Exception {
        p_sBranchCd = oApp.getBranchCode();

        String lsAPI = "http://192.168.10.70/LosPedritos/getDeliveryAR.php";

        Map<String, String> headers = getAPIHeader();

        try {
            JSONObject param = new JSONObject();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("branchCd", p_sBranchCd);

            param.put("payload", jsonObject);
            System.out.println("json object :" + param);
            String response = WebClient.sendHTTP(lsAPI, param.toJSONString(), (HashMap<String, String>) headers);

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response);
            JSONArray DSjson = (JSONArray) json.get("Delivery_Service");
            JSONArray ARjson = (JSONArray) json.get("AR_Master");

            if (json == null) {
                System.out.println("No Response");
                System.exit(1);
            } else {
                if (json.get("result").equals("error")) {
                    System.out.println(json.toJSONString());
                    System.exit(1);
                } else {

                    if (DSjson.size() <= 0) {
                        System.out.println("No Delivery Service Found");
                        System.exit(1);
                    }
                    for (Object obj : DSjson) {
                        JSONObject deliveryService = (JSONObject) obj;
                        String sRiderIDx = (String) deliveryService.get("sRiderIDx");
                        String sClientID = (String) deliveryService.get("sClientID");
                        String sBriefDsc = (String) deliveryService.get("sBriefDsc");
                        String sDescript = (String) deliveryService.get("sDescript");
                        String dPartnerx = (String) deliveryService.get("dPartnerx");
                        String nSrvcChrg = (String) deliveryService.get("nSrvcChrg");
                        String dSrvcChrg = (String) deliveryService.get("dSrvcChrg");
                        String cRecdStat = (String) deliveryService.get("cRecdStat");
                        String sModified = (String) deliveryService.get("sModified");
                        String dModified = (String) deliveryService.get("dModified");
                        String dTimeStmp = (String) deliveryService.get("dTimeStmp");

                        String lsSQL = (MiscUtil.addCondition(getSQ_DeliveryParam(), " sRiderIDx = "
                                + SQLUtil.toSQL(sRiderIDx)));

                        ResultSet loRS = oApp.executeQuery((lsSQL));

                        String lsSQLQuery;
                        if (loRS.next()) {
                            lsSQLQuery = "UPDATE Delivery_Service set "
                                    + " nSrvcChrg = " + nSrvcChrg
                                    + " WHERE sRiderIDx = " + SQLUtil.toSQL(sRiderIDx);
                        } else {
                            lsSQLQuery = " INSERT INTO Delivery_Service set "
                                    + " sRiderIDx = " + SQLUtil.toSQL(sRiderIDx)
                                    + ", sClientID = " + SQLUtil.toSQL(sClientID)
                                    + ", sBriefDsc = " + SQLUtil.toSQL(sBriefDsc)
                                    + ", sDescript = " + SQLUtil.toSQL(sDescript)
                                    + ", dPartnerx = " + SQLUtil.toSQL(dPartnerx)
                                    + ", nSrvcChrg = " + nSrvcChrg
                                    + ", dSrvcChrg = " + SQLUtil.toSQL(dSrvcChrg)
                                    + ", cRecdStat = " + SQLUtil.toSQL(cRecdStat)
                                    + ", sModified = " + SQLUtil.toSQL(sModified)
                                    + ", dModified = " + SQLUtil.toSQL(dModified)
                                    + ", dTimeStmp = " + SQLUtil.toSQL(dTimeStmp);
                        }
                        if (oApp.executeQuery(lsSQLQuery, "", "", "") <= 0) {
                            loRS.close();
                            System.out.println(response);
                        }
                    }

                    if (ARjson.size() <= 0) {
                        System.out.println("AR Master Found");
                        System.exit(1);
                    }
                    for (Object objAR : ARjson) {
                        JSONObject arMaster = (JSONObject) objAR;
                        String sClientID = (String) arMaster.get("sClientID");
                        String sBranchCd = (String) arMaster.get("sBranchCd");
                        String sCPerson1 = (String) arMaster.get("sCPerson1");
                        String sCPPosit1 = (String) arMaster.get("sCPPosit1");
                        String sTelNoxxx = (String) arMaster.get("sTelNoxxx");
                        String sFaxNoxxx = (String) arMaster.get("sFaxNoxxx");
                        String sRemarksx = (String) arMaster.get("sRemarksx");
                        String sTermIDxx = (String) arMaster.get("sTermIDxx");
                        String nDisCount = (String) arMaster.get("nDisCount");
                        String nCredLimt = (String) arMaster.get("nCredLimt");
                        String dBalForwd = (String) arMaster.get("dBalForwd");
                        String nBalForwd = (String) arMaster.get("nBalForwd");
                        String nOBalance = (String) arMaster.get("nOBalance");
                        String nABalance = (String) arMaster.get("nABalance");
                        String dCltSince = (String) arMaster.get("dCltSince");
                        String cAutoHold = (String) arMaster.get("cAutoHold");
                        String cHoldAcct = (String) arMaster.get("cHoldAcct");
                        String cRecdStat = (String) arMaster.get("cRecdStat");
                        String dTimeStmp = (String) arMaster.get("dTimeStmp");

                        String lsSQL = (MiscUtil.addCondition(getSQ_ARMaster(), " sClientID = "
                                + SQLUtil.toSQL(sClientID)));

                        ResultSet loRS = oApp.executeQuery((lsSQL));

                        String lsSQLQuery;
                        if (loRS.next()) {
                            lsSQLQuery = " UPDATE AR_Master set "
                                    + " nDisCount = " + nDisCount
                                    + ", nCredLimt = " + nCredLimt
                                    + ", nOBalance = " + nOBalance
                                    + ", nABalance = " + nABalance
                                    + " WHERE sClientID = " + SQLUtil.toSQL(sClientID);
                        } else {
                            lsSQLQuery = " INSERT INTO AR_Master set "
                                    + " sClientID = " + SQLUtil.toSQL(sClientID)
                                    + ", sBranchCd = " + SQLUtil.toSQL(sBranchCd)
                                    + ", sCPerson1 = " + SQLUtil.toSQL(sCPerson1)
                                    + ", sCPPosit1 = " + SQLUtil.toSQL(sCPPosit1)
                                    + ", sTelNoxxx = " + SQLUtil.toSQL(sTelNoxxx)
                                    + ", sFaxNoxxx = " + SQLUtil.toSQL(sFaxNoxxx)
                                    + ", sRemarksx = " + SQLUtil.toSQL(sRemarksx)
                                    + ", sTermIDxx = " + SQLUtil.toSQL(sTermIDxx)
                                    + ", nDisCount = " + nDisCount
                                    + ", nCredLimt = " + nCredLimt
                                    + ", dBalForwd = " + SQLUtil.toSQL(dBalForwd)
                                    + ", nBalForwd = " + nBalForwd
                                    + ", nOBalance = " + nOBalance
                                    + ", nABalance = " + nABalance
                                    + ", dCltSince = " + SQLUtil.toSQL(dCltSince)
                                    + ", cAutoHold = " + SQLUtil.toSQL(cAutoHold)
                                    + ", cHoldAcct = " + SQLUtil.toSQL(cHoldAcct)
                                    + ", cRecdStat = " + SQLUtil.toSQL(cRecdStat)
                                    + ", dTimeStmp = " + SQLUtil.toSQL(dTimeStmp);
                        }
                        if (oApp.executeQuery(lsSQLQuery, "", "", "") <= 0) {
                            loRS.close();

                        }

                    }
                }
            }
            System.out.println(response);
            System.exit(0);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
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

    private String getSQ_DeliveryParam() {
        String lsSQL;

        lsSQL = "SELECT"
                + " sRiderIDx "
                + " , sClientID "
                + " , sBriefDsc "
                + " , sDescript "
                + " , dPartnerx "
                + " , nSrvcChrg "
                + " , dSrvcChrg "
                + " , cRecdStat "
                + " , sModified "
                + " , dModified "
                + " , dTimeStmp "
                + " FROM Delivery_Service ";

        return lsSQL;
    }

    private String getSQ_ARMaster() {
        String lsSQL;
        lsSQL = " SELECT "
                + " sClientID "
                + " , sBranchCd "
                + " , sCPerson1 "
                + " , sCPPosit1 "
                + " , sTelNoxxx "
                + " , sFaxNoxxx "
                + " , sRemarksx "
                + " , sTermIDxx "
                + " , nDisCount "
                + " , nCredLimt "
                + " , dBalForwd "
                + " , nBalForwd "
                + " , nOBalance "
                + " , nABalance "
                + " , dCltSince "
                + " , cAutoHold "
                + " , cHoldAcct "
                + " , cRecdStat "
                + " , dTimeStmp "
                + "   FROM AR_Master ";

        return lsSQL;
    }
}
