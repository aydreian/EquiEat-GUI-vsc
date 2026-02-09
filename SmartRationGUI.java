import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableModel;


/**
 * EquiEat - SMART RATIONING SYSTEM 
 * ---------------------------------------------------------
 * FEATURES:
 * 1. Demographic Analysis: Counts Infants, Seniors, Injured, etc.
 * 2. Audit Log: Logs actions to 'audit_log.html'.
 * 3. Claim Stubs: Generates printable HTML tickets.
 */
public class SmartRationGUI extends JFrame { // Creates our GUI/Window

    // To Save The Data We Use Private
    // Local Variable Disappears After Being Used In A Method
    // Only This Class Will Be Able To Modify These PRIVATE Variables (Para hindi magalaw ng ibang class)
    private List<Family> loadedFamilies = new ArrayList<>();
    private List<Supply> inventoryList = new ArrayList<>();

    // For Data Visualization We Put It Into A Model
    // You Can't Add It Directly As A Data To A Table Or You Can But Mahirap and Hassle
    // MVC: Model-View-Controller (Java Swing)
    private DefaultTableModel inventoryTableModel;
    private DefaultTableModel resultsTableModel;
    private DefaultTableModel reserveTableModel;
    private JLabel statusLabel; // Our UI Feedback: Status of Waiting for Data Or Received

    // ENGINES | BRAINS
    // Private Final Variable So That It Will Not Be Change ANYWHERE
    // Private so that only this class will be able to touch it or modify
    // BUT usage of final so that it CANNOT be modified as it will be absolute
    // And we only need one brain or engine within our program
    private final RationEngine engine = new RationEngine();
    private final AuditLogger logger = new AuditLogger();
    
    // try-catch method
    // try { unsure input / risky input } catch {what you do after an error occurs}
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } // Bases the UI on the OS Used
        catch (Exception ignored) {} // If an error happens, simply ignore
        SwingUtilities.invokeLater(() -> new bootStrapper().setVisible(true)); // Prevents the GUI to crash
    } // Lets the background thread do the math while the EDT provides the user GUI

    // Bootstrap/Loading Screen Class 
    // This makes an illusion that something is processing while the program is loading the main GUI
static class bootStrapper extends JFrame {

    static ImageIcon[] frames; // Array of frames for animation
    static JLabel animationLabel; // Label to display the animation
    static int currentFrame = 0; // Current frame index

    public bootStrapper() {
        AuditLogger tempLogger = new AuditLogger();
        tempLogger.log("SYSTEM_STARTUP", "Application launched.");

        
        setTitle("BootStrapper - Smart Rationing System");
        setSize(1000, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true); // Makes no border or titlebar
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20)); // Roundds the corners of the window
        

        // Inputs background image and sets it as the content pane, allowing us to add components on top of it
        JLabel background = new JLabel(new ImageIcon("resources\\bootStrapBG.png"));
        background.setLayout(new BorderLayout());
        setContentPane(background);

        
        //initializes the labels of this loading screen
        JLabel label = new JLabel("Loading EquiEat Smart Rationing System...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel loadingStatus = new JLabel("Loading... ", SwingConstants.LEFT);
        JProgressBar progressBar = new JProgressBar(0, 100); // Uses the progress bar to visually show the loading progress
        
        progressBar.setStringPainted(true);
        loadingStatus.setFont(new Font("Arial", Font.PLAIN, 15));
        progressBar.setFont(new Font("Arial", Font.PLAIN, 15));
        
        bottomPanel.add(loadingStatus, BorderLayout.WEST);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        background.add(label, BorderLayout.CENTER);
        background.add(bottomPanel, BorderLayout.SOUTH);

        // uses array to store the file of each frames
        frames = new ImageIcon[5];
        frames[0] = new ImageIcon("resources\\frame1.png");
        frames[1] = new ImageIcon("resources\\frame2.png");
        frames[2] = new ImageIcon("resources\\frame3.png");
        frames[3] = new ImageIcon("resources\\frame4.png");
        frames[4] = new ImageIcon("resources\\frame5.png");
        
        // animation starts at frame 1
        animationLabel = new JLabel(frames[0]);
        add(animationLabel, BorderLayout.CENTER);

        animationLabel.setSize(200, 200);

        // Uses timer t to create the looping animation
        Timer timer = new Timer(200, new ActionListener(){
            @Override 
            public void actionPerformed(ActionEvent e){
                currentFrame++; // increments array index 
                
                if(currentFrame >= frames.length){ // checks if the current frame is greater than to the frames length
                    currentFrame =0; // resets the frame to 0
                }

                animationLabel.setIcon(frames[currentFrame]);
            }
        });

        timer.start();
        

        // Uses the array to allow modification inside the lambda expression of the timer
        int[] progress = {0};
            Timer progressTimer = new Timer(50, e -> {
            progress[0]++;
            progressBar.setValue(progress[0]);

            // Updates teh status when percentage reaches certain percentage
            if(progress[0] == 10) {
                loadingStatus.setText("Checking directories...");
                File resourcesDir = new File("resources");
                if (!resourcesDir.exists()){
                    JOptionPane.showMessageDialog(this, "Error 1: Missing 'resources' folder. Please reinstall!");
                    System.exit(1);
                }  
            }

            else if(progress[0] == 20) {
                loadingStatus.setText("Loading assets...");
                ImageIcon bg = new ImageIcon("resources\\bootStrapBG.png"); // Preloads the image to ensure it's cached
                setIconImage(new ImageIcon("resources\\icon.png").getImage());

                if (bg.getIconWidth() == -1){
                    JOptionPane.showMessageDialog(this, "Error 2: Missing file in resources. Please reinstall!");
                    System.exit(1);
                }
            }

            else if(progress[0] == 50){
                loadingStatus.setText("Loading system resources...");
                new RationEngine();
            }

            else if (progress[0] == 70){
                loadingStatus.setText("Verifying permissions...");
                File testFile = new File("resources\\test.tmp");
                try {
                    boolean created = testFile.createNewFile();
                    if(created){
                        testFile.delete();
                    } else 
                    {
                        testFile.delete();
                    }
                } catch (IOException ioException) {
                    JOptionPane.showMessageDialog(this, "Error 4: Insufficient write permissions in application directory.");
                    System.exit(1);
                } 
                    
            }

            else if(progress[0] == 90){
                loadingStatus.setText("Finalizing setup...");
            }
            
            // Checks when the progress has reached 100%, disposes the loading screen, and opens the main GUI -v-
            if (progress[0] >= 100) {
                ((javax.swing.Timer)e.getSource()).stop();
                dispose();
                new SmartRationGUI().setVisible(true); // now calls the GUI 
            }
        });

        progressTimer.start();
    }
}

    public SmartRationGUI() {
        logger.log("SYSTEM_STARTUP", "Main Application launched.");

        setIconImage(new ImageIcon("resources\\icon.png").getImage());
        setTitle("EquiEat -Smart Rationing System (SRS) - Integer Mode");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // X = Close the program :D
        setLocationRelativeTo(null); // Windows pop up in the dead center ALWAYS
        getContentPane().setBackground(new Color(33, 174, 192));


        // Makes a tab similar to a window tab
        // Lets you create separate tabs in the GUI
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setUI(new RoundedTabbedPaneUI(tabbedPane));
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 25));

        
        // Style the tabbed pane like buttons
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 25));
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        // TAB 1: CONTROL CENTER
        JPanel operationsPanel = new JPanel(new BorderLayout(10, 10));
        operationsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Adds padding around the panel
        operationsPanel.setBackground(new Color(33, 174, 192)); // sets background color in to blue
        getContentPane().setBackground(new Color(33, 174, 192)); // sets background color in to blue
        

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        topPanel.setBackground(new Color(33, 174, 192));

        JButton loadBtn = new JButton("1. Import Demographic CSV");
        loadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        statusLabel = new JLabel("Status: Waiting for Data...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 25));
        statusLabel.setForeground(Color.WHITE);

        loadBtn.addActionListener(e -> loadCSV());
        loadBtn.setBackground(new Color(76, 175, 80)); // Sets background into GREEN
        loadBtn.setForeground(Color.WHITE);
        loadBtn.setFont(new Font("Arial",Font.BOLD, 20));

        loadBtn.setUI(new javax.swing.plaf.metal.MetalButtonUI());

        loadBtn.setOpaque(true);
        loadBtn.setFocusPainted(false);

        loadBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(56, 142, 60), 3), // Dark green border
            BorderFactory.createEmptyBorder(10, 20, 10, 20)            // Padding
        ));
        
        topPanel.add(loadBtn);
        topPanel.add(statusLabel);
        operationsPanel.add(topPanel, BorderLayout.NORTH);
        

        JPanel formPanel = createInventoryForm();

        String[] invCols = {"Category", "Item Name", "Qty", "Target Priority"};
        inventoryTableModel = new DefaultTableModel(invCols, 0);
        JTable invTable = new JTable(inventoryTableModel);
        JScrollPane invScroll = new JScrollPane(invTable);
        invScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 2), "2. Warehouse Inventory"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, invScroll);
        splitPane.setDividerLocation(350);
        operationsPanel.add(splitPane, BorderLayout.CENTER);

        JButton runBtn = new JButton("3. RUN DISTRIBUTION & ANALYZE POPULATION");

        runBtn.setFont(new Font("Arial",Font.BOLD, 25));
        runBtn.setForeground(Color.WHITE);
        runBtn.setBackground(new Color(76, 175, 80)); // Sets background into GREEN
        runBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        runBtn.setUI(new javax.swing.plaf.metal.MetalButtonUI());

        runBtn.setOpaque(true);
        runBtn.setFocusPainted(false);

        // Add custom border
        runBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(56, 142, 60), 3), // Dark green border
            BorderFactory.createEmptyBorder(10, 20, 10, 20)            // Padding
        ));       
        
        runBtn.addActionListener(e -> runDistribution(tabbedPane));

        operationsPanel.add(runBtn, BorderLayout.SOUTH);

        // Load icons for tabs (create small icons, e.g., 20x20 pixels)
        ImageIcon controlIcon = createScaledIcon("resources\\control.png", 50,50);
        ImageIcon resultsIcon = createScaledIcon("resources\\distribution.png", 50, 50);
        ImageIcon reserveIcon = createScaledIcon("resources\\reserve.png", 50, 50);

        tabbedPane.addTab("Control Center", controlIcon, operationsPanel);
        tabbedPane.setBackgroundAt(0, new Color(26, 62, 66)); // Blue background
        tabbedPane.setForegroundAt(0, Color.WHITE); // White text

        // RESULTS
        JPanel resultsPanel = new JPanel(new BorderLayout());
        String[] resCols = {"Family ID", "Head of Family", "Size", "Needs", "RATION PACK CONTENT"};
        resultsTableModel = new DefaultTableModel(resCols, 0);
        JTable resultsTable = new JTable(resultsTableModel);
        resultsTable.setRowHeight(30);
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(500);
        resultsPanel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        JButton exportBtn = new JButton("Export Reports & Tickets");
        exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.setOpaque(true);
        exportBtn.setBorderPainted(false);
        exportBtn.addActionListener(e -> exportResults());
        resultsPanel.add(exportBtn, BorderLayout.SOUTH);

        tabbedPane.addTab("Distribution Results", resultsIcon, resultsPanel);
        tabbedPane.setBackgroundAt(1, new Color(26, 62, 66)); // Green background
        tabbedPane.setForegroundAt(1, Color.WHITE); // White text

        // RESERVE
        JPanel reservePanel = new JPanel(new BorderLayout());
        String[] reserveCols = {"Category", "Item Name", "RESERVE QUANTITY", "Status Note"};
        reserveTableModel = new DefaultTableModel(reserveCols, 0);
        JTable reserveTable = new JTable(reserveTableModel);
        reserveTable.setRowHeight(25);
        reserveTable.getTableHeader().setBackground(new Color(255, 200, 100));
        reservePanel.add(new JScrollPane(reserveTable), BorderLayout.CENTER);
        reservePanel.add(new JLabel("  * Includes Specialized Medicine and Leftovers (Whole Numbers Only)"), BorderLayout.SOUTH);
        tabbedPane.addTab("Reserve & Excess Stock", reserveIcon, reservePanel);
        tabbedPane.setBackgroundAt(2, new Color(26, 62, 66)); // Orange background
        tabbedPane.setForegroundAt(2, Color.WHITE); // White text

        add(tabbedPane);

        
    }

    private JPanel createInventoryForm() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(118, 139, 154), 3), // Outer border
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 2), "1. Add Inventory Items")
        ));
        
        panel.setBackground(new Color(165, 165, 165));



        JComboBox<SupplyCategory> catBox = new JComboBox<>(SupplyCategory.values());
        JTextField nameField = new JTextField();
        JTextField qtyField = new JTextField();
        JComboBox<PriorityAttribute> prioBox = new JComboBox<>(PriorityAttribute.values());
        prioBox.insertItemAt(null, 0);
        prioBox.setSelectedIndex(0);
        prioBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "EVERYONE (General)" : value.toString());
                return this;
            }
        });

        panel.add(new JLabel("Category:")); panel.add(catBox);
        panel.add(new JLabel("Item Name:")); panel.add(nameField);
        panel.add(new JLabel("Quantity (Total):")); panel.add(qtyField);
        panel.add(new JLabel("Priority Target:")); panel.add(prioBox);

        JButton addBtn = new JButton("Add to Inventory");
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
            ReportGenerator.generatePackingList(loadedFamilies, "Final_Packing_List.html");
            ReportGenerator.generateReserveReport(inventoryList, "Reserve_Stock_Report.txt");
            StubGenerator.generateHTMLStubs(loadedFamilies, "Claim_Stubs.html");
            logger.log("EXPORT", "Files generated: HTML, TXT, HTML.");

            JOptionPane.showMessageDialog(this, "Files Generated:\n1. Final_Packing_List.html\n2. Reserve_Stock_Report.txt\n3. Claim_Stubs.html");
            Desktop.getDesktop().open(new File("Claim_Stubs.html"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exporting: " + e.getMessage());
        }
    }

    // Helper method to load and scale icons for tabs
    private ImageIcon createScaledIcon(String path, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(path);
            if (icon.getIconWidth() > 0) {
                Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("Could not load icon: " + path);
        }
        return null; // Return null if icon can't be loaded (tab will show text only)
    }

    // Demographic Analysis

    public static class DemographicAnalyzer {
        public static String analyze(List<Family> families) {
            int totalFamilies = families.size();
            int totalPop = families.stream().mapToInt(Family::getMemberCount).sum();

            // Vulnerability Counters (Households containing X)
            int hasInfant = 0;
            int hasSenior = 0;
            int hasInjured = 0; // Includes INJURED
            int hasPWD = 0;     // Includes PWD

            for (Family f : families) {
                if (f.hasAttribute(PriorityAttribute.HAS_INFANT)) hasInfant++;
                if (f.hasAttribute(PriorityAttribute.HAS_SENIOR)) hasSenior++;
                if (f.hasAttribute(PriorityAttribute.INJURED)) hasInjured++;
                if (f.hasAttribute(PriorityAttribute.PWD)) hasPWD++;
            }

            // Build the Report String
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

    //  Audit Logger (For Transparency) NO TO CORRUPTION :P
    //  Modified into HTML Format for better and easier reading 💪 
    public static class AuditLogger {
        private final String LOG_FILE = "audit_log.html";
        
        public void log(String action, String details) {
            File logFile = new File(LOG_FILE);
            boolean fileExists = logFile.exists();

            try{
                if(!fileExists){
                    try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE))){
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
                String newEntry = String.format("<tr><td class='timestamp'>%s</td><td class='action %s'>%s</td><td class='details'>%s</td></tr>\n<!-- LOG_ENTRIES -->", timeStamp, action, action, details);

                String updatedContent = content.toString().replaceFirst("<!-- LOG_ENTRIES -->", newEntry);

                try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE))) {
                    pw.print(updatedContent);
                }

            }catch(IOException e){
                System.err.println("Logger Error: " + e.getMessage());
            }

        }
    }

    // Printable Stubs For Better Distribution
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

    // Model & Engines
    // enum helps us to only collect fixed inputs; ensures "type-safe" or proper error handling
    public enum PriorityAttribute { HAS_INFANT, HAS_SENIOR, PREGNANT, LACTATING, PWD, DIABETIC, INJURED }
    public enum SupplyCategory { STAPLE, PROTEIN, PRIORITY_NUTRITION, GENERAL_HEALTH, SPECIALIZED_MED }

    // Private variables so that the class family is the only one that can touch/modify them
    public static class Family {
        private String id, headOfFamily;
        private int memberCount;
        public Set<PriorityAttribute> attributes;

        //Map Integer instead of Double for Whole Numbers
        private Map<String, Integer> itemsReceived = new LinkedHashMap<>();
        // is inserted to the inventory (Remembers our input in order)


        // Constructor
        public Family(String id, String name, int size, Set<PriorityAttribute> attrs) {
            this.id = id; this.headOfFamily = name; this.memberCount = size; this.attributes = attrs; // One liner for clear code
        }
        // Receive item takes int qty
        public void receiveItem(String item, int qty) { itemsReceived.put(item, itemsReceived.getOrDefault(item, 0) + qty); }

        // we look at/get the private classes values but not change it
        public void clearReceived() { itemsReceived.clear(); }
        public boolean hasAttribute(PriorityAttribute attr) { return attributes.contains(attr); }
        public int getMemberCount() { return memberCount; }
        public String getId() { return id; }
        public String getHeadOfFamily() { return headOfFamily; }

        // Checks every family received items then turns it into a table
        public String getFormattedPackingList() {
            List<String> s = new ArrayList<>();
            for (Map.Entry<String, Integer> e : itemsReceived.entrySet()) {
                // Simple integer formatting
                s.add(String.format("%d pcs of %s", e.getValue(), e.getKey()));
            }
            return String.join(" + ", s); // i.e rather than "rice 5pcsmeat 6pcs" it will be "rice 5 pcs + meat 5 pcs"
        }
    }

    // all in one initialization and constructor
    public static class Supply {
        String name; SupplyCategory cat; int qty; PriorityAttribute target; double leftover;
        public Supply(String n, SupplyCategory c, int q, PriorityAttribute t) { name=n; cat=c; qty=q; target=t; }
        public void setLeftover(double l) { this.leftover = l; }
    }

    public static class RationEngine {
        public void distributeWithRounding(List<Family> families, List<Supply> inventory, int totalPop) {
            for (Supply item : inventory) {
                if (item.cat == SupplyCategory.SPECIALIZED_MED) {
                    item.setLeftover(item.qty);
                    continue;
                }
                List<Family> eligible = new ArrayList<>();
                int eligiblePop = 0;
                if (item.target != null) {
                    for (Family f : families) if (f.hasAttribute(item.target)) eligible.add(f);
                    eligiblePop = eligible.size();
                } else {
                    eligible.addAll(families);
                    eligiblePop = totalPop;
                }

                // CHANGED: Tracking total distributed as Integer
                int distributedTotal = 0;

                if (eligiblePop > 0) {
                    double unitShare = (double) item.qty / eligiblePop;
                    for (Family f : eligible) {
                        double rawAllocation;
                        if (item.target != null) rawAllocation = (double) item.qty / eligible.size();
                        else rawAllocation = unitShare * f.getMemberCount();

                        // Strict Floor to Integer
                        int safeAllocation = (int) Math.floor(rawAllocation);

                        if (safeAllocation > 0) {
                            f.receiveItem(item.name, safeAllocation);
                            distributedTotal += safeAllocation;
                        }
                    }
                }
                item.setLeftover(item.qty - distributedTotal); // Excess relief goods
            }
        }
    }

    // makes CVS into HTML for better reading and printting
    public static class ReportGenerator {
        public static void generatePackingList(List<Family> fList, String fname) throws IOException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(fname))) {
                
                pw.println("<!DOCTYPE html>");
                pw.println("<html><head><title>EquiEat - Packing List</title></head><body>");
                pw.println("<meta charset='UTF-8'>");
                pw.println("<style>");
                pw.println("body{font-family: Arial, \"Times New Roman\", Times , serif; background: linear-gradient(to bottom, #47BECE, #F3F3EF); height: 100%; margin: 0; background-repeat: no-repeat; background-attachment: fixed; padding: 20px;}\r\n");
                pw.println("nav{position: flex; top:0; right:0; width:100%; background-color: #fff; padding: 1rem; flex-direction: column; gap:1rem; justify-content: center; z-index: 10;}");
                pw.println("h1 {color: #21AEC0; text-align: center; letter-spacing: 2px; font-style: Arial ;}");
                pw.println("table {width: 100%; border-collapse: collapse; background-color: #fff; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); margin-top: 100px;}");
                pw.println("th {background: #21aec0; color: #fff; padding: 12px; text-align: left; border: 2px solid #fff;}");
                pw.println("td {padding: 10px; border-bottom: 1px solid #ddd;}");
                pw.println("tr:hover {background-color: #f1f1f1;}");
                pw.println(".priority {color: #d32f2f; font-weight: bold; font-size: 12px;}");
                pw.println(".allocation {color: #555; font-size: 14px;}");
                pw.println("</style></head><body>");
                pw.println("<nav><h1>EquiEat - Final Packing List</h1></nav>");
                pw.println("<table><tr><th>Family ID</th><th>Head of Family</th><th>Size</th><th>Priorities</th><th>Ration Allocation</th></tr>");

                for (Family f : fList) {
                    pw.printf("<tr><td>%s</td><td>%s</td><td>%d</td><td class='priority'>%s</td><td class='allocation'>%s</td></tr>%n",
                        escapeHtml(f.getId()), 
                        escapeHtml(f.getHeadOfFamily()), 
                        f.getMemberCount(),
                        escapeHtml(f.attributes.toString().replace(",", " ")),
                        escapeHtml(f.getFormattedPackingList()));
                }
                pw.println("</table>");
                pw.println("</body></html>");

            }
        }

        private static String escapeHtml(String text){
            return text.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;")
                       .replace("\"", "&quot;")
                       .replace("'", "&#39;");
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
                        //Format reserve stock as integer
                        String displayQty = String.format("%d", (int)s.leftover);
                        pw.printf("%-20s | %-15s | %-15s%n", s.name, s.cat, displayQty);
                    }
                }
            }
        }
    }
    // method to change the tab color and make it rounded
    static class RoundedTabbedPaneUI extends BasicTabbedPaneUI {
        private JTabbedPane tabbedPane;
        
        // Constructor to receive the tabbed pane for accessing.
        public RoundedTabbedPaneUI(JTabbedPane tabPane) {
            this.tabbedPane = tabPane;
        }
        
        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                           int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                        
            // Get the tab color
            g2d.setColor(tabbedPane.getBackgroundAt(tabIndex));
                                        
            // Draw rounded rectangle
            g2d.fillRoundRect(x, y, w, h, 15, 15); // 15 = corner radius
        }

        @Override
        protected Insets getTabInsets(int tabPlacement, int tabIndex){
            return new Insets(10,12,8, 12); // Adds padding around the tab text for better appearance
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            // No content border for a cleaner look
        }

        @Override 
        protected Insets getTabAreaInsets(int tabPlacement){
            return new Insets(15, 10, 15, 10);
        }
    }

}
