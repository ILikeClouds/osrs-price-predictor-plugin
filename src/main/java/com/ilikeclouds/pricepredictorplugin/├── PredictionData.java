package com.ilikeclouds.pricepredictorplugin;

import java.util.List;

/**
 * Mirrors the JSON structure written by predict_trends.py.
 * Gson populates this automatically when we parse the API response.
 */
public class PredictionData
{
    public int     item_id;
    public String  item_name;
    public String  generated_at;
    public String  stale_after;
    public String  regime_start;
    public int     regime_days;
    public String  model_tier;
    public int     current_price;
    public List<ForecastDay> forecast;
    public Metrics metrics;

    /** One row in the 7-day forecast table. */
    public static class ForecastDay
    {
        public String date;
        public int    predicted_price;
        public int    change_from_now;
    }

    /** Model quality metrics returned alongside the forecast. */
    public static class Metrics
    {
        public Double mae;
        public Double r2;
    }

    /** True if the data is older than stale_after (i.e. the nightly job hasn't run yet). */
    public boolean isStale()
    {
        if (stale_after == null) return false;
        try
        {
            java.time.Instant staleInstant = java.time.Instant.parse(stale_after);
            return java.time.Instant.now().isAfter(staleInstant);
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
