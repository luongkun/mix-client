package mix.client.mixui.management.account.skin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.util.UUIDTypeAdapter;

import mix.client.mixui.utils.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class SkinDownloader {

	private Minecraft mc = Minecraft.getMinecraft();
	private Gson gson = new Gson();

    private void loadSkin(File file, String username, UUID uuid) {

    	File faceFile = new File(file, username + ".png");

        boolean slimSkin = false;
        BufferedImage img;

        try {
            if (uuid == null) {
                try (final InputStreamReader isr = new InputStreamReader(new URL("https://api.mojang.com/users/profiles/minecraft/" + username).openStream(), StandardCharsets.UTF_8)) {
                    uuid = UUIDTypeAdapter.fromString(((JsonObject) gson.fromJson((Reader)isr, (Class<?>)JsonObject.class)).get("id").getAsString());
                }
            }
            String base64Data;
            try (final InputStreamReader isr2 = new InputStreamReader(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDTypeAdapter.fromUUID(uuid)).openStream(), StandardCharsets.UTF_8)) {
                base64Data = ((JsonObject) gson.fromJson((Reader)isr2, (Class<?>)JsonObject.class)).getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
            }
            final JsonObject jo = ((JsonObject) gson.fromJson(new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8), (Class<?>)JsonObject.class)).getAsJsonObject("textures").getAsJsonObject("SKIN");
            if (jo.has("metadata") && jo.getAsJsonObject("metadata").has("model") && jo.getAsJsonObject("metadata").get("model").getAsString().equalsIgnoreCase("slim")) {
                slimSkin = true;
            }
            final String skinUrl = jo.get("url").getAsString();
            img = ImageIO.read(new URL(skinUrl));
        }
        catch (Throwable t) {
            try (final InputStream is = mc.getResourceManager().getResource(new ResourceLocation("textures/entity/steve.png")).getInputStream()) {
                img = ImageIO.read(is);
            }
            catch (Throwable th) {
                return;
            }
        }
        try {
            final BufferedImage model = new BufferedImage(16, 32, 2);

            /* A skin is 64 wide and either 64 tall (modern) or 32 tall (legacy).
   The old test read "width != 64 && (height != 64 || height != 32)"; the
   bracket was always true, so only the width was ever checked and an HD skin
   (64x128) slipped through and got cropped wrong. */
            if (img.getWidth() != 64 || (img.getHeight() != 64 && img.getHeight() != 32)) {
                throw new IllegalStateException();
            }

            final boolean oldSkin = img.getHeight() == 32;

            if (oldSkin) {
                slimSkin = false;
            }

            model.getGraphics().drawImage(img.getSubimage(8, 8, 8, 8), 4, 0, null);
            model.getGraphics().drawImage(img.getSubimage(20, 20, 8, 12), 4, 8, null);
            model.getGraphics().drawImage(img.getSubimage(44, 20, slimSkin ? 3 : 4, 12), slimSkin ? 1 : 0, 8, null);
            model.getGraphics().drawImage(img.getSubimage(oldSkin ? 44 : 36, oldSkin ? 20 : 52, slimSkin ? 3 : 4, 12), 12, 8, null);
            model.getGraphics().drawImage(img.getSubimage(4, 20, 4, 12), 4, 20, null);
            model.getGraphics().drawImage(img.getSubimage(oldSkin ? 4 : 20, oldSkin ? 20 : 52, 4, 12), 8, 20, null);
            model.getGraphics().drawImage(img.getSubimage(40, 8, 8, 8), 4, 0, null);

            if (!oldSkin) {
                model.getGraphics().drawImage(img.getSubimage(20, 36, 8, 12), 4, 8, null);
                model.getGraphics().drawImage(img.getSubimage(44, 36, 4, 12), 0, 8, null);
                model.getGraphics().drawImage(img.getSubimage(52, 52, 4, 12), 12, 8, null);
                model.getGraphics().drawImage(img.getSubimage(4, 36, 4, 12), 4, 20, null);
                model.getGraphics().drawImage(img.getSubimage(4, 52, 4, 12), 8, 20, null);
            }

            // The face cell is cropped straight from the in-memory model. The 16x32
            // body sheet must never be written to <name>.png: the UI caches the
            // texture by file name, so a body sheet that is later overwritten by the
            // face would still render as a whole skin until the next restart.
            writeFace(faceFile, ImageUtils.resize(model.getSubimage(4, 0, 8, 8), 128, 128));
        }
        catch (Throwable t) {
        	t.printStackTrace();
        }
	}

    private void writeFace(File faceFile, BufferedImage image) {

        File parent = faceFile.getParentFile();

        if(parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }

        File tmpFile = new File(parent, faceFile.getName() + ".tmp-" + System.currentTimeMillis());

        try {

            if(!ImageIO.write(image, "png", tmpFile)) {
                return;
            }

            if(faceFile.exists() && !faceFile.delete()) {
                return;
            }

            if(!tmpFile.renameTo(faceFile)) {
                java.nio.file.Files.copy(tmpFile.toPath(), faceFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        finally {

            if(tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    /**
     * A usable head texture is at least as wide as it is tall (a 128x128 face, an 8x8
     * face or an old 16x32 body sheet that must be rejected: height > width).
     */
    public static boolean isFaceShaped(File file) {

        if(file == null || !file.isFile() || file.length() == 0) {
            return false;
        }

        try {

            BufferedImage image = ImageIO.read(file);

            return image != null && image.getWidth() > 0 && image.getWidth() >= image.getHeight();
        }
        catch (Throwable t) {
            return false;
        }
    }

    public void downloadFace(File file, String username, UUID uuid) {

    	File faceFile = new File(file, username + ".png");

    	if(!faceFile.exists() || !isFaceShaped(faceFile)) {
        	loadSkin(file, username, uuid);
    	}
    } 
}