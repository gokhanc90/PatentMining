package reviews;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.analysis.Analyzer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import preprocess.Analyzers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AmazonReviews {
    public static Analyzer KStemmer = Analyzers.analyzerKStem();
    public static Analyzer PorterStemmer = Analyzers.analyzerPorterEng();
    public static Analyzer NoStem = Analyzers.analyzerDefault();


    public static void main(String[] args) throws IOException {
        Analyzer STEMMER = KStemmer; // Change with specified stemmer


        LinkedHashMap<String,Integer> linkPageCount = new LinkedHashMap<>();
        linkPageCount.put("https://www.amazon.com/product-reviews/B01LYCLS24/ref=cm_cr_arp_d_viewopt_sr?" +
                "ie=UTF8&filterByStar=critical&reviewerType=all_reviews&pageNumber=CURRENTPAGENUMBER&formatType=all_formats#reviews-filter-bar",82);

        PrintWriter xmlFile = new PrintWriter("ReviewsCarrot2.xml","UTF-8");
        xmlFile.println("<searchresult>");

        for(Map.Entry<String,Integer> e: linkPageCount.entrySet()) {
            String baseUrl=e.getKey();
            for(int i=1; i<=e.getValue();i++) {
                String url = baseUrl.replace("CURRENTPAGENUMBER",String.valueOf(i));

                Document doc = Jsoup.connect(url).get();
                Element cmReviewList = doc.getElementById("cm_cr-review_list");
                Elements reviews = cmReviewList.getElementsByAttributeValue("data-hook","review");
                for (Element r : reviews) {
                    xmlFile.println("<document>");

                    String title = r.getElementsByAttributeValue("data-hook","review-title").get(0).text();
                    xmlFile.println("<title>");
                    title = preprocess(title,STEMMER);
                    xmlFile.println(title);
                    xmlFile.println("</title>");

                    String body = r.getElementsByAttributeValue("data-hook","review-body").get(0).text();
                    xmlFile.println("<snippet>");
                    body = preprocess(body,STEMMER);
                    xmlFile.println(body);
                    xmlFile.println("</snippet>");

                    xmlFile.println("</document>");
                }
            }
        }
        xmlFile.println("</searchresult>");
        xmlFile.close();
    }


    public static String preprocess(String review,Analyzer stemmer){
        List<String> tokens = Analyzers.getAnalyzedTokens(review,stemmer);
        String preprocessed = tokens.stream()
                .map(t->StringUtils.replaceChars(t,"<$>","")) //Replace some characters
                .filter(t-> !NumberUtils.isCreatable(t)) // Remove numbers
                .filter(t-> !StringUtils.containsAny(t,"'’")) // Remove the terms including ' and ’
                .collect(Collectors.joining(" ")) // Merge all tokens
                .trim(); // Remove white space from beginning and ending
        return preprocessed;
    }
}
