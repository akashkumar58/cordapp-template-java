package com.template;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// Define IOUFlowResponder:
@InitiatedBy(LegalFlow.class)
public class CRMResponder extends FlowLogic<SignedTransaction> {
    private final FlowSession otherPartySession;
    public CRMResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession otherPartySession, ProgressTracker progressTracker) {
                super(otherPartySession, progressTracker);
            }
            @Override
            protected void checkTransaction(SignedTransaction stx) {
                requireThat(require -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    require.using("This must be an IOU transaction.", output instanceof NDAState);
                    NDAState nda = (NDAState) output;
                    require.using("The IOU's value can't be too high.",
                            nda.getIsLegalApproved().equalsIgnoreCase("PENDING"));
                    return null;
                });
            }
        }
        return subFlow(new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker()));
    }
}