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

    // Database Credentials - Update these to your local MySQL credentials
    private static final String DB_URL_BASE = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "art_gallery_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1122"; // Change to your password

    // Strict UI Color Palette
    private static final Color BG_BLACK = new Color(18, 18, 18);
    private static final Color BG_GREY = new Color(32, 32, 32);
    private static final Color PANEL_GREY = new Color(45, 45, 45);
    private static final Color TEXT_WHITE = new Color(240, 240, 240);
    private static final Color TEXT_MUTED = new Color(160, 160, 160);
    private static final Color BORDER_GREY = new Color(65, 65, 65);
    
    private static final Color BTN_GREEN = new Color(46, 204, 113);
    private static final Color BTN_BLUE = new Color(52, 152, 219);
    private static final Color BTN_ORANGE = new Color(230, 126, 34);
    private static final Color BTN_RED = new Color(231, 76, 60);

    // Layout and Navigation
    private CardLayout cardLayout;
    private JPanel mainCardPanel;
    private JPanel homeGridPanel;
    private JPanel searchGridPanel;
    
    // File Storage
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
            JOptionPane.showMessageDialog(null, "Database Connection Error. Please check credentials.\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL_BASE + DB_NAME, DB_USER, DB_PASSWORD);
    }

    private void setupMainFrame() {
        setTitle("Art Gallery");
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_BLACK);

        // Modern Top Navigation Bar
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 15));
        navBar.setBackground(BG_GREY);
        navBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_GREY));

        RoundedButton btnHome = new RoundedButton("Home", BTN_RED, 12);
        RoundedButton btnAddArt = new RoundedButton("Add art", BTN_GREEN, 12);
        RoundedButton btnSearchArt = new RoundedButton("Search art", BTN_BLUE, 12);
        RoundedButton btnFavArt = new RoundedButton("Favourite art", BTN_ORANGE, 12);

        btnHome.addActionListener(e -> showHomeView());
        btnAddArt.addActionListener(e -> showAddArtView());
        btnSearchArt.addActionListener(e -> showSearchView());
        btnFavArt.addActionListener(e -> loadGridData(homeGridPanel, null, true));

        navBar.add(btnHome);
        navBar.add(btnAddArt);
        navBar.add(btnSearchArt);
        navBar.add(btnFavArt);

        add(navBar, BorderLayout.NORTH);

        // Main Content Switcher Container
        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        mainCardPanel.setBackground(BG_BLACK);

        // View 1: Home View (Dynamic Image Gallery Grid)
        homeGridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 25));
        homeGridPanel.setBackground(BG_BLACK);
        JScrollPane homeScrollPane = createModernScrollPane(homeGridPanel);
        
        mainCardPanel.add(homeScrollPane, "HomeView");
        add(mainCardPanel, BorderLayout.CENTER);
    }

    private JScrollPane createModernScrollPane(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.setBackground(BG_BLACK);
        scrollPane.getViewport().setBackground(BG_BLACK);
        return scrollPane;
    }

    private void showHomeView() {
        loadGridData(homeGridPanel, null, false);
        cardLayout.show(mainCardPanel, "HomeView");
    }

    private void showSearchView() {
        JPanel searchMainPanel = new JPanel(new BorderLayout(20, 20));
        searchMainPanel.setBackground(BG_BLACK);
        searchMainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Form Bar Header
        JPanel searchBarPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        searchBarPanel.setBackground(BG_BLACK);

        JLabel lblSearch = new JLabel("Search by Title:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblSearch.setForeground(TEXT_WHITE);

        RoundedTextField txtSearch = new RoundedTextField(25, 12);
        RoundedButton btnFind = new RoundedButton("Search", BTN_BLUE, 12);

        searchBarPanel.add(lblSearch);
        searchBarPanel.add(txtSearch);
        searchBarPanel.add(btnFind);

        searchMainPanel.add(searchBarPanel, BorderLayout.NORTH);

        // Results Grid Layout
        searchGridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 25));
        searchGridPanel.setBackground(BG_BLACK);
        JScrollPane searchResultScroll = createModernScrollPane(searchGridPanel);

        searchMainPanel.add(searchResultScroll, BorderLayout.CENTER);

        // Trigger dynamic query loading
        btnFind.addActionListener(e -> {
            String query = txtSearch.getText().trim();
            if (!query.isEmpty()) {
                loadGridData(searchGridPanel, query, false);
            }
        });

        // Search operational on pressing enter key inside field
        txtSearch.addActionListener(e -> btnFind.doClick());

        mainCardPanel.add(searchMainPanel, "SearchView");
        cardLayout.show(mainCardPanel, "SearchView");
    }

    private void showAddArtView() {
        JPanel addContainerPanel = new JPanel(new GridBagLayout());
        addContainerPanel.setBackground(BG_BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Custom Clickable Rounded Upload Box
        RoundedPanel imgUploadBox = new RoundedPanel(20, PANEL_GREY);
        imgUploadBox.setLayout(new BorderLayout());
        imgUploadBox.setPreferredSize(new Dimension(320, 320));
        imgUploadBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        imgUploadBox.setBorder(BorderFactory.createLineBorder(BORDER_GREY, 1, true));

        JLabel lblPreview = new JLabel("Click here to select image from storage", SwingConstants.CENTER);
        lblPreview.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblPreview.setForeground(TEXT_MUTED);
        imgUploadBox.add(lblPreview, BorderLayout.CENTER);

        RoundedTextField txtTitle = new RoundedTextField(22, 12);
        RoundedTextArea txtDesc = new RoundedTextArea(5, 22, 12);
        
        // Wrap custom text area in customized layout container
        JScrollPane descScroll = new JScrollPane(txtDesc);
        descScroll.setBorder(null);
        descScroll.setOpaque(false);
        descScroll.getViewport().setOpaque(false);

        RoundedButton btnSave = new RoundedButton("Add to your Art gallery", BTN_GREEN, 15);

        final File[] selectedFile = {null};

        // Click-to-upload action handler directly mapping to target block
        imgUploadBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "jpeg"));
                if (chooser.showOpenDialog(ArtGallery.this) == JFileChooser.APPROVE_OPTION) {
                    selectedFile[0] = chooser.getSelectedFile();
                    lblPreview.setText("");
                    lblPreview.setIcon(scaleImage(selectedFile[0].getAbsolutePath(), 300, 300));
                }
            }
        });

        btnSave.addActionListener(e -> {
            if (selectedFile[0] == null || txtTitle.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please click the image box to upload and provide a title.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            saveArt(selectedFile[0], txtTitle.getText().trim(), txtDesc.getText().trim());
        });

        // Grid Positioning Structure setup
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        addContainerPanel.add(imgUploadBox, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; addContainerPanel.add(new JLabel("Title:") {{ setForeground(TEXT_WHITE); setFont(new Font("Segoe UI", Font.BOLD, 14)); }}, gbc);
        gbc.gridx = 1; addContainerPanel.add(txtTitle, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; addContainerPanel.add(new JLabel("Description:") {{ setForeground(TEXT_WHITE); setFont(new Font("Segoe UI", Font.BOLD, 14)); }}, gbc);
        gbc.gridx = 1; addContainerPanel.add(descScroll, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.insets = new Insets(25, 12, 12, 12);
        addContainerPanel.add(btnSave, gbc);

        mainCardPanel.add(addContainerPanel, "AddView");
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

                targetPanel.add(createModernThumbnail(id, title, desc, path, isFav));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        targetPanel.revalidate();
        targetPanel.repaint();
    }

    private JPanel createModernThumbnail(int id, String title, String desc, String path, boolean isFav) {
        RoundedPanel thumbContainer = new RoundedPanel(16, PANEL_GREY);
        thumbContainer.setLayout(new BorderLayout());
        thumbContainer.setPreferredSize(new Dimension(220, 270));
        thumbContainer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        thumbContainer.setBorder(BorderFactory.createLineBorder(BORDER_GREY, 1, true));

        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(JLabel.CENTER);
        imgLabel.setBorder(new EmptyBorder(10, 10, 5, 10));
        ImageIcon icon = scaleImage(path, 190, 190);
        if (icon != null) imgLabel.setIcon(icon);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(TEXT_WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setBorder(new EmptyBorder(5, 10, 12, 10));

        thumbContainer.add(imgLabel, BorderLayout.CENTER);
        thumbContainer.add(titleLabel, BorderLayout.SOUTH);

        thumbContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showDetailView(id, title, desc, path, isFav);
            }
        });

        return thumbContainer;
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

            JOptionPane.showMessageDialog(this, "Art added successfully!");
            showHomeView();

        } catch (IOException | SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error saving art: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDetailView(int id, String title, String desc, String path, boolean isFav) {
        JPanel detailPanel = new JPanel(new BorderLayout(25, 25));
        detailPanel.setBackground(BG_BLACK);
        detailPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Center Pane Image Presenter
        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(JLabel.CENTER);
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height - 260;
        ImageIcon fullIcon = scaleImageKeepAspect(path, Toolkit.getDefaultToolkit().getScreenSize().width - 100, screenHeight);
        if (fullIcon != null) imgLabel.setIcon(fullIcon);

        // Bottom Dashboard Panel Block
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 15));
        bottomPanel.setBackground(BG_BLACK);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setBackground(BG_BLACK);
        
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(TEXT_WHITE);
        
        JLabel lblDesc = new JLabel("<html><center>" + desc + "</center></html>", SwingConstants.CENTER);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDesc.setForeground(TEXT_MUTED);
        
        infoPanel.add(lblTitle);
        infoPanel.add(lblDesc);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        actionPanel.setBackground(BG_BLACK);
        
        RoundedButton btnFav = new RoundedButton(isFav ? "Unfavourite" : "Favourite", BTN_ORANGE, 12);
        RoundedButton btnDelete = new RoundedButton("Delete", BTN_RED, 12);

        btnFav.addActionListener(e -> toggleFavorite(id, !isFav));
        btnDelete.addActionListener(e -> deleteArt(id, path));

        actionPanel.add(btnFav);
        actionPanel.add(btnDelete);

        bottomPanel.add(infoPanel, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        detailPanel.add(imgLabel, BorderLayout.CENTER);
        detailPanel.add(bottomPanel, BorderLayout.SOUTH);

        mainCardPanel.add(detailPanel, "DetailView");
        cardLayout.show(mainCardPanel, "DetailView");
    }

    private void toggleFavorite(int id, boolean makeFav) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE art_pieces SET is_favorite = ? WHERE id = ?")) {
            pstmt.setBoolean(1, makeFav);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
            showHomeView(); 
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteArt(int id, String path) {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this art?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
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

    private ImageIcon scaleImage(String path, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(path);
            Image img = icon.getImage();
            Image newImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(newImg);
        } catch (Exception e) {
            return null;
        }
    }

    private ImageIcon scaleImageKeepAspect(String path, int maxWidth, int maxHeight) {
        try {
            ImageIcon icon = new ImageIcon(path);
            Image img = icon.getImage();
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();

            double widthRatio = (double) maxWidth / width;
            double heightRatio = (double) maxHeight / height;
            double ratio = Math.min(widthRatio, heightRatio);

            if (ratio < 1.0) {
                width = (int) (width * ratio);
                height = (int) (height * ratio);
            }

            Image newImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(newImg);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ArtGallery().setVisible(true);
        });
    }

    // =========================================================================
    // CUSTOM MODERN ANTI-ALIASED UI COMPONENTS
    // =========================================================================

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
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            setFont(new Font("Segoe UI", Font.BOLD, 14));
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
            setBackground(PANEL_GREY);
            setForeground(TEXT_WHITE);
            setCaretColor(TEXT_WHITE);
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
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
            graphics.setColor(BORDER_GREY);
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
            setBackground(PANEL_GREY);
            setForeground(TEXT_WHITE);
            setCaretColor(TEXT_WHITE);
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
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
            graphics.setColor(BORDER_GREY);
            graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            graphics.dispose();
        }
    }
}
