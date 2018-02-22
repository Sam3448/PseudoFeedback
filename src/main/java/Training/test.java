package Training;

import java.io.IOException;

/**
 * Created by SamZhang on 2/21/18.
 */
public class test {
    public static void main(String[] args) throws IOException {
        Evaluation e = new Evaluation();
        e.getAQWVScore("MTDoc/1A.qrel", "sw-en-analysis", "doc", "gold");
    }
}
