package org.pitest.mutationtest.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.pitest.mutationtest.environment.EnvironmentResetPlugin;
import org.pitest.mutationtest.MutationEngineFactory;
import org.pitest.mutationtest.environment.TransformationPlugin;
import org.pitest.testapi.TestPluginFactory;
import org.pitest.util.IsolationUtils;
import org.pitest.util.ServiceLoader;

public class ClientPluginServices {
  private final ClassLoader loader;

  public ClientPluginServices(ClassLoader loader) {
    this.loader = loader;
  }

  public static ClientPluginServices makeForContextLoader() {
    return new ClientPluginServices(IsolationUtils.getContextClassLoader());
  }

  Collection<? extends TestPluginFactory> findTestFrameworkPlugins() {
    List<TestPluginFactory> plugins = new ArrayList<>();

    Iterable<?> loaded = ServiceLoader.load(TestPluginFactory.class, this.loader);

    for (Object o : loaded) {
      plugins.add((TestPluginFactory) o);
    }

    if (plugins.isEmpty()) {
      try {
        Class<?> c = Class.forName("org.pitest.junit.JUnitTestPlugin", false, this.loader);
        Object instance = c.getDeclaredConstructor().newInstance();
        plugins.add((TestPluginFactory) instance);
      } catch (Exception e) {
        // ignore: if fallback also fails, MinionSettings will throw NO_TEST_PLUGIN
      }
    }

    return plugins;
  }

  Collection<? extends MutationEngineFactory> findMutationEngines() {
    return ServiceLoader.load(MutationEngineFactory.class, this.loader);
  }

  Collection<? extends EnvironmentResetPlugin> findResets() {
    return ServiceLoader.load(EnvironmentResetPlugin.class, this.loader);
  }

  public Collection<TransformationPlugin> findTransformations() {
    return ServiceLoader.load(TransformationPlugin.class, this.loader);
  }

}
