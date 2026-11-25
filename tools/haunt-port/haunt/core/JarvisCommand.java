package mix.client.mixui.integration.haunt.core;

import java.util.Arrays;
import java.util.List;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.haunt.Jarvis;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

final class JarvisCommand extends CommandBase {
   public String getCommandName() {
      return "jarvis";
   }

   public String getCommandUsage(ICommandSender sender) {
      return "/jarvis <pedido>";
   }

   public List<String> getCommandAliases() {
      return Arrays.asList(new String[]{"haunt-jarvis"});
   }

   public boolean canCommandSenderUseCommand(ICommandSender sender) {
      return true;
   }

   public void processCommand(ICommandSender sender, String[] args) throws CommandException {
      if(FuriahsVisuals.instance != null && FuriahsVisuals.instance.isRunning()) {
         Jarvis.handle(String.join(" ", args));
      }

   }
}
