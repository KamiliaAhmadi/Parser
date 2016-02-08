

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class DrawMap {
    ArrayList<worldMatrixNode> nodes;//this list contains all of the world nodes and each node has
    //the number of incoming and outgoing edges (hubs) existing in the world.
    ArrayList<worldMatrixNode> sortedNodes;
    ArrayList<worldMatrixNode> hubs;//high demand nodes in terms of their incoming and outgoing edges
    
    static final String DB_URL = "jdbc:mysql://localhost:3306/map";
    static final String USER = "root";
    static final String PASS = "kamel";
    java.sql.Statement mySt;
    
    public int targetGeodiscDistance=5;
    public double percentOfNeighborsForDrwaing=0.25;//how many neighbors of each node do we aim to draw initially?
    
    public String [][] worldMatrixMacro;
    public String [][] worldMatrixMicro;
    public ArrayList<String> worldNodes;
    
    public DrawMap() throws IOException
    {
        worldMatrixMacro=readFromFileArray("worldMatrixMacro");
        worldMatrixMicro=readFromFileArray("worldMatrixMicro");
        
        worldNodes=readFromFileList("worldNodes");
        
        
        
        nodes=new ArrayList<worldMatrixNode>();
        sortedNodes=new ArrayList<worldMatrixNode>();
        hubs=new ArrayList<worldMatrixNode>();
    }
    
    public void draw() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
        updatNodeCharacteristics();
        nodeSorter();
        set_hub_neighbors();//this function gets high demand hubs and finds nodes with geodesic distance of 5 for drawing purposes.
        
        draw_graphVIZ();
        
    }
    
    private void updatNodeCharacteristics() {
        int numEdges=0;
        worldMatrixNode node=new worldMatrixNode();
        for(int i=0; i<worldNodes.size();i++)
        {
            node=new worldMatrixNode();
            node.numEdges=countEdges(worldNodes.get(i), i+1);
            node.nodeLandMark=worldNodes.get(i);
            nodes.add(node);
        }
        
    }
    
    public void nodeSorter() {
        ArrayList<worldMatrixNode> hubsCopy=new ArrayList<worldMatrixNode>();
        hubsCopy=nodes;
        
        worldMatrixNode maxNode=new worldMatrixNode();
        while(hubsCopy.size()!=0)
        {
            maxNode=findMax(hubsCopy);
            sortedNodes.add(maxNode);
            hubsCopy.remove(maxNode);
        }
    }
    
    private worldMatrixNode findMax(ArrayList<worldMatrixNode> list) {
        int maxValue=0;
        int maxIndex=0;
        for(int i=0; i<list.size(); i++)
            if(list.get(i).numEdges>maxValue)
            {
                maxValue=list.get(i).numEdges;
                maxIndex=i;
            }
        
        return list.get(maxIndex);
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void set_hub_neighbors() throws ClassNotFoundException, SQLException, IOException {
        findHubs();//hubs are high demand nodes
        
        for(int i=0; i<hubs.size(); i++)
        {
            System.out.println("num hubs: "+ Integer.toString(hubs.size())+"\n"+ hubs.get(i).nodeLandMark);
            hubs.get(i).setGeodesicNeighbors(findNeighbors(hubs.get(i), i));
        }
        
        PrintWriter hubsNeighborsWriter = new PrintWriter(new FileWriter("hubsNeighbors.txt"), true);
//       hubsNeighborsWriter.write("salam \n");
        
        for(int i=0; i<hubs.size();i++)
        {
            hubsNeighborsWriter.write(hubs.get(i).nodeLandMark+"\n");
            hubsNeighborsWriter.write("numEdges: "+hubs.get(i).numEdges+"\n");
            hubsNeighborsWriter.write(hubs.get(i).neighborsOfGeodesicDistance5+"\n");
            
        }
        hubsNeighborsWriter.close();
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<String> convertToString(ArrayList<worldMatrixNode> nodes)
    {
        if(nodes.size()==0)
            return null;
        
        ArrayList<String> nodesString=new ArrayList<String>();
        for(int i=0; i<nodes.size();i++)
            nodesString.add(nodes.get(i).nodeLandMark);
        
        return nodesString;
    }
    
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void findHubs() {
        int counter=0;
        int numEdegsOfHighestDemandHub=sortedNodes.get(0).numEdges;
        for(int i=0; i<sortedNodes.size(); i++)
            if(sortedNodes.get(i).numEdges==numEdegsOfHighestDemandHub)
                counter++;
        if(counter<sortedNodes.size()/10)
            counter=sortedNodes.size()/10;
        for(int i=0; i<counter; i++)
            hubs.add(sortedNodes.get(i));
        
//        counter=sortedNodes.size()/2;//10 percent of sorted nodes are needed here
//        int mid=sortedNodes.size()/2;
//        for(int i=mid;i<mid+counter;i++)
//            hubs.add(sortedNodes.get(i));
            
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private ArrayList<String> findNeighbors(worldMatrixNode node, int hubID) throws ClassNotFoundException, SQLException {

        ArrayList<String> ququeHelper=new ArrayList<String>();//keeps the nodes of the queue which won't be expanded anymore
        ArrayList<String> queue=new ArrayList<String>(); //the queue for BFS
        queue.add(node.nodeLandMark);
        
        ArrayList<String> neighborsInGD=new ArrayList<String>();
        //we put ";" between macro nodes
        while(queue.size()!=0 && queue.get(queue.size()-1).split(";").length!=targetGeodiscDistance)//when it arrives the frist path with length 6, means that we need to stop our search.
        {
            neighborsInGD=new ArrayList<String>();
            String toBeExpanded=queue.get(0);
            queue.remove(toBeExpanded);
            
            neighborsInGD=findNeighborsInGD(toBeExpanded);
            if(neighborsInGD.size()==1 && neighborsInGD.get(0).equalsIgnoreCase(toBeExpanded))
                ququeHelper.add(toBeExpanded);
            else
                queue=addAllWithoutInterative(queue, neighborsInGD);
        }
        
        return combine(ququeHelper, queue);
        
    }
    
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    //this function finds neighbors in geodesic distance of the target node
    private ArrayList<String> findNeighborsInGD(String target) throws ClassNotFoundException, SQLException {
        InsertDataToDB IDB=new InsertDataToDB();
        String[] targetSplitted=target.split(";");
        
        int targetSplittedSize=targetSplitted.length;
        if(targetSplittedSize==0)
            return null;
        
        String mustInclude=IDB.dealWithSingleQuote(targetSplitted[targetSplittedSize-1].trim());
        
        boolean lengthMoreThanOne=false;
        String exclude="";
        if(targetSplittedSize>1)
        {
            for(int i=0; i<targetSplittedSize-1;i++)
                exclude=exclude+"' and NOT destinationNode='"+IDB.dealWithSingleQuote(targetSplitted[i].trim());
            
            exclude=exclude.trim();
            lengthMoreThanOne=true;
        }
        
        ArrayList<String> geodesicDistanceNeighbors=new ArrayList<String>();
        
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = (Connection) DriverManager.getConnection(DB_URL, USER, PASS);
        System.out.println("Connected to database successfully from DrawMap class to find neighbors...");
        
        mySt=conn.createStatement();
        
        String sqlQuerySource="";
        if(!lengthMoreThanOne)
            sqlQuerySource = "SELECT  destinationNode from worldadjacency where sourceNode='"+ mustInclude+"'";
        else
            sqlQuerySource = "SELECT  destinationNode from worldadjacency where sourceNode='"+ mustInclude+
                    exclude+"'";
        
        ResultSet res = mySt.executeQuery(sqlQuerySource);
        
        while (res.next()) {
            if(!iterative(geodesicDistanceNeighbors, res.getString("destinationNode")))
                geodesicDistanceNeighbors.add(res.getString("destinationNode"));
        }
//        String sqlQueryDestination="";
//        if(!lengthMoreThanOne)
//            sqlQueryDestination = "SELECT  sourceNode from worldadjacency where destinationNode='"+ mustInclude+"'";
//        else
//            sqlQueryDestination = "SELECT  sourceNode from worldadjacency where destinationNode='"+ mustInclude+
//                    "'and NOT sourceNode='"+exclude+"'";
//        
//        ResultSet resDestination = mySt.executeQuery(sqlQueryDestination);
//        
//        while (resDestination.next()) {
//            if(!iterative(geodesicDistanceNeighbors, resDestination.getString("sourceNode")))
//                geodesicDistanceNeighbors.add(resDestination.getString("sourceNode"));
//        }
        
        conn.close();
        mySt.close();
        
        ArrayList<String> newNeighbors=new ArrayList<String>();
        if(geodesicDistanceNeighbors.size()==0)//means that there is no new neighbor to be added
        {
            newNeighbors=new ArrayList<String>();
            newNeighbors.add(target);
            return newNeighbors;
        }
        newNeighbors=new ArrayList<String>();
        for (int i=0; i<geodesicDistanceNeighbors.size(); i++)
        {
            
            String builtNeighbor=target;
            builtNeighbor=builtNeighbor+";"+geodesicDistanceNeighbors.get(i);
            newNeighbors.add(builtNeighbor);
        }
        
        return newNeighbors;
        
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private boolean iterative(ArrayList<String> list, String goal) {
        for(int i=0; i<list.size(); i++)
            if(list.get(i).equalsIgnoreCase(goal))
                return true;
        
        return false;
    }
    
    
    private void draw_graphVIZ() throws IOException, InterruptedException, SQLException, ClassNotFoundException {
        PrintWriter out=null;
        String[][] localMatrix;
        
        for(int i=0; i<hubs.size(); i++)
        {

            localMatrix=produceLocalMatrix(hubs.get(i));//we produce a local matrix for each HUB and draw each hub independantly
            make_graph(localMatrix, hubs.get(i));
        }
        
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
     public ArrayList<String> bodyOfLocalMatrix;
    private String[][] produceLocalMatrix(worldMatrixNode hub) throws SQLException, ClassNotFoundException {
        //ArrayList<String> bodyOfLocalMatrix=new ArrayList<String>();//keeps all of the nodes for local world matix including the hub
       bodyOfLocalMatrix=new ArrayList<String>();
        //I decided to not add all of the neighbors just 1/3 of it for simplifying drawing purposes
//        for(int i=0; i<hub.neighborsOfGeodesicDistance5.size()/4;i++)
//        {
//           bodyOfLocalMatrix.add(hub.neighborsOfGeodesicDistance5.get(i)); 
//        }
        
        bodyOfLocalMatrix.addAll(findNodesOfConnectedComponent(hub.neighborsOfGeodesicDistance5));
        
        if(!iterative(bodyOfLocalMatrix, hub.nodeLandMark))
            bodyOfLocalMatrix.add(hub.nodeLandMark);
        
        
        String [][] localMatrix=new String[bodyOfLocalMatrix.size()][bodyOfLocalMatrix.size()];
        for(int i=0; i<bodyOfLocalMatrix.size();i++)
        {
            for(int j=0; j<bodyOfLocalMatrix.size(); j++)
            {
                String src=bodyOfLocalMatrix.get(i);
                String dest=bodyOfLocalMatrix.get(j);
                
                if(src.equalsIgnoreCase(dest))
                    continue;
                
                int srcIndex=worldNodes.indexOf(src);
                int destIndex=worldNodes.indexOf(dest);
                
                if(worldMatrixMacro[srcIndex+1][destIndex+1]=="" || worldMatrixMacro[srcIndex+1][destIndex+1]==null)
                    localMatrix[i][j]="";
                else
                {
                    String [] macroPath=worldMatrixMacro[srcIndex+1][destIndex+1].split(";");
                    
                    if(macroPath.length > 2)//means that this path is newpath and we just need macro nodes
                    {
                        localMatrix[i][j]=worldMatrixMacro[srcIndex+1][destIndex+1];
                        localMatrix=updateLocalMatrixForMacroPaths(macroPath, localMatrix, bodyOfLocalMatrix);
                    }
                    else
                        localMatrix[i][j]=worldMatrixMicro[srcIndex+1][destIndex+1];
                }
            }
        }
        
        return localMatrix;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    
    private ArrayList<String> combine(ArrayList<String> helper, ArrayList<String> queue) {
        
        ArrayList<String> combined=new ArrayList<String>();
        if(helper.size()!=0)
            for(int i=0; i<helper.size();i++)
                for(int j=0; j<Arrays.asList(helper.get(i).split(";")).size();j++)
                    if(!iterative(combined, Arrays.asList(helper.get(i).split(";")).get(j)))
                        combined.add(Arrays.asList(helper.get(i).split(";")).get(j));
        
        if(queue.size()!=0)
            for(int i=0; i<queue.size();i++)
                for(int j=0; j<Arrays.asList(queue.get(i).split(";")).size(); j++)
                    if(!iterative(combined, Arrays.asList(queue.get(i).split(";")).get(j)))
                        combined.add(Arrays.asList(queue.get(i).split(";")).get(j));
        
        combined.remove(0);
        return combined;
    }
    
    private String makeString(String[] macroPath) {
        String wholeString="";
        for(int i=0; i<macroPath.length;i++)
            wholeString=wholeString+macroPath[i];
        
        return wholeString.trim();
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Based on the direction of the edges, we consider the nodes as neighbors if from A we have an edge to B like A->B
    //Then, since we need to have direct edges between nodes, fro finding hubs we need to count the numebr of edges going out from a node.
    
    private int countEdges(String node, int index) {
        int count=0;
//        for(int i=1; i<worldMatrixMacro.length;i++)
//            if(worldMatrixMacro[i][index]!=null && worldMatrixMacro[i][index].length()>0)
//                count++;
        
        for(int j=1; j<worldMatrixMacro.length;j++)
            if(worldMatrixMacro[index][j]!=null && worldMatrixMacro[index][j].length()>0)
                count++;
        
        return count;
        
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void make_graph(String[][] localMatrix, worldMatrixNode hub) throws IOException, InterruptedException {
        
        System.out.println("Start making graphs");
        String fileName=hub.nodeLandMark;
        PrintWriter out=new PrintWriter(new FileWriter("graphs\\"+fileName+".txt"), true);
        out.write("digraph G\n{");
        String ptransColor="blue";
        String macroNodeColor="black";
        String microNodeColor="green";
        String shortestPathColor="red";
        int fontsizeMicro=8;
        int fontsizeMacro=16;
//        if(fontsizeMicro==4)
//            return;
        int microNodeCounter= bodyOfLocalMatrix.size();
        HashMap<String, Integer> nodeMap = new HashMap<>();
        HashMap<String, Boolean> alreadyDrawnMacropaths = new HashMap<>();
        out.write(createMacroNodesHeader(hub, nodeMap,macroNodeColor, fontsizeMacro));
        for(int i=0; i<localMatrix.length;i++){
            for(int j=0; j<localMatrix[0].length;j++){
                String path=localMatrix[i][j];
                if(path!=null && path.length()>0)//mean that there is a path between nodes i and j
                {
                    if(path.contains("STEP"))
                    {
                        String [] pathSplitted=path.split("STEP");
                        int lastIndex=pathSplitted.length-1;
                        //make sure that the last element is not empty
                        if(pathSplitted[lastIndex].length()==0)
                            lastIndex--;
                        boolean isFirstEdge=true;
                        String startingPoint="";
                        for(int k=0; k<=lastIndex;k++)
                        {
                          
                            String[] steps=pathSplitted[k].replace(";", "").split(",");
                            if(k==0)
                                startingPoint= steps[0];
                            if(steps.length<3)
                                continue;
                            
                            if(!steps[1].contains("MTrans"))
                            {
                                if(k!=lastIndex)
                                {
                                    out.write(String.format("s%d [label=\"%s\", color=%s, fontcolor=%s, fontsize=%d ];\n",microNodeCounter++,steps[2], microNodeColor,ptransColor, fontsizeMicro));
                                }
                                int sourceID=microNodeCounter-2;
                                int targetID=microNodeCounter-1;
                                if(isFirstEdge)
                                {
                                    sourceID= nodeMap.get(startingPoint);
                                    isFirstEdge=false;
                                }
                                else if(k==lastIndex)
                                {
                                    sourceID=microNodeCounter-1;
                                    targetID=nodeMap.get(steps[2]);
                                }
                                String edgeText= steps.length>1  && steps[1]!=null?steps[1]:"";
                                out.write(String.format("s%d -> s%d [label=\"%s\", color=%s, fontsize=%d];\n",sourceID, targetID,edgeText,ptransColor, fontsizeMicro));
                            }
                        }
                    }
                    else
                    {
//                        String temp=path.trim().replace(",","").replace(";","");
                        String [] steps=path.split(";");
//                        if(alreadyDrawnMacropaths.containsKey(temp))
//                            continue;
//                        alreadyDrawnMacropaths.put(temp, Boolean.TRUE);
                        int sourceID= nodeMap.get(steps[0].replace(",",""));
                        int targetID=nodeMap.get(steps[steps.length-1].replace(",",""));
                        out.write(String.format("s%d -> s%d [label=\"%s\", color=%s, fontsize=%d];\n",sourceID, targetID,path.replace(";", "\n"),shortestPathColor, fontsizeMacro));
                        
                        
                    }
                }
            }
        }
        out.write("}");
        out.close();
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(String.format("%sdot -Tpng \"%s\" -o \"%s\"",ParserDemo.dotAddress, "graphs\\"+fileName+".txt",
                "graphs\\"+fileName+".png"));
        p.waitFor();
    }
    
    private String createMacroNodesHeader(worldMatrixNode hub, HashMap<String, Integer> nodeIDs, String fontColor, int fontSize) {
        String nodeListHeader="";
        int i=0;
        for(i=0;i<bodyOfLocalMatrix.size();i++)
        {
            nodeListHeader +=String.format("s%d [label=\"%s\", color=%s, fontcolor=%s, fontsize=%d ];\n",i,bodyOfLocalMatrix.get(i), fontColor,fontColor, fontSize);
            nodeIDs.put(bodyOfLocalMatrix.get(i).replace(",",""), i);
        }
//        nodeListHeader +=String.format("s%d [label=\"%s\", color=%s, fontcolor=%s, fontsize=%d ];\n",i,hub.nodeLandMark, fontColor,fontColor, fontSize);
//        nodeIDs.put(hub.nodeLandMark.replace(",",""), i);
        return nodeListHeader;
    }
    
    private String[][] readFromFileArray(String name) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(name+".txt"));
        String line = br.readLine();
        
        
        int size=line.split("\t").length;
        String [][] data=new String[size][size];
        int lineCounter=0;
        
        while (line != null) {
            
            String[] lineSplitted=line.split("\t");
            for(int j=0; j<lineSplitted.length;j++)
                data[lineCounter][j]=lineSplitted[j];
            line = br.readLine();
            lineCounter++;
        }
        
        return data;
    }
    
    private ArrayList<String> readFromFileList(String name) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(name+".txt"));
        String line = br.readLine();
        String [] lineSplitted=line.split("\t");
        
        ArrayList<String> data=new ArrayList<String>();
        
        for(int i=0; i<lineSplitted.length;i++)
            data.add(lineSplitted[i]);
        
        return data;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private ArrayList<String> addAllWithoutInterative(ArrayList<String> queue, ArrayList<String> toBeAdded) {
        for(int i=0; i<toBeAdded.size();i++)
            if(!iterative(queue, toBeAdded.get(i)))
                queue.add(toBeAdded.get(i));
        
        return queue;
    }
    
    
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private ArrayList<String> findNodesOfConnectedComponent(ArrayList<String> neighborsOfGeodesicDistance5) {
       ArrayList<String> body=new ArrayList<String>();
       for(int i=0; i<neighborsOfGeodesicDistance5.size()*percentOfNeighborsForDrwaing;i++)
           body.add(neighborsOfGeodesicDistance5.get(i));
       
       for(int i=0; i<body.size();i++)
           for(int j=0;j<body.size(); j++)
           {
               if(i==j)
                   continue;
               int srcIndex=worldNodes.indexOf(body.get(i));
               int dstIndex=worldNodes.indexOf(body.get(j));
               
               if(worldMatrixMacro[srcIndex+1][dstIndex+1]!=null && !worldMatrixMacro[srcIndex+1][dstIndex+1].isEmpty())
               {
                   String [] macroPath=worldMatrixMacro[srcIndex+1][dstIndex+1].split(";");
                   if(macroPath.length > 2)
                       for(int k=0; k<macroPath.length;k++)
                           if(!body.contains(macroPath[k]))
                               body.add(macroPath[k]);
               }     
           }       
    return body;
    
    
    
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String[][] updateLocalMatrixForMacroPaths(String[] macroPath, String[][] localMatrix, ArrayList<String> bodyOfLocalMatrix) {
        for(int i=0; i<macroPath.length-1;i++)
        {
               
                String src=macroPath[i];
                String dest=macroPath[i+1];
                
                int srcIndexWorldNodes=worldNodes.indexOf(src);
                int destIndexWorldNodes=worldNodes.indexOf(dest);
                
                int srcIndexBodyMatrix=bodyOfLocalMatrix.indexOf(src);
                int destIndexBodyMatrix=bodyOfLocalMatrix.indexOf(dest);
                
                if(srcIndexBodyMatrix<0 || destIndexBodyMatrix<0)
                    continue;
                
                localMatrix[srcIndexBodyMatrix][destIndexBodyMatrix]=worldMatrixMicro[srcIndexWorldNodes+1][destIndexWorldNodes+1];
                
                
            }
        
        return localMatrix;
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
}



