package de.tuebingen.sfs.iconevol;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.tokenize.GreedyIPATokenizerConfiguration;
import de.jdellert.iwsa.tokenize.IPATokenizer;
import de.jdellert.iwsa.util.io.StringUtils;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFParameter;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.util.LanguageTree;
import de.tuebingen.sfs.util.ListReader;
import de.tuebingen.sfs.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class IconEvolUtil {

    public static PhoneticString extractSegments(CLDFForm form, PhoneticSymbolTable symTable, IPATokenizer tokenizer) {
        if (form == null)
            return new PhoneticString(new int[0]);
        if (tokenizer == null) {
            String[] rootSegments = form.getSegments();
            return new PhoneticString(symTable.encode(rootSegments));
        }
        else {
            String[] tokens = tokenizer.tokenizeIPA(form.getForm());
            //System.err.println(form.getForm() + " -> " + StringUtils.join(" ",tokens));
            return new PhoneticString(symTable.encode(tokens));
        }
    }

    public static IPATokenizer configureLundTokenizer(Map<String, Set<String>> soundGroups) {
        GreedyIPATokenizerConfiguration config = new GreedyIPATokenizerConfiguration();

        for (Set<String> soundGroup : soundGroups.values()) {
            for (String sound : soundGroup) {
                config.addUnchangedSymbol(sound);
            }
        }

        config.addIgnoredSymbol(' ');
        config.addIgnoredSymbol('-');
        config.addIgnoredSymbol('_');
        config.addIgnoredSymbol('.');
        config.addIgnoredSymbol('|');
        config.addIgnoredSymbol('͡');
        config.addIgnoredSymbol('ˈ');
        config.addIgnoredSymbol('ˌ');
        config.addIgnoredSymbol('˦');
        config.addIgnoredSymbol('˨');
        config.addIgnoredSymbol('ˑ');
        config.addIgnoredSymbol('̃');
        config.addIgnoredSymbol('̆');
        config.addIgnoredSymbol('̈');
        config.addIgnoredSymbol('̚');
        config.addIgnoredSymbol('̜');
        config.addIgnoredSymbol('̝');
        config.addIgnoredSymbol('̞');
        config.addIgnoredSymbol('̟');
        config.addIgnoredSymbol('̯');
        config.addIgnoredSymbol('̰');
        config.addIgnoredSymbol('̹');
        config.addIgnoredSymbol('̻');
        config.addIgnoredSymbol('͈');
        config.addIgnoredSymbol('́');
        config.addIgnoredSymbol('̂');
        config.addIgnoredSymbol('̄');
        config.addIgnoredSymbol('̌');
        config.addIgnoredSymbol('ʼ');
        config.addIgnoredSymbol('̪');
        config.addIgnoredSymbol('̇');
        config.addIgnoredSymbol('̩');
        config.addIgnoredSymbol('̬');

        //replacements defined by the Lund tokenization
        config.addSequenceTransformation("ɫ", Arrays.asList(new String[] {"l", "ɣ"}));
        config.addSequenceTransformation("ɡ̥", Arrays.asList(new String[] {"k"}));
        config.addSequenceTransformation("b̥", Arrays.asList(new String[] {"p"}));
        config.addSequenceTransformation("d̥", Arrays.asList(new String[] {"t"}));

        //replacements for correcting a few transcription errors in NorthEuraLex 0.9
        config.addSequenceTransformation("sʰː", Arrays.asList(new String[] {"s", "sʰ"}));
        config.addSequenceTransformation("k̥", Arrays.asList(new String[] {"k"}));
        config.addSequenceTransformation("v̥", Arrays.asList(new String[] {"f"}));
        config.addSequenceTransformation("x̥", Arrays.asList(new String[] {"x"}));
        config.addSequenceTransformation("ḥ", Arrays.asList(new String[] {"h"}));
        config.addSequenceTransformation("ž", Arrays.asList(new String[] {"ʒ"}));
        config.addSequenceTransformation("ē", Arrays.asList(new String[] {"e", "e"}));
        config.addSequenceTransformation("ī", Arrays.asList(new String[] {"i", "i"}));
        config.addSequenceTransformation("ō", Arrays.asList(new String[] {"o", "o"}));
        config.addSequenceTransformation("á", Arrays.asList(new String[] {"a"}));
        config.addSequenceTransformation("é", Arrays.asList(new String[] {"e"}));
        config.addSequenceTransformation("í", Arrays.asList(new String[] {"i"}));
        config.addSequenceTransformation("ô", Arrays.asList(new String[] {"o"}));
        config.addSequenceTransformation("à", Arrays.asList(new String[] {"a"}));
        config.addSequenceTransformation("ò", Arrays.asList(new String[] {"o"}));
        config.addSequenceTransformation("ʲ̥", Arrays.asList(new String[] {"ʲ"}));

        config.factorInGeminationSymbol("ː");

        return new IPATokenizer(config);
    }

    public static  List<Pair<String,String>> defineNorthEuraLexMapping(CLDFWordlistDatabase db) {
        List<Pair<String,String>> nelexConcepts = new LinkedList<Pair<String,String>>();
        for (CLDFParameter concept : db.getConceptMap().values()) {
            nelexConcepts.add(new Pair<String,String>(concept.getProperties().get("concepticon_proposed"), concept.getParamID()));
        }
        return nelexConcepts;
    }

    public static Map<String, String> getLangToFamilyMap(List<String> langs, String treeFileName) {
        LanguageTree globalTree = null;
        try {
            globalTree = LanguageTree.fromNewickFile(treeFileName);
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: tree file \"" + treeFileName + "\" not found!");
            System.exit(1);
        }

        Map<String, String> langToFamily = new TreeMap<String, String>();
        for (String lang : langs) {
            List<String> path = globalTree.pathFromRoot(lang);
            if (path.size() == 1) {
                langToFamily.put(lang, lang);
            } else {
                langToFamily.put(lang, globalTree.pathFromRoot(lang).get(1));
            }
        }

        return langToFamily;
    }

    public static Map<String, Double> loadLexicalStabilities() {
        Map<String,Double> classStabilities = new TreeMap<>();
        try (Stream<String> stream = Files.lines(Paths.get("/home/jdellert/synchro/arb/hst/iconevol/lexical-stability.tsv"))) {
            stream.map(s -> s.split("\t")).forEach(entry -> classStabilities.put(entry[0], Double.parseDouble(entry[1])));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classStabilities;
    }

    public static Map<String, Double> loadSoundGroupStabilities() {
        Map<String,Double> classStabilities = new TreeMap<>();
        try (Stream<String> stream = Files.lines(Paths.get("/home/jdellert/synchro/arb/hst/iconevol/soundgroup-stability.tsv"))) {
            stream.map(s -> s.split("\t")).forEach(entry -> classStabilities.put(entry[0], Double.parseDouble(entry[1])));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classStabilities;
    }

    public static Map<String, Set<String>> loadSoundGroups(String soundGroupTsvFile){
        Map<String,Set<String>> soundGroups = new TreeMap<String,Set<String>>();
        try {
            List<String[]> lines = ListReader.arrayFromTSV(soundGroupTsvFile);
            //first line is the header, hardcoding the format for now instead of relying on column names
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = lines.get(i);
                //use IPA chunk in first column, make sure IWSA is configured to work on the same tokenization
                String ipa = fields[0];
                //ignoring the last two columns, other columns (starting with column 3) contain classes to assign the ipa char to
                for (int c = 2; c < fields.length - 2; c++) {
                    String groupID = fields[c];
                    if (groupID.length() > 0) {
                        Set<String> group = soundGroups.get(groupID);
                        if (group == null) {
                            group = new TreeSet<String>();
                            soundGroups.put(groupID, group);
                        }
                        group.add(ipa);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: could not load sound groups from file " + soundGroupTsvFile + ", returning empty sound classes!");
        }
        return soundGroups;
    }
}
