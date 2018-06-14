package experiments;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

/**
 * @author Ali Ok (ali.ok@apache.org)
 * 6/14/18 11:54 PM
 **/
public class FormatExperiments {

    private static final AnalysisFormatter formatter = new SabanciMetuTreeBankAnalysisFormatter(true);

    public static void main(String[] args) {
        final TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

        // printWordAnalysis(morphology, "taranmış", "(1,\"tara+Verb\")(2,\"Verb+Pass+Pos+Narr+A3sg\")");
        printWordAnalysis(morphology, "dedi", "(1,\"de+Verb+Pos+Past+A3sg\")");
        printWordAnalysis(morphology, "demedi", "(1,\"de+Verb+Neg+Past+A3sg\")");
    }

    private static void printWordAnalysis(TurkishMorphology morphology, String word, String s) {
        final WordAnalysis analysis = morphology.analyze(word);
        System.out.println(s);
        for (SingleAnalysis item : analysis) {
            System.out.printf("%s --> %s\n", item.formatLong(), formatter.format(item));
        }
    }

}
