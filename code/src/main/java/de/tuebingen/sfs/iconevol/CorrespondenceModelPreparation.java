package de.tuebingen.sfs.iconevol;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelInference;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.tokenize.IPATokenizer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class CorrespondenceModelPreparation {
    public static void main(String[] args) {
        try {
            //configure IWSA IPA tokenizer which implements Lund tokenization, using simple new configuration options
            Map<String, Set<String>> soundGroups = IconEvolUtil.loadSoundGroups("src/main/resources/sound-group-definitions.tsv");
            IPATokenizer tokenizer = IconEvolUtil.configureLundTokenizer(soundGroups);

            //infer and store global sound correspondences for Lund tokenization (building only on the forms file)
            LexicalDatabase database = CLDFImport.loadDatabase("src/main/resources/northeuralex-0.9/forms.csv", tokenizer);
            PhoneticSymbolTable symbolTable = database.getSymbolTable();
            InformationModel[] infoModels = InformationModelInference.inferInformationModels(database, symbolTable);

            CorrespondenceModel globalCorrModel = CorrespondenceModelInference.inferGlobalCorrespondenceModel(database, symbolTable, infoModels);
            CorrespondenceModelStorage.serializeGlobalModelToFile(globalCorrModel, "src/main/resources/northeuralex-0.9/global-iw-lund.corr");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
