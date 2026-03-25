package org.rmj.lp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;
import org.rmj.cas.inventory.base.InvAdjustment;
import static org.rmj.lp.util.DSUpload.oApp;

public class CreditDebitMemo {
    public static void main (String [] args){
        String path;

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Java_Systems";
        } else {
            path = "/srv/mac/GGC_Java_Systems";
        }

        System.setProperty("sys.default.path.config", path);

        oApp = new GRider("General");

        if (!oApp.loadEnv("General")) {
            System.exit(1);
        }
        
        if (!oApp.loadUser("General", "M001111122")) {
            System.exit(1);
        }
        
        loadProperties();
        
        CMDM trans = new CMDM(oApp);
        trans.setFile("d:/CMDM 20250726.xlsx");
        
        if (!trans.InitTransaction()) {
            System.exit(1);
        }
        
        if (!trans.ProcessData()){
            System.exit(1);
        }
        
        System.exit(0);
    }
    
    private static boolean loadProperties() {
        try {
            Properties po_props = new Properties();
            po_props.load(new FileInputStream("D:\\GGC_Java_Systems\\config\\rmj.properties"));

            System.setProperty("app.debug.mode", po_props.getProperty("app.debug.mode"));
            System.setProperty("user.id", po_props.getProperty("user.id"));
            System.setProperty("app.product.id", po_props.getProperty("app.product.id"));

            if (System.getProperty("app.product.id").equalsIgnoreCase("integsys")) {
                System.setProperty("pos.clt.nm", po_props.getProperty("pos.clt.nm.integsys"));
            } else {
                System.setProperty("pos.clt.nm", po_props.getProperty("pos.clt.nm.telecom"));
            }

            System.setProperty("pos.clt.tin", po_props.getProperty("pos.clt.tin"));
            System.setProperty("pos.clt.crm.no", po_props.getProperty("pos.clt.crm.no"));
            System.setProperty("pos.clt.dir.ejournal", po_props.getProperty("pos.clt.dir.ejournal"));

            //store info
            System.setProperty("store.commissary", po_props.getProperty("store.commissary"));
            System.setProperty("store.inventory.type", po_props.getProperty("store.inventory.type"));
            System.setProperty("store.inventory.strict.type", po_props.getProperty("store.inventory.strict.type"));
            System.setProperty("store.inventory.type.stock", po_props.getProperty("store.inventory.type.stock"));
            System.setProperty("store.inventory.type.product", po_props.getProperty("store.inventory.type.product"));

            //UI
            System.setProperty("app.product.id.grider", po_props.getProperty("app.product.id.grider"));
            System.setProperty("app.product.id.general", po_props.getProperty("app.product.id.general"));
            System.setProperty("app.product.id.integsys", po_props.getProperty("app.product.id.integsys"));
            System.setProperty("app.product.id.telecom", po_props.getProperty("app.product.id.telecom"));

            return true;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
}

class CMDM{
    private GRider poGRider;
    private String psFileName;
    
    private List<Data> paDataList;
    
    public CMDM(GRider foValue){
        poGRider = foValue;
    }
    
    public void setFile(String fsValue){
        psFileName = fsValue;
    }
    
    public boolean InitTransaction(){
        if (poGRider == null){
            System.err.println("Application driver is not set.");
            return false;
        }
        
        if (psFileName.isEmpty()){
            System.err.println("File is not set.");
            return false;
        }
        
        paDataList = new ArrayList();
        
        return true;
    }
    
    public boolean ProcessData(){
        try {
            if (!captureData()){
                System.err.println("Unable to capture data.");
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
        
        InvAdjustment adj = new InvAdjustment(poGRider, poGRider.getBranchCode(), false);
        
        if (!adj.newTransaction()){
            return false;
        }
        
        adj.setMaster("dTransact", SQLUtil.toDate("2025-07-25", SQLUtil.FORMAT_SHORT_DATE));
        adj.setMaster("sRemarksx", "July 25 Adjustment");
        
        String lsSQL;
        
        for (int lnCtr = 0; lnCtr <= paDataList.size() - 1; lnCtr ++){
            System.out.println(paDataList.get(lnCtr).getId());
            
            lsSQL = "SELECT" +
                        "  a.sStockIDx" +
                        ", a.nQtyOnHnd" +
                    " FROM Inventory a" +
                        ", Inv_Master b" +
                    " WHERE a.sStockIDx = b.sStockIDx" +
                        " AND b.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                        " AND a.sBarCodex = " + SQLUtil.toSQL(paDataList.get(lnCtr).getId());
            
            System.out.println(lsSQL);
        }
        
        return true;
    }
    
    private boolean captureData() throws IOException{
        paDataList.add(new Data("220000000015",	0));
        paDataList.add(new Data("220000000016",	164));
        paDataList.add(new Data("220000000020",	0));
        paDataList.add(new Data("220000000021",	35));
        paDataList.add(new Data("220000000023",	0));
        paDataList.add(new Data("220000000024",	47   ));
        paDataList.add(new Data("220000000054",	0    ));
        paDataList.add(new Data("220000000055",	27   ));
        paDataList.add(new Data("220000000059",	0    ));
        paDataList.add(new Data("220000000060",	7    ));
        paDataList.add(new Data("220000000064",	0    ));
        paDataList.add(new Data("220000000065",	121  ));
        paDataList.add(new Data("220000000068",	0    ));
        paDataList.add(new Data("220000000069",	16   ));
        paDataList.add(new Data("220000000070",	0    ));
        paDataList.add(new Data("220000000071",	17   ));
        paDataList.add(new Data("220000000073",	0    ));
        paDataList.add(new Data("220000000074",	125  ));
        paDataList.add(new Data("220000000082",	0    ));
        paDataList.add(new Data("220000000083",	17   ));
        paDataList.add(new Data("220000000084",	0    ));
        paDataList.add(new Data("220000000085",	20   ));
        paDataList.add(new Data("220000000086",	0    ));
        paDataList.add(new Data("220000000087",	25   ));
        paDataList.add(new Data("220000000090",	0    ));
        paDataList.add(new Data("220000000091",	32   ));
        paDataList.add(new Data("220000000103",	38   ));
        paDataList.add(new Data("220000000109",	48   ));
        paDataList.add(new Data("220000000110",	0    ));
        paDataList.add(new Data("220000000111",	20   ));
        paDataList.add(new Data("220000000113",	0    ));
        paDataList.add(new Data("220000000114",	18   ));
        paDataList.add(new Data("220000000117",	0    ));
        paDataList.add(new Data("220000000118",	7    ));
        paDataList.add(new Data("220000000125",	0    ));
        paDataList.add(new Data("220000000126",	338  ));
        paDataList.add(new Data("220000000188",	0    ));
        paDataList.add(new Data("220000000189",	76   ));
        paDataList.add(new Data("220000000191",	4    ));
        paDataList.add(new Data("220000000193",	0    ));
        paDataList.add(new Data("220000000199",	0    ));
        paDataList.add(new Data("220000000200",	384  ));
        paDataList.add(new Data("220000000201",	0    ));
        paDataList.add(new Data("220000000202",	36   ));
        paDataList.add(new Data("220000000203",	0    ));
        paDataList.add(new Data("220000000204",	132  ));
        paDataList.add(new Data("220000000205",	0    ));
        paDataList.add(new Data("220000000209",	0    ));
        paDataList.add(new Data("220000000210",	35   ));
        paDataList.add(new Data("220000000211",	0    ));
        paDataList.add(new Data("220000000212",	104  ));
        paDataList.add(new Data("220000000235",	0    ));
        paDataList.add(new Data("220000000236",	50   ));
        paDataList.add(new Data("220000000242",	0    ));
        paDataList.add(new Data("220000000243",	90   ));
        paDataList.add(new Data("220000000250",	0    ));
        paDataList.add(new Data("220000000251",	177  ));
        paDataList.add(new Data("220000000265",	0    ));
        paDataList.add(new Data("220000000267",	0    ));
        paDataList.add(new Data("220000000290",	0    ));
        paDataList.add(new Data("220000000291",	87   ));
        paDataList.add(new Data("220000000295",	0    ));
        paDataList.add(new Data("220000000296",	7    ));
        paDataList.add(new Data("220000000299",	0    ));
        paDataList.add(new Data("220000000300",	22   ));
        paDataList.add(new Data("220000000307",	0    ));
        paDataList.add(new Data("220000000316",	0    ));
        paDataList.add(new Data("220000000317",	140  ));
        paDataList.add(new Data("220000000318",	0    ));
        paDataList.add(new Data("220000000319",	26   ));
        paDataList.add(new Data("220000000332",	0    ));
        paDataList.add(new Data("220000000333",	8    ));
        paDataList.add(new Data("220000000340",	0    ));
        paDataList.add(new Data("220000000341",	24   ));
        paDataList.add(new Data("220000000370",	0    ));
        paDataList.add(new Data("220000000371",	4    ));
        paDataList.add(new Data("220000000377",	0    ));
        paDataList.add(new Data("220000000378",	29   ));
        paDataList.add(new Data("220000000382",	0    ));
        paDataList.add(new Data("220000000393",	0    ));
        paDataList.add(new Data("220000000394",	20   ));
        paDataList.add(new Data("220000000413",	0    ));
        paDataList.add(new Data("220000000414",	1377 ));
        paDataList.add(new Data("220000000417",	0    ));
        paDataList.add(new Data("220000000418",	49000));
        paDataList.add(new Data("220000000436",	0    ));
        paDataList.add(new Data("220000000437",	18   ));
        paDataList.add(new Data("220000000449",	65   ));
        paDataList.add(new Data("220000000464",	0    ));
        paDataList.add(new Data("220000000465",	123  ));
        paDataList.add(new Data("220000000483",	0    ));
        paDataList.add(new Data("220000000484",	49   ));
        paDataList.add(new Data("220000000488",	0    ));
        paDataList.add(new Data("220000000489",	94   ));
        paDataList.add(new Data("220000000490",	0    ));
        paDataList.add(new Data("220000000491",	56   ));
        paDataList.add(new Data("220000000492",	0    ));
        paDataList.add(new Data("220000000493",	180  ));
        paDataList.add(new Data("220000000507",	0    ));
        paDataList.add(new Data("220000000508",	212  ));
        paDataList.add(new Data("220000000509",	0    ));
        paDataList.add(new Data("220000000510",	70   ));
        paDataList.add(new Data("220000000511",	0    ));
        paDataList.add(new Data("220000000512",	35   ));
        paDataList.add(new Data("220000000529",	0    ));
        paDataList.add(new Data("220000000530",	146  ));
        paDataList.add(new Data("220000000531",	0    ));
        paDataList.add(new Data("220000000532",	126  ));
        paDataList.add(new Data("220000000538",	0    ));
        paDataList.add(new Data("220000000539",	120  ));
        paDataList.add(new Data("220000000546",	0    ));
        paDataList.add(new Data("220000000547",	75   ));
        paDataList.add(new Data("220000000554",	0    ));
        paDataList.add(new Data("220000000555",	48   ));
        paDataList.add(new Data("220000000556",	0    ));
        paDataList.add(new Data("220000000557",	315  ));
        paDataList.add(new Data("220000000567",	0    ));
        paDataList.add(new Data("220000000571",	0    ));
        paDataList.add(new Data("220000000572",	6    ));
        paDataList.add(new Data("220000000616",	0    ));
        paDataList.add(new Data("220000000621",	20   ));
        paDataList.add(new Data("220000000622",	0    ));
        paDataList.add(new Data("220000000653",	0    ));
        paDataList.add(new Data("220000000654",	410  ));
        paDataList.add(new Data("220000000664",	0    ));
        paDataList.add(new Data("220000000665",	175  ));
        paDataList.add(new Data("220000000668",	0    ));
        paDataList.add(new Data("220000000669",	24   ));
        paDataList.add(new Data("220000000678",	0    ));
        paDataList.add(new Data("220000000679",	58   ));
        paDataList.add(new Data("220000000681",	0    ));
        paDataList.add(new Data("220000000689",	0    ));
        paDataList.add(new Data("220000000690",	274  ));
        paDataList.add(new Data("220000000821",	0    ));
        paDataList.add(new Data("220000000822",	295  ));
        paDataList.add(new Data("220000000826",	0    ));
        paDataList.add(new Data("220000000827",	115  ));
        paDataList.add(new Data("220000000828",	0    ));
        paDataList.add(new Data("220000000829",	90   ));
        paDataList.add(new Data("220000000888",	0    ));
        paDataList.add(new Data("220000000892",	13.5 ));
        paDataList.add(new Data("220000000962",	0    ));
        paDataList.add(new Data("220000000966",	48   ));
        paDataList.add(new Data("220000000967",	0    ));
        paDataList.add(new Data("220000000971",	27   ));
        paDataList.add(new Data("220000000975",	0    ));
        paDataList.add(new Data("220000000993",	19   ));
        paDataList.add(new Data("220000001006",	0    ));
        paDataList.add(new Data("220000001007",	0    ));
        paDataList.add(new Data("220000001008",	69   ));
        paDataList.add(new Data("220000001025",	25   ));
        paDataList.add(new Data("220000001026",	0    ));
        paDataList.add(new Data("220000001137",	0    ));
        paDataList.add(new Data("220000001138",	16   ));
        paDataList.add(new Data("220000001139",	0    ));
        paDataList.add(new Data("220000001140",	493  ));
        paDataList.add(new Data("230000001148",	8    ));
        paDataList.add(new Data("230000001280",	0    ));
        paDataList.add(new Data("230000001339",	0    ));
        paDataList.add(new Data("230000001340",	20   ));
        paDataList.add(new Data("230000001350",	0    ));
        paDataList.add(new Data("230000001360",	0    ));
        paDataList.add(new Data("230000001382",	0    ));
        paDataList.add(new Data("230000001399",	163  ));
        paDataList.add(new Data("230000001404",	43   ));
        paDataList.add(new Data("230000001411",	0    ));
        paDataList.add(new Data("230000001425",	0    ));
        paDataList.add(new Data("230000001430",	36   ));
        paDataList.add(new Data("230000001431",	0    ));
        paDataList.add(new Data("230000001452",	62   ));
        paDataList.add(new Data("230000001460",	0    ));
        paDataList.add(new Data("230000001461",	1311 ));
        paDataList.add(new Data("230000001462",	0    ));
        paDataList.add(new Data("230000001463",	217  ));
        paDataList.add(new Data("230000001469",	0    ));
        paDataList.add(new Data("230000001490",	0    ));
        paDataList.add(new Data("230000001491",	0    ));
        paDataList.add(new Data("230000001492",	22   ));
        paDataList.add(new Data("230000001495",	1000 ));
        paDataList.add(new Data("230000001498",	0    ));
        paDataList.add(new Data("230000001538",	65   ));
        paDataList.add(new Data("230000001586",	0    ));
        paDataList.add(new Data("230000001595",	0    ));
        paDataList.add(new Data("230000001614",	0    ));
        paDataList.add(new Data("230000001615",	27   ));
        paDataList.add(new Data("230000001616",	0    ));
        paDataList.add(new Data("230000001617",	74   ));
        paDataList.add(new Data("230000001666",	0    ));
        paDataList.add(new Data("230000001667",	27   ));
        paDataList.add(new Data("240000001715",	12   ));
        paDataList.add(new Data("240000001726",	0    ));
        paDataList.add(new Data("240000001727",	970  ));
        paDataList.add(new Data("240000001728",	0    ));
        paDataList.add(new Data("240000001729",	454  ));
        paDataList.add(new Data("240000001731",	0    ));
        paDataList.add(new Data("240000001732",	10   ));
        paDataList.add(new Data("240000001949",	0    ));
        paDataList.add(new Data("240000001960",	398  ));
        paDataList.add(new Data("240000001961",	0    ));
        paDataList.add(new Data("240000001991",	0    ));
        paDataList.add(new Data("240000002236",	0    ));
        paDataList.add(new Data("240000002237",	50   ));
        paDataList.add(new Data("240000002240",	0    ));
        paDataList.add(new Data("240000002241",	50   ));
        paDataList.add(new Data("240000002244",	0    ));
        paDataList.add(new Data("240000002245",	86   ));
        paDataList.add(new Data("240000002292",	0    ));
        paDataList.add(new Data("240000002293",	210  ));
        paDataList.add(new Data("240000002294",	0    ));
        paDataList.add(new Data("240000002295",	703  ));
        paDataList.add(new Data("240000002327",	0    ));
        paDataList.add(new Data("240000002328",	1200 ));
        paDataList.add(new Data("250000002473",	336  ));
        paDataList.add(new Data("250000002559",	0    ));
        paDataList.add(new Data("250000002560",	16   ));
        paDataList.add(new Data("250000002568",	0    ));
        paDataList.add(new Data("250000002569",	37   ));
        paDataList.add(new Data("250000002570",	0    ));
        paDataList.add(new Data("250000002571",	23   ));
        paDataList.add(new Data("250000002578",	0    ));
        paDataList.add(new Data("250000002579",	69   ));
        paDataList.add(new Data("250000002600",	60   ));
        paDataList.add(new Data("250000002620",	24   ));
        paDataList.add(new Data("250000002637",	0    ));
        paDataList.add(new Data("250000002638",	74   ));
        paDataList.add(new Data("250000002657",	0    ));
        paDataList.add(new Data("250000002658",	26   ));
        paDataList.add(new Data("250000002660",	1300 ));
        paDataList.add(new Data("250000002790",	0    ));
        paDataList.add(new Data("250000002791",	22   ));
        paDataList.add(new Data("250000002792",	0    ));
        paDataList.add(new Data("250000002793",	54   ));
        paDataList.add(new Data("250000002798",	0    ));
        paDataList.add(new Data("250000002799",	77   ));
        paDataList.add(new Data("250000002810",	0    ));
        paDataList.add(new Data("250000002811",	233  ));
        paDataList.add(new Data("250000002812",	0    ));
        paDataList.add(new Data("250000002813",	233  ));
        paDataList.add(new Data("250000002970",	0    ));
        paDataList.add(new Data("250000002971",	14   ));
        paDataList.add(new Data("250000002979",	0    ));
        paDataList.add(new Data("250000002980",	2400 ));
        paDataList.add(new Data("250000003136",	24   ));
        paDataList.add(new Data("250000003137",	36   ));
        paDataList.add(new Data("250000003139",	100));

        return true;
    }
}

class Data{
    private String id;
    private double qty;
        
    public Data(String id, 
                double qty
                ){
        this.id = id;
        this.qty = qty;
    }
    
    public void setId(String id){
        this.id = id;
    }
    public String getId(){
        return this.id;
    }
    
    public void setQty(double qty){
        this.qty = qty;
    }
    public double getQty(){
        return this.qty;
    }
}