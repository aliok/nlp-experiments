package experiments;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.google.common.io.CharSource;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.WordAnalysis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DisambiguateSentences {

    public static void main(String[] args) throws IOException {

        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        final AnalysisFormatter formatter = new SabanciMetuTreeBankAnalysisFormatter2(true);

        doExperiment(999, false, morphology, formatter);
    }

    private static void doExperiment(int index, boolean printSurfaces, TurkishMorphology morphology, AnalysisFormatter formatter) throws IOException {

        final CharSource source = Resources.asCharSource(Resources.getResource("simpleparseset" + index + ".txt"),
                Charset.forName("utf-8"));

        //read all in advance
        final List<Pair<String, String>> lines = source.readLines(new SimpleParseSetValidationLineProcessor());

        final int numberOfSurfaces = lines.size();
        System.out.println("Number of words to parse " + numberOfSurfaces);

        int unparsable = 0;
        int incorrectParses = 0;
        int skippedSurfaces = 0;
        int skippedExpectedParseResults = 0;

        final TreeMultiset<String> unparsableSurfaces = TreeMultiset.create();
        final TreeMultiset<String> incorrectParsedSurfaces = TreeMultiset.create();

        for (Pair<String, String> line : lines) {
            final String surfaceToParse = line.getLeft();
            if (SURFACES_TO_SKIP.contains(surfaceToParse)) {
                if (printSurfaces)
                    System.out.println("Surface '" + surfaceToParse + "' is a skippedSurface");
                skippedSurfaces++;
                continue;
            }

            final String expectedResult = line.getRight();
            if (isSkippedExpectedParseResult(expectedResult)) {
                if (printSurfaces)
                    System.out.println("Surface with expected parse result '" + expectedResult + "' is a skippedExpectedParseResult");
                skippedExpectedParseResults++;
                continue;
            }

            final WordAnalysis analysis = morphology.analyze(surfaceToParse);

            if (analysis.analysisCount() == 0) {
                if (printSurfaces)
                    System.out.println("Surface '" + surfaceToParse + "' is not parseable");
                unparsableSurfaces.add(surfaceToParse + "\t" + expectedResult);
                unparsable++;
            } else {
                final Collection<String> formattedRetrievedResults = FluentIterable.from(analysis).transform(input -> formatter.format(input)).toList();
                if (!formattedRetrievedResults.contains(expectedResult)) {
                    if (printSurfaces) {
                        System.out.println("Surface '" + surfaceToParse + "' is parseable, but expected result '" + expectedResult + "' is not found!");
                        System.out.println("\t" + Joiner.on("\n\t").join(formattedRetrievedResults));
                    }
                    incorrectParsedSurfaces.add(surfaceToParse + " ----> " + expectedResult + " ----> " + formattedRetrievedResults.toString());
                    incorrectParses++;
                }
            }
        }


        final int correctParses = numberOfSurfaces - unparsable - incorrectParses - skippedSurfaces - skippedExpectedParseResults;

        System.out.println("========SUMMARY===========");
        System.out.println("Surface count             :\t\t" + numberOfSurfaces);
        System.out.println("Unparsable                :\t\t" + unparsable);
        System.out.println("Incorrect parses          :\t\t" + incorrectParses);
        System.out.println("Skipped surfaces          :\t\t" + skippedSurfaces);
        System.out.println("Skipped parse results     :\t\t" + skippedExpectedParseResults);
        System.out.println("Correct parses            :\t\t" + correctParses);
        System.out.println("Correct parse %           :\t\t" + (correctParses) * 1.0 / numberOfSurfaces * 100);

        final ImmutableMultiset<String> unparsableSurfacesByFrequencies = Multisets.copyHighestCountFirst(unparsableSurfaces);
        final ImmutableMultiset<String> incorrectParsedSurfacesByFrequencies = Multisets.copyHighestCountFirst(incorrectParsedSurfaces);

        System.out.println("=====Incorrect parsed surfaces with occurrence count > 1=====");
        for (Multiset.Entry<String> entry : incorrectParsedSurfacesByFrequencies.entrySet()) {
            if (entry.getCount() > 1)
                System.out.println(entry.getElement() + "\t\t\t" + entry.getCount());
        }

        System.out.println("=====Unparsable surfaces");
        for (Multiset.Entry<String> entry : unparsableSurfacesByFrequencies.entrySet()) {
            System.out.println(entry.getElement() + "\t\t\t" + entry.getCount());
        }


    }

    public static class SimpleParseSetValidationLineProcessor implements LineProcessor<List<Pair<String, String>>> {
        final ImmutableList.Builder<Pair<String, String>> builder = ImmutableList.builder();

        @Override
        public boolean processLine(final String line) throws IOException {
            if (!"#END#OF#SENTENCE#".equals(line)) {
                final String[] split = line.split("=", 2);
                Validate.isTrue(split.length == 2, line);
                final String surface = split[0];
                final String expectedParseResultStr = applyParseResultReplaceHack(split[1]);
                builder.add(Pair.of(surface, expectedParseResultStr));
            }
            return true;
        }

        private String applyParseResultReplaceHack(String expectedParseResultStr) {
            for (Map.Entry<String, String> parseResultReplaceHackEntry : PARSE_RESULT_REPLACE_HACK_MAP.entrySet()) {
                expectedParseResultStr = expectedParseResultStr.replace(parseResultReplaceHackEntry.getKey(), parseResultReplaceHackEntry.getValue());
            }
            return expectedParseResultStr;
        }

        @Override
        public List<Pair<String, String>> getResult() {
            return builder.build();
        }
    }

    private static boolean isSkippedExpectedParseResult(String expectedResult) {
        if (EXPECTED_PARSE_RESULTS_TO_SKIP.contains(expectedResult))
            return true;
        for (String s : EXPECTED_PARSE_RESULTS_TO_SKIP) {
            if (expectedResult.contains(s))
                return true;
        }

        return false;
    }

    private static final ImmutableMap<String, String> PARSE_RESULT_REPLACE_HACK_MAP = new ImmutableMap.Builder<String, String>()
//            // for example, in simple parse set, there is a suffix Prog1 for "iyor", and Prog2 for "makta"
//            // but, we don't differentiate them and use "Prog" for both
//            // thus, we need a small hack for validating simple parse sets
//            .put("Prog1", "Prog")
//            .put("Prog2", "Prog")
//            .put("Inf1", "Inf")
//            .put("Inf2", "Inf")
//            .put("Inf3", "Inf")
            .put("WithoutHavingDoneSo1", "WithoutHavingDoneSo")
            .put("WithoutHavingDoneSo2", "WithoutHavingDoneSo")

            //TODO: Hastily suffix is without polarity in simple parse sets, but in TRNLTK, they need polarity
            .put("Hastily", "Hastily+Pos")

            //TODO: BIG TODO! not supported yet!
            .put("Postp+PCNom", "Postp")
            .put("Postp+PCDat", "Postp")
            .put("Postp+PCAcc", "Postp")
            .put("Postp+PCLoc", "Postp")
            .put("Postp+PCAbl", "Postp")
            .put("Postp+PCIns", "Postp")
            .put("Postp+PCGen", "Postp")

            .build();

    private static final ImmutableSet<String> SURFACES_TO_SKIP = new ImmutableSet.Builder<String>()
            .add("yapıyon").add("korkuyo").add("yakak")
            .add("Hiiç").add("Giir").add("hii").add("Geeç").add("yo").add("Yoo").add("ööö")     // mark as "Arbitrary Interjection"
            .add("Aaa").add("ham").add("aga").add("Eee").add("daa").add("çoook")
            .add("Börtü")
            .add("eşşek")
            .add("vb.").add("vb")

            .add("on'da").add("onaltıotuz'da").add("dokuz'a").add("dokuz.").add("atmışbeş'e").add("doksanaltı'dan")
            .add("doksandokuz.").add("dokuzkırkbeş'te").add("onikinci.").add("oniki.").add("onbirbindokuzyüzdoksansekiz").add("ondokuz.sekiz")
            .add("otuzbeşer").add("yedi.").add("yetmişaltı.").add("yirmibeş'lik").add("yirmisekizer").add("yirmiüç'üncü").add("onsekiz.")
            .add("19.8'lik")
            .add("i").add("ii").add("ci").add("yı").add("na")
            .add("ikiyüzyirmiüç.yedi")
            .add("dortyuz-besyuz")
            .add("emmioğlu").add("vizyonuuum")
            .add("diyip").add("diyin")
            .add("cigara").add("for").add("garni").add("hard").add("ilkyardım").add("kafatasımı").add("memişler")
            .add("the").add("thermal").add("volatilite").add("yankee").add("control")
            .add(",bir")
            .add("eşgüdüm").add("garnisine").add("adaları'na").add("anfide").add("düşkırıklığına").add("habire")
            .add("krizmalarıyla").add("kuruyemiş").add("mastıralar").add("mersi").add("önyüzünü").add("krizma")
            .add("metodoljik")
            .add("selahiyet").add("selahiyeti").add("mevlüt").add("usül")

            // "beyin meyin kalmamisti"
            .add("meyin").add("melektronik").add("mekonomi").add("mişletme").add("miçki").add("mumar").add("mefahat").add("moşku")
            .add("mırık").add("meker")


            .add("yüzeysel").add("çıtırlarla").add("vahlara").add("epeyce").add("ekononomik")

            // it is very hard to hack convert these from Zemberek to BounWebCorpus format
            //    NOPE: ise ----> (1,"ise+Conj") ----> [(1,"ise+Adv"), (1,"i+Verb+Cond+A3sg"), (1,"is+Noun+A3sg+Pnon+Dat")]			61
            .add("ise")
            //    NOPE: var ----> (1,"var+Adj")(2,"Verb+Zero+Pres+A3sg") ----> [(1,"var+Adj"), (1,"var+Verb+Imp+A2sg"), (1,"var+Noun+A3sg+Pnon+Nom")]			56
            .add("var")
            //    NOPE: zaman ----> (1,"zaman+Adv") ----> [(1,"zaman+Noun+Time+A3sg")]			45
            .add("zaman")
            //    NOPE: iyi ----> (1,"iyi+Adj")(2,"Adv+Zero") ----> [(1,"iyi+Adj"), (1,"iyi+Adv"), (1,"i+Noun+Prop+A3sg+Pnon+Acc"), (1,"iyi+Noun+A3sg+Pnon+Nom")]			35
            .add("iyi")
            //    NOPE: kendi ----> (1,"kendi+Pron+Reflex+A3sg+P3sg+Nom") ----> [(1,"kendi+Noun+A3sg+Pnon+Nom")
            .add("kendi")
            // kendini ----> (1,"kendi+Pron+Reflex+A3sg+P3sg+Acc") ----> [(1,"kendi+Noun+A3sg+P2sg+Acc")]			13
            .add("kendini")
            // kendimi ----> (1,"kendi+Pron+Reflex+A1sg+P1sg+Acc") ----> [(1,"kendi+Noun+A3sg+P1sg+Acc")]			10
            .add("kendimi")
            // gün ----> (1,"gün+Adv") ----> [(1,"gün+Noun+Prop+A3sg"), (1,"gün+Noun+Time+A3sg+Pnon+Nom")]			25
            .add("gün")
            // tek ----> (1,"tek+Adj") ----> [(1,"tek+Num"), (1,"tek+Adv"), (1,"tek+Noun+Prop+A3sg"), (1,"tek+Noun+A3sg+Pnon+Nom")]			25
            .add("tek")
            // an ----> (1,"an+Adv") ----> [(1,"an+Verb+Imp+A2sg"), (1,"an+Noun+Time+A3sg+Pnon+Nom")]			24
            .add("an")
            // yok ----> (1,"yok+Adj")(2,"Verb+Zero+Pres+A3sg") ----> [(1,"yok+Conj"), (1,"yok+Adj"), (1,"yok+Adv"), (1,"yok+Adj"), (1,"yok+Noun+Prop+A3sg"), (1,"yok+Noun+A3sg+Pnon+Nom")]			23
            .add("yok")
            // bazı ----> (1,"bazı+Adj") ----> [(1,"bazı+Det"), (1,"baz+Noun+A3sg+Pnon+Acc"), (1,"baz+Noun+A3sg+P3sg"), (1,"baz+Adj")(2,"Noun+Zero+A3sg+Pnon+Acc"), (1,"baz+Adj")(2,"Noun+Zero+A3sg+P3sg")]			21
            .add("bazı")
            // biri ----> (1,"biri+Pron+A3sg+P3sg+Nom") ----> [(1,"bir+Noun+Prop+A3sg+Pnon+Acc"), (1,"bir+Noun+Prop+A3sg+P3sg"), (1,"biri+Pron+Quant+A3sg+P3sg"), (1,"bir+Adj")(2,"Noun+Zero+A3sg+Pnon+Acc"), (1,"bir+Adj")(2,"Noun+Zero+A3sg+P3sg"), (1,"bir+Num+Card")(2,"Noun+Zero+A3sg+Pnon+Acc"), (1,"bir+Num+Card")(2,"Noun+Zero+A3sg+P3sg")]			21
            .add("biri")
            // burada ----> (1,"bura+Pron+A3sg+Pnon+Loc") ----> [(1,"bura+Noun+A3sg+Pnon+Loc")]			20
            .add("burada")
            // buraya ----> (1,"bura+Pron+A3sg+Pnon+Dat") ----> [(1,"bura+Noun+A3sg+Pnon+Dat")]			14
            .add("buraya")
            // orada ----> (1,"ora+Pron+A3sg+Pnon+Loc") ----> [(1,"ora+Noun+A3sg+Pnon+Loc")]			14
            .add("orada")
            // oraya ----> (1,"ora+Pron+A3sg+Pnon+Dat") ----> [(1,"ora+Noun+A3sg+Pnon+Dat"), (1,"oray+Noun+Prop+A3sg+Pnon+Dat")]			14
            .add("oraya")
            // ilgili ----> (1,"ilgili+Noun+A3sg+Pnon+Nom") ----> [(1,"ilgi+Noun+A3sg+Pnon+Nom")(2,"Adj+With")]			20
            .add("ilgili")
            // üzerinde ----> (1,"üzer+Noun+A3sg+P3sg+Loc") ----> [(1,"üzerinde+Adv"), (1,"üzer+Noun+Prop+A3sg+P2sg+Loc"), (1,"üzer+Noun+Prop+A3sg+P3sg+Loc"), (1,"üzeri+Noun+A3sg+P2sg+Loc"), (1,"üzeri+Noun+Prop+A3sg+P2sg+Loc")]			17
            .add("üzerinde")
            // üzerine ----> (1,"üzer+Noun+A3sg+P3sg+Dat") ----> [(1,"üzerine+Adv"), (1,"üzer+Noun+Prop+A3sg+P2sg+Dat"), (1,"üzer+Noun+Prop+A3sg+P3sg+Dat"), (1,"üzeri+Noun+A3sg+P2sg+Dat"), (1,"üzeri+Noun+Prop+A3sg+P2sg+Dat")]			16
            .add("üzerine")
            // üzerindeki ----> (1,"üzer+Noun+A3sg+P3sg+Loc")(2,"Adj+PointQual") ----> [(1,"üzer+Noun+Prop+A3sg+P2sg+Loc")(2,"Adj+Rel"), (1,"üzer+Noun+Prop+A3sg+P3sg+Loc")(2,"Adj+Rel"), (1,"üzeri+Noun+A3sg+P2sg+Loc")(2,"Adj+Rel"), (1,"üzeri+Noun+Prop+A3sg+P2sg+Loc")(2,"Adj+Rel")]			7
            .add("üzerindeki")
            // akşam ----> (1,"akşam+Adv") ----> [(1,"akşam+Noun+Time+A3sg+Pnon+Nom")]			16
            .add("akşam")

            .build();

    private static final ImmutableSet<String> EXPECTED_PARSE_RESULTS_TO_SKIP = new ImmutableSet.Builder<String>()
            .add("1+Num+Card")
            .add("70+Num+Card")
            .add("Num+Distrib")

            .add("Postp")
            // sacmalik!
            .add("+Num+Card")       //skip all numbers because of the commented crap below
//            .add("ikibin+Num").add("sekizonikibindokuzyuzdoksansekiz").add("onsekiz").add("onyedi")
//            .add("doksandokuz").add("bindokuzyüzseksendokuz").add("onbirbindokuzyüzdoksansekiz")
//            .add("binyediyüzotuzdört").add("onbir")
            .add("onyedi+Num+Ord").add("kırkyedi")
            .add("ağbi+Noun")
            .add("birbuçuk+Num+Real").add("ikibuçuk+Num+Real").add("binüçyüz+Num+Real")
            .add("birbuçuk+Noun")
            .add("case+Noun")
            .add("flaster+Noun")
            .add("toplusözleşme+Noun").add("anayol+Noun").add("ağabeyi+Noun")
            .add("system+Noun")
            .add("planjon+Noun").add("papiş+Noun").add("ortakokul+Noun").add("praznik+Noun").add("gözbebek+Noun+")
            .add("hoşkal+Noun").add("lordlar+Noun").add("işyeri+Noun").add("yanıbaş+Noun").add("karayol+Noun")
            .add("sözet+Verb").add("terket+Verb").add("varetme+Noun").add("yeral+Verb").add("yolaç+Verb").add("elatma+Noun")
            .add("ayırdet+Verb")
            .add("bastırıvemiş")

            .add("_").add("+Prop").add("+Abbr+")

            // -sel
            .add("dinsel+Adj").add("(1,\"toplumsal+Adj\")").add("kişisel+Adj").add("tarihsel").add("içgüdüsel")
            .add("matematiksel").add("mantıksal").add("deneysel").add("gözlemsel").add("kimyasal")
            .add("ereksel").add("nedensel").add("fiziksel").add("bütünsel").add("duygusal").add("ruhsal")
            .add("kavramsal").add("nesnel+Adj").add("algısal").add("içsel").add("geleneksel").add("madensel")
            .add("hukuksal").add("parasal")

            .build();
}