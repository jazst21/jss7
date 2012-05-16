/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall;

import org.apache.log4j.Logger;
import org.mobicents.protocols.asn.AsnInputStream;
import org.mobicents.protocols.asn.Tag;
import org.mobicents.protocols.ss7.cap.CAPDialogImpl;
import org.mobicents.protocols.ss7.cap.CAPProviderImpl;
import org.mobicents.protocols.ss7.cap.CAPServiceBaseImpl;
import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPOperationCode;
import org.mobicents.protocols.ss7.cap.api.CAPParsingComponentException;
import org.mobicents.protocols.ss7.cap.api.CAPParsingComponentExceptionReason;
import org.mobicents.protocols.ss7.cap.api.CAPServiceListener;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPServiceCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPServiceCircuitSwitchedCallListener;
import org.mobicents.protocols.ss7.map.api.dialog.ServingCheckData;
import org.mobicents.protocols.ss7.map.api.dialog.ServingCheckResult;
import org.mobicents.protocols.ss7.map.dialog.ServingCheckDataImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.Dialog;
import org.mobicents.protocols.ss7.tcap.asn.comp.ComponentType;
import org.mobicents.protocols.ss7.tcap.asn.comp.OperationCode;
import org.mobicents.protocols.ss7.tcap.asn.comp.Parameter;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class CAPServiceCircuitSwitchedCallImpl extends CAPServiceBaseImpl implements CAPServiceCircuitSwitchedCall {

	protected Logger loger = Logger.getLogger(CAPServiceCircuitSwitchedCallImpl.class);

	public CAPServiceCircuitSwitchedCallImpl(CAPProviderImpl capProviderImpl) {
		super(capProviderImpl);
	}

	@Override
	public CAPDialogCircuitSwitchedCall createNewDialog(CAPApplicationContext appCntx, SccpAddress origAddress, SccpAddress destAddress) throws CAPException {

		// We cannot create a dialog if the service is not activated
		if (!this.isActivated())
			throw new CAPException(
					"Cannot create CAPDialogCircuitSwitchedCall because CAPServiceCircuitSwitchedCall is not activated");

		Dialog tcapDialog = this.createNewTCAPDialog(origAddress, destAddress);
		CAPDialogCircuitSwitchedCallImpl dialog = new CAPDialogCircuitSwitchedCallImpl(appCntx, tcapDialog, this.capProviderImpl, this);

		this.putCAPDialogIntoCollection(dialog);

		return dialog;
	}

	@Override
	public void addCAPServiceListener(CAPServiceCircuitSwitchedCallListener capServiceListener) {
		super.addCAPServiceListener(capServiceListener);
	}

	@Override
	public void removeCAPServiceListener(CAPServiceCircuitSwitchedCallListener capServiceListener) {
		super.removeCAPServiceListener(capServiceListener);
	}

	@Override
	protected CAPDialogImpl createNewDialogIncoming(CAPApplicationContext appCntx, Dialog tcapDialog) {
		return new CAPDialogCircuitSwitchedCallImpl(appCntx, tcapDialog, this.capProviderImpl, this);
	}

	@Override
	public ServingCheckData isServingService(CAPApplicationContext dialogApplicationContext) {
		
		switch(dialogApplicationContext) {
		case CapV1_gsmSSF_to_gsmSCF:
		case CapV2_gsmSSF_to_gsmSCF:
		case CapV2_assistGsmSSF_to_gsmSCF:
		case CapV2_gsmSRF_to_gsmSCF:
		case CapV3_gsmSSF_scfGeneric:
		case CapV3_gsmSSF_scfAssistHandoff:
		case CapV3_gsmSRF_gsmSCF:
		case CapV4_gsmSSF_scfGeneric:
		case CapV4_gsmSSF_scfAssistHandoff:
		case CapV4_scf_gsmSSFGeneric:
		case CapV4_gsmSRF_gsmSCF:
			return new ServingCheckDataImpl(ServingCheckResult.AC_Serving);
		}
		
		return new ServingCheckDataImpl(ServingCheckResult.AC_NotServing);
	}

	@Override
	public void processComponent(ComponentType compType, OperationCode oc, Parameter parameter, CAPDialog capDialog, Long invokeId, Long linkedId)
			throws CAPParsingComponentException {

		CAPDialogCircuitSwitchedCallImpl capDialogCircuitSwitchedCallImpl = (CAPDialogCircuitSwitchedCallImpl) capDialog;

		Long ocValue = oc.getLocalOperationCode();
		if (ocValue == null)
			new CAPParsingComponentException("", CAPParsingComponentExceptionReason.UnrecognizedOperation);
		CAPApplicationContext acn = capDialog.getApplicationContext();
		int ocValueInt = (int) (long) ocValue;

		switch (ocValueInt) {
		case CAPOperationCode.initialDP:
			if (acn == CAPApplicationContext.CapV1_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric) {
				if (compType == ComponentType.Invoke) {
					this.initialDpRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.requestReportBCSMEvent:
			if (acn == CAPApplicationContext.CapV1_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					this.requestReportBCSMEventRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.applyCharging:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					this.applyChargingRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.eventReportBCSM:
			if (acn == CAPApplicationContext.CapV1_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					eventReportBCSMRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.continueCode:
			if (acn == CAPApplicationContext.CapV1_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					continueRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.applyChargingReport:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					applyChargingReportRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.releaseCall:
			if (acn == CAPApplicationContext.CapV1_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					releaseCallRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.connect:
			if (acn == CAPApplicationContext.CapV1_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					connectRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.callInformationRequest:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					callInformationRequestRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.callInformationReport:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					callInformationReportRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.activityTest:
			if (acn == CAPApplicationContext.CapV1_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV2_gsmSRF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSRF_gsmSCF || acn == CAPApplicationContext.CapV4_gsmSRF_gsmSCF
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					activityTestRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
				if (compType == ComponentType.ReturnResultLast) {
					activityTestResponse(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.assistRequestInstructions:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV2_gsmSRF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSRF_gsmSCF
					|| acn == CAPApplicationContext.CapV4_gsmSRF_gsmSCF) {
				if (compType == ComponentType.Invoke) {
					assistRequestInstructionsRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.establishTemporaryConnection:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					establishTemporaryConnectionRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.disconnectForwardConnection:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					disconnectForwardConnectionRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.connectToResource:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					connectToResourceRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.resetTimer:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					resetTimerRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.furnishChargingInformation:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric) {
				if (compType == ComponentType.Invoke) {
					furnishChargingInformationRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.sendChargingInformation:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric) {
				if (compType == ComponentType.Invoke) {
					sendChargingInformationRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.specializedResourceReport:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric || acn == CAPApplicationContext.CapV2_gsmSRF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSRF_gsmSCF || acn == CAPApplicationContext.CapV4_gsmSRF_gsmSCF) {
				if (compType == ComponentType.Invoke) {
					specializedResourceReportRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.playAnnouncement:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric || acn == CAPApplicationContext.CapV2_gsmSRF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSRF_gsmSCF || acn == CAPApplicationContext.CapV4_gsmSRF_gsmSCF) {
				if (compType == ComponentType.Invoke) {
					playAnnouncementRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.promptAndCollectUserInformation:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric || acn == CAPApplicationContext.CapV2_gsmSRF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSRF_gsmSCF || acn == CAPApplicationContext.CapV4_gsmSRF_gsmSCF) {
				if (compType == ComponentType.Invoke) {
					promptAndCollectUserInformationRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
				if (compType == ComponentType.ReturnResultLast) {
					promptAndCollectUserInformationResponse(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		case CAPOperationCode.cancelCode:
			if (acn == CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF || acn == CAPApplicationContext.CapV3_gsmSSF_scfGeneric
					|| acn == CAPApplicationContext.CapV4_gsmSSF_scfGeneric || acn == CAPApplicationContext.CapV2_assistGsmSSF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSSF_scfAssistHandoff || acn == CAPApplicationContext.CapV4_gsmSSF_scfAssistHandoff
					|| acn == CAPApplicationContext.CapV4_scf_gsmSSFGeneric || acn == CAPApplicationContext.CapV2_gsmSRF_to_gsmSCF
					|| acn == CAPApplicationContext.CapV3_gsmSRF_gsmSCF || acn == CAPApplicationContext.CapV4_gsmSRF_gsmSCF) {
				if (compType == ComponentType.Invoke) {
					cancelRequest(parameter, capDialogCircuitSwitchedCallImpl, invokeId);
				}
			}
			break;

		default:
			new CAPParsingComponentException("", CAPParsingComponentExceptionReason.UnrecognizedOperation);
		}
	}

	private void initialDpRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding initialDpRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException("Error while decoding initialDpRequest: Bad tag or tagClass or parameter is primitive, received tag="
					+ parameter.getTag(), CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		InitialDPRequestImpl ind = new InitialDPRequestImpl(capDialogImpl.getApplicationContext().getVersion().getVersion() >= 3);
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onInitialDPRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing initialDpRequest: " + e.getMessage(), e);
			}
		}
	}

	private void requestReportBCSMEventRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId)
			throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding requestReportBCSMEventRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding requestReportBCSMEventRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		RequestReportBCSMEventRequestImpl ind = new RequestReportBCSMEventRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onRequestReportBCSMEventRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing requestReportBCSMEventRequest: " + e.getMessage(), e);
			}
		}
	}

	private void applyChargingRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding applyChargingRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding applyChargingRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		ApplyChargingRequestImpl ind = new ApplyChargingRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onApplyChargingRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing requestReportBCSMEventRequest: " + e.getMessage(), e);
			}
		}
	}

	private void eventReportBCSMRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding eventReportBCSMRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding eventReportBCSMRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		EventReportBCSMRequestImpl ind = new EventReportBCSMRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onEventReportBCSMRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing eventReportBCSMRequest: " + e.getMessage(), e);
			}
		}
	}

	private void continueRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		ContinueRequestImpl ind = new ContinueRequestImpl();

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onContinueRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing continueRequest: " + e.getMessage(), e);
			}
		}
	}

	private void applyChargingReportRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding applyChargingReportRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.STRING_OCTET || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || !parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding applyChargingReportRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		ApplyChargingReportRequestImpl ind = new ApplyChargingReportRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onApplyChargingReportRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing applyChargingReportRequest: " + e.getMessage(), e);
			}
		}
	}

	private void releaseCallRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding releaseCallRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.STRING_OCTET || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || !parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding releaseCallRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		ReleaseCallRequestImpl ind = new ReleaseCallRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onReleaseCalltRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing applyChargingReportRequest: " + e.getMessage(), e);
			}
		}
	}

	private void connectRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding connectRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding connectRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		ConnectRequestImpl ind = new ConnectRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onConnectRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing eventReportBCSMRequest: " + e.getMessage(), e);
			}
		}
	}

	private void callInformationRequestRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding callInformationRequestRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding callInformationRequestRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		CallInformationRequestRequestImpl ind = new CallInformationRequestRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onCallInformationRequestRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing eventReportBCSMRequest: " + e.getMessage(), e);
			}
		}
	}

	private void callInformationReportRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding callInformationReportRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding callInformationReportRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		CallInformationReportRequestImpl ind = new CallInformationReportRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onCallInformationReportRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing eventReportBCSMRequest: " + e.getMessage(), e);
			}
		}
	}

	private void activityTestRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		ActivityTestRequestImpl ind = new ActivityTestRequestImpl();

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onActivityTestRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing activityTestRequest: " + e.getMessage(), e);
			}
		}
	}

	private void activityTestResponse(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		ActivityTestResponseImpl ind = new ActivityTestResponseImpl();

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onActivityTestResponseIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing activityTestResponse: " + e.getMessage(), e);
			}
		}
	}

	private void assistRequestInstructionsRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding assistRequestInstructionsRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding assistRequestInstructionsRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		AssistRequestInstructionsRequestImpl ind = new AssistRequestInstructionsRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onAssistRequestInstructionsRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing assistRequestInstructionsRequest: " + e.getMessage(), e);
			}
		}
	}

	private void establishTemporaryConnectionRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding establishTemporaryConnectionRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding establishTemporaryConnectionRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		EstablishTemporaryConnectionRequestImpl ind = new EstablishTemporaryConnectionRequestImpl(capDialogImpl.getApplicationContext()
				.getVersion().getVersion() >= 3);
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onEstablishTemporaryConnectionRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing establishTemporaryConnectionRequest: " + e.getMessage(), e);
			}
		}
	}
	
	private void disconnectForwardConnectionRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		DisconnectForwardConnectionRequestImpl ind = new DisconnectForwardConnectionRequestImpl();

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onDisconnectForwardConnectionRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing disconnectForwardConnectionRequest: " + e.getMessage(), e);
			}
		}
	}

	private void connectToResourceRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding connectToResourceRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding connectToResourceRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		ConnectToResourceRequestImpl ind = new ConnectToResourceRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onConnectToResourceRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing connectToResourceRequest: " + e.getMessage(), e);
			}
		}
	}

	private void resetTimerRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding resetTimerRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding resetTimerRequest: Bad tag or tagClass or parameter is primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		ResetTimerRequestImpl ind = new ResetTimerRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onResetTimerRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing resetTimerRequest: " + e.getMessage(), e);
			}
		}
	}

	private void furnishChargingInformationRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding furnishChargingInformationRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.STRING_OCTET || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || !parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding furnishChargingInformationRequest: Bad tag or tagClass or parameter is not primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		FurnishChargingInformationRequestImpl ind = new FurnishChargingInformationRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onFurnishChargingInformationRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing furnishChargingInformationRequest: " + e.getMessage(), e);
			}
		}
	}

	private void sendChargingInformationRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding sendChargingInformationRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || !parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding sendChargingInformationRequest: Bad tag or tagClass or parameter is not primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		SendChargingInformationRequestImpl ind = new SendChargingInformationRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onSendChargingInformationRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing sendChargingInformationRequest: " + e.getMessage(), e);
			}
		}
	}

	private void specializedResourceReportRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding specializedResourceReportRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (capDialogImpl.getApplicationContext().getVersion().getVersion() < 4) {
			if (parameter.getTag() != Tag.NULL || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || !parameter.isPrimitive())
				throw new CAPParsingComponentException(
						"Error while decoding specializedResourceReportRequest: Bad tag or tagClass or parameter is not primitive, received tag="
								+ parameter.getTag(), CAPParsingComponentExceptionReason.MistypedParameter);
		} else {
			if (parameter.getTagClass() != Tag.CLASS_CONTEXT_SPECIFIC || parameter.isPrimitive())
				throw new CAPParsingComponentException(
						"Error while decoding specializedResourceReportRequest: Bad tagClass or parameter is primitive, received tag="
								+ parameter.getTag(), CAPParsingComponentExceptionReason.MistypedParameter);
		}

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		SpecializedResourceReportRequestImpl ind = new SpecializedResourceReportRequestImpl(capDialogImpl.getApplicationContext()
				.getVersion().getVersion() < 4);
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onSpecializedResourceReportRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing specializedResourceReportRequest: " + e.getMessage(), e);
			}
		}
	}

	private void playAnnouncementRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding playAnnouncementRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || !parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding playAnnouncementRequest: Bad tag or tagClass or parameter is not primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		PlayAnnouncementRequestImpl ind = new PlayAnnouncementRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onPlayAnnouncementRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing playAnnouncementRequest: " + e.getMessage(), e);
			}
		}
	}

	private void promptAndCollectUserInformationRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId)
			throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding promptAndCollectUserInformationRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || !parameter.isPrimitive())
			throw new CAPParsingComponentException(
					"Error while decoding playAnnouncementRequest: Bad tag or tagClass or parameter is not primitive, received tag=" + parameter.getTag(),
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		PromptAndCollectUserInformationRequestImpl ind = new PromptAndCollectUserInformationRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onPromptAndCollectUserInformationRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing promptAndCollectUserInformationRequest: " + e.getMessage(), e);
			}
		}
	}

	private void promptAndCollectUserInformationResponse(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId)
			throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding promptAndCollectUserInformationResponse: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTagClass() != Tag.CLASS_CONTEXT_SPECIFIC)
			throw new CAPParsingComponentException("Error while decoding promptAndCollectUserInformationResponse: bad tagClass",
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		PromptAndCollectUserInformationResponseImpl ind = new PromptAndCollectUserInformationResponseImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onPromptAndCollectUserInformationResponseIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing promptAndCollectUserInformationResponse: " + e.getMessage(), e);
			}
		}
	}

	private void cancelRequest(Parameter parameter, CAPDialogCircuitSwitchedCallImpl capDialogImpl, Long invokeId) throws CAPParsingComponentException {

		if (parameter == null)
			throw new CAPParsingComponentException("Error while decoding cancelRequest: Parameter is mandatory but not found",
					CAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTagClass() != Tag.CLASS_CONTEXT_SPECIFIC)
			throw new CAPParsingComponentException("Error while decoding cancelRequest: bad tagClass",
					CAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		CancelRequestImpl ind = new CancelRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setCAPDialog(capDialogImpl);

		for (CAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onCAPMessage(ind);
				((CAPServiceCircuitSwitchedCallListener) serLis).onCancelRequestIndication(ind);
			} catch (Exception e) {
				loger.error("Error processing cancelRequest: " + e.getMessage(), e);
			}
		}
	}
}

