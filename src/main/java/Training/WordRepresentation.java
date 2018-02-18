package Training;

import java.io.IOException;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class WordRepresentation {
    private static Logger log;
    private static boolean Train;
    private static String modelPath, extendQueryPath, originalQueryPath;
    private static String queryResultPath;

    private static String doc_index, doc_type, field;

    static void init(){
        log = LoggerFactory.getLogger(WordRepresentation.class);

        /*
        * Word2Vec Parameters
        * */

        Train = false;
        modelPath = "MTDoc/w2vmodel.txt";
        extendQueryPath = "MTDoc/query_list_parsed_ES_extend.txt";
        originalQueryPath = "MTDoc/query_list_parsed_ES.txt";
        queryResultPath = "MTDoc/query_list_ES_result.txt";

        /*
        * ES Parameters
        * */

        doc_index = "sw-en-analysis";
        doc_type = "doc";
        field = "gold";
    }
    public static void main(String[] args) throws IOException{
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
            if(! new ClassPathResource(modelPath).getFile().exists()) {
                log.info("Please Train First!");
                System.exit(0);
            }
            W2VModel.loadAndTestW2v(modelPath, originalQueryPath, extendQueryPath, log);

            ES.ESsearchQueryFile(extendQueryPath, queryResultPath, doc_index, doc_type, field);
        }
    }
}

class W2VModel{

    static final String OOV = ".";
    static final String COMMA = ",";
    static final int NEARWORDS = 3;

    /**
     * Train Word2Vec mode.
     */


    public static void trainW2v(String inputFile, Logger log) throws IOException{
        log.info("Load & Vectorize Sentences....");

        SentenceIterator iter = new BasicLineIterator(inputFile);

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

        VocabCache<VocabWord> cache = new AbstractCache();
        WeightLookupTable<VocabWord> table = new InMemoryLookupTable.Builder<VocabWord>()
                .vectorLength(200)
                .useAdaGrad(false)
                .cache(cache).build();

        log.info("Building model....");
        Word2Vec word2vec = new Word2Vec.Builder()
                .minWordFrequency(5)
                .iterations(1)
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
        extendQuery(originalQueryPath, extendQueryPath, word2Vec, log);
    }

    /**
    * Extend query file based on the model trained by Word2Vec.
    * Now only look for nearest words for each query.
    *
    * Problems needs to be solved:
    *   1. How to deal with phrases
    *   2. How to deal with OOVs(currently replace with ".")
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

        while((line = br.readLine()) != null){
            if(line.startsWith("query_id")){
                continue;
            }

            StringBuilder sb = new StringBuilder();

            String[] q = line.split("\t");
            String id = q[0], words = q[1];
            String[] w = words.split(COMMA); // how to deal with phrases?

            sb.append(id).append("\t");

            for(String curWord : w){
                Collection<String> wordList = new ArrayList();

                if(word2Vec.hasWord(curWord)){
                    wordList = word2Vec.wordsNearest(curWord, NEARWORDS);
                }
                else{ //OOV
                    wordList = word2Vec.wordsNearest(OOV, NEARWORDS);
                    countOOV++;
                }

                sb.append(curWord).append(COMMA);
                for(String temp : wordList) sb.append(temp).append(COMMA);
            }

            sb.deleteCharAt(sb.length() - 1);
            fw.write(sb.toString() + "\n");
        }

        fw.close();
        br.close();
        System.out.println(countOOV);
    }
}
