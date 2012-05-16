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

package org.mobicents.protocols.ss7.cap.functional;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPDialogListener;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPMessage;
import org.mobicents.protocols.ss7.cap.api.CAPParameterFactory;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.CAPStack;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPComponentErrorReason;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGeneralAbortReason;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGprsReferenceNumber;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPUserAbortReason;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ActivityTestRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ActivityTestResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.AssistRequestInstructionsRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPServiceCircuitSwitchedCallListener;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CallInformationReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CallInformationRequestRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CancelRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectToResourceRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ContinueRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectForwardConnectionRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EstablishTemporaryConnectionRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.FurnishChargingInformationRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PlayAnnouncementRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ReleaseCallRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.RequestReportBCSMEventRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ResetTimerRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SendChargingInformationRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SpecializedResourceReportRequest;
import org.mobicents.protocols.ss7.cap.isup.CalledPartyNumberCapImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.InitialDPRequestImpl;
import org.mobicents.protocols.ss7.isup.impl.message.parameter.CalledPartyNumberImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.comp.PAbortCauseType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class Client implements CAPDialogListener, CAPServiceCircuitSwitchedCallListener {

	private static Logger logger = Logger.getLogger(Client.class);

	private CAPFunctionalTest runningTestCase;
	private SccpAddress thisAddress;
	private SccpAddress remoteAddress;

	private CAPStack capStack;
	private CAPProvider capProvider;

	private CAPParameterFactory capParameterFactory;

//	private boolean _S_receivedUnstructuredSSIndication, _S_sentEnd;
	
	private CAPDialogCircuitSwitchedCall clientCscDialog;

	private FunctionalTestScenario step;
	
	private long savedInvokeId;
	private int dialogStep;
	private String unexpected = "";


	Client(CAPStack capStack, CAPFunctionalTest runningTestCase, SccpAddress thisAddress, SccpAddress remoteAddress) {
		super();
		
		this.capStack = capStack;
		this.runningTestCase = runningTestCase;
		this.thisAddress = thisAddress;
		this.remoteAddress = remoteAddress;
		this.capProvider = this.capStack.getCAPProvider();

		this.capParameterFactory = this.capProvider.getCAPParameterFactory();

		this.capProvider.addCAPDialogListener(this);
		this.capProvider.getCAPServiceCircuitSwitchedCall().addCAPServiceListener(this);
	}

	public void actionA() throws CAPException {

		this.capProvider.getCAPServiceCircuitSwitchedCall().acivate();
		this.capProvider.getCAPServiceSms().acivate();
		this.capProvider.getCAPServiceSms().acivate();

		CAPApplicationContext appCnt = null;
		switch (this.step) {
		case Action_InitilDp:
			appCnt = CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF;
			break;
		}

		clientCscDialog = this.capProvider.getCAPServiceCircuitSwitchedCall().createNewDialog(appCnt, this.thisAddress, this.remoteAddress);

		switch (this.step) {
		case Action_InitilDp: {
			InitialDPRequest initialDp = getTestInitialDp();
			clientCscDialog.addInitialDPRequest(initialDp.getServiceKey(), initialDp.getCalledPartyNumber(), initialDp.getCallingPartyNumber(),
					initialDp.getCallingPartysCategory(), initialDp.getCGEncountered(), initialDp.getIPSSPCapabilities(), initialDp.getLocationNumber(),
					initialDp.getOriginalCalledPartyID(), initialDp.getExtensions(), initialDp.getHighLayerCompatibility(),
					initialDp.getAdditionalCallingPartyNumber(), initialDp.getBearerCapability(), initialDp.getEventTypeBCSM(),
					initialDp.getRedirectingPartyID(), initialDp.getRedirectionInformation(), initialDp.getCause(),
					initialDp.getServiceInteractionIndicatorsTwo(), initialDp.getCarrier(), initialDp.getCugIndex(), initialDp.getCugInterlock(),
					initialDp.getCugOutgoingAccess(), initialDp.getIMSI(), initialDp.getSubscriberState(), initialDp.getLocationInformation(),
					initialDp.getExtBasicServiceCode(), initialDp.getCallReferenceNumber(), initialDp.getMscAddress(), initialDp.getCalledPartyBCDNumber(),
					initialDp.getTimeAndTimezone(), initialDp.getCallForwardingSSPending(), initialDp.getInitialDPArgExtension());
		}
			break;
		}

		clientCscDialog.send();
	}

	public static boolean checkTestInitialDp(InitialDPRequest ind) {
		
		try {
			if (ind.getServiceKey() != 321)
				return false;

			if (ind.getCalledPartyNumber() == null)
				return false;
			if (ind.getCalledPartyNumber().getCalledPartyNumber() == null)
				return false;
			if (ind.getCalledPartyNumber().getCalledPartyNumber().getNatureOfAddressIndicator() != 1)
				return false;
			if (!ind.getCalledPartyNumber().getCalledPartyNumber().getAddress().equals("11223344"))
				return false;
			if (ind.getCalledPartyNumber().getCalledPartyNumber().getNumberingPlanIndicator() != 2)
				return false;
			if (ind.getCalledPartyNumber().getCalledPartyNumber().getInternalNetworkNumberIndicator() != 1)
				return false;

			return true;

		} catch (CAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}	

	public static InitialDPRequest getTestInitialDp() {
		
		try {
			CalledPartyNumberImpl calledPartyNumber = new CalledPartyNumberImpl(1, "11223344", 2, 1);
			// int natureOfAddresIndicator, String address, int
			// numberingPlanIndicator, int internalNetworkNumberIndicator
			CalledPartyNumberCapImpl calledPartyNumberCap;
			calledPartyNumberCap = new CalledPartyNumberCapImpl(calledPartyNumber);

			InitialDPRequestImpl res = new InitialDPRequestImpl(321, calledPartyNumberCap, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, false, null, null, null, null, null, null, null, null, false, null, false);

			return res;
		} catch (CAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
//		int serviceKey, CalledPartyNumberCap calledPartyNumber, CallingPartyNumberCap callingPartyNumber,
//		CallingPartysCategoryInap callingPartysCategory, CGEncountered CGEncountered, IPSSPCapabilities IPSSPCapabilities,
//		LocationNumberCap locationNumber, OriginalCalledNumberCap originalCalledPartyID, CAPExtensions extensions,
//		HighLayerCompatibilityInap highLayerCompatibility, AdditionalCallingPartyNumberCap additionalCallingPartyNumber, BearerCapability bearerCapability,
//		EventTypeBCSM eventTypeBCSM, RedirectingPartyIDCap redirectingPartyID, RedirectionInformationInap redirectionInformation, CauseCap cause,
//		ServiceInteractionIndicatorsTwo serviceInteractionIndicatorsTwo, Carrier carrier, CUGIndex cugIndex, CUGInterlock cugInterlock,
//		boolean cugOutgoingAccess, IMSI imsi, SubscriberState subscriberState, LocationInformation locationInformation,
//		ExtBasicServiceCode extBasicServiceCode, CallReferenceNumber callReferenceNumber, ISDNAddressString mscAddress,
//		CalledPartyBCDNumber calledPartyBCDNumber, TimeAndTimezone timeAndTimezone, boolean callForwardingSSPending,
//		InitialDPArgExtension initialDPArgExtension, boolean isCAPVersion3orLater
	}
	
	public boolean isFinished() {

		switch (this.step) {
		case Action_InitilDp:
			return true;
//			return _S_receivedUnstructuredSSIndication && _S_sentEnd && _S_receivedMAPOpenInfoExtentionContainer;
		}

		return false;
	}

	public String getStatus() {
		String status = "Scenario: " + this.step + "\n";

		switch (this.step) {
		case Action_InitilDp:
//			status += "_S_receivedUnstructuredSSIndication[" + _S_receivedUnstructuredSSIndication + "]" + "\n";
//			status += "_S_recievedMAPOpenInfoExtentionContainer[" + _S_receivedMAPOpenInfoExtentionContainer + "]" + "\n";
//			status += "_S_sentEnd[" + _S_sentEnd + "]" + "\n";
			break;
		}

		return status + "\n" + unexpected;
	}

	public void reset() {
//		this._S_receivedUnstructuredSSIndication = false;
//		this._S_sentEnd = false;
//		this._S_receivedMAPOpenInfoExtentionContainer = false;
		
		this.dialogStep = 0;
		this.unexpected = "";
	}

	public void setStep(FunctionalTestScenario step) {
		this.step = step;
	}

	
	
	@Override
	public void onDialogDelimiter(CAPDialog capDialog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogRequest(CAPDialog capDialog, CAPGprsReferenceNumber capGprsReferenceNumber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogAccept(CAPDialog capDialog, CAPGprsReferenceNumber capGprsReferenceNumber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogUserAbort(CAPDialog capDialog, CAPGeneralAbortReason generalReason, CAPUserAbortReason userReason) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogProviderAbort(CAPDialog capDialog, PAbortCauseType abortCause) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogClose(CAPDialog capDialog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogRelease(CAPDialog capDialog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogTimeout(CAPDialog capDialog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogNotice(CAPDialog capDialog, CAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	@Override
	public void onErrorComponent(CAPDialog capDialog, Long invokeId, CAPErrorMessage capErrorMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRejectComponent(CAPDialog capDialog, Long invokeId, Problem problem) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInvokeTimeout(CAPDialog capDialog, Long invokeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderErrorComponent(CAPDialog mapDialog, Long invokeId, CAPComponentErrorReason providerError) {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void onInitialDPRequestIndication(InitialDPRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestReportBCSMEventRequestIndication(RequestReportBCSMEventRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onApplyChargingRequestIndication(ApplyChargingRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEventReportBCSMRequestIndication(EventReportBCSMRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onContinueRequestIndication(ContinueRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onApplyChargingReportRequestIndication(ApplyChargingReportRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReleaseCalltRequestIndication(ReleaseCallRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectRequestIndication(ConnectRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCallInformationRequestRequestIndication(CallInformationRequestRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCallInformationReportRequestIndication(CallInformationReportRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onActivityTestRequestIndication(ActivityTestRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onActivityTestResponseIndication(ActivityTestResponse ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAssistRequestInstructionsRequestIndication(AssistRequestInstructionsRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEstablishTemporaryConnectionRequestIndication(EstablishTemporaryConnectionRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDisconnectForwardConnectionRequestIndication(DisconnectForwardConnectionRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onResetTimerRequestIndication(ResetTimerRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFurnishChargingInformationRequestIndication(FurnishChargingInformationRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectToResourceRequestIndication(ConnectToResourceRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSendChargingInformationRequestIndication(SendChargingInformationRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSpecializedResourceReportRequestIndication(SpecializedResourceReportRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayAnnouncementRequestIndication(PlayAnnouncementRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPromptAndCollectUserInformationRequestIndication(PromptAndCollectUserInformationRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPromptAndCollectUserInformationResponseIndication(PromptAndCollectUserInformationResponse ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancelRequestIndication(CancelRequest ind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCAPMessage(CAPMessage capMessage) {
		// TODO Auto-generated method stub
		
	}

}
