package Training;


import ca.szc.configparser.Ini;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Created by SamZhang on 2/21/18.
 */
public class test {
    public static void main(String[] args) throws IOException {
        Path input = Paths.get("/Users/SamZhang/Documents/RA2017/Pseudo/PseudoFeedback/target/classes/Config/pseudo.cfg");
        System.out.println(input.toAbsolutePath());
        Ini ini = new Ini().read(input);
        Map<String, Map<String, String>> cur = ini.getSections();
        System.out.println(cur.get("PreProcessing").get("textfile"));

        Evaluation e = new Evaluation(cur);
        e.preProcessQueryResult("MTDoc/query_list_ES_result.txt");

        Set<String> changedQuery = e.getEvaluateQueryId();

        e.getScore("MTDoc/1A.qrel", "sw-en-analysis",
                          "doc", "gold", true, changedQuery);
    }
}
