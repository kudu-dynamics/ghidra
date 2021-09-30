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


public class SBThreadPlan {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected SBThreadPlan(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(SBThreadPlan obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        lldbJNI.delete_SBThreadPlan(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public SBThreadPlan() {
    this(lldbJNI.new_SBThreadPlan__SWIG_0(), true);
  }

  public SBThreadPlan(SBThreadPlan threadPlan) {
    this(lldbJNI.new_SBThreadPlan__SWIG_1(SBThreadPlan.getCPtr(threadPlan), threadPlan), true);
  }

  public SBThreadPlan(SWIGTYPE_p_std__shared_ptrT_lldb_private__ThreadPlan_t lldb_object_sp) {
    this(lldbJNI.new_SBThreadPlan__SWIG_2(SWIGTYPE_p_std__shared_ptrT_lldb_private__ThreadPlan_t.getCPtr(lldb_object_sp)), true);
  }

  public SBThreadPlan(SBThread thread, String class_name) {
    this(lldbJNI.new_SBThreadPlan__SWIG_3(SBThread.getCPtr(thread), thread, class_name), true);
  }

  public boolean IsValid() {
    return lldbJNI.SBThreadPlan_IsValid__SWIG_0(swigCPtr, this);
  }

  public void Clear() {
    lldbJNI.SBThreadPlan_Clear(swigCPtr, this);
  }

  public StopReason GetStopReason() {
    return StopReason.swigToEnum(lldbJNI.SBThreadPlan_GetStopReason(swigCPtr, this));
  }

  public long GetStopReasonDataCount() {
    return lldbJNI.SBThreadPlan_GetStopReasonDataCount(swigCPtr, this);
  }

  public java.math.BigInteger GetStopReasonDataAtIndex(long idx) {
    return lldbJNI.SBThreadPlan_GetStopReasonDataAtIndex(swigCPtr, this, idx);
  }

  public SBThread GetThread() {
    return new SBThread(lldbJNI.SBThreadPlan_GetThread(swigCPtr, this), true);
  }

  public boolean GetDescription(SBStream description) {
    return lldbJNI.SBThreadPlan_GetDescription(swigCPtr, this, SBStream.getCPtr(description), description);
  }

  public void SetPlanComplete(boolean success) {
    lldbJNI.SBThreadPlan_SetPlanComplete(swigCPtr, this, success);
  }

  public boolean IsPlanComplete() {
    return lldbJNI.SBThreadPlan_IsPlanComplete(swigCPtr, this);
  }

  public boolean IsPlanStale() {
    return lldbJNI.SBThreadPlan_IsPlanStale(swigCPtr, this);
  }

  public boolean GetStopOthers() {
    return lldbJNI.SBThreadPlan_GetStopOthers(swigCPtr, this);
  }

  public void SetStopOthers(boolean stop_others) {
    lldbJNI.SBThreadPlan_SetStopOthers(swigCPtr, this, stop_others);
  }

  public SBThreadPlan QueueThreadPlanForStepOverRange(SBAddress start_address, java.math.BigInteger range_size) {
    return new SBThreadPlan(lldbJNI.SBThreadPlan_QueueThreadPlanForStepOverRange(swigCPtr, this, SBAddress.getCPtr(start_address), start_address, range_size), true);
  }

  public SBThreadPlan QueueThreadPlanForStepInRange(SBAddress start_address, java.math.BigInteger range_size) {
    return new SBThreadPlan(lldbJNI.SBThreadPlan_QueueThreadPlanForStepInRange(swigCPtr, this, SBAddress.getCPtr(start_address), start_address, range_size), true);
  }

  public SBThreadPlan QueueThreadPlanForStepOut(long frame_idx_to_step_to, boolean first_insn) {
    return new SBThreadPlan(lldbJNI.SBThreadPlan_QueueThreadPlanForStepOut__SWIG_0(swigCPtr, this, frame_idx_to_step_to, first_insn), true);
  }

  public SBThreadPlan QueueThreadPlanForStepOut(long frame_idx_to_step_to) {
    return new SBThreadPlan(lldbJNI.SBThreadPlan_QueueThreadPlanForStepOut__SWIG_1(swigCPtr, this, frame_idx_to_step_to), true);
  }

  public SBThreadPlan QueueThreadPlanForRunToAddress(SBAddress address) {
    return new SBThreadPlan(lldbJNI.SBThreadPlan_QueueThreadPlanForRunToAddress(swigCPtr, this, SBAddress.getCPtr(address), address), true);
  }

  public SBThreadPlan QueueThreadPlanForStepScripted(String script_class_name) {
    return new SBThreadPlan(lldbJNI.SBThreadPlan_QueueThreadPlanForStepScripted__SWIG_0(swigCPtr, this, script_class_name), true);
  }

  public SBThreadPlan QueueThreadPlanForStepScripted(String script_class_name, SBError error) {
    return new SBThreadPlan(lldbJNI.SBThreadPlan_QueueThreadPlanForStepScripted__SWIG_1(swigCPtr, this, script_class_name, SBError.getCPtr(error), error), true);
  }

  public SBThreadPlan QueueThreadPlanForStepScripted(String script_class_name, SBStructuredData args_data, SBError error) {
    return new SBThreadPlan(lldbJNI.SBThreadPlan_QueueThreadPlanForStepScripted__SWIG_2(swigCPtr, this, script_class_name, SBStructuredData.getCPtr(args_data), args_data, SBError.getCPtr(error), error), true);
  }

}
