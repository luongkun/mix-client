package mix.client.mixui.integration.haunt.fx.pass;

import mix.client.mixui.integration.haunt.fx.pass.FxPass;

public interface PassItem {
   int passes();

   void drawPass(FxPass var1);

   double passDistanceSq();

   default void passFailed() {
   }
}
