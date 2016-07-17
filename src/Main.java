import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Main {

    public static String[] languages = {"eng", "rus", "deu", "spa", "jpn", "cmn", "ara"};
    public static Map<String, LinkedHashMap> mapAllLangSen;
    public static Map<String, LinkedHashMap> mapAllLangWord;



    public static Set<String> setAudio;
    public static Set<String> AllSenIdsSet;
    public static Set<String> originalLinksSet;



    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.start();

    }

    private void start() throws IOException {
        makeDir("/");




        for (String lang : languages) {
            mapAllLangSen = new LinkedHashMap<>();
            mapAllLangSen.put(lang, new LinkedHashMap());

            mapAllLangWord = new LinkedHashMap<>();
            mapAllLangSen.put(lang, new LinkedHashMap());
        }


        setAudio = fileToSet(new File("/audio.csv"), 1);
        originalLinksSet = fileToSet(new File("/links.csv"));






    }


    private void makeDir(String devDir) {
        File dir1 = new File(devDir + "/seeds_data");
        File dir2 = new File(devDir + "/seeds_data/words");
        dir1.mkdir();
        dir2.mkdir();
    }

    public <T> Set<T> fileToSet(File file) throws IOException {
        Set<T> result = new HashSet<>();

        try (BufferedReader fileReader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            String line;
            while (fileReader.ready()) {
                line = fileReader.readLine();

                String[] arr = parseLineLight(line);

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
