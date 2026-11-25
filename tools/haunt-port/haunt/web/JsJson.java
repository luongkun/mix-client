package mix.client.mixui.integration.haunt.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

public final class JsJson {
   public static String stringify(JsonElement e) {
      StringBuilder sb = new StringBuilder();
      write(e, sb);
      return sb.toString();
   }

   private static void write(JsonElement e, StringBuilder sb) {
      if(e != null && !e.isJsonNull()) {
         if(e.isJsonObject()) {
            sb.append('{');
            boolean first = true;

            for(Entry<String, JsonElement> m : ordered(e.getAsJsonObject())) {
               if(!first) {
                  sb.append(',');
               }

               first = false;
               string((String)m.getKey(), sb);
               sb.append(':');
               write((JsonElement)m.getValue(), sb);
            }

            sb.append('}');
         } else if(e.isJsonArray()) {
            sb.append('[');
            JsonArray a = e.getAsJsonArray();

            for(int i = 0; i < a.size(); ++i) {
               if(i > 0) {
                  sb.append(',');
               }

               write(a.get(i), sb);
            }

            sb.append(']');
         } else {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if(p.isBoolean()) {
               sb.append(p.getAsBoolean());
            } else if(p.isNumber()) {
               sb.append(number(p.getAsDouble()));
            } else {
               string(p.getAsString(), sb);
            }
         }
      } else {
         sb.append("null");
      }

   }

   private static List<Entry<String, JsonElement>> ordered(JsonObject o) {
      ArrayList<Entry<String, JsonElement>> index = new ArrayList();
      ArrayList<Entry<String, JsonElement>> rest = new ArrayList();

      for(Entry m : o.entrySet()) {
         (arrayIndex((String)m.getKey()) >= 0L?index:rest).add(m);
      }

      if(index.isEmpty()) {
         return rest;
      } else {
         Collections.sort(index, new Comparator<Entry<String, JsonElement>>() {
            public int compare(Entry<String, JsonElement> a, Entry<String, JsonElement> b) {
               return Long.compare(JsJson.arrayIndex((String)a.getKey()), JsJson.arrayIndex((String)b.getKey()));
            }
         });
         index.addAll(rest);
         return (int)index;
      }
   }

   private static long arrayIndex(String key) {
      int n = key.length();
      if(n != 0 && n <= 10 && (n <= 1 || key.charAt(0) != 48)) {
         long v = 0L;

         for(int i = 0; i < n; ++i) {
            char c = key.charAt(i);
            if(c < 48 || c > 57) {
               return -1L;
            }

            v = v * 10L + (long)(c - 48);
         }

         return v <= 4294967294L?v:-1L;
      } else {
         return -1L;
      }
   }

   private static void string(String s, StringBuilder sb) {
      sb.append('\"');

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         switch(c) {
         case '\b':
            sb.append("\\b");
            continue;
         case '\t':
            sb.append("\\t");
            continue;
         case '\n':
            sb.append("\\n");
            continue;
         case '\f':
            sb.append("\\f");
            continue;
         case '\r':
            sb.append("\\r");
            continue;
         case '\"':
            sb.append("\\\"");
            continue;
         case '\\':
            sb.append("\\\\");
            continue;
         }

         if(c < 32 || Character.isSurrogate(c) && !paired(s, i)) {
            sb.append(String.format("\\u%04x", new Object[]{Character.valueOf(c)}));
         } else {
            sb.append(c);
         }
      }

      sb.append('\"');
   }

   private static boolean paired(String s, int i) {
      char c = s.charAt(i);
      return Character.isHighSurrogate(c)?i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1)):i > 0 && Character.isHighSurrogate(s.charAt(i - 1));
   }

   public static String number(double d) {
      if(!Double.isNaN(d) && !Double.isInfinite(d)) {
         if(d == 0.0D) {
            return "0";
         } else {
            BigDecimal exact = new BigDecimal(d);
            BigDecimal b = null;

            for(int p = 1; p <= 17; ++p) {
               BigDecimal r = exact.round(new MathContext(p, RoundingMode.HALF_EVEN));
               if(r.doubleValue() == d) {
                  b = r;
                  break;
               }
            }

            if(b == null) {
               b = exact.round(MathContext.DECIMAL64);
            }

            b = b.stripTrailingZeros();
            String digits = b.unscaledValue().abs().toString();
            int k = digits.length();
            int n = k - b.scale();
            StringBuilder sb = new StringBuilder();
            if(d < 0.0D) {
               sb.append('-');
            }

            if(k <= n && n <= 21) {
               sb.append(digits);

               for(int i = 0; i < n - k; ++i) {
                  sb.append('0');
               }
            } else if(0 < n && n <= 21) {
               sb.append(digits, 0, n).append('.').append(digits, n, k);
            } else if(-6 < n && n <= 0) {
               sb.append("0.");

               for(int i = 0; i < -n; ++i) {
                  sb.append('0');
               }

               sb.append(digits);
            } else {
               sb.append(digits.charAt(0));
               if(k > 1) {
                  sb.append('.').append(digits, 1, k);
               }

               sb.append('e').append(n - 1 >= 0?"+":"-").append(Math.abs(n - 1));
            }

            return sb.toString();
         }
      } else {
         return "null";
      }
   }
}
