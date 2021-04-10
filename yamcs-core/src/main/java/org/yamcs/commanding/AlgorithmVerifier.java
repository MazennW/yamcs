package org.yamcs.commanding;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.parameter.ParameterValue;
import org.yamcs.parameter.ParameterValueList;
import org.yamcs.parameter.RawEngValue;
import org.yamcs.Processor;
import org.yamcs.algorithms.AlgorithmExecListener;
import org.yamcs.algorithms.AlgorithmExecutionContext;
import org.yamcs.algorithms.AlgorithmManager;
import org.yamcs.protobuf.Commanding.CommandId;
import org.yamcs.utils.StringConverter;
import org.yamcs.xtce.Algorithm;
import org.yamcs.xtce.CommandVerifier;
import org.yamcs.xtce.XtceDb;

public class AlgorithmVerifier extends Verifier implements AlgorithmExecListener {
    final Algorithm alg;
    final AlgorithmExecutionContext algCtx;
    final PreparedCommand pc;
    Logger log;
    final XtceDb xtcedb;

    final Processor yproc;

    AlgorithmVerifier(CommandVerificationHandler cvh, CommandVerifier cv) {
        super(cvh, cv);
        alg = cv.getAlgorithm();
        algCtx = cvh.getAlgorithmExecutionContext();
        pc = cvh.getPreparedCommand();
        yproc = cvh.getProcessor();
        xtcedb = yproc.getXtceDb();
        log = LoggerFactory.getLogger(AlgorithmVerifier.class);
    }

    @Override
    void doStart() {
        log.debug("Starting verifier for command {} alg: {} stage: {} ",
                StringConverter.toString(pc.getCommandId()), alg.getName(), cv.getStage());
        AlgorithmManager algMgr = cvh.getAlgorithmManager();
        algMgr.activateAlgorithm(alg, algCtx, this);

        // send command arguments
        algMgr.processArguments(cvh.getCommandArguments(), algCtx);

        // push all the command parameters
        List<ParameterValue> pvList = cvh.getCommandParameters();
        System.out.println("sending cmd params: " + pvList);
        if (!pvList.isEmpty()) {
            algMgr.updateDelivery(new ParameterValueList(pvList), algCtx);
        }
    }

    @Override
    void doCancel() {
        AlgorithmManager algMgr = cvh.getAlgorithmManager();
        algMgr.deactivateAlgorithm(alg, algCtx);
    }

    @Override
    public void algorithmRun(List<RawEngValue> inputValues, Object returnValue, List<ParameterValue> outputValues) {
        if (log.isTraceEnabled()) {
            CommandId cmdId = pc.getCommandId();
            log.trace("command: {} algorithm {} stage{} executed: returnValue: {} , outputValues: {}",
                    StringConverter.toString(cmdId), alg.getName(), cv.getStage(), returnValue, outputValues);
        }
        if (returnValue == null) {
            log.trace("Algorithm {} run but did not return a result.", alg.getName());
            return;
        }
        AlgorithmManager algMgr = cvh.getAlgorithmManager();
        algMgr.deactivateAlgorithm(alg, algCtx);

        if (returnValue instanceof Boolean) {
            finished((Boolean) returnValue, null);
        } else {
            finished(false, returnValue.toString());
        }
    }

    @Override
    public void updatedCommandHistoryParam(ParameterValue pv) {
        AlgorithmManager algMgr = cvh.getAlgorithmManager();
        algMgr.updateDelivery(ParameterValueList.asList(pv), algCtx);
    }
}
