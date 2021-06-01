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
import org.jsoup.helper.HttpConnection;
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
        XSSFSheet sheetRaw = workbook.createSheet("Raw");
        XSSFSheet sheetRawSent = workbook.createSheet("RawSentence");
        XSSFSheet sheetKStem = workbook.createSheet("KStemStopWordRemoval");
        XSSFSheet sheetKStemSentence = workbook.createSheet("KStemSentenceStopWordRemoval");
        XSSFSheet sheetPorter = workbook.createSheet("PorterStopWordRemoval");
        XSSFSheet sheetPorterSentence = workbook.createSheet("PorterSentenceStopWordRemoval");


        //Analyzer STEMMER = KStemmer; // Change with specified stemmer


        LinkedHashMap<String,Integer> linkPageCount = new LinkedHashMap<>();
//        linkPageCount.put("https://www.amazon.com/product-reviews/B01LYCLS24/ref=cm_cr_arp_d_viewopt_sr?" +
//                "ie=UTF8&filterByStar=critical&reviewerType=all_reviews&pageNumber=CURRENTPAGENUMBER&formatType=all_formats#reviews-filter-bar",82);

        linkPageCount.put("https://www.amazon.com/product-reviews/B08J5R3K3Q/ref=cm_cr_getr_d_paging_btm_next_2" +
                "?ie=UTF8&filterByStar=all_stars&reviewerType=all_reviews&pageNumber=CURRENTPAGENUMBER#reviews-filter-bar",214);

        PrintWriter xmlFileKStem = new PrintWriter("ReviewsCarrot2KStem.xml","UTF-8");
        PrintWriter xmlFilePorter = new PrintWriter("ReviewsCarrot2Porter.xml","UTF-8");
        PrintWriter xmlFileNoStem = new PrintWriter("ReviewsCarrot2NoStem.xml","UTF-8");

        xmlFileNoStem.println("<searchresult>");
        xmlFileKStem.println("<searchresult>");
        xmlFilePorter.println("<searchresult>");

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

                Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20100101 Firefox/10.0").get(); // Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:10.0) Gecko/20100101 Firefox/10.0
                Element bodyH = doc.body();
                Element cmReviewList = bodyH.getElementById("a-page");
                Elements reviews = cmReviewList.getElementsByAttributeValue("data-hook","review");

                for (Element r : reviews) {
                    xmlFileNoStem.println("<document>");
                    xmlFileKStem.println("<document>");
                    xmlFilePorter.println("<document>");

                    String title = r.getElementsByAttributeValue("data-hook","review-title").get(0).text();
                    xmlFileNoStem.println("<title>");
                    xmlFileKStem.println("<title>");
                    xmlFilePorter.println("<title>");

                    String pKStemtitle = preprocess(title,KStemmer);
                    String pPortertitle = preprocess(title,PorterStemmer);
                    String pRawtitle = preprocess(title,NoStem);

                    xmlFileNoStem.println(pRawtitle);
                    xmlFileKStem.println(pKStemtitle);
                    xmlFilePorter.println(pPortertitle);

                    xmlFileNoStem.println("</title>");
                    xmlFileKStem.println("</title>");
                    xmlFilePorter.println("</title>");

                    String body = r.getElementsByAttributeValue("data-hook","review-body").get(0).text();
                    xmlFileNoStem.println("<snippet>");
                    xmlFileKStem.println("<snippet>");
                    xmlFilePorter.println("<snippet>");

                    String pbodyRaw = preprocess(body,NoStem);
                    String pbodyKStem = preprocess(body,KStemmer);
                    String pbodyPorter = preprocess(body,PorterStemmer);

                    xmlFileNoStem.println(pbodyRaw);
                    xmlFileKStem.println(pbodyKStem);
                    xmlFilePorter.println(pbodyPorter);

                    xmlFileNoStem.println("</snippet>");
                    xmlFileKStem.println("</snippet>");
                    xmlFilePorter.println("</snippet>");



                    String star = r.getElementsByAttributeValue("data-hook","review-star-rating").get(0).text();



                    xmlFileNoStem.println("</document>");
                    xmlFileKStem.println("</document>");
                    xmlFilePorter.println("</document>");

                    sheetRaw.createRow(excelRow).createCell(0).setCellValue(star);
                    sheetRaw.getRow(excelRow).createCell(1).setCellValue(pRawtitle);
                    sheetRaw.getRow(excelRow).createCell(2).setCellValue(pbodyRaw);

                    sheetKStem.createRow(excelRow).createCell(0).setCellValue(star);
                    sheetRaw.getRow(excelRow).createCell(1).setCellValue(pKStemtitle);
                    sheetRaw.getRow(excelRow).createCell(2).setCellValue(pbodyKStem);

                    sheetPorter.createRow(excelRow++).createCell(0).setCellValue(star);
                    sheetRaw.getRow(excelRow).createCell(1).setCellValue(pPortertitle);
                    sheetRaw.getRow(excelRow).createCell(2).setCellValue(pbodyPorter);

                    if(!title.endsWith("."))
                        title=title+".";


                    CoreDocument document = new CoreDocument(title + " " + body);
                    pipeline.annotate(document);
                    for(CoreSentence sentence : document.sentences()) {
                        sheetRawSent.createRow(excelSentenceRow).createCell(0).setCellValue(preprocess(sentence.text(),NoStem));
                        sheetKStemSentence.createRow(excelSentenceRow).createCell(0).setCellValue(preprocess(sentence.text(),KStemmer));
                        sheetPorterSentence.createRow(excelSentenceRow++).createCell(0).setCellValue(preprocess(sentence.text(),PorterStemmer));
                    }
                    excelSentenceRow++;
                }
            }
        }
        xmlFileNoStem.println("</searchresult>");
        xmlFileKStem.println("</searchresult>");
        xmlFilePorter.println("</searchresult>");

        xmlFileNoStem.close();
        xmlFileKStem.close();
        xmlFilePorter.close();

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
