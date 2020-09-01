package nlp;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class POSTagger {
    Properties props;
    StanfordCoreNLP pipeline;
    public POSTagger() {
        props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
        props.setProperty("coref.algorithm", "neural");
        // build pipeline
        pipeline = new StanfordCoreNLP(props);
    }


    private void process(String text) {
        text="Very displeased. It bounces around the room as if it is completely lost, using 90 degree angles whenever it hits a wall. It gets stuck under everything, our couch, inside the maze our dining room chairs make, under the couch again, on carpet runners, bath mats, cords... you name it. I found that we had to \"eufy\"-proof our house as if we were introducing a new child to a home. Couch pillows scattered around the couch so that it won't try to get underneath, pick up any USB cords, put down a 2x4 bumper wall around our dining room set, prop open the door to our laundry room that it always manages to close itself into, etc. The list goes on and on. Maybe this is par for the course with all robotic vacuums, and if that's the case, I say save yourself the trouble. EDIT: I must thoroughly applaud Eufy/Anker Customer Service. They discovered this review and despite my purchasing this product months ago went far above and beyond. They quickly worked to thoroughly understand the problems I had with my Eufy, going so far as to ask for a video of the problems and engaging a product engineer. Sadly nothing could be done about my specific problems, but Eufy surprised me again by offering a full refund, including providing me with a free item of my choosing for the trouble. I have no hesitation in buying another Eufy/Anker product because of their exceptional Customer Service, despite the issues I had with this one particular product.";

        // create a document object
        CoreDocument document = new CoreDocument(text);
        // annnotate the document
        pipeline.annotate(document);
        // examples

        // 10th token of the document
        CoreLabel token = document.tokens().get(0);
        System.out.println("Example: token");
        System.out.println(token);
        System.out.println();

        // text of the first sentence
        String sentenceText = document.sentences().get(0).text();
        System.out.println("Example: sentence");
        System.out.println(sentenceText);
        System.out.println();

        // second sentence
        CoreSentence sentence = document.sentences().get(1);

        // list of the part-of-speech tags for the second sentence
        List<String> posTags = sentence.posTags();
        System.out.println("Example: pos tags");
        System.out.println(posTags);
        System.out.println();

        // list of the ner tags for the second sentence
        List<String> nerTags = sentence.nerTags();
        System.out.println("Example: ner tags");
        System.out.println(nerTags);
        System.out.println();

        // constituency parse for the second sentence
        Tree constituencyParse = sentence.constituencyParse();
        System.out.println("Example: constituency parse");
        System.out.println(constituencyParse);
        System.out.println();

        // dependency parse for the second sentence
        SemanticGraph dependencyParse = sentence.dependencyParse();
        System.out.println("Example: dependency parse");
        System.out.println(dependencyParse);
        System.out.println();

        // kbp relations found in fifth sentence
//        List<RelationTriple> relations =
//                document.sentences().get(4).relations();
//        System.out.println("Example: relation");
//        System.out.println(relations.get(0));
//        System.out.println();

        // entity mentions in the second sentence
        List<CoreEntityMention> entityMentions = sentence.entityMentions();
        System.out.println("Example: entity mentions");
        System.out.println(entityMentions);
        System.out.println();

        // coreference between entity mentions
        CoreEntityMention originalEntityMention = document.sentences().get(1).entityMentions().get(0);
        System.out.println("Example: original entity mention");
        System.out.println(originalEntityMention);
        System.out.println("Example: canonical entity mention");
        System.out.println(originalEntityMention.canonicalEntityMention().get());
        System.out.println();

        // get document wide coref info
        Map<Integer, CorefChain> corefChains = document.corefChains();
        System.out.println("Example: coref chains for document");
        System.out.println(corefChains);
        System.out.println();


    }


    public void process2() throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(new File("Reviews.xlsx")));
        XSSFSheet sheet = workbook.getSheet("Raw");
        Iterator<Row> iterator = sheet.iterator();

        BufferedWriter writer = new BufferedWriter(new FileWriter("NLPReviews.txt"));
        int number = 1;
        while (iterator.hasNext()) {
            System.out.println(number);
            Row currentRow = iterator.next();
            String text = currentRow.getCell(1).getStringCellValue();
            posTag(text,writer,number++);
        }
        writer.close();
    }

    private void posTag(String text,BufferedWriter writer,int reviewNumber) throws IOException {
        // create a document object
        writer.write("R"+reviewNumber+":\t"+text);
        writer.newLine();
        CoreDocument document = new CoreDocument(text);
        // annnotate the document
        pipeline.annotate(document);
        int sentenceNumber=1;
        for(CoreSentence sentence: document.sentences()) {
            writer.write("\tS"+sentenceNumber+":\t"+sentence);
            writer.newLine();
            // list of the part-of-speech tags for the second sentence
            List<String> posTags = sentence.posTags();
            writer.write("\tPosTag:\t"+posTags);
            writer.newLine();
            // list of the ner tags for the second sentence
            List<String> nerTags = sentence.nerTags();
            writer.write("\tNerTag:\t"+nerTags);
            writer.newLine();
            sentenceNumber++;
        }
    }


    public static void main(String[] args) throws IOException {
        new POSTagger().process2();

    }
}
