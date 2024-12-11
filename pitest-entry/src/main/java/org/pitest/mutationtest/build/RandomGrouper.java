package org.pitest.mutationtest.build;

import org.pitest.classinfo.ClassName;
import org.pitest.functional.FCollection;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.ArrayList;
import java.util.Collection;
//import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomGrouper implements MutationGrouper {

    private final int numberOfGroups;
    private final Random random;

    public RandomGrouper(final int numberOfGroups, final Random random) {
        this.numberOfGroups = numberOfGroups;
        this.random = random;
    }

    @Override
    public List<List<MutationDetails>> groupMutations(final Collection<ClassName> codeClasses,
                                                      final Collection<MutationDetails> mutations) {
        System.out.println("numberOfGroups: " + numberOfGroups);

        final Map<ClassName, Collection<MutationDetails>> bucketed = FCollection.
            bucket(mutations, MutationDetails::getClassName);

        List<Collection<MutationDetails>> classMutations = new ArrayList<>(bucketed.values());
        List<List<MutationDetails>> groups = new ArrayList<>();
        for (int i = 0; i < numberOfGroups; i++) {
            groups.add(new ArrayList<>());
        }

        int[] sizes = new int[numberOfGroups];
        for (Collection<MutationDetails> classMutation : classMutations) {
            int minIndex = 0;
            for (int i = 1; i < numberOfGroups; i++) {
                if (sizes[i] < sizes[minIndex]) {
                    minIndex = i;
                }
            }
            groups.get(minIndex).addAll(classMutation);
            sizes[minIndex] += classMutation.size();
        }

        int i = 1;
        for (List<MutationDetails> group: groups) {
            System.out.println("group" + i + ": " + group.size());
            i++;
        }
        return groups;
    }
}
