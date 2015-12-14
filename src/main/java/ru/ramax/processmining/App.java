package ru.ramax.processmining;

import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.XExtensionManager;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryBufferedImpl;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.*;
import org.deckfour.xes.out.XesXmlSerializer;

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        Options options = new Options();

        options.addOption(
            Option.builder("i")
                .desc("Specify csv file from KNOA")
                .hasArg(true)
                .argName("inputFile")
                .numberOfArgs(1)
                .type(String.class)
                .longOpt("input-file")
            .build()
        );

        options.addOption(
            Option.builder("o")
                .desc("Specify output XES file name")
                .hasArg(true)
                .argName("outputFile")
                .numberOfArgs(1)
                .type(String.class)
                .longOpt("input-file")
            .build()
        );

        options.addOption(
            Option.builder("z")
                .type(Boolean.class)
                .longOpt("zipped")
                .desc("Zip creates xes")
                .hasArg(false)
            .build()
        );

        options.addOption(
            Option.builder("?")
                .longOpt("help")
                .desc("Display this message")
            .build()
        );

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );

            List<String> other = line.getArgList();

            if (line.hasOption("?") ||
                    ( ( !line.hasOption("i") || !line.hasOption("o") ) && other.size() != 2)
                    ) {
                printHelp(options);
            }
            else {
                // check for input file
                String inputName, outputName;
                if (line.hasOption("i")) {
                    inputName = line.getOptionValue("i");
                }
                else {
                    inputName = other.get(0);
                }

                if (line.hasOption(("o"))) {
                    outputName = line.getOptionValue("o");
                }
                else {
                    outputName = other.get(1);
                }

                Reader in = null;
                try {
                    in = new FileReader(inputName);
                }
                catch (Exception exp) {
                    System.err.println( "Cannot open input file.  Reason: " + exp.getMessage() );
                }

                createLog(in, outputName, line.hasOption("z"));
            }
        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            printHelp(options);
        }
    }

    private static void createLog(Reader csv, String out, Boolean zipMe) {
        System.out.println( "Hello World!" );
        XFactoryNaiveImpl xFactory = new XFactoryNaiveImpl();

        XAttributeMap logAttributes = xFactory.createAttributeMap();
        logAttributes.put("process", xFactory.createAttributeLiteral("concept:name", "VL02N transaction", XConceptExtension.instance()));
        XLog outputLog = xFactory.createLog(logAttributes);

        XLifecycleExtension xLifecycleExtension = XLifecycleExtension.instance();
        xLifecycleExtension.assignModel(outputLog, "standard");

        XTimeExtension xTimeExtension = XTimeExtension.instance();
        XOrganizationalExtension xOrganizationalExtension = XOrganizationalExtension.instance();
        XConceptExtension xConceptExtension = XConceptExtension.instance();

        outputLog.getGlobalTraceAttributes().add(xFactory.createAttributeLiteral("concept:name", "name", xConceptExtension));

        outputLog.getGlobalEventAttributes().add(xFactory.createAttributeLiteral("concept:name", "", xConceptExtension));
        outputLog.getGlobalEventAttributes().add(xFactory.createAttributeLiteral("org:resource", "", xOrganizationalExtension));
        outputLog.getGlobalEventAttributes().add(xFactory.createAttributeLiteral("time:timestamp", "", xTimeExtension));
        outputLog.getGlobalEventAttributes().add(xFactory.createAttributeLiteral("lifecycle:transition", "", xLifecycleExtension));

        XEventNameClassifier xEventNameClassifier = new XEventNameClassifier();
        outputLog.getClassifiers().add(xEventNameClassifier);

        outputLog.getExtensions().add(xConceptExtension);
        outputLog.getExtensions().add(xTimeExtension);
        outputLog.getExtensions().add(xOrganizationalExtension);
        outputLog.getExtensions().add(xLifecycleExtension);

        int count = 0;

        try {
            for (CSVRecord record : CSVFormat.EXCEL.parse(csv)) {
                System.out.println(record);

                count++;
                if (count > 10) break;
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        XTrace xTrace = xFactory.createTrace();
        xConceptExtension.assignName(xTrace, "1");


        XAttributeMap eventMap = xFactory.createAttributeMap();
        //eventMap.put("user", xFactory.createAttributeLiteral("org:resource", "NONE", XTimeExtension.instance()));
//        eventMap.put("id", xFactory.createAttributeLiteral("concept:name", "1", XConceptExtension.instance()));
        XEvent xEvent = xFactory.createEvent(eventMap);
        xConceptExtension.assignName(xEvent, "12");
        xOrganizationalExtension.assignResource(xEvent, "IVANOV");
        xTimeExtension.assignTimestamp(xEvent, Instant.now().getEpochSecond()*1000);
        xLifecycleExtension.assignStandardTransition(xEvent, XLifecycleExtension.StandardModel.START);

        xTrace.insertOrdered(xEvent);
        outputLog.add(xTrace);

        OutputStream outputStream = new OutputStream() {
            private StringBuilder string = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                this.string.append((char) b );
            }

            public String toString(){
                return this.string.toString();
            }
        };

        XesXmlSerializer xesXmlSerializer = new XesXmlSerializer();

        try {
            xesXmlSerializer.serialize(outputLog, outputStream);
            System.out.println(outputStream.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        String header = "Convert CSV from JNOA into XES\n\n";
        formatter.printHelp("knoa2xes [input-file] [output-file]", header, options, "", true);
    }
}
