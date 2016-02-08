
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.StringUtils;
import java.util.List;

/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

/**
 *
 * @author Kam
 */
public class ActionCharacteristics {
    String Action;
    String direction;
    String measurement;
    String width;
    String material;
    public static int PTRANS=1;
    public static int MTRANS=2;
    
    
    public ActionCharacteristics()
    {
        
    }
    
    public static String [] directions={"right", "left", "straight", "north", "south", "west", "east", "ahead"
            ,"right,", "left,", "stright,", "north,", "south,", "west,", "east,", "ahead,", "down", "down,","up","up,"};
    public static String [] widthMarker={"wide", "width", "wide,"};
    public static String [] materialMarker={"carpet", "stone", "marble", "concrete", "grass", "carpet,", "stone,", "marbel,", "concrete,", "grass,", "brick", "brick,"
            ,"brick-surfaced", "smooth concrete", "brick-surfaced,", "smooth concrete,"};
    public static String[] measurementMarkers={"feet", "feet,", "inches", "inches,", "block", "block,", "blocks", "blocks"};
    
    
    
    public ActionCharacteristics extractActionCharacteristics(Tree root, Tree marker, int nodeNumber, LandMarkAction lma)
    {
        ActionCharacteristics ac=new ActionCharacteristics();
        
        if((lma.Action==null)||(lma.Action.equalsIgnoreCase(""))||(lma.Action.equalsIgnoreCase(Extraction.notFoundString))||
                (lma.Action.equalsIgnoreCase("MTrans/Sense/See"))
                || (lma.Action.equalsIgnoreCase("PTrans/Move")))
            return ac;
        boolean actionmovingDirectionRight=findActionDirectionBasedOnLandMark(lma);
        List<Tree> leaves=root.getLeaves();
        int markerIndex=findNodeIndex(leaves, root, marker, nodeNumber);
        
        Tree verbNode=findVerbNode(markerIndex, leaves, lma, actionmovingDirectionRight);
        if(verbNode==null)
            return ac;
       
        ac.Action=lma.Action;
        ac.direction=extractDirection(leaves, verbNode,verbNode.nodeNumber(root),marker,marker.nodeNumber(root), root, lma);
        ac.material= extractMaterial(leaves, verbNode,verbNode.nodeNumber(root),marker,marker.nodeNumber(root), root, lma);
        ac.measurement= extractMeasurement(leaves,verbNode,verbNode.nodeNumber(root),marker,marker.nodeNumber(root), root, lma);
        ac.width= extractWidth(leaves,verbNode,verbNode.nodeNumber(root),marker,marker.nodeNumber(root), root, lma);
        
        
        return ac;
    }
    
    //Direction of the action related to the marker
    private boolean findActionDirectionBasedOnLandMark(LandMarkAction lma) {
        boolean right=false;
        if((lma.passive!=null)||(lma.gerund!=null))//verb is in the right side of the marker otherwise verb is in the left side of the marker
            right=true;
        return right;
    }
    
    private int findNodeIndex(List<Tree> leaves, Tree root, Tree node, int nodeNumber) {
        for(int i=0;i<leaves.size();i++)
        {
            if((leaves.get(i).value().equalsIgnoreCase(node.getChild(0).label().value()))
                    &&(leaves.get(i).nodeNumber(root)==nodeNumber+1))
                return i;
        }
        return -1;
    }
    
    private Tree findVerbNode(int markerIndex, List<Tree> leaves, LandMarkAction lma, boolean actionmovingDirectionRight) {
        
        if(lma.gerund!=null)//If the case is gerund, teh verbnode is same as marker
        {
            return leaves.get(markerIndex);
            
        }
        
        int startSearchIndex=-1;
        int endSearchIndex=-1;
        if(actionmovingDirectionRight)
        {
            startSearchIndex=markerIndex;
            endSearchIndex=leaves.size();
        }
        else
        {
            startSearchIndex=markerIndex;
            endSearchIndex=-1;
        }
        Tree actionNode=null;
        int counter= startSearchIndex;
        String action=lma.Action.trim();
        String [] splittedAction=action.split(" ");
        action=splittedAction[0];
        
        
        if((action.equalsIgnoreCase("PTrans/Move"))||(action.equalsIgnoreCase("MTrans/Sense/See")))
        {
            if(splittedAction.length==1) //The case where the action just infered!
                return null;
            if(splittedAction.length>1)
                action=splittedAction[1];
        }
        
        while(counter!=endSearchIndex)
        {
            if(leaves.get(counter).value().toString().equalsIgnoreCase(action))
            {
                actionNode=leaves.get(counter);
                break;
            }
            if(actionmovingDirectionRight)
                counter++;
            else
                counter--;
        }
        return actionNode;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String extractDirection(List<Tree> leaves, Tree verbNode, int verbNodeNumber, Tree marker, int markerNodeNumber, Tree root, LandMarkAction lma) {
        int startSearchInleaves=-1;
        int endSearchInleaves=-1;
        
            for(int i=0;i<leaves.size();i++)
            {
                if((leaves.get(i).label().value().equalsIgnoreCase( verbNode.label().value()))
                        &&(leaves.get(i).nodeNumber(root)==verbNodeNumber))
                {
                    startSearchInleaves=i;
                    break;
                }
            }
            
            endSearchInleaves=calculateEndSearchIndex(startSearchInleaves, leaves, root, marker, markerNodeNumber, lma );
       
        
        for(int i=startSearchInleaves;i<endSearchInleaves;i++)
        {
            
            String direction1=searchforMarker(directions, leaves.get(i).label().toString());
            if(direction1!=null)
                return direction1;
        }
        
        String direction2=searchforMarker(directions, lma.Action);
        return direction2;
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String extractMaterial(List<Tree> leaves,Tree verbNode, int verbNodeNumber, Tree marker, int markerNodeNumber,  Tree root, LandMarkAction lma) {
        int startSearchInleaves=-1;
        int endSearchInleaves=-1;
        
        for(int i=0;i<leaves.size();i++)
        {
            if((leaves.get(i).label().value().equalsIgnoreCase( verbNode.label().value()))
                    &&(leaves.get(i).nodeNumber(root)==verbNodeNumber))
                startSearchInleaves=i;
        }
        endSearchInleaves=calculateEndSearchIndex(startSearchInleaves, leaves, root, marker, markerNodeNumber, lma );
        
        for(int i=startSearchInleaves;i<endSearchInleaves;i++)
        {
            String material=searchforMarker(materialMarker, leaves.get(i).label().toString());
            if(material!=null)
                return material;
        }
        String material2=searchforMarker(materialMarker, lma.Action);
        return material2;
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private String extractMeasurement(List<Tree> leaves,Tree verbNode, int verbNodeNumber, Tree marker, int markerNodeNumber, Tree root, LandMarkAction lma) {
        int startSearchInleaves=-1;
        int endSearchInleaves=-1;
        for(int i=0;i<leaves.size();i++)
        {
            if((leaves.get(i).label().value().equalsIgnoreCase( verbNode.label().value()))
                    &&(leaves.get(i).nodeNumber(root)==verbNodeNumber))
                startSearchInleaves=i;
        }
        
        endSearchInleaves=calculateEndSearchIndex(startSearchInleaves, leaves, root, marker, markerNodeNumber, lma );
        
        int test1=endSearchInleaves;
        
        for(int i=startSearchInleaves;i<endSearchInleaves;i++)
        {
            String measurementMarker=searchforMarker(measurementMarkers, leaves.get(i).label().toString());
            if(measurementMarker!=null)
            {
                if((i-1>=0)&& (StringUtils.isNumeric(leaves.get(i-1).label().value())))
                    return leaves.get(i-1).label().value()+" "+measurementMarker;
                if((measurementMarker.equalsIgnoreCase("block"))||(measurementMarker.equalsIgnoreCase("blocks"))||
                        (measurementMarker.equalsIgnoreCase("block,"))||(measurementMarker.equalsIgnoreCase("blocks,")))
                    return extractMeasurementOfBlock(leaves.get(i),leaves, root);
            }
        }
        //String measurementMarker2=searchforMarker(measurementMarkers, lma.LandMark);
        return null;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String extractWidth(List<Tree> leaves,Tree verbNode, int verbNodeNumber, Tree marker, int markerNodeNumber,  Tree root, LandMarkAction lma) {
        int startSearchInleaves=-1;
        int endSearchInleaves=-1;
        for(int i=0;i<leaves.size();i++)
        {
            if((leaves.get(i).label().value().equalsIgnoreCase( verbNode.label().value()))
                    &&(leaves.get(i).nodeNumber(root)==verbNodeNumber))
                startSearchInleaves=i;
        }
        endSearchInleaves=calculateEndSearchIndex(startSearchInleaves, leaves, root, marker, markerNodeNumber, lma );
        
        for(int i=startSearchInleaves;i<endSearchInleaves;i++)
        {
            String widthString=searchforMarker(widthMarker, leaves.get(i).label().toString());
            if(widthString!=null)
            {
                if(hasDash(leaves.get(i).label().toString()))
                {
                    return leaves.get(i).label().toString();
                }
                else if((i-1>=0)&& (StringUtils.isNumeric(leaves.get(i-1).label().value())))
                    return leaves.get(i-1).label().value()+" "+widthString;
            }
        }
        return null;
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
    
    public static String searchforMarker(String [] dictionary, String toCheck) {
        String [] dic=dictionary;
        toCheck=toCheck.trim();
        String [] splittedToCheck=toCheck.split(" |_|-");
        for(int i=0;i<splittedToCheck.length;i++)
            for(int j=0;j<dic.length;j++)
            {
                if(splittedToCheck[i].equalsIgnoreCase(dic[j]))
                    return dic[j];
            }
        return null;
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String[] blockMarker={"a", "the", "one", "two", "three", "four", "five"};
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String extractMeasurementOfBlock(Tree node, List<Tree> leaves, Tree root) {
        int index=-1;
        for(int i=0;i<leaves.size();i++)
        {
            if((leaves.get(i).label().value().equalsIgnoreCase(node.label().value()))
                    &&(leaves.get(i).nodeNumber(root)==node.nodeNumber(root)))
            {
                index=i;
                break;
            }
        }
        if((index-1>0)&&(searchforMarker(blockMarker, leaves.get(index-1).label().value())!=null))
            return leaves.get(index-1).label().value()+" "+leaves.get(index).label().value();
        
        if((index-2>0)&&(searchforMarker(blockMarker, leaves.get(index-2).label().value())!=null))
            return leaves.get(index-2).label().value()+" "+leaves.get(index-1).label().value()+" "+leaves.get(index).label().value();
        
        if((index-3>0)&&(searchforMarker(blockMarker, leaves.get(index-3).label().value())!=null))
            return leaves.get(index-3).label().value()+" "+leaves.get(index-2).label().value()+
                    " "+leaves.get(index-1).label().value()+" "+leaves.get(index).label().value();
        
        return null;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private int calculateEndSearchIndex(int startSearchInleaves, List<Tree> leaves, Tree root, Tree marker, int markerNodeNumber, LandMarkAction lma) {
       
        
        if(
                (lma.LandMark==null)
                ||(lma.LandMark.equalsIgnoreCase(""))
                ||(lma.LandMark.equalsIgnoreCase(Extraction.notFoundString))
                ||(lma.LandMark.equalsIgnoreCase(" "))
                ||(lma.LandMark.equalsIgnoreCase(Extraction.noSiblingString))
                ||(lma.LandMark.equalsIgnoreCase(Extraction.phraseStartingWithConnectorString))
                )
            return -1;
        
         lma.LandMark=lma.LandMark.trim();
        String [] landMarkSplitted=lma.LandMark.split(" ");
        
        for(int i=startSearchInleaves;i<leaves.size();i++)
        {
            if((leaves.get(i).nodeNumber(root)> markerNodeNumber)
                    &&(leaves.get(i).label().value().equalsIgnoreCase(landMarkSplitted[landMarkSplitted.length-1])))
                return i;
        }
        return -1;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private boolean hasDash(String widthString) {
        if((widthString.contains("-"))||(widthString.contains("_")))
            return true;
        return false;
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
