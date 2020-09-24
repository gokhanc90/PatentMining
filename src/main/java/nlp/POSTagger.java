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
import edu.stanford.nlp.util.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class POSTagger {
    Properties props;
    StanfordCoreNLP pipeline;
    public POSTagger() {
        props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref");
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

        HashMap<String,List<String>> tags = new HashMap<>();
        Map<String,Set<String>> nouns = new LinkedHashMap<>();
        List<String> verbs = new ArrayList<>();
        List<String> stops = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("stopwords.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                stops.add(line);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        int count = 0;
        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            String text = currentRow.getCell(0).getStringCellValue();
            if(StringUtils.isNullOrEmpty(text)) continue;
            text = text.replaceAll("[^a-zA-Z0-9.' ]", "").toLowerCase();
            text = text.replaceAll("ı", "i").toLowerCase();
            CoreDocument document = new CoreDocument(text);
            pipeline.annotate(document);

            for (CoreSentence sentence : document.sentences()) {
                List<String> posTags = sentence.posTags();
                for (int i = 0; i < posTags.size(); i++) {
                    String postag = posTags.get(i);
                    if (postag.startsWith("NN") && (!stops.contains(sentence.tokens().get(i).word()) || !stops.contains(sentence.tokens().get(i).lemma())))
                        nouns.put(sentence.tokens().get(i).lemma(), new HashSet<>());
                    if (postag.startsWith("VB") && (!stops.contains(sentence.tokens().get(i).word()) || !stops.contains(sentence.tokens().get(i).lemma())))
                        verbs.add(sentence.tokens().get(i).word());

                }
            }

            System.out.println(count++);
        }



        iterator = sheet.iterator();
        BufferedWriter writer = new BufferedWriter(new FileWriter("NLPReviews.txt"));
        int number = 1;
        while (iterator.hasNext()) {
            System.out.println(number);
            Row currentRow = iterator.next();
            String text = currentRow.getCell(0).getStringCellValue();
            if(StringUtils.isNullOrEmpty(text)) continue;
            text = text.replaceAll("[^a-zA-Z0-9/.' ]", "").toLowerCase();
            text = text.replaceAll("ı", "i").toLowerCase();

            CoreDocument document = new CoreDocument(text);
            pipeline.annotate(document);
            posTag(document,writer,number++,tags);

            for(CoreSentence sentence : document.sentences()) {
                SemanticGraph dependencyParse = sentence.dependencyParse();
                for(IndexedWord word : dependencyParse.vertexListSorted()){
                    for(SemanticGraphEdge edge : dependencyParse.getOutEdgesSorted(word)){
                        if(nouns.containsKey(edge.getSource().lemma())){
                            if(edge.getRelation().toString().equals("amod") || edge.getRelation().toString().equals("advmod")){
                                Set<String> nounPhrases = nouns.get(edge.getSource().lemma());
                                nounPhrases.add(edge.getTarget().toString());
                                nouns.put(edge.getSource().lemma(),nounPhrases);
                            }
                        }

                        if(verbs.contains(edge.getSource().word()) && nouns.containsKey(edge.getTarget().lemma())){
                            if(edge.getRelation().toString().contains("nsubj")){
                                Set<String> nounPhrases = nouns.get(edge.getTarget().lemma());
                                nounPhrases.add(edge.getSource().toString());
                                nouns.put(edge.getTarget().lemma(),nounPhrases);
                            }
                        }
                    }
                }

            }
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

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("outRaw.txt"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            for (Map.Entry<String, Integer> entry : sortedByFreq.entrySet()) {
                if (!nouns.containsKey(entry.getKey())) continue;
                System.out.println("**********************************************************************");
                System.out.println(entry.getKey() + " - " + entry.getValue());
                Set<String> nounRef = nouns.get(entry.getKey());
//                nounRef.forEach(System.out::println);

                bw.write("**********************************************************************");
                bw.newLine();
                bw.write(entry.getKey() + " - " + entry.getValue());
                bw.newLine();

                for(String nnn : nounRef){
                    bw.write(nnn);
                    bw.newLine();
                }
            }
        }


    }

    private void posTag(CoreDocument document,BufferedWriter writer,int reviewNumber, HashMap<String,List<String>> tags) throws IOException {
        // create a document object
        writer.write("R"+reviewNumber+":\t"+document.text());
        writer.newLine();

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
        new POSTagger().process2();
    }
}
