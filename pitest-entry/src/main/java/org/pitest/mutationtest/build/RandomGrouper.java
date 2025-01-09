package org.pitest.mutationtest.build;

import org.pitest.classinfo.ClassName;
import org.pitest.functional.FCollection;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


public class RandomGrouper implements MutationGrouper {

    @Override
    public List<List<MutationDetails>> groupMutations(final Collection<ClassName> codeClasses,
                                                      final Collection<MutationDetails> mutations) {
        final Map<ClassName, Collection<MutationDetails>> bucketed = FCollection.
            bucket(mutations, MutationDetails::getClassName);

        List<Collection<MutationDetails>> classMutations = new ArrayList<>(bucketed.values());
        int maxGroupNumber = classMutations.stream()
            .flatMap(Collection::stream)
            .mapToInt(MutationDetails::getGroupNumber)
            .max()
            .orElse(0);
        int numberOfGroups = maxGroupNumber + 1;
        List<List<MutationDetails>> groups = new ArrayList<>();
        for (int i = 0; i < numberOfGroups; i++) {
            groups.add(new ArrayList<>());
        }

        for (Collection<MutationDetails> classMutation : classMutations) {
            List<MutationDetails> mutationDetailsList = (List<MutationDetails>) classMutation;
            int currentIndex = mutationDetailsList.get(0).getGroupNumber();
            groups.get(currentIndex).addAll(mutationDetailsList);
        }

        for (int i = 0; i < numberOfGroups; i++) {
            groups.get(i).sort(Comparator.comparingInt(MutationDetails::getClassCount)
                .thenComparingInt(MutationDetails::getExecutionSequenceNumber));
        }
        return groups;
    }
}
