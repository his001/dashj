/*
 * Copyright 2018 Dash Core Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file was generated by SWIG (http://www.swig.org) and modified.
 * Version 3.0.12
 */

package org.dashj.bls;

import com.google.common.base.Preconditions;

public class Signature extends BLSObject {

  protected Signature(long cPtr, boolean cMemoryOwn) {
    super(cPtr, cMemoryOwn);
  }

  protected static long getCPtr(Signature obj) {
    return (obj == null) ? 0 : obj.cPointer;
  }

  public synchronized void delete() {
        JNI.delete_Signature(cPointer);
  }

  public static Signature FromBytes(byte [] data) {
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(data.length == SIGNATURE_SIZE);
    return new Signature(JNI.Signature_FromBytes__SWIG_0(data), true);
  }

  public static Signature FromBytes(byte [] data, AggregationInfo info) {
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(data.length == SIGNATURE_SIZE);
    return new Signature(JNI.Signature_FromBytes__SWIG_1(data, AggregationInfo.getCPtr(info)), true);
  }

  public static Signature FromG2(SWIGTYPE_p_g2_t element) {
    return new Signature(JNI.Signature_FromG2__SWIG_0(SWIGTYPE_p_g2_t.getCPtr(element)), true);
  }

  public static Signature FromG2(SWIGTYPE_p_g2_t element, AggregationInfo info) {
    return new Signature(JNI.Signature_FromG2__SWIG_1(SWIGTYPE_p_g2_t.getCPtr(element), AggregationInfo.getCPtr(info)), true);
  }

  public static Signature FromInsecureSig(InsecureSignature sig) {
    return new Signature(JNI.Signature_FromInsecureSig__SWIG_0(InsecureSignature.getCPtr(sig), sig), true);
  }

  public static Signature FromInsecureSig(InsecureSignature sig, AggregationInfo info) {
    return new Signature(JNI.Signature_FromInsecureSig__SWIG_1(InsecureSignature.getCPtr(sig), sig, AggregationInfo.getCPtr(info)), true);
  }

  public Signature(Signature signature) {
    this(JNI.new_Signature(Signature.getCPtr(signature), signature), true);
  }

  public boolean Verify() {
    return JNI.Signature_Verify(cPointer, this);
  }

  public static Signature AggregateSigs(SignatureVector sigs) {
    return new Signature(JNI.Signature_AggregateSigs(SignatureVector.getCPtr(sigs)), true);
  }

  public Signature DivideBy(SignatureVector divisorSigs) {
    return new Signature(JNI.Signature_DivideBy(cPointer, this, SignatureVector.getCPtr(divisorSigs)), true);
  }

  public AggregationInfo GetAggregationInfo() {
    long cPtr = JNI.Signature_GetAggregationInfo(cPointer, this);
    return (cPtr == 0) ? null : new AggregationInfo(cPtr, false);
  }

  public void SetAggregationInfo(AggregationInfo newAggregationInfo) {
    JNI.Signature_SetAggregationInfo(cPointer, this, AggregationInfo.getCPtr(newAggregationInfo));
  }

  public void Serialize(byte[] buffer) {
    Preconditions.checkNotNull(buffer);
    Preconditions.checkArgument(buffer.length >= SIGNATURE_SIZE);
    JNI.Signature_Serialize__SWIG_0(cPointer, this, buffer);
  }

  public SWIGTYPE_p_std__vectorT_unsigned_char_t Serialize() {
    return new SWIGTYPE_p_std__vectorT_unsigned_char_t(JNI.Signature_Serialize__SWIG_1(cPointer, this), true);
  }

  public final static long SIGNATURE_SIZE = JNI.Signature_SIGNATURE_SIZE_get();
}
