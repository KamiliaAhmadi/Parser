/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

//package parser;

import java.beans.Statement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import static java.util.logging.Logger.global;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Kam
 */
public class InsertDataToDB {
    // JDBC driver name and database URL
    //static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/map";
    
    //  Database credentials
    static final String USER = "root";
    static final String PASS = "kamel";
    public static ArrayList<String> worldNodes;
    public static String [][] worldMatrixMacro;//this matrix keeps the macro path between each two nodes in the world. Includes MacroPaths
    public static String [][] worldMatrixMicro;//includes microPaths for only direct edges
    public int counterNewPaths=0;
    java.sql.Statement mySt;
    public Connection conn;
    public int detailedPathStepCounter=0;
    public int worldSize;
    public  int [][] next;//it is used in Floyd-Warshal algorithm
    
    public int pathCounter=0;//counts the number of all paths
    public void insert() throws ClassNotFoundException, SQLException, IOException
    {
        Class.forName("com.mysql.jdbc.Driver");
        conn = (Connection) DriverManager.getConnection(DB_URL, USER, PASS);
        System.out.println("Connected database successfully...");
        
        String source="";
        String destination="";
        String act="";
        int rIndex=0;
 
        mySt=conn.createStatement();    
        
        //clean DB tables
        String sqlQueryTruncate = "truncate table nodes";
        mySt.executeUpdate(sqlQueryTruncate);
        
        sqlQueryTruncate = "truncate table worldadjacency";
        mySt.executeUpdate(sqlQueryTruncate);
        
        
       //insert extracted data to DB
        for(int i=1; i<ParserDemo.extracted.size(); i++ )
        {
            source=dealWithSingleQuote(ParserDemo.extracted.get(i-1).LandMark);
            act=dealWithSingleQuote(ParserDemo.extracted.get(i).Action);
            destination=dealWithSingleQuote(ParserDemo.extracted.get(i).LandMark);
            
            if(ParserDemo.extracted.get(i).routeIndex!=rIndex)
            {
                rIndex=ParserDemo.extracted.get(i).routeIndex;
                continue;
            }
            
            rIndex=ParserDemo.extracted.get(i).routeIndex;
            String sqlQuery = "INSERT INTO nodes(source,action, destination, routeIndex)"+
                    "VALUES('"+source+"','"+act+"','"+ destination+"','"+rIndex+"')";
            
            mySt.executeUpdate(sqlQuery);
        }
        
        int numRoutes=ParserDemo.extracted.get(ParserDemo.extracted.size()-1).routeIndex+1;
        int routeStepsCount=0;
        
        worldNodes=new ArrayList<String>();
        for(int i=0; i<numRoutes; i++)
        {
            
            //finding steps
            String sqlQuerySteps = "SELECT count(*) from nodes where routeIndex="+i;
            ResultSet resSteps = mySt.executeQuery(sqlQuerySteps);
            
            while (resSteps.next()) {
                routeStepsCount=resSteps.getInt(1);
                break;
            }
            
            //finding source and destination of route
            String sqlQuerySourceDestination = "SELECT * from nodes where routeIndex="+i;
            ResultSet resSourceDestination = mySt.executeQuery(sqlQuerySourceDestination);
            
            ArrayList<ArrayList<String>> soureDestSteps=new ArrayList<ArrayList<String>>();
            while (resSourceDestination.next())
            {
                String src = resSourceDestination.getString("source");
                String des = resSourceDestination.getString("destination");
                ArrayList<String> temp=new ArrayList<>();
                temp.add(src);
                temp.add(des);
                soureDestSteps.add(temp);
                
            }
            String sourceNode= dealWithSingleQuote(soureDestSteps.get(0).get(0));
            String destinationNode= dealWithSingleQuote(soureDestSteps.get(soureDestSteps.size()-1).get(1));
            worldNodes=addNewNodeToWorldNodes(worldNodes, sourceNode);
            worldNodes=addNewNodeToWorldNodes(worldNodes, destinationNode);
            
            String sqlQuery2 = "INSERT INTO worldadjacency(sourceNode,destinationNode, weight, routeIndex)"+
                    "VALUES('"+sourceNode+"','"+destinationNode+"','"+ routeStepsCount+"','"+i+"')";
            
            mySt.executeUpdate(sqlQuery2);
        }
       
        
    }
    
    
    private String findPath(int i, int j, int[][] next) throws SQLException, ClassNotFoundException {
        int initial_i=i;
        int initial_j=j;
       
        if (next[i][j] == Integer.MIN_VALUE)
            return null;
        String macroPath = worldNodes.get(i);
        while (i != j)
        {
            i=next[i][j];
            macroPath=macroPath+";"+worldNodes.get(i);
        }
        //assigning macro path to world matrix
        worldMatrixMacro[initial_i+1][initial_j+1]=macroPath;
        
         
        //to check if path is new
        //String macroPathCopy=macroPath.replace(";", "");//because we don't want to pass ";" as a part of our queries and landmarks
        String [] macroPathSplitted=macroPath.split(";");
        
         String detailedPath="";
        if(macroPathSplitted.length>2)
        {
            worldMatrixMicro[initial_i+1][initial_j+1]=macroPath;
            counterNewPaths++;
            pathCounter++;//if the path is new it needs to be considered in all paths
        }
        else if (macroPathSplitted.length==2)
        {          
            //finding detailed path
            detailedPath=findDetailedPath(macroPathSplitted).trim();
            if(detailedPath.equalsIgnoreCase("No_Path"))
                return null;
            worldMatrixMicro[initial_i+1][initial_j+1]=detailedPath;
            pathCounter++;// if the path is not null, it counts in all paths
        }
        return detailedPath;
        
    }
     
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String dealWithSingleQuote(String lm) {
        lm=lm.trim();
        //StringBuilder sbLm=new StringBuilder(lm);
        lm=lm.replace("\'", "''");
        
        return lm;
        
    }
    
    private ArrayList<String> addNewNodeToWorldNodes(ArrayList<String> worldNodes, String currentNode) {
        if(worldNodes.size()==0){
            worldNodes.add(currentNode);
            return worldNodes;
        }
        
        if(!worldNodes.contains(currentNode)){
            worldNodes.add(currentNode);
            return worldNodes;
        }
        return worldNodes;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public  String findDetailedPath(String[] macroPathSplitted) throws SQLException, ClassNotFoundException {
        String detaildPath="";
        detailedPathStepCounter=0;
        for(int i=0; i<macroPathSplitted.length;i++)
            if(i+1<macroPathSplitted.length)
            {
                detaildPath= detaildPath+ findMicroPath(macroPathSplitted[i].trim(), macroPathSplitted[i+1].trim());
                if(detaildPath.trim().equals(""))
                    return "No_Path";
            }
        return detaildPath;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public String findMicroPath(String src, String dest) throws SQLException, ClassNotFoundException {
        
        src=src.replace(";", "");
        src=src.trim();
        
        dest=dest.replace(";", "");
        dest=dest.trim();
        
        int routeIndex=0;
        String steps="";
        String sqlQueryWorldAdjacency = "SELECT routeIndex from worldadjacency where sourceNode='"+src+"'and destinationNode='"+dest+"'";
        ResultSet resW = mySt.executeQuery(sqlQueryWorldAdjacency );
        if(resW.next())
        {
            routeIndex=resW.getInt(1);
            String sqlQueryNodes = "SELECT * from nodes where routeIndex='"+routeIndex+"'";
            ResultSet resN = mySt.executeQuery(sqlQueryNodes);
            
            while (resN.next()) {        
                steps=steps+resN.getString("source").replace(",","")+
                        ","+  resN.getString("action").replace(",","")+","+ resN.getString("destination").replace(",","")+"; \n";
                detailedPathStepCounter++;
            }
        }
        
        return steps;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String[][] initializeWorldMatrix(String[][] world, ArrayList<String> nodes, int size) {
        for(int i=1; i<size; i++)
            world[i][0]=nodes.get(i-1);
        
        for(int j=1; j<size; j++)
            world[0][j]=nodes.get(j-1);
        
        
        for(int i=1; i<size; i++)
            for(int j=1; j<size; j++)
                world[i][j]="";
        return world;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public void printWorldMatrix(String[][] worldMatrix) throws IOException {
        Calendar cal = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        
        int size=worldMatrix.length;
        
        //Blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook();
        
        //Create a blank sheet
        XSSFSheet sheet = workbook.createSheet("WorldMatrix");
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillBackgroundColor(IndexedColors.RED.getIndex());
        cellStyle.setFillPattern(CellStyle.SPARSE_DOTS);
        int rownum=0;
        for (int i=0; i<size; i++)
        {
            Row row = sheet.createRow(rownum++);
            int cellnum = 0;
            for (int j=0;j<size;j++)
            {
                Cell cell = row.createCell(cellnum++);
                cell.setCellValue(worldMatrix[i][j]);
                
                if(worldMatrix[i][j]!=null)
                    if(worldMatrixMacro[i][j].split(";").length>2)
                        cell.setCellStyle(cellStyle);
                
            }
        }
        try (FileOutputStream worldMatrixPrint = new FileOutputStream(new File("worldMatrixPrint.xlsx"))) {
            workbook.write(worldMatrixPrint);
        }
    }
    
    public void floydWarshal() throws SQLException {
        //floyd warshal algorithm
        worldSize=worldNodes.size();
        int weight=0;
        int[][] fwWorld=new int [worldSize][worldSize];
        for(int i=0;i<worldSize; i++) // for each vertex v -->   dist[v][v] ← 0
            fwWorld[i][i]=0;
        
        for(int i=0; i< worldSize; i++)
            for(int j=0; j< worldSize; j++)
            {
                weight=-1;
                String firstNode=worldNodes.get(i);
                String secondNode=worldNodes.get(j);
                String sqlQueryEdgeWeightsFW = "SELECT weight from worldadjacency where sourceNode='"+firstNode+"'and destinationNode='"+secondNode+"'";
                ResultSet resFW = mySt.executeQuery(sqlQueryEdgeWeightsFW );
                while (resFW.next()) {
                    weight=resFW.getInt(1);
                    break;
                }
                fwWorld[i][j]=weight;
                //fwWorld[j][i]=weight;
            }
        
        //core of floyd warshal algorithm
        double [][] dist=new double[worldSize][worldSize];//keeping the distance
        for(int i=0; i<worldSize; i++)//initialize weights to infinity
            for(int j=0; j<worldSize; j++)
                dist[i][j]=Double.POSITIVE_INFINITY;
        
        next=new int[worldSize][worldSize];//keeps the vertices for building the path
        for(int i=0; i<worldSize; i++)//initialize to null
            for(int j=0; j<worldSize; j++)
                next[i][j]=Integer.MIN_VALUE;
        
        for(int i=0; i<worldSize; i++) //put weight of edges in dist matrix
            for(int j=0; j<worldSize; j++)
                if(fwWorld[i][j]!=-1)
                {
                    dist[i][j]=fwWorld[i][j];
                    next[i][j]=j;
                }
        
        for (int k=0; k<worldSize; k++)//Standard body of floyd warshal algorithm
            for ( int i=0; i<worldSize; i++)
                for (int j=0; j<worldSize; j++)
                    if (dist[i][j] > dist[i][k] + dist[k][j])
                    {
                        dist[i][j]=dist[i][k] + dist[k][j];
                        next[i][j]=next[i][k];
                    }
    }
    
    public void buildWorld() throws IOException, SQLException, ClassNotFoundException {
        //buidling the world consisting path from every two nodes to each other.
        worldMatrixMacro=new String [worldSize+1][worldSize+1];
        worldMatrixMacro=initializeWorldMatrix(worldMatrixMacro, worldNodes, worldSize+1);//the first column and first row includes the world's nodes
        
        worldMatrixMicro=new String [worldSize+1][worldSize+1];
        worldMatrixMicro=initializeWorldMatrix(worldMatrixMicro, worldNodes, worldSize+1);//the first column and first row includes the world's nodes
        
        for(int i=0; i<worldSize; i++)
            for(int j=0; j<worldSize; j++)
                if(i!=j)
                {
                    System.out.println("src: "+ worldNodes.get(i));
                    System.out.println("dst: "+ worldNodes.get(j));
                    
                    findPath(i, j, next);
                }
        
        
        printWorldMatrix(worldMatrixMicro);
        conn.close();
        mySt.close();
        
        PrintWriter pathStatistics = new PrintWriter(new FileWriter(ParserDemo.outputPathFolder.toString()+"\\pathStatistics.txt"), true);          
        pathStatistics.write("\r\n");
        pathStatistics.write("The number of new paths are:  "+ counterNewPaths);
        pathStatistics.write("\r\n");
        pathStatistics.write("Number of total paths:\t"+ pathCounter);
        pathStatistics.write("\r\n");
        pathStatistics.write("\r\n");
        
        pathStatistics.close();
    }
    
}//end JDBC






