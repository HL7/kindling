package org.hl7.fhir.tools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.junit.jupiter.api.Test;

public class ExampleInspectorTest {

  @Test
  public void passFixtureStillDependsOnNamedInvariantMessage() {
    assertTrue(ExampleInspector.isInvariantTestSuccessful(
        "cmp-1.p1.pass.xml",
        "cmp-1",
        List.of(message("Search parameter did not find examples", IssueSeverity.INFORMATION))));

    assertTrue(ExampleInspector.isInvariantTestSuccessful(
        "cmp-1.p1.pass.xml",
        "cmp-1",
        List.of(message("Undefined element 'type' at /Composition/relatesTo/type", IssueSeverity.ERROR))));

    assertFalse(ExampleInspector.isInvariantTestSuccessful(
        "cmp-1.p1.pass.xml",
        "cmp-1",
        List.of(message("Constraint failed: cmp-1: A section must contain at least one of text, entries, or sub-sections", IssueSeverity.WARNING))));
  }

  @Test
  public void failFixtureStillDependsOnNamedInvariantMessage() {
    assertTrue(ExampleInspector.isInvariantTestSuccessful(
        "cmp-1.f1.fail.xml",
        "cmp-1",
        List.of(message("Constraint failed: cmp-1: A section must contain at least one of text, entries, or sub-sections", IssueSeverity.WARNING))));

    assertTrue(ExampleInspector.isInvariantTestSuccessful(
        "cmp-1.f1.fail.xml",
        "cmp-1",
        List.of(
            message("Constraint failed: cmp-1: A section must contain at least one of text, entries, or sub-sections", IssueSeverity.ERROR),
            message("Undefined element 'type' at /Composition/relatesTo/type", IssueSeverity.ERROR))));
  }

  @Test
  public void failFixtureDoesNotPassForAnUnrelatedError() {
    assertFalse(ExampleInspector.isInvariantTestSuccessful(
        "cmp-1.f1.fail.xml",
        "cmp-1",
        List.of(message("Constraint failed: ref-2: Reference must have a value", IssueSeverity.ERROR))));
  }

  @Test
  public void unexpectedFixtureErrorsExcludeNamedInvariantMessages() {
    List<ValidationMessage> messages = List.of(
        message("Constraint failed: cmp-1: A section must contain at least one of text, entries, or sub-sections", IssueSeverity.ERROR),
        message("Undefined element 'type' at /Composition/relatesTo/type", IssueSeverity.ERROR),
        message("Search parameter did not find examples", IssueSeverity.WARNING));

    List<ValidationMessage> unexpectedErrors = ExampleInspector.findUnexpectedInvariantFixtureErrors("cmp-1", messages);

    assertEquals(1, unexpectedErrors.size());
    assertEquals("Undefined element 'type' at /Composition/relatesTo/type", unexpectedErrors.get(0).getMessage());
  }

  private ValidationMessage message(String message, IssueSeverity severity) {
    return new ValidationMessage(Source.InstanceValidator, IssueType.STRUCTURE, -1, -1, "fixture", message, severity);
  }
}
