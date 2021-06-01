package preprocess;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Analyzers {

    public static final String FIELD = "field";


    /**
     * Intended to use with one term queries (otq) only
     *
     * @param text input string to analyze
     * @return analyzed input
     */
    public static String getAnalyzedToken(String text, Analyzer analyzer) {
        final List<String> list = getAnalyzedTokens(text, analyzer);
        if (list.size() != 1)
            System.err.println("Text : " + text + " contains more than one tokens : " + list.toString());
        return list.get(0);
    }

    /**
     * Modified from : http://lucene.apache.org/core/4_10_2/core/org/apache/lucene/analysis/package-summary.html
     */
    public static List<String> getAnalyzedTokens(String text, Analyzer analyzer) {

        final List<String> list = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream(FIELD, new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken())
                list.add(termAtt.toString());

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return list;
    }



    public static Analyzer analyzerDefault() {
        try {
            return CustomAnalyzer.builder(Paths.get("./"))
                    .withTokenizer("standard")
                    .addTokenFilter("lowercase")
//                    .addTokenFilter("stop", "ignoreCase", "false", "words", "stopwords.txt", "format", "wordset")
                    .build();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static Analyzer analyzerDefaultWordNet() {
        try {
            return CustomAnalyzer.builder(Paths.get("./"))
                    .withTokenizer("standard")
                    .addTokenFilter("lowercase")
                    .addTokenFilter(SynonymGraphFilterFactory.class, "format", "wordnet", "expand", "true", "synonyms", "wn_s.pl")
                    .build();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static Analyzer analyzerPorter() {
        try {
            return CustomAnalyzer.builder(Paths.get("./"))
                    .withTokenizer("standard")
                    .addTokenFilter("apostrophe")
                    .addTokenFilter("turkishlowercase")
                    .addTokenFilter("stop", "ignoreCase", "false", "words", "stopwords.txt", "format", "wordset")
                    .addTokenFilter(SnowballPorterFilterFactory.class, "language", "Turkish")
                    .build();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    public static Analyzer analyzerPorterEng() {
        try {
            return CustomAnalyzer.builder(Paths.get("./"))
                    .withTokenizer("standard")
                    .addTokenFilter("lowercase")
//                    .addTokenFilter("stop", "ignoreCase", "false", "words", "stopwords.txt", "format", "wordset")
                    .addTokenFilter(SnowballPorterFilterFactory.class)
                    .build();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    public static Analyzer analyzerKStem() {
        try {
            return CustomAnalyzer.builder(Paths.get("./"))
                    .withTokenizer("standard")
                    .addTokenFilter("lowercase")
//                    .addTokenFilter("stop", "ignoreCase", "false", "words", "stopwords.txt", "format", "wordset")
                    .addTokenFilter(KStemFilterFactory.class)
                    .build();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static Map<Integer,List<String>> getAnalyzedTokensWithSynonym(String text, Analyzer analyzer) {

        final Map<Integer,List<String>> list = new LinkedHashMap<>();
        try (TokenStream ts = analyzer.tokenStream(FIELD, new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            final OffsetAttribute offsetAttribute = ts.addAttribute(OffsetAttribute.class);

            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                int startOffset = offsetAttribute.startOffset();
                if(list.containsKey(startOffset)){
                    List<String> l = list.get(startOffset);
                    l.add(termAtt.toString());
                    list.put(startOffset,l);
                }
                else{
                    ArrayList<String> l = new ArrayList<>();
                    l.add(termAtt.toString());
                    list.put(startOffset, l);
                }
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return list;
    }
}
