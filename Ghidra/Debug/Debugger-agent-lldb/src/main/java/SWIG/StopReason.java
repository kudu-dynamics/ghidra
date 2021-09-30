/* ###
 * IP: Apache License 2.0 with LLVM Exceptions
 */
package SWIG;


/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */


public final class StopReason {
  public final static StopReason eStopReasonInvalid = new StopReason("eStopReasonInvalid", lldbJNI.eStopReasonInvalid_get());
  public final static StopReason eStopReasonNone = new StopReason("eStopReasonNone");
  public final static StopReason eStopReasonTrace = new StopReason("eStopReasonTrace");
  public final static StopReason eStopReasonBreakpoint = new StopReason("eStopReasonBreakpoint");
  public final static StopReason eStopReasonWatchpoint = new StopReason("eStopReasonWatchpoint");
  public final static StopReason eStopReasonSignal = new StopReason("eStopReasonSignal");
  public final static StopReason eStopReasonException = new StopReason("eStopReasonException");
  public final static StopReason eStopReasonExec = new StopReason("eStopReasonExec");
  public final static StopReason eStopReasonPlanComplete = new StopReason("eStopReasonPlanComplete");
  public final static StopReason eStopReasonThreadExiting = new StopReason("eStopReasonThreadExiting");
  public final static StopReason eStopReasonInstrumentation = new StopReason("eStopReasonInstrumentation");

  public final int swigValue() {
    return swigValue;
  }

  public String toString() {
    return swigName;
  }

  public static StopReason swigToEnum(int swigValue) {
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (int i = 0; i < swigValues.length; i++)
      if (swigValues[i].swigValue == swigValue)
        return swigValues[i];
    throw new IllegalArgumentException("No enum " + StopReason.class + " with value " + swigValue);
  }

  private StopReason(String swigName) {
    this.swigName = swigName;
    this.swigValue = swigNext++;
  }

  private StopReason(String swigName, int swigValue) {
    this.swigName = swigName;
    this.swigValue = swigValue;
    swigNext = swigValue+1;
  }

  private StopReason(String swigName, StopReason swigEnum) {
    this.swigName = swigName;
    this.swigValue = swigEnum.swigValue;
    swigNext = this.swigValue+1;
  }

  private static StopReason[] swigValues = { eStopReasonInvalid, eStopReasonNone, eStopReasonTrace, eStopReasonBreakpoint, eStopReasonWatchpoint, eStopReasonSignal, eStopReasonException, eStopReasonExec, eStopReasonPlanComplete, eStopReasonThreadExiting, eStopReasonInstrumentation };
  private static int swigNext = 0;
  private final int swigValue;
  private final String swigName;
}

