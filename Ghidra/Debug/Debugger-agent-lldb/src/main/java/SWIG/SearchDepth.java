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


public final class SearchDepth {
  public final static SearchDepth eSearchDepthInvalid = new SearchDepth("eSearchDepthInvalid", lldbJNI.eSearchDepthInvalid_get());
  public final static SearchDepth eSearchDepthTarget = new SearchDepth("eSearchDepthTarget");
  public final static SearchDepth eSearchDepthModule = new SearchDepth("eSearchDepthModule");
  public final static SearchDepth eSearchDepthCompUnit = new SearchDepth("eSearchDepthCompUnit");
  public final static SearchDepth eSearchDepthFunction = new SearchDepth("eSearchDepthFunction");
  public final static SearchDepth eSearchDepthBlock = new SearchDepth("eSearchDepthBlock");
  public final static SearchDepth eSearchDepthAddress = new SearchDepth("eSearchDepthAddress");
  public final static SearchDepth kLastSearchDepthKind = new SearchDepth("kLastSearchDepthKind", lldbJNI.kLastSearchDepthKind_get());

  public final int swigValue() {
    return swigValue;
  }

  public String toString() {
    return swigName;
  }

  public static SearchDepth swigToEnum(int swigValue) {
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (int i = 0; i < swigValues.length; i++)
      if (swigValues[i].swigValue == swigValue)
        return swigValues[i];
    throw new IllegalArgumentException("No enum " + SearchDepth.class + " with value " + swigValue);
  }

  private SearchDepth(String swigName) {
    this.swigName = swigName;
    this.swigValue = swigNext++;
  }

  private SearchDepth(String swigName, int swigValue) {
    this.swigName = swigName;
    this.swigValue = swigValue;
    swigNext = swigValue+1;
  }

  private SearchDepth(String swigName, SearchDepth swigEnum) {
    this.swigName = swigName;
    this.swigValue = swigEnum.swigValue;
    swigNext = this.swigValue+1;
  }

  private static SearchDepth[] swigValues = { eSearchDepthInvalid, eSearchDepthTarget, eSearchDepthModule, eSearchDepthCompUnit, eSearchDepthFunction, eSearchDepthBlock, eSearchDepthAddress, kLastSearchDepthKind };
  private static int swigNext = 0;
  private final int swigValue;
  private final String swigName;
}

