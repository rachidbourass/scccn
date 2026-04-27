package org.ascn.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.lang3.SystemUtils;

public class SearchInFiles {

	private List<java.lang.String> matchesList;
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String rootPath = "C:\\work\\repos\\identityIQ\\config\\Rule"; 
		Path p = Paths.get(rootPath);
		//searchWithWc(p,"Report of Expiring AD Accounts Before:");
		
		 
		File fObj = new File(rootPath);  
		if(fObj.exists() && fObj.isDirectory())  
		{  
		// array for the files of the directory pointed by fObj  
		File a[] = fObj.listFiles();  
		// display statements  
		System.out.println("= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =");  
		System.out.println("Displaying Files from the directory: " + fObj);  
		System.out.println("= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =");  
		// Calling the method  
		SearchInFiles obj = new SearchInFiles();
		obj.printFileNames(a, 0, 0);  
		}  
	}

	
	public List<String> search(String inStr, String inFilePath) throws IOException {

	    List<String> found = new ArrayList<>();
	    if (SystemUtils.IS_OS_WINDOWS) {
	    	ProcessBuilder builder = new ProcessBuilder(
	                "cmd.exe", "/c", "findstr /i /r /m \"" + inStr +"\" "+inFilePath);
	        builder.redirectErrorStream(true);
	        Process p = builder.start();
	        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line;
	        while (true) {
	            line = r.readLine();
	            if (line == null) {
	                break;
	            }
	            System.out.println("File: "+inFilePath+" \t"+line);
	            found.add(line);
	        }

	    } else if (SystemUtils.IS_OS_LINUX) {

	        // well. do the same thing but with grep LOL

	    } else {
	        //logger.error("So not supported.");
	    }
	  
	    return found;
	}
	
	@SuppressWarnings("hiding")
	public static List<String> searchWithWc(Path rootDir, String pattern) throws IOException {
		    List<String> matchesList = new ArrayList<>();
	    	matchesList.clear();
	        FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<Path>() {
	            @Override
	            public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) throws IOException {
	                FileSystem fs = FileSystems.getDefault();
	                PathMatcher matcher = fs.getPathMatcher(pattern);
	                Path name = file.getFileName();
	                if (matcher.matches(name)) {
	                    matchesList.add(name.toString());
	                }
		        return FileVisitResult.CONTINUE;
	            }
	        };
	        Files.walkFileTree(rootDir, matcherVisitor);
	        return matchesList;
	    }
	public void printFileNames(File[] a, int i, int lvl) throws IOException  
	{  
    List<String> outList = new ArrayList();
	// base case of the recursion  
	// i == a.length means the directory has   
	// no more files. Hence, the recursion has to stop  
	if(i == a.length)  
	{  
	return;  
	}  
	// tabs for providing the indentation  
	// for the files of sub-directory  
	for (int j = 0; j < lvl; j++)  
	{  
	System.out.print("\t");  
	}  
	// checking if the encountered object is a file or not  
	if(a[i].isFile())  
	{  
	//System.out.println(a[i].getAbsolutePath()+" - "+a[i].getName());  
		search("Report of Expiring AD Accounts Before:",a[i].getAbsolutePath());
	/*System.out.println("Search List Size: "+searchList.size());
		if(!searchList.isEmpty()) {
			outList.add(a[i].getAbsolutePath());
			System.out.println(a[i].getAbsolutePath()+" - "+a[i].getName()); 
		}*/
	}  
	// for sub-directories  
	else if(a[i].isDirectory())  
	{  
	System.out.println("[" + a[i].getName() + "]");  
	// recursion for sub-directories  
	printFileNames(a[i].listFiles(), 0, lvl + 1);  
	}  
	// recursively printing files from the directory  
	// i + 1 means look for the next file  
	printFileNames(a, i + 1, lvl);  
	}  
	
	public void searchOldWay(String inStr, String inFilePath) throws IOException {
		try {
	        int count = 0;
	        FileReader fileIn = new FileReader(inFilePath);
	        BufferedReader reader = new BufferedReader(fileIn);
	        String line;
	        while((line = reader.readLine()) != null) {
	            if((line.contains(inStr))) {
	                count++;
	                System.out.println(inFilePath+" Number of instances of String " + count);
	            }
	        }
	    }catch (IOException e){
	        System.out.println(e);
	    }
		
		}
}
