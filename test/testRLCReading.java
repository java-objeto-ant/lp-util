
import java.util.Date;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.lib.net.LogWrapper;
import org.rmj.lp.util.RLCReading;

public class testRLCReading {
    public static void main (String [] args){
        LogWrapper logwrapr = new LogWrapper("testRLCReading", "RLCsftp.log");
        logwrapr.info("Start of Process!");
        
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);
        
        GRiderX instance = new GRiderX("gRider");
        
        if (!instance.logUser("gRider", "M001111122")){
            logwrapr.severe(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        RLCReading read = new RLCReading(instance, "50080729");
        if (!read.initMachine("22010313392685364")){
            logwrapr.severe(read.getMessage());
            System.exit(1);
        }
        
        String lsDate1 = "2024-07-02"; //always -1 day of the requested date
        String lsDate2 = "2024-07-03";
        
        Date ldDate1;
        Date ldDate2;
        
        ldDate1 = SQLUtil.toDate(lsDate1, SQLUtil.FORMAT_SHORT_DATE);
        ldDate2 = SQLUtil.toDate(lsDate2, SQLUtil.FORMAT_SHORT_DATE);
        while (0 != 1){
            ldDate1 = CommonUtils.dateAdd(ldDate1, 1);
            lsDate1 = SQLUtil.dateFormat(ldDate1, SQLUtil.FORMAT_SHORT_DATE);
            
            logwrapr.info("Trying to create sales file.");
            if (!read.createFile(lsDate1.replace("-", ""), 1)){
                logwrapr.severe(read.getMessage());
                System.exit(1);
            } else {
                logwrapr.info(read.getMessage());
            }
            
            if (CommonUtils.dateDiff(ldDate1, ldDate2) >= 0) break;
        }
    }
}