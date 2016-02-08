
import edu.stanford.nlp.trees.Tree;
import java.util.ArrayList;
import java.util.List;


public class NodeFunctionalities {
    
    public static Tree getNextRightSibling(Tree currentNode, Tree parse, int nodeNumber) {
        List<Tree> siblings= currentNode.siblings(parse);
        for(int i=0;i<siblings.size();i++)
        {
            if(siblings.get(i).nodeNumber(parse)>nodeNumber)
                return siblings.get(i);
        }
        return null;
    }
    
    
    public  ArrayList<String> produceLeavesStrings(Tree parse) {
        List<Tree> leaves=parse.getLeaves();
        ArrayList<String> stringsOfTheLeaves=new ArrayList<String> ();
        for(int i=0; i< leaves.size(); i++)
            stringsOfTheLeaves.add(leaves.get(i).toString());
        return stringsOfTheLeaves;
    }
  
    
    public static Tree findPreviousNodeBasedOnNodeNumber(Tree temp, int nodeNumber, List<Tree> leaves, Tree root) {
        nodeNumber++;
        for(int i=0;i<leaves.size();i++)
            if(leaves.get(i).toString().equals(temp.getChild(0).value()))
                if(leaves.get(i).nodeNumber(root)==nodeNumber)
                    if((i-1>=0 )&&(leaves.get(i-1)!=null))//If previos node exist
                        return leaves.get(i-1).parent(root);
        return null;
    }
    
    
    public static Tree getTheNextNodeToRight(Tree root, Tree marker, int nodeNumber) {
        List<Tree> leaves=root.getLeaves();
        for(int i=0;i<leaves.size();i++)
            if((leaves.get(i).equals(marker.getChild(0)))&&(leaves.get(i).nodeNumber(root)==nodeNumber+1))
                return leaves.get(i+1).parent(root);
        return null;
    }
    
    
    public static Tree getNodeFromNodeNumber(Tree root, int nodeNumber) {
        List<Tree> leaves=root.getLeaves();
        for(int i=0;i<leaves.size();i++)
            if(leaves.get(i).nodeNumber(root)==(nodeNumber+1))
                return leaves.get(i);
        return null;
    }
    
    
    public static Tree findNextNodeBasedOnNodeNumber(Tree temp, int nodeNumber, List<Tree> leaves, Tree root) {
        for(int i=0;i<leaves.size();i++)
            if((leaves.get(i).toString().equals(temp.getChild(0).value()))&&(leaves.get(i).nodeNumber(root)==nodeNumber+1))
                if(((i+1)<leaves.size())&&(leaves.get(i+1)!=null))
                    return leaves.get(i+1);
        return null;
    }
    
    
    public Tree getNodeFromStringValue(Tree root, Tree marker, int markerNodeNumber, String value) {
        List<Tree> leaves=root.getLeaves();
        for(int i=0;i<leaves.size();i++)
            if(leaves.get(i).nodeNumber(root)> markerNodeNumber)
                if(leaves.get(i).value().toString().equalsIgnoreCase(value))
                    return leaves.get(i);
        return null;
    }
    
}
