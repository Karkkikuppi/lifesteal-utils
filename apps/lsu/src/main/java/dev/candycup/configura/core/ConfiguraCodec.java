package dev.candycup.configura.core;

import java.util.Map;

public interface ConfiguraCodec {
   Map<String, Object> decode(String raw);

   String encode(Map<String, Object> values);
}
