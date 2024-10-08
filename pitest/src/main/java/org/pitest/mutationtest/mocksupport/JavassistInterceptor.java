/*
 * Copyright 2011 Henry Coles
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
package org.pitest.mutationtest.mocksupport;

import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.reflection.Reflection;
import org.pitest.util.Unchecked;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class JavassistInterceptor {

  private JavassistInterceptor() {

  }

  private static Mutant mutant;

  private static byte[] mutantBytes;

  private static ClassName mutantName;

  public static InputStream openClassfile(final Object classPath, // NO_UCD
      final String name) {

    if (isMutatedClass(name)) {
      return  new ByteArrayInputStream(
          mutantBytes);
    } else {
      return returnNormalBytes(classPath, name);
    }

  }

  private static InputStream returnNormalBytes(final Object classPath,
      final String name) {
    try {
      return (InputStream) Reflection.publicMethod(classPath.getClass(),
          "openClassfile").invoke(classPath, name);
    } catch (final Exception e) {
      throw Unchecked.translateCheckedException(e);
    }
  }

  private static boolean isMutatedClass(final String name) {
    return mutantName != null && mutantName.equals(ClassName.fromString(name));
  }

  public static void setMutant(final Mutant newMutant) {
    mutant = newMutant;
    if (mutant != null) {
      mutantBytes = mutant.getBytes();
      mutantName = mutant.getDetails().getClassName();
    }
  }

  public static void setBytesAndName(byte[] bytes, ClassName name) {
    mutantBytes = bytes;
    mutantName = name;
  }
}
