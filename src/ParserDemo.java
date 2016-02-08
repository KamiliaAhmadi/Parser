
import java.util.Collection;
import java.util.List;
import java.io.StringReader;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.text.DefaultEditorKit;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


class ParserDemo {
    
    /**
     * The main method demonstrates the easiest way to load a parser. Simply
     * call loadModel and specify the path of a serialized grammar model, which
     * can be a file, a resource on the classpath, or even a URL. For example,
     * this demonstrates loading from the models jar file, which you therefore
     * need to include in the classpath for ParserDemo to work.
     */
    public static ArrayList<LandMarkAction> extracted;
    public static ArrayList<Route> routes;
    public static LexicalizedParser lp;  
    public static int currentRowIndex;
    public static int currentSentenceIndex;
    public static int currentRouteIndex;
    public static int extractionStartingIndexForCurrentRoute;
   
    public static String inputFileName="inputFiles\\narrativeMap.xlsx";
    public static String dotAddress="packages\\graphviz238\\bin\\";
    
    public static Path outputPathFolder=Paths.get("ProjectOutputs");
    public static String outputFileName=outputPathFolder.toString()+"\\outputSummary";
    public static String graphsLocation=outputPathFolder.toString()+"\\outputsGraphs\\";
    
    
    
    public static void main(String[] args) throws IOException, FileNotFoundException, InvalidFormatException, InterruptedException, ClassNotFoundException, SQLException {
        extracted=new ArrayList<>();
        lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        Date date= new Date();
        if(Files.exists(outputPathFolder))
        {
            outputFileName=outputFileName+date.toString().replace(':', '-')+".txt";
        } 
        else
        {
            File dir = new File(outputPathFolder.toString());
            dir.mkdir();
            
            File dir2 = new File(graphsLocation);
            dir2.mkdir();
            outputFileName=outputFileName+date.toString().replace(':', '-')+".txt";            
        }
        if (args.length > 0) {
            demoDP(lp, args[0]);
        } else {
            demoAPI(lp);
        }
    }
    
    /**
     * demoDP demonstrates turning a file into tokens and then parse trees. Note
     * that the trees are printed by calling pennPrint on the Tree object. It is
     * also possible to pass a PrintWriter to pennPrint if you want to capture
     * the output.
     */
    public static void demoDP(LexicalizedParser lp, String filename) {
        // This option shows loading, sentence-segmenting and tokenizing
        // a file using DocumentPreprocessor.
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        // You could also create a tokenizer here (as below) and pass it
        // to DocumentPreprocessor
        for (List<HasWord> sentence : new DocumentPreprocessor(filename)) {
            Tree parse = lp.apply(sentence);
            parse.pennPrint();
            System.out.println();
            
            GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
            Collection tdl = gs.typedDependenciesCCprocessed();
            System.out.println(tdl);
            System.out.println();
        }
    }
    
    /**
     * demoAPI demonstrates other ways of calling the parser with already
     * tokenized text, or in some cases, raw text that needs to be tokenized as
     * a single sentence. Output is handled with a TreePrint object. Note that
     * the options used when creating the TreePrint can determine what results
     * to print out. Once again, one can capture the output by passing a
     * PrintWriter to TreePrint.printTree.
     */
    public static String[] sent;
    public static String[] sentCopy;
    public static void demoAPI(LexicalizedParser lp) throws FileNotFoundException, IOException, InvalidFormatException, InterruptedException, ClassNotFoundException, SQLException {
        
        InputStream inputStream = new FileInputStream(new File(inputFileName));
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        
        //Get first/desired sheet from the workbook
        XSSFSheet sheet = workbook.getSheetAt(0);
        int totalRows = sheet.getPhysicalNumberOfRows();
        System.out.println("total no of rows >>>>" + totalRows);
        
        int rows; // No of rows
        rows = sheet.getPhysicalNumberOfRows();
        int cols; // No of columns
        
        String sentence;
        ArrayList<ArrayList<String>> map = new ArrayList<>();
        
        //Read from XLSX and store in map
        int routeIndex=0;
        currentRowIndex=0;
        int lastRowIndex=0;
              
        
        //building new map //creating new map which has routeIndex.
        XSSFWorkbook newMapWorkbook = new XSSFWorkbook ();
        XSSFSheet newMapSheet = newMapWorkbook.createSheet("newMapSheet");
        
        XSSFRow labelRow = newMapSheet.createRow(0);
        XSSFCell currentCell0 = labelRow.createCell(0);
        currentCell0.setCellValue("Route Index");
        
        XSSFCell currentCell1 = labelRow.createCell(1);
        currentCell1.setCellValue("Location");
        
        XSSFCell currentCell2 = labelRow.createCell(2);
        currentCell2.setCellValue("Starting Landmark");
        
        XSSFCell currentCell3 = labelRow.createCell(3);
        currentCell3.setCellValue("Destination Landmark");
        
        XSSFCell currentCell4 = labelRow.createCell(4);
        currentCell4.setCellValue("Step#");
        
        XSSFCell currentCell5 = labelRow.createCell(5);
        currentCell5.setCellValue("Description");
        
        for (int r = 1; r < rows; r++) {
            ArrayList<String> temp = new ArrayList<>();
            temp.add(Integer.toString(routeIndex));
            Row row = sheet.getRow(r);
            if (row != null) {
                cols = row.getPhysicalNumberOfCells();
                for (int c = 0; c < cols; c++) {
                    Cell cell = row.getCell(c);
                    sentence = cell.toString();
                    temp.add(sentence);
                    if(c==3)
                    {
                        lastRowIndex=currentRowIndex;
                        currentRowIndex=(int)Double.parseDouble(sentence);
                    }
                } // for
            }// end if
            
            if(lastRowIndex > currentRowIndex || (lastRowIndex==currentRowIndex))
            {
                routeIndex++;
                temp.set(0, Integer.toString(routeIndex));
            }
            map.add(temp);
            XSSFRow currentRow = newMapSheet.createRow(r); 
           for(int j=0;j<temp.size(); j++)
           {
               XSSFCell currentCell = currentRow.createCell(j);
               currentCell.setCellValue(temp.get(j));
           }
        }// end for
       
        FileOutputStream newMap =new FileOutputStream(new File("inputFiles\\newMap.xlsx"));
        newMapWorkbook.write(newMap);
       // newMap.flush();
        newMap.close();
        
        double currentStep=1;
        routes=new ArrayList<>();
        //currentRouteIndex=-1;
        for (int i = 0; i < map.size(); i++) {
            currentRowIndex=i;
            if(Double.parseDouble(map.get(i).get(4)) <= currentStep){
                if(map.get(i).size()==0)
                {
                    System.err.print("Empty row found");
                    continue;
                }
                extractionStartingIndexForCurrentRoute=extracted.size();
                routes.add(new Route(map.get(i).get(2),map.get(i).get(2)));
                currentRouteIndex=Integer.parseInt(map.get(i).get(0));
                LandMarkAction start=new LandMarkAction(map.get(i).get(2),"PTRANS/Start", ActionCharacteristics.PTRANS,currentRouteIndex,0,0,0);
                extracted.add(start);
                LandMarkAction end=new LandMarkAction(map.get(i).get(3),"PTRANS/Arrive at",ActionCharacteristics.PTRANS ,currentRouteIndex,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE);
                extracted.add(end);
            }
            currentStep=Double.parseDouble(map.get(i).get(4));
            String description = map.get(i).get(5); //Description of the route here
            
            String[] sentences = description.split("\\.");
            
            for (int j = 0; j < sentences.length; j++)//Loop with J
            {
                currentSentenceIndex=j;
                sentences[j]=sentences[j].trim();
                //sentences[j]=sentences[j].toLowerCase();
                sentences[j]=sentences[j].replaceAll("  ", " ");
               // sentences[j]= sentences[j].toLowerCase();
                sent = sentences[j].split(" ");
               
                sentCopy=sentences[j].split(" ");
                
                List<CoreLabel> rawWords = Sentence.toCoreLabelList(sent);
                Tree parse = lp.apply(rawWords);
                
                Extraction extraction=new Extraction();
                
                //Preposition Based Extraction--After First Extraction we need to exclude whatever we extracted to avoid iterative landMark and Action
                ArrayList<String> prepositions=extraction.prepositionBasedExtraction(parse, sentences[j]);
                
                //LandMark Extraction based on ExQuantifier
                ArrayList<String> existantialQuantifiers=extraction.exBasedExtraction(parse, sentences[j]);
                
                //Land Mark Extraction based on Articles
                ArrayList<String> articles=extraction.articleBasedExtraction(parse, sentences[j]);
                
                //LandMark extraction based on gerounds
                ArrayList<String> verbs=extraction.gerundBasedExtraction(parse, sentences[j]);
                
                // You can also use a TreePrint object to print trees and dependencies
                TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
                tp.printTree(parse);
                
                checkForOFCases(parse);  
            }//End loop with J
           
            
            System.out.println("WE ARE IN THE ROW EQUAL TO: "+i);
        }
        
        PostProcessing processor= new PostProcessing();
        extracted=processor.eliminateOverlaps(extracted);
        processor.makeGraphs(extracted);
        editExtracted();
        
        //Print preposition based extraction
        PrintWriter out = new PrintWriter(new FileWriter(outputFileName), true);
        out.write("RouteIndex"+"\t"+"RowIndex"+"\t"+"SentenceIndex"+"\t"+"StartingNodeIndex"+"\t"+"Action"+"\t"+"preposition"+"\t"+
                "exQuantifier"+"\t"+ "article"+"\t"+"existingConnector"+"\t"+"gerund"+"\t"+ "passive"+"\t"+"LandMark");
        out.write("\r\n");
        for(int k=0;k<extracted.size();k++)
        {
            out.write(extracted.get(k).routeIndex+"\t"+ extracted.get(k).rowIndex+"\t"+extracted.get(k).sentenceIndex+"\t" +
                    extracted.get(k).startingNodeNumber+"\t" +extracted.get(k).Action+"\t"+extracted.get(k).preposition+"\t"+
                    extracted.get(k).exQuantifier+"\t"+ extracted.get(k).article+"\t"+extracted.get(k).existingConnector+"\t"+
                    extracted.get(k).gerund+"\t"+ extracted.get(k).passive+"\t"+extracted.get(k).LandMark);
            out.write("\r\n");
        }
        
        InsertDataToDB DB=new InsertDataToDB();
        DB.insert();//insert data to DB and get query for Finding path utilizing Floyd Warshall algorithm.
        DB.floydWarshal();//all pair shortest path
        DB.buildWorld();
        
        writeIntoFileList(InsertDataToDB.worldNodes, "worldNodes");
        writeIntoFileArray(InsertDataToDB.worldMatrixMicro, "worldMatrixMicro");
        writeIntoFileArray(InsertDataToDB.worldMatrixMacro, "worldMatrixMacro");
        
        DrawMap dm=new DrawMap();
        dm.draw();
        out.close();

    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void checkForOFCases(Tree root) {
        
        if(extracted.size()==0)
            return;
        
        int indexOfLastAddedExtraction;
        int searchIndex=0;
        if((extracted.get(extracted.size()-1).rowIndex==2147483647)&&(extracted.size()-2>=0))
           indexOfLastAddedExtraction= extracted.size()-2;
        else
           indexOfLastAddedExtraction= extracted.size()-1; 
            
        for(int i=indexOfLastAddedExtraction;i>=0;i--)
            if(!( extracted.get(i).routeIndex==currentRouteIndex && extracted.get(i).rowIndex==currentRowIndex &&
                    extracted.get(i).sentenceIndex==currentSentenceIndex))
            {
                searchIndex=i+1;
                break;
            }
        
        for(int i=searchIndex+1;i<=indexOfLastAddedExtraction;i++)
            if(extracted.get(i).LandMark!=null && extracted.get(i).preposition!=null)
            {
                if((extracted.get(i).preposition.equalsIgnoreCase("of")))
                    if(checkForPossiblityOfMerge(extracted.get(i),extracted.get(i-1) , root))
                        return;
            }
    }
    
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
     public static boolean checkForPossiblityOfMerge(LandMarkAction destinationLMA, LandMarkAction sourceLMA, Tree root) {
        boolean canBeMerged=false;
         NodeFunctionalities nf=new NodeFunctionalities();
        ArrayList<String> leavesString=nf.produceLeavesStrings(root); 
               
        if((sourceLMA.LandMark==null)||(destinationLMA.LandMark==null))
            return false;
        
        String[] sourceSplitted=sourceLMA.LandMark.split(" ");
        String[] destinationSplitted=destinationLMA.LandMark.split(" ");
        
        if(sourceLMA.Action.equalsIgnoreCase(destinationLMA.Action))
        {
            canBeMerged=true;
            if(Math.abs((leavesString.indexOf(sourceSplitted[sourceSplitted.length-1])-leavesString.indexOf(destinationSplitted[0])))> 1)
            {
                int sourceIndex=ParserDemo.extracted.indexOf(sourceLMA);
                sourceLMA.LandMark=extractWholeString(leavesString, sourceSplitted[0], destinationSplitted[destinationSplitted.length-1]);
                ParserDemo.extracted.set(sourceIndex, sourceLMA);
                ParserDemo.extracted.remove(destinationLMA);
            }
            else
            {
                int sourceIndex=ParserDemo.extracted.indexOf(sourceLMA);
                sourceLMA.LandMark=sourceLMA.LandMark+" "+destinationLMA.LandMark;
                ParserDemo.extracted.set(sourceIndex, sourceLMA);
                ParserDemo.extracted.remove(destinationLMA);
            }
        }
        return canBeMerged;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////     
     public static String extractWholeString(ArrayList<String> leavesString, String source, String destination) {
        
        String extractedString="";
        int sourceIndex=leavesString.indexOf(source);
        int destinationIndex=leavesString.indexOf(destination);
        
        for(int i=sourceIndex;i<=destinationIndex;i++)
            extractedString=extractedString+" "+leavesString.get(i);
        return extractedString;       
        
    }

     private static void editExtracted() {
         for(int i=1; i<extracted.size();i++)
         {
             if(extracted.get(i).Action.trim().equalsIgnoreCase("notFound") &&
                     extracted.get(i).LandMark.equalsIgnoreCase("No Siblings/No LandMark"))
                 extracted.remove(i);
             
             if(extracted.get(i).Action.trim().equalsIgnoreCase("notFound"))
             {
                 if(extracted.get(i).preposition!=null &&
                         (extracted.get(i).preposition.trim().equalsIgnoreCase("to") || extracted.get(i).preposition.equalsIgnoreCase("toward")
                         ||extracted.get(i).preposition.equalsIgnoreCase("through") || extracted.get(i).preposition.equalsIgnoreCase("in")))
                     extracted.get(i).article="PTrans/Move";
                 else
                     extracted.get(i).Action="MTrans/See";
                 
             }
             
             if(extracted.get(i).LandMark.trim().equalsIgnoreCase("No Siblings/No LandMark") ||
                     extracted.get(i).LandMark.trim().equalsIgnoreCase(""))
                 extracted.get(i).LandMark= extracted.get(i-1).LandMark;
         }
      }    

    private static void writeIntoFileArray(String [][] array, String FileName) throws IOException {
        PrintWriter arrayWriter = new PrintWriter(new FileWriter(FileName+".txt"), true);
        
        for(int i=0; i<array.length;i++)
        {
            for(int j=0; j<array[0].length;j++)
            {
                if(array[i][j]!=null &&  array[i][j].contains("\n"))
                {
                    array[i][j]=array[i][j].replace("\n", "STEP");
                    array[i][j]=array[i][j].trim();
                }
                arrayWriter.write(array[i][j]+"\t");
            }
            arrayWriter.write("\n");
        } 
        
        arrayWriter.close();
    }

    private static void writeIntoFileList(ArrayList<String> list, String name) throws IOException {
        PrintWriter listWriter = new PrintWriter(new FileWriter(name+".txt"), true);
        
        for(int i=0; i<list.size();i++)
            listWriter.write(list.get(i)+"\t");
        
        listWriter.close();
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private ParserDemo() {
    }     
}
