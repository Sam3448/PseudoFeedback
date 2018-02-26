package Training;


/**
 * Created by SamZhang on 2/15/18.
 *
 * Util for ES. Can take either query_file/extended_query_file or single query.
 *
 */
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.datavec.api.util.ClassPathResource;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.*;
import java.net.InetAddress;
import java.util.*;

public class ES {

    private static final int RESULT_SIZE = 15;
    private static final String LOGIC = "OR";
    RestHighLevelClient client;
    RestClient lowLevelRestClient;

    FileWriter test;
    /**
     * Init
     * */

    ES(){
        //Get client
        lowLevelRestClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http")).build();
        client = new RestHighLevelClient(lowLevelRestClient);
    }

    /**
     * Input query file, and search for the whole file
     * */

    public void ESsearchQueryFile(String extendQueryPath, String queryResultPath,
                                         String doc_index, String doc_type, String field) throws IOException{
        //Get query file
        ClassPathResource srcPath = new ClassPathResource(extendQueryPath);
        File extendQueryFile = srcPath.getFile();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(extendQueryFile)));
        String line = "";

        //Get output result file
        FileWriter fw = new FileWriter(new File(srcPath.getFile().getParentFile().getParent() + "/" + queryResultPath));
        test = new FileWriter(new File("/Users/SamZhang/Downloads/cmp.txt"));

        //Execute each query
        while((line = br.readLine()) != null){
            //Parse query
            String[] curQuery = line.split("\t");
            String queryId = curQuery[0];
            String queryString = curQuery[1];

            //Execute search for one query
            SearchResponse searchResponse = ESsearch(doc_index, doc_type, field, queryString);

            //Write out results
            SearchHits hits = searchResponse.getHits();
            System.out.println("Total Number of Hits :\t" + hits.getTotalHits());

            test.write(queryId + "\t" + queryString + "\t" + hits.getTotalHits() + "\n\n");

            fw.write(queryId + "\t");

            for(SearchHit hit : hits.getHits()){
                String id = hit.getId();
                String content = hit.getSourceAsString();
                float score = hit.getScore();

                fw.write(String.format("%s(%.5f) ", id, score));//Precision .5f
            }

            fw.write("\n");
        }

        test.close();
        fw.close();
        br.close();
    }

    /**
     * Use ES searching for one line of query
     * */

    public SearchResponse ESsearch( String doc_index, String doc_type,
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
            queryArr[i] = queryArr[i].trim();
            sb.append(queryArr[i]).append((i == queryArr.length - 1 ? "" : " " + LOGIC + " "));
        }
        System.out.println(sb.toString());
        QueryBuilder queryBuilder = QueryBuilders.queryStringQuery(sb.toString().trim()).field(field);
        test.write(queryBuilder.toString() + "\n");

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

    /**
     * Get how many docs under a index/type
     * */
    public SearchResponse getMatchAllResults(String doc_index, String doc_type, String field, int expectSize) throws IOException{

        QueryBuilder queryBuilder = QueryBuilders.queryStringQuery("*");

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.size(expectSize);

        SearchRequest searchRequest = new SearchRequest(doc_index);
        searchRequest.types(doc_type);
        searchRequest.source(searchSourceBuilder);

        SearchResponse res = client.search(searchRequest);

        return res;
    }

    public List<String> getAnalyzedTokens(String index, String query) throws IOException{

        TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));

        List<String> res = new ArrayList();

        AnalyzeRequest request = (new AnalyzeRequest(index)).analyzer("standard").text(query);//Using standard analyzer, the same as Elasticsearch mapping setting

        List<AnalyzeResponse.AnalyzeToken> tokens = client.admin().indices().analyze(request).actionGet().getTokens();
        for (AnalyzeResponse.AnalyzeToken token : tokens)
        {
            res.add(token.getTerm());
        }

        client.close();

        return res;
    }

    public void close() throws IOException {
        lowLevelRestClient.close();
    }
}
