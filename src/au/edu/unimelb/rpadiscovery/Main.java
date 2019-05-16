package au.edu.unimelb.rpadiscovery;

import au.edu.unimelb.rpadiscovery.Utils.Log;

import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.CPNtoXES;



import java.io.File;
import java.util.*;

import static au.edu.unimelb.rpadiscovery.Utils.Functions.tryParseDouble;
import static au.edu.unimelb.rpadiscovery.Utils.LogTAG.*;

public class Main {

    public static Log log;


    public static void main(String[] args) throws Exception {
        String fileName=null;
        String foofahPath=null;
        String foofahTimeOut="";
        for(int i=0; i<args.length-1; i++){
            String cmd=args[i].trim();
            switch (cmd){
                case "-L":
                    fileName=args[i+1].trim();
                    break;
                case "-FP":
                    foofahPath=args[i+1].trim();
                    break;
                case "-FT":
                    foofahTimeOut ="--timeout "+args[i+1].trim();
                    break;
            }

        }

        checkArgs(fileName, foofahPath, foofahTimeOut);


        //fileName = "C:\\Users\\Antonio\\Desktop\\TESI\\CPN\\SIMULATION\\TEST\\output\\runningExample.txt";
        log = Log.getInstance();
        List<String> tags = Arrays.asList();//TAG, TAG_DAFSA, TAG_RPST, TAG_FLAT_POLYGONS, TAG_FLAT_POLYGONS_IN_TRACES, TAG_AUTOMATABLE_POLYGONS_IN_TRACES);

        boolean showTimestamp = false;
        boolean showTag = false;
        int detailsLevel = 3;
        log.settingsLog(tags, showTimestamp, showTag, detailsLevel);




        log.println(TAG_IMPORT, 1, "input file: " + fileName);
        //System.exit(-1);
        //*************************************CONVERT CPN SIMULATION IN MXML***********************************************
        //
        //
        if (fileName.endsWith(".txt")) {
            CPNtoXES.convertCPNLogToMXML(fileName);
            fileName = fileName.replace(".txt", ".mxml.gz");
        }

        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************

        //******************************************DISCOVER RULES**********************************************************
        //
        //
        RpaDiscovery rpaDiscovery = new RpaDiscovery();
        rpaDiscovery.startProcedureDiscovery(fileName, foofahPath, foofahTimeOut);
        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************




        long timestamp= System.currentTimeMillis();


        timestamp=System.currentTimeMillis()-timestamp;
        System.out.println("Time: "+((double)timestamp)/1000+" sec");
        System.exit(0);
    }

    private static void checkArgs(String fileName, String foofahPath, String foofahTimeOut) {
        if(fileName==null){
            System.err.println("no input file");
            System.exit(-1);
        }
        if (!fileName.endsWith(".txt") && !fileName.endsWith(".xes") && !fileName.endsWith("mxml.gz")) {
            System.err.println("wrong extension input file, extension accepted: .txt, .mxml.gz, .xes");
            System.exit(-1);
        }
        File file = new File(fileName);
        if (!file.exists()) {
            System.err.println("simulation file not exist");
            System.exit(-1);
        }
        if(foofahPath!=null) {
            file = new File(foofahPath);
            if (!file.exists()) {
                System.err.println("Foofah file not exist");
                System.exit(-1);
            }
            if(!foofahTimeOut.equals("") || !tryParseDouble(foofahTimeOut)){
                System.err.println("Foofah time out wrong");
                System.exit(-1);
            }

        }

    }




}
