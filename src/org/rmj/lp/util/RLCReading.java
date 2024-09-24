package org.rmj.lp.util;

import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.KnownHosts;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.StringHelper;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.FileUtil;
import org.rmj.lib.net.MiscReplUtil;
import org.rmj.lib.net.SFTP_DU;

public class RLCReading {
    private final String RLC_PATH = "RLC/";
    private final String MASTER_TABLE = "Daily_Summary";
    private final String FORMAT = "0.00";
    
    private GRiderX instance;
    private String message;
    
    private String tenantid = "";
    private String machinex = "";
    private String terminal = "";
    private String hostname = "";
    private String username = "";
    private String password = "";
    
    public RLCReading(GRiderX foGrider){
        instance = foGrider;
    }
    
    public RLCReading(GRiderX foGrider, String fsTenantID){
        instance = foGrider;
        tenantid = fsTenantID;
    }
    
    public String getMessage(){
        return message;
    }
    
    public boolean initMachine(String sCRMNumbr){
        if (sCRMNumbr.isEmpty()){
            message = "Machine number is not set.";
            return false;
        }
        
        if (tenantid.isEmpty()){
            message = "Tenant ID is not set.";
            return false;
        }
        
        if (instance == null){
            message = "Application driver is not set.";
            return false;
        }
        
        String lsSQL = "SELECT" +
                            "  sAccredtn" +
                            ", sPermitNo" +
                            ", sSerialNo" +
                            ", nPOSNumbr" +
                            ", nZReadCtr" +
                            ", sRLCHostx" +
                            ", sRLCUserx" +
                            ", sRLCPassx" +
                            ", sIDNumber" +
                        " FROM Cash_Reg_Machine" +
                        " WHERE sIDNumber = " + SQLUtil.toSQL(sCRMNumbr);
        
        ResultSet loRS = instance.executeQuery(lsSQL);
        
        try {
            if (!loRS.next()){
                message = "Machine is not registered.";
                return false;
            }
            
            machinex = loRS.getString("sIDNumber");
            terminal = loRS.getString("nPOSNumbr");
            hostname = loRS.getString("sRLCHostx");
            username = loRS.getString("sRLCUserx");
            password = loRS.getString("sRLCPassx");
        } catch (SQLException e) {
            e.printStackTrace();
            message = e.getMessage();
            return false;
        }
        
        return true;
    }
    
    public boolean createFile(String sTranDate, int nBatchNo){
        try {
            message = "";
            
            if (machinex.isEmpty()){
                message = "Machine is not initialized.";
                return false;
            }
            
            String lsSQL = "SELECT" +
                                "  nAccuSale" +
                                ", nZReadCtr" +
                            " FROM " + MASTER_TABLE +
                            " WHERE sTranDate < " + SQLUtil.toSQL(sTranDate) +
                                " AND sCRMNumbr = " + SQLUtil.toSQL(machinex) +
                                " AND cTranStat IN ('2')" +
                            " ORDER BY dClosedxx DESC LIMIT 1";

            ResultSet loRS = instance.executeQuery(lsSQL);
            
            double lnPrevSale = 0;
            int lnZReadCtr = 0;
            
            if (loRS.next()){
                lnPrevSale = loRS.getDouble("nAccuSale");
                lnZReadCtr = loRS.getInt("nZReadCtr");
            }

            MiscUtil.close(loRS);            

            lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTranDate = " + SQLUtil.toSQL(sTranDate) +
                                                    " AND sCRMNumbr = " + SQLUtil.toSQL(machinex) +
                                                    " AND cTranStat IN ('2')");
            
            loRS = instance.executeQuery(lsSQL);
            
            if (!loRS.next()){
                message = "No closed transaction for this day(" + sTranDate + ").";
                return true;
            }
            
            double lnCompute = 0.00;
            int lnCtr = 0;
            
            StringBuilder lsBuilder = new StringBuilder();
            lsBuilder.append("01" + StringHelper.prepad(tenantid, 16, '0') + "\n");
            lsBuilder.append("02" + StringHelper.prepad(terminal, 16, '0') + "\n");
            
            lnCompute = loRS.getDouble("nSalesAmt") + loRS.getDouble("nSChargex") + loRS.getDouble("nDiscount")
                        + loRS.getDouble("nVatDiscx") + loRS.getDouble("nPWDDiscx") + loRS.getDouble("nReturnsx")
                        - (loRS.getDouble("nSChargex") + loRS.getDouble("nVatDiscx"));
            lsBuilder.append("03" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            
            lnCompute = ((loRS.getDouble("nSalesAmt") - 
                            loRS.getDouble("nVatDiscx")) - 
                            loRS.getDouble("nPWDDiscx") - 
                            loRS.getDouble("nNonVATxx")) / 1.12 * 0.12;
            lsBuilder.append("04" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            
            lsBuilder.append("05" + StringHelper.prepad(CommonUtils.NumberFormat(loRS.getDouble("nVoidAmnt"), FORMAT), 16, '0') + "\n");
            lsBuilder.append("06" + StringHelper.prepad(String.valueOf(loRS.getInt("nVoidCntx")), 16, '0') + "\n");
            
            lsBuilder.append("07" + StringHelper.prepad(CommonUtils.NumberFormat(loRS.getDouble("nDiscount"), FORMAT), 16, '0') + "\n");
            lsBuilder.append("08" + StringHelper.prepad(String.valueOf(loRS.getInt("nTotlDisc")), 16, '0') + "\n");
            
            lsBuilder.append("09" + StringHelper.prepad(CommonUtils.NumberFormat(loRS.getDouble("nReturnsx"), FORMAT), 16, '0') + "\n");
            lsBuilder.append("10" + StringHelper.prepad(String.valueOf(loRS.getInt("nTotlRtrn")), 16, '0') + "\n");
            
            lsBuilder.append("11" + StringHelper.prepad(CommonUtils.NumberFormat(loRS.getDouble("nPWDDiscx"), FORMAT), 16, '0') + "\n");
            lsBuilder.append("12" + StringHelper.prepad(String.valueOf(loRS.getInt("nTotSCPWD")), 16, '0') + "\n");
            lsBuilder.append("13" + StringHelper.prepad(CommonUtils.NumberFormat(loRS.getDouble("nSChargex"), FORMAT), 16, '0') + "\n");
            
            lnCtr = getEODCtr(sTranDate);
            lsBuilder.append("14" + StringHelper.prepad(String.valueOf(lnCtr == 0 ? 0 : lnCtr - 1), 16, '0') + "\n");
            lsBuilder.append("15" + StringHelper.prepad(CommonUtils.NumberFormat(lnPrevSale, FORMAT), 16, '0') + "\n");
            lsBuilder.append("16" + StringHelper.prepad(String.valueOf(lnCtr), 16, '0') + "\n");
            
            lnCompute = ((loRS.getDouble("nSalesAmt") - 
                            loRS.getDouble("nVatDiscx")) - 
                            loRS.getDouble("nDiscount") - 
                            loRS.getDouble("nPWDDiscx")) + lnPrevSale;
            lsBuilder.append("17" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");

            lsSQL = sTranDate.substring(4, 6) + "/" + sTranDate.substring(6, 8) + "/" + sTranDate.substring(0, 4);
            lsBuilder.append("18" + StringHelper.prepad(lsSQL, 16, '0') + "\n");
            
            lnCompute = 0.00;
            lsBuilder.append("19" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            lsBuilder.append("20" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            lsBuilder.append("21" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            
            lnCompute = loRS.getDouble("nCrdtAmnt");
            lsBuilder.append("22" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            lnCompute = lnCompute / 1.12 * 0.12;
            lsBuilder.append("23" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            
            lnCompute = loRS.getDouble("nNonVATxx") * (1 - 0.20);
            lsBuilder.append("24" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
        
            lnCompute = 0.00;
            lsBuilder.append("25" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            lsBuilder.append("26" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            lsBuilder.append("27" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
            lsBuilder.append("28" + StringHelper.prepad(CommonUtils.NumberFormat(lnCompute, FORMAT), 16, '0') + "\n");
        
            lsBuilder.append("29" + StringHelper.prepad(CommonUtils.NumberFormat(loRS.getDouble("nRepAmntx"), FORMAT), 16, '0') + "\n");
            lsBuilder.append("30" + StringHelper.prepad(String.valueOf(loRS.getInt("nReprintx")), 16, '0') + "\n");
            
            //check if upload path exists
            lsSQL = instance.getApplPath() + RLC_PATH + "UPLOAD/";
            if (!MiscReplUtil.fileExists(lsSQL)){
                File loFile = new File(lsSQL);
                if (!loFile.exists()) {
                    if (loFile.mkdirs()){
                        System.out.println("Upload directory created successfully.");
                    } else {
                        message = "Upload directory.";
                        return false;
                    } 
                }
            }
            
            String lsFile = tenantid.substring(tenantid.length() - 4) + sTranDate.substring(4, 6) + sTranDate.substring(6) + "." +
                            StringHelper.prepad(terminal.toString(), 2, '0') + String.valueOf(nBatchNo);
            lsSQL += lsFile;
            
            if (!FileUtil.fileWrite(lsSQL, lsBuilder.toString())){
                message = "Unable to create sales file. - " + lsFile;
                return false;
            }
            
            if (!uploadFile()) return false;
            
            message = "Sales file successfully sent to RLC server.";
        } catch (SQLException e) {
            e.printStackTrace();
            message = e.getMessage();
            return false;
        }
        
        return true;
    }
    
    public boolean uploadFile(){
        String lsSQL = instance.getApplPath() + RLC_PATH + "SUCCESS/";
        
        File loFile = new File(lsSQL);
        if (!MiscReplUtil.fileExists(lsSQL)){
            if (!loFile.exists()) {
                if (loFile.mkdirs()){
                    System.out.println("Upload success directory created successfully.");
                } else {
                    message = "Unable to create upload success directory.";
                    return false;
                } 
            }
        }
        
        SFTP_DU sftp = new SFTP_DU();
        sftp.setHost("112.199.91.14");
        sftp.setHostKey("ssh-ed25519 255 e6vFs7ULtSiKo3GfwWlkuz792QGW2YeyWn/83Vsq38A=");
        sftp.setUser("LPPangasinan");
        sftp.setPassword("RLC@20211102Pag");
        sftp.setPort(22);      
        
        loFile = new File(instance.getApplPath() + RLC_PATH + "UPLOAD/");
        File [] contents = loFile.listFiles();     
        
        String lsUpload = instance.getApplPath() + RLC_PATH + "UPLOAD/";
        String lsSuccess = instance.getApplPath() + RLC_PATH + "SUCCESS/";
        
        for (File obj : contents){
            if (obj.isFile()){
                try {
                    if (sftp.Upload(lsUpload, "/50080729/", obj.getName())){
                        if (!FileUtil.moveFile(lsUpload + obj.getName(), lsSuccess + obj.getName())){
                            message = "Unable to move file to sent directory. " + lsUpload + obj.getName();
                            return false;
                        }
                        
                        if (FileUtil.exists(lsUpload + obj.getName())){
                            if (!FileUtil.fileDelete(lsUpload + obj.getName())){
                                message = "Unable to delete file. " + lsUpload + obj.getName();
                                return false;
                            }
                        }
                    }
                } catch (Exception e) {
                    message = e.getMessage();
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private int getEODCtr(String fsTranDate) throws SQLException{
        //get last end of day
        String lsSQL = "SELECT sTranDate" +
                        " FROM Daily_Summary" +
                        " WHERE cTranStat = '2'" +
                        " ORDER BY sTranDate DESC, dClosedxx" +
                        " LIMIT 1";
        ResultSet loRS = instance.executeQuery(lsSQL);
        
        if (!loRS.next()) return 0;
        String lsLastEOD = loRS.getString("sTranDate");
        MiscUtil.close(loRS);
        
        //get last EOD counter
        lsSQL = "SELECT nEODCtrxx" +
                " FROM Cash_Reg_Machine" +
                " WHERE sIDNumber = " + SQLUtil.toSQL(machinex);
        loRS = instance.executeQuery(lsSQL);
        
        if (!loRS.next()) return 0;
        int lnLastEOD = loRS.getInt("nEODCtrxx");
        MiscUtil.close(loRS);
        
        String lsDate1 = fsTranDate.substring(0, 4) + "-" + fsTranDate.substring(4, 6) + "-" + fsTranDate.substring(6);
        String lsDate2 = lsLastEOD.substring(0, 4) + "-" + lsLastEOD.substring(4, 6) + "-" + lsLastEOD.substring(6);
        
        Date ldDate1 = SQLUtil.toDate(lsDate1, SQLUtil.FORMAT_SHORT_DATE);
        Date ldDate2 = SQLUtil.toDate(lsDate2, SQLUtil.FORMAT_SHORT_DATE);
        
        int lnDiff = (int) CommonUtils.dateDiff(ldDate2, ldDate1);
        
        if (lnDiff < 0){
            return 0;
        } else if (lnDiff == 0){
            return lnLastEOD;
        } else {
            //get the eod transaction difference between the last eod and the requested date
            lsSQL = "SELECT sTranDate" + 
                    " FROM Daily_Summary" +
                    " WHERE sTranDate BETWEEN " + SQLUtil.toSQL(fsTranDate) + " AND " + SQLUtil.toSQL(lsLastEOD) +
                        " AND cTranStat = '2'" +
                    " GROUP BY sTranDate";
            loRS = instance.executeQuery(lsSQL);
            lnDiff = (int) MiscUtil.RecordCount(loRS) - 1;
            MiscUtil.close(loRS);
            
            return lnLastEOD - lnDiff;
        }
    }
    
    private String getSQ_Master(){
        return "SELECT" +
                    "  a.sTranDate" +
                    ", a.sCRMNumbr" +
                    ", a.sCashierx" +
                    ", SUM(a.nOpenBalx) nOpenBalx" +
                    ", SUM(a.nCPullOut) nCPullOut" +
                    ", SUM(a.nSalesAmt) nSalesAmt" +
                    ", SUM(a.nVATSales) nVATSales" +
                    ", SUM(a.nVATAmtxx) nVATAmtxx" +
                    ", SUM(a.nNonVATxx) nNonVATxx" +
                    ", SUM(a.nZeroRatd) nZeroRatd" +
                    ", SUM(a.nDiscount) nDiscount" +
                    ", SUM(a.nPWDDiscx) nPWDDiscx" +
                    ", SUM(a.nVatDiscx) nVatDiscx" +
                    ", SUM(a.nReturnsx) nReturnsx" +
                    ", SUM(a.nVoidAmnt) nVoidAmnt" +
                    ", SUM(a.nAccuSale) nAccuSale" +
                    ", SUM(a.nCashAmnt) nCashAmnt" +
                    ", SUM(a.nChckAmnt) nChckAmnt" +
                    ", SUM(a.nCrdtAmnt) nCrdtAmnt" +
                    ", SUM(a.nChrgAmnt) nChrgAmnt" +
                    ", SUM(a.nSChargex) nSChargex" +
                    ", SUM(a.nRepAmntx) nRepAmntx" +
                    ", SUM(a.nCancelld) nCancelld" +
                    ", a.sORNoFrom" +
                    ", a.sORNoThru" +
                    ", a.nZReadCtr" +
                    ", SUM(a.nGiftAmnt) nGiftAmnt" +
                    ", a.cTranStat" +
                    ", SUM(a.nVoidCntx) nVoidCntx" +
                    ", SUM(a.nTotlDisc) nTotlDisc" +
                    ", SUM(a.nTotSCPWD) nTotSCPWD" +
                    ", SUM(a.nTotlRtrn) nTotlRtrn" +
                    ", SUM(a.nTotlCncl) nTotlCncl" +
                    ", SUM(a.nReprintx) nReprintx" +
                " FROM " + MASTER_TABLE + " a" +
                " GROUP BY a.sTranDate" +
                " ORDER BY a.sTranDate ASC";
    }
}
