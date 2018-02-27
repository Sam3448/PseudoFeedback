package Training;

/**
 * Main class.
 *  1. Call preprocess to get en/sw files for W2V to train.
 *  2. Train W2V model
 *  3. Get W2V model and extend query file by neighbor words (NEARWORDS). Format: query_id    words
 *  4. Send the extend file to ES and get the retrieved results. Format: query_id    retrieved_doc_ids
 *      5. Evaluate the retrieved result with reference file
 *
 *
 *  get retrieved results and use words inside to further expand query. Loop step 3 to 5.
 *
 * */

import java.io.IOException;

import DataStructure.Trie;
import org.apache.log4j.BasicConfigurator;
import org.datavec.api.util.ClassPathResource;

import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

//import sys.change.preprocessor.*;

import org.apache.uima.resource.ResourceInitializationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.util.*;

public class WordRepresentation {
    private static Logger log;
    private static boolean Train;
    private static String modelPath, extendQueryPath, originalQueryPath, copyQueryPath;
    private static String queryResultPath;

    private static String doc_index, doc_type, field;

    private static final int PSEUDO_LOOP = 1;

    static void init(){
        log = LoggerFactory.getLogger(WordRepresentation.class);

        /*
        * Word2Vec Parameters
        * */

        Train = false;
        modelPath = "MTDoc/w2vmodel.txt";
        extendQueryPath = "MTDoc/query_list_parsed_ES_extend.txt";
        originalQueryPath = "MTDoc/query_list_parsed_ES.txt";
        copyQueryPath = "MTDoc/query_list_parsed_ES_copy.txt";
        queryResultPath = "MTDoc/query_list_ES_result.txt";

        /*
        * ES Parameters
        * */

        doc_index = "sw-en-analysis";
        doc_type = "doc";
        field = "gold";
    }
    public static void main(String[] args) throws IOException, ResourceInitializationException {
        // TODO Auto-generated method stub
        //log setup
        BasicConfigurator.configure();

        init();

        if(Train) {
            PreProcessing.processing();
            String[] sw_enFile = PreProcessing.fileOutput();
            W2VModel.trainW2v(sw_enFile[1], log);
        }
        else{
            //Exception
            if(! new ClassPathResource(modelPath).getFile().exists()) {
                log.info("Please Train First!");
                System.exit(0);
            }

            //Copy original query file
            copyOriginalQuery();

            //Pseudo feedback and query extension
            for(int i = 0; i < PSEUDO_LOOP; i++) {
                //Step 3: extend query file
                W2VModel.loadAndTestW2v(modelPath, copyQueryPath, extendQueryPath, log);
                //Step 4: ES for result file
                ES es = new ES();
                es.ESsearchQueryFile(extendQueryPath, queryResultPath, doc_index, doc_type, field);
                es.close();
                //Pseudo feedback to copy query file
                pseudoFeedback();
            }
        }
    }

    public static void pseudoFeedback(){//???

    }

    //Copy original query file
    public static void copyOriginalQuery() throws IOException{
        ClassPathResource srcPath = new ClassPathResource(originalQueryPath);
        File originalQueryFile = srcPath.getFile();
        if(!originalQueryFile.exists()){
            log.info("Please import query file (Parsed) for extension.");
            System.exit(0);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(originalQueryFile)));
        FileWriter fw = new FileWriter(new File(srcPath.getFile().getParentFile().getParent()
                + "/" + copyQueryPath));

        String line = "";
        while((line = br.readLine()) != null){
            fw.write(line + "\n");
        }

        br.close();
        fw.close();
    }
}

class W2VModel{

    private static final String OOV = "."; // add nothing
    private static final String COMMA = ",";
    private static final int NEARWORDS = 1;
    private static Trie root;
    private static PrefixSearch prefixsearch;
    private static final int tolerance = 3;
    private static CommonPreprocessor commonpreprocessor = new CommonPreprocessor();

    /**
     * Train Word2Vec mode.
     */


    public static void trainW2v(String inputFile, Logger log) throws IOException, ResourceInitializationException {
        log.info("Load & Vectorize Sentences....");

        SentenceIterator iter = new BasicLineIterator(inputFile);

        TokenizerFactory t = new DefaultTokenizerFactory();

        t.setTokenPreProcessor(commonpreprocessor); // Common preprocessor: 去掉所有标点，并且所有单词lowercase化

        VocabCache<VocabWord> cache = new AbstractCache();
        WeightLookupTable<VocabWord> table = new InMemoryLookupTable.Builder<VocabWord>()
                .vectorLength(200)
                .useAdaGrad(false)
                .cache(cache).build();

        log.info("Building model....");
        Word2Vec word2vec = new Word2Vec.Builder()
                .minWordFrequency(0)
                .iterations(10)
                .epochs(1)
                .layerSize(100)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .lookupTable(table)
                .vocabCache(cache)
                .build();

        log.info("Fitting Word2Vec model....");

        word2vec.fit();

        String modelPath = inputFile.substring(0, inputFile.lastIndexOf('/')) + "/w2vmodel.txt";

        log.info("writing model file to path :" + modelPath);

        WordVectorSerializer.writeWord2VecModel(word2vec, modelPath);

        log.info("Finished");
    }


    /**
     * Load trained Word2Vec model, and call extendQuery to write out the extended query file.
     *
     * */


    public static void loadAndTestW2v(String modelPath, String originalQueryPath, String extendQueryPath, Logger log) throws IOException {
        Word2Vec word2Vec = WordVectorSerializer.readWord2VecModel(new ClassPathResource(modelPath)
                .getFile()
                .getAbsolutePath());

        //Check out vocab words
        Collection<String> vocab = word2Vec.getVocab().words();

        FileWriter fw = new FileWriter("/Users/SamZhang/Documents/RA2017/Pseudo/PseudoFeedback/target/classes/MTDoc/w2v_dict.txt");
        for(String s : vocab){
            fw.write(s + "\n");
        }
        fw.close();

        //build Trie base on w2v vocab, for prefix match of OOVs
        prefixsearch = new PrefixSearch();
        root = prefixsearch.buildeTrieRoot(vocab);

        //word2vec done. extend query
        extendQuery(originalQueryPath, extendQueryPath, word2Vec, log);
    }

    /**
    * Extend query file based on the model trained by Word2Vec.
    * Now only look for nearest words for each query.
    *
    * Problems needs to be solved:
    *   1. How to deal with phrases ignore or word by word or parse and get the nouns or verbs
    *   2. How to deal with OOVs(currently replace with ".") ==> ignore
    *   3. Python version?
    * */

    public static void extendQuery(String originalQueryPath, String extendQueryPath, Word2Vec word2Vec, Logger log) throws IOException{
        ClassPathResource srcPath = new ClassPathResource(originalQueryPath);
        if(! srcPath.getFile().exists()){
            log.info("Please import query file (Parsed) for extension.");
            System.exit(0);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(srcPath.getFile())));

        File extendQueryFile = new File(srcPath.getFile().getParentFile().getParent() + "/" + extendQueryPath);
        FileWriter fw = new FileWriter(extendQueryFile);

        String line = "";
        int countOOV = 0;
        int totalWords = 0;



        while((line = br.readLine()) != null){
            if(line.startsWith("query_id")){
                continue;
            }

            StringBuilder sb = new StringBuilder();

            String[] q = line.split("\t");
            String id = q[0], words = q[1];
            String[] w = words.split(COMMA);

            sb.append(id).append("\t");

            for(String curWord : w){
                if(curWord == null || curWord.length() == 0 || curWord.trim().length() == 0) continue;

                Collection<String> wordList = new ArrayList();
                totalWords++;

                //replaceWord 就是当前query的表征词, 一个词的query就是query本身，词组才会选择出表证词
                String representWord = curWord.trim();
//                if(representWord.contains(" ")){
//                        List<String> rankedWords = extractWords(representWord);
//                        representWord = rankedWords.get(0);//highest score
//                }

                //进行预处理，对于本来就在vocab的词没有任何影响，对于不在的筛除可能的标点
                representWord = representWord.trim();
                representWord = commonpreprocessor.preProcess(representWord);


                //进行prefix match with tolerance of left_over length.
//                if(!word2Vec.hasWord(representWord)){
//                    System.out.println("Prefix before : " + representWord);
//                    representWord = prefixsearch.searcPrefix(representWord, root, tolerance); //Low precision. Need Improvement.
//                    System.out.println("Prefix after : " + representWord);
//                }


                if(representWord != null && word2Vec.hasWord(representWord)){//query word in vocab, extend straightly
                    wordList = word2Vec.wordsNearest(representWord, NEARWORDS);
                    System.out.println();
                }
                else{ //OOV
                    countOOV++;
                    System.out.println("Before : " + curWord + "\tAfter : " + (representWord.length() == 0 ? "NONE" : representWord) + '\n');
                }

                sb.append(curWord).append(COMMA);
                for(String temp : wordList) sb.append(temp).append(COMMA);
            }

            sb.deleteCharAt(sb.length() - 1);
            fw.write(sb.toString() + "\n");
        }

        fw.close();
        br.close();
        System.out.println("****OOV = " + countOOV);
        System.out.println("****Total = " + totalWords);
    }

    private static List<String> extractWords(String phrase){// Not implemented
        List<String> rankWords = new ArrayList();

        rankWords.add(phrase.substring(phrase.lastIndexOf(' ') + 1));

        return rankWords;
    }
}
