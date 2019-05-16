package au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer;

import au.edu.unimelb.rpadiscovery.Utils.Functions;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.*;
import org.deckfour.xes.out.*;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class CPNtoXES {
    public static void convertCPNLogToMXML(String logFile_path) {

        long initialTimeEpoch = System.currentTimeMillis();
        String logName = logFile_path.substring(logFile_path.lastIndexOf("/") + 1, logFile_path.lastIndexOf("."));

        removeSilentTransitionInCPNLog(logFile_path);

        XFactory factory = XFactoryRegistry.instance().currentDefault();

        XAttributeMap logAttributeMap = factory.createAttributeMap();
        XAttribute logConceptNameAttribute = factory.createAttributeLiteral(XConceptExtension.KEY_NAME, logName, XConceptExtension.instance());
        logAttributeMap.put(XConceptExtension.KEY_NAME, logConceptNameAttribute);
        XAttribute logTimestampAttribute = factory.createAttributeLiteral(XTimeExtension.KEY_TIMESTAMP, "1970-01-01T00:00:00", XTimeExtension.instance());
        logAttributeMap.put(XTimeExtension.KEY_TIMESTAMP, logTimestampAttribute);

        XLog log = factory.createLog(logAttributeMap);

        HashMap<String, Integer> TraceId_TraceIndex_InLog = new HashMap<>();

        BufferedReader reader;
        List<String> lines = new ArrayList<String>();

        try {

            reader = new BufferedReader(new FileReader(new File(logFile_path)));
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {

                lines.add(currentLine);

            }

            reader.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String lineSplits[] = line.split("\\s");

            if (Functions.isLong(lineSplits[0]) && Functions.isDouble(lineSplits[1])) {

                HashMap<String, String> block = new HashMap<>();

                String timeStamp = lineSplits[1].indexOf('.') != -1 ? lineSplits[1].substring(0, lineSplits[1].indexOf('.')) : lineSplits[1];
                //System.out.println(timeStamp);
                block.put(XTimeExtension.KEY_TIMESTAMP, timeStamp);
                int j = i + 1;
                for (; j < lines.size(); j++) {

                    line = lines.get(j);
                    if (line.startsWith(" - ")) {
                        //System.out.println("line: "+line);
                        lineSplits = line.split("\\s");
                        //System.out.println("line split: "+Arrays.toString(lineSplits));
                        if (lineSplits[2].equals("c")) {
                            block.put("i", lineSplits[4].substring(1, lineSplits[4].indexOf(',')));
                        } else {
                            if (lineSplits[2].equals("payload")) {
                                int indexStartPayload=line.indexOf("payload");
                                indexStartPayload=line.indexOf('{', indexStartPayload)+1;
                                int indexFinishPayload=line.lastIndexOf('}');
                                /*String t2 = lineSplits[4].substring(1, lineSplits[4].length() - 1);
                                System.out.println("t2: "+t2);*/
                                String t1 = line.substring(indexStartPayload, indexFinishPayload);
                                //System.out.println(lineSplits[4]);
                                for (int k = 0; k < 3; k++) {
                                    int indexStart = t1.indexOf('=');
                                    int indexFinish = indexStart;
                                    String variableName = t1.substring(0, indexStart);
                                    if (variableName.equals("name")) {
                                        indexFinish = t1.indexOf('"', indexStart + 2);
                                        String name = t1.substring(indexStart + 2, indexFinish);
                                        block.put(XConceptExtension.KEY_NAME, name);
                                        //System.out.println(name);
                                    } else {
                                        if (variableName.equals("source")) {
                                            indexFinish = t1.indexOf('"', indexStart + 2);
                                            String source = t1.substring(indexStart + 2, indexFinish);
                                            block.put("source", source);
                                            //System.out.println(source);
                                        } else {
                                            //args
                                            indexFinish = t1.indexOf(']', indexStart + 2);
                                            String args = t1.substring(indexStart + 2, indexFinish);
                                            if (args.contains(":")) { //if there is at least one arg, split and insert it
                                                String[] arrayArgs = args.split(",");
                                                //System.out.println(args);
                                                for (int arg_i = 0; arg_i < arrayArgs.length; arg_i++) {
                                                    String variableNameArg_i = arrayArgs[arg_i].substring(1, arrayArgs[arg_i].indexOf(':'));
                                                    String valueArg_i = arrayArgs[arg_i].substring(arrayArgs[arg_i].indexOf(':') + 1, arrayArgs[arg_i].length() - 1);
                                                    block.put(variableNameArg_i, valueArg_i);
                                                    //System.out.println(variableNameArg_i+" ----> "+valueArg_i);
                                                }
                                            }
                                        }
                                    }
                                    if (k < 2)
                                        t1 = t1.substring(indexFinish + 2);
                                }
                                //System.out.println();

                              /*  System.out.println(lineSplits[4]);
                                System.out.println(lineSplits[4].substring(1, lineSplits[4].length()-1));
                                System.out.println(Arrays.toString(payloadSplits));
                                System.out.println();*/
                            }
                        }


                    } else
                        break;

                }
                i = j - 1;


                if (block.containsKey("i")) {

                    XTrace trace = null;
                    XAttributeMap eventAttributeMap = factory.createAttributeMap();

                    XAttribute eventTransitionAttribute = factory.createAttributeLiteral(XLifecycleExtension.KEY_TRANSITION, "complete", XLifecycleExtension.instance());
                    eventAttributeMap.put(XLifecycleExtension.KEY_TRANSITION, eventTransitionAttribute);

                    Iterator<String> it = block.keySet().iterator();
                    while (it.hasNext()) {

                        String key = it.next();
                        if (key.compareToIgnoreCase("i") == 0) {

                            String traceID = block.get(key);
                            Integer TraceIndex_InSubLog = TraceId_TraceIndex_InLog.get(traceID);
                            if (TraceIndex_InSubLog == null) {

                                XAttributeMap traceAttributeMap = factory.createAttributeMap();
                                XAttribute traceConceptNameAttribute = factory.createAttributeLiteral(XConceptExtension.KEY_NAME, block.get(key), XConceptExtension.instance());
                                traceAttributeMap.put(XConceptExtension.KEY_NAME, traceConceptNameAttribute);

                                trace = factory.createTrace(traceAttributeMap);

                                log.add(trace);
                                TraceIndex_InSubLog = log.size() - 1;
                                TraceId_TraceIndex_InLog.put(traceID, TraceIndex_InSubLog);

                            } else {

                                trace = log.get(TraceIndex_InSubLog);

                            }


                        } else if (key.compareToIgnoreCase(XConceptExtension.KEY_NAME) == 0) {

                            XAttribute eventConceptNameAttribute = factory.createAttributeLiteral(XConceptExtension.KEY_NAME, block.get(key), XConceptExtension.instance());
                            eventAttributeMap.put(XConceptExtension.KEY_NAME, eventConceptNameAttribute);

                        } else if (key.compareToIgnoreCase(XTimeExtension.KEY_TIMESTAMP) == 0) {


                            XAttribute eventTimestampAttribute = factory.createAttributeTimestamp(XTimeExtension.KEY_TIMESTAMP, initialTimeEpoch + Long.valueOf(block.get(key)), XTimeExtension.instance());
                            eventAttributeMap.put(XTimeExtension.KEY_TIMESTAMP, eventTimestampAttribute);

                        } else {

                            XAttribute eventOtherAttribute = factory.createAttributeLiteral(key, block.get(key), null);
                            eventAttributeMap.put(key, eventOtherAttribute);

                        }

                    }

                    XEvent event = factory.createEvent(eventAttributeMap);
                    trace.add(event);

                }

            }

        }

        String mxmlLogPath = logFile_path.substring(0, logFile_path.lastIndexOf(".")) + ".mxml";
        ByteArrayOutputStream baos = saveLogInMemory(log, mxmlLogPath);
        mxmlLogPath += ".gz";
        GzipLogAndSaveInDisk(baos, mxmlLogPath);
        //System.exit(-1);
    }

    public static void removeSilentTransitionInCPNLog(String filePath) {

        BufferedReader reader;
        List<String> lines = new ArrayList<String>();

        try {

            reader = new BufferedReader(new FileReader(new File(filePath)));
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {

                lines.add(currentLine);

            }

            reader.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);
            String splits[] = line.split("\\s+");

//			for(String split : splits)
//				System.out.print(split + ",");
//
//			System.out.println();

            if (splits.length > 2 && splits[2].compareToIgnoreCase("@") == 0) {

                lines.remove(i); // remove transaction line
                while (i < lines.size()) {

                    line = lines.get(i);
                    if (line.startsWith(" - "))
                        lines.remove(i); // remove bindings line
                    else
                        break;

                }
                i--;

            }


        }

        BufferedWriter writer;

        try {

            writer = new BufferedWriter(new FileWriter(new File(filePath)));

            for (int i = 0; i < lines.size() - 1; i++) {
                writer.write(lines.get(i) + "\n");
            }

            writer.write(lines.get(lines.size() - 1));

            writer.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static ByteArrayOutputStream saveLogInMemory(XLog log, String logFilePath) {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            XSerializer serializer = getSerializer(logFilePath);
            serializer.serialize(log, os);

        } catch (Exception e) {
            System.out.println("Exception when writing sublog in stream " + e.toString());
        }

        return os;

    }

    public static void GzipLogAndSaveInDisk(ByteArrayOutputStream baos, String logFilePath) {

        try {

            GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(logFilePath));
            gzos.write(baos.toByteArray());
            gzos.flush();
            gzos.close();

        } catch (IOException e) {
            System.out.println("Failed to Gzip and write to disk!!");
        }

    }

    public static XSerializer getSerializer(String logName) {

        XSerializer xs = null;

        if (logName.toLowerCase().endsWith("mxml.gz")) {

            xs = new XMxmlGZIPSerializer();

        } else if (logName.toLowerCase().endsWith("mxml") ||
                logName.toLowerCase().endsWith("xml")) {

            xs = new XMxmlSerializer();

        }

        if (logName.toLowerCase().endsWith("xes.gz")) {

            xs = new XesXmlGZIPSerializer();

        } else if (logName.toLowerCase().endsWith("xes")) {

            xs = new XesXmlSerializer();

        }

        return xs;

    }


}
