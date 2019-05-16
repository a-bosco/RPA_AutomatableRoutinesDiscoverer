package au.edu.unimelb.rpadiscovery;





import java.io.*;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Antonio Bosco on 01/02/19.
 */
public class FoofahParser {



    public static String getFoofahTransformation(String exec, List<String> from, List<String> to, String setting){
        //System.out.println("foofah");
        String output = null;
        StringBuilder sb = valuesToJsonFoofah(from, to);
        try {
            String path = createFile(sb);
            output = execPython(exec, "--input "+path+ " "+setting);
            //System.out.println("path: "+path);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        //System.out.println(sb.toString());
        return output;
    }

    private static StringBuilder valuesToJsonFoofah(List<String> from, List<String> to) {
        StringBuilder sb=new StringBuilder();
        sb.append("{\"InputTable\": [");
        listToStringBuilder(from, sb);
        sb.append("], \"OutputTable\": [");
        listToStringBuilder(to, sb);
        sb.append("]}");
        return sb;
    }

    private static void listToStringBuilder(List<String> list, StringBuilder sb) {
        int i=0;
        for(String value: list){
            sb.append("[\""+value+"\"]");
            if(i==list.size()-1){
                break;
            }
            sb.append(',');
            i++;
        }
    }

    private static String createFile(StringBuilder sb) throws IOException{
        String path = "foofahTEMP.txt";

        File file = new File(path);
        if (!file.exists())
            if (!file.createNewFile())
                new IOException("it is not possible make the file: "+file.getAbsolutePath());
        FileWriter fw = new FileWriter(file);
        fw.write(sb.toString());
        fw.flush();
        fw.close();
        return file.getAbsolutePath();
    }

    public static String execPython(String exec, String parameters) {
        String output = null;
        try {
            String s = null;
            StringBuilder sb = new StringBuilder(3000);

            Process p = Runtime.getRuntime().exec("python " + exec + " " + parameters);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // read the output from the command
            //System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                sb.append(s + "\n");
                //System.out.println(s);
            }
            //System.out.println(sb);
            if (sb.toString().contains("*** Solution Not Found ***"))
                return null;
            else {

                output = sb.substring(sb.indexOf("#\n" +
                        "# Data Transformation\n" +
                        "#") + 26);
            }
            // read any errors from the attempted command
            //System.out.println("Here is the standard error of the command (if any):\n");
            boolean error=false;
            while ((s = stdError.readLine()) != null) {
                error=true;
                sb.append(s+"\n");
            }
            if(error){
                new Exception("Error Foofah: "+sb.toString());
            }

            return output;

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }

    }


}
