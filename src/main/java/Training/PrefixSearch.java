package Training;

import DataStructure.Trie;

import java.util.Collection;

/**
 * Created by SamZhang on 2/24/18.
 */
public class PrefixSearch {

    public Trie buildeTrieRoot(Collection<String> vocab){
        Trie root = new Trie();

        for(String word : vocab){
            Trie p = root;

            for(char c : word.toCharArray()){
                p.words.add(word);
                p.child.putIfAbsent(c, new Trie());

                Trie next = p.child.get(c);
                next.prefix = p.prefix + c;

                p = next;
            }

            p.words.add(word);
        }

        return root;
    }

    public String searcPrefix(String word, Trie root, int tolerance){
        if(word == null) return null;
        word = word.trim();
        if(word.length() == 0) return "";

        Trie p = root;
        char[] c = word.toCharArray();
        int len = word.length();

        int i = 0;
        for(; i < len; i++){
            if(!p.child.containsKey(c[i])){
                if(len - i <= tolerance) break;
                else return "";
            }

            p = p.child.get(c[i]);
        }

        return p.words.first();
    }
}
