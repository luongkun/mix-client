package mix.client.mixui.management.account;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.mojang.util.UUIDTypeAdapter;

import mix.client.mixui.Mix;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.account.skin.SkinDownloader;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.utils.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

/**
 * Resolves the head/face texture file that the UI should draw for an account.
 *
 * <p>Resolution order (first existing readable PNG wins):
 * <ol>
 *   <li>custom avatar ({@code mixclient/avatar/custom-<millis>.png})</li>
 *   <li>{@code mixclient/cache/head/<name>.png}</li>
 *   <li>crop of the local skin ({@code mixclient/cache/skin/<name>.png}) - written to the head path</li>
 *   <li>asynchronous one-shot Mojang download, {@code null} for this frame</li>
 * </ol>
 *
 * <p>{@link #getAvatarFile(Account)} is called from the render loop, so it never blocks,
 * never touches the network, allocates almost nothing and never throws.</p>
 */
public class AvatarManager {

	private static final int FACE_SIZE = 128;
	private static final int SKIN_MIN_WIDTH = 64;
	private static final int SKIN_MIN_HEIGHT = 32;
	private static final int FACE_SOURCE_X = 8;
	private static final int FACE_SOURCE_Y = 8;
	private static final int FACE_SOURCE_SIZE = 8;
	private static final int MAX_CUSTOM_SOURCE_SIZE = 256;
	private static final long CUSTOM_SCAN_INTERVAL = 1000L;

	private static volatile AvatarManager instance;

	private final Set<String> requestedDownloads = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	private final Set<String> loggedFailures = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	private volatile File cachedCustomAvatar;
	private volatile long nextCustomScan;

	private AvatarManager() {
	}

	public static AvatarManager getInstance() {

		AvatarManager local = instance;

		if(local == null) {
			synchronized (AvatarManager.class) {
				local = instance;
				if(local == null) {
					local = new AvatarManager();
					instance = local;
				}
			}
		}

		return local;
	}

	/**
	 * Head image file to draw for the given account, or null when nothing is available yet.
	 * Safe to call from the render thread: never blocks, never performs network access.
	 */
	public File getAvatarFile(Account account) {

		try {

			File customAvatar = getCustomAvatarFile();

			if(isUsable(customAvatar)) {
				return customAvatar;
			}

			FileManager fileManager = getFileManager();

			if(fileManager == null) {
				return null;
			}

			File cacheDir = fileManager.getCacheDir();

			if(cacheDir == null) {
				return null;
			}

			Session session = Minecraft.getMinecraft().getSession();
			String name = account != null ? account.getName()
					: session == null ? null : session.getUsername();
			String uuid = account != null ? account.getUuid()
					: session == null ? null : session.getPlayerID();

			if(name == null || name.length() == 0) {
				return null;
			}

			File headDir = new File(cacheDir, "head");
			File headFile = new File(headDir, name + ".png");
			File launcherSkin = getLauncherSkinFile(name);

			if(isUsable(headFile) && SkinDownloader.isFaceShaped(headFile)
					&& (launcherSkin == null || headFile.lastModified() >= launcherSkin.lastModified())) {
				return headFile;
			}

			File skinFile = account == null ? null : account.getSkinFile();

			if(!isUsable(skinFile)) {
				skinFile = new File(new File(cacheDir, "skin"), name + ".png");
			}
			if(!isUsable(skinFile)) {
				skinFile = launcherSkin;
			}

			if(isUsable(skinFile)) {

				File croppedFile = cropFace(skinFile, headFile);

				if(isUsable(croppedFile)) {
					return croppedFile;
				}
			}

			requestDownload(headDir, name, uuid);

			return null;
		} catch (Throwable t) {
			logFailure("avatar-resolve", "Failed to resolve the avatar file", t);
			return null;
		}
	}

	/** Newest custom avatar PNG, or null when the user has not picked one. */
	public File getCustomAvatarFile() {

		try {

			File cached = cachedCustomAvatar;

			if(cached != null) {

				if(isUsable(cached)) {
					return cached;
				}

				cachedCustomAvatar = null;
			}

			long now = System.currentTimeMillis();

			if(now < nextCustomScan) {
				return null;
			}

			File dir = getAvatarDir();

			if(dir == null || !dir.isDirectory()) {
				nextCustomScan = now + CUSTOM_SCAN_INTERVAL;
				return null;
			}

			File newest = findNewestPng(dir);

			if(newest != null) {
				cachedCustomAvatar = newest;
				return newest;
			}

			nextCustomScan = now + CUSTOM_SCAN_INTERVAL;

			return null;
		} catch (Throwable t) {
			logFailure("avatar-custom-scan", "Failed to scan the custom avatar directory", t);
			return null;
		}
	}

	public boolean hasCustomAvatar() {
		return getCustomAvatarFile() != null;
	}

	/**
	 * Copies/ re-encodes the picked image into {@code mixclient/avatar/custom-<millis>.png} and removes
	 * the previously installed custom avatar files. The file name is always new because the
	 * NanoVG asset cache is keyed by file name.
	 */
	public void setCustomAvatar(File source) {

		if(source == null) {
			return;
		}

		try {

			File dir = getAvatarDir();

			if(dir == null) {
				logFailure("avatar-custom-no-dir", "The custom avatar directory is unavailable", null);
				return;
			}

			if(!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
				logFailure("avatar-custom-mkdir", "Failed to create the custom avatar directory", null);
				return;
			}

			BufferedImage image = ImageIO.read(source);

			if(image == null) {
				logFailure("avatar-custom-decode:" + source.getName(), "Unable to decode the custom avatar image " + source.getName(), null);
				return;
			}

			if(image.getWidth() > MAX_CUSTOM_SOURCE_SIZE || image.getHeight() > MAX_CUSTOM_SOURCE_SIZE) {
				image = ImageUtils.resize(image, FACE_SIZE, FACE_SIZE);
			}

			File target = newUniqueFile(dir);

			if(!ImageIO.write(image, "png", target)) {
				logFailure("avatar-custom-write:" + source.getName(), "Unable to encode the custom avatar image " + source.getName(), null);
				return;
			}

			if(!isUsable(target)) {
				logFailure("avatar-custom-empty:" + source.getName(), "The written custom avatar image is empty", null);
				return;
			}

			cachedCustomAvatar = target;
			nextCustomScan = 0L;

			deleteFiles(dir, target);
		} catch (Throwable t) {
			logFailure("avatar-custom-set:" + source.getName(), "Failed to install the custom avatar", t);
		}
	}

	public void clearCustomAvatar() {

		try {

			File dir = getAvatarDir();

			cachedCustomAvatar = null;
			nextCustomScan = 0L;

			if(dir == null || !dir.isDirectory()) {
				return;
			}

			deleteFiles(dir, null);
		} catch (Throwable t) {
			logFailure("avatar-custom-clear", "Failed to clear the custom avatar", t);
		}
	}

	private File cropFace(File skinFile, File headFile) {

		File headDir = headFile.getParentFile();

		try {

			if(!headDir.isDirectory() && !headDir.mkdirs() && !headDir.isDirectory()) {
				logFailure("avatar-head-mkdir", "Failed to create the head cache directory", null);
				return null;
			}

			BufferedImage skin = ImageIO.read(skinFile);

			if(skin == null) {
				logFailure("avatar-skin-decode:" + skinFile.getName(), "The skin file is not a readable image: " + skinFile.getName(), null);
				return null;
			}

			if(skin.getWidth() < SKIN_MIN_WIDTH || skin.getHeight() < SKIN_MIN_HEIGHT) {
				logFailure("avatar-skin-small:" + skinFile.getName(), "The skin is too small to crop a face: " + skinFile.getName() + " (" + skin.getWidth() + "x" + skin.getHeight() + ")", null);
				return null;
			}

			BufferedImage face = skin.getSubimage(FACE_SOURCE_X, FACE_SOURCE_Y, FACE_SOURCE_SIZE, FACE_SOURCE_SIZE);
			BufferedImage scaled = ImageUtils.resize(face, FACE_SIZE, FACE_SIZE);

			File tmpFile = new File(headDir, headFile.getName() + ".tmp-" + System.currentTimeMillis());

			try {

				if(!ImageIO.write(scaled, "png", tmpFile)) {
					logFailure("avatar-head-write:" + skinFile.getName(), "Failed to encode the face image for " + skinFile.getName(), null);
					return null;
				}

				if(headFile.exists()) {
					headFile.delete();
				}

				if(!tmpFile.renameTo(headFile) && !copyFile(tmpFile, headFile)) {
					logFailure("avatar-head-move:" + skinFile.getName(), "Failed to move the face image into place for " + skinFile.getName(), null);
					return null;
				}
			} finally {
				if(tmpFile.exists()) {
					tmpFile.delete();
				}
			}

			return isUsable(headFile) ? headFile : null;
		} catch (Throwable t) {
			logFailure("avatar-head-crop:" + skinFile.getName(), "Failed to crop the face out of the skin " + skinFile.getName(), t);
			return null;
		}
	}

	private void requestDownload(final File headDir, final String name, final String uuidString) {

		final String key = name.toLowerCase(Locale.ROOT);

		if(!requestedDownloads.add(key)) {
			return;
		}

		try {

			if(!headDir.isDirectory() && !headDir.mkdirs() && !headDir.isDirectory()) {
				return;
			}

			final UUID uuid = parseUuid(uuidString);

			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {

					try {
						new SkinDownloader().downloadFace(headDir, name, uuid);
					} catch (Throwable t) {
						logFailure("avatar-download:" + key, "Failed to download the avatar for " + name, t);
					}
				}
			}, "Mix-Avatar-" + name);

			thread.setDaemon(true);
			thread.start();
		} catch (Throwable t) {
			logFailure("avatar-download-start:" + key, "Failed to start the avatar download for " + name, t);
		}
	}

	private static File getLauncherSkinFile(String name) {

		String appData = System.getenv("APPDATA");

		if(appData == null || appData.length() == 0) {
			return null;
		}

		File file = new File(appData,
				"mixclient-launcher/skin-cache/" + name.toLowerCase(Locale.ROOT) + ".png");

		return isUsable(file) ? file : null;
	}

	private static UUID parseUuid(String uuidString) {

		if(uuidString == null || uuidString.length() == 0) {
			return null;
		}

		try {
			return UUIDTypeAdapter.fromString(uuidString);
		} catch (Throwable t) {
			return null;
		}
	}

	private static File newUniqueFile(File dir) {

		File target = new File(dir, "custom-" + System.currentTimeMillis() + ".png");
		int attempt = 0;

		while(target.exists() && attempt < 64) {
			attempt++;
			target = new File(dir, "custom-" + System.currentTimeMillis() + "-" + attempt + ".png");
		}

		return target;
	}

	private static File findNewestPng(File dir) {

		File[] files = dir.listFiles();
		File newest = null;
		long newestTime = Long.MIN_VALUE;

		if(files == null) {
			return null;
		}

		for(File file : files) {

			if(!file.isFile() || file.length() <= 0 || !file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
				continue;
			}

			long modified = file.lastModified();

			if(newest == null || modified > newestTime) {
				newest = file;
				newestTime = modified;
			}
		}

		return newest;
	}

	private void deleteFiles(File dir, File keep) {

		File[] files = dir.listFiles();

		if(files == null) {
			return;
		}

		for(File file : files) {

			if(!file.isFile() || (keep != null && file.equals(keep))) {
				continue;
			}

			if(!file.delete()) {
				logFailure("avatar-delete:" + file.getName(), "Failed to delete the avatar file " + file.getName(), null);
			}
		}
	}

	private static boolean copyFile(File from, File to) {

		InputStream in = null;
		OutputStream out = null;

		try {

			in = new FileInputStream(from);
			out = new FileOutputStream(to);

			byte[] buffer = new byte[8192];
			int read;

			while((read = in.read(buffer)) > 0) {
				out.write(buffer, 0, read);
			}

			out.flush();

			return true;
		} catch (Throwable t) {
			return false;
		} finally {

			closeQuietly(in);
			closeQuietly(out);
		}
	}

	private static void closeQuietly(java.io.Closeable closeable) {

		if(closeable == null) {
			return;
		}

		try {
			closeable.close();
		} catch (Throwable ignored) {
		}
	}

	private static boolean isUsable(File file) {

		if(file == null) {
			return false;
		}

		try {
			return file.isFile() && file.length() > 0 && file.canRead();
		} catch (Throwable t) {
			return false;
		}
	}

	private static FileManager getFileManager() {

		try {

			Mix mix = Mix.getInstance();

			return mix == null ? null : mix.getFileManager();
		} catch (Throwable t) {
			return null;
		}
	}

	private static File getAvatarDir() {

		FileManager fileManager = getFileManager();

		if(fileManager == null) {
			return null;
		}

		File mixDir = fileManager.getMixDir();

		return mixDir == null ? null : new File(mixDir, "avatar");
	}

	private void logFailure(String key, String message, Throwable throwable) {

		if(!loggedFailures.add(key)) {
			return;
		}

		try {

			if(throwable instanceof Exception) {
				MixLogger.error(message, (Exception) throwable);
			} else if(throwable != null) {
				MixLogger.error(message + " (" + throwable + ")", new Exception(throwable));
			} else {
				MixLogger.error(message);
			}
		} catch (Throwable ignored) {
		}
	}
}
