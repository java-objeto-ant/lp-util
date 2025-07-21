package org.rmj.lp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rmj.appdriver.GRider;

public class ParentToChildConversion {
    public static void main (String [] args){
    
    }
    
    
}

class P2CConvert{
    private GRider poGRider;
    private String psFileName;
    
    private List<Data> paDataList;
    
    public P2CConvert(GRider foValue){
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
        
        return true;
    }
    
    private boolean captureData() throws IOException{
        File file = new File(psFileName);
        
        try (
            FileInputStream fis = new FileInputStream(file); 
            Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(1);
            
            for (Row lnRow : sheet) {
                paDataList.add(new Data(lnRow.getCell(0).getStringCellValue()
                                        , lnRow.getCell(1).getNumericCellValue()
                                        , lnRow.getCell(2).getNumericCellValue()
                                        , lnRow.getCell(3).getStringCellValue()
                                        , lnRow.getCell(4).getNumericCellValue()));
            }            
        }
        return true;
    }
}

class Data{
    private String parentId;
    private String childId;
    private double parent2Child;
    private double parentQty;
    private double childQty;
    
    public Data(String parentId, 
                double parentQty,
                double parent2ChildConversion,
                String childId, 
                double childQty){
        this.parentId = parentId;
        this.parentQty = parentQty;
        this.parent2Child = parent2ChildConversion;
        this.childId = childId;
        this.childQty = childQty;
    }
    
    public void setParentId(String parentId){
        this.parentId = parentId;
    }
    public String getParentId(){
        return this.parentId;
    }
    
    public void setParentQty(double parentQty){
        this.parentQty = parentQty;
    }
    public double getParentQty(){
        return this.parentQty;
    }
    
    public void setParent2ChildConversion(double qty){
        this.parent2Child = qty;
    }
    public double getParent2ChildConversion(){
        return this.parent2Child;
    }
    
    public void setChildId(String childId){
        this.childId = childId;
    }
    public String getChildId(){
        return this.childId;
    }
    
    public void setChildQty(double childQty){
        this.childQty = childQty;
    }
    public double getChildQty(){
        return this.childQty;
    }
}