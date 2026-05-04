package com.ilikeclouds.pricepredictorplugin;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Sidebar panel for the OSRS Price Predictor plugin.
 *
 * Layout (top to bottom):
 *   ┌─────────────────────────────┐
 *   │  Search box + button        │
 *   ├─────────────────────────────┤
 *   │  Item name + current price  │
 *   │  Model tier badge           │
 *   │  Metrics (MAE / R²)         │
 *   ├─────────────────────────────┤
 *   │  Sparkline chart            │
 *   ├─────────────────────────────┤
 *   │  7-day forecast table       │
 *   ├─────────────────────────────┤
 *   │  Stale / generated-at note  │
 *   └─────────────────────────────┘
 */
public class PricePredictorPanel extends PluginPanel
{
    private static final NumberFormat GP_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private static final Color COLOR_GAIN         = new Color(0, 200, 83);
    private static final Color COLOR_LOSS         = new Color(220, 50, 50);
    private static final Color COLOR_FLAT         = new Color(150, 150, 150);
    private static final Color COLOR_BADGE_GLOBAL = new Color(200, 120, 0);
    private static final Color COLOR_BADGE_ITEM   = new Color(0, 140, 200);

    private final PricePredictorPlugin plugin;
    private final PricePredictorConfig config;

    // Search
    private JTextField searchField;
    private JButton    searchButton;

    // Header
    private JLabel itemNameLabel;
    private JLabel currentPriceLabel;
    private JLabel modelTierLabel;
    private JLabel metricsLabel;
    private JLabel statusLabel;

    // Sparkline
    private SparklinePanel sparkline;

    // Forecast table
    private DefaultTableModel tableModel;
    private JTable            forecastTable;

    // Footer
    private JLabel generatedLabel;

    public PricePredictorPanel(PricePredictorPlugin plugin, PricePredictorConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setLayout(new BorderLayout(0, 6));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildSearchPanel(),  BorderLayout.NORTH);
        add(buildContentPanel(), BorderLayout.CENTER);
        add(buildFooterPanel(),  BorderLayout.SOUTH);

        showPlaceholder();
    }


    // ── Search panel ──────────────────────────────────────────────────────────

    private JPanel buildSearchPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 6, 0));

        searchField = new JTextField();
        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(4, 6, 4, 6)
        ));
        searchField.setToolTipText("Enter an item name, e.g. \"Dragon bones\"");
        searchField.addActionListener(e -> doSearch());  // Enter key triggers search

        searchButton = new JButton("Search");
        searchButton.setBackground(ColorScheme.BRAND_ORANGE);
        searchButton.setForeground(Color.WHITE);
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> doSearch());

        panel.add(searchField,  BorderLayout.CENTER);
        panel.add(searchButton, BorderLayout.EAST);
        return panel;
    }

    private void doSearch()
    {
        String query = searchField.getText().trim();
        if (!query.isEmpty())
        {
            plugin.searchByName(query);
        }
    }


    // ── Content panel ─────────────────────────────────────────────────────────

    private JPanel buildContentPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        itemNameLabel = new JLabel("—");
        itemNameLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(14f));
        itemNameLabel.setForeground(Color.WHITE);
        itemNameLabel.setAlignmentX(LEFT_ALIGNMENT);

        currentPriceLabel = new JLabel(" ");
        currentPriceLabel.setFont(FontManager.getRunescapeFont());
        currentPriceLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        currentPriceLabel.setAlignmentX(LEFT_ALIGNMENT);

        modelTierLabel = new JLabel(" ");
        modelTierLabel.setFont(FontManager.getRunescapeSmallFont());
        modelTierLabel.setOpaque(true);
        modelTierLabel.setBorder(new EmptyBorder(2, 6, 2, 6));
        modelTierLabel.setAlignmentX(LEFT_ALIGNMENT);

        metricsLabel = new JLabel(" ");
        metricsLabel.setFont(FontManager.getRunescapeSmallFont());
        metricsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        metricsLabel.setAlignmentX(LEFT_ALIGNMENT);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FontManager.getRunescapeFont());
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        sparkline = new SparklinePanel();
        sparkline.setAlignmentX(LEFT_ALIGNMENT);
        sparkline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        String[] columns = {"Date", "Price (GP)", "Change"};
        tableModel = new DefaultTableModel(columns, 0)
        {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        forecastTable = new JTable(tableModel);
        forecastTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        forecastTable.setForeground(Color.WHITE);
        forecastTable.setGridColor(ColorScheme.MEDIUM_GRAY_COLOR);
        forecastTable.setRowHeight(22);
        forecastTable.setFont(FontManager.getRunescapeSmallFont());
        forecastTable.getTableHeader().setBackground(ColorScheme.DARK_GRAY_COLOR);
        forecastTable.getTableHeader().setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        forecastTable.getTableHeader().setFont(FontManager.getRunescapeSmallFont());
        forecastTable.setDefaultRenderer(Object.class, new ChangeColorRenderer());

        JScrollPane tableScroll = new JScrollPane(forecastTable);
        tableScroll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tableScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        tableScroll.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(itemNameLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(currentPriceLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(modelTierLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(metricsLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sparkline);
        panel.add(Box.createVerticalStrut(6));
        panel.add(tableScroll);

        return panel;
    }


    // ── Footer ────────────────────────────────────────────────────────────────

    private JPanel buildFooterPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(6, 0, 0, 0));

        generatedLabel = new JLabel(" ");
        generatedLabel.setFont(FontManager.getRunescapeSmallFont());
        generatedLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        panel.add(generatedLabel, BorderLayout.WEST);

        return panel;
    }


    // ── Public update methods (called from plugin on EDT) ─────────────────────

    public void showLoading()
    {
        SwingUtilities.invokeLater(() ->
        {
            statusLabel.setText("Loading...");
            statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            itemNameLabel.setText("—");
            currentPriceLabel.setText(" ");
            modelTierLabel.setText(" ");
            metricsLabel.setText(" ");
            generatedLabel.setText(" ");
            tableModel.setRowCount(0);
            sparkline.clear();
        });
    }

    public void showError(String message)
    {
        SwingUtilities.invokeLater(() ->
            statusLabel.setText("<html><font color='#DC3232'>" +
                message.replace("\n", "<br>") + "</font></html>")
        );
    }

    public void showPlaceholder()
    {
        SwingUtilities.invokeLater(() ->
        {
            statusLabel.setText("Search for an item above.");
            statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        });
    }

    public void onDataLoaded(PredictionData data)
    {
        SwingUtilities.invokeLater(() ->
        {
            statusLabel.setText(" ");

            itemNameLabel.setText(data.item_name);
            currentPriceLabel.setText("Current: " + formatGP(data.current_price) + " GP");

            boolean isGlobal = "Global".equalsIgnoreCase(data.model_tier);
            modelTierLabel.setText(isGlobal ? "⚠ Global model (cold start)" : "✓ Item-specific model");
            modelTierLabel.setBackground(isGlobal ? COLOR_BADGE_GLOBAL : COLOR_BADGE_ITEM);
            modelTierLabel.setForeground(Color.WHITE);
            modelTierLabel.setToolTipText(isGlobal
                ? "Only " + data.regime_days + " days of data. Accuracy improves over time."
                : data.regime_days + " days of regime data. Full accuracy.");

            if (config.showMetrics() && data.metrics != null)
            {
                String mae = data.metrics.mae != null
                    ? String.format("%.0f GP", data.metrics.mae) : "N/A";
                String r2 = data.metrics.r2 != null
                    ? String.format("%.4f", data.metrics.r2) : "N/A";
                metricsLabel.setText("MAE: " + mae + "  |  R²: " + r2);
            }

            if (config.showSparkline() && data.forecast != null)
            {
                int[] prices = new int[data.forecast.size() + 1];
                prices[0] = data.current_price;
                for (int i = 0; i < data.forecast.size(); i++)
                {
                    prices[i + 1] = data.forecast.get(i).predicted_price;
                }
                sparkline.setData(prices);
            }

            tableModel.setRowCount(0);
            if (data.forecast != null)
            {
                for (PredictionData.ForecastDay day : data.forecast)
                {
                    String changeStr = (day.change_from_now >= 0 ? "+" : "")
                        + formatGP(day.change_from_now);
                    tableModel.addRow(new Object[]{
                        day.date,
                        formatGP(day.predicted_price),
                        changeStr + " GP"
                    });
                }
            }

            String genAt = data.generated_at != null
                ? "Updated: " + data.generated_at.substring(0, 10) : "";
            String staleWarning = data.isStale() ? "  ⚠ Stale" : "";
            generatedLabel.setText(genAt + staleWarning);
            generatedLabel.setForeground(data.isStale() ? COLOR_LOSS : ColorScheme.LIGHT_GRAY_COLOR);
        });
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String formatGP(int value)
    {
        return GP_FORMAT.format(value);
    }


    // ── Sparkline inner panel ─────────────────────────────────────────────────

    private static class SparklinePanel extends JPanel
    {
        private int[] prices = new int[0];

        SparklinePanel()
        {
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
            setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
            setPreferredSize(new Dimension(0, 75));
        }

        void setData(int[] prices)
        {
            this.prices = prices;
            repaint();
        }

        void clear()
        {
            this.prices = new int[0];
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (prices == null || prices.length < 2) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = 6;
            int w = getWidth()  - pad * 2;
            int h = getHeight() - pad * 2;

            int min = prices[0], max = prices[0];
            for (int p : prices) { min = Math.min(min, p); max = Math.max(max, p); }
            int range = Math.max(max - min, 1);

            int n = prices.length;
            int[] xs = new int[n];
            int[] ys = new int[n];
            for (int i = 0; i < n; i++)
            {
                xs[i] = pad + (i * w / (n - 1));
                ys[i] = pad + h - (int) ((prices[i] - min) * (double) h / range);
            }

            // Filled area under the line
            int[] polyX = new int[n + 2];
            int[] polyY = new int[n + 2];
            System.arraycopy(xs, 0, polyX, 0, n);
            System.arraycopy(ys, 0, polyY, 0, n);
            polyX[n] = xs[n - 1]; polyY[n] = pad + h;
            polyX[n + 1] = xs[0]; polyY[n + 1] = pad + h;
            g2.setColor(new Color(0, 100, 200, 40));
            g2.fillPolygon(polyX, polyY, n + 2);

            boolean trendingUp = prices[n - 1] >= prices[0];
            g2.setColor(trendingUp ? COLOR_GAIN : COLOR_LOSS);
            g2.setStroke(new BasicStroke(1.8f));
            for (int i = 0; i < n - 1; i++)
            {
                g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
            }

            for (int i = 0; i < n; i++)
            {
                g2.setColor(i == 0 ? Color.WHITE : (trendingUp ? COLOR_GAIN : COLOR_LOSS));
                g2.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
            }

            g2.dispose();
        }
    }


    // ── Cell renderer that colours the Change column ──────────────────────────

    private static class ChangeColorRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int col)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setBackground(row % 2 == 0
                ? ColorScheme.DARKER_GRAY_COLOR
                : ColorScheme.DARK_GRAY_COLOR);

            if (col == 2 && value != null)
            {
                String text = value.toString();
                if      (text.startsWith("+")) setForeground(COLOR_GAIN);
                else if (text.startsWith("-")) setForeground(COLOR_LOSS);
                else                           setForeground(COLOR_FLAT);
            }
            else
            {
                setForeground(Color.WHITE);
            }
            return this;
        }
    }
}
