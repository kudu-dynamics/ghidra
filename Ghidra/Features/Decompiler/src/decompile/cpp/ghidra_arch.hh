/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/// \file ghidra_arch.hh
/// \brief Ghidra specific architecture information and connection to a Ghidra client

#ifndef __GHIDRA_ARCH__
#define __GHIDRA_ARCH__

#include "architecture.hh"

/// \brief Exception that mirrors exceptions thrown by the Ghidra client
///
/// If the Ghidra client throws an exception while trying to answer a query,
/// the exception is caught and sent back to the ArchitectureGhidra object
/// in a specially formatted interrupt message.  The message is decoded
/// into this object, which is than thrown.
///
/// This class also doubles as an exception generated by the decompiler
/// because of message protocol \e alignment, which should get sent back to the Ghidra client
struct JavaError : public LowlevelError {
  string type;				///< The name of the Java exception class
  JavaError(const string &tp,const string &message) : LowlevelError(message) {
    type = tp; }			///< Construct given a class and message
};

/// \brief An implementation of the Architecture interface and connection to a Ghidra client
///
/// In addition to managing the major pieces of the architecture
/// (LoadImage, Translate, Database, TypeFactory, ContextDatbase, CommentDatabase etc.),
/// this class manages a communication channel between the decompiler and a Ghidra client
/// for a single executable. The protocol supports a simple query/response format with exceptions.
/// On top of the low level protocol, this class manages a higher level interface that performs
/// specific queries, providing addresses, names, and other info as input, and returning
/// an XML document or other structure as a result.
///
/// This class overrides the build* methods to produce Architecture components that are
/// also backed by the Ghidra client.  These all use this same object to do their communication.
///
/// This class acts as a main control point for what information gets sent back to the
/// Ghidra client when it requests the main decompilation action.
/// Generally the decompiler sends back the recovered source representation of the function
/// but optionally it can send back:
///   - Recovered source code (with mark up)
///   - Data-flow and control-flow structures
///   - Local symbol and jump-table information
///   - Parameter identification information
class ArchitectureGhidra : public Architecture {
  istream &sin;			///< Input stream for interfacing with Ghidra
  ostream &sout;		///< Output stream for interfacing with Ghidra
  mutable string warnings;	///< Warnings accumulated by the decompiler
  string pspecxml;              ///< XML pspec passed from Ghidra
  string cspecxml;		///< XML cspec passed from Ghidra
  string tspecxml;              ///< Stripped down .sla file passed from Ghidra
  string corespecxml;		///< A specification of the core data-types
  bool sendsyntaxtree;		///< True if the syntax tree should be sent with function output
  bool sendCcode;		///< True if C code should be sent with function output
  bool sendParamMeasures;       ///< True if measurements for argument and return parameters should be sent
  virtual Scope *buildDatabase(DocumentStorage &store);
  virtual Translate *buildTranslator(DocumentStorage &store);
  virtual void buildLoader(DocumentStorage &store);
  virtual PcodeInjectLibrary *buildPcodeInjectLibrary(void);
  virtual void buildTypegrp(DocumentStorage &store);
  virtual void buildCommentDB(DocumentStorage &store);
  virtual void buildStringManager(DocumentStorage &store);
  virtual void buildConstantPool(DocumentStorage &store);
  virtual void buildContext(DocumentStorage &store);
  virtual void buildSpecFile(DocumentStorage &store);
  virtual void modifySpaces(Translate *trans) {}	// This is handled directly by GhidraTranslate::initialize
  virtual void postSpecFile(void);
  virtual void resolveArchitecture(void);
public:
  ArchitectureGhidra(const string &pspec,const string &cspec,const string &tspec,const string &corespec,istream &i,ostream &o);
  const string &getWarnings(void) const { return warnings; }	///< Get warnings produced by the last decompilation
  void clearWarnings(void) { warnings.clear(); }		///< Clear warnings
  Document *getRegister(const string &regname);			///< Retrieve a register description given a name
  string getRegisterName(const VarnodeData &vndata);		///< Retrieve a register name given its storage location
  Document *getTrackedRegisters(const Address &addr);		///< Retrieve \e tracked register values at the given address
  string getUserOpName(int4 index);				///< Get the name of a user-defined p-code op
  uint1 *getPcodePacked(const Address &addr);			///< Get p-code for a single instruction
  Document *getMappedSymbolsXML(const Address &addr);		///< Get symbols associated with the given address
  Document *getExternalRefXML(const Address &addr);		///< Retrieve a description of an external function
  Document *getNamespacePath(uint8 id);				///< Get a description of a namespace path
  bool isNameUsed(const string &nm,uint8 startId,uint8 stopId);	///< Is given name used along namespace path
  string getCodeLabel(const Address &addr);			///< Retrieve a label at the given address
  Document *getType(const string &name,uint8 id);		///< Retrieve a data-type description for the given name and id
  Document *getComments(const Address &fad,uint4 flags);	///< Retrieve comments for a particular function
  void getBytes(uint1 *buf,int4 size,const Address &inaddr);	///< Retrieve bytes in the LoadImage at the given address
  Document *getPcodeInject(const string &name,int4 type,const InjectContext &con);
  Document *getCPoolRef(const vector<uintb> &refs);		///< Resolve a constant pool reference
  //  Document *getScopeProperties(Scope *newscope);

  /// \brief Toggle whether the data-flow and control-flow is emitted as part of the main decompile action
  ///
  /// If the toggle is \b on, the decompiler will emit complete descriptions of the graphs.
  /// \param val is \b true to enable emitting
  void setSendSyntaxTree(bool val) { sendsyntaxtree = val; }

  bool getSendSyntaxTree(void) const { return sendsyntaxtree; }	///< Get the current setting for emitting data/control-flow.

  /// \brief Toggle whether the recovered source code is emitted as part of the main decompile action
  ///
  /// If the toggle is \b on, the decompiler will emit source code (marked up in an XML document)
  /// \param val is \b true to enable emitting
  void setSendCCode(bool val) { sendCcode = val; }

  bool getSendCCode(void) const { return sendCcode; }		///< Get the current setting for emitting source code

  /// \brief Toggle whether recovered parameter information is emitted as part of the main decompile action
  ///
  /// If the toggle is \b on, the decompiler will emit a more detailed description of what
  /// it thinks the input parameters to the function are.
  /// \param val is \b true enable emitting
  void setSendParamMeasures(bool val) { sendParamMeasures = val; }

  bool getSendParamMeasures(void) const { return sendParamMeasures; }	///< Get the current setting for emitting parameter info

  void getStringData(vector<uint1> &buffer,const Address &addr,Datatype *ct,int4 maxBytes,bool &isTrunc);
  virtual void printMessage(const string &message) const;

  static void segvHandler(int4 sig);				///< Handler for a segment violation (SIGSEGV) signal
  static int4 readToAnyBurst(istream &s);			///< Read the next message protocol marker
  static bool readBoolStream(istream &s);			///< Read a boolean value from the client
  static void readStringStream(istream &s,string &res);		///< Receive a string from the client
  static void writeStringStream(ostream &s,const string &msg);	///< Send a string to the client
  static void readToResponse(istream &s);			///< Read the query response protocol marker
  static void readResponseEnd(istream &s);			///< Read the ending query response protocol marker
  static Document *readXMLAll(istream &s);			///< Read a whole response as an XML document
  static Document *readXMLStream(istream &s);			///< Receive an XML document from the client
  static uint1 *readPackedStream(istream &s);			///< Read packed p-code op information
  static uint1 *readPackedAll(istream &s);			///< Read a whole response as packed p-code op information
  static void passJavaException(ostream &s,const string &tp,const string &msg);

  static bool isDynamicSymbolName(const string &nm);		///< Check if name is of form FUN_.. or DAT_..
};

#endif
