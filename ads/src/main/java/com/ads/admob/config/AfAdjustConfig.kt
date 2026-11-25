package com.ads.admob.config

class AfAdjustConfig private constructor(
    val adjustToken: String,
    val environmentProduct: Boolean,
    val adRevenueKey: String,
    val adRevenueMintegralKey: String,
    /**
     * Callback được gọi khi Adjust trả về attribution data.
     * Parameters: (network, campaign, costType) - đều có thể null nếu Organic.
     * - network == null hoặc "Organic" -> Organic install
     * - network != null && != "Organic" -> Paid install
     */
    val onAttributionChanged: ((network: String?, campaign: String?, costType: String?) -> Unit)?
) {
    class Build(
        private var adjustToken: String,
        private var environmentProduct: Boolean = true,
        private var adRevenueKey: String = "",
        private var adRevenueMintegralKey: String = "",
        private var onAttributionChanged: ((network: String?, campaign: String?, costType: String?) -> Unit)? = null
    ) {
        fun adRevenueKey(key: String) = apply {
            adRevenueKey = key
        }
        fun environmentProduct(product: Boolean) = apply { environmentProduct = product }
        fun onAttributionChanged(listener: ((network: String?, campaign: String?, costType: String?) -> Unit)?) = apply {
            onAttributionChanged = listener
        }
        fun build() = AfAdjustConfig(adjustToken, environmentProduct, adRevenueKey, adRevenueMintegralKey, onAttributionChanged)
    }
}
