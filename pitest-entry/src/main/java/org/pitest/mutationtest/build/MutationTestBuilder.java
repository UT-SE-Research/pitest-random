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
package org.pitest.mutationtest.build;

import org.pitest.classinfo.ClassName;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.coverage.NoCoverage;
import org.pitest.coverage.TestInfo;
import org.pitest.mutationtest.History;
import org.pitest.mutationtest.DetectionStatus;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Arrays;

public class MutationTestBuilder {

  private final MutationSource   mutationSource;
  private final History analyser;
  private final WorkerFactory    workerFactory;
  private final MutationGrouper  grouper;

  public MutationTestBuilder(final WorkerFactory workerFactory,
                             final History analyser,
                             final MutationSource mutationSource,
                             final MutationGrouper grouper) {

    this.mutationSource = mutationSource;
    this.analyser = analyser;
    this.workerFactory = workerFactory;
    this.grouper = grouper;
  }

  public List<MutationAnalysisUnit> createMutationTestUnits(
      final Collection<ClassName> codeClasses) {
    final List<MutationAnalysisUnit> tus = new ArrayList<>();

    final List<MutationDetails> mutations = codeClasses.stream()
        .flatMap(c -> mutationSource.createMutations(c).stream())
        .collect(Collectors.toList());

    mutations.sort(comparing(MutationDetails::getId));

    List<MutationResult> analysisUnits = this.analyser.analyse(mutations);

    Collection<MutationDetails> needProcessing = filterAlreadyAnalysedMutations(mutations, analysisUnits);

    List<MutationResult> analysedMutations = analysisUnits.stream()
            .filter(r -> r.getStatus() != DetectionStatus.NOT_STARTED)
            .collect(Collectors.toList());

    if (!analysedMutations.isEmpty()) {
      tus.add(makePreAnalysedUnit(analysedMutations));
    }

    if (!needProcessing.isEmpty()) {
      for (final Collection<MutationDetails> ms : this.grouper.groupMutations(
          codeClasses, needProcessing)) {
        tus.add(makeUnanalysedUnit(ms));
      }
    }

    tus.sort(new AnalysisPriorityComparator());
    return tus;
  }

  public static List<Integer> parseStringToIntegers(String input) {
    String sanitized = input.replaceAll("[\\[\\]]", "");
    List<Integer> numbers = Arrays.stream(sanitized.split(","))
        .map(String::trim)
        .map(Integer::parseInt)
        .collect(Collectors.toList());
    return numbers;
  }

  private static MutationIdentifier parseMutationIdentifier(JsonObject o) {
    JsonObject locationObject = o.getAsJsonObject("location");
    String clazz = locationObject.get("clazz").getAsString();
    String method = locationObject.get("method").getAsString();
    String methodDesc = locationObject.get("methodDesc").getAsString();
    Location location = new Location(ClassName.fromString(clazz), method, methodDesc);

    String indexes = o.get("indexes").getAsString();
    String mutator = o.get("mutator").getAsString();

    if (indexes.contains(",")) {
      return new MutationIdentifier(location, parseStringToIntegers(indexes), mutator);
    } else {
      return new MutationIdentifier(location, parseStringToIntegers(indexes).get(0), mutator);
    }
  }

  private static ArrayList<TestInfo> parseTestsInOrder(JsonArray testArray) {
    ArrayList<TestInfo> testInfoList = new ArrayList<>();
    for (JsonElement test : testArray) {
      JsonObject testObject = test.getAsJsonObject();
      String name = testObject.get("name").getAsString();
      int time = testObject.get("time").getAsInt();
      TestInfo testInfo = new TestInfo(name, time);
      testInfoList.add(testInfo);
    }
    return testInfoList;
  }

  public List<MutationAnalysisUnit> createMutationTestUnits(final Collection<ClassName> codeClasses,
                                                            final String filePath,
                                                            CoverageDatabase coverage) {
    final List<MutationAnalysisUnit> tus = new ArrayList<>();
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      JsonElement jsonElement = JsonParser.parseReader(reader);
      JsonArray mutantArray = jsonElement.getAsJsonArray();
      List<MutationDetails> mutationDetailsList = new ArrayList<>();
      for (JsonElement mutantElement : mutantArray) {
        JsonObject mutant = mutantElement.getAsJsonObject();
        MutationIdentifier id = parseMutationIdentifier(mutant.getAsJsonObject("id"));
        String filename = mutant.get("filename").getAsString();
        String blocks = mutant.get("block").getAsString();
        int lineNumber = mutant.get("lineNumber").getAsInt();
        String description = mutant.get("description").getAsString();
        List<TestInfo> testsInOrder = new ArrayList<>();
        if (!(coverage instanceof NoCoverage)) {
          testsInOrder = parseTestsInOrder(mutant.getAsJsonArray("testsInOrder"));
        }
        MutationDetails mutationDetails = new MutationDetails(id, filename, description, lineNumber, parseStringToIntegers(blocks));
        mutationDetails.addTestsInOrder(testsInOrder);
        mutationDetailsList.add(mutationDetails);
      }
      for (final Collection<MutationDetails> md : this.grouper.groupMutations(
          codeClasses, mutationDetailsList)) {
        tus.add(makeUnanalysedUnit(md));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    tus.sort(new AnalysisPriorityComparator());
    return tus;
  }

  public List<MutationAnalysisUnit> createMutationTestUnits(
          final Collection<ClassName> codeClasses, Random random) {
    final List<MutationAnalysisUnit> tus = new ArrayList<>();

    final List<MutationDetails> mutations = codeClasses.stream()
        .flatMap(c -> mutationSource.createMutations(c).stream())
        .collect(Collectors.toList());

    mutations.sort(comparing(MutationDetails::getId));

    List<MutationResult> analysisUnits = this.analyser.analyse(mutations);

    Collection<MutationDetails> needProcessing = filterAlreadyAnalysedMutations(mutations, analysisUnits);

    List<MutationResult> analysedMutations = analysisUnits.stream()
            .filter(r -> r.getStatus() != DetectionStatus.NOT_STARTED)
            .collect(Collectors.toList());

    if (!analysedMutations.isEmpty()) {
      tus.add(makePreAnalysedUnit(analysedMutations));
    }

    if (!needProcessing.isEmpty()) {
      for (final Collection<MutationDetails> ms : this.grouper.groupMutations(
              codeClasses, needProcessing)) {
        ArrayList<MutationDetails> mutationDetails = new ArrayList<>(ms);
        Collections.shuffle(mutationDetails, random);
        tus.add(makeUnanalysedUnit(mutationDetails));
      }
    }
    tus.sort(new AnalysisPriorityComparator());
    return tus;
  }

  private static Collection<MutationDetails> filterAlreadyAnalysedMutations(List<MutationDetails> mutations, Collection<MutationResult> analysedMutations) {
    final Set<MutationIdentifier> alreadyAnalysed = analysedMutations.stream()
            .map(mr -> mr.getDetails().getId())
            .collect(Collectors.toSet());

    final Collection<MutationDetails> needAnalysis = mutations.stream()
        .filter(m -> !alreadyAnalysed.contains(m.getId()))
        .collect(Collectors.toList());

    // If we've prioritised a test, the mutations will be returned with a status of not started.
    // The mutation returned will however have a modified test order so should be used in
    // place of the original
    final Collection<MutationDetails> haveBeenAltered = analysedMutations.stream()
            .filter(m -> m.getStatus() == DetectionStatus.NOT_STARTED)
            .map(r -> r.getDetails())
            .collect(Collectors.toList());

    needAnalysis.addAll(haveBeenAltered);

    return needAnalysis;
  }


  private MutationAnalysisUnit makePreAnalysedUnit(
      final List<MutationResult> analysed) {
    return new KnownStatusMutationTestUnit(analysed);
  }

  private MutationAnalysisUnit makeUnanalysedUnit(
      final Collection<MutationDetails> needAnalysis) {
    return new MutationTestUnit(needAnalysis, this.workerFactory);
  }

}
