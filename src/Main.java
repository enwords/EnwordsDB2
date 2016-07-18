import java.io.*;
import java.util.*;

public class Main {

    //    public static String[] languages = {"eng", "rus", "deu", "spa", "jpn", "cmn", "ara"};
    public static String[] languages = {"eng", "rus"};
    public static String[] unusualLanguages = {"jpn", "cmn", "ara"};
    public static Set<String> unusualLanguagesSet = new HashSet<>(Arrays.asList(unusualLanguages));

    private static int superIdCounter = 1;
    private static int superIdCounterLimit = 50000;


    private static String learningLang;


    private static String devDir = "src/development/files/";
    private static String dataDir = devDir + "seeds_data/";
    private static String wordsDir = dataDir + "words/";


    public static Map<String, LinkedHashMap<String, String>> mapAllLangSen;
    public static Map<String, TreeMap<Integer, String>> mapAllLangWord;
    public static Map<String, String> allSentences;
    public static Map<Integer, Set<String>> word_sentencesLinksMap;


    public static Set<String> setAudio;
    public static Set<String> allSenIdsSet;
    public static Set<String> originalLinksSet;


    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.start();

    }

    private void start() throws IOException {
        System.out.println("wait few minutes");
        makeDir();

        long startTime, endTime;
        startTime = System.nanoTime();


        initialize();

        endTime = System.nanoTime();
        System.out.println("init is done  " + ((endTime - startTime) / 1000000000L) + " sec");


        startTime = System.nanoTime();
        fillSentenceMap();
        endTime = System.nanoTime();
        System.out.println("fill is done  " + ((endTime - startTime) / 1000000000L) + " sec");

        startTime = System.nanoTime();

        wordCount();

        endTime = System.nanoTime();
        System.out.println("wordcount is done  " + ((endTime - startTime) / 1000000000L) + " sec");


        startTime = System.nanoTime();

        createWordSentencesLinks();
        endTime = System.nanoTime();
        System.out.println("word_sen link is done  " + ((endTime - startTime) / 1000000000L) + " sec");


        startTime = System.nanoTime();

        writeToFiles();

        endTime = System.nanoTime();
        System.out.println("write is done  " + ((endTime - startTime) / 1000000000L) + " sec");


    }

    private void writeToFiles() throws IOException {
        List<String> linksList = fileToList(new File("src/development/files/links.csv"));


        writeWordSentencesLinks(new File(dataDir + "word_sentence.tsv"));
        writeLinks(linksList, new File(dataDir + "links.tsv"));
        writeSentences(new File(dataDir + "sentences.tsv"));
        writeWords(new File(dataDir + "words.tsv"));
        writeAudio(new File(dataDir + "audio.tsv"));


    }

    private void writeAudio(File file) throws FileNotFoundException {
        try (PrintWriter printWriter = new PrintWriter(file)) {
            for (String s : setAudio) {
                if (allSenIdsSet.contains(s)) {
                    printWriter.println(s);
                }
            }
        }
    }

    private void writeWords(File file) throws FileNotFoundException {
        try (PrintWriter printWriter = new PrintWriter(file)) {
            for (Map.Entry<String, TreeMap<Integer, String>> lang_words : mapAllLangWord.entrySet()) {
                String lang = lang_words.getKey();
                Map<Integer, String> ids_words = lang_words.getValue();
                for (Map.Entry<Integer, String> wrds : ids_words.entrySet()) {
                    int id = wrds.getKey();
                    String word = wrds.getValue();
                    printWriter.println(id + "\t" + lang + "\t" + word);
                }
            }
        }
    }

    private void writeSentences(File file) throws FileNotFoundException {
        try (PrintWriter printWriter = new PrintWriter(file)) {
            for (Map.Entry<String, LinkedHashMap<String, String>> lang_sentences : mapAllLangSen.entrySet()) {
                String lang = lang_sentences.getKey();
                Map<String, String> ids_sentences = lang_sentences.getValue();
                for (Map.Entry<String, String> sen : ids_sentences.entrySet()) {
                    String id = sen.getKey();
                    String line = sen.getValue();
                    if (allSenIdsSet.contains(id)) {
                        printWriter.println(id + "\t" + lang + "\t" + convertStringForTSV(line));
                    }
                }
            }
        }
    }

    private void writeLinks(List<String> linksList, File outFile) throws IOException {
        try (PrintWriter printWriter = new PrintWriter(outFile)) {

            for (String line : linksList) {
                String[] arr = parseLineLight(line);
                if (allSenIdsSet.contains(arr[0]) && allSenIdsSet.contains(arr[1])) {
                    printWriter.println(line);
                }
            }
        }
    }

    private void writeWordSentencesLinks(File file) throws FileNotFoundException {
        try (PrintWriter printWriter = new PrintWriter(file)) {

            for (Map.Entry<Integer, Set<String>> pair : word_sentencesLinksMap.entrySet()) {
                int wordId = pair.getKey();

                Set<String> senIds = pair.getValue();

                for (String senId : senIds) {
                    printWriter.println(wordId + "\t" + senId);
                }
            }
        }
    }

    private void createWordSentencesLinks() {
        for (Map.Entry<String, TreeMap<Integer, String>> lang_wordMap : mapAllLangWord.entrySet()) {
            String currentLang = lang_wordMap.getKey();


            Map<Integer, String> wordMap = lang_wordMap.getValue();
            Map<String, Integer> swappedWordMap = reverse(wordMap);
            int wordId;
            String sentenceId;

            if (unusualLanguagesSet.contains(currentLang)) {
                for (Map.Entry<String, Integer> pair : swappedWordMap.entrySet()) {
                    String word = pair.getKey();
                    wordId = pair.getValue();
                    for (Map.Entry<String, String> senMap : mapAllLangSen.get(currentLang).entrySet()) {
                        sentenceId = senMap.getKey();
                        String sentence = senMap.getValue();
                        if (sentence.contains(word)) {
                            addToWordSentencesLinksMap(wordId, sentenceId);
                        }
                    }
                }
            } else {
                for (Map.Entry<String, String> senMap : mapAllLangSen.get(currentLang).entrySet()) {
                    sentenceId = senMap.getValue();
                    String sentence = senMap.getKey();

                    String[] words = removePunctuationAndDigits(sentence).split(" ");
                    for (String word : words) {
                        wordId = swappedWordMap.get(word);
                        if (swappedWordMap.containsKey(word)) {
                            addToWordSentencesLinksMap(wordId, sentenceId);
                        }
                    }
                }
            }
        }
    }

    private void addToWordSentencesLinksMap(int wordId, String sentenceId) {
        Set<String> sentencesIdsForOneWord;
        try {
            sentencesIdsForOneWord = word_sentencesLinksMap.get(wordId);
            if (sentencesIdsForOneWord.size() <= 100) {
                sentencesIdsForOneWord.add(sentenceId);
                allSenIdsSet.add(sentenceId);
                word_sentencesLinksMap.put(wordId, sentencesIdsForOneWord);
            }
        } catch (Exception e) {
            sentencesIdsForOneWord = new LinkedHashSet<>();
            sentencesIdsForOneWord.add(sentenceId);
            allSenIdsSet.add(sentenceId);
            word_sentencesLinksMap.put(wordId, sentencesIdsForOneWord);
        }
    }


    private <K, V> Map<V, K> reverse(Map<K, V> map) {
        Map<V, K> rev = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet())
            rev.put(entry.getValue(), entry.getKey());
        return rev;
    }


    private void wordCount() throws IOException {
        List<String> wordCountList;
        for (Map.Entry<String, LinkedHashMap<String, String>> pair : mapAllLangSen.entrySet()) {
            String lang = pair.getKey();
            learningLang = lang;
            Map<String, String> allSentencesOfCurrentLang = pair.getValue();

            File file = new File(wordsDir + lang + ".txt");
            if (file.exists() && !file.isDirectory()) {
                wordCountList = fileToList(file);
            } else {
                wordCountList = countingWords(allSentencesOfCurrentLang);
            }


            Map<Integer, String> map = mapAllLangWord.get(lang);
            map.putAll(setIdToWords(wordCountList));


        }

    }

    private Map<Integer, String> setIdToWords(List<String> wordList) {
        Map<Integer, String> res = new TreeMap<>();
        for (String line : wordList) {

            String word = line.split(" ")[0];
            res.put(superIdCounter, word);
            superIdCounter++;
        }
        while ((superIdCounter - 1) % superIdCounterLimit != 0) {
            superIdCounter++;
        }
        return res;

    }

    public List<String> fileToList(File file) throws IOException {
        List<String> result = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            String line;
            while (fileReader.ready()) {
                line = fileReader.readLine();
                result.add(line);
            }
        }
        return result;
    }

    private List<String> countingWords(Map<String, String> allSentencesOfCurrentLang) {
        Map<String, Integer> wordCountMap = new HashMap<>();
        for (Map.Entry<String, String> pair : allSentencesOfCurrentLang.entrySet()) {
            String id = pair.getKey();
            String sentence = pair.getValue();


            String[] words = removePunctuationAndDigits(sentence).split(" ");
            Set<String> set = conditions(words);

            for (String word : set) {
                if (wordCountMap.containsKey(word)) {
                    wordCountMap.put(word, wordCountMap.get(word) + 1);

                } else {
                    wordCountMap.put(word, 1);
                }
            }
        }

        List list2 = new ArrayList(wordCountMap.entrySet());

        Collections.sort(list2, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> a, Map.Entry<Integer, Integer> b) {
                return a.getValue() - b.getValue();
            }
        });

        Collections.reverse(list2);


        List<String> res = new ArrayList<>();
        for (Object string : list2) {
            String[] arr = string.toString().replaceAll("[,\"]", "").split("=");

            String word = arr[0];
            Integer count = Integer.parseInt(arr[1]);

            if (count > 1) {
                res.add(word);
            }
        }
        return res;
    }

    public Set<String> conditions(String[] words) {
        Set<String> set = new HashSet<>();
        if ("eng".equals(learningLang)) {
            for (String word : words) {
                if (((word.length() > 1) || "i".equals(word)) && word.length() <= 20 && !word.startsWith("'")
                        && (!"tom".equals(word) && !"mary".equals(word) && !"tatoeba".equals(word) && !"th".equals(word)
                        && !"".equals(word) && !word.startsWith("tom'") && !word.startsWith("mary'"))) {

                    set.add(word);
                }
            }
        } else if (unusualLanguagesSet.contains(learningLang)) {
            for (String word : words) {
                if (word.length() <= 20 && (!"tom".equals(word) && !"mary".equals(word) && !"tatoeba".equals(word)
                        && !"".equals(word))) {

                    word = word.replaceAll("[а-яА-Яa-zA-Z]", "");
                    if (!"".equals(word)) set.add(word);
                }
            }

        } else {
            for (String word : words) {
                if (word.length() <= 20 && (!"tom".equals(word) && !"mary".equals(word)
                        && !"tatoeba".equals(word) && !"".equals(word))) {

                    set.add(word);
                }
            }
        }
        return set;
    }

    public String removePunctuationAndDigits(String word) {
        String res = word.replaceAll("[^'\\p{L}]+", " ").toLowerCase();
        if ("eng".equals(learningLang)) {
            String[] arr = res.split(" ");
            for (String s : arr) {
                if (s.endsWith("'s") && !s.equals("let's")) {
                    s = s.replace("'s", "");
                } else if (s.endsWith("s'")) {
                    s = s.replace("s'", "");
                } else if (s.endsWith("rs")) {
                    s = replaceLast(s, "rs", "r");
                }
                res += (" " + s);
            }
            return res;
        } else {
            return res.replaceAll("[']", " ");
        }
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }


    private void fillSentenceMap() {

        String id;
        String line;
        String lang;
        String sentence;
        LinkedHashMap<String, String> res;
        for (Map.Entry<String, String> pair : allSentences.entrySet()) {
            id = pair.getKey();
            line = pair.getValue();

            String arr[] = parseLineLight(line);
            lang = arr[1];
            sentence = arr[2];
            res = mapAllLangSen.get(lang);
            res.put(id, sentence);
            mapAllLangSen.put(lang, res);
        }
    }

    private void initialize() throws IOException {
        allSenIdsSet = new LinkedHashSet<>();
        word_sentencesLinksMap = new LinkedHashMap<>();
        mapAllLangSen = new LinkedHashMap<>();
        mapAllLangWord = new LinkedHashMap<>();
        for (String lang : languages) {
            mapAllLangSen.put(lang, new LinkedHashMap());
            mapAllLangWord.put(lang, new TreeMap<>());
        }
        setAudio = fileToSet(new File("src/development/files/sentences_with_audio.csv"));
        originalLinksSet = fileToSet(new File("src/development/files/links.csv"));
        allSentences = readSentencesFile(new File("src/development/files/sentences.csv"));
    }

    public String convertStringForTSV(String text) {
        if (text.contains("\"")) {
            text = "\"" + text.replaceAll("\"", "\"\"") + "\"";
        }
        return text;
    }

    private Map<String, String> readSentencesFile(File file) throws IOException {
        Map<String, String> withAudio = new LinkedHashMap<>();
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
                        withAudio.put(id, line);
                    } else withoutAudio.put(id, line);
                }

            }
        }
        withAudio.putAll(withoutAudio);
        return withAudio;
    }


    private void makeDir() {
        File dir1 = new File(wordsDir);
        dir1.mkdir();
    }

    public <T> Set<T> fileToSet(File file) throws IOException {
        Set<T> result = new HashSet<>();
        String line;
        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            while (fileReader.ready()) {
                line = fileReader.readLine();
                try {
                    String[] arr = parseLineLight(line);

                    result.add((T) arr[0]);
                    result.add((T) arr[1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    result.add((T) line);
                }


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
