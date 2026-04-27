package org.ascn.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.*;

public class ReadCSV {

	static String csvFileName = "C:\\work\\deployment\\01-10-2023\\SI-1216\\Query\\PROD-ManagedAttribute-full-load.csv";
	
	public static void main(String[] args) throws IOException {
		//readFile();
		System.out.println(readCSV().size());
		
		for(Object a:readCSV()) {
			System.out.println(a.toString());
		}
	}
	public static void readFile() throws FileNotFoundException {
		
		//parsing a CSV file into Scanner class constructor  
		Scanner sc = new Scanner(new File(csvFileName));  
		sc.useDelimiter("##");   //sets the delimiter pattern  
		while (sc.hasNext())  //returns a boolean value  
		{  
		System.out.print(sc.next());  //find and returns the next complete token from this scanner  
		}   
		sc.close();  //closes the scanner  
				
	}
	public static List readCSV() throws FileNotFoundException, IOException {
        List rows = new ArrayList < > ();
        BufferedReader br = new BufferedReader(new FileReader(csvFileName));
        String line = "";
        String headerline = br.readLine(); 
        while((line = br.readLine()) != null) { 
           rows.add(line);
        }
             br.close(); 
             return rows;
    
}
	
}
