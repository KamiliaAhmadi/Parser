/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/


import java.util.List;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static java.util.Collections.list;

public class Extraction {
    static String [] mistakenPrepositions= {"once", "as", "until" , "front", "If", "that"};
    static String [] subjects={"I", "You", "He", "She", "It", "We", "They"};
    static String [] determiners={"that", "which", "who", "whom", "Until", "As", "just", "while", "then"};
    static String [] articles={"the", "a", "an", "some", "this", "these", "another"};
    static String [] multiplePartPrepositions={"in front of", "next to", "near to", "close to", "as well as", "right to",
        "as soon as", "ahead of", "left to", "outside of", "out of"};
    public static String [] notAllaowedConnectorsAtEndOfLandMark={"and", "or", "but", "that", "which", "who", "whom", "Until", "As", "just",
        "while", "the", "a", "an", "some", "this", "these"};
    public static ArrayList<String> forTest=new ArrayList<String>();
    public static String noSiblingString="No Siblings/No LandMark";
    public static String phraseStartingWithConnectorString="Phrase starting with connector/No LandMark";
    public static String notFoundString="notFound";
    public static String[] measurementMarkers={"feet", "feet,", "inches", "inches,", "block", "block,","blocks", "blocks,", "degree","degree,","degrees","degrees,"};
    public static int sizeMultiplePartPreposition=0;
    public static int startIndexOfPreposition=0;
    LexicalizedParser lp;
    
    
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //The aim of this function is to extract as much as landMark and actions as we can based on preposition. we look for prepositions
    //then we try to extract landmarks and actions.
    public ArrayList<String> prepositionBasedExtraction(Tree parse, String text) {
        
        ArrayList<String> prepositions=new ArrayList<>();
        ArrayList<Tree> nodes=new ArrayList<>();
        Tree temp=parse;
        nodes.add(temp);
        
        if (parse == null || parse.isLeaf()) {
            return null;
        }
        
        boolean moveDirectionRight;
        int nodeNumber=temp.nodeNumber(parse);
        boolean passNextPreposition=false;
        
        //whole tree is travesed via DFS, searching for prepositions
        while(!nodes.isEmpty())
        {
            String LandMark="";
            LandMarkAction lma=new LandMarkAction();
            temp=nodes.get(nodes.size()-1);
            if ((temp.label().value().equals("TO") || temp.label().value().equals("IN"))){
                nodeNumber=temp.nodeNumber(parse);//node number is used as identification of nodes
                
                if(passNextPreposition) //If the detected preposition is a part of previous LandMark- It doesn't need to be detected again
                {
                    enqueueChildren(temp, nodes);
                    passNextPreposition=false;
                    continue;
                }
                
                if (!checkForPrepositions(temp.getChild(0).toString()))//If this preposition is mistakenly tagged as preposition- remove it.
                {
                    enqueueChildren(temp, nodes);
                    continue;
                }
                
                //Extract LandMark
                LandMark=findLandMark(temp, parse);
                
                //Refine Extracted LandMark
                LandMark=landMarkRefiener(LandMark, temp, parse);// This function refines the extracted landmark from extra words which has been attached to the landMark
                
                //trim the refined landMark
                LandMark=LandMark.trim();
                
                if(LandMark.equalsIgnoreCase("")||LandMark==null)//If no LandMark is detected
                {
                    enqueueChildren(temp, nodes);
                    continue;
                }
                
                lma.preposition=temp.getChild(0).toString();
                prepositions.add(lma.preposition);
                //Check for existance of another preposition in this landMark
                passNextPreposition=checkForPassingNextPreposition(LandMark);//This function checks the existance of a preposition and a landMark inside of our detected ladnMark
                
                //Extracting proper Action verb which matches the found preposition- Direction identifies where the possible action located with respect to the marker
                //Since here the marker is preposition, then the action is in the left of the marker.
                moveDirectionRight=false;
                
                VerbMatcher vm=new VerbMatcher();
                lma.Action=vm.extractMatchingVerb(temp, temp.nodeNumber(parse), parse, moveDirectionRight);
                excludeFromSentence(lma.Action, parse, temp.nodeNumber(parse), moveDirectionRight);
                
                //Check if sentence is passive.
                String passiveNode=sentenceIsPassive(temp, temp.nodeNumber(parse), parse);
                if((passiveNode!=null)&&(lma.Action!= null ) &&(lma.Action.equalsIgnoreCase(notFoundString)))
                {
                    lma.Action=passiveNode;
                    lma.passive="passive";
                    lma.preposition=null;
                    excludeFromSentence(lma.Action, parse, temp.nodeNumber(parse), moveDirectionRight);
                }
                
                //Appending preposition to LandMark
                if((LandMark.equalsIgnoreCase("No Siblings/No LandMark"))||(LandMark.equalsIgnoreCase("Existing Connector/No LandMark")))
                    lma.LandMark=LandMark;
                else if (passiveNode!=null)
                {
                    lma.LandMark=LandMark;
                    excludeFromSentence(lma.LandMark, parse, temp.nodeNumber(parse), false);
                }
                else
                {
                    LandMark=lma.preposition+" "+LandMark;
                    lma.LandMark=LandMark;
                    moveDirectionRight=true;//exclude the findings from whole sentence to avoid iterative landMark detection
                    excludeFromSentence(lma.LandMark, parse, temp.nodeNumber(parse), true);
                }
                
                //Based on LandMark we infer some actions- Measurement shows Physical transfer. Sentence with preposition and a LandMark shows an MTRANS
                if(CheckForMeasurementInExtractedLandMark(LandMark))
                {
                    //lma.Action="PTrans/Move"+ " "+ lma.Action;
                    lma.Action="PTrans/Move";
                }
                
                if((lma.Action!=null)&&(lma.Action.equalsIgnoreCase(notFoundString)))
                {
                    boolean isUpperCase=Character.isUpperCase(lma.preposition.charAt(0));
                    if(isUpperCase)
                        lma.Action="MTrans/Sense/See";
                }
                lma.rowIndex=ParserDemo.currentRowIndex;
                lma.sentenceIndex=ParserDemo.currentSentenceIndex;
                lma.routeIndex=ParserDemo.currentRouteIndex;
                lma.startingNodeNumber=nodeNumber;
                ActionCharacteristics ac=new ActionCharacteristics();
                lma.actionCharacteristics=ac.extractActionCharacteristics(parse, temp, temp.nodeNumber(parse),lma);
                MTransPTransClassifier MPClassifier=new MTransPTransClassifier();
                String MTransPTransClassifeid=MPClassifier.checkforMTransPTrans(lma.Action);
                lma.Action=MTransPTransClassifeid;
                
                insertSortedLandmarkAction(lma, parse);
            }
            enqueueChildren(temp, nodes);
        }
        return prepositions;
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String landMarkRefiener(String toBeRefined, Tree marker, Tree root) {
        NodeFunctionalities nf=new NodeFunctionalities();
        String refined="";
        toBeRefined=toBeRefined.trim();
        if((toBeRefined.equalsIgnoreCase(noSiblingString))
                ||(toBeRefined.equalsIgnoreCase(phraseStartingWithConnectorString)))
            return toBeRefined;
        
        String [] toBeRefinedSplitted=toBeRefined.split(" ");
        for(int i=0; i<toBeRefinedSplitted.length;i++)
        {
            if(hasComma(toBeRefinedSplitted[i]))
            {
                refined=refined+" "+toBeRefinedSplitted[i];
                break;
            }
            else if (isInSubjectGroup(toBeRefinedSplitted[i]))
                break;
            else if (isInDeterminerGroup(toBeRefinedSplitted[i]))
                break;
            else
                refined=refined+" "+toBeRefinedSplitted[i];
        }
        
        //if landMark is a numeric, check to see if there is a measurement marker associated with this number
        if(StringUtils.isNumeric(toBeRefined))
        {
            String measurementMarker= checkForMeasurementMarker(marker, root, toBeRefined);
            if(measurementMarker!=null)
                refined=toBeRefined+ " "+ measurementMarker;
        }
        refined=refined.trim();
        
        
        String lastRefined="";
        String [] splittedRefined=refined.split(" ");
        boolean notAllaowedConnectorsFlag=false;
        for(int i=0;i<notAllaowedConnectorsAtEndOfLandMark.length;i++)
        {
            if(splittedRefined[splittedRefined.length-1].equalsIgnoreCase(notAllaowedConnectorsAtEndOfLandMark[i]))
                notAllaowedConnectorsFlag=true;
        }
        if( notAllaowedConnectorsFlag)
        {
            for(int j=0;j<splittedRefined.length-1;j++)
                lastRefined=lastRefined+" "+splittedRefined[j];
        }
        else
            lastRefined=refined;
        
        
        int markerNodeNumber=marker.nodeNumber(root);
        String [] refinedSplitted=lastRefined.split(" ");
        if(refinedSplitted[refinedSplitted.length-1].equalsIgnoreCase("number"))
        {
            String value="number";
            Tree lastNodeInDetectedLandMark=nf.getNodeFromStringValue(root, marker, markerNodeNumber , value);
            List<Tree> leaves=root.getLeaves();
            int index=leaves.indexOf(lastNodeInDetectedLandMark);
            if((index+1<leaves.size())&&(leaves.get(index+1).parent(root).value().equalsIgnoreCase("CD")))
                lastRefined=lastRefined+" "+leaves.get(index+1).value().toString();
            
            if((index+2<leaves.size())&&(leaves.get(index+2).value().equalsIgnoreCase("and")||
                    leaves.get(index+2).value().equalsIgnoreCase("or")))
            {
                lastRefined=lastRefined+" "+leaves.get(index+2).value()+ " "+getNPAfterConnector(root, leaves.get(index+2), lastRefined);
                lastRefined=landMarkRefiener(lastRefined, leaves.get(index+2), root);
            }
        }
        //remove verb at the end of landMark
        lastRefined=removeVerbAtEndOfLandMark(root, lastRefined, marker);
        
        
        lastRefined=longLandMarksRefinement(root, lastRefined, marker);
        return lastRefined;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static boolean hasComma(String toBeChecked) {
        
        boolean hasComma=false;
        String [] splittedToBeChecked=toBeChecked.split("");
        
        for(int i=0; i<splittedToBeChecked.length;i++ )
        {
            if(splittedToBeChecked[i].equalsIgnoreCase(","))
                return true;
        }
        return hasComma;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static boolean checkForPassingNextPreposition(String forCheck) {
        boolean passingNextPreposition=false;
        forCheck=forCheck.trim();
        String [] forCheckSplitted=forCheck.split(" ");
        List<CoreLabel> forCheckSplittedList = Sentence.toCoreLabelList(forCheckSplitted);
        //LexicalizedParser lp=LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        Tree forCheckTree = ParserDemo.lp.apply(forCheckSplittedList);
        // forCheckTree.pennPrint();
        
        ArrayList<Tree> tempNodes=new ArrayList<>();
        tempNodes.add(forCheckTree);
        Tree temp;
        
        while(!tempNodes.isEmpty())
        {
            
            temp=tempNodes.get(0);
            if(temp.label().value().equals("TO") || temp.label().value().equals("IN"))
            {
                if(checkForPrepositions(temp.getChild(0).toString()))
                {
                    passingNextPreposition=true;
                }
                break;
            }
            else
            {
                tempNodes.addAll(temp.getChildrenAsList());
                tempNodes.remove(temp);
            }
            
        }
        return passingNextPreposition;
    }
    
    private static boolean isInSubjectGroup(String toBeChecked)
    {
        boolean isSubject=false;
        
        for(int i=0; i<subjects.length;i++)
        {
            if(toBeChecked.equalsIgnoreCase(subjects[i]))
                return true;
        }
        return isSubject;
    }
    
    private static boolean isInDeterminerGroup(String toBeChecked) {
        boolean isDeterminer=false;
        
        for(int i=0; i<determiners.length;i++)
        {
            if(toBeChecked.equalsIgnoreCase(determiners[i]))
                return true;
        }
        return isDeterminer;
    }
    
    public ArrayList<String> exBasedExtraction(Tree parse, String text) {
        
        ArrayList<String> exQuantifiers=new ArrayList<>();
        
        if (parse == null || parse.isLeaf()) {
            return null;
        }
        
        ArrayList <Tree> nodes=new ArrayList<>();
        nodes.add(parse);
        
        while(!nodes.isEmpty())
        {
            String LandMark="";
            LandMarkAction lma=new LandMarkAction();
            Tree temp=nodes.get(nodes.size()-1);
            if (temp.label().value().equals("EX")){
                int nodeNumber=temp.nodeNumber(parse);
                
                Tree tempParent=temp.parent(parse);
                //int numParent=tempParent.nodeNumber(parse);
                List<Tree> parentSiblings=tempParent.siblings(parse);
                Tree temp1=parentSiblings.get(parentSiblings.size()-1);
                Tree temp2=temp1.getChild(0);
                
                Tree targetNode=CheckIfNodeHasVBZVBPChild(temp2);
                
                LandMark=findLandMark(targetNode, parse);
                LandMark=landMarkRefiener(LandMark, temp, parse);// This function refines the extracted landmark
                LandMark=LandMark.trim();
                
                if((LandMark.equalsIgnoreCase(""))||(LandMark.equalsIgnoreCase(null)))
                {
                    enqueueChildren(temp, nodes);
                    continue;
                }
                
                exQuantifiers.add(temp.getChild(0).label().value());
                
                boolean directionRight=true;
                int startIndex=findStartingIndex(LandMark, temp, parse, directionRight);
                if(CheckIfDetectedBefore(LandMark, startIndex))//If LandMark detected already, we just exclude it
                {
                    enqueueChildren(temp, nodes);
                    continue;
                }
                lma.LandMark=LandMark;
                lma.exQuantifier=temp.label().value();
                lma.rowIndex=ParserDemo.currentRowIndex;
                excludeFromSentence(LandMark, parse, temp.nodeNumber(parse), directionRight);
                if(CheckForMeasurementInExtractedLandMark(LandMark))
                {
                    lma.Action="PTrans/Move";
                }
                else{
                    //When we extract LandMark based on Exestential Quantifiers, there is not associated PTrans in them and their Actions is MTrans.
                    lma.Action="MTrans/Sense/See";
                }
                lma.sentenceIndex=ParserDemo.currentSentenceIndex;
                lma.routeIndex=ParserDemo.currentRouteIndex;
                lma.startingNodeNumber=nodeNumber;
                ActionCharacteristics ac=new ActionCharacteristics();
                lma.actionCharacteristics=ac.extractActionCharacteristics(parse, temp, temp.nodeNumber(parse),lma);
                insertSortedLandmarkAction(lma, parse);
            }
            
            enqueueChildren(temp, nodes);
        }//End While
        return exQuantifiers;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    //IN identifying the landmarks coming after existantial quantifires, we need to detect the "VBZ" or "VBP" (is and are) after
    //Existantial quantifers. Then we need to detect the NP coimg after that. Sometimes these VBZ or VBP don't come exactly after the
    //EX and we need to check for it or traverse the tree deeper to find it.
    private static Tree CheckIfNodeHasVBZVBPChild(Tree temp2) {
        Tree targetNode=temp2;
        if((temp2.label().value().equals("VBP") || temp2.label().value().equals("VB") || temp2.label().value().equals("VBN")
                || temp2.label().value().equals("VBG") || temp2.label().value().equals("VBD") || temp2.label().value().equals("VBZ") ))
        {
            return temp2;
        }
        while(!(temp2.label().value().equals("VBP") || temp2.label().value().equals("VB") || temp2.label().value().equals("VBN")
                || temp2.label().value().equals("VBG") || temp2.label().value().equals("VBD") || temp2.label().value().equals("VBZ") ))
        {
            if(temp2.getChildrenAsList().size()>0)
            {
                targetNode=temp2.getChild(0);
                temp2=targetNode;
                targetNode=null;
            }
            else
                break;
        }
        
        return temp2;
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<String> articleBasedExtraction(Tree parse, String text) {
        ArrayList<String> articles=new ArrayList<String>();
        if (parse == null || parse.isLeaf()) {
            return null;
        }
        List<Tree> leaves=parse.getLeaves();
        ArrayList <Tree> nodes=new ArrayList<>();
        nodes.add(parse);
        boolean directionRight;
        
        while(!nodes.isEmpty())
        {
            String LandMark="";
            LandMarkAction lma=new LandMarkAction();
            Tree temp=nodes.get(nodes.size()-1);
            if ((temp.label().value().equals("DT"))&&(isArticle(temp))){
                int nodeNumber=temp.nodeNumber(parse);
                LandMark=findLandMark(temp, parse);
                LandMark=landMarkRefiener(LandMark,temp, parse);// This function refines the extracted landmark
                LandMark=LandMark.trim();
                
                if((LandMark.equalsIgnoreCase(""))||(LandMark.equalsIgnoreCase(null)))
                {
                    enqueueChildren(temp, nodes);
                    continue;
                }
                
                if(!(LandMark.equalsIgnoreCase(noSiblingString))||(LandMark.equalsIgnoreCase(phraseStartingWithConnectorString)))
                {
                    LandMark=temp.getChild(0).toString()+" "+LandMark;
                }
                
                directionRight=true;//LandMark is in the right side of the marker, so direction would be to the right
                
                int startIndex=findStartingIndex(LandMark, temp , parse, directionRight);
                if(CheckIfDetectedBefore(LandMark, startIndex)){
                    enqueueChildren(temp, nodes);
                    continue;
                }
                
                lma.LandMark=LandMark;
                lma.rowIndex=ParserDemo.currentRowIndex;
                lma.sentenceIndex=ParserDemo.currentSentenceIndex;
                lma.routeIndex=ParserDemo.currentRouteIndex;
                lma.startingNodeNumber=nodeNumber;
                lma.article=temp.getChild(0).label().value();
                excludeFromSentence(LandMark, parse, temp.nodeNumber(parse),directionRight);
                
                String ExtractedAction="";
                String passiveNode=sentenceIsPassive(temp, temp.nodeNumber(parse), parse);
                if(passiveNode!=null)
                {
                    lma.Action=passiveNode;
                    lma.passive="passive";
                    lma.article=null;
                    
                }
                else
                {
                    directionRight=false;
                    VerbMatcher vm=new VerbMatcher();
                    ExtractedAction=vm.extractMatchingVerb(temp, temp.nodeNumber(parse),parse, directionRight);
                    lma.Action=ExtractedAction;
                }
                
                excludeFromSentence(ExtractedAction, parse, temp.nodeNumber(parse), directionRight);
                
                if(CheckForMeasurementInExtractedLandMark(LandMark))
                {
                    //lma.Action="PTrans/Move"+" "+lma.Action;
                    lma.Action="PTrans/Move";
                }
                if(lma.Action!=null && lma.Action.equalsIgnoreCase(notFoundString))
                {
                    boolean isUpperCase=Character.isUpperCase(lma.article.charAt(0));
                    if(isUpperCase)
                        lma.Action="MTrans/Sense/See";
                }
                excludeFromSentence(lma.Action, parse, temp.nodeNumber(parse), directionRight);
                //lma.distance=findDistanceBetweenLandMarkAction(lma, temp, temp.nodeNumber(parse), parse);
                ActionCharacteristics ac=new ActionCharacteristics();
                lma.actionCharacteristics=ac.extractActionCharacteristics(parse, temp, temp.nodeNumber(parse),lma);
                MTransPTransClassifier MPClassifier=new MTransPTransClassifier();
                String MTransPTransClassifeid=MPClassifier.checkforMTransPTrans(lma.Action);
                lma.Action=MTransPTransClassifeid;
                insertSortedLandmarkAction(lma, parse);
            }
            enqueueChildren(temp, nodes);
        }//End While
        return articles;
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static boolean isArticle(Tree node) {
        boolean isArticle=false;
        
        for(int i=0;i<articles.length;i++)
        {
            if(node.getChild(0).value().toString().equalsIgnoreCase(articles[i]))
                return true;
            
        }
        return isArticle;
        
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private static boolean CheckForMeasurementInExtractedLandMark(String LandMark) {
        
        boolean includeMeasurement=false;
        String [] splittedLandMark=LandMark.split(" ");
        
        for(int i=0;i<splittedLandMark.length;i++)
        {
            for(int j=0;j<measurementMarkers.length;j++)
            {
                if(splittedLandMark[i].equalsIgnoreCase(measurementMarkers[j]))
                    return true;
            }
        }
        return includeMeasurement;
    }
    
    private String findLandMark(Tree currentNode, Tree parse) {
        NodeFunctionalities nf=new NodeFunctionalities();
        String lm="";
        List<Tree> nodeSiblings = currentNode.siblings(parse);
        
        if(nodeSiblings==null || nodeSiblings.isEmpty())
        {
            return getNounPhraseComesAfterCurrentNode(currentNode, parse);
        }
        Tree rightSibling= nf.getNextRightSibling(currentNode, parse, currentNode.nodeNumber(parse));
        if( rightSibling!=null && rightSibling.label().value().equalsIgnoreCase("CC"))
            return phraseStartingWithConnectorString;
        
        for (Tree sib : nodeSiblings)
        {
            // if(sib.label().value().equalsIgnoreCase(lm))
            if(sib.nodeNumber(parse)>currentNode.nodeNumber(parse))//we need to find right siblings of the node.
            {
                List<Tree> leaves=sib.getLeaves();
                for (Tree leaf : leaves)
                {
                    lm=lm+" "+ leaf.label().value().toString();
                }
            }
        }
        
        
        return lm.trim();
    }
    
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<String> gerundBasedExtraction(Tree parse, String text) {
        
        NodeFunctionalities nf=new NodeFunctionalities();
        VerbMatcher vm=new VerbMatcher();
        ArrayList<String> gerunds=new ArrayList<>();
        
        if (parse == null || parse.isLeaf())
            return null;
        
        
        ArrayList <Tree> nodes=new ArrayList<>();
        nodes.add(parse);
        
        while(!nodes.isEmpty())
        {
            String LandMark="";
            LandMarkAction lma=new LandMarkAction();
            Tree temp=nodes.get(nodes.size()-1);
            
            if ((temp.label().value().equals("VB"))||(temp.label().value().equals("VBG"))||
                    (temp.label().value().equals("VBP"))||(temp.label().value().equals("VBN")) ||
                    (temp.label().value().equals("VBD"))|| (vm.ifThisNodeIsUndetectedVerb(temp, parse ))){
                Tree forCheck=null;
                
                if(!vm.checkFoundVerb(temp.getChild(0).toString()))//Check if the verb detected correctly.
                {
                    enqueueChildren(temp, nodes);
                    continue;
                }
                
                //check for passive sentence
                if(temp.getChild(0).value().equalsIgnoreCase("be"))
                {
                    forCheck=nf.getTheNextNodeToRight(parse, temp, temp.nodeNumber(parse));
                    if ((forCheck.label().value().equals("VBN"))&&(vm.checkFoundVerb(forCheck.getChild(0).value())))//means that sentence is passive and not gerund
                    {
                        enqueueChildren(temp, nodes);
                        continue;
                    }
                }
                boolean directRight=true;
                int index=parse.getLeaves().indexOf(temp.getChild(0));
                ArrayList<String> leavesStrings=nf.produceLeavesStrings(parse);
                
                
                //Check if verb has two parts
                
                String extractedAction=vm.checkIfVerbHasTwoParts(temp, temp.nodeNumber(parse),parse,leavesStrings,index,directRight);
                
                Tree temp1;
                if(!extractedAction.equalsIgnoreCase(temp.getChild(0).toString()))//which means that adverb has been added to verb
                {
                    temp1=nf.getTheNextNodeToRight(parse, temp, temp.nodeNumber(parse));
                }
                else
                {
                    temp1=temp;
                }
                //check if extracted verb detected before
                gerunds.add(extractedAction);
                
                LandMark=findLandMark(temp1, parse);
                LandMark=landMarkRefiener(LandMark, temp1, parse);// This function refines the extracted landmark
                LandMark=LandMark.trim();
                
                int startIndex=findStartingIndex(LandMark, temp , parse, directRight);
                if(CheckIfDetectedBefore(LandMark, startIndex))//If LandMark detected already, we just exclude it
                {
                    enqueueChildren(temp, nodes);
                    continue;
                }
                
                lma.LandMark=LandMark;
                
                lma.Action=extractedAction;
                lma.gerund="gerund";
                lma.rowIndex=ParserDemo.currentRowIndex;
                lma.sentenceIndex=ParserDemo.currentSentenceIndex;
                lma.routeIndex=ParserDemo.currentRouteIndex;
                int landmarkIndexInSentence=text.indexOf(LandMark);
                lma.startingNodeNumber=landmarkIndexInSentence;
                if(landmarkIndexInSentence<0)
                    landmarkIndexInSentence=-1;
                excludeFromSentence(LandMark, parse, temp.nodeNumber(parse), directRight);
                excludeFromSentence(extractedAction, parse, temp.nodeNumber(parse), directRight);//how to exclude from sentence when the marker is the action
                if(CheckForMeasurementInExtractedLandMark(LandMark))
                {
                    //lma.Action="PTrans/Move"+"+"+lma.Action;
                    lma.Action="PTrans/Move";
                }
                //lma.distance=findDistanceBetweenLandMarkAction(lma, temp, temp.nodeNumber(parse), parse);
                ActionCharacteristics ac=new ActionCharacteristics();
                lma.actionCharacteristics=ac.extractActionCharacteristics(parse, temp, temp.nodeNumber(parse),lma);
                MTransPTransClassifier MPClassifier=new MTransPTransClassifier();
                String MTransPTransClassifeid=MPClassifier.checkforMTransPTrans(lma.Action);
                lma.Action=MTransPTransClassifeid;
                insertSortedLandmarkAction(lma, parse);
            }
            
            enqueueChildren(temp, nodes);
        }//End While
        return gerunds;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static void excludeFromSentence(String toBeExcluded, Tree root, int nodeNumber, boolean right) {
        //toBeExcluded=toBeExcluded.trim();
        NodeFunctionalities nf=new NodeFunctionalities();
        if(toBeExcluded == null)
            return;
        if (!toBeExcluded.equalsIgnoreCase(notFoundString))//Sometime the ations can't be found! Then we put notFoundString in their places. Since notFoundString is not part of the sentence, it doesn't need to be excluded.
        {
            toBeExcluded=toBeExcluded.trim();
            String [] toBeExcludedSplitted=toBeExcluded.split(" ");
            List<Tree> leaves=root.getLeaves();
            Tree node=nf.getNodeFromNodeNumber(root, nodeNumber);
            int start, end;
            
            if(right)
            {
                start=leaves.indexOf(node);
                end=leaves.size();
            }
            else
            {
                start=leaves.indexOf(node);
                end=-1;
            }
            int counterleaves=start;
            int counterSplitted=0;
            while(counterleaves!=end)
            {
                if(leaves.get(counterleaves).value().equalsIgnoreCase(toBeExcludedSplitted[counterSplitted]))
                {
                    if(markedWholeSeneteceAsDetected(counterleaves, toBeExcludedSplitted))
                        break;
                }
                if(right)
                    counterleaves++;
                else
                    counterleaves--;
            }
        }
    }
    
    //If whole thing hasn't been detected before return false, otherwise return true.
    private boolean CheckIfDetectedBefore(String toBeChecked, int startIndex) {
        
        
        if(
                (toBeChecked.equalsIgnoreCase(""))
                ||(toBeChecked.equalsIgnoreCase(notFoundString))
                ||(toBeChecked.equalsIgnoreCase(" "))
                ||(toBeChecked.equalsIgnoreCase(null))
                ||(toBeChecked.equalsIgnoreCase(noSiblingString))
                ||(toBeChecked.equalsIgnoreCase(phraseStartingWithConnectorString))
                ||(startIndex==-1))
            
            return false;
        
        toBeChecked=toBeChecked.trim();
        String [] toBeCheckedSplitted=toBeChecked.split(" ");
        
        int endIndex=startIndex+toBeCheckedSplitted.length;
        for(int i=startIndex;i<endIndex;i++)
        {
            if(ParserDemo.sent[i].equalsIgnoreCase(toBeCheckedSplitted[i-startIndex]))
                return false;
            
        }
        return true;
    }
    
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static int findStartingIndex(String toBeChecked, Tree marker, Tree root, boolean right) {
        int index=-1;
        toBeChecked=toBeChecked.trim();
        String [] splittedtoBeChecked=toBeChecked.split(" ");
        
        List<Tree> leaves=root.getLeaves();
        int startOfSearch=-1;
        int endOfSearch=-1;
        
        if(right)
        {
            startOfSearch=leaves.indexOf(marker.getChild(0));
            endOfSearch=leaves.size();
        }
        else
        {
            startOfSearch=leaves.indexOf(marker.getChild(0))-1;
            endOfSearch=-1;
        }
        int counter=startOfSearch;
        while(counter!=endOfSearch)
        {
            if(leaves.get(counter).label().value().equalsIgnoreCase(splittedtoBeChecked[0]))
                if(checkForWholeSentence(splittedtoBeChecked, counter))
                    return counter;
            if(right)
                counter++;
            else
                counter--;
        }
        return index;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private static boolean checkForWholeSentence(String[] splittedtoBeChecked, int index) {
        for(int i=0;i<splittedtoBeChecked.length;i++)
        {
            if(!ParserDemo.sentCopy[i+index].equalsIgnoreCase(splittedtoBeChecked[i]))
                return false;
        }
        return true;
    }
    
    //This function checks the passive sentences from marker node and if it is passive,
    //the function returns the passive node which is the verb in past participle.
    private String sentenceIsPassive(Tree markerNode , int nodeNumber, Tree wholeTreeRoot) {
        
        NodeFunctionalities nf=new NodeFunctionalities();
        Tree passiveNode;
        VerbMatcher vm=new VerbMatcher();
        ArrayList<Tree> nodes=new ArrayList<>();
        Tree temp=wholeTreeRoot;
        nodes.add(temp);
        
        
        List<Tree> leaves=wholeTreeRoot.getLeaves();
        //int nodeNumber=temp.nodeNumber(wholeTreeRoot);
        boolean afterward=false;
        while(!nodes.isEmpty())
        {
            temp=nodes.get(nodes.size()-1);
            if(temp.equals(markerNode))
            {
                afterward=true;
            }
            if(afterward)
            {
                if((temp.label().value().equals("VBN"))&&(vm. checkFoundVerb(temp.getChild(0).value())))
                {
                    nodeNumber=temp.nodeNumber(wholeTreeRoot);
                    passiveNode=temp;
                    Tree previousNode=nf.findPreviousNodeBasedOnNodeNumber(temp, nodeNumber, leaves, wholeTreeRoot);
                    if((previousNode!=null)&&((previousNode.label().value().equals("VBZ"))||(previousNode.label().value().equals("VBP"))||
                            (previousNode.label().value().equals("VB"))))
                    {
                        Tree nextNode=nf.findNextNodeBasedOnNodeNumber(temp, nodeNumber, leaves, wholeTreeRoot);
                        if((nextNode!=null)&&((nextNode.label().value().equals("VBZ"))||(nextNode.label().value().equals("VBP"))||
                                (nextNode.label().value().equals("VB"))))//passive node has 3 parts like "has ben detecetd"
                        {
                            return previousNode.getChild(0).value()+" "+temp.getChild(0).value()+" "+nextNode.getChild(0).value();
                        }
                        else
                        {
                            return  previousNode.getChild(0).value()+" "+temp.getChild(0).value();
                        }
                    }
                }
            }
            
            enqueueChildren(temp, nodes);
        }
        
        return null;
    }
    
    private static String checkForMeasurementMarker(Tree node, Tree root, String toBeRefined) {
        toBeRefined=toBeRefined.trim();
        List<Tree> leaves=root.getLeaves();
        for(int i=0;i<leaves.size();i++)
        {
            if((leaves.get(i).value().toString().equalsIgnoreCase(toBeRefined))&&(leaves.get(i).nodeNumber(root)>node.nodeNumber(root)))
            {
                if((i+1)<leaves.size())
                {
                    if(CheckForMeasurementInExtractedLandMark(leaves.get(i+1).toString()))
                        return leaves.get(i+1).toString();
                }
            }
        }
        return null;
    }
    
    private static boolean markedWholeSeneteceAsDetected(int counterleaves, String[] toBeExcludedSplitted) {
        
        if(!checkForWholeSentence(toBeExcludedSplitted, counterleaves))
            return false;
        for(int i=0;i<toBeExcludedSplitted.length;i++)
        {
            ParserDemo.sent[i+counterleaves]="detected/Omitted";
        }
        return true;
    }
    
    private void enqueueChildren(Tree temp, ArrayList<Tree> nodes) {
        List<Tree> tempChildren=temp.getChildrenAsList();
        Collections.reverse(tempChildren);
        nodes.addAll(tempChildren);
        nodes.remove(temp);
    }
    
    private void insertSortedLandmarkAction(LandMarkAction currentLandmark, Tree root) {
        int score=sortingScore(currentLandmark);
        for(int i=ParserDemo.extractionStartingIndexForCurrentRoute;i<ParserDemo.extracted.size();i++){
            if(score<sortingScore(ParserDemo.extracted.get(i))){
                ParserDemo.extracted.add(i, currentLandmark);
                return;
            }
        }
        ParserDemo.extracted.add(currentLandmark);
    }
    
    private int sortingScore(LandMarkAction landmark){
        return landmark.rowIndex*10000+landmark.sentenceIndex*100+landmark.startingNodeNumber;
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String getNPAfterConnector(Tree root, Tree currentNode, String lastRefined) {
        
        String np="";
        List<Tree> nodeSiblings = currentNode.parent(root).siblings(root);
        if(nodeSiblings.isEmpty())
            return np;
        
        for (Tree sib : nodeSiblings)
        {
            // if(sib.label().value().equalsIgnoreCase(lm))
            if(sib.nodeNumber(root)>currentNode.nodeNumber(root))//we need to find right siblings of the node.
            {
                List<Tree> leaves=sib.getLeaves();
                for (Tree leaf : leaves)
                {
                    np=np+" "+ leaf.label().value().toString();
                }
            }
        }
        return np.trim();
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static String checkForPrepositionComingAfterBaseVerb(Tree node, int nodeNumber, Tree root) {
        String verb=node.value();
        List<Tree> leaves=root.getLeaves();
        for(int i=0; i<leaves.size();i++)
        {
            if(leaves.get(i).nodeNumber(root)==nodeNumber)
                
                if((i+1<leaves.size())&&((leaves.get(i+1).parent(root).value().equalsIgnoreCase("TO")||
                        (leaves.get(i+1).parent(root).value().equalsIgnoreCase("IN")) ||
                        (leaves.get(i+1).parent(root).value().equalsIgnoreCase("RP")))&&
                        (checkForPrepositions(leaves.get(i+1).parent(root).value()))))
                    
                    verb=verb+" "+leaves.get(i+1).value();
            
        }
        return verb;
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private String getNounPhraseComesAfterCurrentNode(Tree currentNode, Tree parse) {
        NodeFunctionalities nf=new NodeFunctionalities();
        String landMark="";
        
        List<Tree> subtree;
        //This current node doesn't have siblings but might its father has NP siblings
        Tree currentNodeFather=currentNode.parent(parse);
        if(currentNodeFather==null)
            return noSiblingString;
            
        List<Tree> currentNodeFatherSiblings=currentNodeFather.siblings(parse);
        
        if(currentNodeFatherSiblings.size()>0)
        {
            int a=0;
            Tree nextNodeToRight=nf.getNextRightSibling(currentNodeFather, parse, currentNodeFather.nodeNumber(parse));
            if((nextNodeToRight!=null)&&
                    (nextNodeToRight.value().equalsIgnoreCase("NP")))
            {
                subtree=nextNodeToRight.getLeaves();
                for(int i=0; i<subtree.size();i++)
                    landMark=landMark+" "+subtree.get(i).value().toString();
            }
        }
        else
            return noSiblingString;
        return landMark;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private static boolean checkForPrepositions(String toCheck) {
        
        for(int i=0;i<mistakenPrepositions.length;i++)
        {
            if(toCheck.equalsIgnoreCase(mistakenPrepositions[i]))
            {
                return false;
            }
        }
        return true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
    
    private String removeVerbAtEndOfLandMark(Tree root, String toBeRefined, Tree marker) {
        if(toBeRefined.equalsIgnoreCase(""))
            return "";
        VerbMatcher vm=new VerbMatcher();
        
        // boolean landMarkRefinement=true;
        NodeFunctionalities nf=new NodeFunctionalities();
        toBeRefined=toBeRefined.trim();
        String [] toBeRefinedSplitted=toBeRefined.split(" ");
        int len=toBeRefinedSplitted.length;
        Tree [] toBeRefinedSplittedNodes=new Tree[len];
        
        ArrayList<String> toBeRefinedSplittedArrayList;
        toBeRefinedSplittedArrayList=new ArrayList( Arrays.asList(toBeRefinedSplitted));
        String[] splittedLandMark=new String[len];
        
        Tree firstNode=nf.getNodeFromStringValue(root, null, 0 ,toBeRefinedSplitted[0]);
        
        toBeRefinedSplittedNodes[0]=firstNode;
        for(int i=1; i<len; i++)
            toBeRefinedSplittedNodes[i]=nf.getNodeFromStringValue(root, firstNode, firstNode.nodeNumber(root),toBeRefinedSplitted[i]);
        
        for(int i=0; i<len; i++)
            if(toBeRefinedSplittedNodes[i].parent(root).label().value().equalsIgnoreCase("CC") && i+1<len)
                
                if (vm.hasVerbTag(toBeRefinedSplittedNodes[i+1].parent(root).label().value())
                        ||vm.ifThisNodeIsUndetectedVerb(toBeRefinedSplittedNodes[i+1].parent(root), root))
                    
                    if(vm.checkFoundVerb(toBeRefinedSplittedNodes[i+1].parent(root).label().value()) &&
                            (vm.isAdjectiveVerb(toBeRefinedSplittedNodes[i+1], root, toBeRefinedSplittedNodes[i+1].nodeNumber(root))))
                    {
                        splittedLandMark=splitLandMark(toBeRefinedSplitted, i);
                        toBeRefined=splittedLandMark[0];
                        
                        List<CoreLabel> rawWords = Sentence.toCoreLabelList(splittedLandMark[1]);
                        lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
                        Tree rootNode = lp.apply(rawWords);
                        gerundBasedExtraction(rootNode, splittedLandMark[1]);
                    }
        return toBeRefined;
    }
////////////////////////////////////////////////////////////////////////////////////////////////
    private String longLandMarksRefinement(Tree root, String landMark, Tree marker) {
        String [] landMarkSplitted=landMark.split(" ");
        String refinedLandMark=removeDescriptiveMarkersInText(landMarkSplitted);
//        String [] refinedLandMarkSplitted=refinedLandMark.split(" ");
        
//        String sentenceAfterComma="";
//        String sentenceWithoutComma="";
//        for(int i=0; i<refinedLandMarkSplitted.length; i++)
//        {
//            if (refinedLandMarkSplitted[i].indexOf(",")!=-1)
//            {
//                if(i+1<refinedLandMarkSplitted.length && isInSubjectGroup(refinedLandMarkSplitted[i+1]))
//                {
//                    String [] sent=splitLandMark(refinedLandMarkSplitted, i+1);
//                    sentenceWithoutComma=sent[0];
//                    sentenceAfterComma=refinedLandMarkSplitted[i]+" "+ sent[1];
//                    
//                    //calling whole code on the new sentence
//                    
//                    return sentenceWithoutComma;
//                    
//                }
//            }
//            sentenceWithoutComma+=refinedLandMarkSplitted[i]+" ";
//        }
       return refinedLandMark;
    }
////////////////////////////////////////////////////////////////////////////////////////////////
    
    private String[] splitLandMark(String[] toBeRefinedSplitted, int index) {
        String firstPart="";
        String secondPart="";
        for(int i=0; i<toBeRefinedSplitted.length; i++)
        {
            if( i< index)
                firstPart+=toBeRefinedSplitted[i]+" ";
            if(i > index)
                secondPart+=toBeRefinedSplitted[i]+" ";
        }
        String [] splitted=new String[2];
        splitted[0]=firstPart;
        splitted[1]=secondPart;
        
        return splitted;
    }
    
    
    
    String [] descriptiveConnectores={"where", "because", "However", "On the other hand", "On the contrary", "Also", "therefore","thus"};
    private String removeDescriptiveMarkersInText(String[] landMarkSplitted) {
        String refined="";
        for(int i=0; i<landMarkSplitted.length; i++)
            if(!ifDescriptiveConnectores(landMarkSplitted[i]))
                refined+=landMarkSplitted[i]+" ";
            else
               return refined.trim(); 

       return refined.trim();
    }

    private boolean ifDescriptiveConnectores(String target) {
        for (int j=0; j< descriptiveConnectores.length; j++)
                if(target.equalsIgnoreCase(descriptiveConnectores[j]))
                    return true;
        return false;
    }
    
    
}
