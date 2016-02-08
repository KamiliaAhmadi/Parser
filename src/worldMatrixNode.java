
import java.util.ArrayList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Kam
 */
public class worldMatrixNode {
    public String nodeLandMark;
    public int numEdges;  
    public ArrayList<String> neighborsOfGeodesicDistance5=new ArrayList<String>();//includes nodes with geodesic distance of the current node

    public void setGeodesicNeighbors(ArrayList<String> neighbors)
    {
        this.neighborsOfGeodesicDistance5=neighbors;
    }

}
