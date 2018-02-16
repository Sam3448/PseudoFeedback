package Training;


/**
 * Created by SamZhang on 2/15/18.
 */
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;

public class ES {

    private static final int RESULT_SIZE = 20;

    public static void ESsearch(String doc_index, String doc_type, String field, String query) throws IOException{
        /*
        * Search setup
        * */
        RestClient lowLevelRestClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http")).build();

        RestHighLevelClient client = new RestHighLevelClient(lowLevelRestClient);

        SearchRequest searchRequest = new SearchRequest(doc_index);
        searchRequest.types(doc_type);

        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(field, query);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchQueryBuilder);
        searchSourceBuilder.size(RESULT_SIZE);

        searchRequest.source(searchSourceBuilder);

        /*
        * Response from ES
        * */

        SearchResponse searchResponse = client.search(searchRequest);

        SearchHits hits = searchResponse.getHits();
        System.out.println("Total Number of Hits :\t" + hits.getTotalHits());

        int count = 0;
        for(SearchHit hit : hits.getHits()){
            String id = hit.getId();
            String content = hit.getSourceAsString();
            float score = hit.getScore();

            System.out.println("Document Number :\t" + ++count);
            System.out.println("Document Id :\t" + id + "\t with score :\t" + score);
            System.out.println(content + "\n");
        }

        lowLevelRestClient.close();
    }
}
