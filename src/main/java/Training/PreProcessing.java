package Training;
/**
 * Created by SamZhang on 2/14/18.
 *
 * Util class. Change the raw MT data format to a pure English/Swahili file.
**/
import org.datavec.api.util.ClassPathResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class PreProcessing {
    static File inputFile;
    static String swOutputFile;
    static String enOutputFile;
    static HashMap<String, String> sw = new HashMap(), en = new HashMap();

    /**
    * Initialize all parameters
    * */
    static void init() throws IOException{

        inputFile = new ClassPathResource("/MTDoc/MATERIAL_BASE-1A-BUILD_bitext.txt").getFile();
        swOutputFile = inputFile.getParentFile() + "/sw.txt";
        enOutputFile = inputFile.getParentFile() + "/en.txt";

    }

    /*
    * Read MT files with tuple [id, sw, en], and store in sw,en HashMap.
    * */

    public static void processing() throws IOException{
        init();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
        String line = "";
        while((line = br.readLine()) != null){
            String[] cur = line.split("\t");
            sw.put(cur[0], cur[1]);
            en.put(cur[0], cur[2]);
        }
        br.close();
    }

    /*
    * write sw,en HashMap out to certain files for Word2Vec model to use.
    * */

    public static String[] fileOutput() throws IOException{
        File swout = new File(swOutputFile), enout = new File(enOutputFile);
        FileWriter fwsw = new FileWriter(swout), fwen = new FileWriter(enout);

        Set<String> keys = sw.keySet();
        for(String s : keys){

            fwsw.write(sw.get(s) + "\n");
            fwen.write(en.get(s) + "\n");

        }

        fwsw.close();
        fwen.close();

        return new String[]{swOutputFile, enOutputFile};
    }
}

