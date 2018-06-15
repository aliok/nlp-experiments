package experiments;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.*;

import java.util.List;

/**
 * @author Ali Ok (ali.ok@apache.org)
 * 6/14/18 11:54 PM
 **/
public class FormatExperiments {

    private static final AnalysisFormatter formatter = new SabanciMetuTreeBankAnalysisFormatter2(true);

    public static void main(String[] args) {
        final TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

//        printWordAnalysis(morphology, "taranmış", "(1,\"tara+Verb\")(2,\"Verb+Pass+Pos+Narr+A3sg\")", 1);
//        printWordAnalysis(morphology, "dedi", "(1,\"de+Verb+Pos+Past+A3sg\")", -1);
//        printWordAnalysis(morphology, "demedi", "(1,\"de+Verb+Neg+Past+A3sg\")", -1);
    }

//    private static void printWordAnalysis(TurkishMorphology morphology, String word, String s, int index) {
//        final WordAnalysis analysis = morphology.analyze(word);
//        // SentenceAnalysis result = morphology.disambiguate(sentence, analyses);
//
//        if (index >= 0) {
//            final SingleAnalysis item = analysis.getAnalysisResults().get(index);
//            System.out.println(word + "---------------");
//            System.out.println("\tEXPECTED: " + s);
//            System.out.println("\tACTUAL:   " + formatter.format(item));
//            System.out.println("\tOFLAZER:  " + AnalysisFormatters.OFLAZER_STYLE.format(item));
//        } else {
//            System.out.println(word + "---------------");
//            System.out.println("\t  EXPECTED: " + s);
//            List<SingleAnalysis> analysisResults = analysis.getAnalysisResults();
//            for (int i = 0; i < analysisResults.size(); i++) {
//                SingleAnalysis item = analysisResults.get(i);
//                System.out.println("\t" + i + " ACTUAL:   " + formatter.format(item));
//                System.out.println("\t" + i + " OFLAZER:   " + AnalysisFormatters.OFLAZER_STYLE.format(item));
//            }
//        }
//    }

}
