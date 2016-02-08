/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Kam
 */
public class MTransPTransClassifier {
     public static String [] PTRANS={"exit", "walk", "reach", "trail", "turn" , "pass", "lead", "take", "tak", "face", "fac","follow",
    "continue", "continu","cross", "step", "head", "approach", "bring", "stay", "come","com" ,"wrap", "play", "change","chang", "head",
    "do", "go", "veer", "leave", "leav","pass", "enter", "re-enter", "reverse", "proceed", "reversing","reverse","revers", "keep",
    "came", "stop", "wait", "stop", "find", "wait", "position", "stand", "search", "offset", "protrunding","protrund", 
    "place", "plac","placed","slope","slop", "set", "set back","climb", "wind", "connect", "placed", "sloping", "form", "shift",
    "prevent", "using", "arriv","arrive", "mak", "crossed"};
    
     
     public static String [] MTRANS={"open", "is", "are", "be", "notic","nptice", "begin", "end", "cover", "feel", "detect",
     "mak", "make","be aware", "mark", "see", "sens","sense", "look", "meet", "press", "meets", "space","spac", "start", "not", "npte","locat",
     "separate","separat", "separating", "press", "select", "hear", "find", "stick", "use", "encounter", "found","occur",
     "happen", "runs", "exhaust", "met", "separated", "edged","edge", "appear", "border", "curv", "curve", "curving", "controls", "control",
     "encircl","encircle", "know", "identify","indicat", "intersect", "treat", "trear", "avoid", "enclose","enclos", "narrow", "pick", "check",
     "buzz", "handle","handl", "try", "imagine","imagin", "surrounded","surround", "block", "becomes", "become", "prefer", "have", "need", "seperating", "prevent"
     , "marked", "avoids", "serves", "covered", "blocking", "blocking,"};

     
     //////////////////////////////////////////////////////////////////////////////////////////////////

     public String checkforMTransPTrans(String matchingAction) {
         if(matchingAction==null)
            return matchingAction;
        if(matchingAction.equalsIgnoreCase("notFound"))
            return matchingAction;
        if(matchingAction.contains("PTrans/Move"))
            return matchingAction;
        if(matchingAction.contains("MTrans/Sense/See"))
            return matchingAction;        
        
        String [] matchingActionSplitted=matchingAction.split(" ");
        
        for(int i=0;i<matchingActionSplitted.length;i++)
        {
            for(int j=0;j<PTRANS.length;j++)
                if(matchingActionSplitted[i].toLowerCase().contains(PTRANS[j].toLowerCase()))
                    return "PTrans/Move" +" "+matchingAction;
        
        }
        
        for(int i=0;i<matchingActionSplitted.length;i++)
        {
            for(int j=0;j<MTRANS.length;j++)
                if(matchingActionSplitted[i].toLowerCase().contains(MTRANS[j].toLowerCase()))
                    return "MTrans/Sense/See"+" "+matchingAction;
        
        }
        return matchingAction;
    }
     
//     public String checkforMTransPTrans(String matchingAction) {
//        if(matchingAction.equalsIgnoreCase("notFound"))
//            return matchingAction;
//        if(matchingAction.contains("PTrans/Move"))
//            return matchingAction;
//        if(matchingAction.contains("MTrans/Sense/See"))
//            return matchingAction;        
//        
//        String [] matchingActionSplitted=matchingAction.split(" ");
//        
//        for(int i=0;i<matchingActionSplitted.length;i++)
//        {
//            for(int j=0;j<PTRANS.length;j++)
//                if(matchingActionSplitted[i].toLowerCase().contains(PTRANS[j].toLowerCase()))
//                    return "PTrans/Move" +" "+matchingAction;
//        
//        }
//        
//        for(int i=0;i<matchingActionSplitted.length;i++)
//        {
//            for(int j=0;j<MTRANS.length;j++)
//                if(matchingActionSplitted[i].toLowerCase().contains(MTRANS[j].toLowerCase()))
//                    return "MTrans/Sense/See"+" "+matchingAction;
//        
//        }
//        return matchingAction;
//    }
    
     
    
}