package org.pitest.mutationtest.execute;

import org.pitest.boot.HotSwapAgent;
import org.pitest.classinfo.ClassName;
import org.pitest.util.Log;
import org.pitest.util.Unchecked;
import org.pitest.util.Verbosity;

import java.util.logging.Logger;

/**
 * Since pitest 1.9.4 there is an implicit assumption that pitest will never mutate
 * more than once class within the same jvm. If this assumption is ever broken, mutants
 * from the previous class would remain active and invalidate results.
 */
class HotSwap {

  private static final Logger LOG = Log.getLogger();

  public Boolean insertClass(final ClassName clazzName, ClassLoader loader, final byte[] mutantBytes) {
    try {
      final long t0 = System.nanoTime();

      // Some frameworks (eg quarkus) run tests in non delegating
      // classloaders. Need to make sure these are transformed too
      CatchNewClassLoadersTransformer.setMutant(clazzName.asInternalName(), mutantBytes);
      final long t1 = System.nanoTime();

      // trigger loading for the current loader
      Class<?> clazz = Class.forName(clazzName.asJavaName(), false, loader);
      final long t2 = System.nanoTime();

      // will still need to explicitly swap it... not clear why the transformed does not do this
      final Boolean r = HotSwapAgent.hotSwap(clazz, mutantBytes);
      final long t3 = System.nanoTime();

      // Match your existing style: log only under RANDOM_VERBOSE
      if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
        final long setNs = t1 - t0;
        final long forNameNs = t2 - t1;
        final long hotSwapNs = t3 - t2;
        final long totalNs = t3 - t0;

        // include loader identity to detect "new loader per mutant"
        final int loaderId = System.identityHashCode(loader);
        final String loaderType = (loader == null) ? "null" : loader.getClass().getName();

        LOG.info(
                "RANDOM LOG: HotSwap.insertClass clazz=" + clazzName.asJavaName()
                        + " loader=" + loaderType + "@" + Integer.toHexString(loaderId)
                        + " bytes=" + (mutantBytes == null ? -1 : mutantBytes.length)
                        + " setMutant=" + setNs + "ns"
                        + " forName=" + forNameNs + "ns"
                        + " hotSwap=" + hotSwapNs + "ns"
                        + " total=" + totalNs + "ns"
        );
      }
      return r;

    } catch (final ClassNotFoundException e) {
      throw Unchecked.translateCheckedException(e);
    }

  }

}
