package org.ihtsdo.otf.mapping.jpa.test;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.GetterSetterTester;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.FeedbackJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit class for testing getters, setters, equals, and hashCode methods.
 */
public class GetterSetterTest {

  /**
   * Setup.
   */
  @Before
  public void setup() {
    // do nothing
  }

  /**
   * Test model.
   */
  @Test
  public void testModel() {
    try {

      Object[] objects =
          new Object[] {
            new FeedbackConversationJpa(),
            new FeedbackJpa()
          };

      for (Object object : objects) {
        Logger.getLogger(this.getClass()).info(
            "  Testing " + object.getClass().getName());
        GetterSetterTester tester = new GetterSetterTester(object);
        tester.exclude("objectId");
        tester.test();
      }

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Test validationResultJpa
   */
  @SuppressWarnings("static-method")
  @Test
  public void testValidationResult() {
    ValidationResultJpa result = new ValidationResultJpa();
    result.addError("error1");
    result.addError("error2");
    result.addWarning("warning1");
    result.addWarning("warning2");
    Assert.assertEquals(result.getErrors().size(), 2);
    Assert.assertEquals(result.getWarnings().size(), 2);
    Assert.assertFalse(result.isValid());
    
    Set<String> errors = new HashSet<>();
    errors.add("error3");
    errors.add("error4");
    Set<String> warnings = new HashSet<>();
    warnings.add("warning3");
    warnings.add("warning4");
    result.addErrors(errors);
    result.addWarnings(warnings);
    Assert.assertEquals(result.getErrors().size(), 4);
    Assert.assertEquals(result.getWarnings().size(), 4);
    Assert.assertFalse(result.isValid());

    result.setErrors(errors);
    result.setWarnings(warnings);
    Assert.assertEquals(errors, result.getErrors());
    Assert.assertEquals(warnings, result.getWarnings());
  
  }
  
  /**
   * Teardown.
   */
  @After
  public void teardown() {
    // do nothing
  }

}
