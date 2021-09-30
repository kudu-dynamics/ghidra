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


public final class CommandInterpreterResult {
  public final static CommandInterpreterResult eCommandInterpreterResultSuccess = new CommandInterpreterResult("eCommandInterpreterResultSuccess");
  public final static CommandInterpreterResult eCommandInterpreterResultInferiorCrash = new CommandInterpreterResult("eCommandInterpreterResultInferiorCrash");
  public final static CommandInterpreterResult eCommandInterpreterResultCommandError = new CommandInterpreterResult("eCommandInterpreterResultCommandError");
  public final static CommandInterpreterResult eCommandInterpreterResultQuitRequested = new CommandInterpreterResult("eCommandInterpreterResultQuitRequested");

  public final int swigValue() {
    return swigValue;
  }

  public String toString() {
    return swigName;
  }

  public static CommandInterpreterResult swigToEnum(int swigValue) {
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (int i = 0; i < swigValues.length; i++)
      if (swigValues[i].swigValue == swigValue)
        return swigValues[i];
    throw new IllegalArgumentException("No enum " + CommandInterpreterResult.class + " with value " + swigValue);
  }

  private CommandInterpreterResult(String swigName) {
    this.swigName = swigName;
    this.swigValue = swigNext++;
  }

  private CommandInterpreterResult(String swigName, int swigValue) {
    this.swigName = swigName;
    this.swigValue = swigValue;
    swigNext = swigValue+1;
  }

  private CommandInterpreterResult(String swigName, CommandInterpreterResult swigEnum) {
    this.swigName = swigName;
    this.swigValue = swigEnum.swigValue;
    swigNext = this.swigValue+1;
  }

  private static CommandInterpreterResult[] swigValues = { eCommandInterpreterResultSuccess, eCommandInterpreterResultInferiorCrash, eCommandInterpreterResultCommandError, eCommandInterpreterResultQuitRequested };
  private static int swigNext = 0;
  private final int swigValue;
  private final String swigName;
}

