package Training;

import java.util.*;
import java.io.*;

import org.datavec.api.util.ClassPathResource;
import org.json.*;

/**
 * Created by SamZhang on 6/11/18.
 */
//public class TextRankTest {
//
//    public static void main(String[] args) throws IOException{
//        TextRank tr = new TextRank();
//        tr.handle();
//    }
//
//}

public class TextRank{
    int windowSize = 10;
    double STOPTHRESHOLD = 1e-10;
    int max_iter = 200;
    double d = 0.85;
    int TOP = 5;

    HashSet<String> stopword;
    String queryResultFile, rankOutputFile;
    String StopWordFile = "/Users/SamZhang/Documents/RA2017/Pseudo/PseudoFeedback/target/classes/MTDoc/english_stopwords";

    public TextRank(String queryResultFile, String rankOutputFile) throws IOException{
        this.stopword = loadStopWord(StopWordFile);

        this.queryResultFile = new ClassPathResource(queryResultFile)
                .getFile().getParentFile().getParent() + "/" + queryResultFile;

        this.rankOutputFile = new ClassPathResource(rankOutputFile)
                .getFile().getParentFile().getParent() + "/" + rankOutputFile;;
    }

    public void handle() throws IOException{

        //1. read in all document, words in order, Map<queryId, List of results<List or each result words>>
        Map<String, List<List<String>>> wordList = getWordList(queryResultFile);

        FileWriter fw = new FileWriter(new File(rankOutputFile));

        for(String queryId : wordList.keySet()){
            List<List<String>> curIdResList = wordList.get(queryId);

            //2. get the word map/connection from list of words, without stopwords and words with chars other than letters
            Map<String, Set<String>> wordMap = getWordMap(curIdResList);

            //3. initialize word ranking score
            Map<String, Double> wordScore = initScore(wordMap);

            //4. rank words based on map/connection as TextRank, result stores in wordScore
            wordScore = rank(wordMap, wordScore);

            //5. Sort the words based on their scores
            List<Node> sortWord = sortWords(wordScore);

            //6. Write out top ranked words for each query
            fw.write(queryId + "\t");
            for(int i = 0; i < TOP && i < sortWord.size(); i++){
                Node cur = sortWord.get(i);
                fw.write(String.format("%s %.8f\t", cur.word.replace("\"", ""), cur.score));
            }
            fw.write("\n");
        }

        fw.close();
    }

    public Map<String, List<List<String>>> getWordList(String fileName) throws IOException{
        File inputFile = new File(fileName);
        if(!inputFile.exists()) {
            System.err.println("Invalid Input File Path");
            System.exit(0);
        }

        Map<String, List<List<String>>> wordList = new HashMap();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
        String line = "";
        while((line = br.readLine()) != null){
            String[] temp = line.split("\t");
            String queryId = temp[0];
            List<List<String>> curListResults = new ArrayList();

            for(int resNum = 1; resNum < temp.length; resNum++){
                List<String> curList = new ArrayList();
                String curRes = temp[resNum];
                String[] curWords = curRes.split("\\s+");
                for(String w : curWords){
                    curList.add(w);
                }
                curListResults.add(curList);
            }
            wordList.put(queryId, curListResults);
        }

        return wordList;
    }

    public List<String> mergeLists(List<List<String>> l){
        List<String> merged = new ArrayList();
        for(List<String> temp : l){
            merged.addAll(temp);
        }
        return merged;
    }

    public Map<String, Set<String>> getWordMap(List<List<String>> curIdResList) {
        Map<String, Set<String>> wordMap = new HashMap<>();

        for(List<String> curWordList : curIdResList){
            Queue<Integer> queue = new LinkedList<>();
            for (int i = 0; i < curWordList.size(); i++) {
                String curWord = curWordList.get(i);

                if (!shouldInclude(curWord)) continue;
                while (!queue.isEmpty() && i - queue.peek() >= windowSize) queue.poll();

                wordMap.putIfAbsent(curWord, new HashSet<>());
                for (int index : queue) {
                    String preWord = curWordList.get(index);
                    wordMap.get(curWord).add(preWord);
                    wordMap.get(preWord).add(curWord);
                }

                queue.offer(i);
            }
        }

        return wordMap;
    }

    public Map<String, Double> initScore(Map<String, Set<String>> wordMap){
        Map<String, Double> wordScore = new HashMap();
        Random r = new Random();
        for(String temp : wordMap.keySet()) {
            wordScore.put(temp, r.nextDouble());
        }
        return wordScore;
    }

    public Map<String, Double> rank(Map<String, Set<String>> wordMap, Map<String, Double> wordScore){

        Map<String, Double> pre = wordScore;

        for(int i = 0; i < max_iter; i++) {
            //System.out.println(i);
            Map<String, Double> cur = new HashMap();
            double max_diff = 0;
            for (String curWord : wordMap.keySet()) {
                Set<String> In = wordMap.get(curWord);

                double curScore = 1 - d;
                for (String Vj : In) {
                    if (Vj.equals(curWord)) continue;
                    curScore += d * ((1 + 0.0) / wordMap.get(Vj).size()) * pre.get(Vj);
                }

                cur.put(curWord, curScore);
//                System.out.println(curWord + " " + pre.get(curWord) + " " + cur.get(curWord));
                max_diff = Math.max(max_diff, Math.abs(curScore - pre.getOrDefault(curWord, 1.0)));
            }
            pre = cur;

            //System.out.println(max_diff);
            if (max_diff <= STOPTHRESHOLD) break;
        }
        return pre;
    }

    public List<Node> sortWords(Map<String, Double> wordScore){
        List<Node> sortList = new ArrayList();
        for(String w : wordScore.keySet()){
            sortList.add(new Node(w, wordScore.get(w)));
        }
        Collections.sort(sortList,
                (a, b) -> a.score == b.score ? a.word.compareTo(b.word) : (b.score > a.score ? 1 : -1));

        return sortList;
    }

    private HashSet<String> loadStopWord(String StopWordFile) throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(StopWordFile))));
        String line = "";

        HashSet<String> stopword = new HashSet<>();

        while((line = br.readLine()) != null){
            line = line.trim();
            if(!line.isEmpty()) stopword.add(line);
        }

        br.close();
        return stopword;
    }

    private boolean isChar(String s){
        if(s == null || s.trim().isEmpty()) return false;
        return s.matches("[A-Z]?[a-z]*|[A-Z]+");
    }

    private boolean shouldInclude(String s){
        return !stopword.contains(s.toLowerCase()) && isChar(s);
    }

    class Node{
        double score;
        String word;
        Node(String word, double score){
            this.score = score;
            this.word = word;
        }
    }
}


