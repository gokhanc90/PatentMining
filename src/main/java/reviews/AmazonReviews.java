package reviews;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import preprocess.Analyzers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AmazonReviews {
    public static Analyzer KStemmer = Analyzers.analyzerKStem();
    public static Analyzer PorterStemmer = Analyzers.analyzerPorterEng();
    public static Analyzer NoStem = Analyzers.analyzerDefault();


    public static void main(String[] args) throws IOException {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Text");
        XSSFSheet sheet2 = workbook.createSheet("Raw");
        XSSFSheet sheet3 = workbook.createSheet("Sentence");


        Analyzer STEMMER = KStemmer; // Change with specified stemmer


        LinkedHashMap<String,Integer> linkPageCount = new LinkedHashMap<>();
        linkPageCount.put("https://www.amazon.com/product-reviews/B01LYCLS24/ref=cm_cr_arp_d_viewopt_sr?" +
                "ie=UTF8&filterByStar=critical&reviewerType=all_reviews&pageNumber=CURRENTPAGENUMBER&formatType=all_formats#reviews-filter-bar",82);

        PrintWriter xmlFile = new PrintWriter("ReviewsCarrot2.xml","UTF-8");
        xmlFile.println("<searchresult>");

        int excelRow=0;
        int excelSentenceRow=0;

        Properties props;
        StanfordCoreNLP pipeline;
        props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit");
        props.setProperty("coref.algorithm", "neural");
        pipeline = new StanfordCoreNLP(props);


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
                    String ptitle = preprocess(title,STEMMER);
                    xmlFile.println(ptitle);
                    xmlFile.println("</title>");

                    String body = r.getElementsByAttributeValue("data-hook","review-body").get(0).text();
                    xmlFile.println("<snippet>");
                    String pbody = preprocess(body,STEMMER);
                    xmlFile.println(pbody);
                    xmlFile.println("</snippet>");

                    xmlFile.println("</document>");

                    sheet.createRow(excelRow).createCell(0).setCellValue(ptitle+" "+pbody);
                    sheet2.createRow(excelRow++).createCell(0).setCellValue(title + " " + body);

                    if(!title.endsWith("."))
                        title=title+".";


                    CoreDocument document = new CoreDocument(title + " " + body);
                    pipeline.annotate(document);
                    for(CoreSentence sentence : document.sentences()) {
                        sheet3.createRow(excelSentenceRow++).createCell(0).setCellValue(preprocess(sentence.text(),STEMMER));
                    }
                    excelSentenceRow++;
                }
            }
        }
        xmlFile.println("</searchresult>");
        xmlFile.close();

        FileOutputStream out = new FileOutputStream(new File("Reviews.xlsx"));
        workbook.write(out);
        out.close();
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
