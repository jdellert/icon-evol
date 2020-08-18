package de.tuebingen.sfs.iconevol;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.tokenize.IPATokenizer;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.cldfjava.io.CLDFImport;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.tuebingen.sfs.util.LanguageTree;

public class SoundGroupStabilityOutput {
    private static final String DB_DIR = "src/main/resources/northeuralex-0.9";

    public static Map<String,Map<String,FormProjectionModel>> inferProjectionModels(CLDFWordlistDatabase db, Map<String, String> langToFamily, IPATokenizer tokenizer, CorrespondenceModel corrModel, Map<String, InformationModel> infoModels) {

        // initialize empty projection models
        Map<String,Map<String,FormProjectionModel>> projectionModels = new TreeMap<String,Map<String,FormProjectionModel>>();
        for (String lang1 : langToFamily.keySet()) {
            Map<String, FormProjectionModel> modelsForLang1 = new TreeMap<String, FormProjectionModel>();
            String family1 = langToFamily.get(lang1);
            for (String lang2 : langToFamily.keySet()) {
                if (lang1.equals(lang2)) continue;
                String family2 = langToFamily.get(lang2);
                if (!family1.equals(family2)) continue;
                modelsForLang1.put(lang2, new FormProjectionModel(lang1, lang2));
            }
            projectionModels.put(lang1, modelsForLang1);
        }

        if (corrModel != null) {
            PhoneticSymbolTable symTable = corrModel.getSymbolTable();

            Map<Integer, Set<Integer>> cognateSets = db.getCogsetToCognates();
            for (Map.Entry<Integer, Set<Integer>> entry : cognateSets.entrySet()) {
                Set<Integer> cognateSet = entry.getValue();
                for (int cldfFormId1 : cognateSet) {
                    CLDFForm cldfForm1 = db.getFormsMap().get(cldfFormId1);
                    String lang1 = db.getLanguageMap().get(cldfForm1.getLangID()).getIso();
                    String family1 = langToFamily.get(lang1);
                    PhoneticString form1 = IconEvolUtil.extractSegments(cldfForm1, symTable, tokenizer);
                    for (int cldfFormId2 : cognateSet) {
                        CLDFForm cldfForm2 = db.getFormsMap().get(cldfFormId2);
                        String lang2 = db.getLanguageMap().get(cldfForm2.getLangID()).getIso();
                        if (lang1.equals(lang2)) continue;
                        String family2 = langToFamily.get(lang2);
                        if (!family1.equals(family2)) continue;
                        PhoneticString form2 = IconEvolUtil.extractSegments(cldfForm2, symTable, tokenizer);
                        PhoneticStringAlignment align = InformationWeightedSequenceAlignment.constructAlignment(
                                form1, form2, corrModel, corrModel, corrModel, corrModel, infoModels.get(lang1), infoModels.get(lang2));

                        List<String[]> pairs = align.getSymbolPairs(symTable);
                        int pos1 = 0;
                        int pos2 = 0;
                        for (int i = 0; i < pairs.size(); i++) {
                            String upperSymbol = pairs.get(i)[0];
                            String lowerSymbol = pairs.get(i)[1];

                            double infoScore = 1.0;

                            if (upperSymbol.equals("-")) {
                                infoScore = InformationWeightedSequenceAlignment.getMeanInfoScore(form2, form2, pos2, pos2, infoModels.get(lang2), infoModels.get(lang2));
                                pos2++;
                            } else if (lowerSymbol.equals("-")) {
                                infoScore = InformationWeightedSequenceAlignment.getMeanInfoScore(form1, form1, pos1, pos1, infoModels.get(lang1), infoModels.get(lang1));
                                pos1++;
                            } else {
                                infoScore = InformationWeightedSequenceAlignment.getMeanInfoScore(form1, form2, pos1, pos2, infoModels.get(lang1), infoModels.get(lang2));
                                pos1++;
                                pos2++;
                            }

                            projectionModels.get(lang1).get(lang2).storePairWithWeight(upperSymbol, lowerSymbol, infoScore);

                            String gappyBigram = upperSymbol;
                            if (i == 0) {
                                gappyBigram = "#" + gappyBigram;
                            } else {
                                gappyBigram = pairs.get(i-1)[0] + gappyBigram;
                            }
                            if (i == pairs.size() - 1) {
                                gappyBigram += "#";
                            } else {
                                gappyBigram += pairs.get(i+1)[0];
                            }
                            projectionModels.get(lang1).get(lang2).storeGappyBigramWithWeight(gappyBigram, lowerSymbol, infoScore);
                        }
                    }
                }
            }
        }

        return projectionModels;
    }

    public static void main(String[] args) {
        // load correspondence model previously trained on Lund tokenization (using CorrespondenceModelPreparation script)
        String corrPath = DB_DIR + "/global-iw-lund.corr";
        CorrespondenceModel corrModel = null;
        try {
            System.err.print("Loading global correspondence model...");
            corrModel = CorrespondenceModelStorage.deserializeCorrespondenceModel(new ObjectInputStream(new FileInputStream(corrPath)));
            System.err.println(" Done.");
        } catch (FileNotFoundException e) {
            System.err.println(e.toString());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            System.exit(1);
        } catch (ClassNotFoundException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            System.exit(1);
        }
        PhoneticSymbolTable symbolTable = corrModel.getSymbolTable();

        // load NorthEuraLex database
        System.err.print("Loading database...");
        CLDFWordlistDatabase db = CLDFImport.loadDatabase(DB_DIR);
        System.err.println(" Done.");

        // load and prepare language data (only family membership is actually used)
        String treeFileName = DB_DIR + "/tree.nwk";
        LanguageTree globalTree = null;
        try {
            globalTree = LanguageTree.fromNewickFile(treeFileName);
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: tree file \"" + treeFileName + "\" not found!");
            System.exit(1);
        }

        List<String> langs = db.listLanguageISOs();
        Map<String, String> langToFamily = new TreeMap<String, String>();
        for (String lang : langs) {
            List<String> path = globalTree.pathFromRoot(lang);
            if (path.size() == 1) {
                langToFamily.put(lang, lang);
            } else {
                langToFamily.put(lang, globalTree.pathFromRoot(lang).get(1));
            }
        }

        // load Lund tokenization model and configure the tokenizer accordingly
        Map<String,Set<String>> soundGroups = IconEvolUtil.loadSoundGroups("src/main/resources/sound-group-definitions.tsv");
        IPATokenizer tokenizer = IconEvolUtil.configureLundTokenizer(soundGroups);

        // create information models (for information-weighted sequence alignment)
        System.err.print("Building information models... ");
        Map<String, InformationModel> infoModels = new TreeMap<String, InformationModel>();
        for (String isoCode : langs) {
            String langID = db.searchLangIdForIsoCode(isoCode);
            InformationModel infoModel = InformationModelInference.inferInformationModelForLanguage(langID, db, symbolTable, tokenizer);
            infoModels.put(isoCode, infoModel);
        }
        System.err.println("Done.");

        // extract projection models for each pair of languages from the same family
        System.err.println("Building projection models... ");
        Map<String, Map<String, FormProjectionModel>> projectionModels = inferProjectionModels(db, langToFamily, tokenizer, corrModel, infoModels);
        for (String lang1 : projectionModels.keySet()) {
            for (FormProjectionModel model : projectionModels.get(lang1).values()) {
                model.finalizeCounts(5);
            }
        }
        System.err.println("Done.");

        Map<String,Double> weightedNumInstances = new TreeMap<String,Double>();
        Map<String,Map<String,Double>> weightedShiftCounts =  new TreeMap<String,Map<String,Double>>();

        Map<Integer, Set<Integer>> cognateSets = db.getCogsetToCognates();
        for (Map.Entry<Integer, Set<Integer>> entry : cognateSets.entrySet()) {
            Set<Integer> cognateSet = entry.getValue();
            for (int cldfFormId1 : cognateSet) {
                CLDFForm cldfForm1 = db.getFormsMap().get(cldfFormId1);
                String lang1 = db.getLanguageMap().get(cldfForm1.getLangID()).getIso();
                String family1 = langToFamily.get(lang1);
                PhoneticString form1 = IconEvolUtil.extractSegments(cldfForm1, symbolTable, tokenizer);
                for (int cldfFormId2 : cognateSet) {
                    CLDFForm cldfForm2 = db.getFormsMap().get(cldfFormId2);
                    String lang2 = db.getLanguageMap().get(cldfForm2.getLangID()).getIso();
                    if (lang1.equals(lang2)) continue;
                    String family2 = langToFamily.get(lang2);
                    if (family1.equals(family2)) {
                        PhoneticString form2 = IconEvolUtil.extractSegments(cldfForm2, symbolTable, tokenizer);
                        PhoneticStringAlignment align = InformationWeightedSequenceAlignment.constructAlignment(
                                form1, form2, corrModel, corrModel, corrModel, corrModel, infoModels.get(lang1), infoModels.get(lang2));

                        int pos1 = 0;
                        int pos2 = 0;
                        for (String[] pair : align.getSymbolPairs(symbolTable)) {
                            String s1 = pair[0];
                            String s2 = pair[1];

                            System.err.print(lang1 + "\t" + cldfForm1.getForm() + "\t" + lang2 + "\t" + cldfForm2.getForm() + "\t" + s1 + "\t" + s2);

                            double infoScore = 1.0;

                            if (s1.equals("-")) {
                                infoScore = InformationWeightedSequenceAlignment.getMeanInfoScore(form2, form2, pos2, pos2, infoModels.get(lang2), infoModels.get(lang2));
                                pos2++;
                            } else if (s2.equals("-")) {
                                infoScore = InformationWeightedSequenceAlignment.getMeanInfoScore(form1, form1, pos1, pos1, infoModels.get(lang1), infoModels.get(lang1));
                                pos1++;
                            } else {
                                infoScore = InformationWeightedSequenceAlignment.getMeanInfoScore(form1, form2, pos1, pos2, infoModels.get(lang1), infoModels.get(lang2));
                                pos1++;
                                pos2++;
                            }

                            double weight = (1.0 - projectionModels.get(lang1).get(lang2).lang1Stability) * infoScore;

                            System.err.println("\t" + infoScore + "\t" + weight);

                            Double weightedInstancesS1 = weightedNumInstances.get(s1);
                            if (weightedInstancesS1 == null) {
                                weightedInstancesS1 = 0.0;
                                weightedShiftCounts.put(s1, new TreeMap<String, Double>());
                            }
                            weightedNumInstances.put(s1, weightedInstancesS1 + weight);

                            Map<String,Double> weightedShiftCountsS1 = weightedShiftCounts.get(s1);
                            Double weightedInstancesS1S2 = weightedShiftCountsS1.get(s2);
                            if (weightedInstancesS1S2 == null) {
                                weightedInstancesS1S2 = 0.0;
                            }
                            weightedShiftCountsS1.put(s2, weightedInstancesS1S2 + weight);

                        }
                    }
                }
            }
        }

        List<String> orderOfSoundGroups = new LinkedList<String>(soundGroups.keySet());

        System.out.println("SoundGroup\tWeightedNumAlignments\tStable\tShiftInGroup\tShiftOutOfGroup\tLossOrGain");
        for (String soundGroupName : orderOfSoundGroups) {
            Set<String> soundGroup = soundGroups.get(soundGroupName);
            double weightedNumAlignments = 0.0;
            double weightedCountStable = 0.0;
            double weightedCountShiftInGroup = 0.0;
            double weightedCountShiftOutOfGroup = 0.0;
            double weightedCountLossOrGain = 0.0;

            for (String sound : soundGroup) {
                Double numInstances = weightedNumInstances.get(sound);
                if (numInstances == null) continue;
                weightedNumAlignments += numInstances;
                for (String sound2 : weightedShiftCounts.get(sound).keySet()) {
                    double weightedCount = weightedShiftCounts.get(sound).get(sound2);
                    if (sound2.equals(sound)) {
                        weightedCountStable += weightedCount;
                    } else if (sound2.equals("-")) {
                        weightedCountLossOrGain += weightedCount;
                    } else if (soundGroup.contains(sound2)) {
                        weightedCountShiftInGroup += weightedCount;
                    } else {
                        weightedCountShiftOutOfGroup += weightedCount;
                    }
                }
            }
            System.out.println(soundGroupName + "\t" + weightedNumAlignments + "\t"
                    + weightedCountStable / weightedNumAlignments + "\t"
                    + weightedCountShiftInGroup / weightedNumAlignments + "\t"
                    + weightedCountShiftOutOfGroup / weightedNumAlignments + "\t"
                    + weightedCountLossOrGain / weightedNumAlignments);
        }
    }
}
