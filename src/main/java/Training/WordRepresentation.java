package Training;

import java.io.IOException;

import com.sun.tools.javac.util.BasicDiagnosticFormatter;
import org.apache.log4j.BasicConfigurator;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
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

public class WordRepresentation {
    private static Logger log;
    static void init(){
        log = LoggerFactory.getLogger(WordRepresentation.class);
    }
    public static void main(String[] args) throws IOException{
        // TODO Auto-generated method stub
        BasicConfigurator.configure();
        init();
        PreProcessing.processing();
        String[] sw_enFile = PreProcessing.fileOutput();
        TrainW2V.trainW2v(sw_enFile[1], log);
    }
}

class TrainW2V{
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
        Word2Vec vec = new Word2Vec.Builder()
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
        vec.fit();

        log.info("Finished");
    }
}
