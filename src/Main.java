import java.io.*;
import java.util.*;

public class Main {

    private static String[] languages = {"eng", "epo", "tur", "ita", "rus", "deu", "fra", "spa", "por", "jpn", "hun", "heb", "ber", "pol", "mkd", "fin", "nld", "cmn", "mar", "ukr", "swe", "dan", "srp", "bul", "ces", "ina", "lat", "ara", "nds", "lit"};
    private static String[] unusualLanguages = {"jpn", "cmn"};
    private static Set<String> unusualLanguagesSet = new HashSet<>(Arrays.asList(unusualLanguages));

    private static int superIdCounter = 1;
    private static int superIdCounterLimit = 25000;
    private static int sentencesForOneWordLimit = 100;
    private static int maxSentenceLength = 150;


    private static String learningLang;


    private static String devDir = "src/development/files/";
    private static String dataDir = devDir + "seeds_data/";
    private static String wordsDir = dataDir + "words/";


    private static Map<String, LinkedHashMap<String, String>> mapAllLangSen;
    private static Map<String, TreeMap<Integer, String>> mapAllLangWord;
    private static Map<String, String> allSentences;
    private static Map<Integer, Set<String>> word_sentencesLinksMap;


    private static Set<Integer> setAudio;
    private static Set<String> allSenIdsSet;
    private static Set<String> originalLinksSet;


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
        writeWordSentencesLinks(new File(dataDir + "word_sentence.tsv"));
        writeSentences(new File(dataDir + "sentences.tsv"));
        writeWords(new File(dataDir + "words.tsv"));
        writeAudio(new File(dataDir + "audio.tsv"));

        resetStaticCollections();
        List<String> linksList = fileToList(new File("src/development/files/links.csv"));
        writeLinks(linksList, new File(dataDir + "links.tsv"));
    }

    private void writeAudio(File file) throws FileNotFoundException {
        try (PrintWriter printWriter = new PrintWriter(file)) {
            for (Integer s : setAudio) {
                if (allSenIdsSet.contains(s.toString())) {
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

                    if (word_sentencesLinksMap.containsKey(id)) {
                        printWriter.println(id + "\t" + lang + "\t" + word);
                    }
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
        StringTokenizer st;
        try (PrintWriter printWriter = new PrintWriter(outFile)) {
            for (String line : linksList) {
                st = new StringTokenizer(line, "\t");
                if (allSenIdsSet.contains(st.nextToken()) && allSenIdsSet.contains(st.nextToken())) {
                    printWriter.println(line);
                }
            }
        }
    }

    private void writeWordSentencesLinks(File file) throws FileNotFoundException {
        int wordId;
        Set<String> senIds;

        try (PrintWriter printWriter = new PrintWriter(file)) {
            for (Map.Entry<Integer, Set<String>> pair : word_sentencesLinksMap.entrySet()) {
                wordId = pair.getKey();
                senIds = pair.getValue();

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
                    int counter = sentencesForOneWordLimit;
                    for (Map.Entry<String, String> senMap : mapAllLangSen.get(currentLang).entrySet()) {
                        if (counter == 0) {
                            break;
                        } else {
                            sentenceId = senMap.getKey();
                            String sentence = senMap.getValue();
                            if (sentence.contains(word)) {
                                addToWordSentencesLinksMap(wordId, sentenceId);
                                counter--;
                            }
                        }
                    }
                }
            } else {

                StringTokenizer st;
                for (Map.Entry<String, String> senMap : mapAllLangSen.get(currentLang).entrySet()) {
                    sentenceId = senMap.getKey();
                    String sentence = senMap.getValue();

                    st = new StringTokenizer(removePunctuationAndDigits(sentence));

                    while (st.hasMoreTokens()) {
                        try {
                            wordId = swappedWordMap.get(st.nextToken());
                            addToWordSentencesLinksMap(wordId, sentenceId);
                        } catch (Exception e) {

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
            if (sentencesIdsForOneWord.size() <= sentencesForOneWordLimit) {
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
                wordCountList = fileWordsToList(file);
            } else {
                wordCountList = countingWords(allSentencesOfCurrentLang);
            }

            Map<Integer, String> map = mapAllLangWord.get(lang);
            map.putAll(setIdToWords(wordCountList));
        }

    }

    private Map<Integer, String> setIdToWords(List<String> wordList) {
        Map<Integer, String> res = new TreeMap<>();

        int count = superIdCounterLimit;
        for (String line : wordList) {

            String word = line.split(" ")[0];
            res.put(superIdCounter, word);
            superIdCounter++;
            count--;
            if (count == 0) break;

        }
        while ((superIdCounter - 1) % superIdCounterLimit != 0) {
            superIdCounter++;
        }
        return res;

    }

    private List<String> fileWordsToList(File file) throws IOException {
        List<String> result = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            String line;
            while (fileReader.ready()) {
                if (result.size() >= superIdCounterLimit) break;
                line = fileReader.readLine();
                result.add(line);
            }
        }
        return result;
    }

    private List<String> fileToList(File file) throws IOException {
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

    private Set<String> conditions(String[] words) {
        Set<String> set = new HashSet<>();


        for (String word : words) {
//            if (word.length() > 1 && word.length() < 21 && !word.startsWith("'")  && (!"tom".equals(word) &&
//                    !"mary".equals(word) && !"tatoeba".equals(word) && !"th".equals(word) &&
//                    !"".equals(word) && !word.startsWith("tom'") && !word.startsWith("mary'"))){
            if (word.length() > 1 && word.length() < 21 && (!"tom".equals(word) &&
                    !"mary".equals(word) && !"tatoeba".equals(word) &&
                    !"".equals(word))) {
                if (unusualLanguagesSet.contains(learningLang)) {
                    word = word.replaceAll("[а-яА-Яa-zA-Z]", "");
                } else if ("rus".equals(learningLang)) {
                    word = word.replaceAll("[a-zA-Z]", "");
                }

                if (!"".equals(word)) set.add(word);
            }
        }
        return set;
    }

    private String removePunctuationAndDigits(String word) {
//        String res = word.replaceAll("[^'\\p{L}]+", " ").toLowerCase();
        String res = word.replaceAll("[^\\p{L}]+", " ").toLowerCase();
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

    private static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }


    private void fillSentenceMap() {

        String id;
        String line;
        String lang;
        String sentence;
        LinkedHashMap<String, String> res;
        StringTokenizer st;
        for (Map.Entry<String, String> pair : allSentences.entrySet()) {
            line = pair.getValue();
            st = new StringTokenizer(line, "\t");
            id = st.nextToken();
            lang = st.nextToken();
            sentence = st.nextToken();
            res = mapAllLangSen.get(lang);
            res.put(id, sentence);
            mapAllLangSen.put(lang, res);
        }
    }

    private void initialize() throws IOException {
        allSenIdsSet = new LinkedHashSet<>();
        word_sentencesLinksMap = new TreeMap<>();
        mapAllLangSen = new LinkedHashMap<>();
        mapAllLangWord = new LinkedHashMap<>();
        for (String lang : languages) {
            mapAllLangSen.put(lang, new LinkedHashMap());
            mapAllLangWord.put(lang, new TreeMap<>());
        }
        setAudio = audioFileToSet(new File("src/development/files/sentences_with_audio.csv"));
        originalLinksSet = fileToSet(new File("src/development/files/links.csv"));
        allSentences = readSentencesFile(new File("src/development/files/sentences.csv"));
    }

    private String convertStringForTSV(String text) {
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
            String sen;
            StringTokenizer st;
            while (fileReader.ready()) {
                line = fileReader.readLine();
                st = new StringTokenizer(line, "\t");
                id = st.nextToken();
                lang = st.nextToken();
                sen = st.nextToken();
                if (originalLinksSet.contains(id) && mapAllLangSen.containsKey(lang) && sen.length() <= maxSentenceLength) {
                    if (setAudio.contains(Integer.parseInt(id))) {
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

    private <T> Set<T> fileToSet(File file) throws IOException {
        Set<T> result = new HashSet<>();
        String line;
        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            StringTokenizer st;
            while (fileReader.ready()) {
                line = fileReader.readLine();

                st = new StringTokenizer(line, "\t");

                result.add((T) st.nextToken());
                result.add((T) st.nextToken());
            }
        }
        return result;
    }


    private Set<Integer> audioFileToSet(File file) throws IOException {
        Set<Integer> result = new TreeSet<>();
        String line;
        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            while (fileReader.ready()) {
                line = fileReader.readLine();
                result.add(Integer.parseInt(line));
            }
        }
        return result;
    }

    private void resetStaticCollections() {
        mapAllLangSen = new HashMap<>();
        mapAllLangWord = new HashMap<>();
        allSentences = new HashMap<>();
        word_sentencesLinksMap = new HashMap<>();

        setAudio = new HashSet<>();
        originalLinksSet = new HashSet<>();
    }
}
