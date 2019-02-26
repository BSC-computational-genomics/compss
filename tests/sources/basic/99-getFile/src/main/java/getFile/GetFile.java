package getFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import es.bsc.compss.api.*;
import java.lang.Thread;
import java.util.ArrayList;

public class GetFile {
   
    private static final String FILE_NAME="/tmp/text.txt";
    private static final int TASK_SLEEP_TIME = 2_000; // ms
    private static final int M = 5; // number of tasks to be executed
    
    public static void main(String[] args) throws Exception {
        
       // Initialize test file
        try {
            newFile(FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Execute task loop
        for (int i=0; i<M; i++) {
            GetFileImpl.writeInFile(FILE_NAME, i);
        }
        
        //Get the renamed file
        COMPSs.getFile(FILE_NAME);
       
        //Shell commands to execute to check contents of file
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("/bin/cat");
        commands.add(FILE_NAME);
        
        //ProcessBuilder start
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        Process process = null;
        try {
            process = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        //Read file content
        readContents(process);

        //Check result
        try {
            if (process.waitFor() == 0) {
                System.exit(0);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }


    
    private static void newFile(String fileName) throws IOException {
        File file = new File(fileName);
        // Delete previous occurrences of the file
        if (file.exists()) {
            file.delete();
        }
        // Create the file and directories if required
        boolean createdFile = file.createNewFile();
        if (!createdFile) {
            throw new IOException("[ERROR] Cannot create test file");
        }
    }
    
    private static void readContents (Process process) throws Exception {
       //Read output of file
        StringBuilder out = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null, previous = null;
        int count = 0;
        try {
            while ((line = br.readLine()) != null)
                if (!line.equals(previous)) {
                    previous = line;
                    out.append(line).append('\n');
                    count = count + 1;
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Exception if number of writers has not been correct
        if (count != M) {
            throw new Exception("Incorrect number of writers " + count);
        }
    }

}