package Training;

import javax.xml.soap.Text;
import java.io.IOException;

/**
 * Created by SamZhang on 6/20/18.
 */
public class maintext {
    public static void main(String[] args) throws IOException{
        TextRank tr = new TextRank("MTDoc/query_list_ES_result_content.txt", "MTDoc/ranked_word.txt");
        tr.handle();
    }
}
