package DataStructure;

import java.util.*;

/**
 * Created by SamZhang on 2/24/18.
 */
public class Trie {
    public String prefix;
    public TreeSet<String> words;
    public HashMap<Character, Trie> child;

    public Trie(){
        words = new TreeSet<String>((a, b) -> a.length() - b.length());
        child = new HashMap<>();
        prefix = "";
    }
}
