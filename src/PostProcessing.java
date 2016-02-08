

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.

 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Kam
 */
public class PostProcessing {

    
    String mtransColor="green";
    String ptransColor="black";
    
    public ArrayList<LandMarkAction> eliminateOverlaps(ArrayList<LandMarkAction> landmarks){
        Boolean[] isMerged=new Boolean[landmarks.size()];
        Arrays.fill(isMerged, Boolean.FALSE);
        ArrayList<LandMarkAction> refinedLandMarks=new ArrayList<>();
        LandMarkAction currentLandmark;
        for(int i=0;i<landmarks.size();i++){
            if(isMerged[i])
                continue;
            currentLandmark=landmarks.get(i);
            for(int j=i+1;j<landmarks.size();j++){
                if(landmarks.get(i).routeIndex != landmarks.get(j).routeIndex || landmarks.get(i).rowIndex != landmarks.get(j).rowIndex)
                    break;
                
                if(checkForOverlap(currentLandmark,landmarks.get(j))){
                    currentLandmark=mergeLandMarks(currentLandmark,landmarks.get(j));
                    isMerged[j]=true;
                }
            }
            refinedLandMarks.add(currentLandmark);
        }
        return refinedLandMarks;        
    }
    
    
    public void makeGraphs(ArrayList<LandMarkAction> landmarks) throws IOException, InterruptedException{
        int currentRouteIndex=-1;
        LandMarkAction currentLandmark;
        PrintWriter out=null;
        for(int i=0;i<landmarks.size();i++){
            currentLandmark=landmarks.get(i);
            if(!isPtrans(currentLandmark))
                continue;
            if(currentLandmark.routeIndex!=currentRouteIndex){
                if(out!=null)
                {
                    out.write("}");
                    out.close();
                    String fileName=ParserDemo.graphsLocation+"Route"+ ParserDemo.extracted.get(i-1).routeIndex;
                    Runtime r = Runtime.getRuntime();
                    Process p = r.exec(String.format("%sdot -Tpng \"%s\" -o \"%s\"",ParserDemo.dotAddress, fileName+".txt",fileName+".png"));
                    p.waitFor();
                }
                out = new PrintWriter(new FileWriter(ParserDemo.graphsLocation+"Route"+currentLandmark.routeIndex+".txt"), true);
                out.write("digraph G\n{");
                currentRouteIndex=currentLandmark.routeIndex;
                out.write(createNodeList(landmarks,i));
            }
            int nextPtransIndex=findNextPtrans(landmarks,i);
            
            if(i!=nextPtransIndex)
                out.write(String.format("s%d -> s%d [label=\"%s\", color=%s];\n",i,nextPtransIndex,ParserDemo.extracted.get(nextPtransIndex).Action,ptransColor));
            for(int j=i+1;j<nextPtransIndex;j++){
                out.write(String.format("s%d -> s%d [label=\"%s\", color=%s];\n",i,j,ParserDemo.extracted.get(j).Action,mtransColor));
                out.write(String.format("s%d -> s%d [color=%s];\n",j,nextPtransIndex,mtransColor));
            }
        }
        if(out==null)
            return;
        out.write("}");
        out.close();
        String fileName=ParserDemo.graphsLocation+"Route"+ ParserDemo.extracted.get(ParserDemo.extracted.size()-1).routeIndex;
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(String.format("%sdot -Tpng \"%s\" -o \"%s\"",ParserDemo.dotAddress, fileName+".txt",fileName+".png"));
        p.waitFor();
    }

    
    private int findNextPtrans(ArrayList<LandMarkAction> landmarks, int index) {
        LandMarkAction landmark= landmarks.get(index);
        for(int i=index+1;i<landmarks.size();i++){
            if(landmarks.get(i).routeIndex!=landmark.routeIndex)
                return i-1;
            if(isPtrans(landmarks.get(i)))
                return i;
        }
        return landmarks.size()-1;
    }
    
    private boolean isPtrans(LandMarkAction landmark) {
        if(landmark.Action.toLowerCase().contains("ptrans"))
            return true;
//        if(landmark.actionType==ActionCharacteristics.PTRANS)
//            return true;
        return false;
    }
        
    private boolean checkForOverlap(LandMarkAction l1, LandMarkAction l2) {
        if(l1.sentenceIndex!=l2.sentenceIndex)
            return false;
        if(l1.LandMark.length()<l2.LandMark.length()){
            LandMarkAction temp=l1;
            l1=l2;
            l2=temp;
        }
      
        return l1.LandMark.toLowerCase().contains(l2.LandMark.toLowerCase());
    }

    private LandMarkAction mergeLandMarks(LandMarkAction l1, LandMarkAction l2) {
        if(l1.LandMark.length()<l2.LandMark.length()){
            LandMarkAction temp=l1;
            l1=l2;
            l2=temp;           
        }
        //We may need a more complicated merging technique later, but for now it works!
        
        return l1;
    }    

    private String createNodeList(ArrayList<LandMarkAction> landmarks, int index) {
        int currentRoutIndex=landmarks.get(index).routeIndex;
        String header="";
        LandMarkAction lma;
        for(int i=index;i<landmarks.size();i++)
        {
            lma=landmarks.get(i);
            if(lma.routeIndex!=currentRoutIndex)
                return header;
            String text=landmarks.get(i).LandMark;
            if(text.toLowerCase().contains("no landmark"))
                text="";
            if(isPtrans(lma))
                header+=String.format("s%d [label=\"%s\", color=%s, fontcolor=%s ];\n",i,text, ptransColor,ptransColor);
            else
                header+=String.format("s%d [label=\"%s\", color=%s, fontcolor=%s ];\n",i,text, mtransColor,mtransColor);
        }
        return header;
    }
}
