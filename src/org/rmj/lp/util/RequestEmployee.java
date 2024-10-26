package org.rmj.lp.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.MySQLAESCrypt;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.WebClient;

public class RequestEmployee {

    public static void main(String[] args) {
        String sURL = "https://restgk.guanzongroup.com.ph/lospedritos/accounts/getEmployee.php";

        String path;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems/temp";
        } else {
            path = "/srv/GGC_Java_Systems/temp";
        }

        System.setProperty("sys.default.path.config", path);
        System.setProperty("app.employee.id", args[0]);

        Map<String, String> headers = getHeader();

        JSONObject param = new JSONObject();

        param.put("employno", System.getProperty("app.employee.id"));
        String tempFilePath = System.getProperty("sys.default.path.config") + "/pos.tmp";
        File tempFile = new File(tempFilePath);

        // Delete existing temp file if it exists
        if (tempFile.exists()) {
            tempFile.delete();
        }
        String response;
        try {
            response = WebClient.sendHTTP(sURL, param.toJSONString(), (HashMap<String, String>) headers);
            if (response == null) {
                System.err.println("No Response"); //return value
                System.exit(1);
            }
            JSONParser loParser = new JSONParser();
            JSONObject loJSON = (JSONObject) loParser.parse(response);

            if (!"success".equals((String) loJSON.get("result"))) {
                loJSON = (JSONObject) loParser.parse(loJSON.get("error").toString());

                response = (String) loJSON.get("message");
                System.out.println(response);
                System.exit(1);
            } else {
                response = MySQLAESCrypt.Decrypt((String) loJSON.get("payload"), "20190625");

                System.out.println(response);
                // Write the response to the temp file with UTF-8 encoding
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                    writer.write(response);
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.exit(0);
            }
        } catch (IOException | ParseException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    private static HashMap getHeader() {
        String clientid = "GGC_BM001";
        String productid = "gRider";
        String imei = "GMC_SEG09";
        String user = "M001111122";
        String log = "";

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
    }
}
