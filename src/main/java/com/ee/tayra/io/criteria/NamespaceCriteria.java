/*******************************************************************************
 * Copyright (c) 2013, Equal Experts Ltd
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the Tayra Project.
 ******************************************************************************/
package com.ee.tayra.io.criteria;

public class NamespaceCriteria implements Criterion {
  private static final String DOT = "\\.";
  private static final String BLANK = "";
  private static final String NO_OP = "NO_OP";
  private String incomingNs;
  private final boolean toExclude;
  private String dBCollectionNs;
  private String incomingOpCode;

  public NamespaceCriteria(final String ns, final boolean toExclude) {
    this.incomingNs = ns;
    this.toExclude = toExclude;
    this.incomingOpCode = extractDbCollectionAndOpCodeFromIncomingNs();
  }

  @Override
  public boolean isSatisfiedBy(final String document) {
    if (toExclude) {
      return !isCriteriaSatisfied(document);
    }
    return isCriteriaSatisfied(document);
  }

  private boolean isCriteriaSatisfied(final String document) {
    String documentNamespace = getNamespace(document);
    if (BLANK.equals(documentNamespace)) {
      return false;
    }
    OperationType type = OperationType.create(documentNamespace);
    if (type.match(document, documentNamespace, dBCollectionNs)) {
      if (type.getClass().equals(DDLOperation.class)) {
        return true;
      }
      return matchOperation(document);
    }
    return false;
  }

  private boolean matchOperation(final String document) {
    if (!(incomingOpCode.equals(NO_OP))) {
      return incomingOpCode.equals(getDocumentOpcode(document));
    }
    return true;
  }

  private String extractDbCollectionAndOpCodeFromIncomingNs() {
    String incomingOperation = extractOperationFromNs();
    Opcode incomingNsOperationEnum = Opcode.map(incomingOperation);
    dBCollectionNs = extractNsAsDbCollection(incomingOperation,
            incomingNsOperationEnum);
    return incomingNsOperationEnum.getOpCode();
  }

  private String extractNsAsDbCollection(final String incomingNsOperation,
      final Opcode incomingNsOperationEnum) {
    String dBCollection = incomingNs;
    if (!isNoOp(incomingNsOperationEnum)) {
      dBCollection = incomingNs.substring(0,
          incomingNs.indexOf(incomingNsOperation) - 1);
// check for db.collection pattern (should contain one dot)
    }
    return dBCollection;
  }

  private boolean isNoOp(final Opcode incomingNsOperationEnum) {
    return incomingNsOperationEnum.compareTo(Opcode.No_Op) == 0;
  }

  private String extractOperationFromNs() {
    String[] incomingNsInfo = incomingNs.split(DOT);
    String incomingOperation = incomingNsInfo[(incomingNsInfo.length) - 1];
    return incomingOperation;
  }

  private String getDocumentOpcode(final String document) {
    int opcodeStartIndex = document.indexOf("op") - 1;
    int opcodeEndIndex = document.indexOf(",", opcodeStartIndex);
    String opcodeSpec = document.substring(opcodeStartIndex, opcodeEndIndex);
    String quotedOpcode = opcodeSpec.split(":")[1];
    return quotedOpcode.replaceAll("\"", "").trim();
}

  private String getNamespace(final String document) {
    int startIndex = document.indexOf("ns") - 1;
    int endIndex = document.indexOf(",", startIndex);
    String namespace = document.substring(startIndex, endIndex)
      .split(":") [1];
    return namespace.replaceAll("\"", BLANK).trim();
  }
}
