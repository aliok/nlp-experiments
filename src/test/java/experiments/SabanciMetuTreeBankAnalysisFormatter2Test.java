package experiments;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SabanciMetuTreeBankAnalysisFormatter2Test {

    private static AnalysisFormatter formatter;
    private static TurkishMorphology morphology;

    @BeforeClass
    public static void beforeClass() {
        formatter = new SabanciMetuTreeBankAnalysisFormatter2(true);
        morphology = TurkishMorphology.createWithDefaults();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"taranmış", "(1,\"tara+Verb\")(2,\"Verb+Pass+Pos+Narr+A3sg\")", 1},
                {"dedi", "(1,\"de+Verb+Pos+Past+A3sg\")", 0},
                {"demedi", "(1,\"de+Verb+Neg+Past+A3sg\")", 0},
                {"olan", "(1,\"ol+Verb+Pos\")(2,\"Adj+PresPart\")", 0},
                {"ben", "(1,\"ben+Pron+Pers+A1sg+Pnon+Nom\")", 1},
                {"olarak", "(1,\"ol+Verb+Pos\")(2,\"Adv+ByDoingSo\")", 0},
                {"şey", "(1,\"şey+Noun+A3sg+Pnon+Nom\")", 0},
                {"bana", "(1,\"ben+Pron+Pers+A1sg+Pnon+Dat\")", 3},
                {"ne", "(1,\"ne+Pron+Ques+A3sg+Pnon+Nom\")", 5},
                {"beni", "(1,\"ben+Pron+Pers+A1sg+Pnon+Acc\")", 1},
                {"değil", "(1,\"değil+Verb+Pres+A3sg\")", 1},
                {"olduğunu", "(1,\"ol+Verb+Pos\")(2,\"Noun+PastPart+A3sg+P3sg+Acc\")", 1},
        });
    }

    private final String word;
    private final String expected;
    private final int parseResultIndex;

    public SabanciMetuTreeBankAnalysisFormatter2Test(String word, String expected, int parseResultIndex) {
        this.word = word;
        this.expected = expected;
        this.parseResultIndex = parseResultIndex;
    }

    @Test
    public void test() {
        final WordAnalysis analysis = morphology.analyze(word);

        if (parseResultIndex >= 0) {
            final SingleAnalysis item = analysis.getAnalysisResults().get(parseResultIndex);
            final String actual = formatter.format(item);
            if (actual.equals(expected)) {
                return;
            }

            System.out.println(word + "---------------");
            System.out.println("\tEXPECTED: " + expected);
            System.out.println("\tACTUAL:   " + actual);
            System.out.println("\tOFLAZER:  " + AnalysisFormatters.OFLAZER_STYLE.format(item));
            fail();
        } else {
            System.out.println(word + "---------------");
            System.out.println("\t  EXPECTED: " + expected);
            List<SingleAnalysis> analysisResults = analysis.getAnalysisResults();
            for (int i = 0; i < analysisResults.size(); i++) {
                SingleAnalysis item = analysisResults.get(i);
                System.out.println("\t" + i + " ACTUAL:   " + formatter.format(item));
                System.out.println("\t" + i + " OFLAZER:   " + AnalysisFormatters.OFLAZER_STYLE.format(item));
            }
        }
    }


}