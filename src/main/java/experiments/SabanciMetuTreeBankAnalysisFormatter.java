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
public class SabanciMetuTreeBankAnalysisFormatter implements AnalysisFormatter {

    private boolean addIndices;

    public SabanciMetuTreeBankAnalysisFormatter(boolean addIndices) {
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
                    nextGroupPos = surfaces.get(i + 1).morpheme.pos.getStringForm();
                }
                currentGroup = new ArrayList<>(Arrays.asList(nextGroupPos));
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

        return builder.toString();
    }

    private static final ImmutableSet<Pair<PrimaryPos, SecondaryPos>> DERIVATION_GROUPING_FORMAT_SECONDARY_POS_TO_SKIP
            = new ImmutableSet.Builder<Pair<PrimaryPos, SecondaryPos>>()
            //.add(Pair.of(PrimaryPos.Adverb, SecondaryPos.Question))
            .add(Pair.of(PrimaryPos.Adverb, SecondaryPos.Time))
            //.add(Pair.of(PrimaryPos.Adjective, SecondaryPos.Question))
            .build();
}
