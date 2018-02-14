package Training;
/**
 * Created by SamZhang on 2/14/18.
**/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class PreProcessing {
    static String inputFile = "/Users/SamZhang/Documents/RA2017/Pseudo/src/bitext/MATERIAL_BASE-1A-BUILD_bitext.txt";
    static String swOutputFile = "/Users/SamZhang/Documents/RA2017/Pseudo/src/bitext/en.txt";
    static String enOutputFile = "/Users/SamZhang/Documents/RA2017/Pseudo/src/bitext/sw.txt";
    static HashMap<String, String> sw = new HashMap(), en = new HashMap();
    public static void processing() throws IOException{
        File input = new File(inputFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
        String line = "";
        while((line = br.readLine()) != null){
            String[] cur = line.split("\t");
            en.put(cur[0], cur[1]);
            sw.put(cur[0], cur[2]);
        }
        br.close();
    }
    public static void fileOutput() throws IOException{
        File swout = new File(swOutputFile), enout = new File(enOutputFile);
        FileWriter fwsw = new FileWriter(swout), fwen = new FileWriter(enout);

        Set<String> keys = sw.keySet();
        for(String s : keys){
            fwsw.write(sw.get(s) + "\n");
            fwen.write(en.get(s) + "\n");
        }

        fwsw.close();
        fwen.close();
    }
}

