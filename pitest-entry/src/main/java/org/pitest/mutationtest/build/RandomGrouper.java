package org.pitest.mutationtest.build;

import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomGrouper implements MutationGrouper {

    private final int unitSize;
    private final Random random;

    public RandomGrouper(final int unitSize, final Random random) {
        this.unitSize = unitSize;
        this.random = random;
    }

    @Override
    public List<List<MutationDetails>> groupMutations(
            final Collection<ClassName> codeClasses,
            final Collection<MutationDetails> mutations) {

        List<MutationDetails> mutationList = new ArrayList<>(mutations);
        Collections.shuffle(mutationList, random); // 随机打乱所有的 MutationDetails
        final List<List<MutationDetails>> chunked = new ArrayList<>();

        if (this.unitSize > 0) {
            for (int i = 0; i < mutationList.size(); i += unitSize) {
                int end = Math.min(i + unitSize, mutationList.size());
                chunked.add(new ArrayList<>(mutationList.subList(i, end)));
            }
        } else {
            chunked.add(new ArrayList<>(mutationList));
        }

        return chunked;
    }
}
