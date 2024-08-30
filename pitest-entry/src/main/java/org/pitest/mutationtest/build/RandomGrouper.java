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

    private final int unitSize;
    private final Random random;

    public RandomGrouper(final int unitSize, final Random random) {
        this.unitSize = unitSize;
        this.random = random;
    }

//    @Override
//    public List<List<MutationDetails>> groupMutations(
//            final Collection<ClassName> codeClasses,
//            final Collection<MutationDetails> mutations) {
//
//        List<MutationDetails> mutationList = new ArrayList<>(mutations);
////        Collections.shuffle(mutationList, random);
//        final List<List<MutationDetails>> chunked = new ArrayList<>();
//        chunked.add(mutationList);
////
////        if (this.unitSize > 0) {
////            for (int i = 0; i < mutationList.size(); i += unitSize) {
////                int end = Math.min(i + unitSize, mutationList.size());
////                chunked.add(new ArrayList<>(mutationList.subList(i, end)));
////            }
////        } else {
////            chunked.add(new ArrayList<>(mutationList));
////        }
//
//        return chunked;
//    }

    @Override
    public List<List<MutationDetails>> groupMutations(
            final Collection<ClassName> codeClasses,
            final Collection<MutationDetails> mutations) {
        final Map<ClassName, Collection<MutationDetails>> bucketed = FCollection
                .bucket(mutations, MutationDetails::getClassName);
        final List<List<MutationDetails>> chunked = new ArrayList<>();
        for (final Map.Entry<ClassName,Collection<MutationDetails>> each : bucketed.entrySet()) {
            shrinkToMaximumUnitSize(chunked, each.getValue());
        }
        List<List<MutationDetails>> oneChunked = new ArrayList<>();
        oneChunked.add(new ArrayList<>());
        for (List<MutationDetails> mutationDetails : chunked) {
            oneChunked.get(0).addAll(mutationDetails);
        }



        return oneChunked;
    }

    private void shrinkToMaximumUnitSize(
            final List<List<MutationDetails>> chunked,
            final Collection<MutationDetails> each) {
        if (this.unitSize > 0) {
            for (final List<MutationDetails> ms : FCollection.splitToLength(
                    this.unitSize, each)) {
                chunked.add(ms);
            }
        } else {
            chunked.add(new ArrayList<>(each));
        }
    }
}
