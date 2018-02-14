package Training;

import java.io.IOException;

import com.sun.tools.javac.util.BasicDiagnosticFormatter;
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
    private static String modelPath;

    static void init(){
        log = LoggerFactory.getLogger(WordRepresentation.class);
        Train = false;
        modelPath = "MTDoc/w2vmodel.txt";
    }
    public static void main(String[] args) throws IOException{
        // TODO Auto-generated method stub
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
            W2VModel.testW2v(modelPath);
        }
    }
}

class W2VModel{
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
        System.out.println(modelPath);
        WordVectorSerializer.writeWord2VecModel(word2vec, modelPath);

        log.info("Finished");
    }

    public static void testW2v(String modelPath) throws IOException{
        Word2Vec word2Vec = WordVectorSerializer.readWord2VecModel(new ClassPathResource(modelPath)
                .getFile()
                .getAbsolutePath());
        Collection<String> res = word2Vec.wordsNearest("is", 10);
        System.out.println(word2Vec.hasWord("is"));
        System.out.println(res.toString());
    }
}
