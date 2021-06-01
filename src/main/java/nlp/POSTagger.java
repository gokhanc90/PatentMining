package nlp;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import org.apache.commons.text.similarity.CosineDistance;
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
//        NLPinit();
    }

    private void NLPinit(){
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
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(new File("Reviews_short_short.xlsx")));
        XSSFSheet sheet = workbook.getSheet("Raw");
        Iterator<Row> iterator = sheet.iterator();

        HashMap<String,List<String>> tags = new HashMap<>();
        Map<String,Map<String,Integer>> nouns = new LinkedHashMap<>();
        List<String> verbs = new ArrayList<>();
        List<String> nounphrasess = new ArrayList<>();
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
                        nouns.put(sentence.tokens().get(i).lemma(), new LinkedHashMap<>());
                    if (postag.startsWith("VB") && (!stops.contains(sentence.tokens().get(i).word()) || !stops.contains(sentence.tokens().get(i).lemma())))
                        verbs.add(sentence.tokens().get(i).lemma());
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
                    if(!(word.tag().startsWith("NN") || word.tag().startsWith("VB"))) continue;
                    for(SemanticGraphEdge edge : dependencyParse.getOutEdgesSorted(word)){
                        if(nouns.containsKey(edge.getSource().lemma())){
                            if(edge.getRelation().toString().equals("amod") || edge.getRelation().toString().equals("advmod") || edge.getRelation().toString().equals("compound")){
                                Map<String,Integer> nounPhrases = nouns.get(edge.getSource().lemma());
                                if(nounPhrases.get(edge.getTarget().lemma()) == null)
                                    nounPhrases.put(edge.getTarget().lemma()+"/ADJ",1);
                                else
                                    nounPhrases.put(edge.getTarget().lemma()+"/ADJ",nounPhrases.get(edge.getTarget().lemma())+1);
                                nouns.put(edge.getSource().lemma(),nounPhrases);
                            }
                        }

                        if(verbs.contains(edge.getSource().lemma()) && nouns.containsKey(edge.getTarget().lemma())){
                            if(edge.getRelation().toString().contains("nsubj") || edge.getRelation().toString().contains("obj")){
                                Map<String,Integer> nounPhrases = nouns.get(edge.getTarget().lemma());
                                if(nounPhrases.get(edge.getSource().lemma()) == null)
                                    nounPhrases.put(edge.getSource().lemma()+"/VB",1);
                                else
                                    nounPhrases.put(edge.getSource().lemma()+"/VB",nounPhrases.get(edge.getSource().lemma())+1);
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

//        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("outRaw.txt"), StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
//            for (Map.Entry<String, Integer> entry : sortedByFreq.entrySet()) {
//
//                System.out.println("*******************************************************************");
//                System.out.println(entry.getKey() + " - " + entry.getValue());
//                bw.write("*******************************************************************");
//                bw.write(entry.getKey() + " - " + entry.getValue());
//                bw.newLine();
//
//                Map<String,Integer> nounRef = nouns.get(entry.getKey());
//
//                if(nounRef==null) continue;
//
//                Map<String,Integer> sorted =
//                        nounRef.entrySet().stream()
//                                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
//                                .collect(Collectors.toMap(
//                                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));;
//
//
//
//                for(Map.Entry<String,Integer> enttry : sorted.entrySet()){
//                    System.out.println(enttry.getKey() + " - " + enttry.getValue());
//                    bw.write(enttry.getKey() + " - " + enttry.getValue());
//                    bw.newLine();
//                }
//            }
//        }


//        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("outRaw.txt"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
//
//            for (Map.Entry<String, Integer> entry : sortedByFreq.entrySet()) {
//                if (!nouns.containsKey(entry.getKey())) continue;
//                System.out.println("**********************************************************************");
//                System.out.println(entry.getKey() + " - " + entry.getValue());
//                Set<String> nounRef = nouns.get(entry.getKey());
////                nounRef.forEach(System.out::println);
//
//                bw.write("**********************************************************************");
//                bw.newLine();
//                bw.write(entry.getKey() + " - " + entry.getValue());
//                bw.newLine();
//
//                for(String nnn : nounRef){
//                    bw.write(nnn);
//                    bw.newLine();
//                }
//            }
//        }


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

    private void deneme(String excel, String sheetName, String output) throws IOException {
//        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(new File("Reviews.xlsx")));
//        XSSFSheet sheet = workbook.getSheet("Raw");
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(new File(excel)));
        XSSFSheet sheet = workbook.getSheet(sheetName);
        Iterator<Row> iterator = sheet.iterator();

        List<Noun> nouns = new ArrayList<>();
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
        int revNum=1;
        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            String text = currentRow.getCell(0).getStringCellValue();
            if(StringUtils.isNullOrEmpty(text)) continue;
            text = text.replaceAll("[^a-zA-Z0-9.' ]", "").toLowerCase();
            text = text.replaceAll("ı", "i").toLowerCase();
            CoreDocument document = new CoreDocument(text);
            pipeline.annotate(document);
            int sentenceNum=1;
            for (CoreSentence sentence : document.sentences()) {
                List<String> posTags = sentence.posTags();
                for (int i = 0; i < posTags.size(); i++) {
                    String postag = posTags.get(i);
                    if (postag.startsWith("NN") && (!stops.contains(sentence.tokens().get(i).word()) || !stops.contains(sentence.tokens().get(i).lemma()))){
                        String nounWord = sentence.tokens().get(i).lemma();
                        Noun noun = nouns.stream().filter(nnn -> nounWord.equals(nnn.getNounText())).findFirst().orElse(null);
                        if(noun==null){
                            ArrayList<String> revSent = new ArrayList<>();
                            revSent.add(revNum + "," + sentenceNum);
                            noun = new Noun();
                            noun.setNounText(nounWord);
                            noun.setFreq(1);
                            noun.setReviewSentenceIndex(revSent);
                            noun.setAdjectives(new LinkedHashMap<String,Integer>());
                            noun.setVerbs(new LinkedHashMap<String,Integer>());
                            noun.setCompounds(new LinkedHashMap<String,Integer>());
                        }else{
                            nouns.remove(noun);
                            List<String> revSent = noun.getReviewSentenceIndex();
                            revSent.add(revNum + "," + sentenceNum);
                            noun.setFreq(noun.getFreq()+1);
                            noun.setReviewSentenceIndex(revSent);
                        }
                        nouns.add(noun);
                    }
                    if (postag.startsWith("VB") && (!stops.contains(sentence.tokens().get(i).word()) || !stops.contains(sentence.tokens().get(i).lemma())))
                        verbs.add(sentence.tokens().get(i).lemma());
                }
                sentenceNum++;
            }

            revNum++;
            System.out.println(++count);
        }

        count=0;
        iterator = sheet.iterator();
        while (iterator.hasNext()) {
            System.out.println(++count);
            Row currentRow = iterator.next();
            String text = currentRow.getCell(0).getStringCellValue();
            if(StringUtils.isNullOrEmpty(text)) continue;
            text = text.replaceAll("[^a-zA-Z0-9/.' ]", "").toLowerCase();
            text = text.replaceAll("ı", "i").toLowerCase();

            CoreDocument document = new CoreDocument(text);
            pipeline.annotate(document);

            for(CoreSentence sentence : document.sentences()) {
                SemanticGraph dependencyParse = sentence.dependencyParse();

//                boolean sentPos = true;
//                Tree tree = sentence.coreMap().get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
//                if(tree!=null) {
//                    int mainSentiment = RNNCoreAnnotations.getPredictedClass(tree);
//                    if (mainSentiment == 0 || mainSentiment == 1) sentPos = false;
//                }

                for(IndexedWord word : dependencyParse.vertexListSorted()){
                    if(word.tag().startsWith("NN")){
                        Noun noun = nouns.stream().filter(nnn -> word.lemma().equals(nnn.getNounText())).findFirst().orElse(null);
                        if(noun==null) continue;
                        for (SemanticGraphEdge edge : dependencyParse.getOutEdgesSorted(word)) {
                            if (edge.getRelation().toString().equals("compound")) {
                                Map<String,Integer> compounds = noun.getCompounds();
                                compounds.put(edge.getTarget().lemma() + " " + edge.getSource().lemma(),compounds.getOrDefault(edge.getTarget().lemma() + " " + edge.getSource().lemma(), 0)+1);
                                noun.setCompounds(compounds);
                            }
                            if(edge.getRelation().toString().equals("amod") || edge.getRelation().toString().equals("advmod")){
                                if(stops.contains(edge.getTarget().lemma()))
                                    continue;
                                if(!edge.getTarget().toString().split("/")[1].startsWith("JJ"))
                                    continue;
                                Map<String,Integer> adjectives = noun.getAdjectives();
                                adjectives.put(edge.getTarget().lemma(),adjectives.getOrDefault(edge.getTarget().lemma(), 0)+1);
                                noun.setAdjectives(adjectives);
                            }

                            if(edge.getRelation().toString().equals("nn") || edge.getRelation().toString().equals("npadvmod")
                                    || edge.getRelation().toString().equals("nsubj")){
                                if(stops.contains(edge.getTarget().lemma()))
                                    continue;
                                if(!edge.getTarget().toString().split("/")[1].startsWith("JJ"))
                                    continue;
                                Map<String,Integer> adjectives = noun.getAdjectives();
                                adjectives.put(edge.getTarget().lemma(),adjectives.getOrDefault(edge.getTarget().lemma(), 0)+1);
                                noun.setAdjectives(adjectives);
                            }
                        }

                        for (SemanticGraphEdge edge : dependencyParse.getIncomingEdgesSorted(word)) {
                            if(edge.getRelation().toString().equals("nsubj") || edge.getRelation().toString().contains("obj")){
                                if(!verbs.contains(edge.getSource().lemma())) continue;
                                String verb = edge.getSource().lemma();
                                for (SemanticGraphEdge edge2 : dependencyParse.getOutEdgesSorted(edge.getSource())) {
                                    if(edge2.getRelation().toString().equals("dep") || edge2.getRelation().toString().contains("comp")
                                            || edge2.getRelation().toString().equals("dobj") || edge2.getRelation().toString().equals("advmod")
                                            || edge2.getRelation().toString().equals("prt")|| edge2.getRelation().toString().equals("tmod")
                                            || edge2.getRelation().toString().equals("vmod")){
                                        verb += " " + edge2.getTarget().lemma();
                                    }
                                }

                                for (SemanticGraphEdge edge3 : dependencyParse.getIncomingEdgesSorted(edge.getSource())) {
                                    if(edge3.getRelation().toString().equals("dep") || edge3.getRelation().toString().equals("acomp")
                                            || edge3.getRelation().toString().equals("dobj") || edge3.getRelation().toString().equals("advmod")
                                            || edge3.getRelation().toString().equals("prt")|| edge3.getRelation().toString().equals("tmod")
                                            || edge3.getRelation().toString().equals("vmod") || edge3.getRelation().toString().equals("xcomp")){
                                        verb += " " + edge3.getSource().lemma();
                                    }
                                }
                                Map<String,Integer> nounVerbs = noun.getVerbs();
                                nounVerbs.put(verb,nounVerbs.getOrDefault(verb, 0)+1);
                                noun.setVerbs(nounVerbs);
                            }
                        }
                    }
                }

            }
        }

        Collections.sort(nouns, Comparator.comparing(Noun::getFreq).reversed());

        for(Noun noun : nouns){
            noun.setCompounds(noun.getCompounds().entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
            noun.setAdjectives(noun.getAdjectives().entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
            noun.setVerbs(noun.getVerbs().entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
        }

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(output), StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {

            for(Noun noun : nouns){
                Map<String,Integer> compounds = noun.getCompounds();
                Map<String,Integer> adjectives = noun.getAdjectives();
                Map<String,Integer> nounVerbs = noun.getVerbs();

                bw.write("********************* " + noun.getNounText() + " ("+ noun.getFreq() + ")" +" **********************************");
                bw.newLine();
                bw.write("*********** Compunds of " + noun.getNounText());
                bw.newLine();
                for(Map.Entry<String,Integer> entry : compounds.entrySet())
                    bw.write(entry.getKey()+"("+entry.getValue()+") ");

                bw.newLine();
                bw.write("*********** Adjectives of " + noun.getNounText());
                bw.newLine();
                for(Map.Entry<String,Integer> entry : adjectives.entrySet())
                    bw.write(entry.getKey()+"("+entry.getValue()+") ");

                bw.newLine();
                bw.write("*********** Verbs of " + noun.getNounText());
                bw.newLine();
                for(Map.Entry<String,Integer> entry : nounVerbs.entrySet())
                    bw.write(entry.getKey()+"("+entry.getValue()+") ");
                bw.newLine();
            }
        }
    }

    private void similarityDeneme(String inputFile, String reviewsFile) throws IOException {

        Map<String,String> termKeyword = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String term = "";
            String line;
            boolean change = false;
            String keywords = "";
            while ((line = br.readLine()) != null) {
                if(StringUtils.isNullOrEmpty(line)){
                    change = false;
                    termKeyword.put(term,keywords.trim());
                    term="";
                    keywords="";
                    continue;
                }
                if(line.startsWith("*********************")){
                    change = false;
                    term = line.split("\\s+")[1];
                    continue;
                }
                if(line.startsWith("*********** Compunds of") || line.startsWith("*********** Adjectives of") || line.startsWith("*********** Verbs of")){
                    change=true;
                    continue;
                }
                if(change){
                    String[] tokens = line.split("\\)");
                    for(String token : tokens){
                        token=token.trim();
                        if(token.length()==0) continue;
                        keywords += token.substring(0,token.indexOf("(")) + " ";
                    }
                }


            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

        for(Map.Entry<String,String> termK : termKeyword.entrySet()) {

            Map<String, Double> sims = new LinkedHashMap<>();
            Map<String, String> raws = new LinkedHashMap<>();
            Map<String,Double> rawsSorted = new LinkedHashMap<>();
            String str = termK.getValue();
            XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(new File(reviewsFile)));
            XSSFSheet sheet = workbook.getSheet("KStemSentenceStopWordRemoval");
            Iterator<Row> iterator = sheet.iterator();
            int rowno = 0;
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                String text = currentRow.getCell(0).getStringCellValue();
                text = text.replaceAll("[^a-zA-Z0-9.' ]", "").toLowerCase();
                text = text.replaceAll("ı", "i").toLowerCase();
                double score = cosSim(str, text);
                sims.put(rowno + " - " + text, score);
                rowno++;
            }

            XSSFSheet sheet2 = workbook.getSheet("RawSentence");
            Iterator<Row> iterator2 = sheet2.iterator();
            rowno = 0;
            while (iterator2.hasNext()) {
                Row currentRow = iterator2.next();
                String text = currentRow.getCell(0).getStringCellValue();
                raws.put(rowno + "", text);
                rowno++;
            }

            Map<String, Double> sorted =
                    sims.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            for (Map.Entry<String, Double> sortt : sorted.entrySet()) {
                String row = sortt.getKey().substring(0, sortt.getKey().indexOf("-")).trim();
                rawsSorted.put(raws.get(row),sortt.getValue());
            }

            System.out.println("********* " + termK.getKey() + " *********");
            rawsSorted.entrySet()
                    .stream()
                    .limit(20)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (v1,v2) -> v1, LinkedHashMap::new))
                    .forEach((k,v)->System.out.println(String. format("%.2f",v) + " - " + k));
        }
    }

    private double cosSim(String str1, String str2){
        if(str1.length()==0 || str2.length()==0) return 0;
        double score =0.0;
        try{
            score = 1-(new CosineDistance().apply(str1,str2));
        }catch (IllegalArgumentException ex){
        }
        return score;
    }

    private void filterByMedian(String file){
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {


            String term = "";

            String line;
            Set<Integer> freqs = new LinkedHashSet<>();
            boolean change = false;
            while ((line = br.readLine()) != null) {

                if(StringUtils.isNullOrEmpty(line)){
                    change = false;
                    System.out.println(line);
                    continue;
                }
                if(line.startsWith("*********************")){
                    change = false;
                    term = line.split("\\s+")[1];
                    System.out.println(line);
                    continue;
                }

                if(line.startsWith("*********** Compunds of") || line.startsWith("*********** Adjectives of") || line.startsWith("*********** Verbs of")){
                    change=true;
                    System.out.println(line);
                    continue;
                }

                if(change){
                    String newLine = "";
                    freqs = new LinkedHashSet<>();
                    line = line.replaceAll(" "+term,"");
                    String[] tokens = line.split("\\)");
                    for(String token : tokens){
                        token=token.trim();
                        if(token.length()==0) continue;
                        freqs.add(Integer.parseInt(token.substring(token.indexOf("(")+1,token.length())));
                    }


                    int median = Iterables.get(freqs, (freqs.size() - 1) / 2);

                    for(String token : tokens){
                        token=token.trim();
                        if(token.length()==0) continue;
                        if(Integer.parseInt(token.substring(token.indexOf("(")+1,token.length()))>=median)
                            newLine += token + ") ";
                    }

                    System.out.println(newLine);
                }


            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {
//        new POSTagger().process2();
//        new POSTagger().deneme("Reviews_short_short.xlsx","Raw","outRaw.txt");
//        new POSTagger().deneme("Reviews.xlsx","PorterStopWordRemoval","outPorterStopWordRemoval.txt");
        new POSTagger().similarityDeneme("outKStemStopWordRemovalByTopics_Filtered.txt", "Reviews.xlsx");

//        new POSTagger().filterByMedian("G:\\Google Drive\\patent madenciliği\\Patent Mining Files\\outPorterStopWordRemovalByTopics_Filtered.txt");
    }

    private class Noun{

        String nounText;
        Map<String,Integer> compounds;
        Map<String,Integer> adjectives;
        Map<String,Integer> verbs;
        List<String> reviewSentenceIndex;
        int freq;

        public Noun(){
        }

        public Noun(String nounText, Map<String,Integer> compounds, Map<String,Integer> adjectives, Map<String,Integer> verbs, List<String> reviewSentenceIndex, int freq) {
            this.compounds = compounds;
            this.adjectives = adjectives;
            this.verbs = verbs;
            this.reviewSentenceIndex = reviewSentenceIndex;
            this.freq = freq;
        }

        public String getNounText() {
            return nounText;
        }

        public void setNounText(String nounText) {
            this.nounText = nounText;
        }

        public Map<String,Integer> getCompounds() {
            return compounds;
        }

        public void setCompounds(Map<String,Integer> compounds) {
            this.compounds = compounds;
        }

        public Map<String,Integer> getAdjectives() {
            return adjectives;
        }

        public void setAdjectives(Map<String,Integer> adjectives) {
            this.adjectives = adjectives;
        }

        public Map<String,Integer> getVerbs() {
            return verbs;
        }

        public void setVerbs(Map<String,Integer> verbs) {
            this.verbs = verbs;
        }

        public List<String> getReviewSentenceIndex() {
            return reviewSentenceIndex;
        }

        public void setReviewSentenceIndex(List<String> reviewSentenceIndex) {
            this.reviewSentenceIndex = reviewSentenceIndex;
        }

        public int getFreq() {
            return freq;
        }

        public void setFreq(int freq) {
            this.freq = freq;
        }
    }
}
