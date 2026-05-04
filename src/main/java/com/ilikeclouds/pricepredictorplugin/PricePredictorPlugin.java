package com.ilikeclouds.pricepredictorplugin;

import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@PluginDescriptor(
    name        = "OSRS Price Predictor",
    description = "7-day Grand Exchange price forecasts powered by machine learning.",
    tags        = {"grand exchange", "price", "forecast", "market", "flipping"}
)
public class PricePredictorPlugin extends Plugin
{
    @Inject private ClientToolbar        clientToolbar;
    @Inject private PricePredictorConfig config;
    @Inject private OkHttpClient         okHttpClient;
    @Inject private Gson                 gson;

    private PricePredictorPanel panel;
    private NavigationButton    navButton;

    // Single-thread executor so API calls don't block the game thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void startUp()
    {
        panel = new PricePredictorPanel(this, config);

        // Load the sidebar icon (place icon.png in src/main/resources/)
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");

        navButton = NavigationButton.builder()
            .tooltip("OSRS Price Predictor")
            .icon(icon)
            .priority(6)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
        log.info("OSRS Price Predictor started.");
    }

    @Override
    protected void shutDown()
    {
        clientToolbar.removeNavigation(navButton);
        executor.shutdownNow();
        log.info("OSRS Price Predictor stopped.");
    }

    /**
     * Fetches the prediction JSON for the given item ID from GitHub Pages.
     * Runs on a background thread — calls panel.onDataLoaded() when done.
     */
    public void fetchPrediction(int itemId)
    {
        panel.showLoading();
        executor.submit(() ->
        {
            String url = config.apiBaseUrl() + "/predictions/" + itemId + ".json";
            log.debug("Fetching: {}", url);

            Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "OSRS-Price-Predictor-Plugin")
                .build();

            try (Response response = okHttpClient.newCall(request).execute())
            {
                if (!response.isSuccessful() || response.body() == null)
                {
                    panel.showError("Item not found or API unavailable.\n"
                        + "Status: " + response.code());
                    return;
                }

                String json = response.body().string();
                PredictionData data = gson.fromJson(json, PredictionData.class);
                panel.onDataLoaded(data);
            }
            catch (IOException e)
            {
                log.warn("Failed to fetch prediction for item {}: {}", itemId, e.getMessage());
                panel.showError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches index.json to resolve an item name to its ID,
     * then calls fetchPrediction() with the resolved ID.
     */
    public void searchByName(String itemName)
    {
        panel.showLoading();
        executor.submit(() ->
        {
            String url = config.apiBaseUrl() + "/index.json";
            Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "OSRS-Price-Predictor-Plugin")
                .build();

            try (Response response = okHttpClient.newCall(request).execute())
            {
                if (!response.isSuccessful() || response.body() == null)
                {
                    panel.showError("Could not load item index.\nStatus: " + response.code());
                    return;
                }

                IndexData index = gson.fromJson(response.body().string(), IndexData.class);
                String query = itemName.trim().toLowerCase();

                // Exact match first, then partial
                IndexData.IndexItem match = null;
                for (IndexData.IndexItem item : index.items)
                {
                    if (item.name.toLowerCase().equals(query))
                    {
                        match = item;
                        break;
                    }
                }
                if (match == null)
                {
                    for (IndexData.IndexItem item : index.items)
                    {
                        if (item.name.toLowerCase().contains(query))
                        {
                            match = item;
                            break;
                        }
                    }
                }

                if (match == null)
                {
                    panel.showError("'" + itemName + "' is not tracked yet.\n"
                        + "Tracked items: " + index.item_count);
                    return;
                }

                fetchPrediction(match.id);
            }
            catch (IOException e)
            {
                panel.showError("Network error: " + e.getMessage());
            }
        });
    }

    @Provides
    PricePredictorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PricePredictorConfig.class);
    }
}
