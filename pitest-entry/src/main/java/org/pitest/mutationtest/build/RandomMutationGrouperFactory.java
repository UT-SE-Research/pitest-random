package org.pitest.mutationtest.build;

import org.pitest.classpath.CodeSource;
import org.pitest.mutationtest.config.ReportOptions;


// public class RandomMutationGrouperFactory implements MutationGrouperFactory {
//     @Override
//     public String description() {
//         return "Random mutation grouping";
//     }

//     @Override
//     public MutationGrouper makeFactory(final CodeSource codeSource, final ReportOptions data) {
//         return new RandomGrouper(data.getMutationUnitSize(), data.getPitestRandom());
//     }
// }

public class RandomMutationGrouperFactory implements MutationGrouperFactory {
    @Override
    public String description() {
        return "Random mutation grouping";
    }

    @Override
    public MutationGrouper makeFactory(final CodeSource codeSource, final ReportOptions data) {
        return new RandomGrouper(data.getNumberOfGroups(), data.getPitestRandom());
    }
}
