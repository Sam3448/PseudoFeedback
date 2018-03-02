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

    static String inputFile;
    static File input;
    static String swOutputFile;
    static String enOutputFile;
    static String goldOutputFile;

    static String text, audio;
    static HashMap<String, String> sw = new HashMap(), en = new HashMap();

    /**
    * Initialize all parameters
    * */
    public static void init(Map<String, String> config) throws IOException{
        inputFile = config.get("inputFile".toLowerCase());
        text = config.get("textFile".toLowerCase());
        audio = config.get("audioFile".toLowerCase());

        input = new ClassPathResource(inputFile).getFile();
        swOutputFile = input.getParentFile() + "/sw.txt";
        enOutputFile = input.getParentFile() + "/en.txt";
        goldOutputFile = input.getParentFile() + "/GOLD.txt";
    }



    /*
    * Read MT files with tuple [id, sw, en], and store in sw,en HashMap.
    * */

    public static void processing() throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
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

        extend_MT();

        return new String[]{swOutputFile, enOutputFile, goldOutputFile};
    }


    public static void extend_MT() throws IOException{
        List<File> textFiles = getFiles(text);
        List<File> audioFiles = getFiles(audio);

        FileWriter fw = new FileWriter(goldOutputFile);

//        Set<String> keys = en.keySet();
//        for(String s : keys){//MT
//            fw.write(en.get(s) + "\n");
//        }

        //GOLD
        for(File f : textFiles) makeExtendFile(f, fw);
        for(File f : audioFiles) makeExtendFile(f, fw);

        fw.close();
    }

    private static void makeExtendFile(File f, FileWriter fw) throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String line = "";
        while((line = br.readLine()) != null){
            String[] temp = line.split("\t");
            fw.write(temp[2] + "\n");
        }
        br.close();
    }

    private static List<File> getFiles(String path) throws IOException{
        List<File> files = new ArrayList();
        File root = new File(path);
        if(root.exists() && root.isDirectory()){
            for(String child : root.list()){
                files.add(new File(root.getPath() + "/" + child));
            }
        }
        return files;
    }
}

