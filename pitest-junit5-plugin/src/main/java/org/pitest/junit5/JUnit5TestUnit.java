package org.pitest.junit5;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.pitest.testapi.AbstractTestUnit;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;

public class JUnit5TestUnit extends AbstractTestUnit {

    private final Class<?> testClass;
    private final String uniqueId;
    private final String displayName;
    private final TestIdentifier testIdentifier;

    // ---------- static launcher + delegating listener ----------

    private static volatile Launcher CACHED_LAUNCHER;
    private static final Object LAUNCHER_LOCK = new Object();
    private static final DelegatingTestExecutionListener DELEGATING_LISTENER =
            new DelegatingTestExecutionListener();

    private static Launcher getOrCreateLauncher() {
        Launcher local = CACHED_LAUNCHER;
        if (local == null) {
            synchronized (LAUNCHER_LOCK) {
                local = CACHED_LAUNCHER;
                if (local == null) {
                    local = LauncherFactory.create();
                    // 只注册一次全局 listener，后续通过它把事件转发给当前的 ResultCollector
                    local.registerTestExecutionListeners(DELEGATING_LISTENER);
                    CACHED_LAUNCHER = local;
                }
            }
        }
        return local;
    }

    /**
     * 静态 listener，通过 AtomicReference 保存“当前这次执行”的上下文，
     * 避免重复注册大量 listener。
     */
    private static final class DelegatingTestExecutionListener implements TestExecutionListener {

        private final AtomicReference<ResultCollector> currentCollector = new AtomicReference<>();
        private final AtomicReference<Class<?>> currentTestClass = new AtomicReference<>();

        void setContext(ResultCollector rc, Class<?> clazz) {
            currentCollector.set(rc);
            currentTestClass.set(clazz);
        }

        void clearContext() {
            currentCollector.set(null);
            currentTestClass.set(null);
        }

        @Override
        public void executionSkipped(TestIdentifier ti, String reason) {
            ResultCollector rc = currentCollector.get();
            Class<?> clazz = currentTestClass.get();
            if (rc != null && clazz != null && ti.isTest()) {
                rc.notifySkipped(new Description(ti.getUniqueId(), clazz));
            }
        }

        @Override
        public void executionStarted(TestIdentifier ti) {
            ResultCollector rc = currentCollector.get();
            Class<?> clazz = currentTestClass.get();
            if (rc != null && clazz != null && ti.isTest()) {
                rc.notifyStart(new Description(ti.getUniqueId(), clazz));
            }
        }

        @Override
        public void executionFinished(TestIdentifier ti, TestExecutionResult result) {
            ResultCollector rc = currentCollector.get();
            Class<?> clazz = currentTestClass.get();
            if (rc == null || clazz == null) {
                return;
            }

            Optional<Throwable> throwable = result.getThrowable();
            if (ti.isTest()) {
                if (result.getStatus() == TestExecutionResult.Status.ABORTED) {
                    // assumptions → 当 success 处理
                    rc.notifyEnd(new Description(ti.getUniqueId(), clazz));
                } else if (throwable.isPresent()) {
                    rc.notifyEnd(new Description(ti.getUniqueId(), clazz), throwable.get());
                } else {
                    rc.notifyEnd(new Description(ti.getUniqueId(), clazz));
                }
            } else {
                // BeforeAll fail 的 container
                throwable.ifPresent(value ->
                        rc.notifyEnd(new Description(ti.getUniqueId(), clazz), value));
            }
        }
    }

    // ---------- constructors & getters ----------

    public JUnit5TestUnit(Class<?> testClass, TestIdentifier testIdentifier) {
        this(testClass, testIdentifier.getUniqueId(), testIdentifier.getDisplayName(), testIdentifier);
    }

    public JUnit5TestUnit(Class<?> testClass, String uniqueId, String displayName) {
        this(testClass, uniqueId, displayName, null);
    }

    private JUnit5TestUnit(Class<?> testClass,
                           String uniqueId,
                           String displayName,
                           TestIdentifier testIdentifier) {
        super(new Description(uniqueId, testClass));
        this.testClass = testClass;
        this.uniqueId = uniqueId;
        this.displayName = displayName;
        this.testIdentifier = testIdentifier;
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TestIdentifier getTestIdentifier() {
        return testIdentifier;
    }

    // ---------- execute ----------

    @Override
    public void execute(ResultCollector resultCollector) {
        Launcher launcher = getOrCreateLauncher();

        // 告诉全局 listener：这一次执行要把事件发给哪个 ResultCollector、对应哪个 testClass
        DELEGATING_LISTENER.setContext(resultCollector, testClass);
        try {
            LauncherDiscoveryRequest launcherDiscoveryRequest =
                    LauncherDiscoveryRequestBuilder.request()
                            .selectors(DiscoverySelectors.selectUniqueId(uniqueId))
                            .build();

            launcher.execute(launcherDiscoveryRequest);
        } finally {
            // 清理上下文，防止后续误用
            DELEGATING_LISTENER.clearContext();
        }
    }

    @Override
    public String toString() {
        return "JUnit5TestUnit [uniqueId=" + uniqueId
                + ", displayName=" + displayName + "]";
    }
}
