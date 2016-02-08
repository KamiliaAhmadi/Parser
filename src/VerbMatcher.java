


import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

public class VerbMatcher {
    public static String[] notVerbs={"easiest", "here,", "grass", "left", "right", "please", "tile,", "desk,", "forward,", "curves",
        "ahead", "building", "narrow", "stright", "masked", "olympic,", "descending", "ascending","hallways," ,"slopes",
        "centered", "seating", "audible", "paralleled",  "feet,", "seating",  "carpeted", "degrees", "apart,", "elevators,", "totaling",
        "route,",  "left,", "doorway", "edge",  "ramp,", "curb", "put", "alternating",  "wardenburg,", "intersecting", "carpet,",
        "ahead", "ahead,", "beyond", "beyond,", "block", "block,", "downcurb", "downcurb,", "degrees,", "degrees", "or", "or,", "and",
        "and,", "but", "but,", "stairs", "stairs,", "grill", "grill,", "Main,", "main", "up", "up,", "proctor,", "proctor,",
        "means,", "means", "foyer", "foyer,", "booth", "booth,", "flagstone", "first", "firs,", "you're", "landings,", "street,",
        "Kittredge,", "street", "Kittredge", "level", "level,","club", "club,", "again", "again,", "brick", "brick,","grass", "grass,",
        "mulch", "lions,", "lions", "east", "east,", "west", "west,", "south", "south,", "north", "north,","hallway", "hallway,",
        "inside","inside,", "gravel", "gravel,", "wall", "wall,", "Einsteins,", "away,", "avenue",
        "avenue,", "lobby", "lobby," ,"carpet","wide,", "wide", "finally", "finally", "stright", "stright,", "first,", "have", "has",
        "thorndike", "upland", "concord", "degrees", "lowell", "norfolk", "bookstore"};
    
    public static String[] undetectedVerbs={"exit", "walk", "reach", "trail", "turn" , "pass", "leads", "take", "face", "follow",
        "continue", "cross", "step", "head", "approach", "bring", "stay", "come", "wrap", "play", "change",
        "do", "go", "veer", "leave", "pass", "enter", "re-enter", "use", "proceed", "reversing","reverse", "keep",
        "came", "come", "stop", "wait", "find", "position", "stand", "search", "offset", "protrunding","protrund",
        "place", "placed","slope", "set", "set back","climb", "connect", "sloping", "form", "shift",
        "prevent", "using", "arrive", "crossed","open", "is", "are", "be", "notice", "begin",
        "end", "ends","cover", "feel", "detect", "make","be aware", "mark", "see", "sens","sense", "look", "meet",
        "press", "meets", "space", "start", "note","locate","separate", "separating", "press",
        "select", "hear", "find", "stick", "use", "encounter", "found","occur","happen", "runs", "exhaust", "met",
        "separated", "edged", "appear", "curve", "curving", "controls", "control",
        "encircle", "know", "identify","indicat", "intersect", "treat", "avoid", "enclose",
        "enclos", "narrows", "pick", "check","buzz", "handle","handl", "try", "imagine","imagin", "surrounded",
        "surround", "blocked","becomes", "become", "prefer", "have", "need", "separating", "prevent"
            ,"marked", "avoids", "serves", "covered", "blocking", "blocking,", "block", "narrow", "become", "becomes"};
    
    public static String [] verbLables={"VBP", "VB", "VBN", "VBG", "VBD", "VBZ"};
    
    public static String extractMatchingVerb(Tree marker, int nodeNumber, Tree rootNode, boolean right) {
        NodeFunctionalities nf=new NodeFunctionalities();
        String matchingAction="";
        List<Tree> leaves=rootNode.getLeaves();
        ArrayList<String> leavesString=nf.produceLeavesStrings(rootNode);
        Tree thisLeaf;
        int startOfSearch=-1;
        int endOfSearch=-1;
        int markerIndex=-1;
        for(int i=0; i<leaves.size();i++)
        {
            if((leaves.get(i).equals(marker.getChild(0)))&&(leaves.get(i).nodeNumber(rootNode)==nodeNumber+1))
            {
                markerIndex=i;
                break;
            }
        }
        if(right)
        {
            startOfSearch=markerIndex;
            endOfSearch=leaves.size();
        }
        else
        {
            startOfSearch=markerIndex;
            endOfSearch=-1;
        }
        
        int counter=startOfSearch;
        while(counter != endOfSearch)
        {
            thisLeaf=leaves.get(counter).parent(rootNode);
            if(thisLeaf.label().value().equals("VBP") || thisLeaf.label().value().equals("VB") ||
                    thisLeaf.label().value().equals("VBN") || thisLeaf.label().value().equals("VBG")||
                    thisLeaf.label().value().equals("VBD") || thisLeaf.label().value().equals("VBZ"))
            {
                matchingAction=leaves.get(counter).toString();
                if(checkFoundVerb(matchingAction))//check if found verb tagged as verb correctly
                    return checkForBiPartVerb(rootNode, matchingAction, leavesString, markerIndex, right, counter);
            }
            if(right)
                counter++;
            else
                counter--;
        }
        
        //if matchingAction is still emty
        matchingAction=undetectedVerbFinder(leavesString, markerIndex);
        matchingAction=findSecondPartOfVerb(rootNode, matchingAction,leavesString, markerIndex, right);
        matchingAction=checkForNotAllowedConnectorsAtEndOfAction(matchingAction);
        return matchingAction;
    }
    
    private static String checkForNotAllowedConnectorsAtEndOfAction(String action) {
        String[] splittedAction=action.split(" ");
        
        for(int i=0;i<Extraction.notAllaowedConnectorsAtEndOfLandMark.length;i++)
        {
            if(splittedAction[splittedAction.length-1].equalsIgnoreCase(Extraction.notAllaowedConnectorsAtEndOfLandMark[i]))
            {
                action.replace(splittedAction[splittedAction.length-1], "");
                return action;
            }
            
        }
        
        
        return action;
    }
    
    private static String checkForINGVerbsComingAfterBaseVerb( Tree node, int nodeNumber, Tree root) {
        String verb=node.value();
        List<Tree> leaves=root.getLeaves();
        for(int i=0; i<leaves.size();i++)
        {
            if(leaves.get(i).nodeNumber(root)==nodeNumber)
            {
                if((i+1<leaves.size())&&(leaves.get(i+1).parent(root).value().equalsIgnoreCase("VBG")))
                    verb=verb+" "+leaves.get(i+1).value();
            }
        }
        return verb;
    }
    
    private static String checkForBiPartVerb(Tree rootNode, String matchingAction, ArrayList<String> leavesString,
            int markerIndex, boolean right, int counter) {
        
        List<Tree> leaves=rootNode.getLeaves();
        if((matchingAction.equalsIgnoreCase("be"))|| (matchingAction.equalsIgnoreCase("turn"))||
                (matchingAction.equalsIgnoreCase("stay"))||(matchingAction.equalsIgnoreCase("keep")))
        {
            matchingAction=findSecondPartOfVerb(rootNode, matchingAction,leavesString, markerIndex, right);//If detected action has two parts
            return checkForNotAllowedConnectorsAtEndOfAction(matchingAction);
        }
        
        String verbAndAdverb=checkForAdverbsComingAfterVerb(leaves.get(counter), leaves.get(counter).nodeNumber(rootNode),rootNode);
        if(!verbAndAdverb.equalsIgnoreCase(matchingAction))
            return checkForNotAllowedConnectorsAtEndOfAction(verbAndAdverb);
        String INGVerb=checkForINGVerbsComingAfterBaseVerb( leaves.get(counter), leaves.get(counter).nodeNumber(rootNode),rootNode);
        if( !INGVerb.equalsIgnoreCase(matchingAction))
            return checkForNotAllowedConnectorsAtEndOfAction(INGVerb);
        return matchingAction;
    }
    
    //This function detects words has been taged as verbs but actually they are not verbs
    public static boolean checkFoundVerb(String matchingAction) {
        if((matchingAction.equalsIgnoreCase(""))||(matchingAction.equalsIgnoreCase(" "))||
                (matchingAction.equalsIgnoreCase(",")))
        {
            return false;
        }
        boolean isVerb=true;
        for(int i=0;i<notVerbs.length;i++)
        {
            if(matchingAction.equalsIgnoreCase(notVerbs[i]))
                return false;
        }
        
        return isVerb;
    }
    
    public static String undetectedVerbFinder(ArrayList<String> leafs, int indexOfPrepositionInLeaves) {
        String undetectedVerb=Extraction.notFoundString;
        for(int i=0;i<indexOfPrepositionInLeaves; i++)
        {
            for(int j=0;j<undetectedVerbs.length;j++)
            {
                if(leafs.get(i).equalsIgnoreCase(undetectedVerbs[j]))
                    return leafs.get(i);
            }
        }
        return undetectedVerb;
    }
    
    
    //Some verbs like "Be aware", "Turn Right",...need to come with their according adverb.
    //This function matches these verbs with their adverbs
    private static String findSecondPartOfVerb( Tree root, String matchingAction, ArrayList<String> leavesString, int markerIndex, boolean directionRight) {
        
        List<Tree> leaves=root.getLeaves();
        int startSearch=-1;
        int endSearch=-1;
        if(directionRight)
        {
            startSearch=markerIndex;
            endSearch=leavesString.size();
        }
        else
        {
            startSearch=markerIndex;
            endSearch=-1;
        }
        int counter=startSearch;
        
        
        if((matchingAction.equalsIgnoreCase("be"))|| (matchingAction.equalsIgnoreCase("keep")))
        {
            while (counter!=endSearch)
            {
                if(matchingAction.equalsIgnoreCase(leavesString.get(counter)))
                {
                    if(counter+1>=leavesString.size())
                        break;
                    if((leaves.get(counter+1).parent(root).label().value().equalsIgnoreCase("JJR"))
                            ||(leaves.get(counter+1).parent(root).label().value().equalsIgnoreCase("JJ"))
                            ||(leaves.get(counter+1).parent(root).label().value().equalsIgnoreCase("RB"))
                            ||(leaves.get(counter+1).parent(root).label().value().equalsIgnoreCase("RBR"))
                            ||(leaves.get(counter+1).parent(root).label().value().equalsIgnoreCase("RBS")))
                    {
                        
                        //check if the next part of the verb contains numeric or not- which is not acceptable if it has numeric in it
                        String s=leavesString.get(counter+1);
                        s=s.replaceAll("[*a-zA-Z]", ""); //replaces all alphabets
                        if(!StringUtils.isNumeric(s))
                            matchingAction=matchingAction+ " " + leavesString.get(counter+1);
                        //forTest.add(matchingAction);
                        break;
                    }
                }
                if(directionRight)
                    counter++;
                else
                    counter--;
            }
            return matchingAction;
        }
        
        if((matchingAction.equalsIgnoreCase("turn"))|| (matchingAction.equalsIgnoreCase("stay")))
        {
            while (counter!=endSearch)
            {
                if(matchingAction.equalsIgnoreCase(leavesString.get(counter)))
                {
                    if(counter+1>=leavesString.size())
                        break;
                    
                    if(ActionCharacteristics.searchforMarker(ActionCharacteristics.directions, leavesString.get(counter+1).toString())!=null)
                    {
                        matchingAction=matchingAction+ " " + leavesString.get(counter+1);
                        break;
                    }
                }
                if(directionRight)
                    counter++;
                else
                    counter--;
            }
            return matchingAction;
        }
        return matchingAction;
    }
    
    private static String checkForAdverbsComingAfterVerb(Tree verbNode, int nodeNumber, Tree rootNode) {
        
        List<Tree> leaves=rootNode.getLeaves();
        for(int i=0;i<leaves.size();i++)
        {
            if((leaves.get(i).value().toString().equalsIgnoreCase(verbNode.value().toString()))
                    &&(leaves.get(i).nodeNumber(rootNode)==nodeNumber))
            {
                if(i+1<leaves.size())
                {
                    Tree tempNode=leaves.get(i+1).parent(rootNode);
                    if((tempNode.label().value().equalsIgnoreCase("RB"))||(tempNode.label().value().equalsIgnoreCase("RBR"))
                            ||(tempNode.label().value().equalsIgnoreCase("RBS"))||(tempNode.label().value().equalsIgnoreCase("ADVP")))
                    {
                        return leaves.get(i).value().toString()+" "+leaves.get(i+1).value().toString();
                    }
                    else
                        return leaves.get(i).value().toString();
                }
                break;
            }
        }
        return verbNode.value().toString();
    }
       
    public String checkIfVerbHasTwoParts(Tree temp, int nodeNumber, Tree parse, ArrayList<String> leavesStrings, int index, boolean directRight) {
        String extractedAction=findSecondPartOfVerb(parse, temp.getChild(0).toString(),leavesStrings, index, directRight );
        if(!extractedAction.equalsIgnoreCase(temp.getChild(0).toString()))
            return extractedAction;
        
        String verbAndAdverb=checkForAdverbsComingAfterVerb(temp.getChild(0), temp.getChild(0).nodeNumber(parse),parse);
        if(!verbAndAdverb.equalsIgnoreCase(temp.getChild(0).toString()))
            return verbAndAdverb;
        
        
        String baseVerbING=checkForINGVerbsComingAfterBaseVerb( temp.getChild(0), temp.getChild(0).nodeNumber(parse),parse);
        if(!baseVerbING.equalsIgnoreCase(temp.getChild(0).toString()))
            return baseVerbING;
        
        
        String baseVerbPreposition=Extraction.checkForPrepositionComingAfterBaseVerb( temp.getChild(0), temp.getChild(0).nodeNumber(parse),parse);
        if(!baseVerbPreposition.equalsIgnoreCase(temp.getChild(0).toString()))
            return baseVerbPreposition;
        
        
        return temp.getChild(0).value();
    }
    
    //For finding out if the node is verb we need to check its differences with nouns. Nounus usaullay come after article or number.
    public boolean checkIfThisNodeIsVerb(Tree temp, Tree root) {
        List<Tree> leaves=root.getLeaves();
        Tree targetNode;
        if(!temp.getChild(0).isEmpty())
            targetNode=temp.getChild(0);
        else
            targetNode=temp;
        
        for(int i=0; i<leaves.size();i++)
        {
            
            if(leaves.get(i).nodeNumber(root)==targetNode.nodeNumber(root))
            {
                if(i-1>=0)
                    if(leaves.get(i-1).parent(root).value().equalsIgnoreCase("DT")
                            || leaves.get(i-1).parent(root).value().equalsIgnoreCase("JJ")
                            || leaves.get(i-1).parent(root).value().equalsIgnoreCase("CD"))
                        return false;
            }
        }
        return true;
    } 
    
    
    public boolean ifThisNodeIsUndetectedVerb(Tree temp, Tree root) {
        VerbMatcher vm=new VerbMatcher();
        
        if(temp.getChildrenAsList().size()==0)
            return false;
        
        String [] tempValue=temp.getChild(0).value().split(" ");
        for(int i=0; i<vm.undetectedVerbs.length; i++)
        {
            
            
            if(tempValue[0].equalsIgnoreCase(vm.undetectedVerbs[i]))
                if(vm.checkIfThisNodeIsVerb(temp, root))//Check if this node is exactly a verb
                    return true;
        }
        return false;
    }
    
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean hasVerbTag(String tag)
    {
        for(int i=0; i<verbLables.length; i++)
            if (tag.equalsIgnoreCase(verbLables[i]))
                    return true;
        return false;
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    //verbs which has been modified to adjectives like "covered area"
    public static boolean isAdjectiveVerb(Tree node, Tree root, int nodeNumber) {
        List<Tree> leaves=root.getLeaves();
        if(node.parent(root).label().value().equalsIgnoreCase("VBG") || node.parent(root).label().value().equalsIgnoreCase("VBN"))
        {
            int index=leaves.indexOf(node);
            if(index+1<leaves.size())
                if(leaves.get(index+1).parent(root).label().value().equalsIgnoreCase("NP")||
                        leaves.get(index+1).parent(root).label().value().equalsIgnoreCase("NNP") ||
                        leaves.get(index+1).parent(root).label().value().equalsIgnoreCase("NN"))
                    return false;
        }
        return true;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
