import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    public static String[] languages = {"eng", "rus", "deu", "spa", "jpn", "cmn", "ara"};
    public static String[] unusualLanguages = {"jpn", "cmn", "ara"};
    public static Set<String> unusualLanguagesSet = new HashSet<>(Arrays.asList(unusualLanguages));

    private static String learningLang;


    private static String devDir = "src/development/files/";
    private static String dataDir = devDir + "seeds_data/";
    private static String wordsDir = dataDir + "words/";


    public static Map<String, LinkedHashMap<String, String>> mapAllLangSen;
    public static Map<String, LinkedHashMap<String, String>> mapAllLangWord;
    public static Map<String, String> allSentences;
    public static Map<String, String> word_sentencesLinksMap;


    public static Set<String> setAudio;
    public static Set<String> allSenIdsSet;
    public static Set<String> originalLinksSet;


    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.start();

    }

    private void start() throws IOException {
        makeDir();


        initialize();
        fillSentenceMap();
        wordCount();


    }

    private void wordCount() {
        Map<String, Integer> wordCountMap;
        for (Map.Entry<String, LinkedHashMap<String, String>> pair : mapAllLangSen.entrySet()) {
            String lang = pair.getKey();
            learningLang = lang;
            Map<String, String> allSenOfCurrentLang = pair.getValue();

            File file = new File(wordsDir + lang + ".txt");
            if (file.exists() && !file.isDirectory()) {
                indexingWords();
            } else{
                wordCountMap =  countingWords(allSenOfCurrentLang);
                indexingWords();
            }

        }

    }

    private Map<String, Integer> countingWords(Map<String, String> allSenOfCurrentLang) {
        Map<String, Integer> res = new HashMap<>();
        for (Map.Entry<String, String> pair : allSenOfCurrentLang.entrySet()) {
            String id = pair.getKey();
            String line = pair.getValue();

            String[] arr = parseLineLight(line);
            String sentence = arr[2];

            String[] words = removePunctuationAndDigits(sentence).split(" ");

        }
    }


    public String removePunctuationAndDigits(String word) {
        String res1 = word.replaceAll("[^\\p{L}]+", " ").toLowerCase();
        String res2 = "";
        if ("eng".equals(learningLang)) {
            String[] arr = res1.split(" ");
            for (String s : arr) {
                if (s.endsWith("'s") && !s.equals("let's")) {
                    s = s.replace("'s", "");
                } else if (s.endsWith("s'")) {
                    s = s.replace("s'", "");
                } else if (s.endsWith("rs")) {
                    s = replaceLast(s, "rs", "r");
                }
                res2 += (" " + s);
            }
        } else {
            return res1;
        }
        return res2;
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }


    private void fillSentenceMap() {
        for (Map.Entry<String, String> pair : allSentences.entrySet()) {
            String id = pair.getKey();
            String line = pair.getValue();

            String arr[] = parseLineLight(line);
            String lang = arr[1];
            String sentence = arr[2];
            mapAllLangSen.get(lang).put(id, convertStringForTSV(sentence));
        }
    }

    private void initialize() throws IOException {
        for (String lang : languages) {
            mapAllLangSen = new LinkedHashMap<>();
            mapAllLangSen.put(lang, new LinkedHashMap());

            mapAllLangWord = new LinkedHashMap<>();
            mapAllLangSen.put(lang, new LinkedHashMap());
        }
        setAudio = fileToSet(new File("/audio.csv"), 1);
        originalLinksSet = fileToSet(new File("/links.csv"));
        allSentences = readSentencesFile(new File("/sentences.csv"));
    }

    public String convertStringForTSV(String text) {
        if (text.contains("\"")) {
            text = "\"" + text.replaceAll("\"", "\"\"") + "\"";
        }
        return text;
    }

    private Map<String, String> readSentencesFile(File file) throws IOException {
        Map<String, String> widthAudio = new LinkedHashMap<>();
        Map<String, String> withoutAudio = new LinkedHashMap<>();


        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            String line;
            String id;
            String lang;
            while (fileReader.ready()) {
                line = fileReader.readLine();
                String[] arr = parseLineLight(line);

                id = arr[0];
                lang = arr[1];
                if (originalLinksSet.contains(id) && mapAllLangSen.containsKey(lang)) {
                    if (setAudio.contains(id)) {
                        widthAudio.put(id, line);
                    } else withoutAudio.put(id, line);
                }

            }
        }
        widthAudio.putAll(withoutAudio);
        return widthAudio;
    }


    private void makeDir() {
        File dir1 = new File(wordsDir);
        dir1.mkdir();
    }

    public <T> Set<T> fileToSet(File file) throws IOException {
        Set<T> result = new HashSet<>();

        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            while (fileReader.ready()) {
                String[] arr = parseLineLight(fileReader.readLine());

                result.add((T) arr[0]);
                result.add((T) arr[1]);

            }
        }
        return result;
    }


    public <T> Set<T> fileToSet(File file, int param) throws IOException {
        Set<T> result = new HashSet<>();

        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            String line;
            while (fileReader.ready()) {
                line = fileReader.readLine();

                String[] arr = parseLineLight(line);

                result.add((T) arr[param]);

            }
        }
        return result;
    }

    public String[] parseLineLight(String line) {
        String[] list = line.split("\\t");
        return list;
    }
}
