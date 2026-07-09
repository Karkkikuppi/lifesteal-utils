package dev.candycup.lifestealutils.features.ah;

public enum AhStabilityCode {
   AH_TITLE_UNSUPPORTED("ah_title_unsupported"),
   AH_NO_LISTINGS_FOUND("ah_no_listings_found"),
   AH_LISTING_META_MISSING("ah_listing_meta_missing"),
   AH_DUPLICATE_NAV("ah_duplicate_nav"),
   AH_FILTER_OPTIONS_MISSING("ah_filter_options_missing"),
   AH_SEARCH_CONTROL_MISSING("ah_search_control_missing");

   private final String code;

   AhStabilityCode(String code) {
      this.code = code;
   }

   public String code() {
      return code;
   }
}
