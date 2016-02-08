
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//this class keeps preposition, landmark, and action
class LandMarkAction { 
    String preposition;
    String exQuantifier;
    String LandMark;
    String Action;
    int actionType;
    int rowIndex;//In which row we dectected this landmark
    int sentenceIndex; //In which sentence of the current row we dectected this landmark
    int startingNodeNumber; //the node index in the constructed tree
    int routeIndex; //Index of the route based on aggregation of multiple rows using step#
    String article;
    String existingConnector;
    String gerund;
    String passive;
    int distance;
    ActionCharacteristics actionCharacteristics;
    public LandMarkAction(){}
    public LandMarkAction(String landmark, String action, int actionType, int routeIndex, int rowIndex, int sentenceIndex, int startingNodeNumber){
        LandMark=landmark;
        Action=action;
        this.actionType=actionType;
        this.routeIndex=routeIndex;
        this.rowIndex=rowIndex;
        this.sentenceIndex=sentenceIndex;
        this.startingNodeNumber=startingNodeNumber;
    }
    
}

class Route{
    String startingPoint;
    String endPoint;
    public Route(String start, String end)
    {
        startingPoint=start;
        endPoint=end;
    }
}