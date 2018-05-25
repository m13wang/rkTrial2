package dashboard.rk.com.rktrial2;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by MarkW on 5/24/2018.
 */

public class DataLogger {
    private static File file;
    public static void init (){

           /* create a file */
            file = null;
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            try {

                //Specify the file name and path here
                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                file = new File(path, dateFormat.format(date) + "logfile.csv");

                            /* Create the file if the file is not already present */
                if (!file.exists()) {
                    file.createNewFile();
                }

            } catch (IOException ioe) {
                System.out.println("Exception occurred:");
                ioe.printStackTrace();
            }

    }

    public static void append (String dataline){

        // write the line
        try{
            //Here true is to append the content to file
            FileWriter fw = new FileWriter(file,true);
            //BufferedWriter writer give better performance
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(dataline);
            //Closing BufferedWriter Stream
            bw.close();

            //System.out.println("Data successfully appended at the end of file");

        }catch(IOException ioe){
            System.out.println("Exception occurred:");
            ioe.printStackTrace();
        }
    }
}
