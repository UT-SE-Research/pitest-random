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

// public class RandomGrouper implements MutationGrouper {

//     private final int unitSize;
//     private final Random random;

//     public RandomGrouper(final int unitSize, final Random random) {
//         this.unitSize = unitSize;
//         this.random = random;
//     }

// //    @Override
// //    public List<List<MutationDetails>> groupMutations(
// //            final Collection<ClassName> codeClasses,
// //            final Collection<MutationDetails> mutations) {

// //        List<MutationDetails> mutationList = new ArrayList<>(mutations);
// //        Collections.shuffle(mutationList, random);
// //        final List<List<MutationDetails>> chunked = new ArrayList<>();
// //        chunked.add(mutationList);

// //        if (this.unitSize > 0) {
// //            for (int i = 0; i < mutationList.size(); i += unitSize) {
// //                int end = Math.min(i + unitSize, mutationList.size());
// //                chunked.add(new ArrayList<>(mutationList.subList(i, end)));
// //            }
// //        } else {
// //            chunked.add(new ArrayList<>(mutationList));
// //        }

// //        return chunked;
// //    }

//     @Override
//     public List<List<MutationDetails>> groupMutations(
//             final Collection<ClassName> codeClasses,
//             final Collection<MutationDetails> mutations) {
//         final Map<ClassName, Collection<MutationDetails>> bucketed = FCollection
//                 .bucket(mutations, MutationDetails::getClassName);
//         final List<List<MutationDetails>> chunked = new ArrayList<>();
//         for (final Map.Entry<ClassName,Collection<MutationDetails>> each : bucketed.entrySet()) {
//             shrinkToMaximumUnitSize(chunked, each.getValue());
//         }
//         List<List<MutationDetails>> oneChunked = new ArrayList<>();
//         oneChunked.add(new ArrayList<>());
//         for (List<MutationDetails> mutationDetails : chunked) {
//             oneChunked.get(0).addAll(mutationDetails);
//         }
//         return oneChunked;
//     }

//     private void shrinkToMaximumUnitSize(
//             final List<List<MutationDetails>> chunked,
//             final Collection<MutationDetails> each) {
//         if (this.unitSize > 0) {
//             for (final List<MutationDetails> ms : FCollection.splitToLength(
//                     this.unitSize, each)) {
//                 chunked.add(ms);
//             }
//         } else {
//             chunked.add(new ArrayList<>(each));
//         }
//     }
// }



public class RandomGrouper implements MutationGrouper {

    private final int numberOfGroups;
    private final Random random;

    public RandomGrouper(final int numberOfGroups, final Random random) {
        this.numberOfGroups = numberOfGroups;
        this.random = random;
    }

    @Override
    public List<List<MutationDetails>> groupMutations(
            final Collection<ClassName> codeClasses,
            final Collection<MutationDetails> mutations) {

        System.out.println("numberOfGroups: " + this.numberOfGroups);
        
        final Map<ClassName, Collection<MutationDetails>> bucketed = 
            FCollection.bucket(mutations, MutationDetails::getClassName);

        // 将每个类的变异存入列表
        List<Collection<MutationDetails>> classMutations = new ArrayList<>(bucketed.values());
        
        // 准备最终分组的容器，每组是一个List<MutationDetails>
        List<List<MutationDetails>> groups = new ArrayList<>();
        for (int i = 0; i < numberOfGroups; i++) {
            groups.add(new ArrayList<>());
        }

        // 计算总的变异数量和每组期望的最小变异数
        int totalMutations = mutations.size();
        int expectedSizePerGroup = totalMutations / numberOfGroups;
        int extraMutations = totalMutations % numberOfGroups; // 用来分配到某些组中

        // 用于记录当前正在处理的组的索引
        int currentGroupIndex = 0;
        
        // 按照类分配变异，尽量使组的大小均匀
        for (Collection<MutationDetails> classMutation : classMutations) {
            // 获取当前组的引用
            List<MutationDetails> currentGroup = groups.get(currentGroupIndex);

            // 如果当前组的变异数已达到期望大小，切换到下一个组
            if (currentGroup.size() >= expectedSizePerGroup + (currentGroupIndex < extraMutations ? 1 : 0)) {
                currentGroupIndex++;
                currentGroup = groups.get(currentGroupIndex);
            }

            // 把当前类的所有变异加到当前组
            currentGroup.addAll(classMutation);
        }

        int i = 1;
        for (List<MutationDetails> tmp: groups) {
            System.out.println("group" + i + ": " + tmp.size());
            i++;
        }

        return groups;
    }
}
