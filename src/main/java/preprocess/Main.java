package preprocess;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.analysis.Analyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {

        PrintWriter writer = new PrintWriter("ReviewsKStem.txt");
            List<String> lines =Files.readAllLines(Paths.get("ReviewsCarrot2.txt"), StandardCharsets.UTF_8);
            for (String line:lines){
                List<String> tokens = Analyzers.getAnalyzedTokens(line,Analyzers.analyzerKStem());
                System.out.println(tokens);
                writer.println(tokens.stream().filter(t-> !NumberUtils.isCreatable(t)).filter(t-> !StringUtils.containsAny(t,"'’")).collect(Collectors.joining(" ")).trim());
            }
            writer.close();
        }
 //   }
}
