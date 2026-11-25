package mix.client.mixui.management.download;

import java.io.File;
import java.util.ArrayList;

import mix.client.mixui.Mix;
import mix.client.mixui.management.download.file.DownloadFile;
import mix.client.mixui.management.download.file.DownloadZipFile;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.utils.Multithreading;
import mix.client.mixui.utils.file.FileUtils;
import mix.client.mixui.utils.network.HttpUtils;

public class DownloadManager {

	private ArrayList<DownloadFile> downloadFiles = new ArrayList<DownloadFile>();
	
	private boolean downloaded;
	
	public DownloadManager() {
		
		FileManager fileManager = Mix.getInstance().getFileManager();
		
		downloaded = false;
		
		/* Mix Client ships without a host of its own, so the optional tools are
		   opt-in through system properties. A blank property (the default) means
		   the tool is absent and nothing is fetched -- no outbound call at all.
		   The configured URLs must serve the same artifacts, so the size checks
		   below stay valid. */
		String ytdlpUrl = System.getProperty("mixclient.ytdlpUrl", "");
		String ffmpegUrl = System.getProperty("mixclient.ffmpegUrl", "");
		String cefUrl = System.getProperty("mixclient.cefUrl", "");
		
		if(!ytdlpUrl.isEmpty()) {
			downloadFiles.add(new DownloadFile(ytdlpUrl,
					"ytdlp.exe", new File(fileManager.getExternalDir(), "ytdlp"), 13947126));
		}
		if(!ffmpegUrl.isEmpty()) {
			downloadFiles.add(new DownloadZipFile(ffmpegUrl,
					"ffmpeg.zip", new File(fileManager.getExternalDir(), "ffmpeg"), 51986763, 147600195));
		}
		if(!cefUrl.isEmpty()) {
			downloadFiles.add(new DownloadZipFile(cefUrl,
					"cef.zip", new File(fileManager.getExternalDir(), "cef"), 115822583, 265676507));
		}
		
		Multithreading.runAsync(() -> startDownloads());
	}
	
	private void startDownloads() {
		
		for(DownloadFile df : downloadFiles) {
			
			if(!df.getOutputDir().exists()) {
				df.getOutputDir().mkdirs();
			}
			
			if(df instanceof DownloadZipFile) {
				
				DownloadZipFile dzf = (DownloadZipFile) df;
				
				if(FileUtils.getDirectorySize(dzf.getOutputDir()) != dzf.getUnzippedSize()) {
					
					File outputFile = new File(dzf.getOutputDir(), dzf.getFileName());
					
					HttpUtils.downloadFile(dzf.getUrl(), outputFile);
					FileUtils.unzip(outputFile, dzf.getOutputDir());
					outputFile.delete();
				}
			} else {
				
				File outputFile = new File(df.getOutputDir(), df.getFileName());
				
				if(outputFile.length() != df.getSize()) {
					HttpUtils.downloadFile(df.getUrl(), outputFile);
				}
			}
		}
		
		checkFiles(0);
	}
	
	/* Bounded retry: an unreachable configured URL used to recurse forever and
	   blow the stack on the download thread. */
	private void checkFiles(int attempt) {
		
		for(DownloadFile df : downloadFiles) {
			
			boolean complete;
			
			if(df instanceof DownloadZipFile) {
				
				DownloadZipFile dzf = (DownloadZipFile) df;
				complete = FileUtils.getDirectorySize(dzf.getOutputDir()) == dzf.getUnzippedSize();
			} else {
				
				File outputFile = new File(df.getOutputDir(), df.getFileName());
				complete = outputFile.length() == df.getSize();
			}
			
			if(!complete) {
				if(attempt < 3) {
					startDownloads();
				} else {
					MixLogger.error("Optional download failed after 3 attempts: " + df.getFileName());
				}
				return;
			}
		}
		
		downloaded = true;
	}

	public boolean isDownloaded() {
		return downloaded;
	}
}