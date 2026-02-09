import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * EquiEat - SMART RATIONING SYSTEM 
 * ---------------------------------------------------------
 * What does this app do?
 * - Help distribute food/supplies fairly to families in need
 * - Keep a record of what was given to who
 * - Print out tickets so families can claim their supplies
 * 
 * Main Sections:
 * 1. Load family data from a CSV file
 * 2. Add supplies to warehouse
 * 3. Calculate fair sharing
 * 4. Show results in tables
 * 5. Save reports as files
 * 
 * How to think about this:
 * - Family = one household needing supplies
 * - Supply = one type of item in warehouse (rice, beans, medicine, etc)
 * - Distribution = giving each family their fair share
 * - Leftover = extra items that couldn't be divided fairly
 */
public class SmartRationGUI extends JFrame { // Creates our GUI/Window

    // TODO: Store family and supply data
    // Remember all families and supplies throughout the app
    private List<Family> loadedFamilies = new ArrayList<>();
    private List<Supply> inventoryList = new ArrayList<>();

    // Todo checklist - Main App Setup
    // [x] Create main window donee
    // [x] Make 3 tabs (Control, Results, Reserves) doneeee
    // [x] Add buttons for loading data and running distribution doneee
    // [x] Connect buttons to actions doneeeeeeee
    // [x] Make tables look nice with colors doneeeeeeeee

    // Tables need "models" to hold data
    // Think of model = the container, table = how it looks on screen
    private DefaultTableModel inventoryTableModel;
    private DefaultTableModel resultsTableModel;
    private DefaultTableModel reserveTableModel;
    private JLabel statusLabel; // Shows busy/ready status

    // Engine = robot that does math and distributes supplies
    // Logger = assistant that writes down everything we do
    // FINAL = can't change these once created
    private final RationEngine engine = new RationEngine();
    private final AuditLogger logger = new AuditLogger();

    
    // Try = "try this code"
    // Catch = "if something breaks, do this instead"
    // This protects the program from crashing
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } // Make it look like Windows/Mac
        catch (Exception ignored) {} // If fail, just ignore
        SwingUtilities.invokeLater(() -> new bootStrapper().setVisible(true)); // Show loading screen
    } // Lets the background thread do the math while the EDT provides the user GUI

    // Loading screen - shows fancy animation while app starts
    // Makes user think something is happening
static class bootStrapper extends JFrame {
    public bootStrapper() {
        AuditLogger tempLogger = new AuditLogger();
        tempLogger.log("SYSTEM_STARTUP", "Application launched.");
        
        setTitle("BootStrapper - Smart Rationing System");
        setSize(1000, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Put background image as the base
        // All buttons and labels go on top of it
        JLabel background = new JLabel(new ImageIcon("bootStrapBG.png"));
        background.setLayout(new BorderLayout());
        setContentPane(background);
        
        // Setup the text for loading screen
        JLabel label = new JLabel("Loading EquiEat Smart Rationing System...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel loadingStatus = new JLabel("Loading... ", SwingConstants.LEFT);
        JLabel percentage = new JLabel("0%", SwingConstants.RIGHT);
        loadingStatus.setFont(new Font("Arial", Font.PLAIN, 15));
        percentage.setFont(new Font("Arial", Font.PLAIN, 15));
        
        bottomPanel.add(loadingStatus, BorderLayout.WEST);
        bottomPanel.add(percentage, BorderLayout.EAST);
        
        background.add(label, BorderLayout.CENTER);
        background.add(bottomPanel, BorderLayout.SOUTH);

        // Need array because we want to change progress inside the timer
        int[] progress = {0};
        javax.swing.Timer progressTimer = new javax.swing.Timer(50, e -> {
            progress[0]++;
            percentage.setText(progress[0] + "%");
            
            // Update message at different percentages
            if (progress[0] == 10) loadingStatus.setText("Loading Methods...");
            else if (progress[0] == 50) loadingStatus.setText("Initializing Systems...");
            else if (progress[0] == 90) loadingStatus.setText("Almost Ready...");
            
            // Done loading - close splash screen and show main window
            if (progress[0] >= 100) {
                ((javax.swing.Timer)e.getSource()).stop();
                dispose();
                new SmartRationGUI().setVisible(true);
            }
        });
        
        progressTimer.start();
    }
}

    public SmartRationGUI() {
        logger.log("SYSTEM_STARTUP", "Main Application launched.");

        setTitle("EquiEat -Smart Rationing System (SRS) - Integer Mode");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close program when user clicks X
        setLocationRelativeTo(null); // Center window on screen

        // Load and scale the window icon (32x32 pixels)
        try {
            ImageIcon icon = new ImageIcon("icon.png");
            Image scaledIcon = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            setIconImage(scaledIcon);
        } catch (Exception ignored) {}

        // Global light gray background for the main window
        getContentPane().setBackground(new Color(240, 240, 240));

        // Create tabbed interface (tabs at top, each with different content)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(33, 174, 192));
        tabbedPane.setForeground(Color.BLACK);
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));

        // TAB 1: MAIN CONTROL PANEL - where users import data and add inventory items
        JPanel operationsPanel = new JPanel(new BorderLayout(10, 10));
        operationsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        operationsPanel.setBackground(new Color(240, 240, 240));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(true);
        topPanel.setBackground(new Color(33, 174, 192));

        JButton loadBtn = new JButton(" 1. Import Demographic CSV");
        loadBtn.setFocusable(false);
        loadBtn.setIcon(createTextIcon("↓", Color.WHITE, new Color(76, 175, 80), 18));
        loadBtn.setBackground(new Color(76, 175, 80)); loadBtn.setForeground(Color.BLACK);
        loadBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

        statusLabel = new JLabel("Status: Waiting for Data...");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setForeground(Color.WHITE);
        loadBtn.addActionListener(e -> loadCSV());

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        leftTop.setOpaque(false);
        leftTop.add(loadBtn);
        leftTop.add(statusLabel);
        topPanel.add(leftTop, BorderLayout.WEST);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rightTop.setOpaque(false);
        JLabel equieatBrand = new JLabel("EquiEat");
        equieatBrand.setFont(new Font("SansSerif", Font.BOLD, 30));
        equieatBrand.setForeground(Color.WHITE);
        ImageIcon logoIcon = null;
        try { logoIcon = new ImageIcon("logo.png"); } catch (Exception ignored) {}
        JLabel logoLabel = (logoIcon != null && logoIcon.getIconWidth() > 0)
            ? new JLabel(logoIcon)
            : new JLabel("Supplies", SwingConstants.CENTER);
        logoLabel.setPreferredSize(new Dimension(100, 100));
        logoLabel.setForeground(Color.WHITE);
        rightTop.add(equieatBrand);
        rightTop.add(logoLabel);
        topPanel.add(rightTop, BorderLayout.EAST);

        operationsPanel.add(topPanel, BorderLayout.NORTH);

        JPanel formPanel = createInventoryForm();
        String[] invCols = {"Category", "Item Name", "Qty", "Target Priority"};
        inventoryTableModel = new DefaultTableModel(invCols, 0);
        JTable invTable = new JTable(inventoryTableModel);
        invTable.setRowHeight(35);
        invTable.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
        invTable.getTableHeader().setBackground(new Color(33, 174, 192));
        invTable.getTableHeader().setForeground(Color.BLACK);
        invTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        JScrollPane invScroll = new JScrollPane(invTable);
        invScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 140, 0), 2),
                "2. Warehouse Inventory"
            ),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, invScroll);
        splitPane.setDividerLocation(350);
        operationsPanel.add(splitPane, BorderLayout.CENTER);

        JButton runBtn = new JButton(" 3. RUN DISTRIBUTION & ANALYZE POPULATION");
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        runBtn.setBackground(new Color(32, 160, 162));
        runBtn.setForeground(Color.BLACK);
        runBtn.setFocusable(false);
        runBtn.setIcon(createTextIcon("▶", Color.WHITE, new Color(32, 160, 162), 16));
        runBtn.addActionListener(e -> runDistribution(tabbedPane));
        operationsPanel.add(runBtn, BorderLayout.SOUTH);

        tabbedPane.addTab("Control Center", operationsPanel);

        // Display distribution results in table format
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setOpaque(true);
        resultsPanel.setBackground(new Color(240, 240, 240));
        String[] resCols = {"Family ID", "Head of Family", "Size", "Needs", "RATION PACK CONTENT"};
        resultsTableModel = new DefaultTableModel(resCols, 0);
        JTable resultsTable = new JTable(resultsTableModel);
        resultsTable.setRowHeight(30);
        resultsTable.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
        // Center-align the header text for better layout
        ((DefaultTableCellRenderer)resultsTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        resultsTable.getTableHeader().setBackground(new Color(32, 160, 162));
        resultsTable.getTableHeader().setForeground(Color.BLACK);
        resultsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        // Column sizing
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(500);
        // Use text-wrapping renderer for the packing content column (so long text wraps to multiple lines)
        resultsTable.getColumnModel().getColumn(4).setCellRenderer(new WrapCellRenderer());
        // Center-align the numeric size column
        resultsTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {{ setHorizontalAlignment(SwingConstants.CENTER); setBackground(new Color(243,247,255)); }});
        JScrollPane resultsScroll = new JScrollPane(resultsTable);
        resultsScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(32, 160, 162), 2), "Distribution Results"),
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        resultsPanel.add(resultsScroll, BorderLayout.CENTER);

        JButton exportBtn = new JButton(" Export Reports & Tickets");
        exportBtn.setIcon(createTextIcon("⇩", Color.BLACK, new Color(32, 160, 162), 16));
        exportBtn.setBackground(new Color(32, 160, 162)); exportBtn.setForeground(Color.BLACK);
        exportBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        exportBtn.setFocusable(false);
        exportBtn.addActionListener(e -> exportResults());
        resultsPanel.add(exportBtn, BorderLayout.SOUTH);
        tabbedPane.addTab("Distribution Results", resultsPanel);

        // Reserve and excess stock section - shows leftover items
        JPanel reservePanel = new JPanel(new BorderLayout());
        reservePanel.setOpaque(true);
        reservePanel.setBackground(new Color(240, 240, 240));
        String[] reserveCols = {"Category", "Item Name", "RESERVE QUANTITY", "Status Note"};
        reserveTableModel = new DefaultTableModel(reserveCols, 0);
        JTable reserveTable = new JTable(reserveTableModel);
        reserveTable.setRowHeight(28);
        reserveTable.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
        ((DefaultTableCellRenderer)reserveTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        reserveTable.getTableHeader().setBackground(new Color(32, 160, 162));
        reserveTable.getTableHeader().setForeground(Color.BLACK);
        reserveTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        // Column sizing for clarity
        reserveTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        reserveTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        reserveTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        reserveTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        // Apply special renderers: wrap item names, right-align quantities, color-code status
        reserveTable.getColumnModel().getColumn(1).setCellRenderer(new WrapCellRenderer());
        reserveTable.getColumnModel().getColumn(2).setCellRenderer(new IntegerCellRenderer());
        reserveTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        JScrollPane reserveScroll = new JScrollPane(reserveTable);
        reserveScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(32, 160, 162), 2), "Reserve & Excess Stock"),
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        reservePanel.add(reserveScroll, BorderLayout.CENTER);
        reservePanel.add(new JLabel("  * Includes Specialized Medicine and Leftovers (Whole Numbers Only)"), BorderLayout.SOUTH);
        tabbedPane.addTab("Reserve & Excess Stock", reservePanel);

        // Add colored icons to tabs for visual identification
        tabbedPane.setIconAt(0, createCircleIcon(new Color(33, 130, 220), 14));
        tabbedPane.setIconAt(1, createCircleIcon(new Color(60, 120, 200), 14));
        tabbedPane.setIconAt(2, createCircleIcon(new Color(255, 140, 0), 14));

        add(tabbedPane);
    }

    private JPanel createInventoryForm() {
        // TO-DO: Create the form where user adds supplies
        // Step 1: Make panel
        // Step 2: Add dropdown for category
        // Step 3: Add text boxes for name and quantity
        // Step 4: Add button to confirm
        
        // USE: Orange color theme to stand out from other sections
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setOpaque(true);
        panel.setBackground(new Color(230, 230, 230));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 140, 0), 2),
                "Add Supply Item"
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JComboBox<SupplyCategory> catBox = new JComboBox<>(SupplyCategory.values());
        catBox.setBackground(Color.WHITE);
        catBox.setForeground(new Color(0, 0, 0));
        catBox.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        JTextField nameField = new JTextField();
        nameField.setBackground(Color.WHITE);
        nameField.setForeground(Color.BLACK);
        nameField.setCaretColor(Color.BLACK);
        nameField.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameField.setBorder(BorderFactory.createLineBorder(new Color(200, 100, 0)));
        
        JTextField qtyField = new JTextField();
        qtyField.setBackground(Color.WHITE);
        qtyField.setForeground(Color.BLACK);
        qtyField.setCaretColor(Color.BLACK);
        qtyField.setFont(new Font("SansSerif", Font.BOLD, 12));
        qtyField.setBorder(BorderFactory.createLineBorder(new Color(200, 100, 0)));
        
        JComboBox<PriorityAttribute> prioBox = new JComboBox<>(PriorityAttribute.values());
        prioBox.insertItemAt(null, 0);
        prioBox.setSelectedIndex(0);
        prioBox.setBackground(Color.WHITE);
        prioBox.setForeground(new Color(0, 0, 0));
        prioBox.setFont(new Font("SansSerif", Font.BOLD, 12));
        prioBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "EVERYONE (General)" : value.toString());
                setFont(new Font("SansSerif", Font.BOLD, 12));
                if (!isSelected) {
                    setBackground(Color.WHITE);
                    setForeground(new Color(0, 0, 0));
                }
                return this;
            }
        });

        JLabel catLabel = new JLabel("Category:");
        catLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        catLabel.setForeground(new Color(0, 0, 0));
        
        JLabel nameLabel = new JLabel("Item Name:");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameLabel.setForeground(new Color(0, 0, 0));
        
        JLabel qtyLabel = new JLabel("Quantity (Total):");
        qtyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        qtyLabel.setForeground(new Color(0, 0, 0));
        
        JLabel prioLabel = new JLabel("Priority Target:");
        prioLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        prioLabel.setForeground(new Color(0, 0, 0));

        panel.add(catLabel); panel.add(catBox);
        panel.add(nameLabel); panel.add(nameField);
        panel.add(qtyLabel); panel.add(qtyField);
        panel.add(prioLabel); panel.add(prioBox);

        JButton addBtn = new JButton(" Add to Inventory");
        addBtn.setIcon(createTextIcon("+", Color.BLACK, new Color(76, 175, 80), 14));
        addBtn.setBackground(new Color(76, 175, 80)); addBtn.setForeground(Color.BLACK);
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        addBtn.setFocusable(false);
        addBtn.addActionListener(e -> {
            try {
                String name = nameField.getText();
                int qty = Integer.parseInt(qtyField.getText());
                SupplyCategory cat = (SupplyCategory) catBox.getSelectedItem();
                PriorityAttribute prio = (PriorityAttribute) prioBox.getSelectedItem();

                Supply s = new Supply(name, cat, qty, prio);
                inventoryList.add(s);
                inventoryTableModel.addRow(new Object[]{cat, name, qty, (prio == null ? "ALL" : prio)});
                logger.log("INVENTORY_ADD", "Added " + qty + "x " + name);
                nameField.setText(""); qtyField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid Input.");
            }
        });
        panel.add(new JLabel("")); panel.add(addBtn);
        return panel;
    }

    private void loadCSV() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            loadedFamilies = importCSV(file.getAbsolutePath());
            if (!loadedFamilies.isEmpty()) {
                statusLabel.setText("Status: Ready (" + loadedFamilies.size() + " Families Loaded)");
                statusLabel.setForeground(new Color(0, 150, 0));
                logger.log("DATA_LOAD", "Loaded demographics from: " + file.getName());
            }
        }
    }

    private List<Family> importCSV(String filePath) {
        // TODO: Read family data from file
        // - Read lines one by one
        // - Find the right columns (ID, name, size)
        // - Create Family objects
        // - Handle bad data gracefully
        List<Family> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            if (line == null) return list;
            line = line.replace("\uFEFF", "");
            String delimiter = line.contains(";") ? ";" : ",";
            String[] headers = line.split(delimiter);

            int idIndex = -1, nameIndex = -1, sizeIndex = -1, priorityIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().toLowerCase().replace("\"", "");
                if (h.equals("id")) idIndex = i;
                else if (h.contains("head") || h.contains("name")) nameIndex = i;
                else if (h.contains("size")) sizeIndex = i;
                else if (h.contains("priorit")) priorityIndex = i;
            }
            if (sizeIndex == -1) {
                JOptionPane.showMessageDialog(this, "Error: Could not find 'Size' column.");
                return list;
            }
            while ((line = br.readLine()) != null) {
                if(line.trim().isEmpty()) continue;
                String[] data = line.split(delimiter);
                if (data.length <= sizeIndex) continue;
                try {
                    String id = (idIndex != -1 && idIndex < data.length) ? data[idIndex].replace("\"", "").trim() : "Unknown";
                    String name = (nameIndex != -1 && nameIndex < data.length) ? data[nameIndex].replace("\"", "").trim() : "Unknown";
                    int size = Integer.parseInt(data[sizeIndex].replace("\"", "").trim());
                    Set<PriorityAttribute> attributes = new HashSet<>();
                    if (priorityIndex != -1 && priorityIndex < data.length) {
                        String rawPrio = data[priorityIndex].replace("\"", "");
                        for (String p : rawPrio.split(";")) {
                            String key = p.trim().toUpperCase().replace(" ", "_");
                            if (!key.isEmpty() && !key.equals("NONE")) {
                                try { attributes.add(PriorityAttribute.valueOf(key)); } catch(Exception ignored){}
                            }
                        }
                    }
                    list.add(new Family(id, name, size, attributes));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage());
        }
        return list;
    }

    private void runDistribution(JTabbedPane tabs) {
        // TODO: Main calculation happens here
        // Step 1: Check if we have data to process
        // Step 2: Clear old results
        // Step 3: Calculate fair shares
        // Step 4: Show results
        // Step 5: Display summary
        if (loadedFamilies.isEmpty() || inventoryList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Missing Data.");
            return;
        }

        // Reset
        for (Family f : loadedFamilies) f.clearReceived();
        for (Supply s : inventoryList) s.setLeftover(0);
        int totalPop = loadedFamilies.stream().mapToInt(Family::getMemberCount).sum();

        // 2. Run Engine (Updated to Whole Numbers)
        engine.distributeWithRounding(loadedFamilies, inventoryList, totalPop);

        // The Analysis of Demographic Data
        String censusReport = DemographicAnalyzer.analyze(loadedFamilies);

        // Update UI
        resultsTableModel.setRowCount(0);
        for (Family f : loadedFamilies) {
            String pack = f.getFormattedPackingList();
            resultsTableModel.addRow(new Object[]{
                    f.getId(), f.getHeadOfFamily(), f.getMemberCount(), f.attributes.toString().replace(",", " "), pack
            });
        }

        reserveTableModel.setRowCount(0);
        for (Supply s : inventoryList) {
            if (s.cat == SupplyCategory.SPECIALIZED_MED || s.leftover > 0) {
                String status = (s.cat == SupplyCategory.SPECIALIZED_MED) ? "Medical Stock" : "Rounding Excess";
                // Formatting for display: Whole numbers only
                String displayQty = String.format("%d", (int)s.leftover);
                reserveTableModel.addRow(new Object[]{ s.cat, s.name, displayQty, status });
            }
        }

        logger.log("DISTRIBUTION_RUN", "Computed rations for " + loadedFamilies.size() + " families.");

        tabs.setSelectedIndex(1);

        // Show result with Demographic Info
        JOptionPane.showMessageDialog(this, censusReport);
    }

    private void exportResults() {
        try {
            ReportGenerator.generatePackingList(loadedFamilies, "Final_Packing_List.csv");
            ReportGenerator.generateReserveReport(inventoryList, "Reserve_Stock_Report.txt");
            StubGenerator.generateHTMLStubs(loadedFamilies, "Claim_Stubs.html");
            logger.log("EXPORT", "Files generated: CSV, TXT, HTML.");

            JOptionPane.showMessageDialog(this, "Files Generated:\n1. Final_Packing_List.csv\n2. Reserve_Stock_Report.txt\n3. Claim_Stubs.html");
            Desktop.getDesktop().open(new File("Claim_Stubs.html"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exporting: " + e.getMessage());
        }
    }

    // TODO: Demographic analysis
    // Count how many families have special needs
    public static class DemographicAnalyzer {
        public static String analyze(List<Family> families) {
            int totalFamilies = families.size();
            int totalPop = families.stream().mapToInt(Family::getMemberCount).sum();

            // Count households with babies, old people, injured, PWDs
            int hasInfant = 0;
            int hasSenior = 0;
            int hasInjured = 0; // Count families with injured members
            int hasPWD = 0;     // Count families with disabled members

            for (Family f : families) {
                if (f.hasAttribute(PriorityAttribute.HAS_INFANT)) hasInfant++;
                if (f.hasAttribute(PriorityAttribute.HAS_SENIOR)) hasSenior++;
                if (f.hasAttribute(PriorityAttribute.INJURED)) hasInjured++;
                if (f.hasAttribute(PriorityAttribute.PWD)) hasPWD++;
            }

            // Make the summary
            StringBuilder sb = new StringBuilder();
            sb.append("EquiEat Complete!\n\n");
            sb.append("Demographic Analysis Summary:\n");
            sb.append("----------------------------------\n");
            sb.append("Total Population:   ").append(totalPop).append(" citizens\n");
            sb.append("Total Families:     ").append(totalFamilies).append("\n\n");
            sb.append("Number of Vulnerable Persons:\n");
            sb.append("   • With Infants:  ").append(hasInfant).append("\n");
            sb.append("   • With Seniors:  ").append(hasSenior).append("\n");
            sb.append("   • With Injured:  ").append(hasInjured).append("\n");
            if (hasPWD > 0) sb.append("   • With PWDs:     ").append(hasPWD).append("\n");

            return sb.toString();
        }
    }

    // AuditLogger - writes down everything we do (for safety)
    // WHY? So we can see what happened if something goes wrong
    // Keep a record: time, action, details
    public static class AuditLogger {
        private final String LOG_FILE = "audit_log.html";
        
        public void log(String action, String details) {
            File logFile = new File(LOG_FILE);
            boolean fileExists = logFile.exists();

            try{
                if(!fileExists){
                    try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE))){ // creates HTML file
                        pw.println("<!DOCTYPE html>");
                        pw.println("<html><head><title>EquiEat - Audit Log</title>");
                        pw.println("<meta charset='UTF-8'>");
                        pw.println("<style>");
                        pw.println("body{font-family: Arial, \"Times New Roman\", Times , serif; background: linear-gradient(to bottom, #47BECE, #F3F3EF); height: 100%; margin: 0; background-repeat: no-repeat; background-attachment: fixed; padding: 20px;}");
                        pw.println("nav{position: fixed; display: flex; top:0; right:0; width:100%; background-color: #fff; padding: 1rem; flex-direction: column; gap:1rem; justify-content: center; z-index: 10;}");
                        pw.println("h1 {color: #21AEC0; text-align: center; letter-spacing: 2px; font-style: Arial ;}");
                        pw.println("table {width: 100%; border-collapse: collapse; background-color: #fff; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); margin-top: 100px;}");
                        pw.println("th {background: #21aec0; color: #fff; padding: 12px; text-align: left; border: 2px solid #fff;}");
                        pw.println("td {padding: 10px; border-bottom: 1px solid #ddd;}");
                        pw.println("tr:hover {background-color: #f1f1f1;}");
                        pw.println(".Timestamp {color: #666; font-size: 14px;}");
                        pw.println(".action {color: #333; font-weight: bold;}");
                        pw.println(".details {color: #555; font-size: 14px;}");
                        pw.println(".SYSTEM_STARTUP {color: #4CAF50;}");
                        pw.println(".DATA_LOAD {color: #2196F3;}");
                        pw.println(".INVENTORY_ADD {color:#FF9800;}");
                        pw.println(".DISTRIBUTION_RUN {color: #9C27B0;}");
                        pw.println(".EXPORT {color: #E91E63;}");
                        pw.println("</style></head><body>");
                        pw.println("<nav><h1>EquiEat Audit Log</h1></nav>");
                        pw.println("<table><tr><th>Timestamp</th><th>Action</th><th>Details</th></tr>");
                        pw.println("<!-- LOG_ENTRIES -->");
                        pw.println("</table>");
                        pw.println("</body></html>");
                    }
                }

                StringBuilder content = new StringBuilder();
                try(BufferedReader br = new BufferedReader(new FileReader(LOG_FILE))){
                    String line;
                    while((line = br.readLine()) != null){
                        content.append(line).append("\n");
                    }
                }

                String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String newEntry = String.format("<tr><td class='timestamp'>%s</td><td class='action %s'>%s</td><td class='details'>%s</td></tr>\n<!-- LOG_ENTRIES -->", timeStamp, action, action, details); // inputs new HTML table when new actions have performed 

                String updatedContent = content.toString().replaceFirst("<!-- LOG_ENTRIES -->", newEntry);

                try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE))) {
                    pw.print(updatedContent);
                }

            }catch(IOException e){
                System.err.println("Logger Error: " + e.getMessage());
            }

        }
    }


    // StubGenerator - creates printable tickets for families to claim their rations
    public static class StubGenerator {
        public static void generateHTMLStubs(List<Family> families, String filename) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><style>");
            html.append("body { font-family: Arial, sans-serif; background: #f0f0f0; }");
            html.append(".ticket { background: #fff; width: 300px; border: 2px dashed #333; padding: 15px; margin: 10px; display: inline-block; vertical-align: top; }");
            html.append(".header { font-weight: bold; font-size: 16px; border-bottom: 2px solid black; margin-bottom: 10px; }");
            html.append(".item { font-size: 14px; padding: 2px 0; }");
            html.append(".footer { margin-top: 10px; font-size: 10px; color: grey; text-align: right; }");
            html.append(".prio { color: red; font-weight: bold; font-size: 11px; }");
            html.append("</style></head><body>");
            html.append("<h2>Relief Distribution Claim Stubs</h2>");
            for (Family f : families) {
                String pack = f.getFormattedPackingList();
                if (pack.isEmpty()) continue;
                html.append("<div class='ticket'>");
                html.append("<div class='header'>FAMILY: ").append(f.getHeadOfFamily()).append("</div>");
                html.append("<div><strong>ID:</strong> ").append(f.getId()).append("</div>");
                html.append("<div><strong>Members:</strong> ").append(f.getMemberCount()).append("</div>");
                if (!f.attributes.isEmpty()) html.append("<div class='prio'>NOTES: ").append(f.attributes).append("</div>");
                html.append("<hr>");
                for (String item : pack.split("\\+")) html.append("<div class='item'>&#9744; ").append(item.trim()).append("</div>");
                html.append("<div class='footer'>EquiEat Distribution</div></div>");
            }
            html.append("</body></html>");
            try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) { pw.print(html.toString()); }
        }
    }

    // Small utility to create a colored round icon for tabs and buttons
    // INPUT: color, size  OUTPUT: small pretty circle
    private static ImageIcon createCircleIcon(Color color, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(0, 0, size - 1, size - 1);
        g.setColor(color.darker());
        g.drawOval(0, 0, size - 1, size - 1);
        g.dispose();
        return new ImageIcon(img);
    }

    // Create a small icon with text in center (for button decorations)
    // Make a rounded square and put text in the middle
    private static ImageIcon createTextIcon(String text, Color fg, Color bg, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        // Draw background box
        g.setColor(bg);
        g.fillRoundRect(0, 0, size, size, size/4, size/4);
        // Draw border
        g.setColor(bg.darker());
        g.drawRoundRect(0, 0, size-1, size-1, size/4, size/4);
        // Draw the text
        g.setColor(fg);
        Font font = new Font("SansSerif", Font.BOLD, Math.max(10, size/2));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        // Center the text - both left-right and up-down
        int tx = (size - fm.stringWidth(text)) / 2;
        int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, tx, ty);
        g.dispose();
        return new ImageIcon(img);
    }

    // StripedTableCellRenderer - makes every other row a different color
    // WHY? Easier to read tables when rows have alternating colors
    // EFFECT: Light gray row, light blue row, light gray row...
    public static class StripedTableCellRenderer extends DefaultTableCellRenderer {
        private final Color evenColor = new Color(250, 250, 250);
        private final Color oddColor = new Color(243, 247, 255);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground((row % 2 == 0) ? evenColor : oddColor);
                setForeground(Color.BLACK);
            }
            setBorder(noFocusBorder);
            return this;
        }
    }

    // WrapCellRenderer - splits long text into multiple lines
    // PROBLEM SOLVED: Text was cut off in table cells
    // SOLUTION: Make cells taller to fit wrapped text
    public static class WrapCellRenderer extends JTextArea implements TableCellRenderer {
        public WrapCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setFont(new Font("SansSerif", Font.PLAIN, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground((row % 2 == 0) ? new Color(250, 250, 250) : new Color(243, 247, 255));
                setForeground(Color.BLACK);
            }
            // Adjust row height to fit wrapped text content
            int prefHeight = (int) getPreferredSize().getHeight();
            if (table.getRowHeight(row) != prefHeight) table.setRowHeight(row, Math.max(table.getRowHeight(row), prefHeight));
            return this;
        }
    }

    // IntegerCellRenderer - shows numbers on the right side of cells
    // (Instead of left like text)
    public static class IntegerCellRenderer extends DefaultTableCellRenderer {
        public IntegerCellRenderer() { setHorizontalAlignment(SwingConstants.RIGHT); }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    // StatusCellRenderer - color codes the status
    // Red = medical stuff, Green = extra stuff we have left
    public static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String s = (value == null) ? "" : value.toString();
            setHorizontalAlignment(SwingConstants.CENTER);
            if (s.toLowerCase().contains("medical")) {
                setBackground(new Color(255, 235, 238));
                setForeground(new Color(183, 28, 28));
            } else if (s.toLowerCase().contains("rounding") || s.toLowerCase().contains("excess")) {
                setBackground(new Color(232, 245, 233));
                setForeground(new Color(27, 94, 32));
            } else {
                setBackground((row % 2 == 0) ? new Color(250, 250, 250) : new Color(243, 247, 255));
                setForeground(Color.BLACK);
            }
            setBorder(noFocusBorder);
            return this;
        }
    }

    // Type definitions - restrict choices to valid options
    // enum = only these values allowed
    public enum PriorityAttribute { HAS_INFANT, HAS_SENIOR, PREGNANT, LACTATING, PWD, DIABETIC, INJURED }
    public enum SupplyCategory { STAPLE, PROTEIN, PRIORITY_NUTRITION, GENERAL_HEALTH, SPECIALIZED_MED }

    // Family class - one household needing supplies
    // PRIVATE = only this class touches these values
    public static class Family {
        private String id, headOfFamily;
        private int memberCount;
        public Set<PriorityAttribute> attributes;

        // Store items this family got (keeps order received)
        // Map = key(item name) points to value(quantity)
        private Map<String, Integer> itemsReceived = new LinkedHashMap<>();


        // Start a family with its info
        public Family(String id, String name, int size, Set<PriorityAttribute> attrs) {
            this.id = id; this.headOfFamily = name; this.memberCount = size; this.attributes = attrs;
        }
        // Record one item given to this family
        public void receiveItem(String item, int qty) { itemsReceived.put(item, itemsReceived.getOrDefault(item, 0) + qty); }

        // Forget old items (when we calculate again)
        public void clearReceived() { itemsReceived.clear(); }
        // Does family have this special attribute? (baby, old, etc)
        public boolean hasAttribute(PriorityAttribute attr) { return attributes.contains(attr); }
        public int getMemberCount() { return memberCount; }
        public String getId() { return id; }
        public String getHeadOfFamily() { return headOfFamily; }

        // Make a nice readable list of items: "5 pcs of rice + 3 pcs of beans"
        public String getFormattedPackingList() {
            List<String> s = new ArrayList<>();
            for (Map.Entry<String, Integer> e : itemsReceived.entrySet()) {
                // Format: "5 pcs of rice"
                s.add(String.format("%d pcs of %s", e.getValue(), e.getKey()));
            }
            return String.join(" + ", s); // Join with " + " between items
        }
    }

    // Supply class - one type of item in warehouse
    public static class Supply {
        String name; SupplyCategory cat; int qty; PriorityAttribute target; double leftover;
        // Create one supply item
        public Supply(String n, SupplyCategory c, int q, PriorityAttribute t) { name=n; cat=c; qty=q; target=t; }
        // Store how much was left over
        public void setLeftover(double l) { this.leftover = l; }
    }

    // RationEngine - the brain that calculates fair sharing
    public static class RationEngine {
        // NOTES: How Distribution Works
        // ==============================
        // 1. For each supply item in warehouse:
        //    - Medical items? Keep them, don't give out
        //    - Who can get it? (everyone or just one group?)
        // 2. Calculate: How much per person? (total ÷ number of people)
        // 3. For each family: Give them their share
        // 4. Left over? Keep it as reserve
        //
        // Example: 100kg rice for 50 people
        //   Each person gets: 100 ÷ 50 = 2kg
        //   Family of 4 gets: 2 × 4 = 8kg
        public void distributeWithRounding(List<Family> families, List<Supply> inventory, int totalPop) {
            for (Supply item : inventory) {
                    // Medical items stay in reserve, don't distribute to families
                if (item.cat == SupplyCategory.SPECIALIZED_MED) {
                    item.setLeftover(item.qty);
                continue;
            }
            // Find all families eligible to receive this item
            List<Family> eligible = new ArrayList<>();
                int eligiblePop = 0;
                if (item.target != null) {
                    // Special group only? Give just to them
                    for (Family f : families) if (f.hasAttribute(item.target)) eligible.add(f);
                    eligiblePop = eligible.size();
                } else {
                    // Nope, give to everyone
                    eligible.addAll(families);
                    eligiblePop = totalPop;
                }

                // Count up what was distributed
                int distributedTotal = 0;

                if (eligiblePop > 0) {
                    double unitShare = (double) item.qty / eligiblePop;
                    for (Family f : eligible) {
                        // The math: How much does this family get?
                        double rawAllocation;
                        if (item.target != null) rawAllocation = (double) item.qty / eligible.size();
                        else rawAllocation = unitShare * f.getMemberCount();

                        // Convert decimals to whole numbers (7.8 becomes 7)
                        int safeAllocation = (int) Math.floor(rawAllocation);

                        if (safeAllocation > 0) {
                            // Give to family and count it
                            f.receiveItem(item.name, safeAllocation);
                            distributedTotal += safeAllocation;
                        }
                    }
                }
                // Calculate leftover
                item.setLeftover(item.qty - distributedTotal);
            }
        }
    }

    // ReportGenerator - saves results to files
    public static class ReportGenerator {
        public static void generatePackingList(List<Family> fList, String fname) throws IOException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(fname))) {
                pw.println("Family_ID,Head,Size,Priorities,Ration_Allocation");
                for (Family f : fList) {
                    pw.printf("%s,%s,%d,%s,\"%s\"%n",
                            f.getId(), f.getHeadOfFamily(), f.getMemberCount(),
                            f.attributes.toString().replace(",", " "),
                            f.getFormattedPackingList());
                }
            }
        }
        public static void generateReserveReport(List<Supply> inv, String fname) throws IOException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(fname))) {
                pw.println("=== RESERVE & MEDICAL REPORT ===");
                pw.println("Date: " + new Date());
                pw.println("----------------------------------------------");
                pw.printf("%-20s | %-15s | %-15s%n", "ITEM", "CATEGORY", "RESERVE QTY");
                pw.println("----------------------------------------------");
                for(Supply s : inv) {
                    if (s.cat == SupplyCategory.SPECIALIZED_MED || s.leftover > 0) {
                        // Show leftovers as whole number
                        String displayQty = String.format("%d", (int)s.leftover);
                        pw.printf("%-20s | %-15s | %-15s%n", s.name, s.cat, displayQty);
                    }
                }
            }
        }
    }

}