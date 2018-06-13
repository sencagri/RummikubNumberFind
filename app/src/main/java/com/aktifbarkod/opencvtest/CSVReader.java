package com.aktifbarkod.opencvtest;

import android.renderscript.ScriptGroup;
import android.view.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {
    InputStreamReader stream;

    public CSVReader(InputStreamReader stream)
    {
        this.stream = stream;
    }

    public List<String[]> read(){
        List<String[]> resultList = new ArrayList<String[]>();
        BufferedReader reader = new BufferedReader(stream);

        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null)
            {
                String[] row = csvLine.split(",");
                resultList.add(row);
            }
        }
        catch (IOException ex){
            throw new RuntimeException(ex.getMessage());
        }
        finally {
            try {
                stream.close();
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex.getMessage());
            }
        }
        return  resultList;
    }
}
