/*
 * Copyright 2010 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.pitest.mutationtest.execute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.pitest.coverage.TestInfo;
import org.pitest.mutationtest.TimeoutLengthStrategy;
import org.pitest.testapi.TestUnit;
import org.pitest.util.Log;

public class TimeOutDecoratedTestSource {

  private final Map<String, TestUnit> allTests = new HashMap<>();
  private final TimeoutLengthStrategy timeoutStrategy;
  private final Reporter              r;

  private static final Logger LOG = Log.getLogger();

  public TimeOutDecoratedTestSource(
      final TimeoutLengthStrategy timeoutStrategy,
      final List<TestUnit> allTests, final Reporter r) {
    this.timeoutStrategy = timeoutStrategy;
    mapTests(allTests);
    this.r = r;
  }

  private void mapTests(final List<TestUnit> tests) {
    for (final TestUnit each : tests) {
      this.allTests.put(each.getDescription().getQualifiedName(), each);
    }
  }

  public List<TestUnit> translateTests(final List<TestInfo> testsInOrder) {
    return testsInOrder.stream().flatMap(testToTestUnit()).collect(Collectors.toList());
  }

  private Function<TestInfo, Stream<TestUnit>> testToTestUnit() {
    return a -> {
      String name = a.getName();
      TestUnit tu = TimeOutDecoratedTestSource.this.allTests.get(name);

      if (tu == null) {
        int paren = name.indexOf('(');
        String noParams = (paren != -1) ? name.substring(0, paren) : name;

        if (!noParams.equals(name)) {
          tu = TimeOutDecoratedTestSource.this.allTests.get(noParams);
//          LOG.info("DEBUG TRANS: fallback1 " + name + " -> " + noParams + " => " + (tu != null));
        }

        if (tu == null) {
          int lastDot = noParams.lastIndexOf('.');
          if (lastDot != -1) {
            String className = noParams.substring(0, lastDot);
            tu = TimeOutDecoratedTestSource.this.allTests.get(className);
//            LOG.info("DEBUG TRANS: fallback2 " + name + " -> " + className + " => " + (tu != null));
          }
        }
      }

      if (tu != null) {
        return Stream.of(
                new MutationTimeoutDecorator(
                        tu,
                        new TimeOutSystemExitSideEffect(TimeOutDecoratedTestSource.this.r),
                        TimeOutDecoratedTestSource.this.timeoutStrategy,
                        a.getTime()
                )
        );
      }

//      LOG.info("DEBUG TRANS: still no TestUnit for `" + name + "`");
      return Stream.empty();
    };
  }


}
