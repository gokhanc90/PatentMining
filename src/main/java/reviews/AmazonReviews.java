package reviews;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class AmazonReviews {
    public static void main(String[] args) throws IOException {
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
                    xmlFile.println(title);
                    xmlFile.println("</title>");

                    String body = r.getElementsByAttributeValue("data-hook","review-body").get(0).text();
                    xmlFile.println("<snippet>");
                    xmlFile.println(body);
                    xmlFile.println("</snippet>");

                    xmlFile.println("</document>");
                }
            }
        }
        xmlFile.println("</searchresult>");
        xmlFile.close();
    }
}
