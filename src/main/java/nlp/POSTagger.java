package nlp;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
        XSSFSheet sheet = workbook.getSheet("Text");
        Iterator<Row> iterator = sheet.iterator();

        HashMap<String,List<String>> tags = new HashMap<>();

        BufferedWriter writer = new BufferedWriter(new FileWriter("NLPReviews.txt"));
        int number = 1;
        while (iterator.hasNext()) {
            System.out.println(number);
            Row currentRow = iterator.next();
            String text = currentRow.getCell(1).getStringCellValue();
            posTag(text,writer,number++,tags);
        }
        writer.close();

        Map<String,Integer> freq = new HashMap<>();
        for(Map.Entry<String,List<String>> entry : tags.entrySet()){
            List<String> words = entry.getValue();

            if(entry.getKey().startsWith("NN")){
                for(String word : words){
                    freq.put(word,freq.getOrDefault(word,0)+1);
                }
            }
        }

        Map<String,Integer> sortedByFreq =
                freq.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        for(Map.Entry<String,Integer> entry : sortedByFreq.entrySet()){
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }


    }

    private void posTag(String text,BufferedWriter writer,int reviewNumber, HashMap<String,List<String>> tags) throws IOException {
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

            for(int i=0;i<posTags.size(); i++){
                String postag = posTags.get(i);
                String word = sentence.tokens().get(i).word();
                if(tags.get(postag)==null){
                    tags.put(postag,new ArrayList<String>());
                }
                List<String> tagWords = tags.get(postag);
                tagWords.add(word);
                tags.put(postag,tagWords);
            }
        }
    }


    public static void main(String[] args) throws IOException {
//        new POSTagger().process2();
        String text = "5/35/18 update Don’t waste your money. " +
                "These products are NOT made to last. " +
                "The Robovac 11c replacement (3rd replacement) quit working properly. " +
                "The vacuum now gives a false error as if there is something stuck. " +
                "I spent $200 on a vacuum that lasted a year! " +
                "We had a great initial experience with this product, but it was short lived. " +
                "Unfortunately the warranty from the original purchase expired 60 days ago. " +
                "I am now stuck with another Eufy Robovac that does not work, and all I get from customer support is 15% discount for another product that will not last. DON’T BUY IT!!!!! " +
                "Update… Eufy customer support contacted me after my last review where I commented on the issues I was having after 3 months of continuous use. " +
                "Once again, I have nothing but praises about their support. " +
                "They offered another replacement, except this time with an 11c model. " +
                "So far it works great, as did the first two. I hope this model lasts longer. " +
                "I purchased this in march and was very pleased with it. " +
                "The robovac does an excellent job navigating through our lower floor while we are asleep. " +
                "It works quietly and we woke up to a clean floor. " +
                "I am now on my second RoboVac thanks to Eufy's excellent customer service and running into the same problems I had with my original purchase. " +
                "The vacuum does an excellent job avoiding walls and large objects, not so much when it comes to chair legs. " +
                "After 3 months of ocasional bumping into chairs, the lens that covers the sensor gets scratched and becomes cloudy. " +
                "Now I wake up to the sound of the robovac ramming into walls instead of avoiding them, just like my original did as well. " +
                "I've tried cleaning the plastic lens but only works momentarily. " +
                "Now it's also starting to have problems docking into the charging station, which I presume is due to the cloudy lens cover. " +
                "It's sad that such a good product fails because o a small lens cover. " +
                "Some small tweaks in design should fix this problem and allow the vacuum to work properly for longer than 3 months. " +
                "Hey Eufy! How about placing a better rubber on the bumper to protect the lens cover, or changing the material ? " +
                "If not for that this vac would have had a 5 star\n";


        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,kbp,quote");
        // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
        props.setProperty("coref.algorithm", "neural");
        // build pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // create a document object
        CoreDocument document = new CoreDocument(text);
        // annnotate the document
        pipeline.annotate(document);


        Map<String,List<String>> nouns = new LinkedHashMap<>();
        List<String> verbs = new ArrayList<>();
        for(CoreSentence sentence : document.sentences()) {
            List<String> posTags = sentence.posTags();
            for(int i=0;i<posTags.size(); i++){
                String postag = posTags.get(i);
                if(postag.startsWith("NN"))
                    nouns.put(sentence.tokens().get(i).word(),new ArrayList<>());
                if(postag.startsWith("VB"))
                    verbs.add(sentence.tokens().get(i).word());

            }
        }



        // second sentence
//        CoreSentence sentence = document.sentences().get(5);

        for(CoreSentence sentence : document.sentences()) {
//        // list of the ner tags for the second sentence
//        List<String> nerTags = sentence.nerTags();
//        System.out.println("Example: ner tags");
//        System.out.println(nerTags);
//        System.out.println();
//
//        // constituency parse for the second sentence
//        Tree constituencyParse = sentence.constituencyParse();
//        System.out.println("Example: constituency parse");
//        System.out.println(constituencyParse);
//        System.out.println();

            // dependency parse for the second sentence
            SemanticGraph dependencyParse = sentence.dependencyParse();
            System.out.println("Example: dependency parse");
            System.out.println(dependencyParse);
            System.out.println();

            for(IndexedWord word : dependencyParse.vertexListSorted()){
                for(SemanticGraphEdge edge : dependencyParse.getOutEdgesSorted(word)){
                    if(nouns.containsKey(edge.getSource().word())){
                        if(edge.getRelation().toString().equals("amod") || edge.getRelation().toString().equals("advmod")){
                            List<String> nounPhrases = nouns.get(edge.getSource().word());
                            nounPhrases.add(edge.getTarget().toString());
                            nouns.put(edge.getSource().word(),nounPhrases);
                        }
                    }

                    if(verbs.contains(edge.getSource().word()) && nouns.containsKey(edge.getTarget().word())){
                        if(edge.getRelation().toString().contains("nsubj")){
                            List<String> nounPhrases = nouns.get(edge.getTarget().word());
                            nounPhrases.add(edge.getSource().toString());
                            nouns.put(edge.getTarget().word(),nounPhrases);
                        }
                    }
                }
            }
            
        }

        System.out.println("");

//        // kbp relations found in fifth sentence
//        List<RelationTriple> relations =
//                document.sentences().get(4).relations();
//        System.out.println("Example: relation");
//        System.out.println(relations.get(0));
//        System.out.println();
//
//        // entity mentions in the second sentence
//        List<CoreEntityMention> entityMentions = sentence.entityMentions();
//        System.out.println("Example: entity mentions");
//        System.out.println(entityMentions);
//        System.out.println();
//
//        // coreference between entity mentions
//        CoreEntityMention originalEntityMention = document.sentences().get(3).entityMentions().get(1);
//        System.out.println("Example: original entity mention");
//        System.out.println(originalEntityMention);
//        System.out.println("Example: canonical entity mention");
//        System.out.println(originalEntityMention.canonicalEntityMention().get());
//        System.out.println();
//
//        // get document wide coref info
//        Map<Integer, CorefChain> corefChains = document.corefChains();
//        System.out.println("Example: coref chains for document");
//        System.out.println(corefChains);
//        System.out.println();
//
//        // get quotes in document
//        List<CoreQuote> quotes = document.quotes();
//        CoreQuote quote = quotes.get(0);
//        System.out.println("Example: quote");
//        System.out.println(quote);
//        System.out.println();
//
//        // original speaker of quote
//        // note that quote.speaker() returns an Optional
//        System.out.println("Example: original speaker of quote");
//        System.out.println(quote.speaker().get());
//        System.out.println();
//
//        // canonical speaker of quote
//        System.out.println("Example: canonical speaker of quote");
//        System.out.println(quote.canonicalSpeaker().get());
//        System.out.println();

    }
}
