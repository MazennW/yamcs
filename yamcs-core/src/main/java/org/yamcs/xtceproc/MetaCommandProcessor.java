package org.yamcs.xtceproc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yamcs.ErrorInCommand;
import org.yamcs.ProcessorConfig;
import org.yamcs.commanding.ArgumentValue;
import org.yamcs.parameter.Value;
import org.yamcs.utils.BitBuffer;
import org.yamcs.xtce.Argument;
import org.yamcs.xtce.ArgumentAssignment;
import org.yamcs.xtce.CommandContainer;
import org.yamcs.xtce.Comparison;
import org.yamcs.xtce.ComparisonList;
import org.yamcs.xtce.Container;
import org.yamcs.xtce.MatchCriteria;
import org.yamcs.xtce.MetaCommand;
import org.yamcs.xtce.OperatorType;
import org.yamcs.xtce.Parameter;
import org.yamcs.xtce.ParameterInstanceRef;

public class MetaCommandProcessor {
    final ProcessorData pdata;

    public MetaCommandProcessor(ProcessorData pdata) {
        this.pdata = pdata;
    }

    public CommandBuildResult buildCommand(MetaCommand mc, List<ArgumentAssignment> argAssignmentList)
            throws ErrorInCommand {
        return buildCommand(pdata, mc, argAssignmentList);
    }

    public static CommandBuildResult buildCommand(ProcessorData pdata, MetaCommand mc,
            List<ArgumentAssignment> argAssignmentList) throws ErrorInCommand {
        if (mc.isAbstract()) {
            throw new ErrorInCommand("Will not build command " + mc.getQualifiedName() + " because it is abstract");
        }

        Map<Argument, ArgumentValue> args = new HashMap<>();
        Map<String, String> argAssignment = new HashMap<>();
        for (ArgumentAssignment aa : argAssignmentList) {
            argAssignment.put(aa.getArgumentName(), aa.getArgumentValue());
        }

        ProcessorConfig procConf = pdata.getProcessorConfig();
        collectAndCheckArguments(mc, args, argAssignment);

        CommandContainer cmdContainer = mc.getCommandContainer();
        if (cmdContainer == null && !procConf.allowContainerlessCommands()) {
            throw new ErrorInCommand("MetaCommand " + mc.getName()
                    + " has no container (and the processor option allowContainerlessCommands is set to false)");
        }


        byte[] binary = null;

        if (cmdContainer != null) {
            Map<Parameter, Value> params = new HashMap<>();
            collectParameters(cmdContainer, params);
            BitBuffer bitbuf = new BitBuffer(new byte[procConf.getMaxCommandSize()]);
            TcProcessingContext pcontext = new TcProcessingContext(pdata, args, params, bitbuf, 0);
            try {
                pcontext.mccProcessor.encode(mc);
            } catch (CommandEncodingException e) {
                throw new ErrorInCommand("Error when encoding command: " + e.getMessage());
            }

            int length = pcontext.size;
            binary = new byte[length];
            System.arraycopy(bitbuf.array(), 0, binary, 0, length);
        }
        return new CommandBuildResult(binary, args);
    }

    /**
     * Builds the argument values args based on the argAssignment (which is basically the user input) and on the
     * inheritance assignments
     * 
     * The argAssignment is emptied as values are being used so if at the end of the call there are still assignment not
     * used -> invalid argument provided
     * 
     * This function is called recursively.
     * 
     * @param args
     * @param argAssignment
     * @throws ErrorInCommand
     */
    private static void collectAndCheckArguments(MetaCommand mc, Map<Argument, ArgumentValue> args,
            Map<String, String> argAssignment) throws ErrorInCommand {
        List<Argument> argList = mc.getArgumentList();
        if (argList != null) {
            // check for each argument that we either have an assignment or a value
            for (Argument a : argList) {
                if (args.containsKey(a)) {
                    continue;
                }
                Value argValue = null;
                Object argObj = null;
                if (!argAssignment.containsKey(a.getName())) {
                    argObj = a.getInitialValue();
                    if (argObj == null) {
                        argObj = a.getArgumentType().getInitialValue();
                    }
                    if (argObj == null) {
                        throw new ErrorInCommand("No value provided for argument " + a.getName()
                                + " (and the argument has no default value either)");
                    }
                } else {
                    String stringValue = argAssignment.remove(a.getName());
                    try {
                        argObj = a.getArgumentType().parseString(stringValue);

                    } catch (Exception e) {
                        throw new ErrorInCommand("Cannot assign value to " + a.getName() + ": " + e.getMessage());
                    }
                }
                try {
                    ArgumentTypeProcessor.checkRange(a.getArgumentType(), argObj);
                    argValue = DataTypeProcessor.getValueForType(a.getArgumentType(), argObj);
                } catch (Exception e) {
                    throw new ErrorInCommand("Cannot assign value to " + a.getName() + ": " + e.getMessage());
                }
                args.put(a, new ArgumentValue(a, argValue));
            }
        }

        // now, go to the parent
        MetaCommand parent = mc.getBaseMetaCommand();
        if (parent != null) {
            List<ArgumentAssignment> aaList = mc.getArgumentAssignmentList();
            if (aaList != null) {
                for (ArgumentAssignment aa : aaList) {
                    if (argAssignment.containsKey(aa.getArgumentName())) {
                        throw new ErrorInCommand("Cannot overwrite the argument " + aa.getArgumentName()
                                + " which is defined in the inheritance assignment list");
                    }
                    argAssignment.put(aa.getArgumentName(), aa.getArgumentValue());
                }
            }
            collectAndCheckArguments(parent, args, argAssignment);
        }
    }

    // look at the command container if it inherits another container using a condition list and add those parameters
    // with the respective values
    private static void collectParameters(Container container, Map<Parameter, Value> params) throws ErrorInCommand {
        Container parent = container.getBaseContainer();
        if (parent != null) {
            MatchCriteria cr = container.getRestrictionCriteria();
            if (cr instanceof ComparisonList) {
                ComparisonList cl = (ComparisonList) cr;
                for (Comparison c : cl.getComparisonList()) {
                    if (c.getComparisonOperator() == OperatorType.EQUALITY) {
                        Parameter param = ((ParameterInstanceRef) c.getRef()).getParameter();
                        if (param != null) {
                            try {
                                Value v = ParameterTypeUtils.parseString(param.getParameterType(), c.getStringValue());
                                params.put(param, v);
                            } catch (IllegalArgumentException e) {
                                throw new ErrorInCommand("Cannot parse '" + c.getStringValue()
                                        + "' as value for parameter " + param.getQualifiedName());
                            }
                        }
                    }
                }
            }
        }
    }

    static public class CommandBuildResult {
        byte[] cmdPacket;
        Map<Argument, ArgumentValue> args;

        public CommandBuildResult(byte[] b, Map<Argument, ArgumentValue> args) {
            this.cmdPacket = b;
            this.args = args;
        }

        public byte[] getCmdPacket() {
            return cmdPacket;
        }

        public Map<Argument, ArgumentValue> getArgs() {
            return args;
        }
    }
}
