package dev.candycup.lifestealutils.features.shop;

public enum ShopStabilityCode {
   SHOP_HOME_IMPOSTORS_1("shop_home_impostors_1"),
   LISTING_META_QUESTIONABLE("listing_meta_questionable"),
   NO_LISTINGS_FOUND("no_listings_found"),
   NO_BUY_AMOUNTS("no_buy_amounts"),
   SHOP_TITLE_MISMATCH("shop_title_mismatch"),
   SHOP_DUPLICATE_NAV("shop_duplicate_nav"),
   SHOP_MISSING_BACK_BUTTON("shop_missing_back_button"),
   SHOP_PRICE_PARSE_FAILED("shop_price_parse_failed");

   private final String code;

   ShopStabilityCode(String code) {
      this.code = code;
   }

   public String code() {
      return code;
   }
}
