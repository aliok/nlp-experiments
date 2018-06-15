package experiments;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ali Ok (ali.ok@apache.org)
 * 6/15/18 12:40 AM
 * <p>
 * (1, " kitap + Noun + A3sg + Pnon + Dat ") or ("kitap+Noun+A3sg+Pnon+Dat")
 **/
@SuppressWarnings("Duplicates")
public class SabanciMetuTreeBankAnalysisFormatter2 implements AnalysisFormatter {

    private boolean addIndices;

    public SabanciMetuTreeBankAnalysisFormatter2(boolean addIndices) {
        this.addIndices = addIndices;
    }

    @Override
    public String format(SingleAnalysis analysis) {
        final List<List<String>> groups = new ArrayList<List<String>>();

        final DictionaryItem dictionaryItem = analysis.getDictionaryItem();
        final String lemmaRoot = dictionaryItem.root;
        final PrimaryPos primaryPos = dictionaryItem.primaryPos;
        final SecondaryPos secondaryPos = dictionaryItem.secondaryPos;

        final String secondaryPosStr;
        if (secondaryPos != null && secondaryPos != SecondaryPos.None) {
            if (DERIVATION_GROUPING_FORMAT_SECONDARY_POS_TO_SKIP.contains(Pair.of(primaryPos, secondaryPos)))
                secondaryPosStr = null;
            else
                secondaryPosStr = secondaryPos.getStringForm();
        } else {
            secondaryPosStr = null;
        }

        final String formattedLexeme = Joiner.on("+").skipNulls().join(Arrays.asList(lemmaRoot, primaryPos.getStringForm(), secondaryPosStr));

        List<String> currentGroup = new ArrayList<String>(Arrays.asList(formattedLexeme));

        List<SingleAnalysis.MorphemeData> surfaces = analysis.getMorphemeDataList();
        for (int i = 1; i < surfaces.size(); i++) {
            SingleAnalysis.MorphemeData s = surfaces.get(i);
            Morpheme morpheme = s.morpheme;

            if (morpheme.derivational) {
                groups.add(currentGroup);
                String nextGroupPos = "";
                if (surfaces.size() >= i + 1) {
                    final PrimaryPos nextPos = surfaces.get(i + 1).morpheme.pos;
                    nextGroupPos = nextPos.getStringForm();
                }
                currentGroup = new ArrayList<>(Arrays.asList(nextGroupPos));
                i++;
            }

            currentGroup.add(s.morpheme.id);
        }

        groups.add(currentGroup);

        final List<String> formattedGroups = Lists.transform(groups, new Function<List<String>, String>() {
            @Override
            public String apply(List<String> input) {
                return Joiner.on("+").join(input);
            }
        });

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < formattedGroups.size(); i++) {
            final String formattedGroup = formattedGroups.get(i);
            builder.append("(");
            if (addIndices)
                builder.append(i + 1).append(",");
            builder.append("\"").append(formattedGroup).append("\")");
        }

        final String base = builder.toString();
        return applyRules(base);
    }

    private static final ImmutableSet<Pair<PrimaryPos, SecondaryPos>> DERIVATION_GROUPING_FORMAT_SECONDARY_POS_TO_SKIP
            = new ImmutableSet.Builder<Pair<PrimaryPos, SecondaryPos>>()
            //.add(Pair.of(PrimaryPos.Adverb, SecondaryPos.Question))
            .add(Pair.of(PrimaryPos.Adverb, SecondaryPos.Time))
            //.add(Pair.of(PrimaryPos.Adjective, SecondaryPos.Question))
            .build();

    private String applyRules(String base) {
        base = base.replace("+Noun+Time+A3sg\"", "+Noun+Time+A3sg+Pnon+Nom\"");  // gun

        base = base.replace("Pass+Narr", "Pass+Pos+Narr");  // taranmis

        base = base.replace("değil+Verb+Neg+", "değil+Verb+");  // değil

        base = base.replace("Verb+Past", "Verb+Pos+Past");  // oldu
        base = base.replace("Verb+Prog1", "Verb+Pos+Prog1");  // oluyor
        base = base.replace("Verb+Fut", "Verb+Pos+Fut");  // olacak
        base = base.replace("Verb+Aor", "Verb+Pos+Aor");  // olur
        base = base.replace("Verb+Imp", "Verb+Pos+Imp");  // olsun
        base = base.replace("Verb+Desr", "Verb+Pos+Desr");  // olsa
        base = base.replace("Verb+Narr", "Verb+Pos+Narr");  // olmuş

        base = base.replace("Noun+Inf1+A3sg\"", "Noun+Inf1+A3sg+Pnon+Nom\"");  // olmak
        base = base.replace("Noun+Inf2+A3sg\"", "Noun+Inf2+A3sg+Pnon+Nom\"");  // olma
        base = base.replace("Noun+Inf2+A3pl\"", "Noun+Inf2+A3pl+Pnon+Nom\"");  // olmalar

        base = base.replace("Ness+A3sg\"", "Ness+A3sg+Pnon+Nom\"");  // cocukluk

        base = base.replace("+Verb\")(2,\"Adj", "+Verb+Pos\")(2,\"Adj");  // olan
        base = base.replace("+Verb\")(2,\"Adv", "+Verb+Pos\")(2,\"Adv");  // olarak
        base = base.replace("\"Adv+ByDoingSo+Adv\"", "\"Adv+ByDoingSo\"");  // olarak
        base = base.replace("+Verb\")(2,\"Noun", "+Verb+Pos\")(2,\"Noun");  // olduğunu


        base = base.replace("Verb+Pass\")(3,\"Adj", "Verb+Pass+Pos\")(3,\"Adj");  // yapılan

        base = base.replace("+Noun+A3sg\"", "+Noun+A3sg+Pnon+Nom\"");  // sey
        base = base.replace("+Noun+A3pl\"", "+Noun+A3pl+Pnon+Nom\"");  // seyler

        base = base.replace("A3sg+P1sg\"", "A3sg+P1sg+Nom\"");  // seyim
        base = base.replace("A3sg+P2sg\"", "A3sg+P2sg+Nom\"");  // seyin
        base = base.replace("A3sg+P3sg\"", "A3sg+P3sg+Nom\"");  // seyi
        base = base.replace("A3sg+P1pl\"", "A3sg+P1pl+Nom\"");  // seyimiz
        base = base.replace("A3sg+P2pl\"", "A3sg+P2pl+Nom\"");  // seyiniz
        base = base.replace("A3sg+P3pl\"", "A3sg+P3pl+Nom\"");  // seyleri

        base = base.replace("A3sg+Loc", "A3sg+Pnon+Loc");  // seyde
        base = base.replace("A3sg+Abl", "A3sg+Pnon+Abl");  // seyden
        base = base.replace("A3sg+Ins", "A3sg+Pnon+Ins");  // seyle

        base = base.replace("A3pl+P1sg\"", "A3pl+P1sg+Nom\"");  // seylerim
        base = base.replace("A3pl+P2sg\"", "A3pl+P2sg+Nom\"");  // seylerin
        base = base.replace("A3pl+P3sg\"", "A3pl+P3sg+Nom\"");  // seyleri

        base = base.replace("A3pl+Dat\"", "A3pl+Pnon+Nom\"");  // seylere
        base = base.replace("A3pl+Dat\"", "A3pl+Pnon+Nom\"");  // seylerden

        base = base.replace("Pers+A1sg\"", "Pers+A1sg+Pnon+Nom\"");  // ben
        base = base.replace("Pers+A2sg\"", "Pers+A2sg+Pnon+Nom\"");  // sen
        base = base.replace("Pers+A3sg\"", "Pers+A3sg+Pnon+Nom\"");  // o
        base = base.replace("Pers+A1pl\"", "Pers+A1pl+Pnon+Nom\"");  // biz
        base = base.replace("Pers+A2pl\"", "Pers+A2pl+Pnon+Nom\"");  // siz
        base = base.replace("Pers+A3pl\"", "Pers+A3pl+Pnon+Nom\"");  // siz

        base = base.replace("+A1sg+Dat", "+A1sg+Pnon+Dat");  // bana
        base = base.replace("+A2sg+Dat", "+A2sg+Pnon+Dat");  // sana
        base = base.replace("+A3sg+Dat", "+A3sg+Pnon+Dat");  // ona
        base = base.replace("+A1pl+Dat", "+A1pl+Pnon+Dat");  // bize
        base = base.replace("+A2pl+Dat", "+A2pl+Pnon+Dat");  // size
        base = base.replace("+A3pl+Dat", "+A3pl+Pnon+Dat");  // onlara

        base = base.replace("+A1sg+Acc\"", "+A1sg+Pnon+Acc\"");  // beni
        base = base.replace("+A2sg+Acc\"", "+A2sg+Pnon+Acc\"");  // seni
        base = base.replace("+A3sg+Acc\"", "+A3sg+Pnon+Acc\"");  // onu
        base = base.replace("+A1pl+Acc\"", "+A1pl+Pnon+Acc\"");  // bizi
        base = base.replace("+A2pl+Acc\"", "+A2pl+Pnon+Acc\"");  // sizi
        base = base.replace("+A3pl+Acc\"", "+A3pl+Pnon+Acc\"");  // onlari

        base = base.replace("+A1sg+Gen", "+A1sg+Pnon+Gen");  // benim
        base = base.replace("+A2sg+Gen", "+A2sg+Pnon+Gen");  // senin
        base = base.replace("+A3sg+Gen", "+A3sg+Pnon+Gen");  // onun
        base = base.replace("+A1pl+Gen", "+A1pl+Pnon+Gen");  // bizim
        base = base.replace("+A2pl+Gen", "+A2pl+Pnon+Gen");  // sizin
        base = base.replace("+A3pl+Gen", "+A3pl+Pnon+Gen");  // onlarin

        base = base.replace("Pron+Pers+A3pl+Gen", "Pron+Pers+A3pl+Pnon+Gen");  // onlarin

        base = base.replace("+A3sg+Abl", "+A3sg+Pnon+Abl");  // ondan

        base = base.replace("+Ques+A3sg\"", "+Ques+A3sg+Pnon+Nom\"");  // ne

        base = base.replace("+Ques+A3pl\"", "+Ques+A3pl+Pnon+Nom\"");  // neler

        base = base.replace("+Demons+A3sg\"", "+Demons+A3sg+Pnon+Nom\"");  // bu
        base = base.replace("+Demons+A3pl\"", "+Demons+A3pl+Pnon+Nom\"");  // bunlar
        base = base.replace("+Demons+A3pl+Acc\"", "+Demons+A3pl+Pnon+Acc\"");  // bunlari


        // change ids
        base = base.replace("+AfterDoing", "+AfterDoingSo");  // gidip

        return base;
    }
}
