package org.pitest.mutationtest.build;


import org.pitest.classpath.CodeSource;
import org.pitest.mutationtest.config.ReportOptions;

public class DefaultMutationGrouperFactory implements MutationGrouperFactory {

  @Override
  public String description() {
    return "Default mutation grouping";
  }

  @Override
  public MutationGrouper makeFactory(final CodeSource codeSource, final ReportOptions data) {
    return new DefaultGrouper(data.getMutationUnitSize());
  }

}
