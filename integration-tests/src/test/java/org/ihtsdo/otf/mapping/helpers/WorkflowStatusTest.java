package org.ihtsdo.otf.mapping.helpers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowStatusTest {

  @BeforeClass
  public static void init() {

  }

  /**
   * Test that REVISION, PUBLISHED, and READY_FOR_PUBLICATION are the
   * three highest workflow values, in that descending order
   */
  @Test
  public void testHighestWorkflowStatusOrder() {

    // test that REVISION is the highest workflow status
    for (WorkflowStatus status : WorkflowStatus.values()) {
      assertTrue(status.compareTo(WorkflowStatus.REVISION) <= 0);
    }

    // test that PUBLISHED is the second highest status
    for (WorkflowStatus status : WorkflowStatus.values()) {
      if (!status.equals(WorkflowStatus.REVISION))
        assertTrue(status.compareTo(WorkflowStatus.PUBLISHED) <= 0);
    }

    // test that READY_FOR_PUBLICATION is the third highest status
   for (WorkflowStatus status : WorkflowStatus.values()) {
      if (!status.equals(WorkflowStatus.REVISION) && !status.equals(WorkflowStatus.PUBLISHED))
        assertTrue(status.compareTo(WorkflowStatus.READY_FOR_PUBLICATION) <= 0);
    }
  }

  /**
   * TEST: No workflow status must be an initial substring of another For
   * example, REVIEW and REVIEW_NEEDED cannot coexist, though NEW and REVIEW_NEW
   * can
   */
  @Test
  public void testPairsForInitialSubstringMatches() {

    // cycle over all values of workflow status
    for (WorkflowStatus status1 : WorkflowStatus.values()) {

      // cycle (again) over all values of workflow status
      for (WorkflowStatus status2 : WorkflowStatus.values()) {

        // if statuses not equal, compare
        if (!status1.equals(status2)) {
          if (status1.toString().toLowerCase()
              .startsWith(status2.toString().toLowerCase())) {
            fail("Workflow status " + status2.toString()
                + " is an initial substring of workflow status "
                + status1.toString());
          }
        }
      }
    }
  }

  @AfterClass
  public static void cleanup() {

  }
}
