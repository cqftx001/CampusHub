package com.campushub.marketplace.error;

import com.campushub.shared.error.BaseException;

public class MarketplaceException extends BaseException {

   public MarketplaceException(MarketplaceErrorCode errorCode) {
       super(errorCode);
   }

   public MarketplaceException(MarketplaceErrorCode errorCode, String customMessage) {
       super(errorCode, customMessage);
   }
}
