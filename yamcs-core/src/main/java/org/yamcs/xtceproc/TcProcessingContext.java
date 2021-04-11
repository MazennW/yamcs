package org.yamcs.xtceproc;

import java.util.Map;

import org.yamcs.commanding.ArgumentValue;
import org.yamcs.parameter.ParameterValue;
import org.yamcs.parameter.Value;
import org.yamcs.utils.BitBuffer;
import org.yamcs.xtce.Argument;
import org.yamcs.xtce.Parameter;

/**
 * Keeps track of where we are when filling in the bits and bytes of a command
 *
 * @author nm
 *
 */
public class TcProcessingContext {
    final ProcessorData pdata;
    final BitBuffer bitbuf;

    // arguments and their values
    final private Map<Argument, ArgumentValue> argValues;

    //context parameters and their values
    final private Map<Parameter, Value> paramValues;

    public long generationTime;
    final MetaCommandContainerProcessor mccProcessor;
    final DataEncodingEncoder deEncoder;
    public int size;

    public TcProcessingContext(ProcessorData pdata, Map<Argument, ArgumentValue> argValues,
            Map<Parameter, Value> paramValues,
            BitBuffer bitbuf, int bitPosition) {
        this.bitbuf = bitbuf;
        this.pdata = pdata;
        this.argValues = argValues;
        this.paramValues = paramValues;
        this.mccProcessor = new MetaCommandContainerProcessor(this);
        this.deEncoder = new DataEncodingEncoder(this);
    }

    public ArgumentValue getArgumentValue(Argument arg) {
        return argValues.get(arg);
    }

    /**
     * Look up an argument by name only, for cases in which we do not have the
     * full argument definition, such as arguments used for defining the length
     * of other variable-length arguments.
     *
     * @param argName the name of the argument
     * @return the argument value, if found, or null
     */
    public ArgumentValue getArgumentValue(String argName) {
        for (Map.Entry<Argument, ArgumentValue> entry : argValues.entrySet()) {
            if (argName.equals(entry.getKey().getName())) {
                return entry.getValue();
            }
        }

        return null;
    }

    public Value getParameterValue(Parameter param) {
        Value v = paramValues.get(param);
        if(v == null) {
            ParameterValue pv = pdata.getLastValueCache().getValue(param);
            if(pv!=null) {
                v = pv.getEngValue();
            }
        }
        return v;
    }
}
