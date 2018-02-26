package Training;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by SamZhang on 2/25/18.
 */
public class TFIDF {
    public static void main(String[] args) throws IOException{
        TFIDF_test test = new TFIDF_test();
        test.buildTokens("sw-en-analysis", "doc", "gold");
    }
}

class TFIDF_test{
    public void buildTokens(String index, String type, String field) throws IOException{
        ES es = new ES();

        SearchResponse allDocs = es.getMatchAllResults(index, type, field, 1000);

        for(SearchHit curHit : allDocs.getHits()){
            Set<String> curSet = curHit.getSource().keySet();
            String key = curSet.iterator().next();
            String content = curHit.getSource().get(key).toString();

            List<String> tokens = es.getAnalyzedTokens(index, content);
            System.out.println(tokens.toString());
        }

        es.close();
    }
}
