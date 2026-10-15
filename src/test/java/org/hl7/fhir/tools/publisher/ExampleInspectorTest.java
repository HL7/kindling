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
            messageAt("Composition.section", "Constraint failed: cmp-1: A section must contain at least one of text, entries, or sub-sections", IssueSeverity.ERROR),
            messageAt("Composition.relatesTo.type", "Undefined element 'type' at /Composition/relatesTo/type", IssueSeverity.ERROR))));
  }

  @Test
  public void failFixtureDoesNotPassForAnUnrelatedError() {
    assertFalse(ExampleInspector.isInvariantTestSuccessful(
        "cmp-1.f1.fail.xml",
        "cmp-1",
        List.of(message("Constraint failed: ref-2: Reference must have a value", IssueSeverity.ERROR))));
  }

  @Test
  public void failFixtureAllowsCorrelatedConstraintErrors() {
    assertTrue(ExampleInspector.isInvariantTestSuccessful(
        "cmp-1.f1.fail.xml",
        "cmp-1",
        List.of(
            message("Constraint failed: cmp-1: A section must contain at least one of text, entries, or sub-sections", IssueSeverity.ERROR),
            message("Constraint failed: cmp-3: Sections containing section.text SHALL NOT contain emptyReason", IssueSeverity.ERROR))));
  }

  @Test
  public void failFixtureUnexpectedErrorsExcludeInvariantMessages() {
    List<ValidationMessage> messages = List.of(
        messageAt("Composition.section", "Constraint failed: cmp-1: A section must contain at least one of text, entries, or sub-sections", IssueSeverity.ERROR),
        messageAt("Composition.section", "Constraint failed: cmp-3: Sections containing section.text SHALL NOT contain emptyReason", IssueSeverity.ERROR),
        messageAt("Composition.relatesTo.type", "Undefined element 'type' at /Composition/relatesTo/type", IssueSeverity.ERROR),
        message("Search parameter did not find examples", IssueSeverity.WARNING));

    List<ValidationMessage> unexpectedErrors = ExampleInspector.findUnexpectedInvariantFixtureErrors("cmp-1.f1.fail.xml", "cmp-1", messages);

    assertEquals(1, unexpectedErrors.size());
    assertEquals("Undefined element 'type' at /Composition/relatesTo/type", unexpectedErrors.get(0).getMessage());
  }

  @Test
  public void failFixtureUnexpectedErrorsAllowSameSubtreeValidatorMessages() {
    List<ValidationMessage> messages = List.of(
        messageAt("Bundle", "Constraint failed: bdl-11: A document must have a Composition as the first resource", IssueSeverity.ERROR),
        messageAt("Bundle.entry.resource", "The first entry in a document must be a composition", IssueSeverity.ERROR));

    List<ValidationMessage> unexpectedErrors = ExampleInspector.findUnexpectedInvariantFixtureErrors("bdl-11.f1.fail.xml", "bdl-11", messages);

    assertEquals(0, unexpectedErrors.size());
  }

  @Test
  public void passFixtureUnexpectedErrorsIncludeInvariantMessages() {
    List<ValidationMessage> messages = List.of(
        message("Constraint failed: cmp-3: Sections containing section.text SHALL NOT contain emptyReason", IssueSeverity.ERROR),
        message("Search parameter did not find examples", IssueSeverity.WARNING));

    List<ValidationMessage> unexpectedErrors = ExampleInspector.findUnexpectedInvariantFixtureErrors("cmp-1.p1.pass.xml", "cmp-1", messages);

    assertEquals(1, unexpectedErrors.size());
    assertEquals("Constraint failed: cmp-3: Sections containing section.text SHALL NOT contain emptyReason", unexpectedErrors.get(0).getMessage());
  }

  private ValidationMessage message(String message, IssueSeverity severity) {
    return new ValidationMessage(Source.InstanceValidator, IssueType.STRUCTURE, -1, -1, "fixture", message, severity);
  }

  private ValidationMessage messageAt(String location, String message, IssueSeverity severity) {
    return new ValidationMessage(Source.InstanceValidator, IssueType.STRUCTURE, -1, -1, location, message, severity);
  }
}
