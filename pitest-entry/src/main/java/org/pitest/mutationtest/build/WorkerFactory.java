package org.pitest.mutationtest.build;

import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.EngineArguments;
import org.pitest.mutationtest.MutationConfig;
import org.pitest.mutationtest.TimeoutLengthStrategy;
import org.pitest.mutationtest.config.ClientPluginServices;
import org.pitest.mutationtest.config.MinionSettings;
import org.pitest.mutationtest.config.TestPluginArguments;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.execute.DescriptorCachingTestUnitProvider;
import org.pitest.mutationtest.execute.MinionArguments;
import org.pitest.mutationtest.execute.MutationTestProcess;
import org.pitest.process.ProcessArgs;
import org.pitest.testapi.Configuration;
import org.pitest.testapi.TestUnitDescriptor;
import org.pitest.util.Log;
import org.pitest.util.SocketFinder;
import org.pitest.util.Verbosity;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.List;

import static org.pitest.functional.prelude.Prelude.printlnWith;

public class WorkerFactory {

  private final String                classPath;
  private final File                  baseDir;
  private final TestPluginArguments   pitConfig;
  private final TimeoutLengthStrategy timeoutStrategy;
  private final Verbosity             verbosity;
  private final boolean               fullMutationMatrix;
  private final MutationConfig        config;
  private final EngineArguments       args;

  private final boolean               randomGroup;
  private final DescriptorCachingTestUnitProvider descriptorProvider;
  private final ClassLoader                       loader;

  public WorkerFactory(
          final File baseDir,
          final TestPluginArguments pitConfig,
          final MutationConfig mutationConfig,
          final EngineArguments args,
          final TimeoutLengthStrategy timeoutStrategy,
          final Verbosity verbosity,
          final boolean fullMutationMatrix,
          final String classPath,
          final boolean randomGroup,
          final ClassLoader loader
  ) {
    this.pitConfig = pitConfig;
    this.timeoutStrategy = timeoutStrategy;
    this.verbosity = verbosity;
    this.fullMutationMatrix = fullMutationMatrix;
    this.classPath = classPath;
    this.baseDir = baseDir;
    this.config = mutationConfig;
    this.args = args;
    this.randomGroup = randomGroup;
    this.loader = loader;
    ClientPluginServices cps = ClientPluginServices.makeForContextLoader();
    MinionSettings minionSettings = new MinionSettings(cps);
    Configuration testConfig = minionSettings.getTestFrameworkPlugin(
            this.pitConfig,
            ClassloaderByteArraySource.fromContext()
    );

    this.descriptorProvider = new DescriptorCachingTestUnitProvider(testConfig);
  }

  public MutationTestProcess createWorker(
      final Collection<MutationDetails> remainingMutations,
      final Collection<ClassName> testClasses) {
    final List<TestUnitDescriptor> descriptorList =
            this.descriptorProvider.getDescriptorsFor(testClasses, this.loader);

    final MinionArguments fileArgs = new MinionArguments(
            remainingMutations, testClasses, this.config.getEngine().getName(),
            this.args, this.timeoutStrategy, Log.verbosity(), this.fullMutationMatrix,
            this.pitConfig, this.randomGroup, descriptorList
    );

    final ProcessArgs args = ProcessArgs.withClassPath(this.classPath)
        .andLaunchOptions(this.config.getLaunchOptions())
        .andBaseDir(this.baseDir).andStdout(captureStdOutIfVerbose())
        .andStderr(captureStdErrIfVerbose());

    final SocketFinder sf = new SocketFinder();
    return new MutationTestProcess(
        sf.getNextAvailableServerSocket(), args, fileArgs);
  }

  private Consumer<String> captureStdOutIfVerbose() {
    if (this.verbosity.showMinionOutput()) {
      return Prelude.noSideEffect(String.class);
      // don't know why there will be much useless stdout when show minion outpus
      //      return printlnWith("stdout ");
    } else {
      return Prelude.noSideEffect(String.class);
    }
  }

  private Consumer<String> captureStdErrIfVerbose() {
    if (this.verbosity.showMinionOutput()) {
      return printlnWith("stderr ");
    } else {
      return Prelude.noSideEffect(String.class);
    }
  }

}
