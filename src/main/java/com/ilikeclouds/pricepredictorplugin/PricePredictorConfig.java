package com.ilikeclouds.pricepredictorplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pricepredictorplugin")
public interface PricePredictorConfig extends Config
{
    @ConfigItem(
        keyName     = "apiBaseUrl",
        name        = "API Base URL",
        description = "GitHub Pages URL where prediction JSON files are hosted. "
                    + "Change this if you self-host the pipeline.",
        position    = 0
    )
    default String apiBaseUrl()
    {
        return "https://ilikeclouds.github.io/osrs-price-predictor";
    }

    @ConfigItem(
        keyName     = "showMetrics",
        name        = "Show model metrics",
        description = "Display MAE and R² accuracy metrics below the forecast.",
        position    = 1
    )
    default boolean showMetrics()
    {
        return true;
    }

    @ConfigItem(
        keyName     = "showSparkline",
        name        = "Show sparkline chart",
        description = "Display a small price trend chart above the forecast table.",
        position    = 2
    )
    default boolean showSparkline()
    {
        return true;
    }
}
