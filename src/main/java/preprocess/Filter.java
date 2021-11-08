package preprocess;

import models.GibbsSamplingLDA;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import reviews.AmazonReviews;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Filter {
    public static List<String> modalsAndRequirements = Arrays.asList("can", "could", "may", "might", "will", "would", "shall", "should", "must",
            "cant", "couldnt", "wont","wouldnt","shant","shouldnt","require","advise","suggest","offer","propose","need","recommend",
            "below","follow","option","least","adequate","minimum","bug","fix","error","crash","allow","let","permit","responsible","applicable");

    public static List<String> modalsAndRequirementsStem = new LinkedList<>();

    public static void main(String[] args) throws Exception {
        for(String s: modalsAndRequirements){
            modalsAndRequirementsStem.add(Analyzers.getAnalyzedToken(s,Analyzers.analyzerPorterEng()));
        }


        XSSFWorkbook workbook = new XSSFWorkbook("Reviews.xlsx");

        Sheet sraw = workbook.getSheet("RawSentence");

        TreeMap<Integer,String> sentStem = new TreeMap<>();
        TreeMap<Integer,String> sentRaw = new TreeMap<>();


        for(int i=0; i<sraw.getLastRowNum()-1;i++){
            if (sraw.getRow(i)==null) continue;
            String s = sraw.getRow(i).getCell(0).getStringCellValue();
            if(s.trim().length()==0) continue;
            sentRaw.put(i+1,AmazonReviews.preprocess(s,Analyzers.analyzerDefaultWithStopWordRemoval()));
            sentStem.put(i+1, AmazonReviews.preprocess(s,Analyzers.analyzerPorterEngWithStopwordRemoval()));
        }

        TreeMap<Integer,String> filtered =  getSentWithModals(sentStem);
        System.out.println(filtered.size());

        Map<Integer,String> filteredRaw = sentRaw.entrySet().stream().filter(e->filtered.keySet().contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        TreeMap<Integer,String> filteredRawS = new TreeMap<>(filteredRaw);
        System.out.println(filteredRawS.size());


        try (BufferedWriter writer = new BufferedWriter(new FileWriter("FilteredPorterSentences.txt"))) {
            for (String s : filtered.values()) {
                writer.write(s);
                writer.newLine();
            }
        }catch (Exception e){

        }
        GibbsSamplingLDA lda = new GibbsSamplingLDA("FilteredPorterSentences.txt",
                10, 0.1, 0.01,
                2000, 10, "Model",
               "", 0);
        lda.inference();

    }

    public static TreeMap<Integer,String> getSentWithModals (TreeMap<Integer,String> sents){
        TreeMap<Integer,String> clone = (TreeMap<Integer, String>) sents.clone();
        for(Map.Entry<Integer,String> s: sents.entrySet()) {
            StringTokenizer tokens = new StringTokenizer(s.getValue());
            boolean found = false;
            while(tokens.hasMoreElements()){
                if(modalsAndRequirementsStem.contains(tokens.nextToken())){
                    found = true;
                    break;
                }
            }
            if(!found){
                clone.remove(s.getKey());
            }

        }
        return clone;
    }
}
