package ru.ramax.processmining;

import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.model.XTrace;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Iterator;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by nsitnikov on 17/12/15.
 */
public class RealLogTest {
    @Test
    public void logLoadAndTraceStatistics() throws Exception {
        XesXmlGZIPParser parser = new XesXmlGZIPParser();

        URL url = RealLogTest.class.getClassLoader().getResource("out.xes.gz");
        File file = new File(url.toURI());
        assertNotNull("File should be present", file);
        assertTrue("File should be parsable", parser.canParse(file));

        XFactoryNaiveImpl xFactory = new XFactoryNaiveImpl();
        XEStools parsed = new XEStools();
        boolean result = parsed.parseLog(Paths.get(url.toURI()).toString());
        assertTrue("Log should be parsed", result);

        XTrace xTrace = parsed.getXTrace("610705");
        assertNotNull("Trace 610705 should be present", xTrace);
        assertTrue("Trace has 4 events, count is" + xTrace.size(), xTrace.size() == 4);

        boolean lessThen3 = false;
        Iterator iterator = parsed.getXlog().iterator();
        while (iterator.hasNext()) {
            XTrace current = (XTrace) iterator.next();
            if (current.size() < 3) {
                lessThen3 = true;
                break;
            }
        }
        assertTrue("All traces has at least 3 events", !lessThen3);

    }
}
