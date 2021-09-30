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


public class SBQueueItem {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected SBQueueItem(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(SBQueueItem obj) {
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
        lldbJNI.delete_SBQueueItem(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public SBQueueItem() {
    this(lldbJNI.new_SBQueueItem__SWIG_0(), true);
  }

  public SBQueueItem(SWIGTYPE_p_std__shared_ptrT_lldb_private__QueueItem_t queue_item_sp) {
    this(lldbJNI.new_SBQueueItem__SWIG_1(SWIGTYPE_p_std__shared_ptrT_lldb_private__QueueItem_t.getCPtr(queue_item_sp)), true);
  }

  public boolean IsValid() {
    return lldbJNI.SBQueueItem_IsValid(swigCPtr, this);
  }

  public void Clear() {
    lldbJNI.SBQueueItem_Clear(swigCPtr, this);
  }

  public QueueItemKind GetKind() {
    return QueueItemKind.swigToEnum(lldbJNI.SBQueueItem_GetKind(swigCPtr, this));
  }

  public void SetKind(QueueItemKind kind) {
    lldbJNI.SBQueueItem_SetKind(swigCPtr, this, kind.swigValue());
  }

  public SBAddress GetAddress() {
    return new SBAddress(lldbJNI.SBQueueItem_GetAddress(swigCPtr, this), true);
  }

  public void SetAddress(SBAddress addr) {
    lldbJNI.SBQueueItem_SetAddress(swigCPtr, this, SBAddress.getCPtr(addr), addr);
  }

  public void SetQueueItem(SWIGTYPE_p_std__shared_ptrT_lldb_private__QueueItem_t queue_item_sp) {
    lldbJNI.SBQueueItem_SetQueueItem(swigCPtr, this, SWIGTYPE_p_std__shared_ptrT_lldb_private__QueueItem_t.getCPtr(queue_item_sp));
  }

  public SBThread GetExtendedBacktraceThread(String type) {
    return new SBThread(lldbJNI.SBQueueItem_GetExtendedBacktraceThread(swigCPtr, this, type), true);
  }

}
