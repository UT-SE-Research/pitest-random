package org.pitest.mutationtest.build;


import org.pitest.classpath.CodeSource;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.plugin.ToolClasspathPlugin;

public interface MutationGrouperFactory extends ToolClasspathPlugin {

  MutationGrouper makeFactory(CodeSource codeSource, ReportOptions data);
}
