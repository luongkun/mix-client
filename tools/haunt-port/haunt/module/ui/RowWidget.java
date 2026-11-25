package mix.client.mixui.integration.haunt.module.ui;

import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.setting.Setting;

public abstract class RowWidget extends Widget {
   public Setting row;

   public boolean isVisible() {
      return this.row == null || this.row.isVisible();
   }
}
