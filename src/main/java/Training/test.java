package Training;


import java.io.IOException;
import java.util.Set;

/**
 * Created by SamZhang on 2/21/18.
 */
public class test {
    public static void main(String[] args) throws IOException {
        Evaluation e = new Evaluation();
        e.preProcessQueryResult("MTDoc/query_list_ES_result.txt");

        Set<String> changedQuery = e.getEvaluateQueryId();

        e.getScore("MTDoc/1A.qrel", "sw-en-analysis",
                          "doc", "gold", true, changedQuery);
    }
}
