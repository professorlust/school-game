package de.entwicklerpages.java.schoolgame.tools.map;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.entwicklerpages.java.schoolgame.tools.PathHelper;

/**
 * Tool zum erzeugen einer Basis-Map.
 *
 * @author nico
 */
public class MapPanel extends JPanel implements ActionListener
{
    private final JButton createButton;

    private final FileFilter filter = new FileNameExtensionFilter("Tiled Map Datei", "tmx");

    /**
     * Konstruktor.
     *
     * Fügt ein Label als Erklärung und einen Button zum Erzeugen hinzu.
     */
    public MapPanel()
    {
        super(new BorderLayout());

        add(new JLabel("Eine neue Map erstellen:"), BorderLayout.NORTH);

        add(new JLabel("<html>Dieses Tool erstellt eine Vorlage für eine Map.<br>" +
                "Die Breite und Höhe beträgt 50 Kacheln.<br>" +
                "Die Größe kann in Tiled geändert werden.<br>" +
                "Die Layer sind korrekt beschriftet und richtig geordnet.</html>", JLabel.CENTER), BorderLayout.CENTER);


        createButton = new JButton("Erstellen");
        createButton.addActionListener(this);

        add(createButton, BorderLayout.SOUTH);
    }

    /**
     * Zeigt einen Speicher DIalog an und erzeugt in der ausgewählten Datei eine Map mithilfe des
     * MapCreators.
     *
     * @see MapCreator
     *
     * @param content
     */
    private void saveFile(String content)
    {
        JFileChooser chooser = new JFileChooser(PathHelper.getMapDirIfFound());
        chooser.addChoosableFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setMultiSelectionEnabled(false);

        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = chooser.getSelectedFile();

            if (!selectedFile.getAbsolutePath().endsWith(".tmx"))
                selectedFile = new File(selectedFile.getAbsolutePath() + ".tmx");

            if (selectedFile.exists() && selectedFile.isDirectory())
            {
                JOptionPane.showMessageDialog(this, "Es existiert dort bereits ein Verzeichnis mit diesem Namen!\n", "Fehler!", JOptionPane.OK_OPTION);
                return;
            }

            if (selectedFile.exists())
            {
                result = JOptionPane.showConfirmDialog(this, "Achtung!\n" +
                        "Es existiert bereits eine Datei mit diesem Namen.\n" +
                        "Wollen Sie wirklich die vorhandene Datei unwiederruflich überschreiben?", "Datei überschreiben", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(selectedFile));
                out.write(content);
                out.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Beim Speichern ist ein Fehler aufgetreten!\n" + e.getMessage(), "Fehler!", JOptionPane.OK_OPTION);
            }
        }
    }

    /**
     * Wird aufgerufen, wenn der Button gedrückt wurde.
     *
     * @param event das ausgelöste Ereignis
     */
    @Override
    public void actionPerformed(ActionEvent event)
    {
        if (event.getSource().equals(createButton))
        {
            saveFile(MapCreator.createMap());
        }
    }
}
