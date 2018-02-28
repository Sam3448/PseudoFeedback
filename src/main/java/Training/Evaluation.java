package Training;

import org.datavec.api.util.ClassPathResource;

import java.util.*;

import java.io.*;

/**
 * Created by SamZhang on 2/21/18.
 *
 * Util class.
 * For this part, we just use the "query_result" file and get the AQWV score.
 */
public class Evaluation {
    /**
     * Parameter setting
     * */
    private static String searchOutputFilePath = "MTDoc/search_output.txt";
    private static String getSearchOutputFilePath = "MTDoc/changedQueryId.txt";

    /**
     * AQWV evaluation function
     *
     * reference file: files that should be retrieved
     * search file: files that we retrieved
     * */
    class Cell{
        String fileId;
        double score;
        Cell(String fileId, double score){
            this.fileId = fileId;
            this.score = score;
        }
    }
    TreeSet<Cell> treeSetBuilder(){
        return new TreeSet<Cell>((a, b) -> b.score > a.score ? 1 : -1);
    }

    public Set<String> getEvaluateQueryId() throws IOException{ // 用的是reference的file path
        ClassPathResource srcPath = new ClassPathResource(searchOutputFilePath);
        File f = new File(srcPath.getFile().getParentFile().getParent() + "/" + getSearchOutputFilePath);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

        HashSet<String> res = new HashSet();
        String line = "";

        while((line = br.readLine()) != null){
            res.add(line.trim());
        }

        br.close();
        return res;
    }

    public void getScore(String referenceFilePath, String doc_index, String doc_type,
                         String field, boolean ignoreNotExpanded, Set<String> changedQuery) throws IOException {

        //AQWV
        final double C = 0.0333;
        final double V = 1.0;
        final double P_relevant = 1.0 / 600;
        final double beta = 20.0;
        final double theta = 0.0;

        ClassPathResource searchPath = new ClassPathResource(searchOutputFilePath);
        ClassPathResource refPath = new ClassPathResource(referenceFilePath);
        File searchFile = searchPath.getFile(), refFile = refPath.getFile();
        HashMap<String, HashSet<String>> refQuery2File = new HashMap();
        HashMap<String, TreeSet<Cell>> searchQuery2File = new HashMap();

        //Exceptions
        if (!refFile.exists()) {
            System.out.println("Reference file path doesn't exist. Please input valid file path.");
            System.exit(0);
        }
        if (!searchFile.exists()) {
            System.out.println("Search file path doesn't exist. Please call preProcessQueryResult function first.");
            System.exit(0);
        }

        //Extract reference file
        BufferedReader brref = new BufferedReader(new InputStreamReader(new FileInputStream(refFile)));
        String refLine = "";
        while ((refLine = brref.readLine()) != null) {
            String[] temp = refLine.trim().split("\\s");
            String queryId = temp[0], relatedFile = temp[2];
            double relateLevel = Double.parseDouble(temp[3]);

            if (relateLevel > 0) {
                refQuery2File.putIfAbsent(queryId, new HashSet());
                refQuery2File.get(queryId).add(relatedFile);
            }
        }
        brref.close();

        //Extract search file
        BufferedReader brsearch = new BufferedReader(new InputStreamReader(new FileInputStream(searchFile)));
        String searchLine = "";
        while ((searchLine = brsearch.readLine()) != null) {
            String[] temp = searchLine.trim().split("\\s");
            String queryId = temp[0], relatedFile = temp[2];
            double relateLevel = Double.parseDouble(temp[4]);

            searchQuery2File.putIfAbsent(queryId, treeSetBuilder());
            searchQuery2File.get(queryId).add(new Cell(relatedFile, relateLevel)); // why in order? what's the difference than not sorted?
        }
        brsearch.close();

        //Get total number of documents
        ES es = new ES();
        int N_total = (int)es.getMatchAllResults(doc_index, doc_type, field, 0).getHits().getTotalHits();
        es.close();

        //Start evaluation
        double total_score = 0.0;
        double total_AP = 0.0;
        int total_count = 0;
        int total_miss = 0;
        int total_relevant = 0;
        int total_retrieved = 0;
        int total_match = 0;
        int total_FA = 0;
        int numberOfQuery = 0;

        HashMap<String, Double> scores = new HashMap();
        for(String queryId : searchQuery2File.keySet()){
            //ignore un expanded queries
            if(ignoreNotExpanded && !changedQuery.contains(queryId)) continue;

            if(!refQuery2File.containsKey(queryId)){
                continue;
            }

            numberOfQuery++;

            HashSet<String> ref_docs = refQuery2File.get(queryId);
            TreeSet<Cell> search_docs_withscore = searchQuery2File.get(queryId);
            List<String> search_docs = new ArrayList();

            for(Cell c : search_docs_withscore) search_docs.add(c.fileId);

            int N_miss = 0, N_FA = 0, N_match = 0;
            int N_relevant = ref_docs.size();
            total_relevant += N_relevant;
            int N_retrieved = search_docs.size();
            total_retrieved += N_retrieved;

            double precision_addup = 0.0;

            //get N_miss
            for(String temp : ref_docs){
                if(!search_docs.contains(temp)) N_miss++;
            }

            //get N_FA
            int count = 0;
            for(String temp : search_docs){
                ++count;
                if(!ref_docs.contains(temp)) N_FA++;
                else{
                    N_match++;
                    precision_addup += N_match / count;
                }
            }
            total_miss += N_miss;
            total_match += N_match;

            total_FA += N_FA;

            double P_miss = (N_miss * 1.0) / N_relevant;
            double P_FA = (N_FA * 1.0) / (N_total - N_relevant);
            double AP = precision_addup / N_relevant;
            total_AP += AP;

            scores.put(queryId, 1.0 - (P_miss + beta * P_FA));

            total_score += scores.get(queryId);
            total_count++;
        }

        double AQWV = total_score / total_count;
        double P_overall_miss = total_miss * 1.0 / total_relevant;
        double P_overall_FA = total_FA * 1.0 / (numberOfQuery * N_total - total_relevant);
        double P_overall_precision = (total_match * 1.0) / total_retrieved;
        double P_overall_recall = (total_match * 1.0) / total_relevant;
        double P_overall_F1 = (2 * P_overall_precision * P_overall_recall) / (P_overall_precision + P_overall_recall);
        double MAP = total_AP * 1.0 / total_count;

        //Print result
        System.out.println("****************RESULT******************");
        //System.out.println("Final AQWV =\t" + AQWV);
        System.out.println("Final P_miss =\t" + P_overall_miss);
        //System.out.println("Final P_FA =\t" + P_overall_FA);
        //System.out.println("Final P_precision =\t" + P_overall_precision);
        System.out.println("Final P_recall =\t" + P_overall_recall);
        //System.out.println("Final F1 =\t" + P_overall_F1);
        //System.out.println("Final MAP =\t" + MAP);
        System.out.println("****************/RESULT*****************");
    }

    /**
     * This function can preprocess the query_list_ES_result file to a standard format for later AQWV evaluation
     * */

    public void preProcessQueryResult(String queryResultPath) throws IOException{
        //get query result file
        ClassPathResource srcPath = new ClassPathResource(queryResultPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(srcPath.getFile())));
        String line = "";

        //Get output file
        FileWriter fw = new FileWriter(new File(srcPath.getFile().getParentFile().getParent() + "/" + searchOutputFilePath));

        //process each query and hits
        while((line = br.readLine()) != null){
            String[] id_contentwScore = line.split("\t"); //Query id
            String queryId = id_contentwScore[0];
            if(id_contentwScore.length != 2 || id_contentwScore[1].trim().length() == 0){
                fw.write(queryId + " " + "1 NO_HIT -1 1.0 STANDARD\n");
                continue;
            }
            String[] contentwScore = id_contentwScore[1].split("\\s"); // Query result

            //parse line
            int size = contentwScore.length;
            String[] retrieveId = new String[size];
            double[] score = new double[size];

            //for cur queryId, get all its retrieved doc Id and score
            for(int i = 0; i < size; i++){
                String temp = contentwScore[i];
                int startIndex = temp.indexOf("("), endIndex = temp.indexOf(")");
                retrieveId[i] = temp.substring(0, startIndex);
                score[i] = Double.parseDouble(temp.substring(startIndex + 1, endIndex));
            }

            // Add min_score normalization
            normalize_score(score);

            //Write out processed files
            for(int i = 0; i < size; i++){
                fw.write(String.format("%s 1 %s -1 %.5f STANDARD\n", queryId, retrieveId[i], score[i])); //Precision .5f
            }
        }

        br.close();
        fw.close();
    }
    private void normalize_score(double[] score){ //add min_score smoothing
        int len = score.length;
        double min_score = Double.MAX_VALUE;
        double total_score = 0;

        for(int i = 0; i < len; i++){
            double each_score = score[i];
            total_score += each_score;
            min_score = Math.min(min_score, each_score);
        }

        total_score += len * min_score;

        for(int i = 0; i < len; i++){
            score[i] = (score[i] + min_score) / total_score;
        }
    }
}
