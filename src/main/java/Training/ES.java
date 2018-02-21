package Training;


/**
 * Created by SamZhang on 2/15/18.
 */
import org.apache.http.HttpHost;
import org.datavec.api.util.ClassPathResource;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.*;
import java.util.*;

public class ES {

    private static final int RESULT_SIZE = 10;
    private static final String LOGIC = "OR";

    /**
     * Input query file, and search for the whole file
     * */

    public static void ESsearchQueryFile(String extendQueryPath, String queryResultPath,
                                         String doc_index, String doc_type, String field) throws IOException{
        //Get query file
        ClassPathResource srcPath = new ClassPathResource(extendQueryPath);
        File extendQueryFile = srcPath.getFile();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(extendQueryFile)));
        String line = "";

        //Get output result file
        FileWriter fw = new FileWriter(new File(srcPath.getFile().getParentFile().getParent() + "/" + queryResultPath));

        //Get client
        RestClient lowLevelRestClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http")).build();
        RestHighLevelClient client = new RestHighLevelClient(lowLevelRestClient);

        //Execute each query
        fw.write(String.valueOf(RESULT_SIZE) + "\n");
        while((line = br.readLine()) != null){
            //Parse query
            String[] curQuery = line.split("\t");
            String queryId = curQuery[0];
            String queryString = curQuery[1];

            //Execute search for one query
            SearchResponse searchResponse = ESsearch(client, doc_index, doc_type, field, queryString);

            //Write out results
            SearchHits hits = searchResponse.getHits();
            System.out.println("Total Number of Hits :\t" + hits.getTotalHits());

            fw.write(queryId + "\t");

            for(SearchHit hit : hits.getHits()){
                String id = hit.getId();
                String content = hit.getSourceAsString();
                float score = hit.getScore();

                fw.write(String.format("%s(%.5f) ", id, score));
            }

            fw.write("\n");
        }

        lowLevelRestClient.close();
        fw.close();
        br.close();
    }

    /**
     * Use ES searching for one line of query
     * */

    public static SearchResponse ESsearch(RestHighLevelClient client,
                                          String doc_index, String doc_type,
                                          String field, String query) throws IOException{

        /*
        * Search setup
        * */

        //Build query
        String[] queryArr = new String[1];
        if(query.contains(",")) queryArr = query.split(",");
        else queryArr[0] = query;

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < queryArr.length; i++){
            sb.append(queryArr[i]).append((i == queryArr.length ? "" : " " + LOGIC + " "));
        }

        QueryBuilder queryBuilder = QueryBuilders.matchQuery(field, sb.toString());
        System.out.println("*************" + queryBuilder.toString());

        //Use querybuilder to define searchSourceBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.size(RESULT_SIZE);

        //Get search request
        SearchRequest searchRequest = new SearchRequest(doc_index);
        searchRequest.types(doc_type);

        //Send search request
        searchRequest.source(searchSourceBuilder);

        //return the result of search, synchronize
        return client.search(searchRequest);
    }
}
