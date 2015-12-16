package ru.ramax.processmining;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.XExtensionManager;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryBufferedImpl;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.id.XIDFactory;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.*;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XesXmlGZIPSerializer;
import org.deckfour.xes.out.XesXmlSerializer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Hello world!
 *
 */
public class App extends Application
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

        options.addOption(
            Option.builder("gui")
                .longOpt("start-gui")
                .desc("Start App GUI")
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
            else if (line.hasOption("gui")) {
                launch(args);
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

                File in = null;
                try {
                    in = new File(inputName);
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

    private static void createLog(File csv, String out, Boolean zipMe) {
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
        CSVFormat csvFormat = CSVFormat
                .EXCEL
                .withHeader()
                .withIgnoreEmptyLines();

        String oldTrace = "";
        XTrace xTrace = xFactory.createTrace();
        CSVRecord lastRecord = null;
        ZoneId zoneId = ZoneId.systemDefault();

        try {
            for (CSVRecord record : CSVParser.parse(csv, StandardCharsets.UTF_8, csvFormat)) {
                //System.out.println(record);

                if (!record.get("Workflow Activity").equals("Screen Start")) continue;

                // 11/02/2015 02:58:13 pm
                LocalDateTime start =  LocalDateTime.parse(record.get("Workflow Start Time").toUpperCase(), DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a"));
                LocalDateTime end =  LocalDateTime.parse(record.get("Workflow End Time").toUpperCase(), DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a"));

                if (!oldTrace.equals(record.get("Module Instance ID"))) {
                    if (!xTrace.isEmpty()) {
                        outputLog.add(xTrace);


                        LocalDateTime lastend =  LocalDateTime.parse(lastRecord.get("Workflow End Time").toUpperCase(), DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a"));
                        // add end event
                        xTrace.add(createEvent(
                                xFactory,
                                "Finish transaction",
                                "NA",
                                "Finish transaction",
                                "",
                                "",
                                lastRecord.get("Workflow ID"),
                                lastend.atZone(zoneId).toEpochSecond(),
                                XLifecycleExtension.StandardModel.COMPLETE
                        ));
                    }

                    xTrace = xFactory.createTrace();
                    xConceptExtension.assignName(xTrace, record.get("Module Instance ID"));
                    oldTrace = record.get("Module Instance ID");

                    // start event
                    xTrace.add(createEvent(
                            xFactory,
                            "Start transaction",
                            "NA",
                            "Start transaction",
                            "",
                            "",
                            record.get("Workflow ID"),
                            start.atZone(zoneId).toEpochSecond(),
                            XLifecycleExtension.StandardModel.COMPLETE
                    ));

                }

                String screen = record.get("Screen");
                screen = screen.replaceAll("^> +", "");
                screen = screen.replaceAll("\\d+", "#");

                xTrace.add(createEvent(
                        xFactory,
                        screen,
                        record.get("Screen ID"),
                        record.get("Screen"),
                        record.get("Description"),
                        record.get("Workflow Activity"),
                        record.get("Workflow ID"),
                        start.atZone(zoneId).toEpochSecond(),
                        XLifecycleExtension.StandardModel.START
                ));

                xTrace.add(createEvent(
                        xFactory,
                        screen,
                        record.get("Screen ID"),
                        record.get("Screen"),
                        record.get("Description"),
                        record.get("Workflow Activity"),
                        record.get("Workflow ID"),
                        end.atZone(zoneId).toEpochSecond(),
                        XLifecycleExtension.StandardModel.COMPLETE
                ));

                count++;
                lastRecord = record;
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }


//        OutputStream outputStream = new OutputStream() {
//            private StringBuilder string = new StringBuilder();
//
//            @Override
//            public void write(int b) throws IOException {
//                this.string.append((char) b );
//            }
//
//            public String toString(){
//                return this.string.toString();
//            }
//        };

        try {


            if (!zipMe) {
                File outFile = new File(out);
                OutputStream outputStream = new FileOutputStream(outFile);
                XesXmlSerializer xesXmlSerializer = new XesXmlSerializer();
                xesXmlSerializer.serialize(outputLog, outputStream);
            }
            else {
                File outFile = new File(out.concat(".gz"));
                OutputStream outputStream = new FileOutputStream(outFile);
                XesXmlGZIPSerializer xesXmlGZIPSerializer = new XesXmlGZIPSerializer();
                xesXmlGZIPSerializer.serialize(outputLog, outputStream);
            }
//            System.out.println(outputStream.toString());
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

    private static  XEvent createEvent(XFactory xFactory, String screen, String screenId, String rawScreen, String description,
                                       String activity, String resource, Long time, XLifecycleExtension.StandardModel stage) {
        XEvent event = xFactory.createEvent();

        XAttributeMap eventMap = xFactory.createAttributeMap();
        eventMap.put("raw_screen", xFactory.createAttributeLiteral("rawScreen", rawScreen, null));
        eventMap.put("screen_id", xFactory.createAttributeLiteral("screenId", screenId, null));
        eventMap.put("description", xFactory.createAttributeLiteral("description", description, null));
        eventMap.put("activity", xFactory.createAttributeLiteral("activity", activity, null));

        XConceptExtension.instance().assignName(event, screen);
        XOrganizationalExtension.instance().assignResource(event, resource);
        XTimeExtension.instance().assignTimestamp(event, time*1000);
        XLifecycleExtension.instance().assignStandardTransition(event, stage);

        return event;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setTitle("KNOA Event Log Analytics");
        primaryStage.show();

    }
}
