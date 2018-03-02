package Training;

import ca.szc.configparser.Ini;
import org.datavec.api.util.ClassPathResource;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by SamZhang on 2/25/18.
 */
public class TFIDF {
    public static void main(String[] args) throws Exception{
        Path input = Paths.get("/Users/SamZhang/Documents/RA2017/Pseudo/PseudoFeedback/target/classes/Config/pseudo.cfg");
        Ini ini = new Ini().read(input);
        Map<String, Map<String, String>> cur = ini.getSections();

        TFIDF_test test = new TFIDF_test(cur);
        File f = new File("/Users/SamZhang/Documents/RA2017/Pseudo/PseudoFeedback/target/classes/MTDoc/tokens_After_Analyzer.txt");
        if(!f.exists()){
            test.buildTokens("sw-en-analysis", "doc", "gold");
        }
        else{
            test.loadTokens();
        }
        test.calculate_TFIDF();
    }
}

class TFIDF_test{
    Map<String, List<String>> doc_tokens;
    Map<String, Map<String, Integer>> doc_vocab_count;
    Map<String, Set<String>> vocab_docs;
    Map<String, Integer> doc_wordCount;
    Map<String, TreeMap<Double, String>> doc_TF_IDF;

    Map<String, Map<String, String>> totalConfig;

    private String tokensOutput;

    public TFIDF_test(Map<String, Map<String, String>> totalConfig){
        doc_vocab_count = new HashMap();
        vocab_docs = new HashMap<>();
        doc_wordCount = new HashMap<>();
        doc_TF_IDF = new HashMap<>();
        doc_tokens = new HashMap<>();
        this.totalConfig = totalConfig;

        Map<String, String> config = totalConfig.get("TFIDF");
        this.tokensOutput = config.get("tokensOutput".toLowerCase());
    }

    public void loadTokens() throws IOException{
        File input = new ClassPathResource(tokensOutput).getFile();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
        String line = "";

        while((line = br.readLine()) != null){
            line = line.trim();
            String[] temp = line.split("\t");
            doc_tokens.put(temp[0], new ArrayList<>(Arrays.asList(temp[1].split(" "))));
        }

        br.close();
    }

    public void buildTokens(String index, String type, String field) throws IOException{
        ES es = new ES(totalConfig.get("ES"));

        FileWriter fw = new FileWriter(new File(
                "/Users/SamZhang/Documents/RA2017/Pseudo/PseudoFeedback/target/classes/MTDoc/tokens_After_Analyzer.txt"));

        SearchResponse allDocs = es.getMatchAllResults(index, type, field, 1000);
        int count = 0;

        for(SearchHit curHit : allDocs.getHits()){
            Set<String> curSet = curHit.getSource().keySet();
            String key = curSet.iterator().next();//gold只选了这一个
            String content = curHit.getSource().get(key).toString();//得到每个文档的段落内容

            List<String> tokens = es.getAnalyzedTokens(index, content);
            String curDocId = curHit.getId();
            System.out.println(curDocId + " " + count++);

            fw.write(curDocId + "\t");
            for(String t : tokens){
                fw.write(t + " ");
            }
            fw.write("\n");

            doc_tokens.put(curDocId, tokens);
        }

        es.close();
        fw.close();
    }

    public void buildMaps(){
        for(String curDocId : doc_tokens.keySet()){
            List<String> tokens = doc_tokens.get(curDocId);

            doc_wordCount.put(curDocId, tokens.size());//统计文章总词数
            doc_vocab_count.putIfAbsent(curDocId, new HashMap<>());
            Map<String, Integer> curWord_count = doc_vocab_count.get(curDocId);

            for(String word : tokens){
                vocab_docs.putIfAbsent(word, new HashSet<>());
                Set<String> curIdSet = vocab_docs.get(word);
                if(!curIdSet.contains(curDocId)) curIdSet.add(curDocId);//将文档id加到词出现过的的set中

                curWord_count.put(word, curWord_count.getOrDefault(word, 0) + 1); //统计当前文档，这个词出现次数
            }
        }
    }

    public void calculate_TFIDF() {
        buildMaps();

        Set<String> docId = doc_wordCount.keySet();
        int totalDocs = docId.size();

        for (String curId : docId) {
            doc_TF_IDF.putIfAbsent(curId, new TreeMap<Double, String>((a, b) -> b - a > 0 ? 1 : -1));
            Map<Double, String> curdoc_TF_IDF = doc_TF_IDF.get(curId);

            Map<String, Integer> vocab_count = doc_vocab_count.get(curId);
            int totalWordsInThisDoc = doc_wordCount.get(curId);

            for (String curword : vocab_count.keySet()) {
                double TF = (vocab_count.get(curword) * 1.0) / totalWordsInThisDoc;
                double IDF = Math.log((totalDocs * 1.0) / (vocab_docs.get(curword).size() + 1.0));
                double TF_IDF = TF * IDF;
                curdoc_TF_IDF.put(TF_IDF, curword);
            }
        }

    }
}
