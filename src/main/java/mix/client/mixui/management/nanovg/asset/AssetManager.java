package mix.client.mixui.management.nanovg.asset;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.system.MemoryUtil;

import mix.client.mixui.logger.MixLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class AssetManager {

	private Minecraft mc = Minecraft.getMinecraft();
	
	private HashMap<String, NVGAsset> imageCache = new HashMap<String, NVGAsset>();
	private HashMap<Integer, Integer> glTextureCache = new HashMap<Integer, Integer>();
	private HashMap<String, NVGAsset> svgCache = new HashMap<String, NVGAsset>();
	
	// File name -> the cache key that file name currently owns. Avatar textures are rewritten
	// in place (a freshly downloaded face replaces the placeholder of the same name), so a pure
	// file name key would keep serving the first bytes it ever saw until the game restarts.
	private HashMap<String, String> fileCacheKeys = new HashMap<String, String>();
	
	public boolean loadImage(long nvg, int texture, float width, float height) {
		
		if(!glTextureCache.containsKey(texture)) {
			
			glTextureCache.put(texture, NanoVGGL2.nvglCreateImageFromHandle(nvg, texture, (int) width, - (int) height, 0));
			
			return true;
		}
		
		return true;
	}
	
	public boolean loadImage(long nvg, ResourceLocation location) {
		
		if(!imageCache.containsKey(location.getResourcePath())) {
			
			try (InputStream stream = mc.getResourceManager().getResource(location).getInputStream()) {
				NVGAsset asset = uploadImage(nvg, ImageIO.read(stream));
				if (asset == null) return false;
				imageCache.put(location.getResourcePath(), asset);
			} catch (Exception e) {
				MixLogger.error("Failed to decode image: " + location, e);
				return false;
			}
			
			return true;
		}
		
		return true;
	}
	
	private String getFileCacheKey(File file) {
		return "file:" + file.getName() + ":" + file.length() + ":" + file.lastModified();
	}
	
	public boolean loadImage(long nvg, File file) {
		
		String name = file.getName();
		String key = getFileCacheKey(file);
		String cachedKey = fileCacheKeys.get(name);
		
		if(cachedKey != null && !cachedKey.equals(key)) {
			
			NVGAsset stale = imageCache.remove(cachedKey);
			
			if(stale != null) {
				NanoVG.nvgDeleteImage(nvg, stale.getImage());
			}
			
			fileCacheKeys.remove(name);
			cachedKey = null;
		}
		
		if(cachedKey == null) {
			
			try {
				NVGAsset asset = uploadImage(nvg, ImageIO.read(file));
				if (asset == null) return false;
				imageCache.put(key, asset);
			} catch (Exception e) {
				MixLogger.error("Failed to decode image: " + file, e);
				return false;
			}
			fileCacheKeys.put(name, key);
			
			return true;
		}
		
		return true;
	}
	
	private NVGAsset uploadImage(long nvg, BufferedImage image) {
		if (image == null) return null;
		int width = image.getWidth();
		int height = image.getHeight();
		ByteBuffer pixels = MemoryUtil.memAlloc(Math.multiplyExact(Math.multiplyExact(width, height), 4));
		try {
			int[] row = new int[width];
			for (int y = 0; y < height; y++) {
				image.getRGB(0, y, width, 1, row, 0, width);
				for (int argb : row) {
					pixels.put((byte) (argb >> 16)).put((byte) (argb >> 8))
						.put((byte) argb).put((byte) (argb >>> 24));
				}
			}
			pixels.flip();
			int handle = NanoVG.nvgCreateImageRGBA(nvg, width, height,
				NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS, pixels);
			return handle == 0 ? null : new NVGAsset(handle, width, height);
		} finally {
			MemoryUtil.memFree(pixels);
		}
	}

	public boolean loadSvg(long nvg, ResourceLocation location, float width, float height) {
		
		String name = location.getResourcePath() + "-" + width + "-" + height;
		
		if(!svgCache.containsKey(name)) {
			
			try {
				
				InputStream inputStream = mc.getResourceManager().getResource(location).getInputStream();
				
				if (inputStream == null) {
					return false;
				}
				
				StringBuilder resultStringBuilder = new StringBuilder();
				
				try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        resultStringBuilder.append(line);
                    }
				}
				
                CharSequence s = resultStringBuilder.toString();
                NSVGImage svg = NanoSVG.nsvgParse(s, "px", 96f);
                
                if (svg == null) {
                	return false;
                }
                
                long rasterizer = NanoSVG.nsvgCreateRasterizer();
                
                int w = (int) svg.width();
                int h = (int) svg.height();
                
                float scale = Math.max(width / w, height / h);
                w = (int) (w * scale);
                h = (int) (h * scale);

                ByteBuffer image = MemoryUtil.memAlloc(w * h * 4);
                NanoSVG.nsvgRasterize(rasterizer, svg, 0, 0, scale, image, w, h, w * 4);

                NanoSVG.nsvgDeleteRasterizer(rasterizer);
                NanoSVG.nsvgDelete(svg);

                svgCache.put(name, new NVGAsset(NanoVG.nvgCreateImageRGBA(nvg, w, h, NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS, image), w, h));
                
                return true;
                
			} catch (Exception e) {
				
				MixLogger.error("Failed to load svg", e);
				
				return false;
			}
		}
		
		return true;
	}
	
	public int getImage(ResourceLocation location) {
		return imageCache.get(location.getResourcePath()).getImage();
	}
	
	public int getImage(File file) {
		
		String key = fileCacheKeys.get(file.getName());
		
		if(key == null) {
			key = getFileCacheKey(file);
		}
		
		NVGAsset asset = imageCache.get(key);
		
		return asset == null ? 0 : asset.getImage();
	}
	
	public int getImage(int texture) {
		return glTextureCache.get(texture);
	}
	
	public void removeImage(long nvg, ResourceLocation location) {
		NanoVG.nvgDeleteImage(nvg, imageCache.get(location.getResourcePath()).getImage());
		imageCache.remove(location.getResourcePath());
	}
	
	public void removeImage(long nvg, File file) {
		
		String key = fileCacheKeys.remove(file.getName());
		
		if(key != null) {
			
			NVGAsset asset = imageCache.remove(key);
			
			if(asset != null) {
				NanoVG.nvgDeleteImage(nvg, asset.getImage());
			}
		}
	}
	
	public int getSvg(ResourceLocation location, float width, float height) {
		
		String name = location.getResourcePath() + "-" + width + "-" + height;
		
		return svgCache.get(name).getImage();
	}
	
	public void removeSvg(long nvg, String path, float width, float height) {
		
		String name = path + "-" + width + "-" + height;
		
		NanoVG.nvgDeleteImage(nvg, svgCache.get(name).getImage());
		svgCache.remove(name);
	}
}
