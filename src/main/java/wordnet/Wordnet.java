package wordnet;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import preprocess.Analyzers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wordnet
 * https://wordnet.princeton.edu/download/current-version
 */
public class Wordnet {
    Properties props;
    StanfordCoreNLP pipeline;
    public Wordnet() {
        props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref");
        // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
        props.setProperty("coref.algorithm", "neural");
        // build pipeline
        pipeline = new StanfordCoreNLP(props);
    }
    public void process2() throws IOException, ParseException {
        Analyzer wordnetAnalyzer = Analyzers.analyzerDefaultWordNet();
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
        int count = 1;

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("wordnet.txt"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                String text = currentRow.getCell(1).getStringCellValue();
                if (StringUtils.isNullOrEmpty(text)) continue;

                CoreDocument document = new CoreDocument(text);
                pipeline.annotate(document);
                bw.write(String.valueOf(count++));
                bw.newLine();
                for (CoreSentence sentence : document.sentences()) {
                    Map<Integer, List<String>> tokensMap = Analyzers.getAnalyzedTokensWithSynonym(sentence.text(), wordnetAnalyzer);


                    tokensMap.entrySet().forEach(e -> {
                        try {
                            bw.write(e.getValue() + " ");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                    bw.newLine();
                }

            }
        }



    }

    public void process() throws IOException, ParseException {
        Analyzer wordnetAnalyzer = Analyzers.analyzerDefaultWordNet();
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
        int count = 1;


        HashMap<String,Integer> tokensAndFreqs = new HashMap<>();
             while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                String text = currentRow.getCell(1).getStringCellValue();
                if (StringUtils.isNullOrEmpty(text)) continue;

                CoreDocument document = new CoreDocument(text);
                pipeline.annotate(document);
                for (CoreSentence sentence : document.sentences()) {
                    for(String c : sentence.tokensAsStrings()){
                        tokensAndFreqs.compute(c, (k,v)->  v == null ? 1 : v + 1);
                    }
                }

            }



        sheet = workbook.getSheet("Raw");
        iterator = sheet.iterator();
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("wordnet.txt"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                String text = currentRow.getCell(1).getStringCellValue();
                if (StringUtils.isNullOrEmpty(text)) continue;

                CoreDocument document = new CoreDocument(text);
                pipeline.annotate(document);
                bw.write(String.valueOf(count++));
                bw.newLine();
                for (CoreSentence sentence : document.sentences()) {
                    Map<Integer, List<String>> tokensMap = Analyzers.getAnalyzedTokensWithSynonym(sentence.text(), wordnetAnalyzer);
                    LinkedList<String> modifedSentence = new LinkedList<>();

                    tokensMap.entrySet().forEach(e -> {
                        String token="";
                        Integer tCount = 0;
                        for(String t : e.getValue()){
                            if (tokensAndFreqs.containsKey(t))
                                if(tokensAndFreqs.get(t)>tCount) {
                                    token = t;
                                    tCount = tokensAndFreqs.get(t);
                                }

                        }
                        modifedSentence.add(token);
                    });

                    modifedSentence.forEach(e -> {
                        try {
                            bw.write(e + " ");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
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


    public static void main(String[] args) throws IOException, ParseException {
        new Wordnet().process2();
    }
}
