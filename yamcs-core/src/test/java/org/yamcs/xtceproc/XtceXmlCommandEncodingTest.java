package org.yamcs.xtceproc;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.yamcs.ConfigurationException;
import org.yamcs.ErrorInCommand;
import org.yamcs.YConfiguration;
import org.yamcs.utils.StringConverter;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.xtce.ArgumentAssignment;
import org.yamcs.xtce.MetaCommand;
import org.yamcs.xtce.XtceDb;

public class XtceXmlCommandEncodingTest {
    static XtceDb xtcedb;
    static MetaCommandProcessor metaCommandProcessor;
    
    @BeforeClass 
    public static void beforeClass() throws ConfigurationException {        
        TimeEncoding.setUp();
        YConfiguration.setup();
        xtcedb = XtceDbFactory.createInstanceByConfig("ccsds-green-book");
        metaCommandProcessor = new MetaCommandProcessor(new ProcessorData(xtcedb));
    }
    
    @Test
    public void test1() throws ErrorInCommand {
        // encode command
        MetaCommand mc = xtcedb.getMetaCommand("/SpaceVehicle/PWHTMR");
        List<ArgumentAssignment> arguments = new LinkedList<ArgumentAssignment>() ;
        ArgumentAssignment argumentAssignment1 = new ArgumentAssignment("TimerStartStop", "TIMER_START");
        arguments.add(argumentAssignment1);
        byte[] b = metaCommandProcessor.buildCommand(mc, arguments).getCmdPacket();

        assertEquals("FF0000001E000001", StringConverter.arrayToHexString(b));
    }
}
