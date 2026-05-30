package artgallery;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.UUID;

public class ArtGallery extends JFrame {

    // Database Credentials
    private static final String DB_URL_BASE = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "art_gallery_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1122"; 

    // Light Theme Color Palette with Premium Soft Tones
    private static final Color BG_WHITE = new Color(252, 252, 252);
    private static final Color PATTERN_GREY = new Color(242, 242, 244);
    private static final Color BOX_GREY = new Color(245, 245, 247); 
    private static final Color PANEL_WHITE = new Color(255, 255, 255);
    private static final Color TEXT_DARK = new Color(33, 37, 41);
    private static final Color TEXT_MUTED = new Color(108, 117, 125);
    private static final Color BORDER_LIGHT = new Color(222, 226, 230);
    
    private static final Color BTN_GREEN = new Color(40, 167, 69);
    private static final Color BTN_PINK = new Color(255, 105, 180);
    private static final Color BTN_BLUE = new Color(0, 123, 255);

    // Layout and Navigation
    private CardLayout cardLayout;
    private JPanel mainCardPanel;
    private JPanel homeGridPanel;
    
    private RoundedTextField txtNavbarSearch;
    private String storageFolderPath;

    public ArtGallery() {
        initializeStorageAndDatabase();
        setupMainFrame();
        showHomeView();
    }

    private void initializeStorageAndDatabase() {
        String homePath = System.getProperty("user.home");
        File storageDir = new File(homePath, "Documents" + File.separator + "art gallery images");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        storageFolderPath = storageDir.getAbsolutePath();

        try {
            Connection connBase = DriverManager.getConnection(DB_URL_BASE, DB_USER, DB_PASSWORD);
            Statement stmt = connBase.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            stmt.close();
            connBase.close();

            Connection connDB = getConnection();
            Statement stmtTable = connDB.createStatement();
            String createTableSQL = "CREATE TABLE IF NOT EXISTS art_pieces ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "title VARCHAR(255) NOT NULL, "
                    + "description TEXT, "
                    + "file_path VARCHAR(500) NOT NULL, "
                    + "is_favorite BOOLEAN DEFAULT FALSE)";
            stmtTable.executeUpdate(createTableSQL);
            stmtTable.close();
            connDB.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Connection Error.\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL_BASE + DB_NAME, DB_USER, DB_PASSWORD);
    }

    private void setupMainFrame() {
        setTitle("Art Gallery Application");
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_WHITE);

        // Fixed Header Architecture
        JPanel fixedHeaderPanel = new JPanel();
        fixedHeaderPanel.setLayout(new BoxLayout(fixedHeaderPanel, BoxLayout.Y_AXIS));
        fixedHeaderPanel.setBackground(BG_WHITE);

        // Persistent "Art Gallery" Branding Title
        JLabel lblMainTitle = new JLabel("Art Gallery");
        lblMainTitle.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, 42));
        lblMainTitle.setForeground(TEXT_DARK);
        lblMainTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblMainTitle.setBorder(new EmptyBorder(20, 0, 10, 0));
        fixedHeaderPanel.add(lblMainTitle);

        // Modern Restructured Navigation Bar
        JPanel navBar = new JPanel(new BorderLayout(20, 0));
        navBar.setBackground(PANEL_WHITE);
        navBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, BORDER_LIGHT),
                new EmptyBorder(12, 40, 12, 40)
        ));

        // Right Segment: Search grouped with Context Action Buttons
        JPanel actionSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        actionSection.setOpaque(false);
        
        txtNavbarSearch = new RoundedTextField(20, 12);
        txtNavbarSearch.setToolTipText("Search art titles...");
        RoundedButton btnSearch = new RoundedButton("Search", BTN_BLUE, 12);
        RoundedButton btnAddArt = new RoundedButton("Add Art", BTN_GREEN, 12);
        RoundedButton btnFavArt = new RoundedButton("Favorites", BTN_PINK, 12);

        actionSection.add(txtNavbarSearch);
        actionSection.add(btnSearch);
        actionSection.add(btnAddArt);
        actionSection.add(btnFavArt);

        // Add completely to the right side of the nav
        navBar.add(actionSection, BorderLayout.EAST);
        fixedHeaderPanel.add(navBar);

        add(fixedHeaderPanel, BorderLayout.NORTH);

        // Functional Action Hooks
        btnSearch.addActionListener(e -> executeSearchQuery());
        txtNavbarSearch.addActionListener(e -> executeSearchQuery());
        btnAddArt.addActionListener(e -> showAddArtView());
        btnFavArt.addActionListener(e -> showFavoritesView());

        // Card Engine Content Frame
        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        mainCardPanel.setOpaque(false);

        // Home View Setup 
        homeGridPanel = new ResponsiveGridPanel(5, 30, 35);
        homeGridPanel.setBorder(new EmptyBorder(30, 45, 30, 45));
        homeGridPanel.setOpaque(false);
        
        PatternScrollPane homeScrollPane = new PatternScrollPane(homeGridPanel);
        mainCardPanel.add(homeScrollPane, "HomeView");
        
        add(mainCardPanel, BorderLayout.CENTER);
    }

    private void showHomeView() {
        txtNavbarSearch.setText("");
        loadGridData(homeGridPanel, null, false);
        cardLayout.show(mainCardPanel, "HomeView");
    }

    private void executeSearchQuery() {
        String query = txtNavbarSearch.getText().trim();
        if (!query.isEmpty()) {
            showSearchResultsView(query);
        } else {
            showHomeView();
        }
    }

    // Dedicated Search Results View with right-aligned Back button
    private void showSearchResultsView(String query) {
        JPanel searchContainer = new JPanel(new BorderLayout());
        searchContainer.setOpaque(false);

        // Header Panel using BorderLayout to separate Title (Left) and Button (Right)
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(15, 45, 0, 45));

        RoundedButton btnBack = new RoundedButton("Back →", TEXT_DARK, 12);
        btnBack.addActionListener(e -> showHomeView());
        
        JLabel lblTitle = new JLabel("Search Results for: \"" + query + "\"");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(TEXT_DARK);

        headerPanel.add(lblTitle, BorderLayout.WEST); // Title on Left
        headerPanel.add(btnBack, BorderLayout.EAST);  // Button on Right

        searchContainer.add(headerPanel, BorderLayout.NORTH);

        JPanel searchGrid = new ResponsiveGridPanel(5, 30, 35);
        searchGrid.setBorder(new EmptyBorder(20, 45, 30, 45));
        searchGrid.setOpaque(false);
        
        loadGridData(searchGrid, query, false);
        
        PatternScrollPane scrollPane = new PatternScrollPane(searchGrid);
        searchContainer.add(scrollPane, BorderLayout.CENTER);

        mainCardPanel.add(searchContainer, "SearchView");
        cardLayout.show(mainCardPanel, "SearchView");
    }

    // Favorites View with right-aligned Back button
    private void showFavoritesView() {
        JPanel favContainer = new JPanel(new BorderLayout());
        favContainer.setOpaque(false);

        // Header Panel using BorderLayout to separate Title (Left) and Button (Right)
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(15, 45, 0, 45));

        RoundedButton btnBack = new RoundedButton("Back →", TEXT_DARK, 12);
        btnBack.addActionListener(e -> showHomeView());
        
        JLabel lblTitle = new JLabel("Your Favorite Pieces");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(TEXT_DARK);

        headerPanel.add(lblTitle, BorderLayout.WEST); // Title on Left
        headerPanel.add(btnBack, BorderLayout.EAST);  // Button on Right

        favContainer.add(headerPanel, BorderLayout.NORTH);

        JPanel favGrid = new ResponsiveGridPanel(5, 30, 35);
        favGrid.setBorder(new EmptyBorder(20, 45, 30, 45));
        favGrid.setOpaque(false);
        
        loadGridData(favGrid, null, true);
        
        PatternScrollPane scrollPane = new PatternScrollPane(favGrid);
        favContainer.add(scrollPane, BorderLayout.CENTER);

        mainCardPanel.add(favContainer, "FavoritesView");
        cardLayout.show(mainCardPanel, "FavoritesView");
    }

    // Add Art View with right-aligned Back button
    private void showAddArtView() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setOpaque(false);

        // Header Panel using BorderLayout to separate Title (Left) and Button (Right)
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(20, 45, 0, 45));

        RoundedButton btnBack = new RoundedButton("Back →", TEXT_DARK, 12);
        btnBack.addActionListener(e -> showHomeView());

        JLabel lblViewTitle = new JLabel("Publish New Masterpiece");
        lblViewTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblViewTitle.setForeground(TEXT_DARK);

        headerPanel.add(lblViewTitle, BorderLayout.WEST); // Title on Left
        headerPanel.add(btnBack, BorderLayout.EAST);      // Button on Right
        
        outerPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel formCard = new RoundedPanel(20, PANEL_WHITE);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1, true));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 20, 15, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        RoundedPanel imgUploadBox = new RoundedPanel(16, BOX_GREY);
        imgUploadBox.setLayout(new BorderLayout());
        imgUploadBox.setPreferredSize(new Dimension(340, 240));
        imgUploadBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        imgUploadBox.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1, true));

        JLabel lblPreview = new JLabel("Click to select artwork image", SwingConstants.CENTER);
        lblPreview.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblPreview.setForeground(TEXT_MUTED);
        imgUploadBox.add(lblPreview, BorderLayout.CENTER);

        RoundedTextField txtTitle = new RoundedTextField(25, 12);
        RoundedTextArea txtDesc = new RoundedTextArea(4, 25, 12);
        
        JScrollPane descScroll = new JScrollPane(txtDesc);
        descScroll.setBorder(null);
        descScroll.setOpaque(false);
        descScroll.getViewport().setOpaque(false);

        RoundedButton btnSave = new RoundedButton("Add to your Art gallery", BTN_GREEN, 15);

        final File[] selectedFile = {null};

        imgUploadBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Images (JPG, PNG)", "jpg", "png", "jpeg"));
                if (chooser.showOpenDialog(ArtGallery.this) == JFileChooser.APPROVE_OPTION) {
                    selectedFile[0] = chooser.getSelectedFile();
                    lblPreview.setText("");
                    lblPreview.setIcon(scaleImageKeepAspect(selectedFile[0].getAbsolutePath(), 320, 220, true));
                }
            }
        });

        btnSave.addActionListener(e -> {
            if (selectedFile[0] == null || txtTitle.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select an image and enter an artwork title.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            saveArt(selectedFile[0], txtTitle.getText().trim(), txtDesc.getText().trim());
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        formCard.add(imgUploadBox, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; formCard.add(new JLabel("Title") {{ setForeground(TEXT_DARK); setFont(new Font("Segoe UI", Font.BOLD, 14)); }}, gbc);
        gbc.gridx = 1; formCard.add(txtTitle, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; formCard.add(new JLabel("Description") {{ setForeground(TEXT_DARK); setFont(new Font("Segoe UI", Font.BOLD, 14)); }}, gbc);
        gbc.gridx = 1; formCard.add(descScroll, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.insets = new Insets(30, 20, 15, 20);
        formCard.add(btnSave, gbc);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(formCard);
        
        outerPanel.add(centerWrapper, BorderLayout.CENTER);

        mainCardPanel.add(outerPanel, "AddView");
        cardLayout.show(mainCardPanel, "AddView");
    }

    private void loadGridData(JPanel targetPanel, String searchQuery, boolean onlyFavorites) {
        targetPanel.removeAll();
        
        String sql = "SELECT * FROM art_pieces WHERE 1=1";
        if (searchQuery != null) {
            sql += " AND title LIKE ?";
        }
        if (onlyFavorites) {
            sql += " AND is_favorite = TRUE";
        }
        sql += " ORDER BY id DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (searchQuery != null) {
                pstmt.setString(1, "%" + searchQuery + "%");
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String desc = rs.getString("description");
                String path = rs.getString("file_path");
                boolean isFav = rs.getBoolean("is_favorite");

                targetPanel.add(createAspectCorrectThumbnail(id, title, desc, path, isFav));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        targetPanel.revalidate();
        targetPanel.repaint();
    }

    private JPanel createAspectCorrectThumbnail(int id, String title, String desc, String path, boolean isFav) {
        RoundedPanel thumbContainer = new RoundedPanel(16, BOX_GREY);
        thumbContainer.setLayout(new BorderLayout());
        thumbContainer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        thumbContainer.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1, true));

        JPanel imageCanvas = new JPanel(new GridBagLayout());
        imageCanvas.setOpaque(false);
        
        JLabel imgLabel = new JLabel();
        ImageIcon dynamicIcon = scaleImageKeepAspect(path, 210, 250, true);
        if (dynamicIcon != null) {
            imgLabel.setIcon(dynamicIcon);
        } else {
            imgLabel.setText("Image Missing");
            imgLabel.setForeground(TEXT_MUTED);
        }
        imageCanvas.add(imgLabel);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setBorder(new EmptyBorder(5, 8, 15, 8));

        thumbContainer.add(imageCanvas, BorderLayout.CENTER);
        thumbContainer.add(titleLabel, BorderLayout.SOUTH);

        thumbContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showDetailView(id, title, desc, path, isFav);
            }
        });

        JPanel cellWrapper = new JPanel(new BorderLayout());
        cellWrapper.setOpaque(false);
        cellWrapper.add(thumbContainer, BorderLayout.CENTER); 
        return cellWrapper;
    }

    private void saveArt(File sourceFile, String title, String desc) {
        try {
            String extension = sourceFile.getName().substring(sourceFile.getName().lastIndexOf("."));
            String uniqueName = UUID.randomUUID().toString() + extension;
            File destFile = new File(storageFolderPath, uniqueName);
            
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO art_pieces (title, description, file_path) VALUES (?, ?, ?)")) {
                pstmt.setString(1, title);
                pstmt.setString(2, desc);
                pstmt.setString(3, destFile.getAbsolutePath());
                pstmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Success! Artpiece registered.");
            showHomeView();

        } catch (IOException | SQLException ex) {
            JOptionPane.showMessageDialog(this, "IO Handling Exception: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDetailView(int id, String title, String desc, String path, boolean isFav) {
        JPanel detailPanel = new JPanel(new BorderLayout(30, 30));
        detailPanel.setOpaque(false);
        detailPanel.setBorder(new EmptyBorder(30, 45, 30, 45));

        JPanel detailHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        detailHeader.setOpaque(false);
        RoundedButton btnBack = new RoundedButton("← Back", TEXT_DARK, 12);
        btnBack.addActionListener(e -> showHomeView());
        detailHeader.add(btnBack);
        detailPanel.add(detailHeader, BorderLayout.NORTH);

        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(JLabel.CENTER);
        
        // Calculated a significantly tighter bounding box factoring in header/footer heights to stop layout clipping
        int targetScreenWidth = Toolkit.getDefaultToolkit().getScreenSize().width - 250;
        int targetScreenHeight = Toolkit.getDefaultToolkit().getScreenSize().height - 500; 
        
        // Passing 'false' prevents small images from upscaling and ruining the original ratio
        ImageIcon fullIcon = scaleImageKeepAspect(path, targetScreenWidth, targetScreenHeight, false);
        if (fullIcon != null) imgLabel.setIcon(fullIcon);
        detailPanel.add(imgLabel, BorderLayout.CENTER);

        JPanel detailsDashboard = new JPanel(new BorderLayout(15, 15));
        detailsDashboard.setOpaque(false);

        JPanel textMetaGroup = new JPanel(new GridLayout(2, 1, 6, 6));
        textMetaGroup.setOpaque(false);
        
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(TEXT_DARK);
        
        JLabel lblDesc = new JLabel("<html><center>" + (desc.isEmpty() ? "No description provided." : desc) + "</center></html>", SwingConstants.CENTER);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblDesc.setForeground(TEXT_MUTED);
        
        textMetaGroup.add(lblTitle);
        textMetaGroup.add(lblDesc);

        JPanel controlActionsGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        controlActionsGroup.setOpaque(false);
        
        String heartSymbol = isFav ? "♥" : "♡";
        RoundedButton btnHeartFav = new RoundedButton(heartSymbol + " Favorite", BTN_PINK, 12);
        btnHeartFav.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        RoundedButton btnDelete = new RoundedButton("Delete Artwork", new Color(220, 53, 69), 12);

        btnHeartFav.addActionListener(e -> {
            boolean nextFavState = !isFav;
            toggleFavorite(id, nextFavState);
            showDetailView(id, title, desc, path, nextFavState);
        });
        
        btnDelete.addActionListener(e -> deleteArt(id, path));

        controlActionsGroup.add(btnHeartFav);
        controlActionsGroup.add(btnDelete);

        detailsDashboard.add(textMetaGroup, BorderLayout.CENTER);
        detailsDashboard.add(controlActionsGroup, BorderLayout.SOUTH);

        detailPanel.add(detailsDashboard, BorderLayout.SOUTH);

        mainCardPanel.add(detailPanel, "DetailView");
        cardLayout.show(mainCardPanel, "DetailView");
    }

    private void toggleFavorite(int id, boolean makeFav) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE art_pieces SET is_favorite = ? WHERE id = ?")) {
            pstmt.setBoolean(1, makeFav);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteArt(int id, String path) {
        int confirm = JOptionPane.showConfirmDialog(this, "Permanently wipe this artwork out from registry?", "Confirm Destruction", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM art_pieces WHERE id = ?")) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
                showHomeView();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Upgraded method to support conditionally preventing upscaling
    private ImageIcon scaleImageKeepAspect(String path, int maxWidth, int maxHeight, boolean allowScaleUp) {
        try {
            ImageIcon icon = new ImageIcon(path);
            Image img = icon.getImage();
            
            int originalWidth = icon.getIconWidth();
            int originalHeight = icon.getIconHeight();

            if (originalWidth <= 0 || originalHeight <= 0) return icon;

            double widthRatio = (double) maxWidth / originalWidth;
            double heightRatio = (double) maxHeight / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            // If the image is smaller than our max bounds, stop it from stretching
            if (!allowScaleUp && ratio > 1.0) {
                ratio = 1.0;
            }

            int newWidth = (int) (originalWidth * ratio);
            int newHeight = (int) (originalHeight * ratio);

            Image newImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            return new ImageIcon(newImg);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> {
            new ArtGallery().setVisible(true);
        });
    }

    // =========================================================================
    // PREMIUM RESTRUCTURED LIGHT UI RENDER LAYER COMPONENTS
    // =========================================================================

    class ResponsiveGridPanel extends JPanel implements Scrollable {
        public ResponsiveGridPanel(int columns, int hgap, int vgap) {
            super(new GridLayout(0, columns, hgap, vgap));
            setOpaque(false);
        }
        @Override
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 22; }
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 22; }
        @Override
        public boolean getScrollableTracksViewportWidth() { return true; } 
        @Override
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    class PatternScrollPane extends JScrollPane {
        public PatternScrollPane(Component view) {
            super(view);
            setBorder(null);
            getVerticalScrollBar().setUnitIncrement(22);
            setOpaque(false);
            getViewport().setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(BG_WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(PATTERN_GREY);
            int spatialGridSize = 40; 
            for (int x = 0; x < getWidth(); x += spatialGridSize) {
                g2d.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += spatialGridSize) {
                g2d.drawLine(0, y, getWidth(), y);
            }
            
            g2d.dispose();
            super.paintComponent(g);
        }
    }

    class RoundedPanel extends JPanel {
        private int cornerRadius;
        public RoundedPanel(int radius, Color bgColor) {
            this.cornerRadius = radius;
            setBackground(bgColor);
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(getBackground());
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            graphics.dispose();
        }
    }

    class RoundedButton extends JButton {
        private int cornerRadius;
        public RoundedButton(String text, Color bgColor, int radius) {
            super(text);
            this.cornerRadius = radius;
            setBackground(bgColor);
            
            if (bgColor.equals(PANEL_WHITE) || bgColor.equals(BG_WHITE)) {
                setForeground(TEXT_DARK);
            } else {
                setForeground(Color.WHITE); 
            }
            
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(getBackground());
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            super.paintComponent(graphics);
            graphics.dispose();
        }
    }

    class RoundedTextField extends JTextField {
        private int cornerRadius;
        public RoundedTextField(int columns, int radius) {
            super(columns);
            this.cornerRadius = radius;
            setOpaque(false);
            setBackground(PANEL_WHITE);
            setForeground(TEXT_DARK);
            setCaretColor(TEXT_DARK);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(getBackground());
            graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            super.paintComponent(graphics);
            graphics.dispose();
        }
        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(BORDER_LIGHT);
            graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            graphics.dispose();
        }
    }

    class RoundedTextArea extends JTextArea {
        private int cornerRadius;
        public RoundedTextArea(int rows, int columns, int radius) {
            super(rows, columns);
            this.cornerRadius = radius;
            setOpaque(false);
            setBackground(PANEL_WHITE);
            setForeground(TEXT_DARK);
            setCaretColor(TEXT_DARK);
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(getBackground());
            graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            super.paintComponent(graphics);
            graphics.dispose();
        }
        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(BORDER_LIGHT);
            graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            graphics.dispose();
        }
    }
}
