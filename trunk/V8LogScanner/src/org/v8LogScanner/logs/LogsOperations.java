package org.v8LogScanner.logs;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.v8LogScanner.commonly.Constants;
import org.v8LogScanner.commonly.ExcpReporting;
import org.v8LogScanner.commonly.ProcessListener;
import org.v8LogScanner.commonly.SimpleTable;
import org.v8LogScanner.commonly.Strokes;
import org.v8LogScanner.commonly.fsys;
import org.v8LogScanner.rgx.ScanProfile;
import org.v8LogScanner.rgx.ScanProfile.DateRanges;
import org.v8LogScanner.rgx.ScanProfile.LogTypes;

public class LogsOperations extends ProcessListener  {
	
	private static SimpleTable logFiles = new SimpleTable("filePath, size, fileDate");
	
	private LinkedBlockingQueue<String> processingInfo = new LinkedBlockingQueue<>();
	
	public List<String> readLogFiles(ScanProfile profile){
		
		logFiles.clear();
		
		List<String> sourceLogPaths = profile.getLogPaths();
		LogTypes logType            = profile.getLogType();
		
		//java.beans.
		processingInfo.add("*ACCESSING LOG FILES...");
		
		invoke(getProcessingInfo());
		
		for (String fileName : sourceLogPaths){
			
			String dirPattern = getDirPattern(logType);
			Date startDate = getStartDatePattern(profile);
			Date endDate = getEndDatePattern(profile);
			
			String checkedDirPattern = "";
			if (Files.isDirectory(Paths.get(fileName)));
				checkedDirPattern = dirPattern;
				
			LogsDirVisitor visitor = new LogsDirVisitor(checkedDirPattern,  startDate, endDate); 
			try {
				Files.walkFileTree(Paths.get(fileName), visitor);
			} catch (IOException e) {
				ExcpReporting.LogError(LogsOperations.class, e);
			}
			SimpleTable result = visitor.getLogFilesData();
			
			// sorting and creating sorted table
			result = result.
				stream().
				sorted((n1, n2) -> {
						Date d1 = (Date) n1.get("fileDate");
						Date d2 = (Date) n2.get("fileDate");
						return d1.compareTo(d2);
					}
				).collect(
						() -> new SimpleTable("filePath, size, fileDate"), 
						(n, t) ->{ 
							SimpleTable.Entry entry =  n.addRow();
							entry.putAll(t);
						}, 
						(n, t)-> n.addAll(t));
			
			saveProcessingInfo(result);
			
			invoke(getProcessingInfo());
			
			logFiles.addAll(result);
		}
		
		processingInfo.add(getTotalSizeDescr());
		
		return getLogFiles();
	}
	
	public ArrayList<String> getLogFiles(){
		return logFiles.unloadColumn("filePath", new ArrayList<String>());
	}
	
	public static ArrayList<String> scanLogsInCfgFile(){
		
		List<String> logcfg = Constants.V8_Dirs();
		
		ArrayList<Path> cfgFiles = new ArrayList<Path>(); 
		SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>(){
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path lastName =  file.getFileName();
				if (lastName != null && lastName.toString().matches("(?i)logcfg.xml"))
					cfgFiles.add(file);
				return FileVisitResult.CONTINUE;
			}
		};
		
		try {
			for (String location : logcfg){
				Files.walkFileTree(Paths.get(location), visitor);
			}
		} catch (IOException e) {
			//to do: need a feature that add log location specified by user
			///ExcpReporting.LogError(LogsOperations.class, e);
		}
		
		Pattern pattern = Pattern.compile("<log.*?\\blocation\\b=\".+?(?=\")", 
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		
		ArrayList<String> logPaths = new ArrayList<>();
		for(Path filePath : cfgFiles){
			String cgfString = fsys.readAllFromFile(filePath);
			Matcher matcher = pattern.matcher(cgfString);
			while(matcher.find())
				logPaths.add(matcher.group().replaceFirst("<log.*?\\blocation\\b=\"", ""));
		}
		
		return logPaths;
	}
	
	private void saveProcessingInfo(SimpleTable _logFiles){
		
		for (SimpleTable.Entry row : _logFiles){
			processingInfo.offer(String.format(
				"%1s; %2.3f mb; ", row.get("filePath"), row.get("size")));
		}
	}
	
	public ArrayList<String> getProcessingInfo(){
		
		ArrayList<String> result = new ArrayList<>(); 
		processingInfo.forEach(n -> result.add(processingInfo.poll()));
		
		return result;
	}
	
	public static String getTotalSizeDescr(){
		
		return String.format("%s files, size %2.3f mb", 
			logFiles.size(), logFiles.total("size"));
	}
	
	private static String getDirPattern(LogTypes logType){
		
		switch (logType){
		case RPHOST:
			return "rphost_\\d+";
		case CLIENT:
			return "1cv8.+";
		case ANY:
			return ".*";
		}
		return "";
	}
	
	private static Date getStartDatePattern(ScanProfile profile){
		
		DateRanges dateRanges = profile.getDateRange();
		String userStartDate  = profile.getUserPeriod()[0];
		
		Calendar currCalendar = Calendar.getInstance();
		
		if(dateRanges != DateRanges.LAST_HOUR){
			currCalendar.set(Calendar.HOUR_OF_DAY, 0);
		}
		currCalendar.set(Calendar.MINUTE, 0);
		currCalendar.set(Calendar.SECOND, 0);
		currCalendar.set(Calendar.MILLISECOND, 0);
		
		if(dateRanges == DateRanges.ANY){
			return null;
		}
		else if(dateRanges == DateRanges.LAST_HOUR){
			currCalendar.add(Calendar.HOUR_OF_DAY, -1);
		}
		else if(dateRanges == DateRanges.YESTERDAY){
			currCalendar.add(Calendar.DAY_OF_MONTH, -1);
		}
		else if(dateRanges == DateRanges.PREV_WEEK){
			currCalendar.add(Calendar.WEEK_OF_MONTH, -1);
			currCalendar.set(Calendar.DAY_OF_WEEK, currCalendar.getFirstDayOfWeek());
		}
		else if (dateRanges == DateRanges.PREV_MONTH){
			currCalendar.add(Calendar.MONTH, -1);
			currCalendar.set(Calendar.DAY_OF_MONTH, 1);
		}
		else if (dateRanges == DateRanges.THIS_MONTH){
			currCalendar.set(Calendar.DAY_OF_MONTH, 1);
		}
		else if (dateRanges == DateRanges.THIS_WEEK){
			currCalendar.set(Calendar.DAY_OF_WEEK, currCalendar.getFirstDayOfWeek());
		}
		else if (dateRanges == DateRanges.SET_OWN){
			if (userStartDate == null || userStartDate.isEmpty())
				return null;
			currCalendar.setTime(parseDate(userStartDate));
		}
		return currCalendar.getTime();
	}
	
	private static Date getEndDatePattern(ScanProfile profile){
		
		DateRanges dateRanges = profile.getDateRange();
		String userEndDate    = profile.getUserPeriod()[1];
		
		Calendar currCalendar = Calendar.getInstance();
		
		if(dateRanges != DateRanges.LAST_HOUR){
			currCalendar.set(Calendar.HOUR_OF_DAY, 0);
		}

		if(dateRanges == DateRanges.ANY){
			return null;
		}
		else if(dateRanges == DateRanges.YESTERDAY){
			currCalendar.add(Calendar.DAY_OF_MONTH, -1);
		}
		else if(dateRanges == DateRanges.PREV_WEEK){
			currCalendar.add(Calendar.WEEK_OF_MONTH, -1);
			currCalendar.set(Calendar.DAY_OF_WEEK, 7);
		}
		else if (dateRanges == DateRanges.PREV_MONTH){
			currCalendar.set(Calendar.DAY_OF_MONTH, 1);
			currCalendar.add(Calendar.DAY_OF_MONTH, -1);
		}
		else if (dateRanges == DateRanges.THIS_MONTH){
			currCalendar.set(Calendar.DAY_OF_MONTH, 1);
			currCalendar.add(Calendar.MONTH, 1);
			currCalendar.add(Calendar.DAY_OF_MONTH, -1);
		}
		else if (dateRanges == DateRanges.THIS_WEEK){
			currCalendar.set(Calendar.DAY_OF_WEEK, 7);
		}
		else if (dateRanges == DateRanges.SET_OWN){
			if (userEndDate == null || userEndDate.isEmpty())
				return null;
			currCalendar.setTime(parseDate(userEndDate));
		}
		currCalendar.set(Calendar.HOUR_OF_DAY, 23);
		currCalendar.set(Calendar.MINUTE, 59);
		currCalendar.set(Calendar.SECOND, 59);
		currCalendar.set(Calendar.MILLISECOND, 0);
		return currCalendar.getTime();
	}
	
	public static Date parseDate(String date_str){
		
		Calendar currCalendar = Calendar.getInstance();
		
		if (date_str.length() < 8 || !Strokes.isNumeric(date_str.substring(0, 8))){
			currCalendar.set(2099, 12, 31, 23, 59, 59);
			return currCalendar.getTime();
		}
		
		// parseDate must be strictly conform to the format- yymmddhh.

		try{ 
			int currYear =  Integer.parseInt("20" + date_str.substring(0, 2));
			int currMonth =  Integer.parseInt(date_str.substring(2, 4));
			int currDay =  Integer.parseInt(date_str.substring(4, 6));
			int currHour =  Integer.parseInt(date_str.substring(6, 8));
			
			currCalendar.set(Calendar.YEAR, currYear);
			// months beginning with 0;
			currCalendar.set(Calendar.MONTH, currMonth - 1);
			currCalendar.set(Calendar.DAY_OF_MONTH, currDay);
			currCalendar.set(Calendar.HOUR_OF_DAY, currHour);
			currCalendar.set(Calendar.MINUTE, 59);
			currCalendar.set(Calendar.SECOND, 59);
			currCalendar.set(Calendar.MILLISECOND, 0);
		}
		catch (IndexOutOfBoundsException | NumberFormatException x){
			System.err.format("Class %s Error: incorrect format for a .log file in ParseDate(). It must be yymmddhh", x);
		}
		
		return currCalendar.getTime();
	}
	
	public static String getDatePresentation(ScanProfile profile){
		
		DateRanges dateRange = profile.getDateRange();
		if(dateRange == DateRanges.ANY)
			return dateRange.toString();
		else if (dateRange == DateRanges.LAST_HOUR){
			Date startDate = getStartDatePattern(profile);
			Date endDate = getEndDatePattern(profile);
			return String.format("%1$thh - %2$thh", startDate, endDate);
		}
		else{
			Date startDate = getStartDatePattern(profile);
			Date endDate = getEndDatePattern(profile);
			return String.format("%1$tY.%1$tm.%1$td - %2$tY.%2$tm.%2$td", startDate, endDate);
		}
	}
	
}

